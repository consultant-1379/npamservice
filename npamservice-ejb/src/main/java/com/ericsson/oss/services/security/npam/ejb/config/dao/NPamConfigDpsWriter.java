/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.security.npam.ejb.config.dao;

import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;

/**
 * This bean would act as a delegate to Data persistence service.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class NPamConfigDpsWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(NPamConfigDpsWriter.class);

    @EServiceRef
    private DataPersistenceService dataPersistenceService;



    public PersistenceObject createPO(final String nameSpace, final String type, final String version, final Map<String, Object> attributes) {
        return getLiveBucket().getPersistenceObjectBuilder().namespace(nameSpace).type(type).version(version).addAttributes(attributes).create();
    }

    public void update(final long poId, final Map<String, Object> attributes) {
        LOGGER.trace("Inside DpsWriterImpl.update() for updating to PO Id {} with attributes {}", poId, attributes);
        if (attributes != null && !attributes.isEmpty()) {
            final PersistenceObject persistenceObject = this.getLiveBucket().findPoById(poId);
            if (persistenceObject != null) {
                persistenceObject.setAttributes(attributes);
            } else {
                LOGGER.error("PO not found with Id:{}, and skipiing the attributes Update", poId);
            }
        } else {
            LOGGER.debug("Discarding the PO : {} update as the attributes :{} not valid", poId, attributes);
        }

        LOGGER.trace("Exiting from DpsWriterImpl.update() ");
    }

    public void deletePOList(final List<Long> poIds) {
        final DataBucket liveBucket = getLiveBucket();
        for (final long poId : poIds) {
            liveBucket.deletePo(liveBucket.findPoById(poId));
        }
    }

    private DataBucket getLiveBucket() {
        setWriteOnlyTransaction();
        return this.dataPersistenceService.getLiveBucket();
    }

    private void setWriteOnlyTransaction() {
        dataPersistenceService.setWriteAccess(true);
    }
}
