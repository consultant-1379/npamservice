/*
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 */
package com.ericsson.oss.services.security.npam.ejb.executor;

import com.ericsson.oss.services.security.npam.api.message.NodePamDisableRemoteManagementEventRequest;
import com.ericsson.oss.services.security.npam.ejb.handler.NodePamUpdateHandlerWithTx;
import org.slf4j.Logger;

import javax.inject.Inject;

public class NodePamDisableRemoteManagementEventRequestExecutor {
    @Inject
    private Logger logger;

    @Inject
    NodePamUpdateHandlerWithTx nodePamUpdateHandlerWithTx;

    public void processCommandRequest(final NodePamDisableRemoteManagementEventRequest nodePamDisableRemoteManagementEventRequest) {
        logger.info("processCommandRequest:: set all NetworkElementAccounts to DETACHED state for nodePamRequest={}", nodePamDisableRemoteManagementEventRequest);
        final String nodeName = nodePamDisableRemoteManagementEventRequest.getNodeName();
        nodePamUpdateHandlerWithTx.setAllNetworkElementAccountsToDetachedState(nodeName);
        logger.debug("processCommandRequest:: processCommandRequest STOP");
    }
}
