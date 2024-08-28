/*
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 */
package com.ericsson.oss.services.security.npam.ejb.executor;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.CHECK_IF_REMOTE_MANAGEMENT_IS_TRUE;
import static com.ericsson.oss.services.security.npam.ejb.handler.FdnUtility.getSubjectNameToBeSet;
import static com.ericsson.oss.services.security.npam.ejb.handler.FdnUtility.isNodeSyncronized;

import java.io.UnsupportedEncodingException;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.ericsson.oss.services.security.npam.api.constants.NodePamEventOperation;
import com.ericsson.oss.services.security.npam.api.interfaces.JobConfigurationService;
import com.ericsson.oss.services.security.npam.api.message.NodePamUpdatePasswordRequest;
import com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccount;
import com.ericsson.oss.services.security.npam.ejb.exceptions.ExceptionFactory;
import com.ericsson.oss.services.security.npam.ejb.exceptions.NodePamError;
import com.ericsson.oss.services.security.npam.ejb.handler.NodePamEventSenderWithTx;
import com.ericsson.oss.services.security.npam.ejb.handler.NodePamFunctionalityCheckerWithTx;
import com.ericsson.oss.services.security.npam.ejb.handler.NodePamUpdateHandlerWithTx;
import com.ericsson.oss.services.security.npam.ejb.listener.NodePamEndUpdateOperationMapManager;
import com.ericsson.oss.services.security.npam.ejb.services.NodePamCredentialManager;
import com.ericsson.oss.services.security.npam.ejb.services.NodePamEncryptionManager;
import com.ericsson.oss.services.security.npam.ejb.utility.TbacEvaluation;

public class NodePamUpdatePasswordRequestExecutor {

    @Inject
    NodePamFunctionalityCheckerWithTx nodePamFunctionalityCheckerWithTx;

    @Inject
    NodePamEventSenderWithTx nodePamEventSenderWithTx;

    @Inject
    NodePamUpdateHandlerWithTx nodePamUpdateHandlerWithTx;

    @Inject
    private Logger logger;

    @Inject
    NodePamEndUpdateOperationMapManager nodePamEndUpdateOperationMapManager;

    @Inject
    TbacEvaluation tbacEvaluation;

    @Inject
    ExceptionFactory exceptionFactory;

    @Inject
    JobConfigurationService jobConfigurationService;

    @Inject
    NodePamCredentialManager nodePamCredentialManager;

    @Inject
    NodePamEncryptionManager nodePamEncryptionManager;


    @SuppressWarnings({ "squid:S3776" })
    public void processCommandRequest(final String requestId, final NodePamUpdatePasswordRequest nodePamUpdatePasswordRequest)
            throws UnsupportedEncodingException {
        logger.debug("NodePam::NodePamUpdatePasswordRequestExecutor processCommandRequest START");
        final String nodeName = nodePamUpdatePasswordRequest.getNodeName();
        final long neJobId = nodePamUpdatePasswordRequest.getNeJobId();
        final String muId = nodePamUpdatePasswordRequest.getMuId();
        String nextUserNameEncrypted = nodePamUpdatePasswordRequest.getNextUser();
        final String nextPasswordEncrypted = nodePamUpdatePasswordRequest.getNextPasswd();

        //update NPamNEJob status to RUNNING
        jobConfigurationService.updateNeJobStateToRunning(neJobId);

        //Check TBAC
        if (!tbacEvaluation.getNodePermission(nodePamUpdatePasswordRequest.getUserId(), nodeName)) {
            throw exceptionFactory.createValidationException(NodePamError.TBAC_SINGLE_NODE_ACCESS_DENIED, nodeName);
        }

        //Get syncStatus
        final String syncStatus = nodePamFunctionalityCheckerWithTx.getCmFunctionSyncStatus(nodeName);
        logger.debug("processCommandRequest:: nodeName={} syncStatus={}", nodeName, syncStatus);
        //Check if node is synchronized
        if (!isNodeSyncronized(syncStatus)) {
            throw exceptionFactory.createValidationException(NodePamError.NOT_SYNCHRONIZED_SYNC_STATUS, syncStatus, nodeName);
        }

        final NodeInfo nodeInfo = nodePamFunctionalityCheckerWithTx.validateAndReturnMaintenanceUsers(nodeName, muId, CHECK_IF_REMOTE_MANAGEMENT_IS_TRUE);
        final ManagedObjectInfo maintenanceUserManagedObjectInfo = nodeInfo.getSingleMaintenanceUserManagedObjectInfo();

        //Check credentials
        if (nextPasswordEncrypted == null) {
            throw exceptionFactory.createValidationException(NodePamError.NULL_PASSWORD_FOR_NODE, nodeName);
        }

        if (nextUserNameEncrypted == null) {
            final NetworkElementAccount networkElementAccount = nodePamFunctionalityCheckerWithTx.getNetworkElementAccount(nodeName, muId);
            if (networkElementAccount != null) {
                nextUserNameEncrypted = networkElementAccount.getCurrentUsername();
            }
            if (nextUserNameEncrypted == null) {
                nextUserNameEncrypted = nodePamEncryptionManager.encryptPassword(nodePamCredentialManager.generateUserName(nodeName));
            }
        }

        //Create or update NetworkElementAccount with status ONGOING
        nodePamUpdateHandlerWithTx.createOrUpdateNetworkElementAccount(nodeName, muId, nextUserNameEncrypted, nextPasswordEncrypted, nodePamUpdatePasswordRequest.getJobType(), nodePamUpdatePasswordRequest.getMainJobId(), nodePamUpdatePasswordRequest.getNeJobId());

        //update status map in NodePamEndUpdateOperationMap without using topic to avoid concurrency problem
        logger.debug("processCommandRequest:: update status inside NodePamEndUpdateOperationMap");
        nodePamEndUpdateOperationMapManager.setStatusOngoingForJob(requestId, nodeName);
        
        //send message to mediation
        logger.info("processCommandRequest:: send message to mediation");
        nodePamEventSenderWithTx.createAndSendNodePamEvent(requestId, nodeName, getSubjectNameToBeSet(maintenanceUserManagedObjectInfo), nextUserNameEncrypted, nextPasswordEncrypted,
                neJobId, NodePamEventOperation.MODIFY, maintenanceUserManagedObjectInfo);

        //poll status map inside NodePamEndUpdateOperationMap
        nodePamEndUpdateOperationMapManager.pollRequestStatusForJob(nodeName);

        logger.debug("processCommandRequest:: processCommandRequest STOP");
    }
}
