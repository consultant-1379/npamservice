/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.security.npam.rest.api;

import static com.ericsson.oss.services.security.npam.api.rest.NPamConstants.PATTERN_DATE_FORMAT;

import java.util.Date;

import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEAccount;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NPamNEAccount", description = "A Network Element Account")
public class NPamNEAccountJAXB {

    @Schema(description = "Network Element name")
    private String neName;
    @Schema(description = "Current user")
    private String currentUser;
    @Schema(description = "Network Element account id")
    private String id;
    @Schema(description = "Error details if any")
    private String errorDetails;
    @Schema(description = "Status of Network Element account")
    private String status;

    @Schema(description = "Date of latest update")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = PATTERN_DATE_FORMAT)
    private Date lastUpdate;

    public NPamNEAccountJAXB() {
    }

    public NPamNEAccountJAXB(final NPamNEAccount neAccount) {
        this.neName = neAccount.getNeName() != null ? neAccount.getNeName() : "";
        this.currentUser = neAccount.getCurrentUser() != null ? neAccount.getCurrentUser() : "";
        this.id = neAccount.getNetworkElementAccountId() != null ? neAccount.getNetworkElementAccountId() : "";
        this.errorDetails = neAccount.getErrorDetails() != null ? neAccount.getErrorDetails() : "";
        this.status = neAccount.getStatus() != null ? neAccount.getStatus() : "";
        this.lastUpdate = neAccount.getLastUpdate();
    }

    public String getNeName() {
        return neName;
    }

    public void setNeName(final String neName) {
        this.neName = neName != null ? neName : "";
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(final String currentUser) {
        this.currentUser = currentUser != null ? currentUser : "";
    }

    public String getId() {
        return id;
    }

    public void setId(final String networkElementAccountId) {
        this.id = networkElementAccountId != null ? networkElementAccountId : "";
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(final String errorDetails) {
        this.errorDetails = errorDetails != null ? errorDetails : "";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status != null ? status : "";
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(final Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @Override
    public String toString() {
        return "NPamNEAccountJAXB [neName=" + neName + ", currentUser=" + currentUser + ", id=" + id
                + ", errorDetails=" + errorDetails + ", status=" + status + ", lastUpdate=" + lastUpdate + "]";
    }
}
