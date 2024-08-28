package com.ericsson.oss.services.security.npam.ejb.testutil

import com.ericsson.oss.itpf.datalayer.dps.DataBucket
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField
import com.ericsson.oss.itpf.datalayer.dps.query.Query
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEJob

import static com.ericsson.oss.services.security.npam.api.constants.ModelsConstants.NAMESPACE
import static com.ericsson.oss.services.security.npam.api.constants.ModelsConstants.NPAM_NEJOB
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NETWORK_ELEMENT_ACCOUNT_MO
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NETWORK_ELEMENT_MO
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.SECURITY_FUNCTION_MO;

class DpsQueryUtil {
    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    public ManagedObject findManagedObject(final String fdn) {
        DataBucket dataBucket = dataPersistenceService.getLiveBucket()
        final ManagedObject managedObject = dataBucket.findMoByFdn(fdn)
        return managedObject
    }

    public PersistenceObject findPersistentObject(final long poId) {
        DataBucket dataBucket = dataPersistenceService.getLiveBucket()
        final PersistenceObject persistenceObject = dataBucket.findPoById(poId)
        return persistenceObject
    }

    public ManagedObject getNetworkElementAccount(String nodeName, int networkElementAccountId) {
        final String networkElementAccountFdn = NETWORK_ELEMENT_MO + "=" + nodeName + "," + SECURITY_FUNCTION_MO + "=1," + NETWORK_ELEMENT_ACCOUNT_MO + "=" + networkElementAccountId
        return findManagedObject(networkElementAccountFdn)
    }

    public List<PersistenceObject> getNeJobsConnectedToMainJob(final long mainJobId) {
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
        final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
        final Query<TypeRestrictionBuilder> jobTypeQuery = queryBuilder.createTypeQuery(NAMESPACE, NPAM_NEJOB);
        final TypeRestrictionBuilder rb = jobTypeQuery.getRestrictionBuilder();
        final Restriction jobStateRestriction = rb.in(NPamNEJob.NEJOB_MAIN_JOB_ID, mainJobId);
        jobTypeQuery.setRestriction(jobStateRestriction);
        final List<Long> neJobIds = queryExecutor.executeProjection(jobTypeQuery, ProjectionBuilder.field(ObjectField.PO_ID));

        List<PersistenceObject> persistenceObjectList = new ArrayList<>()
        for (final long neJobId : neJobIds) {
            final PersistenceObject po = dataPersistenceService.getLiveBucket().findPoById(neJobId);
            persistenceObjectList.add(po);
        }
        return persistenceObjectList
    }
}
