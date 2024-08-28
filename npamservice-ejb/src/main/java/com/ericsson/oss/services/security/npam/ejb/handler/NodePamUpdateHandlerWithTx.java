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

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DeleteOptions;
import com.ericsson.oss.itpf.datalayer.dps.object.builder.ManagedObjectBuilder;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEJob;
import com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccount;
import com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccountUpdateStatus;
import com.ericsson.oss.services.security.npam.ejb.dao.DpsWriteOperations;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.*;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class NodePamUpdateHandlerWithTx {
    @Inject
    private Logger logger;

    @Inject
    DpsWriteOperations dpsWriteOperations;

    public void createOrUpdateNetworkElementAccount(final String nodeName, final String networkElementAccountId, final String nextUserName, final String nextPassword,
                                                    final JobType jobType, final Long mainJobId, final long neJobId) {
        final DataBucket dataBucket = dpsWriteOperations.getDataBucket(CONFIGURATION_LIVE);

        //Check networkElementAccountFdn
        //          2. verify FDN : NetworkElement=LTE04dg2ERBS00005,SecurityFunction=1,NetworkElementAccount=NPAM1 existence
        final String networkElementAccountFdn = NETWORK_ELEMENT_MO + "=" + nodeName + "," + SECURITY_FUNCTION_MO + "=1," + NETWORK_ELEMENT_ACCOUNT_MO + "=" + networkElementAccountId;

        //          3. create or update  NetworkElement=LTE04dg2ERBS00005,SecurityFunction=1,NetworkElementAccount=NPAM1
        final ManagedObject networkElementAccountMo = dpsWriteOperations.findManagedObject(networkElementAccountFdn, dataBucket);
        if (networkElementAccountMo == null) {

            final String parentFdn = NETWORK_ELEMENT_MO + "=" + nodeName + "," + SECURITY_FUNCTION_MO + "=1";
            final ManagedObject attachedParentMo = dpsWriteOperations.findManagedObject(parentFdn, dataBucket);

            final Map<String, Object> attributes = new HashMap<>();
            attributes.put(NetworkElementAccount.NEA_NEXT_USER_NAME, nextUserName);
            attributes.put(NetworkElementAccount.NEA_NEXT_PASSWORD, nextPassword);
            attributes.put(NetworkElementAccount.NEA_ERROR_DETAILS, NO_ERRROR_VALUE);
            attributes.put(NetworkElementAccount.NEA_JOB_TYPE, getJobTypeStringOrNull(jobType));
            attributes.put(NetworkElementAccount.NEA_MAIN_JOB_ID, mainJobId);
            attributes.put(NetworkElementAccount.NEA_NE_JOB_ID, getNeJobIdOrNull(neJobId));
            attributes.put(NetworkElementAccount.NEA_UPDATE_STATUS, NetworkElementAccountUpdateStatus.ONGOING.name());
            final ManagedObjectBuilder managedObjectBuilder = dataBucket.getManagedObjectBuilder().type(NETWORK_ELEMENT_ACCOUNT_MO).name(networkElementAccountId)
                    .addAttributes(attributes)
                    .parent(attachedParentMo);
            final ManagedObject newNetworkElementAccountMo = managedObjectBuilder.create();
            logger.debug("createOrUpdateNetworkElementAccount:: created new entry={}", newNetworkElementAccountMo);

        } else {
                networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_NEXT_USER_NAME, nextUserName);
                networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_NEXT_PASSWORD, nextPassword);
                networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_ERROR_DETAILS, NO_ERRROR_VALUE);
                networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_JOB_TYPE, getJobTypeStringOrNull(jobType));
                networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_MAIN_JOB_ID, mainJobId);
                networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_NE_JOB_ID, getNeJobIdOrNull(neJobId));
                networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_UPDATE_STATUS, NetworkElementAccountUpdateStatus.ONGOING.name());
                logger.debug("createOrUpdateNetworkElementAccount:: updated networkElementAccountMo={}", networkElementAccountMo);
        }
    }

    public void updateNetworkElementAccountToFailedStatus(final NPamNEJob neJob, final String networkElementAccountId,
                                                          final String errorDetails, final Date lastFailedDate,
                                                          final JobType jobType, final boolean resetCredentials) {
        final DataBucket dataBucket = dpsWriteOperations.getDataBucket(CONFIGURATION_LIVE);

        final String networkElementAccountFdn = NETWORK_ELEMENT_MO + "=" + neJob.getNeName() + "," + SECURITY_FUNCTION_MO + "=1," + NETWORK_ELEMENT_ACCOUNT_MO + "=" + networkElementAccountId;
        final ManagedObject networkElementAccountMo = dpsWriteOperations.findManagedObject(networkElementAccountFdn, dataBucket);

        if (networkElementAccountMo != null)
        {
            final Long neJobIdWritten = networkElementAccountMo.getAttribute(NetworkElementAccount.NEA_NE_JOB_ID);
            if (neJobIdWritten == null || !neJobIdWritten.equals(neJob.getNeJobId())) {
                networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_UPDATE_STATUS, NetworkElementAccountUpdateStatus.FAILED.name());
                networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_ERROR_DETAILS, errorDetails);
                networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_LAST_FAILED, lastFailedDate);
                networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_MAIN_JOB_ID, neJob.getMainJobId());
                networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_NE_JOB_ID, neJob.getNeJobId());
                networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_JOB_TYPE, jobType.getJobTypeName());
                if (resetCredentials) {
                    networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_CURRENT_USER_NAME, NULL_USERNAME);
                    networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_CURRENT_PASSWORD, NULL_PASSWORD);
                }
                logger.info("updateNetworkElementAccountToFailedStatus:: updated networkElementAccountMo={}", networkElementAccountMo);
            }
        } else {
            final String parentFdn = NETWORK_ELEMENT_MO + "=" + neJob.getNeName() + "," + SECURITY_FUNCTION_MO + "=1";
            final ManagedObject attachedParentMo = dpsWriteOperations.findManagedObject(parentFdn, dataBucket);

            final Map<String, Object> attributes = new HashMap<>();
            attributes.put(NetworkElementAccount.NEA_UPDATE_STATUS, NetworkElementAccountUpdateStatus.FAILED.name());
            attributes.put(NetworkElementAccount.NEA_ERROR_DETAILS, errorDetails);
            attributes.put(NetworkElementAccount.NEA_LAST_FAILED, lastFailedDate);
            attributes.put(NetworkElementAccount.NEA_MAIN_JOB_ID, neJob.getMainJobId());
            attributes.put(NetworkElementAccount.NEA_NE_JOB_ID, neJob.getNeJobId());
            attributes.put(NetworkElementAccount.NEA_JOB_TYPE, jobType.getJobTypeName());
            final ManagedObjectBuilder managedObjectBuilder = dataBucket.getManagedObjectBuilder().type(NETWORK_ELEMENT_ACCOUNT_MO).name(networkElementAccountId)
                    .addAttributes(attributes)
                    .parent(attachedParentMo);
            managedObjectBuilder.create();
            logger.info("updateNetworkElementAccountToFailedStatus:: NetworkElementAccount created for this node {}",neJob.getNeName());
        }
    }

    public void updateNetworkElementAccountWithJobDetails(final NPamNEJob neJob, final String networkElementAccountId, final JobType jobType, final boolean resetCredentials) {
        final DataBucket dataBucket = dpsWriteOperations.getDataBucket(CONFIGURATION_LIVE);

        final String networkElementAccountFdn = NETWORK_ELEMENT_MO + "=" + neJob.getNeName() + "," + SECURITY_FUNCTION_MO + "=1," + NETWORK_ELEMENT_ACCOUNT_MO + "=" + networkElementAccountId;
        final ManagedObject networkElementAccountMo = dpsWriteOperations.findManagedObject(networkElementAccountFdn, dataBucket);

        if (networkElementAccountMo != null)
        {
            final Long neJobIdWritten = networkElementAccountMo.getAttribute(NetworkElementAccount.NEA_NE_JOB_ID);
            if (neJobIdWritten == null || !neJobIdWritten.equals(neJob.getNeJobId())) {
                networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_MAIN_JOB_ID, neJob.getMainJobId());
                networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_NE_JOB_ID, neJob.getNeJobId());
                networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_JOB_TYPE, jobType.getJobTypeName());
                if (resetCredentials) {
                    networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_CURRENT_USER_NAME, NULL_USERNAME);
                    networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_CURRENT_PASSWORD, NULL_PASSWORD);
                }
                logger.info("updateNetworkElementAccountWithJobDetails:: updated networkElementAccountMo={}", networkElementAccountMo);
            } else {
                logger.debug("updateNetworkElementAccountWithJobDetails::  neJobIdWritten = {}  neJobId={}",neJobIdWritten,  neJob.getNeJobId());
            }
        } else {
            logger.error("updateNetworkElementAccountWithJobDetails:: NetworkElementAccount not exist for this node {}",neJob.getNeName());
        }
    }

    public void setAllNetworkElementAccountsToDetachedState(final String nodeName) {
        for (Integer i=1; i<=NUM_OF_NE_ACCOUNTS; i++) {
            final String networkElementAccountId = i.toString();
            setNetworkElementAccountToDetachedState(nodeName, networkElementAccountId);
        }
    }

    public void setNetworkElementAccountToDetachedState(final String nodeName, final String networkElementAccountId) {
        final DataBucket dataBucket = dpsWriteOperations.getDataBucket(CONFIGURATION_LIVE);
        final String networkElementAccountFdn = NETWORK_ELEMENT_MO + "=" + nodeName + "," + SECURITY_FUNCTION_MO + "=1," + NETWORK_ELEMENT_ACCOUNT_MO + "=" + networkElementAccountId;
        final ManagedObject networkElementAccountMo = dpsWriteOperations.findManagedObject(networkElementAccountFdn, dataBucket);
        if (networkElementAccountMo != null) {
            networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_MAIN_JOB_ID, null);
            networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_NE_JOB_ID, null);
            networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_JOB_TYPE, null);
            networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_ERROR_DETAILS, NO_ERRROR_VALUE);
            networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_UPDATE_STATUS, NetworkElementAccountUpdateStatus.DETACHED.name());
            logger.info("setNetworkElementAccountToDetachedState:: updateStatus=DETACHED for networkElementAccountMo={}", networkElementAccountFdn);
        }
    }

    public void setNetworkElementAccountToDetachedState(final String nodeName, final Long mainJobId, final long neJobId, final JobType jobType, final String networkElementAccountId) {
        final DataBucket dataBucket = dpsWriteOperations.getDataBucket(CONFIGURATION_LIVE);
        final String networkElementAccountFdn = NETWORK_ELEMENT_MO + "=" + nodeName + "," + SECURITY_FUNCTION_MO + "=1," + NETWORK_ELEMENT_ACCOUNT_MO + "=" + networkElementAccountId;
        final ManagedObject networkElementAccountMo = dpsWriteOperations.findManagedObject(networkElementAccountFdn, dataBucket);
        if (networkElementAccountMo != null) {
            networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_MAIN_JOB_ID, mainJobId);
            networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_NE_JOB_ID, getNeJobIdOrNull(neJobId));
            networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_JOB_TYPE, jobType.getJobTypeName());
            networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_ERROR_DETAILS, NO_ERRROR_VALUE);
            networkElementAccountMo.setAttribute(NetworkElementAccount.NEA_UPDATE_STATUS, NetworkElementAccountUpdateStatus.DETACHED.name());
            logger.info("setNetworkElementAccountToDetachedState:: updateStatus=DETACHED for networkElementAccountMo={}", networkElementAccountFdn);
        }
    }

    public void deleteNetworkElementAccount(final String nodeName, final String networkElementAccountId) {
        final DataBucket dataBucket = dpsWriteOperations.getDataBucket(CONFIGURATION_LIVE);
        final String networkElementAccountFdn = NETWORK_ELEMENT_MO + "=" + nodeName + "," + SECURITY_FUNCTION_MO + "=1," + NETWORK_ELEMENT_ACCOUNT_MO + "=" + networkElementAccountId;
        final ManagedObject networkElementAccountMo = dpsWriteOperations.findManagedObject(networkElementAccountFdn, dataBucket);
        if (networkElementAccountMo != null) {
            dataBucket.deleteManagedObject(networkElementAccountMo, DeleteOptions.defaultDelete());
            logger.info("deleteNetworkElementAccount:: delete networkElementAccountMo={}", networkElementAccountFdn);
        } else {
            logger.debug("deleteNetworkElementAccount:: networkElementAccountMo={} already deleted", networkElementAccountFdn);
        }
    }

    final Long getNeJobIdOrNull(final long neJobId) {
        if (neJobId > 0) {
           return Long.valueOf(neJobId);
        } else {
            return null;
        }
    }

    final String getJobTypeStringOrNull(final JobType jobType) {
        if (jobType != null) {
            return jobType.getJobTypeName();
        } else {
            return null;
        }
    }
}