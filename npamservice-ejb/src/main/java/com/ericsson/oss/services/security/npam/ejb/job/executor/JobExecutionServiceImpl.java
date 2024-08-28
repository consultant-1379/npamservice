/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.security.npam.ejb.job.executor;

import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.NEJOB_CREATE;
import static com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants.PK_NEXT_PASSWORD;
import static com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants.PK_NEXT_USERNAME;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MUID_ONE;
import static com.ericsson.oss.services.security.npam.api.job.modelentities.JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import com.ericsson.oss.services.security.npam.ejb.log.ExceptionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.security.npam.api.constants.ModelsConstants;
import com.ericsson.oss.services.security.npam.api.constants.NodePamApplication;
import com.ericsson.oss.services.security.npam.api.exceptions.JobConfigurationException;
import com.ericsson.oss.services.security.npam.api.exceptions.JobError;
import com.ericsson.oss.services.security.npam.api.exceptions.JobExceptionFactory;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails;
import com.ericsson.oss.services.security.npam.api.interfaces.JobConfigurationService;
import com.ericsson.oss.services.security.npam.api.interfaces.JobExecutionService;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobState;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NEInfo;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEJob;
import com.ericsson.oss.services.security.npam.api.job.modelentities.Step;
import com.ericsson.oss.services.security.npam.api.message.NodePamAutogeneratePasswordRequest;
import com.ericsson.oss.services.security.npam.api.message.NodePamDisableRemoteManagementRequest;
import com.ericsson.oss.services.security.npam.api.message.NodePamEnableRemoteManagementRequest;
import com.ericsson.oss.services.security.npam.api.message.NodePamRecoveryConfigurationRequest;
import com.ericsson.oss.services.security.npam.api.message.NodePamRequest;
import com.ericsson.oss.services.security.npam.api.message.NodePamUpdatePasswordRequest;
import com.ericsson.oss.services.security.npam.ejb.job.mapper.JobMapper;
import com.ericsson.oss.services.security.npam.ejb.job.util.FileResource;
import com.ericsson.oss.services.security.npam.ejb.message.NodePamQueueMessageSender;
import com.ericsson.oss.services.security.npam.ejb.utility.NetworkUtil;
import static com.ericsson.oss.services.security.npam.ejb.utility.ThreadSuspend.waitFor;

