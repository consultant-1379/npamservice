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

import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJobTemplate;

public interface JobCreationService {
    void createNewJob(final long jobTemplateId);
    void createNewJobIfNecessary(final long jobTemplateId, int lastExecutionIndex);
    long createNewJobTemplate(final Map<String, Object> jobTemplateAttributes);
    void updateImportedFilename(final long jobTemplateId, final String string);
    List<Long> getJobTemplatePoIdsByName(String name);

    NPamJobTemplate getJobTemplateByName(String jobName);
}
