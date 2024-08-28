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

public class NPAMRestErrorsMessageDetails {
    public static final String BAD_REQUEST_INVALID_INPUT_DATA = "Missing multipart form data for file import.";
    public static final String UNPROCESSABLE_UNEXPECTED_PERIODIC = "Unexpected periodic attributes for specific jobType";
    public static final String UNPROCESSABLE_UNEXPECTED_ATTRIBUTE_WITH_IMMEDIATE_EXECMODE = "Unexpected attributes with execMode IMMEDIATE";
    public static final String UNPROCESSABLE_ONLY_IMMEDIATE_EXECMODE_ALLOWED = "Only IMMEDIATE execMode allowed.";
    public static final String UNPROCESSABLE_STARTDATE_IN_THE_PAST = "Attribute START_DATE is in the past.";
    public static final String UNPROCESSABLE_INVALID_STARTDATE_FORMAT = "Wrong START_DATE format.";
    public static final String UNPROCESSABLE_MISSING_STARTDATE = "Missing START_DATE attribute.";
    public static final String UNPROCESSABLE_ONLY_STARTDATE_ALLOWED = "Only START_DATE allowed.";
    public static final String UNPROCESSABLE_MANUAL_NOT_SUPPORTED = "MANUAL execMode not supported.";
    public static final String UNPROCESSABLE_DUPLICATED_JOBPROPERTIES = "Duplicated jobProperties.";
    public static final String UNPROCESSABLE_DUPLICATED_PROPERTIES = "Duplicated properties.";
    public static final String UNPROCESSABLE_MISSING_MANDATORY_JOBPROPERTIES = "Missing jobProperties mandatory attributes.";
    public static final String UNPROCESSABLE_UNEXPECTED_JOBPROPERTIES = "Unexpected jobProperties attributes for specific JobType.";
    public static final String UNPROCESSABLE_UNEXPECTED_SELECTEDNES = "Unexpected selectedNE for specific JobType.";
    public static final String UNPROCESSABLE_MAINSCHEDULE_CONFLICT = "Conflict between OCCURENCES and END_DATE.";
    public static final String UNPROCESSABLE_MAINSCHEDULE_DATES_MISMATCH = "END_DATE is earlier than START_DATE.";
    public static final String UNPROCESSABLE_CANCEL_JOB_WRONG_STATE = "Wrong STATE %s . Only jobs in STATE SCHEDULED can be canceled.";
    public static final String INTERNAL_SERVER_ERROR_IP_RETRIEVAL = "Unable to retrieve IP Address";
    public static final String INTERNAL_SERVER_ERROR_RENAME_FILE = "Unable to rename imported file";
    public static final String INTERNAL_SERVER_ERROR_DECRYPT_USER = "currentUser";
    public static final String INTERNAL_SERVER_ERROR_DECRYPT_PASSWD = "currentPswd";
    public static final String INTERNAL_SERVER_ERROR_USER_NOT_FOUND = "User not found";
    public static final String INTERNAL_SERVER_ERROR_SAVE_FILE = "Unable to save imported file";
    public static final String INTERNAL_SERVER_ERROR_FOLDER_NOT_FOUND = "Import folder not found.";

    private NPAMRestErrorsMessageDetails() {
    }
}
