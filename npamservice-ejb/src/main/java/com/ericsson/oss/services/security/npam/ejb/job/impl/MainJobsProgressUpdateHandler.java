/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.security.npam.ejb.job.impl;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.security.npam.api.interfaces.JobConfigurationService;
import com.ericsson.oss.services.security.npam.api.interfaces.JobCreationService;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobState;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEJob;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType;
import com.ericsson.oss.services.security.npam.api.job.modelentities.Step;
import com.ericsson.oss.services.security.npam.api.message.NodePamSubmitMainJobRequest;
import com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccount;
import com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccountUpdateStatus;
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus;
import com.ericsson.oss.services.security.npam.ejb.job.mapper.JobMapper;
import com.ericsson.oss.services.security.npam.ejb.job.util.CompactAuditLoggerCreator;
import com.ericsson.oss.services.security.npam.ejb.message.NodePamQueueMessageSender;
import com.ericsson.oss.services.security.npam.ejb.dao.DpsReadOperations;
import com.ericsson.oss.services.security.npam.ejb.handler.NodePamUpdateHandlerWithTx;
import com.ericsson.oss.services.security.npam.ejb.services.NodePamEncryptionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MUID_ONE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MUID_TWO;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.RESET_CREDENTIALS_FALSE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.RESET_CREDENTIALS_TRUE;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MainJobsProgressUpdateHandler {

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    private JobMapper jobMapper;

    @Inject
    private NodePamQueueMessageSender nodePamQueueMessageSender;

    @Inject
    private JobCreationService jobCreationService;

    @Inject
    private DpsReadOperations dpsReadOperations;

    @Inject
    NodePamConfigStatus nodePamConfigStatus;

    @Inject
    NodePamUpdateHandlerWithTx nodePamUpdateHandlerWithTx;

    @Inject
    NodePamEncryptionManager nodePamEncryptionManager;

    @Inject
    CompactAuditLoggerCreator compactAuditLoggerCreator;

    @Inject
    private SystemRecorder systemRecorder;

    private static final Logger LOGGER = LoggerFactory.getLogger(MainJobsProgressUpdateHandler.class);

    private static final long LONG_TIME_JOB_EXECUTION = 1200000L;   // 20 minutes (too big?)
    private static final long LONG_TIME_WAIT_FOR_EVENT = 1800000L;   // 30 minutes (this is ok)
    private static final String ERROR_EXECUTION_TIMEOUT = "Node configuration not started in 20 minutes";

    private static final String WAIT_FOR_EVENT_TIMEOUT = "remoteManagement attribute value change not received in 30 minutes";

    public void scheduleScheduledMainJobs() {

        final List<Long> scheduledJobList = jobConfigurationService.getMainJobIds(JobState.SCHEDULED.getJobStateName());
        if (!scheduledJobList.isEmpty()) {
            LOGGER.info(" scheduleScheduledMainJobs scheduledJobList list = {}",scheduledJobList);
        }

        final long currentTime = System.currentTimeMillis();
        for (final long jobId : scheduledJobList) {
            final Map<String, Object> jobAttributes= jobConfigurationService.retrieveJob(jobId);

            final NPamJob job = jobMapper.getJobDetails(jobAttributes, jobId);
            LOGGER.debug(" scheduleScheduledMainJobs currentTime = {}  --  scheduledTime = {} ",currentTime, job.getScheduledTime().getTime());
             if (job.getScheduledTime().getTime() < currentTime) {
                 LOGGER.info(" scheduleScheduledMainJobs submitted job = {} ",jobId);
                 jobConfigurationService.updateJobState(jobId,JobState.SUBMITTED.getJobStateName());
                 NodePamSubmitMainJobRequest nodePamSubmitMainJob = new NodePamSubmitMainJobRequest(jobId);
                 nodePamQueueMessageSender.sendSubmitMainJobMessage(nodePamSubmitMainJob);
             }
        }
    }

    @SuppressWarnings({"squid:S3252"})
    public boolean updateRunningMainJobs() {
        final long startTime = System.currentTimeMillis();
        boolean isThereRunningJob = false;
        final List<Long> runningJobList = jobConfigurationService.getMainJobIds(JobState.RUNNING.getJobStateName());
        if (!runningJobList.isEmpty()) {
            LOGGER.info("updateRunningMainJobs runningJobList list = {}",runningJobList);
            isThereRunningJob = true;
        }
        for (final long jobId : runningJobList) {
            final Map<String, Object> jobAttributes= jobConfigurationService.retrieveJob(jobId);
            final NPamJob mainJob = jobMapper.getJobDetails(jobAttributes, jobId);

            updateNeJobAndNeAccountIfNeed(mainJob);
            final long deltaTime1  = System.currentTimeMillis() - startTime;

            updateMainJobStatus(jobId, mainJob);
            final long deltaTime2  = System.currentTimeMillis() - startTime;

            LOGGER.debug("mainJobsTimer::jobId={} deltaTime1={}, deltaTime2={}",jobId, deltaTime1, deltaTime2);
        }
        return isThereRunningJob;
    }
    
    private void updateMainJobStatus( final long mainJobId, final NPamJob mainJob) {
        final List<Long> neJobIds = jobConfigurationService.getNeJobIdsConnectedToMainJobWithJobState(mainJob.getJobId(), JobState.COMPLETED.getJobStateName());  //EMARDEP MA SOLO COMPLETED
        LOGGER.debug("updateMainJobStatus recovered neJobIds.size()={} for mainJobId={} with JobState.COMPLETED", neJobIds.size(), mainJob.getJobId());

        int neJobsCompleted = neJobIds.size();
        int neJobsTotal = mainJob.getNumberOfNetworkElements();
        if (allNeJobsCompleted(neJobsCompleted, neJobsTotal)) {
            LOGGER.info("updateRunningMainJobs ALL NE JOB COMPLETED neJobsCompleted={}, neJobsTotal={}", neJobsCompleted, neJobsTotal);

            //check for SUCCESS or SKIPPED
            int neJobsSuccess = 0;
            int calNeJobSuccess = 0;
            int calNeJobSkipped = 0;
            Date lastEndDate = null;
            for (final long neJobId : neJobIds) {
                final Map<String, Object> neJobAttributes= jobConfigurationService.retrieveNEJob(neJobId);
                final NPamNEJob neJob = jobMapper.getNEJobDetails(neJobAttributes, neJobId);
                LOGGER.info("updateRunningMainJobs neJob completed {} ", neJob.getNeName());
                lastEndDate = updateLastEndDate(neJob.getEndTime(), lastEndDate);

                if ((neJob.getResult() == JobResult.SUCCESS)) {
                   LOGGER.info("updateRunningMainJobs neJob success {} ", neJob.getNeName());
                   neJobsSuccess++;
                   calNeJobSuccess++;
                } else if (neJob.getResult() == JobResult.SKIPPED) {
                    LOGGER.info("updateRunningMainJobs neJob skipped {} ", neJob.getNeName());
                    neJobsSuccess++;
                    calNeJobSkipped++;
                }
            }

            JobResult jobResult = JobResult.FAILED;
            if (neJobsSuccess == neJobsCompleted) {
                jobResult = JobResult.SUCCESS;
            }
            jobConfigurationService.updateMainJobStateToCompletedWithEndDate(mainJobId, jobResult.getJobResult(), lastEndDate);
            if (lastEndDate != null) {
                LOGGER.info("lastEndDate: {}", lastEndDate);
            }

            compactAuditLoggerCreator.logCompactAuditLog(mainJob, jobResult, neJobsTotal, neJobsSuccess, calNeJobSuccess, calNeJobSkipped);

            mainJobInstrumentRecord(mainJob, new Date(), jobResult, neJobsSuccess);

            jobCreationService.createNewJobIfNecessary(mainJob.getTemplateJobId(),mainJob.getExecutionIndex());
        } else {
            double progressPercentage = calculateProgress((double)neJobsCompleted, neJobsTotal);
            LOGGER.info("updateRunningMainJobs new PROGRESS = {} ",progressPercentage);
            if (progressPercentage != mainJob.getProgressPercentage()) {
                jobConfigurationService.updateMainJobProgressPercentage(mainJobId, progressPercentage);
            }
        }
    }

    private void mainJobInstrumentRecord(final NPamJob mainJob, final Date endTime, final JobResult jobResult, final int neJobSuccess) {

        Date startTime;
        long timeSpentOnJobInMillis =0;
        double timeSpentOnJobInSeconds = 0;
        int neJobRate = 0;

        int numberOfNetworkElements = mainJob.getNumberOfNetworkElements();

        startTime = mainJob.getStartTime();
        if (startTime != null) {
            timeSpentOnJobInMillis = endTime.getTime() - startTime.getTime();
                LOGGER.info("startTime: {}  endTime: {} timeSpentOnJobInMillis: {}", startTime, endTime, timeSpentOnJobInMillis);
            if (timeSpentOnJobInMillis != 0) {
                timeSpentOnJobInSeconds = ((endTime.getTime() - mainJob.getStartTime().getTime()) / 1000.0);
            }
        }

        if (timeSpentOnJobInSeconds != 0) {
            neJobRate = (int) (numberOfNetworkElements / timeSpentOnJobInSeconds);
        }

        final Map<String, Object> recordEventData = new HashMap<>();
        recordEventData.put("JobType", mainJob.getJobType().getJobTypeName());
        recordEventData.put("NumberOfNetworkElements", numberOfNetworkElements);
        recordEventData.put("DurationOfJob", timeSpentOnJobInMillis);
        recordEventData.put("NeJobRate", neJobRate);
        recordEventData.put("Status", JobState.COMPLETED.getJobStateName());
        recordEventData.put("Result", jobResult.getJobResult());
        recordEventData.put("NumberOfNeJobFailed", numberOfNetworkElements - neJobSuccess);

        systemRecorder.recordEventData("NPAM.MainJobComplete", recordEventData);
    }

    private boolean allNeJobsCompleted(final int neJobCompleted, final int neJobsTotal) {
        return neJobCompleted >= neJobsTotal;
    }

    private void updateNeJobAndNeAccountIfNeed(final NPamJob mainJob) {
        final List<Long> neJobIds = jobConfigurationService.getNeJobIdsConnectedToMainJobWithJobState(mainJob.getJobId(), JobState.RUNNING.getJobStateName());
        final List<Long> neJobIdsCreated = jobConfigurationService.getNeJobIdsConnectedToMainJobWithJobState(mainJob.getJobId(), JobState.CREATED.getJobStateName());
        neJobIds.addAll(neJobIdsCreated);
        LOGGER.debug("updateNeJobAndNeAccountIfNeed recovered neJobIds.size()={} for mainJobId={} with JobState.RUNNING", neJobIds.size(), mainJob.getJobId());

        final JobType jobType = mainJob.getJobType();
        List<String> neAccountList = neAccountToBeConfigure();

        for (final long neJobId : neJobIds) {
            final Map<String, Object> neJobAttributes= jobConfigurationService.retrieveNEJob(neJobId);
            final NPamNEJob neJob = jobMapper.getNEJobDetails(neJobAttributes, neJobId);

            if (!isWaitingForEvent(neJob)) {
                Date neJobTime = null;
                if (neJob.getState() == JobState.RUNNING ) {
                    neJobTime =  neJob.getStartTime();
                } else {
                    neJobTime = neJob.getCreationTime();
                }
                final long deltaTime = System.currentTimeMillis() - getMilliSecondFromDate(neJobTime);
                LOGGER.debug("updateRunningMainJobs neJob {} running for milliseconds {}", neJob.getNeName(), deltaTime);
                if (deltaTime > LONG_TIME_JOB_EXECUTION) {
                    // long time running execution time then neJob forced to FAILED
                    LOGGER.error("updateRunningMainJobs neJob {} long execution time in {} state - neJob forced to completed/failed state", neJob.getNeName(), neJob.getState());
                    updateNeJobAttributes(neJobId, JobState.COMPLETED, JobResult.FAILED, new Date(), ERROR_EXECUTION_TIMEOUT);
                }
            } else {
                updatePartiallyCompletedNeJob(neJob, jobType, neAccountList);
            }
        }
    }

    //This Step.WFE is set by NodePamUpdateMaintenanceUserHandler when JobType=CREATE_NE_ACCOUNT and DETACH_NE_ACCOUNT ask mediation to modify remoteManagement attribute
    //It indicates we are waiting for remoteManagement DPS notification event
    private boolean isWaitingForEvent(final NPamNEJob neJob) {
        return neJob.getStep() == Step.WFE;
    }

    private void updatePartiallyCompletedNeJob(final NPamNEJob neJob, final JobType jobType, final List<String> neAccountList) {
        if (jobType == JobType.CREATE_NE_ACCOUNT) {
            updatePartiallyCompletedNeJobAndJobTypeEnableRemoteManag(neJob, jobType, neAccountList);
        }
        if (jobType == JobType.DETACH_NE_ACCOUNT) {
            updatePartiallyCompletedNeJobAndJobTypeDisableRemoteManag(neJob, jobType, neAccountList);
        }
    }

    @SuppressWarnings({"squid:S3776"})
    private void updatePartiallyCompletedNeJobAndJobTypeEnableRemoteManag(final NPamNEJob neJob, final JobType jobType, final List<String> neAccountList) {

        Date lastUpdateDate = null;
        int neAccountCompleted = 0;
        boolean isFailed = false;
        String errorDetails = null;
        long neJobId = neJob.getNeJobId();
        long deltaTime = System.currentTimeMillis() - getMilliSecondFromDate(neJob.getEndTime());
        for (final String neAccountId : neAccountList) {
            boolean checkTimeout = true;
            // read neAccount per node neName
            NetworkElementAccount neAccount = dpsReadOperations.getNetworkElementAccount(neJob.getNeName(), neAccountId);
            if (neAccount != null) {
                if ((neAccount.getUpdateStatus() == NetworkElementAccountUpdateStatus.CONFIGURED) && (neAccount.getLastPasswordChange().after(neJob.getStartTime()))) {
                        checkTimeout = false;
                        LOGGER.info("updateRunningMainJobs CREATE_NE_ACCOUNT NPamNEJob.STEP=WFE {}  neAccount state = CONFIGURED then forced neJob to Completed/Success state", neJob.getNeName());
                        nodePamUpdateHandlerWithTx.updateNetworkElementAccountWithJobDetails(neJob, neAccountId, jobType, RESET_CREDENTIALS_FALSE);
                        neAccountCompleted++;
                        lastUpdateDate = updateLastEndDate(neAccount.getLastPasswordChange(), lastUpdateDate);
                }
                if ((neAccount.getUpdateStatus() == NetworkElementAccountUpdateStatus.FAILED) && (neAccount.getLastFailed().after(neJob.getStartTime()))) {
                        checkTimeout = false;
                        LOGGER.info("updateRunningMainJobs CREATE_NE_ACCOUNT NPamNEJob.STEP=WFE {}  neAccount state = FAILED then forced neJob to Completed/Failed state -- LastFailedDate={}",
                                neJob.getNeName(), neAccount.getLastFailed());
                        nodePamUpdateHandlerWithTx.updateNetworkElementAccountWithJobDetails(neJob, neAccountId, jobType, RESET_CREDENTIALS_TRUE);
                        neAccountCompleted++;
                        lastUpdateDate = updateLastEndDate(neAccount.getLastFailed(), lastUpdateDate);
                        isFailed = true;
                        errorDetails = updateErrorDetailsIfNeed(neAccount.getErrorDetails(), errorDetails);
                }
            }

            if (checkTimeout) {
                final long timeout = getTimeoutFromNeAccount(neAccount);
                if (deltaTime > timeout) {   // neJobTimeout
                    LOGGER.info("updateRunningMainJobs CREATE_NE_ACCOUNT NPamNEJob.STEP=WFE {} neAccount timeout expired after {}", neJob.getNeName(), deltaTime);
                    final Date failedDate = new Date();
                    final String errorMessage = getTimeoutMessage(neAccount);
                    nodePamUpdateHandlerWithTx.updateNetworkElementAccountToFailedStatus(neJob, neAccountId, errorMessage, failedDate, jobType, RESET_CREDENTIALS_TRUE);
                    neAccountCompleted++;
                    lastUpdateDate = updateLastEndDate(failedDate, lastUpdateDate);
                    isFailed = true;
                    errorDetails = updateErrorDetailsIfNeed(errorMessage, errorDetails);
                }
            }
        }

        if (neAccountCompleted >= neAccountList.size()) {
            if (isFailed) {
                updateNeJobAttributes(neJobId, JobState.COMPLETED, JobResult.FAILED, lastUpdateDate, errorDetails);
            } else {
                updateNeJobAttributes(neJobId, JobState.COMPLETED, JobResult.SUCCESS, lastUpdateDate, null);
            }
        }
    }

    private long getTimeoutFromNeAccount(NetworkElementAccount neAccount) {
        if (neAccount != null && neAccount.getUpdateStatus() == NetworkElementAccountUpdateStatus.ONGOING) {
            return LONG_TIME_JOB_EXECUTION;
        } else {
            return LONG_TIME_WAIT_FOR_EVENT;
        }
    }

    private String getTimeoutMessage (NetworkElementAccount neAccount) {
        if (neAccount != null && neAccount.getUpdateStatus() == NetworkElementAccountUpdateStatus.ONGOING) {
            return ERROR_EXECUTION_TIMEOUT;
        } else {
            return WAIT_FOR_EVENT_TIMEOUT;
        }
    }

    private void updatePartiallyCompletedNeJobAndJobTypeDisableRemoteManag(final NPamNEJob neJob, final JobType jobType, final List<String> neAccountList) {
        LOGGER.info("updateRunningMainJobs DETACH_NE_ACCOUNT neJob PARTIALLY COMPLETED {} ", neJob.getNeName());
        long neJobId = neJob.getNeJobId();
        int neAccountCompleted = 0;
        boolean isFailed = false;
        String errorDetails = null;

        for (final String neAccountId : neAccountList) {
            NetworkElementAccount neAccount = dpsReadOperations.getNetworkElementAccount(neJob.getNeName(), neAccountId);
            if (neAccount == null || neAccount.getUpdateStatus() == NetworkElementAccountUpdateStatus.DETACHED) {
                neAccountCompleted++;
                nodePamUpdateHandlerWithTx.updateNetworkElementAccountWithJobDetails(neJob, neAccountId, jobType, RESET_CREDENTIALS_FALSE);
            } else {
                long deltaTime = System.currentTimeMillis() - getMilliSecondFromDate(neJob.getEndTime());

                if (deltaTime > LONG_TIME_WAIT_FOR_EVENT) {   // neJobTimeout
                    LOGGER.info("updateRunningMainJobs DETACH_NE_ACCOUNT NPamNEJob.STEP=WFE {}  timeout expired after {}", neJob.getNeName(), deltaTime);
                    final Date failedDate = new Date();
                    final String errorMessage = getTimeoutMessage(neAccount);
                    nodePamUpdateHandlerWithTx.updateNetworkElementAccountToFailedStatus(neJob, neAccountId, errorMessage, failedDate, jobType, RESET_CREDENTIALS_FALSE);
                    neAccountCompleted++;
                    isFailed = true;
                    errorDetails = updateErrorDetailsIfNeed(errorMessage, errorDetails);
                }
            }
        }

        if (neAccountCompleted >= neAccountList.size()) {
            if (isFailed) {
                updateNeJobAttributes(neJobId, JobState.COMPLETED, JobResult.FAILED, new Date(), errorDetails);
            } else {
                updateNeJobAttributes(neJobId, JobState.COMPLETED, JobResult.SUCCESS, new Date(), null);
            }
        }
    }

    private long getMilliSecondFromDate(final Date date) {
        if (date == null) {
            LOGGER.error("Unexpected date null ");
            return 0L;
        }
        return date.getTime();
    }

    @SuppressWarnings({"squid:S3252"})
    private void updateNeJobAttributes(final long neJobId, final JobState jobState, final JobResult jobResult, final Date endDate, final String errorDetails) {

        Map<String, Object> attributes = new HashMap<>(3);
        attributes.put(NPamNEJob.JOB_STATE, jobState.getJobStateName());
        attributes.put(NPamNEJob.JOB_RESULT, jobResult.getJobResult());
        attributes.put(NPamNEJob.JOB_END_TIME, endDate);
        if (errorDetails != null) {
            attributes.put(NPamNEJob.JOB_ERROR_DETAILS, errorDetails);
        }
        jobConfigurationService.updateJobAttributes(neJobId, attributes);
    }

    private double calculateProgress( final double neJobCompleted, final int neJobTotal) {
        if (neJobCompleted != 0.0) {
            double num = (neJobCompleted / neJobTotal) * 100.0;
            DecimalFormat df = new DecimalFormat("#.#");  // we want only 1 decimal (truncated) as 99.5
            df.setRoundingMode(RoundingMode.FLOOR);
            return Double.valueOf(df.format(num));
        }
        return 0.0;
    }

    private List<String> neAccountToBeConfigure() {
        List<String> neAccountList = new ArrayList<>(1);
        neAccountList.add(MUID_ONE);
        if (nodePamConfigStatus.isCbrsDomainEnabled()) {
            neAccountList.add(MUID_TWO);
        }
        return neAccountList;
    }
    private Date updateLastEndDate( final Date endDate, final Date lastEndDate) {
        if (lastEndDate != null && (endDate==null || lastEndDate.after(endDate))) {
            return lastEndDate;
        }
        return endDate;
    }

    private String updateErrorDetailsIfNeed(final String mewErrorDetails, final String oldErrorDetails) {
        if (oldErrorDetails == null) {
            return mewErrorDetails;
        }
        return oldErrorDetails;
    }
}
