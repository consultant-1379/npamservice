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

public abstract class NPamAbstractJob implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String JOB_STATE = "state";
    public static final String JOB_RESULT = "result";
    public static final String JOB_START_TIME = "startTime";
    public static final String JOB_END_TIME = "endTime";
    public static final String JOB_CREATION_TIME = "creationTime";
    public static final String JOB_ERROR_DETAILS = "errorDetails";

    private JobState state;
    private JobResult result;
    private Date startTime;
    private Date endTime;
    private Date creationTime;
    private String errorDetails;

    public JobState getState() {
        return state;
    }

    public void setState(JobState state) {
        this.state = state;
    }

    public JobResult getResult() {
        return result;
    }

    public void setResult(JobResult result) {
        this.result = result;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    protected String appendCommonAttributeToString() {
        return
                attributeToString(JOB_STATE, getState()) +
                attributeToString(JOB_RESULT, getResult()) +
                attributeToString(JOB_START_TIME, getStartTime()) +
                attributeToString(JOB_END_TIME, getEndTime()) +
                attributeToString(JOB_CREATION_TIME, getCreationTime()) +
                attributeToString(JOB_ERROR_DETAILS, getErrorDetails());
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
