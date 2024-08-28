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
/*
These are the implemented scenario:
* remoteManagement=true (cbrs disabled)
* 0) NEA1 present         CONFIGURED  OK   => CONFIGURED (untouched) and result=SKIPPED
* 1) NEA1 not present      na         OK   => CONFIGURED
* 2) NEA1 wrong           DETACHED     OK   => CONFIGURED
* 2) NEA1 wrong           FAILED      OK   => CONFIGURED with reuse if nextUserName and nextPassword present (otherwise new password)
* 3) extra MU entries     2 and 4     OK   => MUid=2 and MUid=4 removed
*
* remoteManagement=true (cbrs enabled)
* 1) NEA2 not present                 OK    => CONFIGURED
* 2) NEA2 wrong                       OK    => CONFIGURED
* 3) extra MU entries    2 and 4      OK    => MUid=4 removed
* 4) NEA1 + NEA2 not present          OK    => CONFIGURED
*
* remoteManagement=false
* 0) NEA1 + NEA2 present   DETACHED    OK  => DETACHED (untouched) and result=SKIPPED
* 1) NEA1 wrong            CONFIGURED OK  => DETACHED
* 2) NEA2 wrong            CONFIGURED OK  => DETACHED
* 3) NEA1 + NEA2 wrong     CONFIGURED OK  => DETACHED
* 4) NEA1 + NEA2 not present    na    OK  => NEA1 + NEA2 not present and result=SKIPPED
* */
package com.ericsson.oss.services.security.npam.ejb.executor;

import com.ericsson.oss.services.security.npam.api.constants.NodePamEventOperation;
import com.ericsson.oss.services.security.npam.api.interfaces.JobConfigurationService;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult;
import com.ericsson.oss.services.security.npam.api.message.NodePamEndUpdateOperation;
import com.ericsson.oss.services.security.npam.api.message.NodePamRecoveryConfigurationRequest;
import com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccount;
import com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccountUpdateStatus;
import com.ericsson.oss.services.security.npam.ejb.dao.DpsReadOperations;
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus;
import com.ericsson.oss.services.security.npam.ejb.exceptions.ExceptionFactory;
import com.ericsson.oss.services.security.npam.ejb.exceptions.NodePamError;
import com.ericsson.oss.services.security.npam.ejb.handler.NodePamEventSenderWithTx;
import com.ericsson.oss.services.security.npam.ejb.handler.NodePamFunctionalityCheckerWithTx;
import com.ericsson.oss.services.security.npam.ejb.handler.NodePamUpdateHandlerWithTx;
import com.ericsson.oss.services.security.npam.ejb.listener.NodePamEndUpdateOperationMapManager;
import com.ericsson.oss.services.security.npam.ejb.services.NodePamCredentialManager;
import com.ericsson.oss.services.security.npam.ejb.services.NodePamEncryptionManager;
import com.ericsson.oss.services.security.npam.ejb.utility.TbacEvaluation;
import org.slf4j.Logger;

import javax.inject.Inject;

import java.io.UnsupportedEncodingException;
import java.util.List;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.*;
import static com.ericsson.oss.services.security.npam.ejb.exceptions.NodePamError.WARNING_MESSAGE_SKIPPED_CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION_FALSE;
import static com.ericsson.oss.services.security.npam.ejb.exceptions.NodePamError.WARNING_MESSAGE_SKIPPED_CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION_TRUE;
import static com.ericsson.oss.services.security.npam.ejb.handler.FdnUtility.getSubjectNameToBeSet;
import static com.ericsson.oss.services.security.npam.ejb.handler.FdnUtility.isNodeSyncronized;
import static com.ericsson.oss.services.security.npam.ejb.utility.ManagedObjectUtility.filterForMoId;
import static com.ericsson.oss.services.security.npam.ejb.utility.ManagedObjectUtility.removeMoIdFromList;


public class NodePamRecoveryConfigurationRequestExecutor {

    @Inject
    NodePamFunctionalityCheckerWithTx nodePamFunctionalityCheckerWithTx;

    @Inject
    NodePamEventSenderWithTx nodePamEventSenderWithTx;

    @Inject
    private Logger logger;

    @Inject
    ExceptionFactory exceptionFactory;

    @Inject
    NodePamEndUpdateOperationMapManager nodePamEndUpdateOperationMapManager;


    @Inject
    JobConfigurationService jobConfigurationService;

