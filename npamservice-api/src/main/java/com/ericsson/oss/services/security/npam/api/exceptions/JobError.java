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

package com.ericsson.oss.services.security.npam.api.exceptions;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SCHEDULE_JOB_ERROR_START_INT;

public enum JobError {
    /*
      S E R V E R - E R R O R
     */
    SERVER_ERROR(SCHEDULE_JOB_ERROR_START_INT, "{0}"),
    UNEXPECTED_ERROR(SCHEDULE_JOB_ERROR_START_INT + 1, "This is an unhandled system error, please check the error log for more details"),

    /*
      CONFIGURATION - E R R O R
     */
    CONFIGURATION_ERROR(SCHEDULE_JOB_ERROR_START_INT + 2, "Configuration error: {0}"), //to be removed
    CONFIGURATION_ERROR_NOT_FOUND_TEMPLATE_ID(SCHEDULE_JOB_ERROR_START_INT + 3, "Configuration error: Unable to retrieve jobtemplate from db for the jobTemplateId={0}"),
    CONFIGURATION_ERROR_NOT_FOUND_JOB_ID(SCHEDULE_JOB_ERROR_START_INT + 4,"Configuration error: Unable to retrieve job from db with jobId={0}"),
    CONFIGURATION_ERROR_NOT_FOUND_JOB_NAME(SCHEDULE_JOB_ERROR_START_INT + 4,"Configuration error: Unable to retrieve job from db with jobName={0}"),
    CONFIGURATION_ERROR_INVALID_STATE_FOR_JOB(SCHEDULE_JOB_ERROR_START_INT + 5,"Configuration error: Impossible to start job with jobId={0} with status={1}"),
    CONFIGURATION_ERROR_SCHEDULE_INVALID_FOR_JOB_TEMPLATE(SCHEDULE_JOB_ERROR_START_INT + 6, "Configuration error: Schedule is not available for the provided jobTemplateId={0}"),
    CONFIGURATION_ERROR_CANCEL_INVALID_FOR_STARTED_JOB(SCHEDULE_JOB_ERROR_START_INT + 7, "Configuration error: Impossible to cancel the already started job={0} with state={1}. Wait for completion to delete next executions."),
    CONFIGURATION_ERROR_CANCEL_INVALID_FOR_JOB(SCHEDULE_JOB_ERROR_START_INT + 8, "Configuration error: Impossible to cancel job={0} with state={1}. Only jobs in SCHEDULED state can be canceled."),
    CONFIGURATION_ERROR_NO_NODES(SCHEDULE_JOB_ERROR_START_INT + 11, "Configuration error: no nodes available for job with jobId={0}"),
    CONFIGURATION_ERROR_UNIMPLEMENTED_METHOD(SCHEDULE_JOB_ERROR_START_INT + 12, "Configuration error: Unimplemented method"),
    CONFIGURATION_ERROR_CREDENTIALS_FILE_EMPTY(SCHEDULE_JOB_ERROR_START_INT + 13, "Configuration error: empty credentials file for jobId={0}"),
    CONFIGURATION_ERROR_WRONG_SCHEDULE(SCHEDULE_JOB_ERROR_START_INT + 14, "Configuration error: Wrong schedule={0} provided for scheduled job"),
    CONFIGURATION_ERROR_WRONG_SCHEDULE_REPEAT_TYPE(SCHEDULE_JOB_ERROR_START_INT + 15, "Configuration error: Wrong repeatType={0} provided for scheduled job"),
    CONFIGURATION_ERROR_WRONG_SCHEDULE_EXEC_MODE(SCHEDULE_JOB_ERROR_START_INT + 16, "Configuration error: Wrong schedule execMode={0} provided for scheduled job"),
    CONFIGURATION_ERROR_WRONG_SCHEDULE_START_DATE(SCHEDULE_JOB_ERROR_START_INT + 17, "Configuration error: Wrong schedule startDate={0} provided for scheduled job"),
    CONFIGURATION_ERROR_WRONG_SCHEDULE_END_DATE(SCHEDULE_JOB_ERROR_START_INT + 18, "Configuration error: Wrong schedule endDate={0} provided for scheduled job"),
    CONFIGURATION_ERROR_WRONG_SCHEDULE_OCCURRENCES(SCHEDULE_JOB_ERROR_START_INT + 19, "Configuration error: Wrong schedule occurrences={0} provided for scheduled job"),
    CONFIGURATION_ERROR_WRONG_SCHEDULE_REPEAT_COUNT(SCHEDULE_JOB_ERROR_START_INT + 20, "Configuration error: Wrong schedule repeatCount={0} provided for scheduled job"),
    CONFIGURATION_ERROR_WRONG_SCHEDULE_REPEAT_ON(SCHEDULE_JOB_ERROR_START_INT + 21, "Configuration error: Wrong schedule repeatOn={0} provided for scheduled job"),
    CONFIGURATION_ERROR_WRONG_SCHEDULE_NON_PERIODIC(SCHEDULE_JOB_ERROR_START_INT + 22,
            "Configuration error: Wrong not periodic schedule={0} provided for scheduled job"),
    CONFIGURATION_ERROR_WRONG_SCHEDULE_IMMEDIATE(SCHEDULE_JOB_ERROR_START_INT + 23, "Configuration error: Wrong immediate schedule provided for scheduled job"),
    CONFIGURATION_ERROR_TO_RENAME_FILE(SCHEDULE_JOB_ERROR_START_INT + 24, "Error to rename file");

    private final String message;
    private final int code;

    JobError(final int code, final String message) {
        this.message = message;
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }
}
