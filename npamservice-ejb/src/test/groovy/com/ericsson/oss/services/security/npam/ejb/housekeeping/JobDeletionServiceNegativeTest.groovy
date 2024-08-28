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
package com.ericsson.oss.services.security.npam.ejb.housekeeping


import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobState
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.security.npam.ejb.job.housekeeping.JobsHouseKeepingHelperUtil
import com.ericsson.oss.services.security.npam.ejb.job.housekeeping.JobsHouseKeepingService
import com.ericsson.oss.services.security.npam.ejb.job.impl.JobConfigurationServiceImpl
import com.ericsson.oss.services.security.npam.ejb.testutil.DpsQueryUtil
import org.joda.time.DateTime

import javax.inject.Inject

class JobDeletionServiceNegativeTest extends BaseSetupForTestSpecs{

    @Inject
    DpsQueryUtil dpsQueryUtil
    
    @ObjectUnderTest
    private JobsHouseKeepingService objectUnderTest;

    @MockedImplementation
    JobsHouseKeepingHelperUtil jobsHouseKeepingHelperUtil

    @ImplementationClasses
    def classes = [JobConfigurationServiceImpl]
    def nodes = Arrays.asList('RadioNode01', 'RadioNode02')
    def owner = "owner"
    def jobType = JobType.ROTATE_NE_ACCOUNT_CREDENTIALS
    def schedule = new HashMap<String, Object>()

    Date templateCreationDate = new DateTime().minusDays(200).toDate();
    Date mainJobEndDate = new DateTime().minusDays(190).toDate();

    int maxHKDate = 180

    PersistenceObject jobTemplatePO
    PersistenceObject mainJobPO1
    PersistenceObject npamNeJob1
    PersistenceObject npamNeJob2
    def setupForTests(JobState jobState, Date tempCreationDate, Date mainJobDate) {
        jobTemplatePO = addNpamJobTemplateForHousekeeping(
                "jobTemplateName",
                "owner",
                JobType.ROTATE_NE_ACCOUNT_CREDENTIALS,
                null,
                schedule,
                nodes,
                new ArrayList<String>(),
                new ArrayList<>(),
                tempCreationDate)
        mainJobPO1 = addNpamJobWithNodesForHousekeeping(
                jobTemplatePO.getPoId(),
                owner,
                jobType,
                jobState,
                new ArrayList<JobProperty>(),
                nodes,
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                mainJobDate,
                "jobTemplateName"
        )

        npamNeJob1 = addNpamNeJob(mainJobPO1.getPoId(), jobState, JobResult.SUCCESS, "RadioNode01")
        npamNeJob2 = addNpamNeJob(mainJobPO1.getPoId(), jobState, JobResult.SUCCESS, "RadioNode02")
    }

    def "Test triggerHouseKeepingOfJobsStartingFromTemplates() when there is NPAM template COMPLETED but not expired available in Database" () {
        given: "when job is persisted in DB and Available for Deletion"
        setupForTests(JobState.COMPLETED, new Date(), new Date())

        when: "NPAM scheduled job is persisted as per multiple scenarios"
        objectUnderTest.triggerHouseKeepingOfJobsStartingFromTemplates(maxHKDate);
        then:"All objects related to job should get deleted from database successfully"
        assert(dpsQueryUtil.findPersistentObject(jobTemplatePO.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(mainJobPO1.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(npamNeJob1.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(npamNeJob2.getPoId()) != null)
    }

    def "Test triggerHouseKeepingOfJobsStartingFromTemplates() when there is NPAM template SCHEDULED but not expired available in Database" () {
        given: "when job is persisted in DB and Available for Deletion"
        setupForTests(JobState.SCHEDULED, new Date(), new Date())

        when: "NPAM scheduled job is persisted as per multiple scenarios"
        objectUnderTest.triggerHouseKeepingOfJobsStartingFromTemplates(maxHKDate);
        then:"All objects related to job should get deleted from database successfully"
        assert(dpsQueryUtil.findPersistentObject(jobTemplatePO.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(mainJobPO1.getPoId()) != null)
    }

    def "Test triggerHouseKeepingOfJobsStartingFromTemplates() when there is NPAM template USER_CANCELLED but not expired available in Database" () {
        given: "when job is persisted in DB and Available for Deletion"
        setupForTests(JobState.USER_CANCELLED, new Date(), new Date())

        when: "NPAM scheduled job is persisted as per multiple scenarios"
        objectUnderTest.triggerHouseKeepingOfJobsStartingFromTemplates(maxHKDate);
        then:"All objects related to job should get deleted from database successfully"
        assert(dpsQueryUtil.findPersistentObject(jobTemplatePO.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(mainJobPO1.getPoId()) != null)
    }

    def "Test triggerHouseKeepingOfJobsStartingFromTemplates() when there is NPAM template COMPLETED but not expired available in Database and deleteTemplate >> 0" () {
        given: "when job is persisted in DB and Available for Deletion"
        setupForTests(JobState.COMPLETED, templateCreationDate, mainJobEndDate)
        and: "deleteTemplate >> 0"
        jobsHouseKeepingHelperUtil.deleteTemplate(*_) >> 0
        jobsHouseKeepingHelperUtil.deleteJobsAndTemplateIfNecessary(*_) >> 1

        when: "NPAM scheduled job is persisted as per multiple scenarios"
        objectUnderTest.triggerHouseKeepingOfJobsStartingFromTemplates(maxHKDate);
        then:"All objects related to job should get deleted from database successfully"
        assert(dpsQueryUtil.findPersistentObject(jobTemplatePO.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(mainJobPO1.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(npamNeJob1.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(npamNeJob2.getPoId()) != null)
    }

    def "Test triggerHouseKeepingOfJobsStartingFromTemplates() when there is NPAM template COMPLETED with Exception deleteJobsAndTemplateIfNecessary>>>exception" () {
        given: "when job is persisted in DB and Available for Deletion"
        setupForTests(JobState.COMPLETED, templateCreationDate, mainJobEndDate)

        and: 'functionality check throws exception'
        jobsHouseKeepingHelperUtil.deleteJobsAndTemplateIfNecessary(*_) >> {throw new Exception("Error")}

        when: "NPAM scheduled job is persisted as per multiple scenarios"
        objectUnderTest.triggerHouseKeepingOfJobsStartingFromTemplates(maxHKDate);
        then:"All objects related to job should get deleted from database successfully"
        assert(dpsQueryUtil.findPersistentObject(jobTemplatePO.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(mainJobPO1.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(npamNeJob1.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(npamNeJob2.getPoId()) != null)
    }
}
