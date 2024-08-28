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
package com.ericsson.oss.services.security.npam.ejb.job.executor;

import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.JOBPROPERTIES;
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.JOB_CREATE;
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_KEY;
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_VALUE;
import static com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants.PK_FILENAME;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.REPEAT_TYPE_VALUE_MONTHLY;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.REPEAT_TYPE_VALUE_WEEKLY;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.REPEAT_TYPE_VALUE_YEARLY;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_END_DATE;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_REPEAT_TYPE;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.INTERNAL_SERVER_ERROR_RENAME_FILE;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.security.npam.api.constants.ModelsConstants;
import com.ericsson.oss.services.security.npam.api.constants.NodePamConstantsGetter;
import com.ericsson.oss.services.security.npam.api.exceptions.JobConfigurationException;
import com.ericsson.oss.services.security.npam.api.exceptions.JobError;
import com.ericsson.oss.services.security.npam.api.exceptions.JobExceptionFactory;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage;
import com.ericsson.oss.services.security.npam.api.interfaces.JobConfigurationService;
import com.ericsson.oss.services.security.npam.api.interfaces.JobCreationService;
import com.ericsson.oss.services.security.npam.api.job.modelentities.ExecMode;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobState;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NEInfo;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJobTemplate;
import com.ericsson.oss.services.security.npam.api.job.modelentities.Schedule;
import com.ericsson.oss.services.security.npam.api.message.NodePamSubmitMainJobRequest;
import com.ericsson.oss.services.security.npam.ejb.job.mapper.JobMapper;
import com.ericsson.oss.services.security.npam.ejb.job.util.DateRecorderUtil;
import com.ericsson.oss.services.security.npam.ejb.job.util.JobScheduleValidator;
import com.ericsson.oss.services.security.npam.ejb.message.NodePamQueueMessageSender;

