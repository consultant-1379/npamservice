/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.security.npam.ejb.listener;

import com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccountUpdateStatus;
import com.ericsson.oss.services.security.npam.api.message.NodePamEndUpdateOperation;
import com.ericsson.oss.services.security.npam.ejb.exceptions.ExceptionFactory;
import com.ericsson.oss.services.security.npam.ejb.exceptions.NodePamError;
import org.slf4j.Logger;

import javax.inject.Inject;

import static com.ericsson.oss.services.security.npam.api.message.NodePamEndUpdateOperation.getKeyForEvent;
import static com.ericsson.oss.services.security.npam.api.message.NodePamEndUpdateOperation.getKeyForJob;
import static com.ericsson.oss.services.security.npam.ejb.utility.ThreadSuspend.waitFor;

public class NodePamEndUpdateOperationMapManager {
    @Inject
    NodePamEndUpdateOperationMap nodePamEndUpdateOperationMap;

    @Inject
    private Logger logger;

    @Inject
    ExceptionFactory exceptionFactory;

    public static final long POLLING_SLEEP_TIME = 200;
    public static final int POLLING_CYCLE_NUM = 5 * 120; //2 min

    // this is called from NodePamEndUpdateOperationListener
    public void setStatus(NodePamEndUpdateOperation nodePamEndUpdateOperation) {
        nodePamEndUpdateOperationMap.setStatus(nodePamEndUpdateOperation);
    }

    public void setStatusOngoingForEvent(final String requestId, final String nodeName) {
        final String key = getKeyForEvent(nodeName);
        NodePamEndUpdateOperation nodePamEndUpdateOperation = new NodePamEndUpdateOperation(requestId, nodeName, NetworkElementAccountUpdateStatus.ONGOING.name(), key);
        nodePamEndUpdateOperationMap.setStatus(nodePamEndUpdateOperation);
    }

    public NodePamEndUpdateOperation pollRequestStatusForEvent(final String nodeName) {
        final String key = getKeyForEvent(nodeName);
        return pollRequestStatus(key);
    }

    public void setStatusOngoingForJob(final String requestId, final String nodeName) {
        final String key = getKeyForJob(nodeName);
        NodePamEndUpdateOperation nodePamEndUpdateOperation = new NodePamEndUpdateOperation(requestId, nodeName, NetworkElementAccountUpdateStatus.ONGOING.name(), key);
        nodePamEndUpdateOperationMap.setStatus(nodePamEndUpdateOperation);
    }

    public NodePamEndUpdateOperation pollRequestStatusForJob(final String nodeName) {
        final String key = getKeyForJob(nodeName);
        return pollRequestStatus(key);
    }

    @SuppressWarnings({"squid:S2142"})
    private NodePamEndUpdateOperation pollRequestStatus(final String key) {
        int cycles = 1;
        boolean terminated = false;
        logger.debug("pollRequestStatus:: START key={}", key);
        NodePamEndUpdateOperation nodePamEndUpdateOperation = null;

        while (cycles <= POLLING_CYCLE_NUM && !terminated) {
            logger.debug("pollRequestStatus:: polling key={} cycles={}", key, cycles);
            cycles++;
            nodePamEndUpdateOperation = nodePamEndUpdateOperationMap.getStatus(key);
            terminated = isRequestTerminated(key, nodePamEndUpdateOperation);
            if (!terminated) {
                logger.debug("pollRequestStatus:: sleep {} msec", POLLING_SLEEP_TIME);
                waitFor(POLLING_SLEEP_TIME);
            }
        }

        logIfLongPolling(key, cycles);
        logger.debug("pollRequestStatus:: STOP key={}", key);
        return nodePamEndUpdateOperation;
    }

    private void logIfLongPolling(final String key, final int cycles) {
        if (cycles > (POLLING_CYCLE_NUM/2)) {
            logger.info("pollRequestStatus:: warning:: STOP key={} after cycles={}", key, cycles);
        }
    }

    private boolean isRequestTerminated(final String key, final NodePamEndUpdateOperation nodePamEndUpdateOperation) {
        logger.debug("isRequestTerminated:: key={} nodePamEndUpdateOperation={}", key, nodePamEndUpdateOperation);
        if (nodePamEndUpdateOperation != null) {
            final String status = nodePamEndUpdateOperation.getStatus();
            return NetworkElementAccountUpdateStatus.CONFIGURED.name().equals(status) || NetworkElementAccountUpdateStatus.FAILED.name().equals(status) || NetworkElementAccountUpdateStatus.DETACHED.name().equals(status);
        }
        return false;
    }

    public void checkStatus(NodePamEndUpdateOperation nodePamEndUpdateOperation, final String phase) {
        logger.debug("checkStatus nodePamEndUpdateOperation={}, phase={}", nodePamEndUpdateOperation, phase);
        final String status = nodePamEndUpdateOperation.getStatus();
        if (NetworkElementAccountUpdateStatus.FAILED.name().equals(status)) {
            logger.debug("checkStatus status={} so raise exception", status);
            throw exceptionFactory.createValidationException(NodePamError.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION_MEDIATION_ERROR, nodePamEndUpdateOperation.getErrorDetails(), phase);
        } else  if (NetworkElementAccountUpdateStatus.ONGOING.name().equals(status)) {
            logger.debug("checkStatus status={} so raise exception", status);
            throw exceptionFactory.createValidationException(NodePamError.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION_TIMEOUT, phase);
        }
    }
}