package com.ericsson.oss.services.security.npam.ejb.job.executor

import javax.ws.rs.core.Response.Status

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobState
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus
import com.ericsson.oss.services.security.npam.ejb.job.impl.JobConfigurationServiceImpl

class JobGetServiceImplSpec extends BaseSetupForTestSpecs {

    @ObjectUnderTest
    private JobGetServiceImpl objUnderTest

    @MockedImplementation
    NodePamConfigStatus nodePamConfigStatusMock

    @ImplementationClasses
    def classes = [JobConfigurationServiceImpl]
    PersistenceObject jobTemplatePO
    def nodes = Arrays.asList('RadioNode01', 'RadioNode02')
    def owner = "owner"
    def jobType = JobType.ROTATE_NE_ACCOUNT_CREDENTIALS
    def schedule = new HashMap<String, Object>()


    private static final int FIRST_EXECUTION_INDEX = 0;
    long poId1 = 0
    long poId2 = 0
    def setup() {
        runtimeDps.withTransactionBoundaries()
        PersistenceObject jobTemplatePO = addNpamJobTemplate("jobTemplateName", "owner", JobType.ROTATE_NE_ACCOUNT_CREDENTIALS,
                null, schedule, ['RadioNode01'], new ArrayList<String>(), new ArrayList<>())
        PersistenceObject mainJobPO1 = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, jobType, JobState.COMPLETED, new ArrayList<JobProperty>(), nodes,
                new ArrayList<>(),
                new ArrayList<>()
                )
        PersistenceObject mainJobPO2 = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, jobType, JobState.SCHEDULED, new ArrayList<JobProperty>(), nodes,
                new ArrayList<>(),
                new ArrayList<>()
                )
        poId1 = mainJobPO1.getPoId()
        poId2 =  mainJobPO2.getPoId()
        addNpamNeJob(mainJobPO1.getPoId(), JobState.COMPLETED, JobResult.SUCCESS, "RadioNode01")
        addNpamNeJob(mainJobPO1.getPoId(), JobState.COMPLETED, JobResult.SUCCESS, "RadioNode02")
    }

    def 'getNeJobForJobId from npamjob completed'() {
        given: 'a completed job'
        nodePamConfigStatusMock.isEnabled() >> true
        when: 'retrieve ne related to npamJob'
        def nPamNeJobs = objUnderTest.getNeJobForJobId(poId1)
        then:
        nPamNeJobs.size() == 2
    }

    def 'getNeJobForJobId from npamjob scheduled'() {
        given: 'a scheduled job'
        nodePamConfigStatusMock.isEnabled() >> true
        when: 'retrieve ne related to npamJob'
        def nPamNeJobs = objUnderTest.getNeJobForJobId(poId2)
        then:
        nPamNeJobs.size() == 0
    }

    def 'getNeJobForJobId from not existing npamjob id'() {
        given: 'a not existing npamjob id'
        nodePamConfigStatusMock.isEnabled() >> true
        when: 'retrieve ne related to npamJob'
        def nPamNeJobs = objUnderTest.getNeJobForJobId(9999L)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.NOT_FOUND_JOB_ID.message)
        ex.getInternalCode().getErrorDetails().equals("9999")
    }

    def 'getNeJobForJobId from npamjob scheduled with feature disabled'() {
        given: 'a scheduled job'
        nodePamConfigStatusMock.isEnabled() >> false
        when: 'retrieve ne related to npamJob'
        objUnderTest.getMainJobs()
        then:
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.FORBIDDEN_FEATURE_DISABLED.message)
        ex.getInternalCode().getHttpStatusCode() == Status.FORBIDDEN.getStatusCode()
    }
}
