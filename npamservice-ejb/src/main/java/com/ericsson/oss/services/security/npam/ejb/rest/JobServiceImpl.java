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
package com.ericsson.oss.services.security.npam.ejb.rest;

import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.CREATION_TIME;
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.DESCRIPTION;
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.EXEC_MODE;
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.JOBPROPERTIES;
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.JOB_NAME;
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.JOB_TEMPLATE_CREATE;
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.JOB_TYPE;
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.MAIN_SCHEDULE;
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.OWNER;
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.SCHEDULE_ATTRIBUTES;
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.SELECTED_NES;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.CREATE_ACTION;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.DELETE_ACTION;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.EXECUTE_ACTION;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_NEACCOUNT_IMPORT_RESOURCE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_NEACCOUNT_JOB_RESOURCE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.QUERY_ACTION;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.READ_ACTION;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.context.ContextService;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.annotation.Authorize;
import com.ericsson.oss.services.security.npam.api.cal.CALConstants;
import com.ericsson.oss.services.security.npam.api.cal.CALDetailResultJSON;
import com.ericsson.oss.services.security.npam.api.cal.CALRecorderDTO;
import com.ericsson.oss.services.security.npam.api.exceptions.JobConfigurationException;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage;
import com.ericsson.oss.services.security.npam.api.interfaces.JobCreationService;
import com.ericsson.oss.services.security.npam.api.interfaces.JobExecutionService;
import com.ericsson.oss.services.security.npam.api.interfaces.JobGetService;
import com.ericsson.oss.services.security.npam.api.interfaces.JobImportService;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NEInfo;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJobTemplate;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEJob;
import com.ericsson.oss.services.security.npam.api.job.modelentities.Schedule;
import com.ericsson.oss.services.security.npam.api.job.modelentities.ScheduleProperty;
import com.ericsson.oss.services.security.npam.api.rest.JobService;
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus;
import com.ericsson.oss.services.security.npam.ejb.job.util.FileResource;
import com.ericsson.oss.services.security.npam.ejb.job.util.JobTemplateValidator;
import com.ericsson.oss.services.security.npam.ejb.services.NodePamEncryptionManager;
import com.ericsson.oss.services.security.npam.ejb.utility.NetworkUtil;

public class JobServiceImpl implements JobService {
    private static final String X_TOR_USER_ID = "X-Tor-UserID";

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private JobCreationService jobCreationService;

    @Inject
    JobExecutionService jobExecutionService;

    @Inject
    private NodePamConfigStatus nodePamConfigStatus;

    @Inject
    private ContextService ctx;

    @Inject
    private FileResource fileResource;

    @Inject
    private JobGetService jobGetService;

    @Inject
    private JobImportService jobImportService;

    @Inject
    NetworkUtil networkUtil;

    @Inject
    JobTemplateValidator jobTemplateValidator;

    @Inject
    NodePamEncryptionManager nodePamEncryptionManager;

    @Inject
    CALRecorderDTO cALRecorderDTO;

    public static final String NPAM_FEATURE_DISABLED = "NPAM feature is disabled";

