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

package com.ericsson.oss.services.security.npam.api.message;


import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType;

import java.util.Objects;

public class NodePamUpdatePasswordRequest extends NodePamRequest {
    private static final long serialVersionUID = 1L;
    private String muId;
    private String nextUser;
    private String nextPasswd;

    private static final String OBSCURED_STRING = "***********";

    public NodePamUpdatePasswordRequest(final String userId, final String requestId, final String nodeName, final long neJobId, final String muId,
                                        final JobType jobType, final Long mainJobId) {
        super(userId, requestId, neJobId, nodeName, jobType, mainJobId);
        this.muId = muId;
    }

    public String getMuId() {
        return muId;
    }

    /**
     * @return the nextUser
     */
    public String getNextUser() {
        return nextUser;
    }

    /**
     * @param nextUser
     *            the nextUser to set
     */
    public void setNextUser(final String nextUser) {
        this.nextUser = nextUser;
    }

    /**
     * @return the nextPasswd
     */
    public String getNextPasswd() {
        return nextPasswd;
    }

    /**
     * @param nextPasswd
     *            the nextPasswd to set
     */
    public void setNextPasswd(final String nextPasswd) {
        this.nextPasswd = nextPasswd;
    }


    public String getRecordingInfo() {
        return getRecordingString(NodePamUpdatePasswordRequest.class.getCanonicalName());
    }

    @Override
    public String toString() {
        return "NodePamUpdatePasswordRequest{" +
                commonValueString() + "muId=" + muId + '\'' + ", nextUser=" + OBSCURED_STRING + '\'' + ", nextPasswd=" +OBSCURED_STRING + '\'' +
                '}';
    }

    public String toStringDebug() {
        return "NodePamUpdatePasswordRequest{" +
                commonValueString() + "muId=" + muId + '\'' + ", nextUser=" + nextUser + '\'' + ", nextPasswd= ********" + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        NodePamUpdatePasswordRequest that = (NodePamUpdatePasswordRequest) o;
        return Objects.equals(muId, that.muId) && Objects.equals(nextUser, that.nextUser) && Objects.equals(nextPasswd, that.nextPasswd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), muId, nextUser, nextPasswd);
    }
}
