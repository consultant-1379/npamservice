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
package com.ericsson.oss.services.security.npam.api.interfaces;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJobTemplate;

public interface JobConfigurationService {

    //read methods
    Map<String, Object> retrieveJobTemplate(long jobTemplateId);
    Map<String, Object> retrieveJob(long jobId);
    Map<String, Object> retrieveNEJob(long neJobId);
    public List<Long> getMainJobIds(final String... jobState);
    public List<Long> getNeJobConnectedToMainJob(final long mainJobId);
    public List<Long> getNeJobIdsConnectedToMainJobWithJobState(final long mainJobId, final String... jobState);
    public List<Long> getJobTemplatePoIdsByName(final String jobName);

    public List<NPamJob> getMainJobs(final String jobName);

    //write methods
    public long createPO(final String namespace, final String type, final String version, final Map<String, Object> jobAttributes);

    public void updateJobAttributes(final long jobId, final Map<String, Object> attributes);

    public void updateJobState(final long jobId, final String jobStatus);
    public void updateMainJobProgressPercentage(final long jobId, final double progressPercentage);
    public void updateMainJobStateToCompleted(final long jobId, final String jobResult, final String errorDetails);

    public void updateMainJobStateToCompletedWithEndDate(final long jobId, final String jobResult, final Date endDate);

    public void updateNeJobStateToRunning(final long neJobId);
    public void updateNeJobStateToCompleted(final long neJobId, final String neJobResult, final String errorDetails);

    public NPamJobTemplate getJobTemplateByName(String jobName);

    /**
     * Searches for the PoId in DPS and returns true if the object retrieved is an NPamJob. If the PoId is not found in DPS or if an exceptions
     * occurs, the method returns false.
     * 
     * @param jobId
     *            : NpamJob PoId on DPS
     * @return True is the PoId belongs to a NpamJob object, False otherwise.
     */
    public boolean isMainJob(long jobId);


    /// HOUSEKEEPING   ///
    public List<NPamJob> fetchJobsFromJobTemplate(final String jobName);
    public List<Object[]> fetchJobTemplatesByAge(final Date dateToCompare);

}

