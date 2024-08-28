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

public class NodePamEndUpdateOperation implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String OPERATION_STARTED_FROM_JOB = "JOB";
    public static final String OPERATION_STARTED_FROM_EVENT = "EVENT";

    private String requestId;
    private String nodeName;
    private String status;
    private String errorDetails;
    private String key;

    @SuppressWarnings({"squid:S107"})
    public NodePamEndUpdateOperation(final String requestId, final String nodeName, final String status, final String key) {
        this.requestId = requestId;
        this.nodeName = nodeName;
        this.status = status;
        this.key = key;
    }

    public String getNodeName() {
        return nodeName;
    }
    public String getStatus() {
        return status;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getKey() {
        return key;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public static String getKeyForJob(final String nodeName) {
        return  nodeName + "_" + NodePamEndUpdateOperation.OPERATION_STARTED_FROM_JOB;
    }

    public static String getKeyForEvent(final String nodeName) {
        return  nodeName + "_" + NodePamEndUpdateOperation.OPERATION_STARTED_FROM_EVENT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodePamEndUpdateOperation that = (NodePamEndUpdateOperation) o;
        return Objects.equals(requestId, that.requestId) && Objects.equals(nodeName, that.nodeName) && Objects.equals(status, that.status) && Objects.equals(errorDetails, that.errorDetails) && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, nodeName, status, errorDetails, key);
    }

    @Override
    public String toString() {
        return "NodePamEndUpdateOperation{" +
                "requestId='" + requestId + '\'' +
                ", nodeName='" + nodeName + '\'' +
                ", status='" + status + '\'' +
                ", errorDetails='" + errorDetails + '\'' +
                ", key='" + key + '\'' +
                '}';
    }
}