/**
 * Job executor to start Main job, NE jobs
 *
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class JobExecutionServiceImpl implements JobExecutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobExecutionServiceImpl.class);

    private static final String QUEUE_FULL = "NodePamRequestQueue Full";
    private static final long TIME_TO_RETRANSMIT = TimeUnit.MILLISECONDS.toMillis(500);
    private static final long RETRANSMISSION_DELAY = TimeUnit.MILLISECONDS.toMillis(200);
    private static final int MAX_RETRANSMISSIONS = 10;
    private static final int BATCH_SIZE = 10;
    private static final int INITIAL_BATCH_SIZE = 500;
    private static final long RATE_LIMIT = TimeUnit.MILLISECONDS.toMillis(900);

    @Inject
    private SystemRecorder systemRecorder;
    @Inject
    private JobMapper jobMapper;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    NetworkUtil networkUtil;

    @Inject
    JobExceptionFactory jobExceptionFactory;

    @Inject
    NodePamQueueMessageSender nodePamUpdateRequestQueueSender;

    @Inject
    FileResource fileResource;

    @Inject
    ExceptionHelper exceptionHelper;

    @Override
    public void runMainJob(final long jobId) {
        try {
            LOGGER.info("runMainJob:: jobId={}", jobId);
            final Map<String, Object> jobAttributes = jobConfigurationService.retrieveJob(jobId);
            if (jobAttributes == null || jobAttributes.isEmpty()) {
                LOGGER.error("runMainJob:: Unable to retrieve job from db for the jobId={}", jobId);
                throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_NOT_FOUND_JOB_ID, jobId);
            }

            LOGGER.info("runMainJob:: Recovered jobAttributes={}", jobAttributes);
            final NPamJob job = jobMapper.getJobDetails(jobAttributes, jobId);
            LOGGER.info("runMainJob:: Remapped jobAttributes={} to job={}", jobAttributes, job);

            if (!JobState.isJobSubmitted(job.getState())) {
                LOGGER.error("runMainJob:: Impossible to start job jobId={} with status={}", job.getJobId(), job.getState());
                throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_INVALID_STATE_FOR_JOB, jobId, job.getState());
            }

            //Retrieve credentials from imported file
            final Map<String, List<String>> nodeCredentialsFromFile = retrieveUserCredentialsIfNecessary(jobId, job);

            createNEJobs(job, job.getOwner(), nodeCredentialsFromFile);

        } catch (final JobConfigurationException e) {
            LOGGER.info("runMainJob:: JobConfigurationException e.getClass={}, e.getMessage={} occured during job starting having jobId={} ", e.getClass(), e.getMessage(), jobId);
            markMainJobAsFailedLoggingException(jobId, e.getMessage());
        } catch (final NPAMRestErrorException e) {
            LOGGER.info("runMainJob:: NPAMRestErrorException e.getClass={}, e.getMessage={} occured during job starting having jobId={} ",
                    e.getClass(), e.getInternalCode().getMessage(), jobId);
            markMainJobAsFailedLoggingException(jobId, e.getInternalCode().getMessage());
        } catch (final Exception e) {
            LOGGER.info("runMainJob:: Exception e.getClass={}, e.getMessage={} occured during job starting having jobId={} ", e.getClass(), e.getMessage(), jobId);
            markMainJobAsFailedLoggingException(jobId, e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("squid:S2139")
    public void cancelScheduledMainJob(final String jobName) {
        try {
            LOGGER.info("cancelScheduledMainJob:: jobName={}", jobName);
            if (jobName == null) {
                // preventive action because, if jobName=null, getMainJobs returns all jobs
                LOGGER.error("cancelScheduledMainJob:: No job found with name = {}", (Object) null);
                throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_NAME);
            }

            final List<NPamJob> jobList = jobConfigurationService.getMainJobs(jobName);
            ///Remember that getMainJobs throws NPAMRestErrorException if no job with 'jobName' is found

            boolean scheduledFound = false;
            NPamJob jobSavedForErrorMessageIfAny = jobList.get(0);
            for (final NPamJob job : jobList) {
                final JobState jobState = job.getState();
                if (JobState.isJobScheduled(jobState)) {
                    LOGGER.info("cancelScheduledMainJob:: Cancelling job = {}", jobName);
                    jobConfigurationService.updateJobState(job.getJobId(), JobState.USER_CANCELLED.getJobStateName());
                    LOGGER.info("cancelScheduledMainJob:: Cancelled job = {}", jobName);
                    scheduledFound = true;
                    // could return, but don't do that in order to delete all possible multiple SCHEDULED jobs (should never happen, but you never know)
                } else {
                    if (!JobState.isJobCompleted(jobState)) {
                        jobSavedForErrorMessageIfAny = job;
                    }
                }
            }
            if (!scheduledFound) {  // no job found with jobState=SCHEDULED
                chooseErrorResponseForJobCancel(jobSavedForErrorMessageIfAny);
            }
        } catch (final NPAMRestErrorException e) {
            LOGGER.error("cancelScheduledMainJob:: NPAMRestErrorException occurred during job={} canceling - message={}", jobName,
                    e.getInternalCode().getMessage());
            throw e;
        } catch (final Exception e) {
            LOGGER.error("cancelScheduledMainJob:: Exception occurred during job canceling={} - message={}", jobName, e.getMessage());
            throw e;
        }
    }

    private void chooseErrorResponseForJobCancel(final NPamJob job) {
        final String jobName = job.getName();
        final JobState jobState = job.getState();

        if (JobState.isJobUserCancelled(jobState)) {
            // JobState = USER_CANCELLED
            LOGGER.error("cancelScheduledMainJob:: Impossible to cancel already cancelled job={}", jobName);
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_CANCEL_JOB,
                    String.format(NPAMRestErrorsMessageDetails.UNPROCESSABLE_CANCEL_JOB_WRONG_STATE, jobState.getJobStateName()));
        } else if (JobState.isJobCompleted(jobState)) {
            // JobState = COMPLETED
            LOGGER.error("cancelScheduledMainJob:: Impossible to cancel already completed job={}", jobName);
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_CANCEL_JOB,
                    String.format(NPAMRestErrorsMessageDetails.UNPROCESSABLE_CANCEL_JOB_WRONG_STATE, jobState.getJobStateName()));
        } else if (JobState.isJobAlreadyStarted(jobState)) {
            // Job already started
            LOGGER.error("cancelScheduledMainJob:: Impossible to cancel and already started job={} and having status={}", jobName, jobState);
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_CANCEL_JOB,
                    String.format(NPAMRestErrorsMessageDetails.UNPROCESSABLE_CANCEL_JOB_WRONG_STATE, jobState.getJobStateName()));
        } else {
            // JobState = CREATED   /// should never happen
            LOGGER.error("cancelScheduledMainJob:: Impossible to cancel job={} having status={}", jobName, jobState);
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_CANCEL_JOB,
                    String.format(NPAMRestErrorsMessageDetails.UNPROCESSABLE_CANCEL_JOB_WRONG_STATE, jobState.getJobStateName()));
        }
    }

    @SuppressWarnings({"squid:S3252"})
    private void setNumberOfNetworkElementsCountAndStatusRunning(final NPamJob job, final int neJobsRunning) {
        final Map<String, Object> jobAttributes = new HashMap<>();
        jobAttributes.put(NPamJob.JOB_START_TIME, new Date());
        jobAttributes.put(NPamJob.JOB_NUMBER_NETWORK_ELEMENTS, neJobsRunning);
        jobAttributes.put(NPamJob.JOB_STATE, JobState.RUNNING.getJobStateName());

        jobConfigurationService.updateJobAttributes(job.getJobId(), jobAttributes);
        LOGGER.debug("Attributes Updated for the Job with Id {} with attributes {}", job.getJobId(), jobAttributes);
    }

    @SuppressWarnings("unchecked")
    private void createNEJobs(final NPamJob job, final String jobOwner, final Map<String, List<String>> nodeCredentialsFromFile) {
        LOGGER.info("createNEJobs::START createNEJobs using jobId={}", job.getJobId());
        LOGGER.debug("createNEJobs:: jobType={}. HERE WE HAVE TO SPLIT FOR JOB TYPE (now only inside sendNodeUpdateMessage)", job.getJobType());
        final Set<String> nodeNames = getAllNes(job.getSelectedNEs(), jobOwner);
        LOGGER.debug("createNEJobs:: recovered nodeNames={}", nodeNames);
        LOGGER.info("createNEJobs:: recovered nodeNames.size={}", nodeNames.size());

        final Map<Long, String> createdNeJobs = new HashMap<>();
        if (!nodeNames.isEmpty()) {
            for (final String nodeName : nodeNames) {
                LOGGER.info("createNEJobs:: Creation Processing started for NEJob for node={}", nodeName);
                final long neJobId = createNEJobAndReturnPoId(job, nodeName);
                systemRecorder.recordEvent(NEJOB_CREATE, EventLevel.COARSE, "",  "NEJOB","NEJob is created with Id: " + neJobId + " with mainJobId: " + job.getJobId());
                createdNeJobs.put(neJobId, nodeName);
            }

            //Set JOB as RUNNING
            setNumberOfNetworkElementsCountAndStatusRunning(job, createdNeJobs.size());

            sendNodeUpdateMessageBatch(job, jobOwner, nodeCredentialsFromFile, createdNeJobs);
        } else {
            LOGGER.error("createNEJobs:: nodeNames is empty for jobId={}", job.getJobId());
            throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_NO_NODES, job.getJobId());
        }
        LOGGER.info("createNEJobs::STOP createNEJobs using jobId={}", job.getJobId());
    }

    private long createNEJobAndReturnPoId(final NPamJob job,
                                          final String neName) {
        final Map<String, Object> jobAttributes = populateNEJobAttributesForCreate(job, neName);
        return jobConfigurationService.createPO(ModelsConstants.NAMESPACE, ModelsConstants.NPAM_NEJOB, ModelsConstants.VERSION,
                jobAttributes);
    }

    @SuppressWarnings({"squid:S3252"})
    private Map<String, Object> populateNEJobAttributesForCreate(final NPamJob job, final String neName) {
        final Map<String, Object> neJobAttributes = new HashMap<>();
        neJobAttributes.put(NPamNEJob.NEJOB_MAIN_JOB_ID, job.getJobId());
        final Date creationTime = new Date();
        neJobAttributes.put(NPamNEJob.JOB_CREATION_TIME, creationTime);
        neJobAttributes.put(NPamNEJob.JOB_STATE, JobState.CREATED.getJobStateName());
        neJobAttributes.put(NPamNEJob.NEJOB_NE_NAME, neName);
        neJobAttributes.put(NPamNEJob.STEP, Step.NONE.name());
        return neJobAttributes;
    }

    public String generateNbiRequestId() {
        return NodePamApplication.NODEPAM.getName().toLowerCase(Locale.ENGLISH) + ":" + UUID.randomUUID().toString();
    }

    private NodePamRequest createNodePamRequestBasedOnJobType(final String jobOwner,
                                                              final JobType jobType,
                                                              final List<JobProperty> jobProperties,
                                                              final long neJobId,
                                                              final String nodeName,
                                                              final Map<String, List<String>> nodeCredentialsFromFile,
                                                              final Long mainJobId) {
        final String requestId = generateNbiRequestId();
        NodePamRequest nodePamRequest = null;
        final String userId = jobOwner;
        final String muId = MUID_ONE; // LO DEVO RICAVARE DALLA REST ??

        LOGGER.debug("createNodePamRequestBasedOnJobType:: jobOwner={} jobType={} jobProperties={} neJobId={} nodeName={}", jobOwner, jobType, jobProperties, neJobId, nodeName);
        switch (jobType) {
            case ROTATE_NE_ACCOUNT_CREDENTIALS:
                nodePamRequest = new NodePamUpdatePasswordRequest(userId, requestId, nodeName, neJobId, muId, jobType, mainJobId);
                ((NodePamUpdatePasswordRequest) nodePamRequest).setNextUser(JobProperty.getPropertyValue(PK_NEXT_USERNAME, jobProperties));
                ((NodePamUpdatePasswordRequest) nodePamRequest).setNextPasswd(JobProperty.getPropertyValue(PK_NEXT_PASSWORD, jobProperties));
                break;
            case ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE:
                nodePamRequest = new NodePamUpdatePasswordRequest(userId, requestId, nodeName, neJobId, muId, jobType, mainJobId);

                //Retrieve credentials from imported file
                final List<String> credentials = nodeCredentialsFromFile.get(nodeName);
                if (credentials != null) {
                    ((NodePamUpdatePasswordRequest) nodePamRequest).setNextUser(credentials.get(0));
                    ((NodePamUpdatePasswordRequest) nodePamRequest).setNextPasswd(credentials.get(1));
                }
                break;
            case ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED:
                nodePamRequest = new NodePamAutogeneratePasswordRequest(userId, requestId, nodeName, neJobId, muId, jobType, mainJobId);
                break;
            case CREATE_NE_ACCOUNT:
                nodePamRequest = new NodePamEnableRemoteManagementRequest(userId, requestId, nodeName, neJobId, jobType, mainJobId);
                break;
            case DETACH_NE_ACCOUNT:
                nodePamRequest = new NodePamDisableRemoteManagementRequest(userId, requestId, nodeName, neJobId, jobType, mainJobId);
                break;
            case CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION:
                nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, nodeName, neJobId, jobType, mainJobId);
                break;
        }
        return nodePamRequest;
    }

    private Map<String, List<String>> retrieveUserCredentialsIfNecessary(final long jobId, final NPamJob job) {
        Map<String, List<String>> nodeCredentialsFromFile = null;
        if (job.getJobType() == ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE) {
            nodeCredentialsFromFile = fileResource.readCredentialsFromFile(fileResource.getFilenameAfterJob(job.getJobProperties()));
            LOGGER.info("retrieveUserCredentials:: recovered retrieveUserCredentials={} jobId={}", nodeCredentialsFromFile, jobId);
            if (nodeCredentialsFromFile.isEmpty()) {
                throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_CREDENTIALS_FILE_EMPTY, jobId);
            }
        }
        return nodeCredentialsFromFile;
    }

    private void sendNodeUpdateMessage(final NPamJob job, final String jobOwner, final long neJobId, final String nodeName, final Map<String, List<String>> nodeCredentialsFromFile)  {
        final NodePamRequest nodePamRequest = createNodePamRequestBasedOnJobType(jobOwner, job.getJobType(), job.getJobProperties(), neJobId, nodeName, nodeCredentialsFromFile, job.getJobId());
        sendNodeUpdateMessage(job.getJobType(), neJobId, nodePamRequest);
    }

    private void sendNodeUpdateMessageBatch(NPamJob job, String jobOwner, Map<String, List<String>> nodeCredentialsFromFile, Map<Long, String> createdNeJobs) {
        int batchMessageSent = 0;
        int totalMessageSent = 0;

        //Send message to execute NEJob(s)
        LOGGER.debug("NodePam::sendNodeUpdateMessageBatch:: START");
        for (final Map.Entry<Long, String> entry : createdNeJobs.entrySet()) {
            totalMessageSent++;

            final String nodeName = entry.getValue();
            final long neJobId = entry.getKey();

            if (totalMessageSent <= INITIAL_BATCH_SIZE) {
                //send fast initial burst of INITIAL_BATCH_SIZE
                sendNodeUpdateMessage(job, jobOwner, neJobId, nodeName, nodeCredentialsFromFile);
            } else {
                //then send slow burst of BATCH_SIZE
                batchMessageSent++;
                sendNodeUpdateMessage(job, jobOwner, neJobId, nodeName, nodeCredentialsFromFile);
                if (batchMessageSent >= BATCH_SIZE) {
                    batchMessageSent = 0;
                    LOGGER.debug("NodePam::sendNodeUpdateMessageBatch::WaitMechanism totalMessageSent={}, batchMessageSent={}", totalMessageSent, batchMessageSent);
                    waitFor(RATE_LIMIT);
                }
            }
        }
    }

    private void sendNodeUpdateMessage(final JobType jobType, final long neJobId, final NodePamRequest nodePamRequest) {
        if (nodePamRequest != null) {
            boolean retryToSend = true;
            int retryNumber = 0;

            do {
                try {
                    nodePamUpdateRequestQueueSender.sendJobExecutorMessage(nodePamRequest.getRequestId(), nodePamRequest);
                    retryToSend = false;
                    logIfRetry(retryNumber);
                } catch (final Exception e) {
                    retryNumber++;
                    String errorMessage = exceptionHelper.getRootCauseAndRewrap(e).getMessage();
                    if (isQueueFull(retryNumber, errorMessage)) {
                        LOGGER.debug("NodePam::sendNodeUpdateMessage::WaitMechanism send message={}, retry number={}, errorMessage={}", nodePamRequest, retryNumber, errorMessage);
                        waitForRetry(TIME_TO_RETRANSMIT,retryNumber);
                    } else {
                        retryToSend = false;
                        markNeJobAsFailed(nodePamRequest, retryNumber, errorMessage);
                    }
                }
            } while ( retryToSend );

        } else {
            LOGGER.debug("NodePam::sendNodeUpdateMessage:: NOTHING TO SEND (STRANGE) CAUSE invalid jobType={} for neJobId={}", jobType, neJobId);
        }
    }

    @SuppressWarnings({"squid:S3252"})
    private void markMainJobAsFailedLoggingException(final long jobId, final String errorDetails) {
        if (jobId > 0) {
            try {
                LOGGER.debug("markMainJobAsFailedLoggingException cause some error for jobId={}", jobId);
                jobConfigurationService.updateMainJobStateToCompleted(jobId, JobResult.FAILED.getJobResult(), errorDetails);
            }
            catch(final Exception ee) {LOGGER.info("markMainJobAsFailedLoggingException:: impossible to updateMainJobStateToCompleted due to exception e.getClass={}, e.getMessage={} ", ee.getClass(), ee.getMessage());}
        }
    }

    public Set<String> getAllNes(final NEInfo selectedNEInfo, final String owner) {
        return networkUtil.getAllNetworkElementFromNeInfo(selectedNEInfo, owner, true);
    }

    private void waitForRetry(final long millisecond, final int retryNumber) {
        waitFor(millisecond + RETRANSMISSION_DELAY * retryNumber);
    }

    private static boolean isQueueFull(int retryNumber, String errorMessage) {
        return (errorMessage != null) && errorMessage.contains("NodePamRequestQueue") && (retryNumber < MAX_RETRANSMISSIONS);
    }

    private static void logIfRetry(int retryNumber) {
        if (retryNumber >0 ) {
            LOGGER.info("NodePam::sendNodeUpdateMessage::RetryMessageMechanism={}", retryNumber);
        }
    }

    private void markNeJobAsFailed(NodePamRequest nodePamRequest, int retryNumber, String errorMessage) {
        if (retryNumber >= MAX_RETRANSMISSIONS ) {
            errorMessage = QUEUE_FULL;
        }
        LOGGER.info("NodePam::sendNodeUpdateMessage::RetryMechanism send message={}, retry number={}, errorMessage={}", nodePamRequest, retryNumber, errorMessage);
        jobConfigurationService.updateNeJobStateToCompleted(nodePamRequest.getNeJobId(), JobResult.FAILED.getJobResult(), errorMessage);
    }
}
