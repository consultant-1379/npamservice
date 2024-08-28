package com.ericsson.oss.services.security.npam.rest.resources

import static com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants.PK_FILENAME;
import static com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants.PK_NEXT_PASSWORD;
import static com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants.PK_NEXT_USERNAME;
import static com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult.SUCCESS
import static com.ericsson.oss.services.security.npam.api.job.modelentities.JobState.COMPLETED
import static com.ericsson.oss.services.security.npam.api.job.modelentities.JobType.CREATE_NE_ACCOUNT

import javax.ws.rs.core.Response

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException
import com.ericsson.oss.services.security.npam.api.interfaces.JobGetService
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJobTemplate
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEJob
import com.ericsson.oss.services.security.npam.api.rest.NeAccountService
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus
import com.ericsson.oss.services.security.npam.ejb.job.dao.JobDpsReader
import com.ericsson.oss.services.security.npam.ejb.job.executor.JobCreationServiceImpl
import com.ericsson.oss.services.security.npam.ejb.job.impl.JobConfigurationServiceImpl
import com.ericsson.oss.services.security.npam.ejb.rest.JobServiceImpl
import com.ericsson.oss.services.security.npam.rest.api.NPAMJobTemplateJAXB
import com.ericsson.oss.services.security.npam.rest.testutil.NPAMRestTestUtilSpec

class NPAMJobListServiceRest2Spec extends NPAMRestTestUtilSpec {
    private static final String JOB_NAME = "jobName"
    private static final String ADMINISTRATOR = "administrator"

    @ObjectUnderTest
    NPAMJobServiceRest objUnderTest

    @MockedImplementation
    NodePamConfigStatus nodePamConfigStatusMock

    @MockedImplementation
    JobGetService jobGetServiceImplMock

    @MockedImplementation
    JobDpsReader jobDpsReaderMock

    @MockedImplementation
    NeAccountService neAccountServiceMock

    @ImplementationClasses
    def classes = [
        JobServiceImpl,
        JobCreationServiceImpl,
        JobConfigurationServiceImpl,
        JobProperty
    ]
    def date = new Date()
    def setup() {
        jobGetServiceImplMock.getNeJobForJobId(_) >> {
            NPamNEJob npamNeJob = new NPamNEJob();
            npamNeJob.setNeJobId(1L)
            npamNeJob.setNeName("RadioNode")
            npamNeJob.setMainJobId(2L)
            npamNeJob.setState(COMPLETED)
            npamNeJob.setResult(SUCCESS)
            List<NPamNEJob> npamNeJobList = new ArrayList();
            npamNeJobList.add(npamNeJob)
            return npamNeJobList
        }

        jobDpsReaderMock.getJobTemplate(_) >> { args ->
            NPamJobTemplate npamJobTemplate = new NPamJobTemplate()
            npamJobTemplate.setName(JOB_NAME)
            npamJobTemplate.setJobTemplateId(999999)
            npamJobTemplate.setJobType(CREATE_NE_ACCOUNT)
            npamJobTemplate.setOwner(ADMINISTRATOR)
            npamJobTemplate.setCreationTime(date)
            npamJobTemplate.setDescription("This is a job template")
            npamJobTemplate.setJobProperties(Arrays.asList(
                    new JobProperty(PK_NEXT_PASSWORD, "password"),
                    new JobProperty(PK_FILENAME, "filename.csv_123456789"),
                    new JobProperty("FAKE_PROPERTY", "fakeValue"),
                    new JobProperty(PK_NEXT_USERNAME, "testUser")))

            return npamJobTemplate
        }
    }

    def 'run get allNeJobForSpecificJob with job instance id'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true

        when: 'exec nejob list with jobInstanceId'
        def response = objUnderTest.allNeJobForSpecificJob(1L)
        def resp = response.getEntity();
        then: 'a list of NPamNeJob is returned'
        response.getStatus() == Response.Status.OK.statusCode
        resp[0].getNeName().equals("RadioNode")
        resp[0].getState().equals(COMPLETED)
        resp[0].getResult().equals(SUCCESS)
    }

    def 'run get jobTemplate with securityViolationException'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        neAccountServiceMock.getPwdInPlainText(_) >> {throw new SecurityViolationException()}
        neAccountServiceMock.getPwdInPlainTextNoRBAC(_) >> {return "username"}
        when: 'exec jobTemplateDetails with jobName'
        def response = objUnderTest.jobTemplateDetails(JOB_NAME)
        def resp = (NPAMJobTemplateJAXB) response.getEntity();
        then: 'a hidden password is returned'
        response.getStatus() == Response.Status.OK.statusCode
        JobProperty.getPropertyValue(PK_NEXT_PASSWORD, resp.getJobProperties()).equals("********")
        JobProperty.getPropertyValue(PK_NEXT_USERNAME, resp.getJobProperties()).equals("username")
    }
}
