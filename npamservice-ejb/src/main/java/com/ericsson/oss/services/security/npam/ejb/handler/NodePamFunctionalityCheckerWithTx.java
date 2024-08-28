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
package com.ericsson.oss.services.security.npam.ejb.handler;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.CMFUNCTION_MO;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.CONFIGURATION_LIVE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_MO;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_MO_NAMESPACE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_SECURITY_MO;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_SECURITY_MO_NAMESPACE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_SECURITY_REMOTE_MANAGEMENT_ATTRIBUTE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NETWORK_ELEMENT_MO;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NETYPE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NE_TYPES_SUPPORTING_FUNCTIONALITY;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NODEROOTREF;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.REMOTE_MANAGEMENT_TRUE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.SYNCSTATUS;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.USER_IDENTITY_MO;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_SUBJECT_NAME;
import static com.ericsson.oss.services.security.npam.ejb.handler.FdnUtility.isValidFdn;
import static com.ericsson.oss.services.security.npam.ejb.utility.ManagedObjectUtility.filterForMoId;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccount;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.security.npam.ejb.dao.DpsReadOperations;
import com.ericsson.oss.services.security.npam.ejb.exceptions.ExceptionFactory;
import com.ericsson.oss.services.security.npam.ejb.exceptions.NodePamError;
import com.ericsson.oss.services.security.npam.ejb.executor.ManagedObjectInfo;
import com.ericsson.oss.services.security.npam.ejb.executor.NodeInfo;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class NodePamFunctionalityCheckerWithTx {
    @Inject
    private Logger logger;

    @Inject
    DpsReadOperations dpsReadOperations;

    @Inject
    ExceptionFactory exceptionFactory;

    public ManagedObjectInfo validateMaintenanceUserSecurityFdnByNodeName(final String nodeName) {

        final DataBucket dataBucket = dpsReadOperations.getDataBucket(CONFIGURATION_LIVE);
        final String associatedNodeRootFdn = validateNodeRootFdn(nodeName, dataBucket);
        return validateMaintenanceUserSecurityFdn(associatedNodeRootFdn, dataBucket);
    }

    private String validateNodeRootFdn(final String nodeName, final DataBucket dataBucket) {
        //Check networkElementFdn
        final String networkElementFdn = NETWORK_ELEMENT_MO + "=" + nodeName;
        final ManagedObject managedObject = dpsReadOperations.findManagedObject(networkElementFdn, dataBucket);
        if (managedObject == null) {
            logger.debug("validateNodeRootFdn:: not found networkElementFdn={}", networkElementFdn);
            throw exceptionFactory.createValidationException(NodePamError.FDN_NOT_FOUND, networkElementFdn);
        }

        //Check if RadioNode
        final String neType = managedObject.getAttribute(NETYPE);
        if (!NE_TYPES_SUPPORTING_FUNCTIONALITY.contains(neType)) {
            logger.info("validateNodeRootFdn:: not valid netype={} for networkElementFdn={}", neType, networkElementFdn);
            throw exceptionFactory.createValidationException(NodePamError.UNSUPPORTED_NE_TYPE, neType);
        }

        //Check getNodeRootRef(final ManagedObject managedObject)
        final String associatedNodeRootFdn = getNodeRootRef(managedObject);
        if (!isValidFdn(associatedNodeRootFdn)) {
            logger.info("validateNodeRootFdn:: invalid  associatedNodeRootFdn={}", associatedNodeRootFdn);
            throw exceptionFactory.createValidationException(NodePamError.INVALID_NODE_ROOT, networkElementFdn, associatedNodeRootFdn);
        }
        logger.debug("validateNodeRootFdn:: associatedNodeRootFdn={}", associatedNodeRootFdn);
        return associatedNodeRootFdn;
    }

    private ManagedObjectInfo validateMaintenanceUserSecurityFdn(final String associatedNodeRootFdn, final DataBucket dataBucket) {

        //Check maintenanceUserSecurityManagedObjectInfo
        final List<ManagedObjectInfo> maintenanceUserSecurityManagedObjectInfos = dpsReadOperations.getFdnsGivenBaseFdn(dataBucket, MAINTENANCE_USER_SECURITY_MO_NAMESPACE, MAINTENANCE_USER_SECURITY_MO, associatedNodeRootFdn);
        logger.debug("validateMaintenanceUserSecurityFdn:: maintenanceUserSecurityManagedObjectInfos={}", maintenanceUserSecurityManagedObjectInfos);
        if (maintenanceUserSecurityManagedObjectInfos.isEmpty()) {
            throw exceptionFactory.createValidationException(NodePamError.NO_ENTRIES, MAINTENANCE_USER_SECURITY_MO, associatedNodeRootFdn);
        }
        if (maintenanceUserSecurityManagedObjectInfos.size() > 1) {
            throw exceptionFactory.createValidationException(NodePamError.TOO_MANY_ENTRIES, MAINTENANCE_USER_SECURITY_MO, associatedNodeRootFdn);
        }
        // use first maintenanceUserSecurityManagedObjectInfo we call this in order to check attributes in future
        final ManagedObjectInfo maintenanceUserSecurityManagedObjectInfo = maintenanceUserSecurityManagedObjectInfos.get(0);
        final ManagedObject managedObject = dpsReadOperations.findManagedObject(maintenanceUserSecurityManagedObjectInfo.getFdn(), dataBucket);
        if (managedObject == null) {
            logger.info("validateMaintenanceUserSecurityFdn:: not found maintenanceUserSecurityManagedObjectInfo={}", maintenanceUserSecurityManagedObjectInfo);
            throw exceptionFactory.createValidationException(NodePamError.FDN_NOT_FOUND, maintenanceUserSecurityManagedObjectInfo.getFdn());
        } else {
            logger.debug("validateMaintenanceUserSecurityFdn:: found maintenanceUserSecurityManagedObjectInfo={}", maintenanceUserSecurityManagedObjectInfo);
            final Boolean attributeValue = fetchRemoteManagementValue(managedObject);
            logger.debug("validateMaintenanceUserSecurityFdn:: found attributeValue={}", attributeValue);

            //store remoteManagement attribute value
            final Map<String, Object> newAttributes = new HashMap<>();
            newAttributes.put(MAINTENANCE_USER_SECURITY_REMOTE_MANAGEMENT_ATTRIBUTE, attributeValue);
            maintenanceUserSecurityManagedObjectInfo.setAttributes(newAttributes);
        }
        return maintenanceUserSecurityManagedObjectInfo;
    }

    @SuppressWarnings({"squid:S2447"})
    private Boolean fetchRemoteManagementValue(final ManagedObject managedObject) {
        try {
            return (Boolean) managedObject.getAttribute(MAINTENANCE_USER_SECURITY_REMOTE_MANAGEMENT_ATTRIBUTE);
        }
        catch (final Exception e) { logger.info("fetchRemoteManagementValue:: exception recovering attributeValue e.getClass={}, e.getMessage={} ", e.getClass(), e.getMessage());}
        return null;
    }

    public NodeInfo validateAndReturnMaintenanceUsers(final String nodeName, final String muId, final boolean checkIfRemoteManagementIsTrue) {
        final DataBucket dataBucket = dpsReadOperations.getDataBucket(CONFIGURATION_LIVE);
        final String associatedNodeRootFdn = validateNodeRootFdn(nodeName, dataBucket);

        //only in case of NodePamAutogeneratePasswordRequestExecutor we check value
        if (checkIfRemoteManagementIsTrue) {
            final ManagedObjectInfo maintenanceUserSecurityManagedObjectInfo = validateMaintenanceUserSecurityFdn(associatedNodeRootFdn, dataBucket);
            validateRemoteManagementAttributeValue(maintenanceUserSecurityManagedObjectInfo, REMOTE_MANAGEMENT_TRUE);
        }

        //Check maintenanceUserManagedObjectInfos
        final NodeInfo nodeInfo = new NodeInfo();
        final List<ManagedObjectInfo> maintenanceUserManagedObjectInfos = dpsReadOperations.getFdnsGivenBaseFdn(dataBucket, MAINTENANCE_USER_MO_NAMESPACE, MAINTENANCE_USER_MO, associatedNodeRootFdn);
        addSubjectNameValue(maintenanceUserManagedObjectInfos, dataBucket);
        logger.debug("validateAndReturnMaintenanceUsers:: maintenanceUserManagedObjectInfos={}", maintenanceUserManagedObjectInfos);

        if (muId != null) {
            final ManagedObjectInfo maintenanceUserManagedObjectInfo = filterForMoId(maintenanceUserManagedObjectInfos, muId);
            if (maintenanceUserManagedObjectInfo == null) {
                throw exceptionFactory.createValidationException(NodePamError.NOT_FOUND_MAINTENANCE_USER, associatedNodeRootFdn, muId);
            } else {
                nodeInfo.setSingleMaintenanceUserManagedObjectInfo(maintenanceUserManagedObjectInfo);
            }
        } else {
            nodeInfo.getMaintenanceUserManagedObjectInfos().addAll(maintenanceUserManagedObjectInfos);
        }

        final ManagedObjectInfo userIdentityManagedObjectInfo = validateUserIdentityFdn(associatedNodeRootFdn, dataBucket);
        nodeInfo.setUserIdentityManagedObjectInfo(userIdentityManagedObjectInfo);
        return nodeInfo;
    }

    private String fetchSubjectNameValue(final ManagedObject managedObject) {
        try {
            return (String) managedObject.getAttribute(MAINTENANCE_USER_SUBJECT_NAME);
        }
        catch (final Exception e) { logger.info("fetchSubjectNameValue:: exception recovering attributeValue e.getClass={}, e.getMessage={} ", e.getClass(), e.getMessage());}
        return null;
    }

    private void addSubjectNameValue(final List<ManagedObjectInfo> maintenanceUserManagedObjectInfos, final DataBucket dataBucket) {
        for (final ManagedObjectInfo maintenanceUserManagedObjectInfo: maintenanceUserManagedObjectInfos) {
            final ManagedObject managedObject = dpsReadOperations.findManagedObject(maintenanceUserManagedObjectInfo.getFdn(), dataBucket);
            if (managedObject != null) {
                logger.debug("addSubjectNameValue:: found maintenanceUserManagedObjectInfo={}", maintenanceUserManagedObjectInfo);
                final String attributeValue = fetchSubjectNameValue(managedObject);
                logger.debug("addSubjectNameValue:: found attributeValue={}", attributeValue);

                //store subjectName attribute value
                final Map<String, Object> newAttributes = new HashMap<>();
                newAttributes.put(MAINTENANCE_USER_SUBJECT_NAME, attributeValue);
                maintenanceUserManagedObjectInfo.setAttributes(newAttributes);
            }
        }
    }

    private ManagedObjectInfo validateUserIdentityFdn(final String associatedNodeRootFdn, final DataBucket dataBucket) {
        final List<ManagedObjectInfo> userIdentityManagedObjectInfos = dpsReadOperations.getFdnsGivenBaseFdn(dataBucket, MAINTENANCE_USER_MO_NAMESPACE, USER_IDENTITY_MO, associatedNodeRootFdn);
        logger.debug("validateUserIdentityFdn:: userIdentityManagedObjectInfos={}", userIdentityManagedObjectInfos);
        if (userIdentityManagedObjectInfos.isEmpty()) {
            throw exceptionFactory.createValidationException(NodePamError.NO_ENTRIES, USER_IDENTITY_MO, associatedNodeRootFdn);
        }
        if (userIdentityManagedObjectInfos.size() > 1) {
            throw exceptionFactory.createValidationException(NodePamError.TOO_MANY_ENTRIES, USER_IDENTITY_MO, associatedNodeRootFdn);
        }
        // use first userIdentityManagedObjectInfos we call this in order to check attributes in future
        return userIdentityManagedObjectInfos.get(0);
    }

    public String getCmFunctionSyncStatus(final String nodeName) {
        final DataBucket dataBucket = dpsReadOperations.getDataBucket(CONFIGURATION_LIVE);
        //Check CmFunctionFdn
        final String networkElementFdn = NETWORK_ELEMENT_MO + "=" + nodeName;
        final String cmFunctionFdn = networkElementFdn + "," + CMFUNCTION_MO + "=1";
        final ManagedObject managedObject = dpsReadOperations.findManagedObject(cmFunctionFdn, dataBucket);
        if (managedObject == null) {
            logger.info("getCmFunctionSyncStatus:: not found cmFunctionFdn={} so we raise NODE_NOT_EXISTENT exception", cmFunctionFdn);
            //This check is the first one so we decided to export this error like node non existent instead of FDN_NOT_FOUND
            throw exceptionFactory.createValidationException(NodePamError.NODE_NOT_EXISTENT);
        }
        final String syncStatus = managedObject.getAttribute(SYNCSTATUS);
        if (syncStatus == null) {
            logger.info("getCmFunctionSyncStatus:: syncStatus={} for cmFunctionFdn={}", syncStatus, cmFunctionFdn);
            throw exceptionFactory.createValidationException(NodePamError.NULL_SYNC_STATUS, nodeName);
        }
        return syncStatus;
    }

    private String getNodeRootRef(final ManagedObject managedObject){
        try {
            logger.debug("getNodeRootRef:: for fdn={}", managedObject.getFdn());
            final Collection<PersistenceObject> persistenceObjects = managedObject.getAssociations(NODEROOTREF);
            if (!persistenceObjects.isEmpty()) {
                return ((ManagedObject)persistenceObjects.iterator().next()).getFdn();
            }
        } catch (final Exception ee) {
            logger.info("getNodeRootRef:: raised Exception={}", ee.getMessage());
        }
        return null;
    }

    private void validateRemoteManagementAttributeValue(final ManagedObjectInfo maintenanceUserSecurityManagedObjectInfo, final boolean expectedRemoteManagementValue) {
        final Boolean remoteManagementValue = (Boolean) maintenanceUserSecurityManagedObjectInfo.getAttributes().get(MAINTENANCE_USER_SECURITY_REMOTE_MANAGEMENT_ATTRIBUTE);
        if ((remoteManagementValue == null) || (remoteManagementValue.booleanValue() != expectedRemoteManagementValue)) {
            logger.info("validateRemoteManagementAttributeValue:: Unexpected RemoteManagement value for FDN={} found={} expected={}", maintenanceUserSecurityManagedObjectInfo.getFdn(), remoteManagementValue, expectedRemoteManagementValue);
            throw exceptionFactory.createValidationException(NodePamError.REMOTE_MANAGEMENT_VALUE_MISMATCH);
        }
    }

    public NetworkElementAccount getNetworkElementAccount(final String nodeName, final String networkElementAccountId) {
            return dpsReadOperations.getNetworkElementAccount(nodeName, networkElementAccountId);
    }
}