    @Inject
    TbacEvaluation tbacEvaluation;

    @Inject
    private DpsReadOperations dpsReadOperations;

    @Inject
    NodePamCredentialManager nodePamCredentialManager;

    @Inject
    NodePamEncryptionManager nodePamEncryptionManager;

    @Inject
    NodePamUpdateHandlerWithTx nodePamUpdateHandlerWithTx;

    @Inject
    NodePamConfigStatus nodePamConfigStatus;

    public static final String PHASE1 = " (NEA1 recovery phase)";
    public static final String PHASE2 = " (extra NEA removal phase)";
    public static final String PHASE3 = " (NEA2 recovery phase)";

    protected static final long DEFAULT_NE_JOB_ID = -1;

    private int executedOperations = 0;

    @SuppressWarnings({"squid:S3776"})
    public void processCommandRequest(final String requestId, final NodePamRecoveryConfigurationRequest nodePamRecoveryConfigurationRequest) throws UnsupportedEncodingException {
        logger.debug("processCommandRequest:: processCommandRequest START");
        executedOperations = 0;
        final String nodeName = nodePamRecoveryConfigurationRequest.getNodeName();
        final long neJobId = nodePamRecoveryConfigurationRequest.getNeJobId();

        //update NPamNEJob status to RUNNING
        jobConfigurationService.updateNeJobStateToRunning(neJobId);

        //Check TBAC
        if (!tbacEvaluation.getNodePermission(nodePamRecoveryConfigurationRequest.getUserId(), nodeName)) {
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

        String warningMessage;
        if (remoteManagementValue.booleanValue()) {
            // HERE remoteManagementValue=true
            logger.info("processCommandRequest:: remoteManagement value={} for FDN={} so try to recreate MaintenanceUsers", remoteManagementValue, maintenanceUserSecurityManagedObjectInfo.getFdn());
            warningMessage = WARNING_MESSAGE_SKIPPED_CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION_TRUE;

            NodeInfo nodeInfo = nodePamFunctionalityCheckerWithTx.validateAndReturnMaintenanceUsers(nodeName, NULL_MUID, !CHECK_IF_REMOTE_MANAGEMENT_IS_TRUE);

            //here create/update, if necessary, maintenanceUser="1"
            manageMuIdOneIfNecessary(requestId, nodePamRecoveryConfigurationRequest, nodeName, nodeInfo);

            //here delete maintenanceUser != "1" (and !="2" if nodePamConfigStatus.isCbrsDomainEnabled())
            removeExtraMaintenanceUsers(requestId, nodeName, nodeInfo);

            //here recreate, if necessary, maintenanceUser="2"
            if (nodePamConfigStatus.isCbrsDomainEnabled()) {
                manageCbrsDomainIfNecessary(requestId, nodePamRecoveryConfigurationRequest, nodeName, nodeInfo);
            } else {
                deleteNetworkElementAccountIfNecesssary(nodeName, MUID_TWO);
            }
        } else {
            // HERE remoteManagementValue=false so mark detached, if necessary, networkElementAccount="1" and "2"
            logger.info("processCommandRequest:: remoteManagement value={} for FDN={} so mark all NetworkElementAccounts as DETACHED", remoteManagementValue, maintenanceUserSecurityManagedObjectInfo.getFdn());
            warningMessage = WARNING_MESSAGE_SKIPPED_CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION_FALSE;

            // mark detached networkElementAccount="1"
            setNetworkElementAccountToDetachedStateIfNecessary(nodePamRecoveryConfigurationRequest, nodeName, neJobId, MUID_ONE);
            // mark detached or delete networkElementAccount="2"
            if (nodePamConfigStatus.isCbrsDomainEnabled()) {
                setNetworkElementAccountToDetachedStateIfNecessary(nodePamRecoveryConfigurationRequest, nodeName, neJobId, MUID_TWO);
            } else {
                deleteNetworkElementAccountIfNecesssary(nodeName, MUID_TWO);
            }
        }

        if (nothingHasBeenDone()) {
            logger.info("processCommandRequest:: set jobResult=SKIPPED");
            jobConfigurationService.updateNeJobStateToCompleted(nodePamRecoveryConfigurationRequest.getNeJobId(), JobResult.SKIPPED.getJobResult(), warningMessage);
        } else {
            logger.info("processCommandRequest:: set jobResult=SUCCESS");
            jobConfigurationService.updateNeJobStateToCompleted(nodePamRecoveryConfigurationRequest.getNeJobId(), JobResult.SUCCESS.getJobResult(), null);
        }
        logger.debug("processCommandRequest:: processCommandRequest STOP");
    }

    private void setNetworkElementAccountToDetachedStateIfNecessary(NodePamRecoveryConfigurationRequest nodePamRecoveryConfigurationRequest, String nodeName, long neJobId, String muId) {
        final NetworkElementAccount networkElementAccount = dpsReadOperations.getNetworkElementAccount(nodeName, muId);
        if (networkElementAccount != null && !isAlreadyDetached(networkElementAccount)) {
            executedOperations++;
            nodePamUpdateHandlerWithTx.setNetworkElementAccountToDetachedState(nodeName, nodePamRecoveryConfigurationRequest.getMainJobId(), neJobId, nodePamRecoveryConfigurationRequest.getJobType(), muId);
        }
    }

    private void deleteNetworkElementAccountIfNecesssary( String nodeName, String muId) {
        final NetworkElementAccount networkElementAccount = dpsReadOperations.getNetworkElementAccount(nodeName, muId);
        if (networkElementAccount != null) {
            executedOperations++;
            nodePamUpdateHandlerWithTx.deleteNetworkElementAccount(nodeName, muId);
        }
    }

    private void manageMuIdOneIfNecessary(final String requestId, final NodePamRecoveryConfigurationRequest nodePamRecoveryConfigurationRequest,
                                          final String nodeName, NodeInfo nodeInfo
                               ) throws UnsupportedEncodingException {

        final String muId = MUID_ONE;
        logger.info("processCommandRequest::manageMuIdOneIfNecessary:: check muId={}", muId);
        NetworkElementAccount networkElementAccount = dpsReadOperations.getNetworkElementAccount(nodeName, muId);
        if (!isAlreadyConfigured(networkElementAccount)) {
            executedOperations++;
            logger.info("processCommandRequest::manageMuIdOneIfNecessary:: NOT isAlreadyConfigured networkElementAccount={}", networkElementAccount);
            final List<ManagedObjectInfo> maintenanceUserManagedObjectInfos = nodeInfo.getMaintenanceUserManagedObjectInfos();
            NodePamEventOperation nodePamEventOperation = NodePamEventOperation.MODIFY;
            ManagedObjectInfo maintenanceUserManagedObjectInfo = filterForMoId(maintenanceUserManagedObjectInfos, muId);
            if (maintenanceUserManagedObjectInfo == null) {
                maintenanceUserManagedObjectInfo = createMaintenanceUser(nodeInfo.getUserIdentityManagedObjectInfo(), muId);
                nodePamEventOperation = NodePamEventOperation.CREATE;
            }

            // if FAILED we try to reuse password
            String nextUserName = null;
            String nextPassword = null;
            String nextUserNameEncrypted = null;
            String nextPasswordEncrypted = null;
            if (isFailed(networkElementAccount) && existsNextUsernameAndNextPassword(networkElementAccount)) {
               logger.info("processCommandRequest::manageMuIdOneIfNecessary:: reuse nextUserNameEncrypted and nextPasswordEncrypted cause it was FAILED");
                nextUserNameEncrypted = networkElementAccount.getNextUsername();
                nextPasswordEncrypted = networkElementAccount.getNextPassword();
            } else {
                // generate nextUserName and nextPassword
                nextUserName = nodePamCredentialManager.generateUserName(nodeName);
                nextPassword = nodePamCredentialManager.generateCredentialString(nextUserName);

                // encrypt nextUserName and nextPassword
                nextUserNameEncrypted = nodePamEncryptionManager.encryptPassword(nextUserName);
                nextPasswordEncrypted = nodePamEncryptionManager.encryptPassword(nextPassword);
            }

            //Create or update NetworkElementAccount with status ONGOING
            nodePamUpdateHandlerWithTx.createOrUpdateNetworkElementAccount(nodeName, muId, nextUserNameEncrypted, nextPasswordEncrypted, nodePamRecoveryConfigurationRequest.getJobType(), nodePamRecoveryConfigurationRequest.getMainJobId(), nodePamRecoveryConfigurationRequest.getNeJobId());

            //update status map in NodePamEndUpdateOperationMap without using topic to avoid concurrency problem
            logger.debug("processCommandRequest::manageMuIdOneIfNecessary:: update status inside NodePamEndUpdateOperationMap");
            nodePamEndUpdateOperationMapManager.setStatusOngoingForEvent(requestId, nodeName);  //EMARDEP SE GLI PASSIAMO neJobId dobbiamo usare forJob

            //send message to mediation
            logger.info("processCommandRequest::manageMuIdOneIfNecessary:: send message to mediation");
            nodePamEventSenderWithTx.createAndSendNodePamEvent(requestId, nodeName, getSubjectNameToBeSet(maintenanceUserManagedObjectInfo), nextUserNameEncrypted, nextPasswordEncrypted, DEFAULT_NE_JOB_ID,  nodePamEventOperation, maintenanceUserManagedObjectInfo);

            //poll status map inside NodePamEndUpdateOperationMap
            checkStatus(nodePamEndUpdateOperationMapManager.pollRequestStatusForEvent(nodeName), PHASE1);
        } else {
            logger.info("processCommandRequest::manageMuIdOneIfNecessary:: isAlreadyConfigured networkElementAccount={}", networkElementAccount);
        }
    }

    private void removeExtraMaintenanceUsers(final String requestId, final String nodeName, NodeInfo nodeInfo)  {
        final List<ManagedObjectInfo> maintenanceUserManagedObjectInfos = nodeInfo.getMaintenanceUserManagedObjectInfos();

        // here remove other entries
        List<ManagedObjectInfo> toBeDeletedMaintenanceUserManagedObjectInfos = removeMoIdFromList(maintenanceUserManagedObjectInfos, MUID_ONE);
        if (nodePamConfigStatus.isCbrsDomainEnabled()) {
            toBeDeletedMaintenanceUserManagedObjectInfos = removeMoIdFromList(toBeDeletedMaintenanceUserManagedObjectInfos, MUID_TWO);
        }

        for (ManagedObjectInfo toBeDeletedMaintenanceUserManagedObjectInfo:toBeDeletedMaintenanceUserManagedObjectInfos) {
            executedOperations++;

            //update status map in NodePamEndUpdateOperationMap without using topic to avoid concurrency problem
            logger.debug("processCommandRequest::removeExtraMaintenanceUsers:: update status inside NodePamEndUpdateOperationMap");
            nodePamEndUpdateOperationMapManager.setStatusOngoingForEvent(requestId, nodeName);

            //send message to mediation
            logger.info("processCommandRequest::removeExtraMaintenanceUsers:: send message to mediation");
            nodePamEventSenderWithTx.createAndSendNodePamEvent(requestId, nodeName, getSubjectNameToBeSet(toBeDeletedMaintenanceUserManagedObjectInfo), NULL_USERNAME, NULL_PASSWORD, DEFAULT_NE_JOB_ID,  NodePamEventOperation.DELETE, toBeDeletedMaintenanceUserManagedObjectInfo);

            //poll status map inside NodePamEndUpdateOperationMap
            checkStatus(nodePamEndUpdateOperationMapManager.pollRequestStatusForEvent(nodeName), PHASE2);
        }

    }
    private void manageCbrsDomainIfNecessary(String requestId, NodePamRecoveryConfigurationRequest nodePamRecoveryConfigurationRequest,
                                             String nodeName, NodeInfo nodeInfo) {
        final String muId2 = MUID_TWO;
        logger.info("processCommandRequest::manageCbrsDomainIfNecessary:: check muId={}", muId2);
        NetworkElementAccount networkElementAccount = dpsReadOperations.getNetworkElementAccount(nodeName, muId2);

        if (!isAlreadyConfigured(networkElementAccount)) {
            // here remove entry "2"
            executedOperations++;
            logger.info("processCommandRequest::manageCbrsDomainIfNecessary:: NOT isAlreadyConfigured networkElementAccount={}", networkElementAccount);
            final List<ManagedObjectInfo> maintenanceUserManagedObjectInfos = nodeInfo.getMaintenanceUserManagedObjectInfos();
            ManagedObjectInfo toBeDeletedMaintenanceUserManagedObjectInfo = filterForMoId(maintenanceUserManagedObjectInfos, muId2);
            if (toBeDeletedMaintenanceUserManagedObjectInfo != null) {
                //update status map in NodePamEndUpdateOperationMap without using topic to avoid concurrency problem
                logger.debug("processCommandRequest::manageCbrsDomainIfNecessary:: update status inside NodePamEndUpdateOperationMap");
                nodePamEndUpdateOperationMapManager.setStatusOngoingForEvent(requestId, nodeName);

                //send message to mediation
                logger.info("processCommandRequest::manageCbrsDomainIfNecessary:: send message to mediation");
                nodePamEventSenderWithTx.createAndSendNodePamEvent(requestId, nodeName, getSubjectNameToBeSet(toBeDeletedMaintenanceUserManagedObjectInfo), NULL_USERNAME, NULL_PASSWORD, DEFAULT_NE_JOB_ID, NodePamEventOperation.DELETE, toBeDeletedMaintenanceUserManagedObjectInfo);

                //poll status map inside NodePamEndUpdateOperationMap
                checkStatus(nodePamEndUpdateOperationMapManager.pollRequestStatusForEvent(nodeName), PHASE3);
            }

            // here create entry "2"
            ManagedObjectInfo maintenanceUserManagedObjectInfo = createMaintenanceUser(nodeInfo.getUserIdentityManagedObjectInfo(), muId2);
            NodePamEventOperation nodePamEventOperation = NodePamEventOperation.CREATE;

            //Create or update NetworkElementAccount with status ONGOING
            nodePamUpdateHandlerWithTx.createOrUpdateNetworkElementAccount(nodeName, muId2, NULL_USERNAME, NULL_PASSWORD, nodePamRecoveryConfigurationRequest.getJobType(), nodePamRecoveryConfigurationRequest.getMainJobId(), nodePamRecoveryConfigurationRequest.getNeJobId());

            //update status map in NodePamEndUpdateOperationMap without using topic to avoid concurrency problem
            logger.debug("processCommandRequest::manageCbrsDomainIfNecessary update status inside NodePamEndUpdateOperationMap");
            nodePamEndUpdateOperationMapManager.setStatusOngoingForEvent(requestId, nodeName);

            //send message to mediation
            logger.info("processCommandRequest::manageCbrsDomainIfNecessary send message to mediation");
            nodePamEventSenderWithTx.createAndSendNodePamEvent(requestId, nodeName, SUBJECT_NAME_CBRS_DOMAIN, NULL_USERNAME, NULL_PASSWORD, DEFAULT_NE_JOB_ID, nodePamEventOperation, maintenanceUserManagedObjectInfo);

            //poll status map inside NodePamEndUpdateOperationMap
            checkStatus(nodePamEndUpdateOperationMapManager.pollRequestStatusForEvent(nodeName), PHASE3);
        } else {
            logger.info("processCommandRequest::manageCbrsDomainIfNecessary:: isAlreadyConfigured networkElementAccount={}", networkElementAccount);
        }
    }


    private boolean isAlreadyConfigured(NetworkElementAccount networkElementAccount) {
        return networkElementAccount != null && (networkElementAccount.getUpdateStatus() == NetworkElementAccountUpdateStatus.CONFIGURED || networkElementAccount.getUpdateStatus() == NetworkElementAccountUpdateStatus.ONGOING);
    }

    private boolean isAlreadyDetached(NetworkElementAccount networkElementAccount) {
        return (networkElementAccount != null && networkElementAccount.getUpdateStatus() == NetworkElementAccountUpdateStatus.DETACHED);
    }

    private boolean isFailed(NetworkElementAccount networkElementAccount) {
        return (networkElementAccount != null && networkElementAccount.getUpdateStatus() == NetworkElementAccountUpdateStatus.FAILED);
    }

    private boolean existsNextUsernameAndNextPassword(NetworkElementAccount networkElementAccount) {
        return (networkElementAccount != null && networkElementAccount.getNextUsername() != null && networkElementAccount.getNextPassword() != null);
    }
    
    private ManagedObjectInfo createMaintenanceUser(final ManagedObjectInfo userIdentityManagedObjectInfo, final String muId) {
        final String fdn = userIdentityManagedObjectInfo.getFdn() + "," + MAINTENANCE_USER_MO + "=" +muId;
        return new ManagedObjectInfo(fdn, MAINTENANCE_USER_MO, userIdentityManagedObjectInfo.getNameSpace(), userIdentityManagedObjectInfo.getNameSpaceVersion());
    }

    private boolean nothingHasBeenDone() {
        return executedOperations == 0;
    }

    private void checkStatus(NodePamEndUpdateOperation nodePamEndUpdateOperation, final String phase) {
        nodePamEndUpdateOperationMapManager.checkStatus(nodePamEndUpdateOperation, phase);
    }
}