/**
 * Job executor to create Main job, NE jobs,Activity jobs and submit NE jobs in workflow service
 *
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class JobCreationServiceImpl implements JobCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobCreationServiceImpl.class);
    private static final int FIRST_EXECUTION_INDEX = 0;
    private static final long DEFAULT_MAIN_JOB_ID = -1;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private JobMapper jobMapper;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    JobExceptionFactory jobExceptionFactory;

    @Inject
    private NodePamQueueMessageSender nodePamQueueMessageSender;

    @Inject
    private NodePamConstantsGetter nodePamConstantsGetter;


    @Inject
    DateRecorderUtil dateRecorderUtil;

    @Inject
    JobScheduleValidator jobScheduleValidator;

    // THIS SHOULD BE CALLED FIRST TIME
    @Override
    public void createNewJob(final long jobTemplateId) {
        prepareMainJob(jobTemplateId, FIRST_EXECUTION_INDEX);
    }

    // THIS SHOULD BE CALLED AFTER FIRST CALL (END PHASE) with new lastExecutionIndex
    @Override
    public void createNewJobIfNecessary(final long jobTemplateId, final int lastExecutionIndex) {
        prepareMainJob(jobTemplateId, lastExecutionIndex);
    }

    @Override
    public long createNewJobTemplate(final Map<String, Object> jobTemplateAttributes) {
        return  jobConfigurationService.createPO(ModelsConstants.NAMESPACE, ModelsConstants.NPAM_JOBTEMPLATE,
                ModelsConstants.VERSION, jobTemplateAttributes);
    }

    @Override
    public List<Long> getJobTemplatePoIdsByName(final String jobName) {
        return jobConfigurationService.getJobTemplatePoIdsByName(jobName);
    }

    @Override
    public NPamJobTemplate getJobTemplateByName(final String jobName) {
        return jobConfigurationService.getJobTemplateByName(jobName);
    }

    @Override
    public void updateImportedFilename(final long jobTemplateId, final String filenameFrom) {
        final File fromFile = new File(filenameFrom);
        final Path path = Paths.get(filenameFrom);
        final Path fileName = path.getFileName();
        final String toFilename = fileName.toString() + "_" + jobTemplateId;
        final StringBuilder toBuilder = new StringBuilder();
        final String filenameTo = toBuilder.append(nodePamConstantsGetter.getImportFolderJob()).append(toFilename).toString();
        final File toFile = new File(filenameTo);
        final boolean resultFlag = fromFile.renameTo(toFile);
        if (!resultFlag) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_NFS_RWISSUE, INTERNAL_SERVER_ERROR_RENAME_FILE);
        }
        final Map<String, Object> jobTemplate = jobConfigurationService.retrieveJobTemplate(jobTemplateId);
        final List<Map<String, Object>> jobProperties = (List<Map<String, Object>>) jobTemplate.get(JOBPROPERTIES);
        for (final Map<String, Object> jobProperty : jobProperties) {
            if (PK_FILENAME.equals(jobProperty.get(PROPERTY_KEY))) {
                jobProperty.put(PROPERTY_VALUE, toFilename);
                break;
            }
        }

        jobConfigurationService.updateJobAttributes(jobTemplateId, jobTemplate);
    }


    @SuppressWarnings({"squid:S2139","squid:S112"})
    private void prepareMainJob(final long jobTemplateId, final int lastExecutionIndex) {
        //1 validate input parameters
        //2 check if to be stopped
        // calculate nextScheduledTime
        //3 create NeJob PO referring main Job (createNEJobs method)
        final int nextExecutionIndex = lastExecutionIndex + 1;

        LOGGER.info("prepareMainJob:: Prepare job with jobTemplateId={}, nextExecutionIndex={}", jobTemplateId, nextExecutionIndex);
        long poId = DEFAULT_MAIN_JOB_ID;
        try {
            final Map<String, Object> jobTemplateAttributes = jobConfigurationService.retrieveJobTemplate(jobTemplateId);
            if (jobTemplateAttributes == null || jobTemplateAttributes.isEmpty()) {
                LOGGER.error("prepareMainJob:: Unable to retrieve job template from db for the jobTemplateId={}", jobTemplateId);
                throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_NOT_FOUND_TEMPLATE_ID, jobTemplateId);
            }

            LOGGER.info("prepareMainJob:: Recovered jobTemplateAttributes={}", jobTemplateAttributes);
            final NPamJobTemplate jobTemplate = jobMapper.getJobTemplateDetails(jobTemplateAttributes, jobTemplateId);
            LOGGER.info("prepareMainJob:: Remapped jobTemplateAttributes={} to jobTemplate={}", jobTemplateAttributes ,jobTemplate);

            final Schedule schedule = jobTemplate.getMainSchedule();
            jobScheduleValidator.validateMainScheduleParameters(schedule);

            if (!haveToCreateNewMainJob(jobTemplate, nextExecutionIndex)) {
                LOGGER.info("prepareMainJob:: NOT NECESSARY TO CREATE A NEW JOB FOR jobTemplateId={} CAUSE LOOP TERMINATED so return", jobTemplateId);
                return;
            }

            final Date nextScheduledTime = calculateNextScheduledTime(schedule, nextExecutionIndex);
            if (nextScheduledDateIsOverEndDate(schedule, nextScheduledTime, nextExecutionIndex)) {
                LOGGER.info("prepareMainJob:: NOT NECESSARY TO CREATE A NEW JOB FOR jobTemplateId={} CAUSE nextScheduledTime WOULD BE EXPIRED so return", jobTemplateId);
                return;
            }
            poId = createJobAndReturnPoId(jobTemplate, nextExecutionIndex, nextScheduledTime);
            LOGGER.info("prepareMainJob: create a new Job jobId={} for jobTemplateId={}", poId, jobTemplateId);
        } catch (final JobConfigurationException e) {
            LOGGER.error("prepareMainJob:: NPamConfigurationException occurred during main job creation having jobTemplateId={} message={} so relaunch it", jobTemplateId, e.getMessage());
            throw e;
        } catch (final Exception e) {
            LOGGER.error("prepareMainJob:: Exception occurred during main job creation having jobTemplateId={} message={} so relaunch it", jobTemplateId, e.getMessage());
            throw e;
        }
    }

    //This method should stop when necessary
    // loop > loopCount
    // schedule expired
    @SuppressWarnings({"squid:S3776"})
    private boolean haveToCreateNewMainJob(final NPamJobTemplate jobTemplate, final int nextExecutionIndex) {
        final Schedule schedule = jobTemplate.getMainSchedule();
        if (schedule != null && schedule.getExecMode() != null) {
            switch (schedule.getExecMode()) {
                case IMMEDIATE:
                    if (nextExecutionIndex > 1) {
                        LOGGER.info("haveToCreateNewMainJob:: CASE IMMEDIATE:: jobTemplate={} stopped cause Scheduled immediate and nextExecutionIndex={}", jobTemplate, nextExecutionIndex);
                        return false;
                    }
                    break;
                case SCHEDULED:
                    if (schedule.checkIsPeriodic()) {
                        if (occurrenceExceeded(schedule, nextExecutionIndex)) {
                            LOGGER.info("haveToCreateNewMainJob:: CASE SCHEDULED periodic:: jobTemplate={} stopped cause Scheduled periodic and occurrenceExceeded=true", jobTemplate);
                            return false;
                        }
                        if (endDateIsExpired(schedule, nextExecutionIndex)) {
                            LOGGER.info("haveToCreateNewMainJob:: CASE SCHEDULED periodic:: jobTemplate={} stopped cause Scheduled periodic and endDateIsExpired=true", jobTemplate);
                            return false;
                        }
                    } else { //not periodic
                        if (nextExecutionIndex > 1) {
                            LOGGER.info("haveToCreateNewMainJob:: CASE SCHEDULED NON periodic:: jobTemplate={} stopped cause Scheduled not periodic and nextExecutionIndex={}", jobTemplate, nextExecutionIndex);
                            return false;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        LOGGER.info("haveToCreateNewMainJob::  jobTemplate={} nextExecutionIndex={} return true", jobTemplate, nextExecutionIndex);
        return true;
    }

    private long createJobAndReturnPoId(final NPamJobTemplate jobTemplate,
                                        final int jobExecutionIndex,
                                        final Date nextScheduledTime) {
        final long jobTemplateId = jobTemplate.getJobTemplateId();
        final Map<String, Object> jobAttributes = populateJobAttributes(jobTemplate, jobExecutionIndex, nextScheduledTime);
        final long jobId = jobConfigurationService.createPO(ModelsConstants.NAMESPACE, ModelsConstants.NPAM_JOB, ModelsConstants.VERSION, jobAttributes);
        systemRecorder.recordEvent(JOB_CREATE, EventLevel.COARSE, "",  "JOB",
                "Main job is created with Id: " + jobId + " with jobTemplateId: " + jobTemplateId);

        sendToExecutionQueueIfImmediateJob(jobTemplate, jobId);

        return jobId;
    }

    private void sendToExecutionQueueIfImmediateJob(final NPamJobTemplate jobTemplate, final long jobId) {
        LOGGER.info("sendToExecutionQueueIfImmediateJob:: jobId={} jobTemplate={}", jobId, jobTemplate);

        final Schedule schedule = jobTemplate.getMainSchedule();
        if (schedule != null && schedule.getExecMode() != null) {
            final ExecMode execMode = schedule.getExecMode();
            if (ExecMode.IMMEDIATE.equals(execMode)) {
                //SEND TO QUEUE TO BE DONE
                //HERE IS State=SUBMITTED set previously inside populateJobAttributes
                LOGGER.info("sendToExecutionQueueIfImmediateJob:: send to execution queue cause IMMEDIATE jobId={} jobTemplate={}", jobId, jobTemplate);
                final NodePamSubmitMainJobRequest nodePamSubmitMainJob = new NodePamSubmitMainJobRequest(jobId);
                nodePamQueueMessageSender.sendSubmitMainJobMessage(nodePamSubmitMainJob);
            }
        }
    }

    @SuppressWarnings({"squid:S3252"})
    private Map<String, Object> populateJobAttributes(final NPamJobTemplate jobTemplate, final int jobExecutionIndex, final Date nextScheduledTime) {
        final Map<String, Object> jobAttributes = new HashMap<>();

        jobAttributes.put(NPamJob.JOB_TYPE, jobTemplate.getJobType().getJobTypeName());
        jobAttributes.put(NPamJob.JOB_EXECUTION_INDEX, jobExecutionIndex);
        jobAttributes.put(NPamJob.JOB_TEMPLATE_ID, jobTemplate.getJobTemplateId());
        jobAttributes.put(NPamJob.JOB_SELECTED_NES, prepareSelectedNeInfoMap(jobTemplate));
        jobAttributes.put(NPamJob.JOB_STATE, JobState.CREATED.getJobStateName());
        jobAttributes.put(NPamJob.JOB_CREATION_TIME, new Date());
        jobAttributes.put( NPamJob.JOB_ERROR_DETAILS , null);

        jobAttributes.put(NPamJob.JOB_PROPERTIES, prepareJobProperties(jobTemplate));

        // SET SCHEDULE TIME
        jobAttributes.put( NPamJob.JOB_SCHEDULED_TIME , nextScheduledTime);

        //SET AS SCHEDULED TIME
        jobAttributes.put(NPamJob.JOB_START_TIME, nextScheduledTime);

        final Schedule schedule = jobTemplate.getMainSchedule();
        if (schedule != null && schedule.getExecMode() != null) {
            final ExecMode execMode = schedule.getExecMode();
            if (ExecMode.SCHEDULED.equals(execMode)) {
                jobAttributes.put(NPamJob.JOB_STATE, JobState.SCHEDULED.getJobStateName());
            }
            if (ExecMode.IMMEDIATE.equals(execMode)) {
                jobAttributes.put(NPamJob.JOB_STATE, JobState.SUBMITTED.getJobStateName());
            }
        }

        jobAttributes.put( NPamJob.JOB_NAME , jobTemplate.getName());

        jobAttributes.put( NPamJob.JOB_OWNER , jobTemplate.getOwner());

        return jobAttributes;
    }

    private Map<String, Object> prepareSelectedNeInfoMap(final NPamJobTemplate jobTemplate) {
        final Map<String, Object> selectedNEsMap = new HashMap<>();
        final NEInfo neInfo = jobTemplate.getSelectedNEs();
        if (neInfo != null) {
            selectedNEsMap.put(NEInfo.NEINFO_NE_NAMES, neInfo.getNeNames());
            selectedNEsMap.put(NEInfo.NEINFO_COLLECTION_NAMES, neInfo.getCollectionNames());
            selectedNEsMap.put(NEInfo.NEINFO_SAVED_SEARCH_IDS, neInfo.getSavedSearchIds());
        }
        return selectedNEsMap;
    }

    private boolean occurrenceExceeded(final Schedule schedule, final int nextExecutionIndex) {
        final int occurencesValue = jobScheduleValidator.getAndValidateOptionalOccurrence(schedule);
        if ((occurencesValue > 0) && (nextExecutionIndex > occurencesValue)) {
            LOGGER.info(
                    "occurrenceExceeded:: CASE SCHEDULED periodic:: nextExecutionIndex={} occurencesValue={} return false cause reached maximum number of executions ",
                    nextExecutionIndex, occurencesValue);
            return true;
        }
        return false;
    }

    private boolean endDateIsExpired(final Schedule schedule, final int nextExecutionIndex) {
        if (nextExecutionIndex > 1 && schedule.getScheduledAttributeValue(SA_END_DATE) != null) {
            // check if END_DATE is expired
            final Date endDate = jobScheduleValidator.getAndValidateEndDate(schedule);
            final DateTime endDateTime = new DateTime(endDate);
            if (!endDateTime.isAfterNow()) {
                LOGGER.info("endDateIsExpired:: CASE SCHEDULED periodic:: schedule={} nextExecutionIndex={} endDate={} return false cause reached end time ", schedule, nextExecutionIndex, endDate);
                return true;
            }
        }
        return false;
    }

    private boolean nextScheduledDateIsOverEndDate(final Schedule schedule, final Date nextScheduledTime, final int lastExecutionIndex) {
        if (lastExecutionIndex > 1 && schedule.getScheduledAttributeValue(SA_END_DATE) != null) {
            final Date endDate = jobScheduleValidator.getAndValidateEndDate(schedule);
            // check if nextScheduledTime is expired
            if (nextScheduledTime.getTime() > endDate.getTime()) {
                LOGGER.info("nextScheduledDateIsOverEndDate:: CASE SCHEDULED periodic, with endDate:: schedule={}, nextScheduledTime={}, lastExecutionIndex={}  return false cause reached end time ", schedule, nextScheduledTime, lastExecutionIndex);
                return true;
            }
        }
        return false;
    }

    /* TIme Check*/

    private Date calculateNextScheduledTime(final Schedule schedule, final int nextExecutionIndex) {
        switch (schedule.getExecMode()) {
            case IMMEDIATE:
                LOGGER.info("calculateNextScheduledTime:: Final Next Scheduled Time : immediate");
                final Date currentDate = new Date();
                dateRecorderUtil.logDate(currentDate);
                return currentDate;

            case SCHEDULED:
                final Date startDate = jobScheduleValidator.getAndValidateStartDate(schedule);
                if (!schedule.checkIsPeriodic()) {
                    //we always return the startDate provided by job template
                    dateRecorderUtil.logDate(startDate);
                    return startDate;
                } else { //isPeriodic
                    final String repeatType = schedule.getScheduledAttributeValue(SA_REPEAT_TYPE);
                    final int repeatCount = jobScheduleValidator.getAndValidateRepeatCount(schedule);
                    final String[] repeatOn = jobScheduleValidator.getAndValidateRepeatOn(schedule);

                    //if is the first execution return the startDate provided by job template
                    if (nextExecutionIndex == 1) {
                        dateRecorderUtil.logDate(startDate);
                        return startDate;
                    }

                    //scheduled periodically calculate the next startDate
                    DateTime nextScheduledDateTime = new DateTime(startDate);
                    dateRecorderUtil.logDate(nextScheduledDateTime);
                    if (!nextScheduledDateTime.isAfterNow()) {
                        LOGGER.info("calculateNextScheduledTime:: Start time is falling before current time, so checking for nextScheduledDateTime");
                        do {
                            nextScheduledDateTime = computeNextDateFromBaseDate(nextScheduledDateTime, repeatType, repeatOn, repeatCount);
                            dateRecorderUtil.logDate(nextScheduledDateTime);
                            LOGGER.info("calculateNextScheduledTime:: NextScheduledTime: {}, Is NextScheduledTime after current time: {}", nextScheduledDateTime, nextScheduledDateTime.isAfterNow());
                        } while (!nextScheduledDateTime.isAfterNow());
                    }
                    LOGGER.info("calculateNextScheduledTime:: Final Next Scheduled Time : {}", nextScheduledDateTime);
                    return nextScheduledDateTime.toDate();
                }
            default:
                LOGGER.info("calculateNextScheduledTime:: Wrong schedule execMode : {}", schedule.getExecMode());
                throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_EXEC_MODE, schedule.getExecMode());
        }
    }

    private DateTime computeNextDateFromBaseDate(final DateTime baseDate, final String repeatType, final String[] repeatOn, final int repeatCount) {
        DateTime nextScheduledTime = null;
        final int[] repeatOnValues = new int[10];
        if(repeatType == null) {
            LOGGER.info("calculateNextScheduledTime:: Wrong schedule repeatType : {}", repeatType);
            throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_REPEAT_TYPE, repeatType);
        }
        switch (repeatType) {
            case REPEAT_TYPE_VALUE_WEEKLY:
                if (repeatOn == null) {
                    nextScheduledTime = baseDate.plusWeeks(repeatCount);
                    LOGGER.debug("computeNextDateFromBaseDate:: Next run: {}", nextScheduledTime);
                    return nextScheduledTime;
                }

                LOGGER.debug("computeNextDateFromBaseDate:: Setting weekly type scheduling interval");
                int repeatInterval = 0;
                repeatInterval = calculateRepeatOnInterval(repeatOnValues, repeatOn, baseDate);
                LOGGER.debug("repeat interval: {}" ,repeatInterval);

                final int currentDay = baseDate.getDayOfWeek();
                if (currentDay + repeatInterval > 6) {
                    repeatInterval = repeatInterval - 7;
                    nextScheduledTime = baseDate.plusWeeks(repeatCount).plusDays(repeatInterval);
                } else {
                    nextScheduledTime = baseDate.plusDays(repeatInterval);
                }
                LOGGER.debug("computeNextDateFromBaseDate:: Next run: {}", nextScheduledTime);
                break;

            case REPEAT_TYPE_VALUE_MONTHLY:
                LOGGER.debug("computeNextDateFromBaseDate:: Setting monthly type scheduling interval");
                nextScheduledTime = baseDate.plusMonths(repeatCount);
                break;

            case REPEAT_TYPE_VALUE_YEARLY:
                LOGGER.debug("computeNextDateFromBaseDate:: Setting yearly type scheduling interval");
                nextScheduledTime = baseDate.plusYears(repeatCount);
                break;
            default:
                LOGGER.info("calculateNextScheduledTime:: Wrong schedule repeatType : {}", repeatType);
                throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_REPEAT_TYPE, repeatType);
        }
        LOGGER.info("computeNextDateFromBaseDate:: Next Scheduled Time is: {}", nextScheduledTime);
        return nextScheduledTime;
    }

    private static int calculateRepeatOnInterval(final int[] repeatOnValues, final String[] repeatOn, final DateTime baseDate) {
        for (int i = 0; i < repeatOn.length; i++) {
            if (Integer.parseInt(repeatOn[i]) == 7) {
                repeatOnValues[i] = 0;
            } else {
                repeatOnValues[i] = Integer.parseInt(repeatOn[i]);
            }
        }
        int currentDay = baseDate.getDayOfWeek();
        if (currentDay == 7) {
            currentDay = 0;
        }
        Arrays.sort(repeatOnValues);
        int repeatInterval = 0;
        for (final int repeatOnValue : repeatOnValues) {
            if (currentDay < repeatOnValue) {
                repeatInterval = repeatOnValue - currentDay;
                break;
            }
        }
        if (repeatInterval == 0) {
            repeatInterval = 7 - (currentDay - repeatOnValues[10 - repeatOn.length]);
        }
        return repeatInterval;
    }

    /**
     * Copy properties from template to job
     *
     * @param jobTemplate
     * @return
     */
    private List<Map<String, String>> prepareJobProperties(final NPamJobTemplate jobTemplate) {
        final List<Map<String, String>> jobProperties = new ArrayList<>();
        for (final JobProperty jobProperty:jobTemplate.getJobProperties()) {
            jobProperties.add(prepareJobProperty(jobProperty.getKey(), jobProperty.getValue()));
        }
        return jobProperties;
    }

    private Map<String, String> prepareJobProperty(final String propertyName, final String propertyValue) {
        final Map<String, String> property = new HashMap<>();
        property.put(JobProperty.JOB_PROPERTY_KEY, propertyName);
        property.put(JobProperty.JOB_PROPERTY_VALUE, propertyValue);
        return property;
    }

}
