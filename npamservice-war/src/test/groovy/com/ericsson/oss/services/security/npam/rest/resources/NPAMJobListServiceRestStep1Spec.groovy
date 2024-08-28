package com.ericsson.oss.services.security.npam.rest.resources

import static com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants.PK_FILENAME;
import static com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants.PK_NEXT_PASSWORD;
import static com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants.PK_NEXT_USERNAME;
import static com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult.SUCCESS
import static com.ericsson.oss.services.security.npam.api.job.modelentities.JobState.COMPLETED
import static com.ericsson.oss.services.security.npam.api.job.modelentities.JobType.DETACH_NE_ACCOUNT
import static com.ericsson.oss.services.security.npam.api.job.modelentities.JobType.CREATE_NE_ACCOUNT

import javax.ws.rs.core.Response

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJobTemplate
import com.ericsson.oss.services.security.npam.api.rest.NeAccountService
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus
import com.ericsson.oss.services.security.npam.ejb.job.dao.JobDpsReader
import com.ericsson.oss.services.security.npam.ejb.job.executor.JobCreationServiceImpl
import com.ericsson.oss.services.security.npam.ejb.job.executor.JobGetServiceImpl
import com.ericsson.oss.services.security.npam.ejb.job.impl.JobConfigurationServiceImpl
import com.ericsson.oss.services.security.npam.ejb.rest.JobServiceImpl
import com.ericsson.oss.services.security.npam.rest.api.NPAMJobJAXB
import com.ericsson.oss.services.security.npam.rest.api.NPAMJobTemplateJAXB
import com.ericsson.oss.services.security.npam.rest.testutil.NPAMRestTestUtilSpec

class NPAMJobListServiceRest1Spec extends NPAMRestTestUtilSpec {
    private static final String JOB_NAME = "jobName"
    private static final String ADMINISTRATOR = "administrator"

    @ObjectUnderTest
    NPAMJobServiceRest objUnderTest

    @MockedImplementation
    NodePamConfigStatus nodePamConfigStatusMock

    @MockedImplementation
    JobDpsReader jobDpsReader

    @MockedImplementation
    NeAccountService neAccountServiceMock

    @ImplementationClasses
    def classes = [
        JobServiceImpl,
        JobCreationServiceImpl,
        JobGetServiceImpl,
        JobConfigurationServiceImpl
    ]
    def date = new Date()
    def setup() {
        jobDpsReader.getMainJobs(_) >> { args ->

            NPamJob npamJob = new NPamJob()
            npamJob.setName(JOB_NAME)
            npamJob.setJobId(1)
            npamJob.setJobType(CREATE_NE_ACCOUNT)
            npamJob.setNumberOfNetworkElements(1)
            npamJob.setResult(SUCCESS)
            npamJob.setState(COMPLETED)
            npamJob.setOwner(ADMINISTRATOR)

            NPamJob npamJob1 = new NPamJob()
            npamJob1.setName("jobName1")
            npamJob1.setJobId(2)
            npamJob1.setJobType(DETACH_NE_ACCOUNT)
            npamJob1.setNumberOfNetworkElements(3)
            npamJob1.setResult(SUCCESS)
            npamJob1.setState(COMPLETED)
            npamJob1.setOwner(ADMINISTRATOR)

            if (args[0] != null && args[0] == JOB_NAME) {
                return Arrays.asList(npamJob)
            }
            else {
                return Arrays.asList(npamJob, npamJob1)
            }
        }
        jobDpsReader.getJobTemplate(_) >> { args ->
            NPamJobTemplate npamJobTemplate = new NPamJobTemplate()
            npamJobTemplate.setName(JOB_NAME)
            npamJobTemplate.setJobTemplateId(999999)
            npamJobTemplate.setJobType(CREATE_NE_ACCOUNT)
            npamJobTemplate.setOwner(ADMINISTRATOR)
            npamJobTemplate.setCreationTime(date)
            npamJobTemplate.setDescription("This is a job template")
            npamJobTemplate.setJobProperties(Arrays.asList(new JobProperty(PK_NEXT_USERNAME, "username"), new JobProperty(PK_NEXT_PASSWORD, "password"),
                    new JobProperty(PK_FILENAME, "filename.csv_123456789"), new JobProperty("FAKE_PROPERTY", "fakeValue")))

            if (args[0] != null && args[0] == JOB_NAME) {
                return npamJobTemplate
            }
            else {
                throw new NPAMRestErrorException(NPamRestErrorMessage.NOT_FOUND_JOB_NAME, args[0]);
            }
        }
    }

    def 'run job list'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        when: 'exec job list'
        def response = objUnderTest.jobList()
        def resp = (List<NPAMJobJAXB>) response.getEntity();

        then: 'a list with 2 Npam element is returned'
        response.getStatus() == Response.Status.OK.statusCode
        resp.size() == 2
    }

    def 'run job list with jobName'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true

        when: 'exec job list with jobName'
        def response = objUnderTest.jobList(JOB_NAME)
        def resp = (List<NPAMJobJAXB>) response.getEntity();

        then: 'a list with one Npam element is returned'
        response.getStatus() == Response.Status.OK.statusCode
        resp.get(0).getName().equals(JOB_NAME)
        resp.get(0).getState().equals("COMPLETED")
        resp.get(0).getResult().equals("SUCCESS")
        resp.get(0).getOwner().equals(ADMINISTRATOR)
    }

    def 'run job list with feature disabled'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> false

        when: 'exec job list with feature disabled'
        def response = objUnderTest.jobList(JOB_NAME)

        then: 'an exception about feature disabled is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.FORBIDDEN_FEATURE_DISABLED.message)
    }

    def 'run get jobTemplate with jobName'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        neAccountServiceMock.getPwdInPlainText(_) >> {return "TestPassw0rd"}
        neAccountServiceMock.getPwdInPlainTextNoRBAC(_) >> {return "username"}
        when: 'exec job list with jobName'
        def response = objUnderTest.jobTemplateDetails(JOB_NAME)
        def resp = (NPAMJobTemplateJAXB) response.getEntity();
        System.out.println("RESP " + resp)
        then: 'a NpamJobTemplate is returned'
        response.getStatus() == Response.Status.OK.statusCode
        resp.getName().equals(JOB_NAME)
        resp.getDescription().equals("This is a job template")
        resp.getCreationTime().equals(date)
        resp.getOwner().equals(ADMINISTRATOR)
    }

    def 'run get jobTemplate with not existing jobName'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true

        when: 'exec job list with not existing jobName'
        def response = objUnderTest.jobTemplateDetails("jobName1")

        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.NOT_FOUND_JOB_NAME.message)
        ex.getInternalCode().getErrorDetails().equals("jobName1")
    }

    def 'run get jobTemplate with feature disabled'() {
        given: 'NPAM_CONFIG not configured in dps'
        nodePamConfigStatusMock.isEnabled() >> false

        when: 'exec jobTemplate with feature disabled'
        def response = objUnderTest.jobTemplateDetails(JOB_NAME)

        then: 'an exception about feature disabled is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.FORBIDDEN_FEATURE_DISABLED.message)
    }
}
