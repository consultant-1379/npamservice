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
package com.ericsson.oss.services.security.npam.api.rest;

import java.util.List;

import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJobTemplate;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEJob;

public interface JobService {
    long createJobTemplate(final NPamJobTemplate nPAMJobTemplateJAXB);

    void createNewJob(long jobTemplateId);

    List<NPamJob> getMainJobs(String jobName);

    List<NPamNEJob> getNeJobForJobId(long jobId);

    void updalodInputFileFromContent(String bodyAsString, String fileName, Boolean overwrite);

    String[] importedJobFilelist();

    void deleteJob(String jobName);

    NPamJobTemplate getJobTemplate(String jobName);
}
