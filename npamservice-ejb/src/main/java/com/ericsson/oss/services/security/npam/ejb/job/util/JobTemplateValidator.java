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

package com.ericsson.oss.services.security.npam.ejb.job.util;


import static com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants.PK_NEXT_PASSWORD;
import static com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants.PK_NEXT_USERNAME;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_END_DATE;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_OCCURRENCES;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_REPEAT_COUNT;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_REPEAT_TYPE;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_START_DATE;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_DUPLICATED_JOBPROPERTIES;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_INVALID_STARTDATE_FORMAT;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_MAINSCHEDULE_CONFLICT;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_MAINSCHEDULE_DATES_MISMATCH;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_MANUAL_NOT_SUPPORTED;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_MISSING_MANDATORY_JOBPROPERTIES;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_MISSING_STARTDATE;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_ONLY_STARTDATE_ALLOWED;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_STARTDATE_IN_THE_PAST;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_UNEXPECTED_ATTRIBUTE_WITH_IMMEDIATE_EXECMODE;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_ONLY_IMMEDIATE_EXECMODE_ALLOWED;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_UNEXPECTED_JOBPROPERTIES;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_UNEXPECTED_PERIODIC;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_UNEXPECTED_SELECTEDNES;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.security.npam.api.exceptions.JobConfigurationException;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage;
import com.ericsson.oss.services.security.npam.api.job.modelentities.ExecMode;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJobTemplate;
import com.ericsson.oss.services.security.npam.api.job.modelentities.Schedule;
import com.ericsson.oss.services.security.npam.api.job.modelentities.ScheduleProperty;
import com.ericsson.oss.services.security.npam.ejb.services.NodePamCredentialManager;

public class JobTemplateValidator {
    private static final Logger logger = LoggerFactory.getLogger(JobTemplateValidator.class);

    @Inject
    private FileResource fileResource;

    @Inject
    NodePamCredentialManager nodePamCredentialManager;

    @Inject
    JobScheduleValidator jobScheduleValidator;

