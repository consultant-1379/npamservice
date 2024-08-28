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

@Schema(name = "NPamNEAccountCredential", description = "An object with Network Element Accounts details associated to the given Network Element")
public class NPamNEAccountCredentialJAXB {

    @Schema(description = "Network Element name")
    private String neName;
    @Schema(description = "NE ip address")
    private String ipAddress;
    @Schema(description = "Current user")
    private String currentUser;
    @Schema(description = "Current password in plain text")
    private String currentPswd;
    @Schema(description = "Network Element account id")
    private String id;
    @Schema(description = "Error details if any")
    private String errorDetails;
    @Schema(description = "Status of Network Element account")
    private String status;

    @Schema(description = "Date of latest update")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = PATTERN_DATE_FORMAT)
    private Date lastUpdate;

    public NPamNEAccountCredentialJAXB() {
    }

    public NPamNEAccountCredentialJAXB(final NPamNEAccount neAccount) {
        this.neName = neAccount.getNeName() != null ? neAccount.getNeName() : "";
        this.ipAddress = "";
        this.currentPswd = neAccount.getCurrentPswd() != null ? neAccount.getCurrentPswd() : "";
        this.id = neAccount.getNetworkElementAccountId();
        this.currentUser = neAccount.getCurrentUser() != null ? neAccount.getCurrentUser() : "";
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

    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * @param ipAddress
     *            the IPAddress to set
     */
    public void setIpAddress(final String ipAddress) {
        this.ipAddress = ipAddress != null ? ipAddress : "";
    }

    public String getCurrentPswd() {
        return currentPswd;
    }

    public void setCurrentPswd(final String currentPswd) {
        this.currentPswd = currentPswd != null ? currentPswd : "";
    }

    public String getId() {
        return id;
    }

    public void setId(final String networkElementAccountId) {
        this.id = networkElementAccountId != null ? networkElementAccountId : "";
    }

    public String getCurrentUser() {
        return currentUser;
    }

    /**
     * @param currentUser
     *            the currentUser to set
     */
    public void setCurrentUser(final String currentUser) {
        this.currentUser = currentUser != null ? currentUser : "";
    }

    /**
     * @return the errorDetails
     */
    public String getErrorDetails() {
        return errorDetails;
    }

    /**
     * @param errorDetails
     *            the errorDetails to set
     */
    public void setErrorDetails(final String errorDetails) {
        this.errorDetails = errorDetails != null ? errorDetails : "";
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(final String status) {
        this.status = status != null ? status : "";
    }

    /**
     * @return the lastUpdate
     */
    public Date getLastUpdate() {
        return lastUpdate;
    }

    /**
     * @param lastUpdate
     *            the lastUpdate to set
     */
    public void setLastUpdate(final Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

}
