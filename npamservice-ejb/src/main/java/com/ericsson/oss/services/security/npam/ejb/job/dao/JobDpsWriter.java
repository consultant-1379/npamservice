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

package com.ericsson.oss.services.security.npam.ejb.job.dao;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.object.builder.PersistenceObjectBuilder;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.*;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import static com.ericsson.oss.services.security.npam.api.constants.ModelsConstants.NAMESPACE;
import static com.ericsson.oss.services.security.npam.api.constants.ModelsConstants.NPAM_NEJOB;

import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.Map;

/**
 * This bean would act as a delegate to Data persistence service.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class JobDpsWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobDpsWriter.class);

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    public PersistenceObject createPO(String nameSpace, String type, String version, Map<String, Object> attributes) {
        return ((PersistenceObjectBuilder)((PersistenceObjectBuilder) ((PersistenceObjectBuilder)this.getLiveBucket().getPersistenceObjectBuilder().namespace(nameSpace)).type(type).version(version)).addAttributes(attributes)).create();
    }

    public void update(long poId, Map<String, Object> attributes) {
        LOGGER.trace("Inside DpsWriterImpl.update() for updating to PO Id {} with attributes {}", poId, attributes);
        if (attributes != null && !attributes.isEmpty()) {
            PersistenceObject persistenceObject = this.getLiveBucket().findPoById(poId);
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

    /// HOUSEKEEPING   ///
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteJobHierarchy(final Long mainJobPOId, final String jobName) {
        DataBucket liveBucket = getLiveBucket();
        final List<PersistenceObject> nPamNEJobPOsList = getNEJobPOsConnectedToMainJob(mainJobPOId, liveBucket);

        //delete NPamNEJobs
        LOGGER.debug("HK-deleteJobHierarchy:: {} NPamNEJob(s) found with mainJobPoId={}. Going to delete them.", nPamNEJobPOsList.size(), mainJobPOId);
        deletePOList(nPamNEJobPOsList, liveBucket);
        LOGGER.info("HK-deleteJobHierarchy:: {} NPamNEJob(s) with mainJobPoId={} have been deleted", nPamNEJobPOsList.size(), mainJobPOId);

        //delete NPamJob
        deletePO(mainJobPOId, liveBucket);
    }

    public void deletePO(PersistenceObject persistenceObject) {
        this.dataPersistenceService.getLiveBucket().deletePo(persistenceObject);
    }

    public PersistenceObject findPoById(long poId) {
        return this.dataPersistenceService.getLiveBucket().findPoById(poId);
    }

    /* PRIVATE methods */

    private DataBucket getLiveBucket() {
        setWriteOnlyTransaction();
        return this.dataPersistenceService.getLiveBucket();
    }

    private void setWriteOnlyTransaction() {
        dataPersistenceService.setWriteAccess(true);
    }


    /// HOUSEKEEPING   ///

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private void deletePO(long poId, DataBucket liveBucket) {
        final PersistenceObject persistenceObject = liveBucket.findPoById(poId);
        liveBucket.deletePo(persistenceObject);
        LOGGER.info("HK-deletePO:: NPamJob with poId={} has been deleted", poId);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private void deletePOList(List<PersistenceObject> jobPOsList, DataBucket liveBucket) {
        for (final PersistenceObject po : jobPOsList) {
            liveBucket.deletePo(po);
            LOGGER.debug("HK-deletePOList:: Deleted NPamNEJob with poId: {}", po.getPoId());
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private List<PersistenceObject> getNEJobPOsConnectedToMainJob(final long mainJobId, DataBucket liveBucket ) {
        List<Long> neJobPoIds;
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
        final Query<TypeRestrictionBuilder> jobTypeQuery = queryBuilder.createTypeQuery(NAMESPACE, NPAM_NEJOB);
        final TypeRestrictionBuilder rb = jobTypeQuery.getRestrictionBuilder();
        final Restriction mainJobRestriction = rb.in(NPamNEJob.NEJOB_MAIN_JOB_ID, mainJobId);
        jobTypeQuery.setRestriction(mainJobRestriction);
        neJobPoIds = queryExecutor.executeProjection(jobTypeQuery, ProjectionBuilder.field(ObjectField.PO_ID));
        LOGGER.debug("HK-getNeJobPOsConnectedToMainJob:: {} NE jobsIds found (related to NPamNEJob:{}) with the following poIds: {}. ", neJobPoIds.size(), mainJobId, neJobPoIds);
        return liveBucket.findPosByIds(neJobPoIds);
    }

}
