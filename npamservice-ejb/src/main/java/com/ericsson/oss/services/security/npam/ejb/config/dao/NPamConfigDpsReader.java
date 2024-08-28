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

import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_NAME;
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_VALUE;
import static com.ericsson.oss.services.security.npam.api.constants.ModelsConstants.NAMESPACE;
import static com.ericsson.oss.services.security.npam.api.constants.ModelsConstants.NPAM_CONFIG;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_PROPERTIES;

import java.util.ArrayList;
import java.util.Iterator;
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
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfig;
import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfigProperty;

/**
 * This bean would act as a delegate to Data persistence service.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class NPamConfigDpsReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(NPamConfigDpsReader.class);

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    public List<NPamConfig> getNPamConfigObject() {
        LOGGER.info("Execute getNPamConfigObject");
        final List<NPamConfig> nPamConfigList = new ArrayList<>();
        try {
            final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
            final DataBucket liveBucket = getLiveBucket();
            final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
            final Query<TypeRestrictionBuilder> nPamConfigQuery = queryBuilder.createTypeQuery(NAMESPACE, NPAM_CONFIG);

            final Iterator<PersistenceObject> poListIterator = queryExecutor.execute(nPamConfigQuery);

            while (poListIterator.hasNext()) {
                final PersistenceObject po = poListIterator.next();
                final NPamConfig nPamConfig = new NPamConfig();
                final List<NPamConfigProperty> nPamConfigProperties = new ArrayList<>();
                final List<Map<String, String>> attributes = (List<Map<String, String>>) po.getAllAttributes().get(NPAM_CONFIG_PROPERTIES);


                for (final Map<String, String> el : attributes) {
                    final NPamConfigProperty nPamConfigProperty = new NPamConfigProperty();

                    nPamConfigProperty.setName(el.get(PROPERTY_NAME));
                    nPamConfigProperty.setValue(el.get(PROPERTY_VALUE));
                    nPamConfigProperties.add(nPamConfigProperty);
                }
                nPamConfig.setnPamConfigProperties(nPamConfigProperties);
                nPamConfig.setPoId(po.getPoId());
                nPamConfigList.add(nPamConfig);
            }

            return nPamConfigList;

        } catch (final Exception e) {
            LOGGER.error("Main job ID retrieval failed due to : {}", e.getMessage());
            throw e;
        }
    }

    private DataBucket getLiveBucket() {
        setReadOnlyTransaction();
        return this.dataPersistenceService.getLiveBucket();
    }

    private void setReadOnlyTransaction() {
        dataPersistenceService.setWriteAccess(false);
    }
}
