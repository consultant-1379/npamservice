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
package com.ericsson.oss.services.security.npam.api.neaccount.modelentities;

import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class NetworkElementAccount implements Serializable {
    private static final long serialVersionUID = -4174837870716355014L;

    public static final String NEA_CURRENT_USER_NAME = "currentUsername";
    public static final String NEA_CURRENT_PASSWORD = "currentPassword";
    public static final String NEA_NEXT_USER_NAME = "nextUsername";
    public static final String NEA_NEXT_PASSWORD = "nextPassword";
    public static final String NEA_UPDATE_STATUS = "updateStatus";
    public static final String NEA_ERROR_DETAILS = "errorDetails";
    public static final String NEA_LAST_PASSWORD_CHANGE = "lastPasswordChange";
    public static final String NEA_NEACCOUNT_ID = "networkElementAccountId";
    public static final String NEA_LAST_FAILED = "lastFailed";
    public static final String NEA_JOB_TYPE = "jobType";
    public static final String NEA_MAIN_JOB_ID = "mainJobId";
    public static final String NEA_NE_JOB_ID = "neJobId";

    private String networkElementAccountId;
    private String currentUsername;
    private String currentPassword;
    private String nextUsername;
    private String nextPassword;
    private Date lastPasswordChange;
    private String errorDetails;
    private Date lastFailed;
    private JobType jobType;
    private Long mainJobId;
    private Long neJobId;
    private NetworkElementAccountUpdateStatus updateStatus;

    public NetworkElementAccount(final Map<String, Object> attributes) {
        this.networkElementAccountId = (String) attributes.get(NEA_NEACCOUNT_ID);
        this.currentUsername = (String) attributes.get(NEA_CURRENT_USER_NAME);
        this.currentPassword = (String) attributes.get(NEA_CURRENT_PASSWORD);
        this.nextUsername = (String) attributes.get(NEA_NEXT_USER_NAME);
        this.nextPassword = (String) attributes.get(NEA_NEXT_PASSWORD);
        this.lastPasswordChange = (Date) attributes.get(NEA_LAST_PASSWORD_CHANGE);
        this.errorDetails = (String) attributes.get(NEA_ERROR_DETAILS);
        this.lastFailed = (Date) attributes.get(NEA_LAST_FAILED);
        final String jobTypeString = (String) attributes.get(NEA_JOB_TYPE);
        this.jobType = jobTypeString != null ? JobType.valueOf(jobTypeString) : null;
        this.mainJobId = (Long) attributes.get(NEA_MAIN_JOB_ID);
        this.neJobId = (Long) attributes.get(NEA_NE_JOB_ID);
        this.updateStatus =  NetworkElementAccountUpdateStatus.valueOf((String) attributes.get(NEA_UPDATE_STATUS));
    }

    public String getNetworkElementAccountId() {
        return networkElementAccountId;
    }

    public void setNetworkElementAccountId(String networkElementAccountId) {
        this.networkElementAccountId = networkElementAccountId;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void setCurrentUsername(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNextUsername() {
        return nextUsername;
    }

    public void setNextUsername(String nextUsername) {
        this.nextUsername = nextUsername;
    }

    public String getNextPassword() {
        return nextPassword;
    }

    public void setNextPassword(String nextPassword) {
        this.nextPassword = nextPassword;
    }

    public Date getLastPasswordChange() {
        return lastPasswordChange;
    }

    public void setLastPasswordChange(Date lastPasswordChange) {
        this.lastPasswordChange = lastPasswordChange;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public Date getLastFailed() {
        return lastFailed;
    }

    public void setLastFailed(Date lastFailed) {
        this.lastFailed = lastFailed;
    }


    public JobType getJobType() { return jobType; }

    public void setJobType(JobType jobType) { this.jobType = jobType; }

    public Long getMainJobId() { return mainJobId; }

    public void setMainJobId(Long mainJobId) { this.mainJobId = mainJobId; }

    public Long getNeJobId() { return neJobId; }

    public void setNeJobId(Long neJobId) { this.neJobId = neJobId; }

    public NetworkElementAccountUpdateStatus getUpdateStatus() {
        return updateStatus;
    }

    public void setUpdateStatus(NetworkElementAccountUpdateStatus updateStatus) {
        this.updateStatus = updateStatus;
    }

    @Override
    public String toString() {
        return "NetworkElementAccount{" +
                attributeToString(NEA_NEACCOUNT_ID, getNetworkElementAccountId()) +
                attributeToString(NEA_CURRENT_USER_NAME, getCurrentUsername()) +
                attributeToString(NEA_CURRENT_PASSWORD, getCurrentPassword()) +
                attributeToString(NEA_NEXT_USER_NAME, getNextUsername()) +
                attributeToString(NEA_NEXT_PASSWORD, getNextPassword()) +
                attributeToString(NEA_LAST_PASSWORD_CHANGE, getLastPasswordChange()) +
                attributeToString(NEA_ERROR_DETAILS, getErrorDetails()) +
                attributeToString(NEA_LAST_FAILED, getLastFailed()) +
                attributeToString(NEA_JOB_TYPE, getJobType()) +
                attributeToString(NEA_MAIN_JOB_ID, getMainJobId()) +
                attributeToString(NEA_NE_JOB_ID, getNeJobId()) +
                attributeToString(NEA_UPDATE_STATUS, getUpdateStatus()) +
                '}';
    }

    protected String attributeToString(final String attributeName, final Object attributeValue) {
        return  new StringBuilder()
                .append(attributeName)
                .append("=")
                .append(attributeValue)
                .append(",")
                .toString();
    }

}
