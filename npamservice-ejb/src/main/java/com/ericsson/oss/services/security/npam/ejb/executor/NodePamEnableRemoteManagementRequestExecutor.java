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
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult;
import com.ericsson.oss.services.security.npam.api.message.NodePamEnableRemoteManagementRequest;
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus;
import com.ericsson.oss.services.security.npam.ejb.exceptions.ExceptionFactory;
import com.ericsson.oss.services.security.npam.ejb.exceptions.NodePamError;
import com.ericsson.oss.services.security.npam.ejb.handler.NodePamEventSenderWithTx;
import com.ericsson.oss.services.security.npam.ejb.handler.NodePamFunctionalityCheckerWithTx;
import com.ericsson.oss.services.security.npam.ejb.listener.NodePamEndUpdateOperationMapManager;
import com.ericsson.oss.services.security.npam.ejb.utility.TbacEvaluation;
import org.slf4j.Logger;

import javax.inject.Inject;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.*;
import static com.ericsson.oss.services.security.npam.ejb.exceptions.NodePamError.WARNING_MESSAGE_SKIPPED_REMOTE_MANAGEMENT_TRUE;
import static com.ericsson.oss.services.security.npam.ejb.handler.FdnUtility.isNodeSyncronized;

public class NodePamEnableRemoteManagementRequestExecutor {
    @Inject
    private Logger logger;

    @Inject
    NodePamFunctionalityCheckerWithTx nodePamFunctionalityCheckerWithTx;

    @Inject
    NodePamEventSenderWithTx nodePamEventSenderWithTx;

    @Inject
    NodePamEndUpdateOperationMapManager nodePamEndUpdateOperationMapManager;

    @Inject
    ExceptionFactory exceptionFactory;

    @Inject
    JobConfigurationService jobConfigurationService;

    @Inject
    TbacEvaluation tbacEvaluation;

    @Inject
    NodePamConfigStatus nodePamConfigStatus;

    @SuppressWarnings({"squid:S3776"})
    public void processCommandRequest(final String requestId, final NodePamEnableRemoteManagementRequest nodePamEnableRemoteManagementRequest) {
        logger.debug("processCommandRequest:: processCommandRequest START");
        final String nodeName = nodePamEnableRemoteManagementRequest.getNodeName();
        final long neJobId = nodePamEnableRemoteManagementRequest.getNeJobId();

        //update NPamNEJob status to RUNNING
        jobConfigurationService.updateNeJobStateToRunning(neJobId);

        //Check TBAC
        if (!tbacEvaluation.getNodePermission(nodePamEnableRemoteManagementRequest.getUserId(), nodeName)) {
            throw exceptionFactory.createValidationException(NodePamError.TBAC_SINGLE_NODE_ACCESS_DENIED, nodeName);
        }

        //Get syncStatus
        final String syncStatus = nodePamFunctionalityCheckerWithTx.getCmFunctionSyncStatus(nodeName);
        logger.debug("processCommandRequest:: nodeName={} syncStatus={}", nodeName, syncStatus);
        //Check if node is synchronized
        if (!isNodeSyncronized(syncStatus)) {
            throw exceptionFactory.createValidationException(NodePamError.NOT_SYNCHRONIZED_SYNC_STATUS, syncStatus, nodeName);
        }

        final ManagedObjectInfo maintenanceUserSecurityManagedObjectInfo = nodePamFunctionalityCheckerWithTx.validateMaintenanceUserSecurityFdnByNodeName(nodeName);
        final Boolean remoteManagementValue = (Boolean) maintenanceUserSecurityManagedObjectInfo.getAttributes().get(MAINTENANCE_USER_SECURITY_REMOTE_MANAGEMENT_ATTRIBUTE);
        if (remoteManagementValue == null) {
            throw exceptionFactory.createValidationException(NodePamError.REMOTE_MANAGEMENT_VALUE_NULL);
        }

        // if false execute else skip
        if (!remoteManagementValue.booleanValue()) {

            //update status map in NodePamEndUpdateOperationMap without using topic to avoid concurrency problem
            logger.debug("processCommandRequest:: update status inside NodePamEndUpdateOperationMap");
            nodePamEndUpdateOperationMapManager.setStatusOngoingForJob(requestId, nodeName);

            //send message to mediation
            logger.info("processCommandRequest:: send message to mediation");
            nodePamEventSenderWithTx.createAndSendNodePamEventForRemoteManagement(requestId, nodeName, neJobId, maintenanceUserSecurityManagedObjectInfo, REMOTE_MANAGEMENT_TRUE, getRestrictMaintenanceUser());

            //poll status map inside NodePamEndUpdateOperationMap
            nodePamEndUpdateOperationMapManager.pollRequestStatusForJob(nodeName);
            logger.debug("processCommandRequest:: processCommandRequest STOP");
        } else {
            logger.info("processCommandRequest:: NOTHING TO DO for nodePamRequest={} cause remoteManagement value={} for FDN={}", nodePamEnableRemoteManagementRequest, remoteManagementValue, maintenanceUserSecurityManagedObjectInfo.getFdn());
            jobConfigurationService.updateNeJobStateToCompleted(nodePamEnableRemoteManagementRequest.getNeJobId(), JobResult.SKIPPED.getJobResult(), WARNING_MESSAGE_SKIPPED_REMOTE_MANAGEMENT_TRUE);
        }
    }

    //Note: this value is passed !=null ONLY when jobType=CREATE_NE_ACCOUNT and NpamConfig restrict_maintenance_user=enabled
    private Boolean getRestrictMaintenanceUser() {
        if (nodePamConfigStatus.isRestrictMaintenanceUserEnabled()) {
            return Boolean.TRUE;
        }
        return RESTRICT_MAINTENANCE_USER_NULL;
    }
}