    @Override
    @Authorize(resource = NPAM_NEACCOUNT_JOB_RESOURCE, action = CREATE_ACTION)
    public long createJobTemplate(final NPamJobTemplate nPAMJobTemplate) {
        if (!nodePamConfigStatus.isEnabled()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.FORBIDDEN_FEATURE_DISABLED);
        }
        return createJobTemplateAndReturnPoId(nPAMJobTemplate);
    }

    @Override
    public void createNewJob(final long jobTemplateId) {
        try {
            jobCreationService.createNewJob(jobTemplateId);
        } catch (final JobConfigurationException ex) {
            throw NPamRestErrorMessage.buildFromJobConfigurationException(ex);
        }
    }

    @Override
    @Authorize(resource = NPAM_NEACCOUNT_JOB_RESOURCE, action = READ_ACTION)
    public List<NPamJob> getMainJobs(final String jobName) {

        if (!nodePamConfigStatus.isEnabled()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.FORBIDDEN_FEATURE_DISABLED);
        }
        return jobGetService.getMainJobs(jobName);
    }

    @Override
    @Authorize(resource = NPAM_NEACCOUNT_IMPORT_RESOURCE, action = EXECUTE_ACTION)
    public void updalodInputFileFromContent(final String bodyAsString, final String fileName, final Boolean overwrite) {
        if (!nodePamConfigStatus.isEnabled()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.FORBIDDEN_FEATURE_DISABLED);
        }
        jobImportService.updalodInputFileFromContent(bodyAsString, fileName, overwrite);
    }

    @Override
    @Authorize(resource = NPAM_NEACCOUNT_JOB_RESOURCE, action = READ_ACTION)
    public List<NPamNEJob> getNeJobForJobId(final long jobId) {
        return jobGetService.getNeJobForJobId(jobId);
    }

    @Override
    @Authorize(resource = NPAM_NEACCOUNT_IMPORT_RESOURCE, action = QUERY_ACTION)
    public String[] importedJobFilelist() {
        if (!nodePamConfigStatus.isEnabled()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.FORBIDDEN_FEATURE_DISABLED);
        }
        return jobImportService.importedJobFilelist();
    }

    @Override
    @Authorize(resource = NPAM_NEACCOUNT_JOB_RESOURCE, action = DELETE_ACTION)
    public void deleteJob(final String jobName) {
        if (!nodePamConfigStatus.isEnabled()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.FORBIDDEN_FEATURE_DISABLED);
        }
        if (systemRecorder.isCompactAuditEnabled()) {
            final CALDetailResultJSON cALDetailResultJSON = new CALDetailResultJSON(CALConstants.DELETE, jobName);
            cALRecorderDTO.setDetailResult(Arrays.asList(cALDetailResultJSON));
        }
        jobExecutionService.cancelScheduledMainJob(jobName);
    }

    @Override
    @Authorize(resource = NPAM_NEACCOUNT_JOB_RESOURCE, action = READ_ACTION)
    public NPamJobTemplate getJobTemplate(final String jobName) {
        if (!nodePamConfigStatus.isEnabled()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.FORBIDDEN_FEATURE_DISABLED);
        }
        return jobCreationService.getJobTemplateByName(jobName);
    }

    private long createJobTemplateAndReturnPoId(final NPamJobTemplate nPAMJobTemplate) {
        validateJobTemplate(nPAMJobTemplate);

        if (nPAMJobTemplate.getJobType() == JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE) {
            populateSelectedNesFromFile(nPAMJobTemplate);
        }
        final Map<String, Object> jobTemplateAttributes = populateJobTemplateAttributes(nPAMJobTemplate);
        final List<Long> poIds = jobCreationService.getJobTemplatePoIdsByName(nPAMJobTemplate.getName());
        if (poIds != null && !poIds.isEmpty()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_JOB_NAME_ALREADY_PRESENT, nPAMJobTemplate.getName());
        }
        final long jobTemplateId = jobCreationService.createNewJobTemplate(jobTemplateAttributes);

        if (nPAMJobTemplate.getJobType() == JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE) {
            //update jobTemplate properties with new filename
            jobCreationService.updateImportedFilename(jobTemplateId, fileResource.getFilenameBeforeJob(nPAMJobTemplate.getJobProperties()));
        }
        systemRecorder.recordEvent(JOB_TEMPLATE_CREATE, EventLevel.COARSE, "", "JOB", "job template is created with Id: " + jobTemplateId);
        return jobTemplateId;
    }

    private void populateSelectedNesFromFile(final NPamJobTemplate nPAMJobTemplate) {
        final String filename = fileResource.getFilenameBeforeJob(nPAMJobTemplate.getJobProperties());
        final List<String> importedFileTargets = retrieveTargetsFromFile(filename);

        if (importedFileTargets.isEmpty()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_FILE_NOT_CONTAIN_ANY_NETWORK_ELEMENT, filename);
        }
        nPAMJobTemplate.getSelectedNEs().getNeNames().addAll(importedFileTargets);
    }

    private void validateJobTemplate(final NPamJobTemplate nPAMJobTemplate) {

        jobTemplateValidator.validateJobTemplateName(nPAMJobTemplate);

        switch (nPAMJobTemplate.getJobType()) {
            case ROTATE_NE_ACCOUNT_CREDENTIALS:
                jobTemplateValidator.validateMainScheduleNotPeriodic(nPAMJobTemplate);
                jobTemplateValidator.validateMainScheduleImmediateOrDeferred(nPAMJobTemplate);
                jobTemplateValidator.validatePropertyValue(nPAMJobTemplate.getJobProperties(), Arrays.asList("PASSWORD"), Arrays.asList("USERNAME",CALConstants.CLIENT_IP_ADDRESS,CALConstants.CLIENT_SESSION_ID));
                jobTemplateValidator.validateCredentials(nPAMJobTemplate.getJobProperties());
                jobTemplateValidator.validateSelectedNEsToBeNotEmpty(nPAMJobTemplate);
                networkUtil.validateNeInfo(nPAMJobTemplate.getSelectedNEs(), ctx.getContextValue(X_TOR_USER_ID));
                break;
            case ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED: {
                jobTemplateValidator.validateMainScheduleParameters(nPAMJobTemplate.getMainSchedule());
                jobTemplateValidator.validatePropertyValue(nPAMJobTemplate.getJobProperties(), new ArrayList<>(), Arrays.asList(CALConstants.CLIENT_IP_ADDRESS,CALConstants.CLIENT_SESSION_ID));
                jobTemplateValidator.validateSelectedNEsToBeNotEmpty(nPAMJobTemplate);
                networkUtil.validateNeInfo(nPAMJobTemplate.getSelectedNEs(), ctx.getContextValue(X_TOR_USER_ID));
                break;
            }
            case ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE:
                jobTemplateValidator.validateMainScheduleNotPeriodic(nPAMJobTemplate);
                jobTemplateValidator.validateMainScheduleImmediateOrDeferred(nPAMJobTemplate);
                jobTemplateValidator.validatePropertyValue(nPAMJobTemplate.getJobProperties(), Arrays.asList("FILENAME"), Arrays.asList(CALConstants.CLIENT_IP_ADDRESS,CALConstants.CLIENT_SESSION_ID));
                jobTemplateValidator.validateFileName(nPAMJobTemplate.getJobProperties());
                jobTemplateValidator.validateSelectedNEsToBeEmpty(nPAMJobTemplate);
                break;
            case CREATE_NE_ACCOUNT:
            case DETACH_NE_ACCOUNT:
            case CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION:
                if (nPAMJobTemplate.getJobType().equals(JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION)) {
                    jobTemplateValidator.validateMainScheduleImmediate(nPAMJobTemplate);
                }
                jobTemplateValidator.validateMainScheduleNotPeriodic(nPAMJobTemplate);
                jobTemplateValidator.validateMainScheduleImmediateOrDeferred(nPAMJobTemplate);
                jobTemplateValidator.validatePropertyValue(nPAMJobTemplate.getJobProperties(), new ArrayList<>(), Arrays.asList(CALConstants.CLIENT_IP_ADDRESS,CALConstants.CLIENT_SESSION_ID));
                jobTemplateValidator.validateSelectedNEsToBeNotEmpty(nPAMJobTemplate);
                networkUtil.validateNeInfo(nPAMJobTemplate.getSelectedNEs(), ctx.getContextValue(X_TOR_USER_ID));
                break;
            default:
                throw new NPAMRestErrorException(NPamRestErrorMessage.BAD_REQUEST_INVALID_JOB_TYPE, nPAMJobTemplate.getJobType().getJobTypeName());
        }
    }

    private List<String> retrieveTargetsFromFile(final String fileName) {
        return fileResource.readTargetsFromFile(fileName);
    }

    private Map<String, Object> populateJobTemplateAttributes(final NPamJobTemplate nPAMJobTemplate) {
        final Map<String, Object> jobTemplateAttributes = new HashMap<>();

        jobTemplateAttributes.put(JOB_NAME, nPAMJobTemplate.getName());

        jobTemplateAttributes.put(JOB_TYPE, nPAMJobTemplate.getJobType().getJobTypeName());

        jobTemplateAttributes.put(OWNER, ctx.getContextValue(X_TOR_USER_ID));

        jobTemplateAttributes.put(CREATION_TIME, new Date());

        jobTemplateAttributes.put(DESCRIPTION, nPAMJobTemplate.getDescription());

        jobTemplateAttributes.put(JOBPROPERTIES, prepareJobProperties(nPAMJobTemplate.getJobProperties()));
        jobTemplateAttributes.put(SELECTED_NES, prepareSelectedNeInfo(nPAMJobTemplate.getSelectedNEs()));
        jobTemplateAttributes.put(MAIN_SCHEDULE, prepareMainSchedule(nPAMJobTemplate.getMainSchedule()));

        return jobTemplateAttributes;
    }

    private List<Map<String, String>> prepareJobProperties(final List<JobProperty> jobPropertiesJAXB) {
        final List<Map<String, String>> jobProperties = new ArrayList<>();
        for (final JobProperty jobProperty : jobPropertiesJAXB) {
            jobProperties.add(prepareJobProperty(jobProperty.getKey(), jobProperty.getValue()));
        }
        return jobProperties;
    }

    private Map<String, String> prepareJobProperty(final String propertyName, final String propertyValue) {
        final Map<String, String> property = new HashMap<>();
        property.put(JobProperty.JOB_PROPERTY_KEY, propertyName);
        if (propertyName.equals("PASSWORD") || propertyName.equals("USERNAME") || propertyName.equals(CALConstants.CLIENT_IP_ADDRESS) || propertyName.equals(CALConstants.CLIENT_SESSION_ID)) {
            try {
                property.put(JobProperty.JOB_PROPERTY_VALUE, nodePamEncryptionManager.encryptPassword(propertyValue));
            } catch (final UnsupportedEncodingException e) {
                throw new NPAMRestErrorException(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_DECRYPT, propertyValue);
            }
        } else {
            property.put(JobProperty.JOB_PROPERTY_VALUE, propertyValue);
        }
        return property;
    }

    private Map<String, Object> prepareSelectedNeInfo(final NEInfo neInfoJAXB) {
        final Map<String, Object> selectedNEsMap = new HashMap<>();

        final NEInfo neInfo = new NEInfo();

        final List<String> neNames = new ArrayList<>();
        for (final String node : neInfoJAXB.getNeNames()) {
            neNames.add(node);
        }
        neInfo.setNeNames(neNames);

        final List<String> neCollections = new ArrayList<>();
        for (final String collection : neInfoJAXB.getCollectionNames()) {
            neCollections.add(collection);
        }
        neInfo.setCollectionNames(neCollections);

        final List<String> neSavedSearchIds = new ArrayList<>();
        for (final String neSavedSearchId : neInfoJAXB.getSavedSearchIds()) {
            neSavedSearchIds.add(neSavedSearchId);
        }
        neInfo.setSavedSearchIds(neSavedSearchIds);

        selectedNEsMap.put(NEInfo.NEINFO_NE_NAMES, neInfo.getNeNames());
        selectedNEsMap.put(NEInfo.NEINFO_COLLECTION_NAMES, neInfo.getCollectionNames());
        selectedNEsMap.put(NEInfo.NEINFO_SAVED_SEARCH_IDS, neInfo.getSavedSearchIds());
        return selectedNEsMap;
    }

    private final Map<String, Object> prepareMainSchedule(final Schedule scheduleJAXB) {
        final Map<String, Object> scheduleMap = new HashMap<>();
        scheduleMap.put(EXEC_MODE, scheduleJAXB.getExecMode().getMode());
        final List<Map<String, String>> scheduleAttributes = prepareScheduleAttributes(scheduleJAXB.getScheduleAttributes());

        scheduleMap.put(SCHEDULE_ATTRIBUTES, scheduleAttributes);
        return scheduleMap;
    }

    private List<Map<String, String>> prepareScheduleAttributes(final List<ScheduleProperty> schedulePropertyJAXB) {
        final List<Map<String, String>> scheduleProperties = new ArrayList<>();
        for (final ScheduleProperty scheduleProperty : schedulePropertyJAXB) {
            scheduleProperties.add(prepareScheduleAttribute(scheduleProperty.getName(), scheduleProperty.getValue()));
        }
        return scheduleProperties;
    }

    private Map<String, String> prepareScheduleAttribute(final String propertyName, final String propertyValue) {
        final Map<String, String> property = new HashMap<>();
        property.put(ScheduleProperty.SCHEDULE_PROPERTY_NAME, propertyName);
        property.put(ScheduleProperty.SCHEDULE_PROPERTY_VALUE, propertyValue);
        return property;
    }

}
