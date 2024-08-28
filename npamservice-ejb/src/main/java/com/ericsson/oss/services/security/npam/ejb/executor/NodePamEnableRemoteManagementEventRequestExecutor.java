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

import com.ericsson.oss.services.security.npam.api.constants.NodePamEventOperation;
import com.ericsson.oss.services.security.npam.api.message.NodePamEnableRemoteManagementEventRequest;
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus;
import com.ericsson.oss.services.security.npam.ejb.exceptions.ExceptionFactory;
import com.ericsson.oss.services.security.npam.ejb.exceptions.NodePamError;
import com.ericsson.oss.services.security.npam.ejb.handler.NodePamEventSenderWithTx;
import com.ericsson.oss.services.security.npam.ejb.handler.NodePamFunctionalityCheckerWithTx;
import com.ericsson.oss.services.security.npam.ejb.handler.NodePamUpdateHandlerWithTx;
import com.ericsson.oss.services.security.npam.ejb.listener.NodePamEndUpdateOperationMapManager;
import com.ericsson.oss.services.security.npam.ejb.services.NodePamCredentialManager;
import java.io.UnsupportedEncodingException;

import com.ericsson.oss.services.security.npam.ejb.services.NodePamEncryptionManager;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.List;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_MO;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NULL_MUID;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NULL_PASSWORD;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NULL_USERNAME;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.SUBJECT_NAME_CBRS_DOMAIN;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.CHECK_IF_REMOTE_MANAGEMENT_IS_TRUE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MUID_ONE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MUID_TWO;
import static com.ericsson.oss.services.security.npam.ejb.handler.FdnUtility.getSubjectNameToBeSet;
import static com.ericsson.oss.services.security.npam.ejb.handler.FdnUtility.isNodeSyncronized;
import static com.ericsson.oss.services.security.npam.ejb.utility.ManagedObjectUtility.filterForMoId;
import static com.ericsson.oss.services.security.npam.ejb.utility.ManagedObjectUtility.removeMoIdFromList;

public class NodePamEnableRemoteManagementEventRequestExecutor {
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
    NodePamConfigStatus nodePamConfigStatus;

    public void processCommandRequest(final String requestId, final NodePamEnableRemoteManagementEventRequest nodePamEnableRemoteManagementEventRequest) throws UnsupportedEncodingException {
        logger.debug("processCommandRequest:: processCommandRequest START");
        final String nodeName = nodePamEnableRemoteManagementEventRequest.getNodeName();
        final long neJobId = nodePamEnableRemoteManagementEventRequest.getNeJobId();

        //Get syncStatus
        String syncStatus = nodePamFunctionalityCheckerWithTx.getCmFunctionSyncStatus(nodeName);
        logger.debug("processCommandRequest:: nodeName={} syncStatus={}", nodeName, syncStatus);
        //Check if node is synchronized
        if (!isNodeSyncronized(syncStatus)) {
            throw exceptionFactory.createValidationException(NodePamError.NOT_SYNCHRONIZED_SYNC_STATUS, syncStatus, nodeName);
        }

        NodeInfo nodeInfo = nodePamFunctionalityCheckerWithTx.validateAndReturnMaintenanceUsers(nodeName, NULL_MUID, !CHECK_IF_REMOTE_MANAGEMENT_IS_TRUE);
        final List<ManagedObjectInfo> maintenanceUserManagedObjectInfos = nodeInfo.getMaintenanceUserManagedObjectInfos();
        final String muId = MUID_ONE;
        NodePamEventOperation nodePamEventOperation = NodePamEventOperation.MODIFY;
        ManagedObjectInfo maintenanceUserManagedObjectInfo = filterForMoId(maintenanceUserManagedObjectInfos, muId);
        if (maintenanceUserManagedObjectInfo == null) {
            maintenanceUserManagedObjectInfo = createMaintenanceUser(nodeInfo.getUserIdentityManagedObjectInfo(), muId);
            nodePamEventOperation = NodePamEventOperation.CREATE;
        }

        // generate nextUserName and nextPassword
        final String nextUserName = nodePamCredentialManager.generateUserName(nodeName);
        final String nextPassword = nodePamCredentialManager.generateCredentialString(nextUserName);

        // encrypt nextUserName and nextPassword
        final String nextUserNameEncrypted = nodePamEncryptionManager.encryptPassword(nextUserName);
        final String nextPasswordEncrypted = nodePamEncryptionManager.encryptPassword(nextPassword);

        //Create or update NetworkElementAccount with status ONGOING
        nodePamUpdateHandlerWithTx.createOrUpdateNetworkElementAccount(nodeName, muId, nextUserNameEncrypted, nextPasswordEncrypted, nodePamEnableRemoteManagementEventRequest.getJobType(), nodePamEnableRemoteManagementEventRequest.getMainJobId(), nodePamEnableRemoteManagementEventRequest.getNeJobId());

        //update status map in NodePamEndUpdateOperationMap without using topic to avoid concurrency problem
        logger.debug("processCommandRequest:: update status inside NodePamEndUpdateOperationMap");
        nodePamEndUpdateOperationMapManager.setStatusOngoingForEvent(requestId, nodeName);

        //send message to mediation
        logger.info("processCommandRequest:: send message to mediation");
        nodePamEventSenderWithTx.createAndSendNodePamEvent(requestId, nodeName, getSubjectNameToBeSet(maintenanceUserManagedObjectInfo), nextUserNameEncrypted, nextPasswordEncrypted, neJobId,  nodePamEventOperation, maintenanceUserManagedObjectInfo);

        //poll status map inside NodePamEndUpdateOperationMap
        nodePamEndUpdateOperationMapManager.pollRequestStatusForEvent(nodeName);

        // here remove other entries
        List<ManagedObjectInfo> toBeDeletedMaintenanceUserManagedObjectInfos = removeMoIdFromList(maintenanceUserManagedObjectInfos, muId);
        for (ManagedObjectInfo toBeDeletedMaintenanceUserManagedObjectInfo:toBeDeletedMaintenanceUserManagedObjectInfos) {
            //update status map in NodePamEndUpdateOperationMap without using topic to avoid concurrency problem
            logger.debug("processCommandRequest:: update status inside NodePamEndUpdateOperationMap");
            nodePamEndUpdateOperationMapManager.setStatusOngoingForEvent(requestId, nodeName);

            //send message to mediation
            logger.info("processCommandRequest:: send message to mediation");
            nodePamEventSenderWithTx.createAndSendNodePamEvent(requestId, nodeName, getSubjectNameToBeSet(toBeDeletedMaintenanceUserManagedObjectInfo), NULL_USERNAME, NULL_PASSWORD, neJobId,  NodePamEventOperation.DELETE, toBeDeletedMaintenanceUserManagedObjectInfo);

            //poll status map inside NodePamEndUpdateOperationMap
            nodePamEndUpdateOperationMapManager.pollRequestStatusForEvent(nodeName);

        }

        if (nodePamConfigStatus.isCbrsDomainEnabled()) {
            manageCbrsDomain(requestId, nodePamEnableRemoteManagementEventRequest, nodeName, neJobId, nodeInfo);
        }

        logger.debug("processCommandRequest:: processCommandRequest STOP");
    }

