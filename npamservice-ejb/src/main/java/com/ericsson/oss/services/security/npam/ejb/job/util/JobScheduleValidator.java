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

import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.REPEAT_TYPE_VALUE_MONTHLY;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.REPEAT_TYPE_VALUE_WEEKLY;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.REPEAT_TYPE_VALUE_YEARLY;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_END_DATE;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_OCCURRENCES;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_REPEAT_COUNT;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_REPEAT_ON;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_REPEAT_TYPE;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_START_DATE;

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.security.npam.api.exceptions.JobError;
import com.ericsson.oss.services.security.npam.api.exceptions.JobExceptionFactory;
import com.ericsson.oss.services.security.npam.api.job.modelentities.Schedule;

public class JobScheduleValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobScheduleValidator.class);

    @Inject
    JobExceptionFactory jobExceptionFactory;

    /* Validation Steps */
    public boolean validateMainScheduleParameters(final Schedule schedule) {
        LOGGER.info("validateScheduleParameters:: schedule={}  START", schedule);
        if (schedule != null && schedule.getExecMode() != null) {
            switch (schedule.getExecMode()) {
                case IMMEDIATE:
                    if (schedule.checkIsWrongImmediate()) {
                        throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_IMMEDIATE);
                    }
                    break;
                case SCHEDULED:
                    getAndValidateStartDate(schedule);//mandatory
                    if (schedule.checkIsPeriodic()) {
                        getAndValidateRepeatType(schedule); //mandatory
                        getAndValidateRepeatCount(schedule); //mandatory
                        getAndValidateOptionalOccurrence(schedule);
                        validateOptionalEndDate(schedule);
                    } else if (schedule.checkIsWrongNonPeriodic()) { //non periodic
                          LOGGER.info("validateMainScheduleParameters:: CASE SCHEDULED WRONG non periodic schedule={}", schedule);
                          throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_NON_PERIODIC,
                                    schedule);
                    }
                    break;
                default:
                    LOGGER.info("validateMainScheduleParameters:: Wrong schedule execMode : {}", schedule.getExecMode());
                    throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_EXEC_MODE,
                            schedule.getExecMode());
            }
        } else {
            LOGGER.info("validateMainScheduleParameters: Wrong schedule execMode schedule:{}", schedule);
            throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE, schedule);
        }
        LOGGER.info("validateScheduleParameters::  STOP");
        return true;
    }

    public int getAndValidateRepeatCount(final Schedule schedule) {
        final String repeatCountString = schedule.getScheduledAttributeValue(SA_REPEAT_COUNT);
        if (repeatCountString == null) {
            LOGGER.info("calculateNextScheduledTime:: Null schedule repeatCount : {}", repeatCountString);
            throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_REPEAT_COUNT, repeatCountString);
        }

        int repeatCount = 0;
        try {
            repeatCount = Integer.parseInt(repeatCountString);
            LOGGER.info("getAndValidateRepeatCount:: Validated schedule repeatOn : {}", repeatCount);
        } catch (final Exception e) {
            LOGGER.info("getAndValidateRepeatCount:: Wrong schedule repeatCount : {}", repeatCountString);
            throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_REPEAT_COUNT, repeatCountString);
        }

        if (repeatCount < 1) {
            LOGGER.info("getAndValidateRepeatCount:: Wrong schedule repeatCount : {}", repeatCountString);
            throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_REPEAT_COUNT, repeatCountString);
        }
        final String repeatType = schedule.getScheduledAttributeValue(SA_REPEAT_TYPE);
        if ((REPEAT_TYPE_VALUE_WEEKLY.equals(repeatType) && repeatCount > 52) || (REPEAT_TYPE_VALUE_MONTHLY.equals(repeatType) && repeatCount > 36)
                || (REPEAT_TYPE_VALUE_YEARLY.equals(repeatType) && repeatCount > 3)) {
            throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_REPEAT_COUNT, repeatCountString);

        }
        return repeatCount;
    }

    public Date getAndValidateStartDate(final Schedule schedule) {
        try {
            final Date startDate = DateFormatterUtil.parseUIDateStringWithJoda(schedule.getScheduledAttributeValue(SA_START_DATE));
            final String startDateToStringWithGMT = DateFormatterUtil.convertDateToStringWithGMT(startDate);
            LOGGER.info("getAndValidateStartDate:: Validated schedule startDate : {}", startDateToStringWithGMT);
            return startDate;
        } catch (final Exception e) {
            LOGGER.info("getAndValidateStartDate:: Wrong schedule startDate : {}", schedule.getScheduledAttributeValue(SA_START_DATE));
            throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_START_DATE,
                    schedule.getScheduledAttributeValue(SA_START_DATE));
        }
    }

    private void validateOptionalEndDate(final Schedule schedule) {
        if (schedule.getScheduledAttributeValue(SA_END_DATE) != null) {
            getAndValidateEndDate(schedule);
        }
    }

    public Date getAndValidateEndDate(final Schedule schedule) {
        try {
            final Date endDate = DateFormatterUtil.parseUIDateStringWithJoda(schedule.getScheduledAttributeValue(SA_END_DATE));
            final String endDateToStringWithGMT = DateFormatterUtil.convertDateToStringWithGMT(endDate);
            LOGGER.info("getAndValidateEndDate:: Validated schedule endDate : {}", endDateToStringWithGMT);
            return endDate;
        } catch (final Exception e) {
            LOGGER.info("getAndValidateEndDate:: Wrong schedule endDate : {}", schedule.getScheduledAttributeValue(SA_END_DATE));
            throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_END_DATE,
                    schedule.getScheduledAttributeValue(SA_END_DATE));
        }
    }

    private String getAndValidateRepeatType(final Schedule schedule) {
        final String repeatType = schedule.getScheduledAttributeValue(SA_REPEAT_TYPE);
        if (!REPEAT_TYPE_VALUE_WEEKLY.equals(repeatType) && !REPEAT_TYPE_VALUE_MONTHLY.equals(repeatType)
                && !REPEAT_TYPE_VALUE_YEARLY.equals(repeatType)) {
            throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_REPEAT_TYPE, repeatType);
        }
        return repeatType;
    }

    public String[] getAndValidateRepeatOn(final Schedule schedule) {
        String[] repeatOn = null;
        final String repeatOnString = schedule.getScheduledAttributeValue(SA_REPEAT_ON);
        if (repeatOnString != null) {
            try {
                repeatOn = repeatOnString.split(",");
                for (final String repeatOnItem : repeatOn) {
                    Integer.parseInt(repeatOnItem);
                }
            } catch (final Exception e) {
                LOGGER.info("getAndValidateRepeatOn:: Wrong schedule repeatOn : {}", repeatOnString);
                throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_REPEAT_ON, repeatOnString);
            }
            LOGGER.info("getAndValidateRepeatOn:: Validated schedule repeatOn : {}", repeatOnString);
        }
        return repeatOn;
    }

    //return > 0 if valid and present
    public int getAndValidateOptionalOccurrence(final Schedule schedule) {
        final String occurrence = schedule.getScheduledAttributeValue(SA_OCCURRENCES);
        int occurencesValue = 0;
        if (occurrence != null) {
            try {
                occurencesValue = Integer.parseInt(occurrence);
            } catch (final Exception e) {
                LOGGER.info("getAndValidateOptionalOccurrence:: Wrong schedule occurrence : {}", occurrence);
                throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_OCCURRENCES, occurrence);
            }
            if (occurencesValue <= 0 || occurencesValue > 10) {
                LOGGER.info("getAndValidateOptionalOccurrence:: Wrong schedule occurrence : {}", occurrence);
                throw jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_OCCURRENCES, occurrence);
            }
        }
        return occurencesValue;
    }
}
