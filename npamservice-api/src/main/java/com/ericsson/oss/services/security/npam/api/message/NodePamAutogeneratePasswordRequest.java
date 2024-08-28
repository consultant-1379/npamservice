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

import java.util.Objects;

public class NodePamAutogeneratePasswordRequest extends NodePamRequest {
    private static final long serialVersionUID = 1L;
    private String muId;

    public NodePamAutogeneratePasswordRequest(String userId, String requestId, String nodeName, long neJobId, final String muId,
                                              final JobType jobType, final Long mainJobId) {
        super(userId, requestId, neJobId, nodeName, jobType, mainJobId);
        this.muId = muId;
    }


    public String getRecordingInfo() {
        return getRecordingString(NodePamAutogeneratePasswordRequest.class.getCanonicalName());
    }


    public String getMuId() {
        return muId;
    }

    public void setMuId(String muId) {
        this.muId = muId;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;
        NodePamAutogeneratePasswordRequest that = (NodePamAutogeneratePasswordRequest) o;
        return Objects.equals(muId, that.muId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), muId);
    }

    @Override
    public String toString() {
        return "NodePamAutogeneratePasswordRequest{" +
                commonValueString() +
                ", muId='" + muId + '\'' +
                '}';
    }
}