    public void validateMainScheduleImmediate(final NPamJobTemplate nPAMJobTemplate) {
        if (!nPAMJobTemplate.getMainSchedule().checkIsImmediate()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE,
                    UNPROCESSABLE_ONLY_IMMEDIATE_EXECMODE_ALLOWED);
        }
    }
    public void validateMainScheduleNotPeriodic(final NPamJobTemplate nPAMJobTemplate) {
        if (nPAMJobTemplate.getMainSchedule().checkIsPeriodic()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE,
                    UNPROCESSABLE_UNEXPECTED_PERIODIC);
        }
    }

    public void validateMainScheduleImmediateOrDeferred(final NPamJobTemplate nPAMJobTemplate) {
        if (nPAMJobTemplate.getMainSchedule().checkIsWrongImmediate()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE,
                    UNPROCESSABLE_UNEXPECTED_ATTRIBUTE_WITH_IMMEDIATE_EXECMODE);
        }

        if (nPAMJobTemplate.getMainSchedule().getExecMode() == ExecMode.SCHEDULED) {
            validateStartDate(nPAMJobTemplate.getMainSchedule().getScheduledAttributeValue(SA_START_DATE));
            if (nPAMJobTemplate.getMainSchedule().getScheduleAttributes().size() > 1) {
                throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE,
                        UNPROCESSABLE_ONLY_STARTDATE_ALLOWED);
            }
        }
        if (nPAMJobTemplate.getMainSchedule().getExecMode() == ExecMode.MANUAL) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE, UNPROCESSABLE_MANUAL_NOT_SUPPORTED);
        }
    }

    private void validateStartDate(final String startDate) {
        if (startDate != null) {
            try {
                final LocalDateTime startTime = LocalDateTime.parse(startDate,
                        DateTimeFormatter.ofPattern("uuuu-M-d HH:mm:ss").withResolverStyle(ResolverStyle.STRICT));
                if (!startTime.isAfter(LocalDateTime.now())) {
                    throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE,
                            UNPROCESSABLE_STARTDATE_IN_THE_PAST);
                }

            } catch (final DateTimeParseException ex) {
                throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE,
                        UNPROCESSABLE_INVALID_STARTDATE_FORMAT);
            }
        } else {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE, UNPROCESSABLE_MISSING_STARTDATE);
        }
    }

    public void validateSelectedNEsToBeEmpty(final NPamJobTemplate nPAMJobTemplate) {
        if (!nPAMJobTemplate.getSelectedNEs().getNeNames().isEmpty() || !nPAMJobTemplate.getSelectedNEs().getCollectionNames().isEmpty()
                || !nPAMJobTemplate.getSelectedNEs().getSavedSearchIds().isEmpty()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES, UNPROCESSABLE_UNEXPECTED_SELECTEDNES);
        }
    }

    public void validateSelectedNEsToBeNotEmpty(final NPamJobTemplate nPAMJobTemplate) {
        if (nPAMJobTemplate.getSelectedNEs().getNeNames().isEmpty() && nPAMJobTemplate.getSelectedNEs().getCollectionNames().isEmpty()
                && nPAMJobTemplate.getSelectedNEs().getSavedSearchIds().isEmpty()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES);
        }
    }

    public void validatePropertyValue(final List<JobProperty> jobProperties, final List<String> mandatoryProperties,
                                      final List<String> optionalProperties) {
        final Set<String> propertyKeys = new HashSet<>();
        int propertiesCount = 0;
        if (jobProperties != null) {
            for (final JobProperty jobProperty : jobProperties) {
                propertyKeys.add(jobProperty.getKey());
            }
            propertiesCount = jobProperties.size();
        }
        //Parameters are not unique
        if (propertyKeys.size() != propertiesCount) {
            logger.warn("Properties for Schedule Job Template are ambiguous");
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_PROPERTIES,
                    UNPROCESSABLE_DUPLICATED_JOBPROPERTIES);
        }
        final Set<String> mandatoryKeys = mandatoryProperties.stream().collect(Collectors.toSet());
        final Set<String> optionalKeys = optionalProperties.stream().collect(Collectors.toSet());
        //Parameters have to contain mandatory keys
        if (!propertyKeys.containsAll(mandatoryKeys)) {
            logger.warn("Properties for Schedule Job Template don't contain all mandatory fields");
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_PROPERTIES,
                    UNPROCESSABLE_MISSING_MANDATORY_JOBPROPERTIES);

        }
        //Parameters can contain optional keys and no other
        checkWrongParameters(propertyKeys, mandatoryKeys, optionalKeys);
    }

    public void validateCredentials(final List<JobProperty> jobProperties) {
        final String username = JobProperty.getPropertyValue(PK_NEXT_USERNAME, jobProperties);
        final String password = JobProperty.getPropertyValue(PK_NEXT_PASSWORD, jobProperties);

        if (!nodePamCredentialManager.validateCredentialString(password, username)) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_CREDENTIALS);
        }
    }

    public void validateJobTemplateName(final NPamJobTemplate nPAMJobTemplate) {
        if (nPAMJobTemplate.getName() == null || nPAMJobTemplate.getName().isEmpty()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_NAME);
        }
    }

    public void validateFileName(final List<JobProperty> jobProperties) {
        final String filename = fileResource.getFilenameBeforeJob(jobProperties);
        if (filename == null) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_PROPERTIES,
                    UNPROCESSABLE_MISSING_MANDATORY_JOBPROPERTIES + " FILENAME");
        }
        final File importedFile = new File(filename);
        if (!importedFile.exists() || importedFile.isDirectory()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_FILE_NOT_FOUND, filename);
        }
    }

    private void checkWrongParameters(final Set<String> propertyKeys, final Set<String> mandatoryKeys, final Set<String> optionalKeys) {
        for (final String jobProperty : propertyKeys) {
            if (!mandatoryKeys.contains(jobProperty) && !optionalKeys.contains(jobProperty)) {
                logger.warn("Properties for Job Template contains wrong fields");
                throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_PROPERTIES,
                        UNPROCESSABLE_UNEXPECTED_JOBPROPERTIES);
            }
        }
    }

    private void validateScheduleAttribute(final Schedule mainSchedule) {
        final List<String> attributeProperties = Stream.of(SA_START_DATE, SA_END_DATE, SA_OCCURRENCES, SA_REPEAT_COUNT, SA_REPEAT_TYPE)
                .collect(Collectors.toList());
        final List<ScheduleProperty> scheduleAttributes = mainSchedule.getScheduleAttributes();
        for (final ScheduleProperty scheduleAttribute: scheduleAttributes) {
            if (!attributeProperties.contains(scheduleAttribute.getName())){
                throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE, scheduleAttribute.getName());
            }
        }
    }

    public void validateMainScheduleParameters(final Schedule mainSchedule) {
        validateScheduleAttribute(mainSchedule);
        if (mainSchedule.getExecMode() == ExecMode.SCHEDULED) {
            validateStartDate(mainSchedule.getScheduledAttributeValue(SA_START_DATE));
        }
        try {
            jobScheduleValidator.validateMainScheduleParameters(mainSchedule);
        } catch (final JobConfigurationException ex) {
            throw NPamRestErrorMessage.buildFromJobConfigurationException(ex);
        }
        if (mainSchedule.getScheduledAttributeValue(SA_END_DATE) != null && mainSchedule.getScheduledAttributeValue(SA_OCCURRENCES) != null) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE, UNPROCESSABLE_MAINSCHEDULE_CONFLICT);
        }
        if (mainSchedule.getScheduledAttributeValue(SA_END_DATE) != null) {
            Date endDate;
            Date startDate;

            endDate = jobScheduleValidator.getAndValidateEndDate(mainSchedule);
            startDate = jobScheduleValidator.getAndValidateStartDate(mainSchedule);

            if (endDate.compareTo(startDate) < 0) {
                throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE,
                        UNPROCESSABLE_MAINSCHEDULE_DATES_MISMATCH);
            }
        }
    }
}
