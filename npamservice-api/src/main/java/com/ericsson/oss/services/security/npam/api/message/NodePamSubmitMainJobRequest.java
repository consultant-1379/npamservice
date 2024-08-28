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

import java.io.Serializable;
import java.util.Objects;

public class NodePamSubmitMainJobRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private long jobId;
    private int retryNumber;

    public NodePamSubmitMainJobRequest(final long jobId) {
        this.jobId = jobId;
        this.retryNumber = 0;
    }

    public long getJobId() {
        return jobId;
    }

    public int getRetryNumber() {
        return retryNumber;
    }

    public void setRetryNumber(int retryNumber) {
        this.retryNumber = retryNumber;
    }

    @Override
    public String toString() {
        return "NodePamSubmitMainJob{" +
                ", jobId='" + jobId + '\'' +
                ", retryNumber=" + retryNumber +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodePamSubmitMainJobRequest that = (NodePamSubmitMainJobRequest) o;
        return jobId == that.jobId && retryNumber == that.retryNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, retryNumber);
    }
}
