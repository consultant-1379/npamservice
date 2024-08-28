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


public class NodePamEnableRemoteManagementEventRequest extends NodePamRequest {
    private static final long serialVersionUID = 1L;

    public NodePamEnableRemoteManagementEventRequest(String userId, String requestId, String nodeName) {
        super(userId, requestId, DEFAULT_NE_JOB_ID, nodeName, DEFAULT_JOB_TYPE, DEFAULT_MAIN_JOB_ID);
    }

    public String getRecordingInfo() {
        return getRecordingString(NodePamEnableRemoteManagementEventRequest.class.getCanonicalName());
    }

    @Override
    public String toString() {
        return "NodePamEnableRemoteManagementEventRequest{" +
                commonValueString() +
                '}';
    }
}
