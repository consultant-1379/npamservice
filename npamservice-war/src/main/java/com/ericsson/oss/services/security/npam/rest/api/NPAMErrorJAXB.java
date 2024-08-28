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
package com.ericsson.oss.services.security.npam.rest.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NPAMError", description = "Npam error message")
public class NPAMErrorJAXB {

    @Schema(description = "Error message corresponding 1:1 to the error code")
    private String userMessage;
    @Schema(description = "Unique NPAM error code")
    private int internalErrorCode;
    @Schema(description = "Optional free text detailing the error reason")
    private String errorDetails;
    
    public NPAMErrorJAXB() {
        super();
    }
    
    public NPAMErrorJAXB(final String userMessage, final int internalErrorCode) {
        super();
        this.userMessage = userMessage;
        this.internalErrorCode = internalErrorCode;
        this.errorDetails = "";
    }

    public NPAMErrorJAXB(final String userMessage, final int internalErrorCode, final String errorDetails) {
        super();
        this.userMessage = userMessage;
        this.internalErrorCode = internalErrorCode;
        this.errorDetails = errorDetails;
    }

    public String getUserMessage() {
        return userMessage;
    }
    
    public void setUserMessage(final String userMessage) {
        this.userMessage = userMessage;
    }
    
    public int getInternalErrorCode() {
        return internalErrorCode;
    }
    
    public void setInternalErrorCode(final int internalErrorCode) {
        this.internalErrorCode = internalErrorCode;
    }

    public String getErrorDetails() {
        return errorDetails;
    }
    
    public void setErrorDetails(final String errorDetails) {
        this.errorDetails = errorDetails;
    }

}
