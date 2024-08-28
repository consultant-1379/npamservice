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

import com.ericsson.oss.services.security.npam.api.interfaces.JobConfigurationService;
import com.ericsson.oss.services.security.npam.api.constants.NodePamEventOperation;
import com.ericsson.oss.services.security.npam.api.message.NodePamAutogeneratePasswordRequest;
import com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccount;
import com.ericsson.oss.services.security.npam.ejb.exceptions.ExceptionFactory;
import com.ericsson.oss.services.security.npam.ejb.exceptions.NodePamError;
import com.ericsson.oss.services.security.npam.ejb.handler.NodePamEventSenderWithTx;
import com.ericsson.oss.services.security.npam.ejb.handler.NodePamFunctionalityCheckerWithTx;
import com.ericsson.oss.services.security.npam.ejb.handler.NodePamUpdateHandlerWithTx;
import com.ericsson.oss.services.security.npam.ejb.listener.NodePamEndUpdateOperationMapManager;
import com.ericsson.oss.services.security.npam.ejb.services.NodePamCredentialManager;
import java.io.UnsupportedEncodingException;

import com.ericsson.oss.services.security.npam.ejb.services.NodePamEncryptionManager;
import com.ericsson.oss.services.security.npam.ejb.utility.TbacEvaluation;
import org.slf4j.Logger;

import javax.inject.Inject;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.CHECK_IF_REMOTE_MANAGEMENT_IS_TRUE;
import static com.ericsson.oss.services.security.npam.ejb.handler.FdnUtility.getSubjectNameToBeSet;
import static com.ericsson.oss.services.security.npam.ejb.handler.FdnUtility.isNodeSyncronized;

public class NodePamAutogeneratePasswordRequestExecutor {
    @Inject
    private Logger logger;

    @Inject
    NodePamFunctionalityCheckerWithTx nodePamFunctionalityCheckerWithTx;

    @Inject
    NodePamUpdateHandlerWithTx nodePamUpdateHandlerWithTx;

    @Inject
    NodePamEventSenderWithTx nodePamEventSenderWithTx;

    @Inject
    NodePamEndUpdateOperationMapManager nodePamEndUpdateOperationMapManager;

    @Inject
    NodePamCredentialManager nodePamCredentialManager;

    @Inject
    NodePamEncryptionManager nodePamEncryptionManager;

    @Inject
    ExceptionFactory exceptionFactory;

    @Inject
    JobConfigurationService jobConfigurationService;

    @Inject
    TbacEvaluation tbacEvaluation;

    @SuppressWarnings({"squid:S3776"})
    public void processCommandRequest(final String requestId, final NodePamAutogeneratePasswordRequest nodePamAutogeneratePasswordRequest) throws UnsupportedEncodingException {
        logger.debug("processCommandRequest:: processCommandRequest START");
        final String nodeName = nodePamAutogeneratePasswordRequest.getNodeName();
        final long neJobId = nodePamAutogeneratePasswordRequest.getNeJobId();
        final String muId = nodePamAutogeneratePasswordRequest.getMuId();

        //update NPamNEJob status to RUNNING
        jobConfigurationService.updateNeJobStateToRunning(neJobId);

        //Check TBAC
        if (!tbacEvaluation.getNodePermission(nodePamAutogeneratePasswordRequest.getUserId(), nodeName)) {
            throw exceptionFactory.createValidationException(NodePamError.TBAC_SINGLE_NODE_ACCESS_DENIED, nodeName);
        }

        //Get syncStatus
        String syncStatus = nodePamFunctionalityCheckerWithTx.getCmFunctionSyncStatus(nodeName);
        logger.debug("processCommandRequest:: nodeName={} syncStatus={}", nodeName, syncStatus);
        //Check if node is synchronized
        if (!isNodeSyncronized(syncStatus)) {
            throw exceptionFactory.createValidationException(NodePamError.NOT_SYNCHRONIZED_SYNC_STATUS, syncStatus, nodeName);
        }

        NodeInfo nodeInfo = nodePamFunctionalityCheckerWithTx.validateAndReturnMaintenanceUsers(nodeName, muId, CHECK_IF_REMOTE_MANAGEMENT_IS_TRUE);
        final ManagedObjectInfo maintenanceUserManagedObjectInfo = nodeInfo.getSingleMaintenanceUserManagedObjectInfo();

        //Recover info from NetworkElementAccount
        String currentUserNameEncrypted = null;
        final NetworkElementAccount networkElementAccount  = nodePamFunctionalityCheckerWithTx.getNetworkElementAccount(nodeName, muId);
        if (networkElementAccount != null) {
            currentUserNameEncrypted = networkElementAccount.getCurrentUsername();
        }

        // generate and encrypt nextUserName
       String nextUserName = null;
        String nextUserNameEncrypted = null;
        if (currentUserNameEncrypted != null) {
            nextUserName = nodePamEncryptionManager.decryptPassword(currentUserNameEncrypted);
            nextUserNameEncrypted = currentUserNameEncrypted;
        } else {
            nextUserName = nodePamCredentialManager.generateUserName(nodeName);
            nextUserNameEncrypted = nodePamEncryptionManager.encryptPassword(nextUserName);
        }

        // generate and encrypt nextPassword
        final String nextPassword = nodePamCredentialManager.generateCredentialString(nextUserName);
        final String nextPasswordEncrypted = nodePamEncryptionManager.encryptPassword(nextPassword);

        //Create or update NetworkElementAccount with status ONGOING
        nodePamUpdateHandlerWithTx.createOrUpdateNetworkElementAccount(nodeName, muId, nextUserNameEncrypted, nextPasswordEncrypted, nodePamAutogeneratePasswordRequest.getJobType(), nodePamAutogeneratePasswordRequest.getMainJobId(), nodePamAutogeneratePasswordRequest.getNeJobId());

        //update status map in NodePamEndUpdateOperationMap without using topic to avoid concurrency problem
        logger.debug("processCommandRequest:: update status inside NodePamEndUpdateOperationMap");
        nodePamEndUpdateOperationMapManager.setStatusOngoingForJob(requestId, nodeName);

        //send message to mediation
        logger.info("processCommandRequest:: send message to mediation");
        nodePamEventSenderWithTx.createAndSendNodePamEvent(requestId, nodeName, getSubjectNameToBeSet(maintenanceUserManagedObjectInfo), nextUserNameEncrypted, nextPasswordEncrypted, neJobId, NodePamEventOperation.MODIFY, maintenanceUserManagedObjectInfo);

        //poll status map inside NodePamEndUpdateOperationMap
        nodePamEndUpdateOperationMapManager.pollRequestStatusForJob(nodeName);

        logger.debug("processCommandRequest:: processCommandRequest STOP");
    }
}
