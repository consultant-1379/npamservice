package com.ericsson.oss.services.security.npam.ejb.job.dao

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobState
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs

class JobDpsReaderSpec extends BaseSetupForTestSpecs {

    @ObjectUnderTest
    private JobDpsReader objUnderTest

    def owner = "owner"

    def setup() {
        def jobProperties = Arrays.asList(new JobProperty("USERNAME","testUser"), new JobProperty("PASSWORD","TestPassw0rd"))
        def mainSchedule = new HashMap<>()
        mainSchedule.put("execMode", "IMMEDIATE")
        addNpamJobTemplate("jobName", "administrator", JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, jobProperties, mainSchedule, nodes, new ArrayList<String>(), new ArrayList<String>())
        addNpamJobWithNodes(1, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.SUBMITTED, null, nodes, new ArrayList<String>(), new ArrayList<String>())
    }

    def nodes = ['RadioNode01', 'RadioNode02']

    def 'test to read main jobs from DPS'() {
        given: 'a main job'
        when: 'call getMainJobs with a correct jobname'
        List<NPamJob> npamJobList = new ArrayList<>()
        npamJobList = objUnderTest.getMainJobs()
        then: 'a list of NpamJob is returned'
        npamJobList.size() == 1
    }

    def 'test to read main jobs from DPS with jobName'() {
        given: 'a main job'
        when: 'call getMainJobs with a not existing name'
        List<NPamJob> npamJobList = new ArrayList<>()
        npamJobList = objUnderTest.getMainJobs("JobName1")
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.NOT_FOUND_JOB_NAME.message)
        ex.getInternalCode().getErrorDetails().equals("JobName1")
    }

    def 'test to read jobTemplate from DPS with jobName'() {
        given: 'a JobTemplate with name JobName'
        when:'call getJobTemplate with a correct jobname'
        def npamJobTemplate = objUnderTest.getJobTemplate("jobName")
        then: 'the JobTemplate is returned'
        npamJobTemplate.getName().equals("jobName")
        npamJobTemplate.getOwner().equals("administrator")
        npamJobTemplate.getDescription().equals("some description")
    }

    def 'test to read jobTemplate from DPS with not existing jobName'() {
        given: 'a JobTemplate with name JobName'
        when: 'call getJobTemplate with a not existing name'
        def npamJobTemplate = objUnderTest.getJobTemplate("JobName1")
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.NOT_FOUND_JOB_NAME.message)
        ex.getInternalCode().getErrorDetails().equals("JobName1")
    }

    def 'test to read jobTemplate from DPS with jobName null'() {
        given: 'a JobTemplate with name JobName'
        when: 'call getJobTemplate with name null'
        def npamJobTemplate = objUnderTest.getJobTemplate()
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_NAME.message)
    }
}

