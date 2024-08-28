/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.security.npam.ejb.job.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage;
import com.ericsson.oss.services.security.npam.api.interfaces.JobConfigurationService;
import com.ericsson.oss.services.security.npam.api.interfaces.JobGetService;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEJob;
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus;
import com.ericsson.oss.services.security.npam.ejb.job.mapper.JobMapper;

public class JobGetServiceImpl implements JobGetService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobGetServiceImpl.class);

    @Inject
    JobConfigurationService jobConfigurationService;

    @Inject
    private JobMapper jobMapper;

    @Inject
    NodePamConfigStatus nodePamConfigStatus;

    @Override
    public List<NPamJob> getMainJobs(final String jobName) {
        if (!nodePamConfigStatus.isEnabled()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.FORBIDDEN_FEATURE_DISABLED);
        }
        return jobConfigurationService.getMainJobs(jobName);
    }

    @Override
    public List<NPamNEJob> getNeJobForJobId(final long jobId) {
        if (!nodePamConfigStatus.isEnabled()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.FORBIDDEN_FEATURE_DISABLED);
        }
        if (!jobConfigurationService.isMainJob(jobId)) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.NOT_FOUND_JOB_ID, String.valueOf(jobId));
            //            throw new NodePamRestValidationException("Job Id not found in database.");
        }
        final List<Long> neJobIds = jobConfigurationService.getNeJobConnectedToMainJob(jobId);
        LOGGER.info("getNeJobForJobId list = {}", neJobIds);
        final List<NPamNEJob> neJobList = new ArrayList<>();

        for (final long neJobId : neJobIds) {
            final Map<String, Object> neJobAttributes = jobConfigurationService.retrieveNEJob(neJobId);
            final NPamNEJob neJob = jobMapper.getNEJobDetails(neJobAttributes, neJobId);
            neJobList.add(neJob);
        }
        return neJobList;

    }

}
