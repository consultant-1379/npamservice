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

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.exception.general.ObjectNotFoundException;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.*;

/**
 * This bean would act as a delegate to Data persistence service.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class DpsWriteOperations {
    @Inject
    private Logger logger;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    /**
     * To find Managed object for the given fdn and {@link DataBucket}.
     *
     * @param fdn
     *            - fully distinguished name for the managed object.
     * @param dataBucket
     *            - {@link DataBucket}
     * @return - {@link ManagedObject}
     */
    public ManagedObject findManagedObject(final String fdn, final DataBucket dataBucket) {
        return dataBucket.findMoByFdn(fdn);
    }

    public DataBucket getDataBucket(final String configuration) {
        setWriteOnlyTransaction();
        try {
            return dataPersistenceService.getDataBucket(configuration);
        } catch (final ObjectNotFoundException e) {
            logger.error("getDataBucket failed due to : {}", e.getMessage());
        }
        return null;
    }

    private void setWriteOnlyTransaction() {
        dataPersistenceService.setWriteAccess(true);
    }
}
