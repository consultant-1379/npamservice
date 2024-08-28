/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.security.npam.api.message;

import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType;

import java.io.Serializable;
import java.util.Objects;

public abstract class NodePamRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    protected static final long DEFAULT_NE_JOB_ID = -1;
    protected static final JobType DEFAULT_JOB_TYPE = null;
    protected static final Long DEFAULT_MAIN_JOB_ID = null;

    private String userId;
    private String requestId;
    private int retryNumber;
    private long neJobId;
    private String nodeName;
    private JobType jobType;
    private Long mainJobId;

    protected NodePamRequest(String userId, String requestId, long neJobId, final String nodeName, final JobType jobType, final Long mainJobId) {
        this.userId = userId;
        this.requestId = requestId;
        this.retryNumber = 0;
        this.neJobId = neJobId;
        this.nodeName =  nodeName;
        this.jobType = jobType;
        this.mainJobId = mainJobId;
    }

    public String getUserId() {
        return userId;
    }

    public String getRequestId() {
        return requestId;
    }

    public int getRetryNumber() {
        return retryNumber;
    }

    public void setRetryNumber(int retryNumber) {
        this.retryNumber = retryNumber;
    }


    public String getRecordingString(final String className) {
        return className + " { nodeName='" + nodeName + '\'' +
                '}';
    }

    public long getNeJobId() {
        return neJobId;
    }

    public String getNodeName() {
        return nodeName;
    }


    public JobType getJobType() {
        return jobType;
    }

    public Long getMainJobId() {
        return mainJobId;
    }

    public String commonValueString() {
        return
                "userId='" + userId + '\'' +
                ", requestId='" + requestId + '\'' +
                ", retryNumber=" + retryNumber +
                ", neJobId=" + neJobId +
                ", nodeName=" + nodeName;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodePamRequest that = (NodePamRequest) o;
        return retryNumber == that.retryNumber && neJobId == that.neJobId && Objects.equals(userId, that.userId) && Objects.equals(requestId, that.requestId) && Objects.equals(nodeName, that.nodeName) && jobType == that.jobType && Objects.equals(mainJobId, that.mainJobId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, requestId, retryNumber, neJobId, nodeName, jobType, mainJobId);
    }
}
