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
package com.ericsson.oss.services.security.npam.ejb.handler;


import com.ericsson.oss.services.security.npam.api.message.NodePamRequest;
import com.ericsson.oss.services.security.npam.ejb.database.availability.DatabaseAvailabilityChecker;
import com.ericsson.oss.services.security.npam.ejb.exceptions.ExceptionFactory;
import com.ericsson.oss.services.security.npam.ejb.message.NodePamQueueMessageSender;
import org.slf4j.Logger;

import javax.inject.Inject;

import static com.ericsson.oss.services.security.npam.ejb.handler.FdnUtility.isNodeSynchronizing;
import static com.ericsson.oss.services.security.npam.ejb.utility.ThreadSuspend.waitFor;

public class SendBackToQueueManager {

    @Inject
    private Logger logger;

    @Inject
    ExceptionFactory exceptionFactory;


    @Inject
    NodePamFunctionalityCheckerWithTx nodePamFunctionalityCheckerWithTx;

    @Inject
    NodePamQueueMessageSender nodePamUpdateRequestQueueSender;

    @Inject
    DatabaseAvailabilityChecker databaseAvailabilityChecker;

    private static final long ONE_SEC_IN_MILLISEC = 1000L;

    public static final int MESSAGES_MAX_RETRY_NUMBER_NODE_IS_SYNCHRONIZING = 300;
    public static final int MESSAGES_MAX_RETRY_NUMBER_DB_UNAVAILABLE = 60;

    public boolean waitNodeIsSynchronizing(final String requestId, final NodePamRequest nodePamRequest, final boolean isEventRequest) {
        //Get syncStatus
        final String nodeName = nodePamRequest.getNodeName();
        String syncStatus = nodePamFunctionalityCheckerWithTx.getCmFunctionSyncStatus(nodeName);
        logger.debug("waitNodeIsSynchronizing:: nodeName={} syncStatus={}", nodeName, syncStatus);

        //Check if node is synchronizing
        final int retryNumber = nodePamRequest.getRetryNumber();
        if (isNodeSynchronizing(syncStatus) && retryNumber < MESSAGES_MAX_RETRY_NUMBER_NODE_IS_SYNCHRONIZING) {
            logger.debug("waitNodeIsSynchronizing:: sleepOneSec:: sleep  1 sec");
            waitFor(ONE_SEC_IN_MILLISEC);
            nodePamRequest.setRetryNumber(retryNumber + 1);
            logger.info("waitNodeIsSynchronizing:: send back nodePamRequest message={} in queue cause nodeName={} syncStatus={}, retryNumber={}", nodePamRequest, nodeName, syncStatus, retryNumber);
            sendMessage(requestId, nodePamRequest, isEventRequest);
            return true;
        }
        return false;
    }

    public boolean checkIfNodePamRequestCanBeProcessed(final String requestId, final NodePamRequest nodePamRequest, final boolean isEventRequest) {
        final int retryNumber = nodePamRequest.getRetryNumber();
        boolean isAvailable = databaseAvailabilityChecker.isAvailable();
        if (!databaseAvailabilityChecker.isAvailable()) {
            if (retryNumber < MESSAGES_MAX_RETRY_NUMBER_DB_UNAVAILABLE) {
                logger.debug("checkIfNodePamRequestCanBeProcessed:: sleepOneSec:: sleep  1 sec");
                waitFor(ONE_SEC_IN_MILLISEC);
                nodePamRequest.setRetryNumber(retryNumber + 1);
                logger.info("checkIfNodePamRequestCanBeProcessed:: send back nodePamRequest message={} in queue cause isAvailable={}, retryNumber={}", nodePamRequest, isAvailable, retryNumber);
                sendMessage(requestId, nodePamRequest, isEventRequest);
            } else {
                logger.error("checkIfNodePamRequestCanBeProcessed:: nodePamRequest message={} DISCARDED CAUSE isAvailable={}, retryNumber={}", nodePamRequest, isAvailable, retryNumber);
            }
            return false;
        }
        return true;
    }


    private void sendMessage( final String requestId, final NodePamRequest nodePamRequest, final boolean isEventRequest) {
        if (isEventRequest) {
            nodePamUpdateRequestQueueSender.sendEventExecutorMessage(requestId, nodePamRequest);
        } else {
            nodePamUpdateRequestQueueSender.sendJobExecutorMessage(requestId, nodePamRequest);
        }
    }
}
