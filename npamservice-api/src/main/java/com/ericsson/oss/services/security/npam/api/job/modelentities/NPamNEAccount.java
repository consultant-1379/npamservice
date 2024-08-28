/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.security.npam.api.job.modelentities;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class NPamNEAccount implements Serializable {

    private static final long serialVersionUID = -4174837870716355014L;
    private String neName;
    private String currentUser;
    private String currentPswd;
    private String networkElementAccountId;
    private String errorDetails;
    private String status;
    private Date lastUpdate;

    public NPamNEAccount() {
    }

    public NPamNEAccount(final String neName, final Map<String, Object> attributes) {
        this.neName = neName;
        this.currentUser = (String) attributes.get("currentUsername");
        this.currentPswd = (String) attributes.get("currentPassword");
        this.status = (String) attributes.get("updateStatus");
        this.errorDetails = (String) attributes.get("errorDetails");
        if (attributes.get("lastPasswordChange") != null) {
            this.lastUpdate = ((Date) attributes.get("lastPasswordChange"));
        } else {
            this.lastUpdate = null;
        }
        this.networkElementAccountId = (String) attributes.get("networkElementAccountId");
    }
    /**
     * @return the neName
     */
    public String getNeName() {
        return neName;
    }
    /**
     * @param neName the neName to set
     */
    public void setNeName(final String neName) {
        this.neName = neName;
    }
    /**
     * @return the currentUser
     */
    public String getCurrentUser() {
        return currentUser;
    }
    /**
     * @param currentUser the currentUser to set
     */
    public void setCurrentUser(final String currentUser) {
        this.currentUser = currentUser;
    }
    /**
     * @return the currentPswd
     */
    public String getCurrentPswd() {
        return currentPswd;
    }

    /**
     * @param currentPswd
     *            the currentPswd to set
     */
    public void setCurrentPswd(final String currentPswd) {
        this.currentPswd = currentPswd;
    }

    /**
     * @return the networkElementAccountId
     */
    public String getNetworkElementAccountId() {
        return networkElementAccountId;
    }

    /**
     * @param networkElementAccountId
     *            the networkElementAccountId to set
     */
    public void setNetworkElementAccountId(final String networkElementAccountId) {
        this.networkElementAccountId = networkElementAccountId;
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
        this.errorDetails = errorDetails;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }
    /**
     * @param status the status to set
     */
    public void setStatus(final String status) {
        this.status = status;
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

    @Override
    public String toString() {
        return "NPamNEAccount [neName=" + neName + ", currentUser=" + currentUser + ", networkElementAccountId=" + networkElementAccountId
                + ", errorDetails=" + errorDetails + ", status=" + status + ", lastUpdate=\" + lastUpdate + \"]";
    }

    /**
     * @return the serialversionuid
     */
    public static long getSerialversionuid() {
        return serialVersionUID;
    }
}
