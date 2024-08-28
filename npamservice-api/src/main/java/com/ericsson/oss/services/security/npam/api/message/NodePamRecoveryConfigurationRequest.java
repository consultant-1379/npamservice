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

public class NodePamRecoveryConfigurationRequest extends NodePamRequest {
    private static final long serialVersionUID = 1L;

    public NodePamRecoveryConfigurationRequest(String userId, String requestId, String nodeName, long neJobId,
                                               final JobType jobType, final Long mainJobId) {
        super(userId, requestId, neJobId, nodeName, jobType, mainJobId);
    }

    public String getRecordingInfo() {
        return getRecordingString(NodePamRecoveryConfigurationRequest.class.getCanonicalName());
    }

    @Override
    public String toString() {
        return "NodePamRecoveryConfigurationRequest{" +
                commonValueString() +
                '}';
    }
}
