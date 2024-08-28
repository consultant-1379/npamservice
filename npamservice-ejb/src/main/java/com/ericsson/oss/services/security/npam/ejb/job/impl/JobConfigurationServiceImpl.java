/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.security.npam.ejb.job.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.security.npam.api.interfaces.JobConfigurationService;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobState;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJobTemplate;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEJob;
import com.ericsson.oss.services.security.npam.ejb.job.dao.JobDpsReader;
import com.ericsson.oss.services.security.npam.ejb.job.dao.JobDpsWriter;

@SuppressWarnings({ "unchecked", "PMD.ExcessiveClassLength" })
//@Profiled
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class JobConfigurationServiceImpl implements JobConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobConfigurationServiceImpl.class);

    @Inject
    private JobDpsReader dpsReader;

    @Inject
    private JobDpsWriter dpsWriter;


    /*
     *             READ METHODS
     *
     * */

    @Override
    public Map<String, Object> retrieveJobTemplate(final long jobTemplateId) {
        LOGGER.debug("JobTemplate id received is {}", jobTemplateId);
        return fetchPo(jobTemplateId);
    }

    @Override
    public Map<String, Object> retrieveJob(final long jobId) {
        LOGGER.debug("Job id received is {}", jobId);
        return fetchPo(jobId);
    }

    @Override
    public Map<String, Object> retrieveNEJob(final long neJobId) {
        LOGGER.debug("Job id received is {}", neJobId);
        return fetchPo(neJobId);
    }

    @Override
    public List<Long> getMainJobIds(final String... jobState) {
        return dpsReader.getMainJobIds(jobState);
    }

    @Override
    public List<NPamJob> getMainJobs(final String jobName) {
        return dpsReader.getMainJobs(jobName);
    }

    @Override
    public boolean isMainJob(final long jobId) {
        return dpsReader.isMainJob(jobId);
    }

    @Override
    public List<Long> getNeJobConnectedToMainJob(final long mainJobId) {
        return dpsReader.getNeJobIdsConnectedToMainJob(mainJobId);
    }

    @Override
    public List<Long> getNeJobIdsConnectedToMainJobWithJobState(final long mainJobId, final String... jobState) {
        return dpsReader.getNeJobIdsConnectedToMainJobWithJobState(mainJobId, jobState);
    }

    private Map<String, Object> fetchPo(final long jobId) {
        final PersistenceObject po = dpsReader.findPOByPoId(jobId);
        if (po != null) {
            return po.getAllAttributes();
        } else {
            return new HashMap<>();
        }
    }


    /*
     *             WRITE METHODS
     *
     * */

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public long createPO(final String namespace, final String type, final String version, final Map<String, Object> jobAttributes) {
        final PersistenceObject jobPO = dpsWriter.createPO(namespace, type, version, jobAttributes);
        return jobPO.getPoId();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateJobAttributes(final long poId, final Map<String, Object> attributes) {
        dpsWriter.update(poId, attributes);
    }

    @Override
    @SuppressWarnings({"squid:S3252"})
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateJobState(final long jobId, final String jobState) {
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        mainJobAttributes.put(NPamJob.JOB_STATE, jobState);
        dpsWriter.update(jobId, mainJobAttributes);
    }


    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateMainJobProgressPercentage(final long jobId,  final double progressPercentage) {
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        mainJobAttributes.put(NPamJob.JOB_PROGRESS_PERCENTAGE, progressPercentage);
        dpsWriter.update(jobId, mainJobAttributes);
    }

    @Override
    @SuppressWarnings({"squid:S3252"})
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateMainJobStateToCompleted(final long jobId, final String jobResult, final String errorDetails) {
        updateMainJobStateToCompleted(jobId, jobResult, null, errorDetails);
    }

    @Override
    @SuppressWarnings({"squid:S3252"})
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateMainJobStateToCompletedWithEndDate(final long jobId, final String jobResult, final Date endDate) {
        updateMainJobStateToCompleted(jobId, jobResult, endDate, null);
    }

    @Override
    @SuppressWarnings({"squid:S3252"})
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateNeJobStateToCompleted(final long neJobId, final String neJobResult, final String errorDetails) {
        final Map<String, Object> neJobAttributes = new HashMap<>();
        neJobAttributes.put(NPamNEJob.JOB_STATE, JobState.COMPLETED.getJobStateName());
        neJobAttributes.put(NPamNEJob.JOB_RESULT, neJobResult);
        neJobAttributes.put(NPamNEJob.JOB_END_TIME, new Date());
        if (errorDetails != null) {
            neJobAttributes.put(NPamNEJob.JOB_ERROR_DETAILS, errorDetails);
        }
        dpsWriter.update(neJobId, neJobAttributes);
    }

    @Override
    @SuppressWarnings({"squid:S3252"})
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateNeJobStateToRunning(final long neJobId) {
        final Map<String, Object> neJobAttributes = new HashMap<>();
        neJobAttributes.put(NPamNEJob.JOB_STATE, JobState.RUNNING.getJobStateName());
        neJobAttributes.put(NPamNEJob.JOB_START_TIME, new Date());
        dpsWriter.update(neJobId, neJobAttributes);
    }

    @Override
    public List<Long> getJobTemplatePoIdsByName(final String jobName) {
        return dpsReader.getJobTemplatesByName(jobName);
    }

    @Override
    public NPamJobTemplate getJobTemplateByName(final String jobName) {
        return dpsReader.getJobTemplate(jobName);
    }

    @SuppressWarnings({"squid:S3252"})
    private void updateMainJobStateToCompleted(final long jobId, final String jobResult, final Date endDate, final String errorDetails) {
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        mainJobAttributes.put(NPamJob.JOB_STATE, JobState.COMPLETED.getJobStateName());
        mainJobAttributes.put(NPamJob.JOB_PROGRESS_PERCENTAGE, 100.0);
        if (jobResult != null) {
            mainJobAttributes.put(NPamJob.JOB_RESULT, jobResult);
        }
        if (endDate != null) {
            mainJobAttributes.put(NPamJob.JOB_END_TIME, endDate);
        } else {
            mainJobAttributes.put(NPamJob.JOB_END_TIME, new Date());
        }
        if (errorDetails != null) {
            mainJobAttributes.put(NPamNEJob.JOB_ERROR_DETAILS, errorDetails);
        }
        dpsWriter.update(jobId, mainJobAttributes);
    }

    /// HOUSEKEEPING   ///
    public List<NPamJob> fetchJobsFromJobTemplate(final String jobName) {
        return dpsReader.fetchJobsFromJobTemplate(jobName);
    }
    public List<Object[]> fetchJobTemplatesByAge(final Date dateToCompare) {
        return dpsReader.fetchJobTemplatesByAge(dateToCompare);
    }

}
