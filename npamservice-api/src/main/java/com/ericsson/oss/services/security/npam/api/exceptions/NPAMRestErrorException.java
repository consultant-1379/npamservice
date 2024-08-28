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

import javax.ejb.ApplicationException;
import javax.ws.rs.core.Response.Status;

@ApplicationException(rollback = false)
public class NPAMRestErrorException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    // Internal Code
    public static final int NPAM_HTTP_400_START = 4000;
    public static final int NPAM_HTTP_401_START = 4100;
    public static final int NPAM_HTTP_403_START = 4200;
    public static final int NPAM_HTTP_404_START = 4300;
    public static final int NPAM_HTTP_422_START = 4400;
    public static final int NPAM_HTTP_500_START = 5000;

    final NPamRestErrorMessage npamRestErrorMessage;

    public NPAMRestErrorException(final NPamRestErrorMessage errorCode) {
        this.npamRestErrorMessage = errorCode;
        this.npamRestErrorMessage.setErrorDetails("");
    }

    public NPAMRestErrorException(final NPamRestErrorMessage errorCode, final String errorDetails) {
        this.npamRestErrorMessage = errorCode;
        this.npamRestErrorMessage.setErrorDetails(errorDetails);
    }

    public NPamRestErrorMessage getInternalCode() {
        return npamRestErrorMessage;
    }

    public enum NPamRestErrorMessage {
        // internalCode , userMessage, httpStatus

        //        NPAM_HTTP_400_START
        BAD_REQUEST(NPAM_HTTP_400_START, "Bad Request.", Status.BAD_REQUEST.getStatusCode()),
        BAD_REQUEST_INVALID_JOB_TYPE(NPAM_HTTP_400_START + 1, "Invalid JobType.", Status.BAD_REQUEST.getStatusCode()),
        BAD_REQUEST_INVALID_INPUT_JSON(NPAM_HTTP_400_START + 2, "Invalid input data, see error details.",
                Status.BAD_REQUEST.getStatusCode()),

        BAD_REQUEST_MISSING_ENCRYPTION_KEY(NPAM_HTTP_400_START + 3, "Missing encryptionKey attribute.", Status.BAD_REQUEST.getStatusCode()),

        //        NPAM_HTTP_401_START
        UNAUTHORIZED(NPAM_HTTP_401_START, "Unauthorized.", Status.UNAUTHORIZED.getStatusCode()),

        //        NPAM_HTTP_403_START
        FORBIDDEN(NPAM_HTTP_403_START, "Forbidden.", Status.FORBIDDEN.getStatusCode()),
        FORBIDDEN_FEATURE_DISABLED(NPAM_HTTP_403_START + 1, "NPAM feature is disabled.", Status.FORBIDDEN.getStatusCode()),

        //        NPAM_HTTP_404_START
        NOT_FOUND(NPAM_HTTP_404_START, "Not Found.", Status.NOT_FOUND.getStatusCode()),
        NOT_FOUND_JOB_NAME(NPAM_HTTP_404_START + 1, "Job name Not Found.", Status.NOT_FOUND.getStatusCode()),
        NOT_FOUND_JOB_ID(NPAM_HTTP_404_START + 2, "Job id Not Found.", Status.NOT_FOUND.getStatusCode()),

        //        NPAM_HTTP_422_START
        UNPROCESSABLE_ENTITY(NPAM_HTTP_422_START, "Unprocessable Entity.", 422),
        UNPROCESSABLE_ENTITY_INVALID_JOB_NAME(NPAM_HTTP_422_START + 1, "Invalid name attribute.", 422),
        UNPROCESSABLE_ENTITY_INVALID_JOB_TYPE(NPAM_HTTP_422_START + 2, "Invalid jobType attribute.", 422),
        UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE(NPAM_HTTP_422_START + 3, "Invalid mainSchedule attribute.", 422),
        UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES(NPAM_HTTP_422_START + 4, "Invalid selectedNEs attribute.", 422),
        UNPROCESSABLE_ENTITY_INVALID_JOB_PROPERTIES(NPAM_HTTP_422_START + 5, "Invalid jobProperties attribute.", 422),
        UNPROCESSABLE_ENTITY_INVALID_CREDENTIALS(NPAM_HTTP_422_START + 6, "Invalid Credentials.", 422),
        UNPROCESSABLE_ENTITY_NOT_EXISTING_NETWORK_ELEMENT(NPAM_HTTP_422_START + 7, "NetworkElement not found.", 422),
        UNPROCESSABLE_ENTITY_COLLECTION_NOT_FOUND(NPAM_HTTP_422_START + 8, "Collection not found.", 422),
        UNPROCESSABLE_ENTITY_COLLECTION_HYBRID(NPAM_HTTP_422_START + 9, "Hybrid Collection not supported.", 422),
        UNPROCESSABLE_ENTITY_COLLECTION_MIXED_CONTENT(NPAM_HTTP_422_START + 10, "Collection with other than NetworkElement not supported.", 422),
        UNPROCESSABLE_ENTITY_SAVED_SEARCH_MIXED_CONTENT(NPAM_HTTP_422_START + 11, "SavedSearch with other than NetworkElement not supported.", 422),
        UNPROCESSABLE_ENTITY_SAVED_SEARCH_NOT_FOUND(NPAM_HTTP_422_START + 12, "SavedSearch not found.", 422),
        UNPROCESSABLE_ENTITY_FILE_NOT_FOUND(NPAM_HTTP_422_START + 13, "File not found.", 422),
        UNPROCESSABLE_ENTITY_JOB_NAME_ALREADY_PRESENT(NPAM_HTTP_422_START + 14, "Job Name already existing.", 422),
        UNPROCESSABLE_ENTITY_JOB_CONFIGURATION_ERROR(NPAM_HTTP_422_START + 15, "Job configuration error.", 422),
        UNPROCESSABLE_ENTITY_FILE_NOT_CONTAIN_ANY_NETWORK_ELEMENT(NPAM_HTTP_422_START + 16,
                "File does not contain any NetworkElement.", 422),
        UNPROCESSABLE_ENTITY_INVALID_MUID(NPAM_HTTP_422_START + 17, "Only NEAccount id 1 is supported.", 422),
        UNPROCESSABLE_ENTITY_NEACCOUNT_NOT_EXISTS(NPAM_HTTP_422_START + 18, "NEAccount not found for selected NetworkElement.", 422),
        UNPROCESSABLE_ENTITY_INVALID_NE_NPAM_STATUS(NPAM_HTTP_422_START + 19, "Invalid neNpamStatus NOT_SUPPORTED.",
                422),
        UNPROCESSABLE_ENTITY_INVALID_NPAMCONFIG_PROPERTIES(NPAM_HTTP_422_START + 20, "Invalid NpamConfig attributes.", 422),
        UNPROCESSABLE_ENTITY_IMPORT_FILE(NPAM_HTTP_422_START + 21, "Import file error.", 422),
        UNPROCESSABLE_ENTITY_ENCRYPTION_KEY(NPAM_HTTP_422_START + 22, "Invalid encryptionKey attribute.", 422),
        UNPROCESSABLE_ENTITY_CANCEL_JOB(NPAM_HTTP_422_START + 23, "Unable to cancel job.", 422),
        UNPROCESSABLE_ENTITY_IMPORT_FILE_PRESENT(NPAM_HTTP_422_START + 24, "Import file already present.", 422),

        //        NPAM_HTTP_500_START
        INTERNAL_SERVER_ERROR(NPAM_HTTP_500_START, "Internal Server Error", Status.INTERNAL_SERVER_ERROR.getStatusCode()),
        INTERNAL_SERVER_ERROR_DPS_RWISSUE(NPAM_HTTP_500_START + 1, "Database Read/Write issue", Status.INTERNAL_SERVER_ERROR.getStatusCode()),
        INTERNAL_SERVER_ERROR_DECRYPT(NPAM_HTTP_500_START + 2, "Cryptography Service decrypt issue.", Status.INTERNAL_SERVER_ERROR.getStatusCode()),
        INTERNAL_SERVER_ERROR_ENCRYPT_FILE(NPAM_HTTP_500_START + 3, "Error to encrypt file.", Status.INTERNAL_SERVER_ERROR.getStatusCode()),
        INTERNAL_SERVER_ERROR_NFS_RWISSUE(NPAM_HTTP_500_START + 4, "Filesystem Read/Write issue",
                Status.INTERNAL_SERVER_ERROR.getStatusCode()),
        INTERNAL_SERVER_ERROR_DUPLICATED_NPAMCONFIG(NPAM_HTTP_500_START + 5, "NpamConfig duplicated objects.",
                Status.INTERNAL_SERVER_ERROR.getStatusCode()),
        ;

        private final String message;
        private final int internalCode;
        private final int httpStatusCode;
        private String errorDetails;

        NPamRestErrorMessage(final int code, final String message, final int httpStatusCode) {
            this.internalCode = code;
            this.message = message;
            this.httpStatusCode = httpStatusCode;
            this.errorDetails = "";
        }

        void setErrorDetails(final String errorDetails) {
            this.errorDetails = errorDetails;
        }

        public String getMessage() {
            return message;
        }

        public int getCode() {
            return internalCode;
        }

        public int getHttpStatusCode() {
            return httpStatusCode;
        }

        public String getErrorDetails() {
            return errorDetails;
        }

        public static NPAMRestErrorException buildFromJobConfigurationException(final JobConfigurationException ex) {
            return new NPAMRestErrorException(UNPROCESSABLE_ENTITY_JOB_CONFIGURATION_ERROR, ex.getMessage());
        }
    }

}
