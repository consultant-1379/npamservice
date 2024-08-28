/*
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 */

package com.ericsson.oss.services.security.npam.ejb.neaccount.executor;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.CONFIGURATION_LIVE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NETWORK_ELEMENT_ACCOUNT_MO;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NETWORK_ELEMENT_ACCOUNT_MO_NAMESPACE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NETWORK_ELEMENT_MO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEAccount;
import com.ericsson.oss.services.security.npam.ejb.dao.DpsReadOperations;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class NeAccountGetServiceImpl {
    @Inject
    DpsReadOperations dpsReadOperations;

    private static final Logger LOGGER = LoggerFactory.getLogger(NeAccountGetServiceImpl.class);
    private static final String IPADDRESS_KEY = "ipAddress";
    private static final String CONNINFO = "ConnectivityInformation";

    public List<NPamNEAccount> findNEAccountObject(final String nodeName, final List<String> neaccountIds) {
        final DataBucket dataBucket = dpsReadOperations.getDataBucket(CONFIGURATION_LIVE);
        return dpsReadOperations.getNEAccountObjectGivenNEAccountFdn(dataBucket, NETWORK_ELEMENT_ACCOUNT_MO_NAMESPACE, NETWORK_ELEMENT_ACCOUNT_MO,
                nodeName, neaccountIds);
    }

    public String retrieveIp(final String nodeName) {
        final DataBucket dataBucket = dpsReadOperations.getDataBucket(CONFIGURATION_LIVE);
        final String parentFdn = NETWORK_ELEMENT_MO + "=" + nodeName;
        final ManagedObject attachedParentMo = dpsReadOperations.findManagedObject(parentFdn, dataBucket);
        return getIpAddress(attachedParentMo);
    }

    private String getIpAddress(final ManagedObject managedObject) {
        LOGGER.info("Entering getIpAddress( {} ) ", managedObject);
        String ipAddress = "";
        final ManagedObject aComConnInfo = getComConnInfo(managedObject);
        if (aComConnInfo != null) {
            final Object ipAddressValue = aComConnInfo.getAttribute(IPADDRESS_KEY);
            if (ipAddressValue != null) {
                ipAddress = ipAddressValue.toString();
            }

        }
        return ipAddress;
    }

    private ManagedObject getComConnInfo(final ManagedObject networkElement) {
        LOGGER.info("Entering getComConnInfo( {} ) ", networkElement);
        ManagedObject ipAddressChild = null;
        final Collection<ManagedObject> children = networkElement.getChildren();
        for (final ManagedObject mo : children) {
            LOGGER.debug("moType of networkElement: {}", mo.getType());
            if (mo.getType().contains(CONNINFO)) {
                ipAddressChild = mo;
            }
        }
        LOGGER.info("child: {}", ipAddressChild);

        return ipAddressChild;
    }

    public List<NPamNEAccount> getAllNEAccountsById(final List<String> neIdAccountList) {
        final DataBucket dataBucket = dpsReadOperations.getDataBucket(CONFIGURATION_LIVE);
        return dpsReadOperations.getNEAccountsById(dataBucket, NETWORK_ELEMENT_ACCOUNT_MO_NAMESPACE, NETWORK_ELEMENT_ACCOUNT_MO, neIdAccountList);
    }

    public Map<String, List<NPamNEAccount>> getMapAllNEAccountsById(final List<String> neIdAccountList) {
        final DataBucket dataBucket = dpsReadOperations.getDataBucket(CONFIGURATION_LIVE);
        return dpsReadOperations.getMapNEAccountsById(dataBucket, NETWORK_ELEMENT_ACCOUNT_MO_NAMESPACE, NETWORK_ELEMENT_ACCOUNT_MO, neIdAccountList);
    }

    public Map<String, Boolean> findAllRemoteManagementField() {
        final DataBucket dataBucket = dpsReadOperations.getDataBucket(CONFIGURATION_LIVE);
        return dpsReadOperations.getAllMaintenanceUser(dataBucket);
    }

}
