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

package com.ericsson.oss.services.security.npam.ejb.dao;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.CONFIGURATION_LIVE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_SECURITY_MO;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_SECURITY_MO_NAMESPACE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_SECURITY_REMOTE_MANAGEMENT_ATTRIBUTE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NETWORK_ELEMENT_ACCOUNT_MO;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NETWORK_ELEMENT_MO;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NETYPE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NE_TYPES_SUPPORTING_FUNCTIONALITY;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NODEROOTREF;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.SECURITY_FUNCTION_MO;
import static com.ericsson.oss.services.security.npam.ejb.handler.FdnUtility.isValidFdn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.exception.general.ObjectNotFoundException;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEAccount;
import com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccount;
import com.ericsson.oss.services.security.npam.ejb.executor.ManagedObjectInfo;
import com.ericsson.oss.services.security.npam.ejb.handler.FdnUtility;


/**
 * This bean would act as a delegate to Data persistence service.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class DpsReadOperations {

    private static final String NETWORK_ELEMENT_ACCOUNT_ID = "networkElementAccountId";

    @Inject
    private Logger logger;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    private static final String NETWORK_ELEMENT = "NetworkElement";
    private static final String EQUAL_SEPARATOR = "=";
    private static final String COMMA_SEPARATOR = ",";
    /**
     * Find a managed object with the given FDN in this data bucket.
     *
     * @param fdn
     *            - the FDN of the managed object to search for
     * @param dataBucket
     *            - {@link DataBucket}.
     * @return - the managed object which has the supplied FDN
     */
    public ManagedObject findManagedObject(final String fdn, final DataBucket dataBucket) {
        return dataBucket.findMoByFdn(fdn);
    }

    public DataBucket getDataBucket(final String configuration) {
        setReadOnlyTransaction();
        try {
            return dataPersistenceService.getDataBucket(configuration);
        } catch (final ObjectNotFoundException e) {
            logger.error("getDataBucket failed due to : {}", e.getMessage());
        }
        return null;
    }

    public DataBucket getLiveBucket() {
        return getDataBucket("Live");
    }

    public List<PersistenceObject> findPOsByPoIds(final List<Long> poIds) {
        return getLiveBucket().findPosByIds(poIds);
    }

    public List<ManagedObjectInfo> getFdnsGivenBaseFdn(final DataBucket dataBucket, final String namespace, final String moType, final String baseFdn) {
        try {
            final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
            final QueryExecutor queryExecutor = dataBucket.getQueryExecutor();
            final Query<TypeContainmentRestrictionBuilder> jobTypeQuery = queryBuilder.createTypeQuery(namespace, moType, baseFdn);

            final List<Long> poids = queryExecutor.executeProjection(jobTypeQuery, ProjectionBuilder.field(ObjectField.PO_ID));
            final List<ManagedObjectInfo> managedObjectInfos = new ArrayList<>();
            for (final Long poid:poids) {
                final PersistenceObject persistenceObject = dataBucket.findPoById(poid);
                if (persistenceObject instanceof ManagedObject) {
                    final ManagedObject managedObject = (ManagedObject)persistenceObject;
                    managedObjectInfos.add(new ManagedObjectInfo(managedObject.getFdn(), managedObject.getType(), managedObject.getNamespace(), managedObject.getVersion()));
                }
            }
            return managedObjectInfos;
        } catch (final Exception e) {
            logger.error("getFdnsGivenBaseFdn failed due to : {}", e.getMessage());
            return Collections.emptyList();
        }
    }


    public List<NPamNEAccount> getNEAccountObjectGivenNEAccountFdn(final DataBucket dataBucket, final String namespace, final String moType,
                                                                   final String nodeName, final List<String> neAccountIds) {
        final String neFdn = NETWORK_ELEMENT + "=" + nodeName;

        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final QueryExecutor queryExecutor = dataBucket.getQueryExecutor();
        final Query<TypeContainmentRestrictionBuilder> neAccountTypeQuery = queryBuilder.createTypeQuery(namespace, moType,
                neFdn);
        if (neAccountIds != null && !neAccountIds.isEmpty()) {
            final String[] array = new String[neAccountIds.size()];
            neAccountIds.toArray(array); // fill the array
            final TypeRestrictionBuilder rb = neAccountTypeQuery.getRestrictionBuilder();

            final Restriction neAccountIdRestriction = rb.in(NETWORK_ELEMENT_ACCOUNT_ID, (Object[]) array);
            neAccountTypeQuery.setRestriction(neAccountIdRestriction);
        }
        final Iterator<ManagedObject> moList = queryExecutor.execute(neAccountTypeQuery);
        final List<NPamNEAccount> neAccounts = new ArrayList<>();
        while (moList.hasNext()) {
            neAccounts.add(new NPamNEAccount(nodeName, moList.next().getAllAttributes()));
        }
        return neAccounts;
    }



    public NetworkElementAccount getNetworkElementAccount(final String nodeName, final String networkElementAccountId) {
        NetworkElementAccount networkElementAccount = null;
        final DataBucket dataBucket = getDataBucket(CONFIGURATION_LIVE);
        final String networkElementAccountFdn = NETWORK_ELEMENT_MO + "=" + nodeName + "," + SECURITY_FUNCTION_MO + "=1," + NETWORK_ELEMENT_ACCOUNT_MO + "=" + networkElementAccountId;
        final ManagedObject managedObject = findManagedObject(networkElementAccountFdn, dataBucket);
        if (managedObject != null) {
            networkElementAccount = new NetworkElementAccount(managedObject.getAllAttributes());
        }
        return networkElementAccount;
    }

    public List<NPamNEAccount> getNEAccountsById(final DataBucket dataBucket, final String namespace, final String moType,
                                                 final List<String> neAccountIds) {
        final Iterator<ManagedObject> moList = retrieveNeAccountById(dataBucket, namespace, moType, neAccountIds);
        final List<NPamNEAccount> neAccounts = new ArrayList<>();
        while (moList.hasNext()) {
            final ManagedObject nextMo = moList.next();
            neAccounts.add(new NPamNEAccount(getNodeName(nextMo.getFdn()), nextMo.getAllAttributes()));
        }
        return neAccounts;
    }

    public Map<String, List<NPamNEAccount>> getMapNEAccountsById(final DataBucket dataBucket, final String namespace, final String moType,
                                                                 final List<String> neAccountIds) {
        final Iterator<ManagedObject> moList = retrieveNeAccountById(dataBucket, namespace, moType, neAccountIds);
        final Map<String, List<NPamNEAccount>> neAccounts = new HashMap<>();
        while (moList.hasNext()) {
            final ManagedObject nextMo = moList.next();
            final String neName = getNodeName(nextMo.getFdn());
            neAccounts.computeIfAbsent(neName, v -> new ArrayList<>());
            neAccounts.get(neName).add(new NPamNEAccount(neName, nextMo.getAllAttributes()));

        }
        return neAccounts;
    }

    private Iterator<ManagedObject> retrieveNeAccountById(final DataBucket dataBucket, final String namespace, final String moType,
                                                          final List<String> neAccountIds) {
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final QueryExecutor queryExecutor = dataBucket.getQueryExecutor();
        final Query<TypeRestrictionBuilder> neAccountTypeQuery = queryBuilder.createTypeQuery(namespace, moType);
        final TypeRestrictionBuilder rb = neAccountTypeQuery.getRestrictionBuilder();
        if (neAccountIds != null && !neAccountIds.isEmpty()) {
            final String[] array = new String[neAccountIds.size()];
            neAccountIds.toArray(array);
            final Restriction neAccountIdRestriction = rb.in(NETWORK_ELEMENT_ACCOUNT_ID, (Object[]) array);
            neAccountTypeQuery.setRestriction(neAccountIdRestriction);
        }

        return queryExecutor.execute(neAccountTypeQuery);
    }

    public Boolean findRemoteManagementField(final DataBucket dataBucket, final String nodeName) {
        final String associatedNodeRootFdn = getAssociatedNodeRootFdn(nodeName, dataBucket);
        final Boolean notFoundValue = null;
        if (associatedNodeRootFdn == null) {
            return notFoundValue;
        }
        return getRemoteManagementByFdn(associatedNodeRootFdn, dataBucket);
    }

    private void setReadOnlyTransaction() {
        dataPersistenceService.setWriteAccess(false);
    }

    private String getAssociatedNodeRootFdn(final String nodeName, final DataBucket dataBucket) {
        //Check networkElementFdn
        final String networkElementFdn = NETWORK_ELEMENT_MO + "=" + nodeName;
        final ManagedObject managedObject = findManagedObject(networkElementFdn, dataBucket);
        if (managedObject == null) {
            logger.info("validateNodeRootFdn:: not found networkElementFdn={}", networkElementFdn);
            return null;
        }

        //Check if RadioNode
        final String neType = managedObject.getAttribute(NETYPE);
        if (!NE_TYPES_SUPPORTING_FUNCTIONALITY.contains(neType)) {
            logger.info("validateNodeRootFdn:: not valid netype={} for networkElementFdn={}", neType, networkElementFdn);
            return null;
        }

        //Check getNodeRootRef(final ManagedObject managedObject)
        final String associatedNodeRootFdn = getNodeRootRef(managedObject);
        if (!isValidFdn(associatedNodeRootFdn)) {
            logger.info("validateNodeRootFdn:: invalid  associatedNodeRootFdn={}", associatedNodeRootFdn);
            return null;
        }
        logger.info("validateNodeRootFdn:: associatedNodeRootFdn={}", associatedNodeRootFdn);
        return associatedNodeRootFdn;
    }

    private Boolean getRemoteManagementByFdn(final String associatedNodeRootFdn, final DataBucket dataBucket) {
        Boolean attributeValue = null;
        //Check maintenanceUserSecurityManagedObjectInfo
        final List<ManagedObjectInfo> maintenanceUserSecurityManagedObjectInfos = getFdnsGivenBaseFdn(dataBucket,
                MAINTENANCE_USER_SECURITY_MO_NAMESPACE, MAINTENANCE_USER_SECURITY_MO, associatedNodeRootFdn);
        logger.info("validateMaintenanceUserSecurityFdn:: maintenanceUserSecurityManagedObjectInfos={}", maintenanceUserSecurityManagedObjectInfos);
        if (maintenanceUserSecurityManagedObjectInfos.isEmpty()) {
            logger.info("validateMaintenanceUserSecurityFdn:: not found maintenanceUserSecurityManagedObjectInfo");
            return attributeValue;
        }
        if (maintenanceUserSecurityManagedObjectInfos.size() > 1) {
            logger.info("validateMaintenanceUserSecurityFdn:: not unique maintenanceUserSecurityManagedObjectInfo");
            return attributeValue;
        }
        // use first maintenanceUserSecurityManagedObjectInfo we call this in order to check attributes in future
        final ManagedObjectInfo maintenanceUserSecurityManagedObjectInfo = maintenanceUserSecurityManagedObjectInfos.get(0);
        final ManagedObject managedObject = findManagedObject(maintenanceUserSecurityManagedObjectInfo.getFdn(), dataBucket);
        if (managedObject == null) {
            logger.info("validateMaintenanceUserSecurityFdn:: not found maintenanceUserSecurityManagedObjectInfo={}",
                    maintenanceUserSecurityManagedObjectInfo);
            return attributeValue;
        } else {
            logger.info("validateMaintenanceUserSecurityFdn:: found maintenanceUserSecurityManagedObjectInfo={}",
                    maintenanceUserSecurityManagedObjectInfo);
            final Map<String, Object> attributes = managedObject.getAllAttributes();
            if (attributes.containsKey(MAINTENANCE_USER_SECURITY_REMOTE_MANAGEMENT_ATTRIBUTE)) {
                attributeValue = (Boolean) attributes.get(MAINTENANCE_USER_SECURITY_REMOTE_MANAGEMENT_ATTRIBUTE);
            }
            logger.info("validateMaintenanceUserSecurityFdn:: found attributeValue={}", (attributeValue == null ? "null" : attributeValue));

        }
        return attributeValue;
    }

    public Map<String, Boolean> getAllMaintenanceUser(final DataBucket dataBucket) {
        Boolean attributeValue = null;

        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final QueryExecutor queryExecutor = dataBucket.getQueryExecutor();
        final Query<TypeRestrictionBuilder> neAccountTypeQuery = queryBuilder.createTypeQuery(MAINTENANCE_USER_SECURITY_MO_NAMESPACE,
                MAINTENANCE_USER_SECURITY_MO);
        final Iterator<ManagedObject> moList = queryExecutor.execute(neAccountTypeQuery);
        final Map<String, Boolean> remoteManagementMap = new HashMap<>();
        while (moList.hasNext()) {
            final ManagedObject managedObject = moList.next();
            final Map<String, Object> attributes = managedObject.getAllAttributes();
            if (attributes.containsKey(MAINTENANCE_USER_SECURITY_REMOTE_MANAGEMENT_ATTRIBUTE)) {
                attributeValue = (Boolean) attributes.get(MAINTENANCE_USER_SECURITY_REMOTE_MANAGEMENT_ATTRIBUTE);
                if (attributeValue != null) {
                    remoteManagementMap.put(FdnUtility.extractNodeNameFromFdn(managedObject.getFdn()), attributeValue);
                }
            }
        }
        return remoteManagementMap;
    }

    private String getNodeRootRef(final ManagedObject managedObject) {
        try {
            logger.info("getNodeRootRef:: for fdn={}", managedObject.getFdn());
            final Collection<PersistenceObject> persistenceObjects = managedObject.getAssociations(NODEROOTREF);
            if (!persistenceObjects.isEmpty()) {
                return ((ManagedObject) persistenceObjects.iterator().next()).getFdn();
            }
        } catch (final Exception ee) {
            logger.error("getNodeRootRef:: raised Exception.");
            throw ee;
        }
        return null;
    }

    private static String getNodeName(final String fdn) {
        final String[] rdnsArray = fdn.split(COMMA_SEPARATOR);
        for (final String rdn : rdnsArray) {
            if (rdn.startsWith(NETWORK_ELEMENT)) {
                return rdn.split(EQUAL_SEPARATOR)[1].trim();
            }
        }
        return null;
    }

    public Set<String> getAllNodeNames() {
        try {
            logger.info("getAllNodeNames:: START");
            final DataBucket dataBucket = getLiveBucket();
            final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
            final QueryExecutor queryExecutor = dataBucket.getQueryExecutor();
            final Query<TypeRestrictionBuilder> moTypeQuery = queryBuilder.createTypeQuery("OSS_NE_DEF", NETWORK_ELEMENT);

            final List<String> names = queryExecutor.executeProjection(moTypeQuery, ProjectionBuilder.field(ObjectField.NAME));
            final Set<String> nodeNames = new HashSet<>();
            nodeNames.addAll(names);
            logger.info("getAllNodeNames:: STOP found nodeNames.size()={}", nodeNames.size());
            return nodeNames;
        } catch (final Exception e) {
            logger.error("getAllNodeNames failed due to : {}", e.getMessage());
            return new HashSet<>();
        }
    }

}
