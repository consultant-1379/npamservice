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

import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.JOB_NAME;
import static com.ericsson.oss.services.security.npam.api.constants.ModelsConstants.NAMESPACE;
import static com.ericsson.oss.services.security.npam.api.constants.ModelsConstants.NPAM_JOB;
import static com.ericsson.oss.services.security.npam.api.constants.ModelsConstants.NPAM_JOBTEMPLATE;
import static com.ericsson.oss.services.security.npam.api.constants.ModelsConstants.NPAM_NEJOB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.exception.general.ObjectNotInContextException;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants;
import com.ericsson.oss.services.security.npam.api.constants.ModelsConstants;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJobTemplate;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEJob;
import com.ericsson.oss.services.security.npam.ejb.job.mapper.JobMapper;


/**
 * This bean would act as a delegate to Data persistence service.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class JobDpsReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobDpsReader.class);

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    JobMapper jobMapper;

    public PersistenceObject findPOByPoId(final long poId) {
        return this.getLiveBucket().findPoById(poId);
    }

    private DataBucket getLiveBucket() {
        setReadOnlyTransaction();
        return this.dataPersistenceService.getLiveBucket();
    }

    @SuppressWarnings({"squid:S3252"})
    public List<Long> getMainJobIds(final String... jobState) {
        try {
            final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
            final DataBucket liveBucket = getLiveBucket();
            final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
            final Query<TypeRestrictionBuilder> jobTypeQuery = queryBuilder.createTypeQuery(NAMESPACE, NPAM_JOB);
            final TypeRestrictionBuilder rb = jobTypeQuery.getRestrictionBuilder();
            if (jobState != null) {
                final Restriction jobStateRestriction = rb.in(NPamJob.JOB_STATE, (Object[]) jobState);
                jobTypeQuery.setRestriction(jobStateRestriction);
            }
            return queryExecutor.executeProjection(jobTypeQuery, ProjectionBuilder.field(ObjectField.PO_ID));

        } catch (final Exception e) {
            LOGGER.error("getMainJobIds::Main job ID retrieval failed with jobState={}  due to : {}", jobState, e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings({"squid:S2139"})
    public List<NPamJob> getMainJobs(String jobName) {
        LOGGER.info("Execute getMainJobs");
        final List<NPamJob> npamJobList = new ArrayList<>();
        try {
            final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
            final DataBucket liveBucket = getLiveBucket();
            final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
            final Query<TypeRestrictionBuilder> jobTypeQuery = queryBuilder.createTypeQuery(NAMESPACE, NPAM_JOB);

            if (jobName != null) {
                final TypeRestrictionBuilder rb = jobTypeQuery.getRestrictionBuilder();
                final Restriction mainJobRestriction = rb.in(NPamJob.JOB_NAME, jobName);
                jobTypeQuery.setRestriction(mainJobRestriction);
            } 

            final Iterator<PersistenceObject> poListIterator = queryExecutor.execute(jobTypeQuery);
            while (poListIterator.hasNext()) {
                final PersistenceObject po = poListIterator.next();
                final NPamJob npamJob = jobMapper.getJobDetails(po.getAllAttributes(), po.getPoId());
                npamJobList.add(npamJob);
            }
            if (npamJobList.isEmpty() && jobName != null) {
                jobName = jobName.replaceAll("[\n\r\t]", "_");
                LOGGER.error("getMainJobs::Job Name {} does not exist", jobName);
                throw new NPAMRestErrorException(NPamRestErrorMessage.NOT_FOUND_JOB_NAME, jobName);
            }
            return npamJobList;

        } catch (final Exception e) {
            if (e instanceof NPAMRestErrorException) {
                throw e;
            }
            LOGGER.error("getMainJobs:: Main job ID retrieval failed due to : {}", e.getMessage());
            throw new NPAMRestErrorException(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_DPS_RWISSUE);
        }
    }

    public NPamJobTemplate getJobTemplate(final String jobName) {
        LOGGER.info("Execute getJobTemplate");
        if (jobName == null) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_NAME);
            //            throw new NodePamRestValidationException("JobName cannot be null");
        }
        final List<NPamJobTemplate> npamJobTemplateList = new ArrayList<>();
        try {
            final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
            final DataBucket liveBucket = getLiveBucket();
            final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
            final Query<TypeRestrictionBuilder> jobTypeQuery = queryBuilder.createTypeQuery(NAMESPACE, NPAM_JOBTEMPLATE);

            final TypeRestrictionBuilder rb = jobTypeQuery.getRestrictionBuilder();
            final Restriction mainJobRestriction = rb.in(NPamJob.JOB_NAME, jobName);
            jobTypeQuery.setRestriction(mainJobRestriction);

            final Iterator<PersistenceObject> poListIterator = queryExecutor.execute(jobTypeQuery);
            while (poListIterator.hasNext()) {
                final PersistenceObject po = poListIterator.next();
                final NPamJobTemplate npamJobTempl = jobMapper.getJobTemplateDetails(po.getAllAttributes(), po.getPoId());
                npamJobTemplateList.add(npamJobTempl);
            }
            if (npamJobTemplateList.isEmpty()) {
                //                final String errorMsg = "JobTemplate with Name " + jobName + " does not exist";
                throw new NPAMRestErrorException(NPamRestErrorMessage.NOT_FOUND_JOB_NAME, jobName);
                //                throw new NodePamRestValidationException(errorMsg);
            }
            return npamJobTemplateList.get(0);

        } catch (final Exception e) {
            LOGGER.error("getJobTemplate:: Job Template by name retrieval failed due to an exception.");
            throw e;
        }
    }

    public List<Long> getNeJobIdsConnectedToMainJob(final long mainJobId) {
        try {
            final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
            final DataBucket liveBucket = getLiveBucket();
            final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
            final Query<TypeRestrictionBuilder> jobTypeQuery = queryBuilder.createTypeQuery(NAMESPACE, NPAM_NEJOB);
            final TypeRestrictionBuilder rb = jobTypeQuery.getRestrictionBuilder();
            final Restriction mainJobRestriction = rb.in(NPamNEJob.NEJOB_MAIN_JOB_ID, mainJobId);
            jobTypeQuery.setRestriction(mainJobRestriction);
            return queryExecutor.executeProjection(jobTypeQuery, ProjectionBuilder.field(ObjectField.PO_ID));
        } catch (final Exception e) {
            LOGGER.error("getNeJobIdsConnectedToMainJob:: NE job IDs retrieval for mainJobId={} failed due to : {}", mainJobId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Long> getNeJobIdsConnectedToMainJobWithJobState(final long mainJobId, final String... jobState) {
        try {
            final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
            final DataBucket liveBucket = getLiveBucket();
            final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
            final Query<TypeRestrictionBuilder> jobTypeQuery = queryBuilder.createTypeQuery(NAMESPACE, NPAM_NEJOB);
            final TypeRestrictionBuilder rb = jobTypeQuery.getRestrictionBuilder();

            List<Restriction> restrictions = new ArrayList<>();

            //prepare mainJobRestriction
            final Restriction mainJobRestriction = rb.in(NPamNEJob.NEJOB_MAIN_JOB_ID, mainJobId);
            restrictions.add(mainJobRestriction);

            //prepare jobStateRestriction
            if (jobState != null) {
                final Restriction jobStateRestriction = rb.in(NPamJob.JOB_STATE, (Object[]) jobState);
                restrictions.add(jobStateRestriction);
            }

            //set restrictions
            final Restriction[] restrictionArray = restrictions.toArray(new Restriction[restrictions.size()]);
            final TypeRestrictionBuilder restrictionBuilder = jobTypeQuery.getRestrictionBuilder();
            jobTypeQuery.setRestriction(restrictionBuilder.allOf(restrictionArray));

            return queryExecutor.executeProjection(jobTypeQuery, ProjectionBuilder.field(ObjectField.PO_ID));
        } catch (final Exception e) {
            LOGGER.error("getNeJobIdsConnectedToMainJobWithJobState:: NE job IDs retrieval for mainJobId={} failed due to : {}", mainJobId, e.getMessage());
            return Collections.emptyList();
        }
    }


    public boolean isMainJob(final long mainJobId) {
        try {
            final PersistenceObject po = findPOByPoId(mainJobId);
            if (po != null) {
                return po.getType().equals(NPAM_JOB);
            } else {
                LOGGER.warn("JobInstanceId {} not found in database.", mainJobId);
            }

        } catch (final ObjectNotInContextException e) {
            LOGGER.warn("isMainJob:: NpamJob retrieval for mainJobId={} failed due to : {}", mainJobId, e.getMessage());
        }
        return false;
    }

    private void setReadOnlyTransaction() {
        dataPersistenceService.setWriteAccess(false);
    }

    public List<Long> getJobTemplatesByName(final String jobName) {
        try {
            final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
            // REMEMBER: REMOVE THIS
            dataPersistenceService.setWriteAccess(true);
            final DataBucket liveBucket = dataPersistenceService.getLiveBucket();

            final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
            final Query<TypeRestrictionBuilder> jobTemplateNameQuery = queryBuilder.createTypeQuery(NAMESPACE, NPAM_JOBTEMPLATE);
            final TypeRestrictionBuilder rb = jobTemplateNameQuery.getRestrictionBuilder();

            final Restriction jobNameRestriction = rb.in(JOB_NAME, jobName);

            jobTemplateNameQuery.setRestriction(jobNameRestriction);
            return queryExecutor.executeProjection(jobTemplateNameQuery, ProjectionBuilder.field(ObjectField.PO_ID));
        } catch (final Exception e) {
            LOGGER.error("Retrieve job templates by name failed due to : {} ", e.getMessage());
            return new ArrayList<>(0);
        }
    }

    /// HOUSEKEEPING   ///

    public List<Object[]> fetchJobTemplatesByAge(final Date dateToCompare) {
        try {
            final DataBucket liveBucket = getLiveBucket();
            List<Object[]> poIdsAndNamesBasedOnAge;
            LOGGER.info("HK-fetchJobTemplatesByAge:: Date for HK comparison in Query = {}", dateToCompare);
            final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
            final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ModelsConstants.NAMESPACE, ModelsConstants.NPAM_JOBTEMPLATE);
            final TypeRestrictionBuilder restrictionBuilder = query.getRestrictionBuilder();
            final Restriction endTimeRestriction = restrictionBuilder.lessThan(JobConfigurationConstants.CREATION_TIME, dateToCompare);
            query.setRestriction(endTimeRestriction);
            poIdsAndNamesBasedOnAge = queryExecutor.executeProjection(query, ProjectionBuilder.field(ObjectField.PO_ID), ProjectionBuilder.attribute("name"));
            return poIdsAndNamesBasedOnAge;
        } catch (final Exception exception) {
            LOGGER.error("HK-Exception while fetching NPamJobTemplate PoIds Based on Age, message: {}", exception.getMessage());
            return new ArrayList<>(0);
        }
    }

    public List<NPamJob> fetchJobsFromJobTemplate(final String jobName) {
            final List<NPamJob> npamJobList = new ArrayList<>();
            try {
                final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
                final DataBucket liveBucket = getLiveBucket();
                final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
                final Query<TypeRestrictionBuilder> jobTypeQuery = queryBuilder.createTypeQuery(NAMESPACE, NPAM_JOB);

                    final TypeRestrictionBuilder rb = jobTypeQuery.getRestrictionBuilder();
                    final Restriction mainJobRestriction = rb.in(NPamJob.JOB_NAME, jobName);
                    jobTypeQuery.setRestriction(mainJobRestriction);

                final Iterator<PersistenceObject> poListIterator = queryExecutor.execute(jobTypeQuery);
                while (poListIterator.hasNext()) {
                    final PersistenceObject po = poListIterator.next();
                    final NPamJob npamJob = jobMapper.getJobDetails(po.getAllAttributes(), po.getPoId());
                    npamJobList.add(npamJob);
                }

                return npamJobList;

            } catch (final Exception e) {
                LOGGER.info("HK-fetchJobsFromJobTemplate Exception message = {}",e.getMessage());
               return new ArrayList<>(0);
            }
        }
}
