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
import com.ericsson.oss.services.security.npam.ejb.job.dao.JobDpsWriter
import com.ericsson.oss.services.security.npam.ejb.job.housekeeping.JobsHouseKeepingService
import com.ericsson.oss.services.security.npam.ejb.job.impl.JobConfigurationServiceImpl
import com.ericsson.oss.services.security.npam.ejb.testutil.DpsQueryUtil
import org.joda.time.DateTime

import javax.inject.Inject

class JobDeletionServiceDpsWriterTest extends BaseSetupForTestSpecs{

    @Inject
    DpsQueryUtil dpsQueryUtil

    @ObjectUnderTest
    private JobsHouseKeepingService objectUnderTest;

    @MockedImplementation
    JobDpsWriter JobDpsWriterMock

    @ImplementationClasses
    def classes = [JobConfigurationServiceImpl]
    PersistenceObject jobTemplatePO
    def nodes = Arrays.asList('RadioNode01', 'RadioNode02')
    def owner = "owner"
    def jobType = JobType.ROTATE_NE_ACCOUNT_CREDENTIALS
    def schedule = new HashMap<String, Object>()

    Date templateCreationDate = new DateTime().minusDays(200).toDate()
    Date mainJobEndDate = new DateTime().minusDays(190).toDate()

    int maxHKDate = 180

    PersistenceObject jobTemplatePOForException
    PersistenceObject npamNeJob1ForException
    PersistenceObject npamNeJob2ForException
    PersistenceObject mainJobPO1ForException
    def setupForExceptionScenario() {
        jobTemplatePOForException = addNpamJobTemplateForHousekeeping(
                "jobTemplateName",
                "owner",
                JobType.ROTATE_NE_ACCOUNT_CREDENTIALS,
                null,
                schedule,
                nodes,
                new ArrayList<String>(),
                new ArrayList<>(),
                templateCreationDate)
        mainJobPO1ForException = addNpamJobWithNodesForHousekeeping(
                jobTemplatePOForException.
                getPoId(),
                owner,
                jobType,
                JobState.COMPLETED,
                new ArrayList<JobProperty>(),
                nodes,
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                mainJobEndDate,
                "jobTemplateName")
        npamNeJob1ForException = addNpamNeJob(mainJobPO1ForException.getPoId(), JobState.COMPLETED, JobResult.SUCCESS, "RadioNode01")
        npamNeJob2ForException = addNpamNeJob(mainJobPO1ForException.getPoId(), JobState.COMPLETED, JobResult.SUCCESS, "RadioNode02")
    }

    def "Test triggerHouseKeepingOfJobsStartingFromTemplates() when there is NPAM template COMPLETED with Exception deleteJobHierarchy>>>exception" () {
        given: "when job is persisted in DB and Available for Deletion"
        setupForExceptionScenario()

        and: 'functionality check throws exception'
        JobDpsWriterMock.deleteJobHierarchy(*_) >> {throw new Exception("Error")}

        when: "NPAM scheduled job is persisted as per multiple scenarios"
        objectUnderTest.triggerHouseKeepingOfJobsStartingFromTemplates(maxHKDate);
        then:"All objects related to job should get deleted from database successfully"
        assert(dpsQueryUtil.findPersistentObject(jobTemplatePOForException.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(mainJobPO1ForException.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(npamNeJob1ForException.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(npamNeJob2ForException.getPoId()) != null)
    }

    def "Test triggerHouseKeepingOfJobsStartingFromTemplates() when there is NPAM template COMPLETED with Exception getLiveBucket>>>RuntimeException" () {
        given: "when job is persisted in DB and Available for Deletion"
        setupForExceptionScenario()

        and: 'functionality check throws exception'
        JobDpsWriterMock.getLiveBucket() >> { throw new RuntimeException()}

        when: "NPAM scheduled job is persisted as per multiple scenarios"
        objectUnderTest.triggerHouseKeepingOfJobsStartingFromTemplates(maxHKDate);
        then:"All objects related to job should get deleted from database successfully"
        assert(dpsQueryUtil.findPersistentObject(jobTemplatePOForException.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(mainJobPO1ForException.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(npamNeJob1ForException.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(npamNeJob2ForException.getPoId()) != null)
    }



    def "Test triggerHouseKeepingOfJobsStartingFromTemplates() when there is NPAM template COMPLETED with Exception deletePOList>>>1" () {
        given: "when job is persisted in DB and Available for Deletion"
        setupForExceptionScenario()

        and: 'functionality check throws exception'
        JobDpsWriterMock.deletePOList(*_) >> 1
        //JobDpsWriterMock.getLiveBucket() >> { throw new RuntimeException()}

        when: "NPAM scheduled job is persisted as per multiple scenarios"
        objectUnderTest.triggerHouseKeepingOfJobsStartingFromTemplates(maxHKDate);
        then:"All objects related to job should get deleted from database successfully"
        assert(dpsQueryUtil.findPersistentObject(jobTemplatePOForException.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(mainJobPO1ForException.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(npamNeJob1ForException.getPoId()) != null)
        assert(dpsQueryUtil.findPersistentObject(npamNeJob2ForException.getPoId()) != null)
    }

}