    private void manageCbrsDomain(String requestId, NodePamEnableRemoteManagementEventRequest nodePamEnableRemoteManagementEventRequest, String nodeName, long neJobId, NodeInfo nodeInfo) {
        logger.debug("processCommandRequest:: manageCbrsDomain is_CBRS_DOMAIN_Enabled=true so create a new MaintenanceUser=2 with subjectName={}", SUBJECT_NAME_CBRS_DOMAIN);
        final String muId2 = MUID_TWO;

        ManagedObjectInfo maintenanceUserManagedObjectInfo = createMaintenanceUser(nodeInfo.getUserIdentityManagedObjectInfo(), muId2);
        NodePamEventOperation nodePamEventOperation = NodePamEventOperation.CREATE;

        //Create or update NetworkElementAccount with status ONGOING
        nodePamUpdateHandlerWithTx.createOrUpdateNetworkElementAccount(nodeName, muId2, NULL_USERNAME, NULL_PASSWORD, nodePamEnableRemoteManagementEventRequest.getJobType(), nodePamEnableRemoteManagementEventRequest.getMainJobId(), nodePamEnableRemoteManagementEventRequest.getNeJobId());

        //update status map in NodePamEndUpdateOperationMap without using topic to avoid concurrency problem
        logger.debug("processCommandRequest:: manageCbrsDomain update status inside NodePamEndUpdateOperationMap");
        nodePamEndUpdateOperationMapManager.setStatusOngoingForEvent(requestId, nodeName);

        //send message to mediation
        logger.info("processCommandRequest:: manageCbrsDomain send message to mediation");
        nodePamEventSenderWithTx.createAndSendNodePamEvent(requestId, nodeName, SUBJECT_NAME_CBRS_DOMAIN, NULL_USERNAME, NULL_PASSWORD, neJobId,  nodePamEventOperation, maintenanceUserManagedObjectInfo);

        //poll status map inside NodePamEndUpdateOperationMap
        nodePamEndUpdateOperationMapManager.pollRequestStatusForEvent(nodeName);
    }

    private ManagedObjectInfo createMaintenanceUser(final ManagedObjectInfo userIdentityManagedObjectInfo, final String muId) {
        final String fdn = userIdentityManagedObjectInfo.getFdn() + "," + MAINTENANCE_USER_MO + "=" +muId;
        return new ManagedObjectInfo(fdn, MAINTENANCE_USER_MO, userIdentityManagedObjectInfo.getNameSpace(), userIdentityManagedObjectInfo.getNameSpaceVersion());
    }

}
