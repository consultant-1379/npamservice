package com.ericsson.oss.services.security.npam.ejb.job.mapper;

import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJobTemplate;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEJob;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType;
import com.ericsson.oss.services.security.npam.api.job.modelentities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.*;


@SuppressWarnings("unchecked")
public class JobMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobMapper.class);

    /*
    *        JobTemplate read phase
    *
    * */

    public NPamJobTemplate getJobTemplateDetails(final Map<String, Object> jobTemplateAttributes, final Long jobTemplateId) {
        final NPamJobTemplate jobTemplate = new NPamJobTemplate();

        final String jobName = (String) jobTemplateAttributes.get(JOB_NAME);
        jobTemplate.setName(jobName);

        final JobType jobType = JobType.valueOf((String) jobTemplateAttributes.get(JOB_TYPE));
        jobTemplate.setJobType(jobType);

        final String owner = (String) jobTemplateAttributes.get(OWNER);
        jobTemplate.setOwner(owner);

        final Date creationTime = (Date) jobTemplateAttributes.get(CREATION_TIME);
        jobTemplate.setCreationTime(creationTime);

        final String description = (String) jobTemplateAttributes.get(DESCRIPTION);
        jobTemplate.setDescription(description);

        final List<Map<String, Object>> jobPropertiesList = (List<Map<String, Object>>) jobTemplateAttributes.get(JOBPROPERTIES);
        jobTemplate.setJobProperties(getJobProperties(jobPropertiesList));

        final Map<String, Object> selectedNeInfoMap = (Map<String, Object>) jobTemplateAttributes.get(SELECTED_NES);
        jobTemplate.setSelectedNEs(getNEInfo(selectedNeInfoMap));

        final Map<String, Object> mainScheduleMap = (Map<String, Object>) jobTemplateAttributes.get(MAIN_SCHEDULE);
        jobTemplate.setMainSchedule(getSchedule(mainScheduleMap));

        jobTemplate.setJobTemplateId(jobTemplateId);

        LOGGER.info("populateNPamJobTemplate: Returning from mapper jobTemplate as {}", jobTemplate);
        return jobTemplate;
    }

    private List<JobProperty> getJobProperties(final List<Map<String, Object>> jobPropertiesList) {
        final List<JobProperty> jobProperties = new ArrayList<>();
        if (jobPropertiesList != null) {
            for (final Map<String, Object> jobPropertyMap : jobPropertiesList) {
                final String key = (String) jobPropertyMap.get(JobProperty.JOB_PROPERTY_KEY);
                final String value = (String) jobPropertyMap.get(JobProperty.JOB_PROPERTY_VALUE);
                final JobProperty jobProperty = new JobProperty(key, value);

                jobProperties.add(jobProperty);
            }
        }
        return jobProperties;
    }

    private Schedule getSchedule(final Map<String, Object> scheduleMap) {
        final Schedule schedule = new Schedule();
        if (scheduleMap != null) {
            final ExecMode execMode = ExecMode.valueOf((String) scheduleMap.get(EXEC_MODE));
            final List<Map<String, Object>> schedulePropertyList = (List<Map<String, Object>>) scheduleMap.get(SCHEDULE_ATTRIBUTES);
            final List<ScheduleProperty> scheduleAttributes = new ArrayList<>();
            if (schedulePropertyList != null) {
                for (final Map<String, Object> schedulePropertyMap : schedulePropertyList) {
                    final ScheduleProperty scheduleProperty = new ScheduleProperty();
                    scheduleProperty.setName((String) schedulePropertyMap.get(ScheduleProperty.SCHEDULE_PROPERTY_NAME));
                    scheduleProperty.setValue((String) schedulePropertyMap.get(ScheduleProperty.SCHEDULE_PROPERTY_VALUE));
                    scheduleAttributes.add(scheduleProperty);
                }
            }
            schedule.setExecMode(execMode);
            schedule.setScheduleAttributes(scheduleAttributes);
        }
        return schedule;
    }

    private NEInfo getNEInfo(final Map<String, Object> selectedNeInfoMap) {
        final NEInfo neInfo = new NEInfo();
        if (selectedNeInfoMap != null) {
            neInfo.setCollectionNames((List<String>) selectedNeInfoMap.get(NEInfo.NEINFO_COLLECTION_NAMES));
            neInfo.setSavedSearchIds((List<String>) selectedNeInfoMap.get(NEInfo.NEINFO_SAVED_SEARCH_IDS));
            neInfo.setNeNames((List<String>) selectedNeInfoMap.get(NEInfo.NEINFO_NE_NAMES));
        }
        LOGGER.debug("In JobMapper getNEInfo - neInfo {}", neInfo);
        return neInfo;
    }

    /*
     *        Job read phase
     *
     * */
    @SuppressWarnings({"squid:S3252"})
    public NPamJob getJobDetails(final Map<String, Object> jobAttributes, final Long jobId) {
        final NPamJob job = new NPamJob();

        JobState state = JobState.valueOf((String) jobAttributes.get(NPamJob.JOB_STATE));
        job.setState(state);

        double progressPercentage = (double) jobAttributes.get(NPamJob.JOB_PROGRESS_PERCENTAGE);
        job.setProgressPercentage(progressPercentage);

        JobResult result = null;
        if (jobAttributes.get(NPamJob.JOB_RESULT) != null) {
            result = JobResult.valueOf((String) jobAttributes.get(NPamJob.JOB_RESULT));
        }
        job.setResult(result);

        final Date startTime = (Date) jobAttributes.get(NPamJob.JOB_START_TIME);
        job.setStartTime(startTime);

        final Date endTime = (Date) jobAttributes.get(NPamJob.JOB_END_TIME);
        job.setEndTime(endTime);

        final List<Map<String, Object>> jobPropertiesList = (List<Map<String, Object>>) jobAttributes.get(NPamJob.JOB_PROPERTIES);
        job.setJobProperties(getJobProperties(jobPropertiesList));

        final Date creationTime = (Date) jobAttributes.get(NPamJob.JOB_CREATION_TIME);
        job.setCreationTime(creationTime);

        final JobType jobType = JobType.valueOf((String) jobAttributes.get(NPamJob.JOB_TYPE));
        job.setJobType(jobType);

        int executionIndex = (int) jobAttributes.get(NPamJob.JOB_EXECUTION_INDEX);
        job.setExecutionIndex(executionIndex);

        final Map<String, Object> selectedNeInfoMap = (Map<String, Object>) jobAttributes.get(NPamJob.JOB_SELECTED_NES);
        job.setSelectedNEs(getNEInfo(selectedNeInfoMap));

        long templateJobId = (long) jobAttributes.get(NPamJob.JOB_TEMPLATE_ID);
        job.setTemplateJobId(templateJobId);

        int numberOfNetworkElements = (int) jobAttributes.get(NPamJob.JOB_NUMBER_NETWORK_ELEMENTS);
        job.setNumberOfNetworkElements(numberOfNetworkElements);

        final Date scheduledTime = (Date) jobAttributes.get(NPamJob.JOB_SCHEDULED_TIME);
        job.setScheduledTime(scheduledTime);

        String errorDetails = (String) jobAttributes.get(NPamJob.JOB_ERROR_DETAILS);
        job.setErrorDetails(errorDetails);

        final String name =  (String) jobAttributes.get(NPamJob.JOB_NAME);
        job.setName(name);

        final String owner =  (String) jobAttributes.get(NPamJob.JOB_OWNER);
        job.setOwner(owner);

        job.setJobId(jobId);

        LOGGER.debug("Returning from mapper job as {}", job);
        return job;
    }

    /*
     *        NEJob read phase
     *
     * */
    @SuppressWarnings({"squid:S3252"})
    public NPamNEJob getNEJobDetails(final Map<String, Object> neJobAttributes, final Long neJobId) {
        final NPamNEJob neJob = new NPamNEJob();

        JobState state = JobState.valueOf((String) neJobAttributes.get(NPamNEJob.JOB_STATE));
        neJob.setState(state);

        JobResult result = null;
        if (neJobAttributes.get(NPamNEJob.JOB_RESULT) != null) {
            result = JobResult.valueOf((String) neJobAttributes.get(NPamNEJob.JOB_RESULT));
        }
        neJob.setResult(result);

        final Date startTime = (Date) neJobAttributes.get(NPamNEJob.JOB_START_TIME);
        neJob.setStartTime(startTime);

        final Date endTime = (Date) neJobAttributes.get(NPamNEJob.JOB_END_TIME);
        neJob.setEndTime(endTime);

        final Date creationTime = (Date) neJobAttributes.get(NPamNEJob.JOB_CREATION_TIME);
        neJob.setCreationTime(creationTime);

        String neName = (String) neJobAttributes.get(NPamNEJob.NEJOB_NE_NAME);
        neJob.setNeName(neName);

        long mainJobId = (long) neJobAttributes.get(NPamNEJob.NEJOB_MAIN_JOB_ID);
        neJob.setMainJobId(mainJobId);

        Step step = null;
        if (neJobAttributes.get(NPamNEJob.STEP) != null) {
            step = Step.valueOf((String) neJobAttributes.get(NPamNEJob.STEP));
        }
        neJob.setStep(step);

        String errorDetails = (String) neJobAttributes.get(NPamNEJob.JOB_ERROR_DETAILS);
        neJob.setErrorDetails(errorDetails);

        neJob.setNeJobId(neJobId);

        LOGGER.debug("Returning from mapper neJob as {}", neJob);
        return neJob;
    }
}
