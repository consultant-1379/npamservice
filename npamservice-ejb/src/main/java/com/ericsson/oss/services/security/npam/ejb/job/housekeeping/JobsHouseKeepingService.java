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
package com.ericsson.oss.services.security.npam.ejb.job.housekeeping;

import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.security.npam.api.interfaces.JobConfigurationService;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob;

/**
 * 
 * This class is used to check if housekeeping is required and then trigger housekeeping if required.
 * 
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class JobsHouseKeepingService {

    @Inject
    private JobsHouseKeepingHelperUtil jobsHouseKeepingHelperUtil;

    @Inject
    private JobConfigurationService  jobConfigurationService;


    private static final Logger LOGGER = LoggerFactory.getLogger(JobsHouseKeepingService.class);

    /**
     *
     * The algorithm:
     *
     * 1) For every NPamJobTemplate, checks if creation date crosses the specified number of days
     * 
     * 2) If yes, checks if related NPamJobs having status COMPLETED or USER_CANCELLED are expired or not
     *    (that is if jobs have been completed or cancelled before the specified number of days)
     *
     * 3) If yes, NPamJobs are deleted, together with the related NPamNEJobs
     *
     * 4) Checks if NPamJobTemplate is still having related NPamJobs
     *
     * 5) If not, NPamJobTemplate is deleted, after deleting the related import file, if any
     *
     */
    public void triggerHouseKeepingOfJobsStartingFromTemplates(int maxJobAge) {
        Date dateToCompare = new DateTime().minusDays(maxJobAge).toDate();

        final List<Object[]> jobTemplatesPoIdAndName = jobConfigurationService.fetchJobTemplatesByAge(dateToCompare);
        LOGGER.info("HK-triggerHouseKeepingOfJobsStartingFromTemplates:: Found {} NPamJobTemplates older than {}", jobTemplatesPoIdAndName.size(), dateToCompare);
        for (final Object[] item : jobTemplatesPoIdAndName) {
            final long jobTemplateId = (Long)item[0];
            final String jobName = (String)item[1];
            LOGGER.debug("HK-triggerHouseKeepingOfJobsStartingFromTemplates:: Analyzing NPamJobTemplate with poId={}, having name={} for Housekeeping", jobTemplateId, jobName);
            checkAndDeleteJobsAndTemplate(jobTemplateId, jobName , dateToCompare);
        }
        LOGGER.info("HK-triggerHouseKeepingOfJobsStartingFromTemplates:: HouseKeepingOfJobs Process Completed");
    }

    public void triggerHouseKeepingOfFiles(int maxFileAge) {
        jobsHouseKeepingHelperUtil.triggerHouseKeepingOfFiles(maxFileAge);
        LOGGER.info("HK-triggerHouseKeepingOfFiles:: HouseKeepingOfFiles Process Completed");
    }

    private void checkAndDeleteJobsAndTemplate(final Long templatePoId, final String name, final Date dateToCompare) {
        try {
            List<NPamJob> jobsToBeChecked = jobConfigurationService.fetchJobsFromJobTemplate(name);
            LOGGER.info("HK-checkAndDeleteJobsAndTemplate:: {} NPamJobs found with name {}", jobsToBeChecked.size(), name);

            jobsHouseKeepingHelperUtil.deleteJobsAndTemplateIfNecessary(name, jobsToBeChecked, dateToCompare, templatePoId);
        } catch (Exception exception) {
            LOGGER.info("HK-checkAndDeleteJobsAndTemplate:: Skipping processing NPamJobTemplate={}, with name={}. Reason: {}", templatePoId, name, exception.getMessage());
        }
    }

}
