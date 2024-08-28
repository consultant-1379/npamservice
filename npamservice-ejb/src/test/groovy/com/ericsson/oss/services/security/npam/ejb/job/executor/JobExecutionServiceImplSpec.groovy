package com.ericsson.oss.services.security.npam.ejb.job.executor


import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.rule.SpyImplementation
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.sdk.eventbus.Channel
import com.ericsson.oss.itpf.sdk.eventbus.Event
import com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants
import com.ericsson.oss.services.security.npam.api.exceptions.JobExceptionFactory
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobState
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob
import com.ericsson.oss.services.security.npam.api.message.NodePamAutogeneratePasswordRequest
import com.ericsson.oss.services.security.npam.api.message.NodePamDisableRemoteManagementRequest
import com.ericsson.oss.services.security.npam.api.message.NodePamEnableRemoteManagementRequest
import com.ericsson.oss.services.security.npam.api.message.NodePamRecoveryConfigurationRequest
import com.ericsson.oss.services.security.npam.api.message.NodePamRequest
import com.ericsson.oss.services.security.npam.api.message.NodePamUpdatePasswordRequest
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs

import spock.lang.Unroll

import javax.inject.Inject

class JobExecutionServiceImplSpec extends BaseSetupForTestSpecs {

    @ObjectUnderTest
    private JobExecutionServiceImpl objUnderTest

    @SpyImplementation
    JobExceptionFactory jobExceptionFactory

    @Inject
    Channel nPamQueueChannelMock

    def successMockEvent = Mock(Event)
    def failureMockEvent = Mock(Event)

    PersistenceObject mainJobPO
    def nodes = ['RadioNode01', 'RadioNode02']

    def owner = "owner"
    def jname = "jobName"

    def setup() {
        runtimeDps.withTransactionBoundaries()
    }

    def 'cancelScheduledMainJob success - 1 job'() {
        given: 'a scheduled job'
            mainJobPO = addNpamJobWithNodes(0, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.SCHEDULED,
                    null, nodes, new ArrayList<String>(), new ArrayList<String>())
        when: 'job cancelling is requested'
            objUnderTest.cancelScheduledMainJob(mainJobPO.getAttribute("name").toString())
            def newState = mainJobPO.getAttribute("state").toString()
        then:
            newState == JobState.USER_CANCELLED.toString()
    }

    def 'cancelScheduledMainJob success - multiple jobs'() {
        given: 'a scheduled job'
        addNpamJobWithNodes(0, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.COMPLETED,
                null, nodes, new ArrayList<String>(), new ArrayList<String>())
        addNpamJobWithNodes(0, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.COMPLETED,
                null, nodes, new ArrayList<String>(), new ArrayList<String>())
        def mainJobPO = addNpamJobWithNodes(0, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.SCHEDULED,
                null, nodes, new ArrayList<String>(), new ArrayList<String>())
        when: 'job cancelling is requested'
            objUnderTest.cancelScheduledMainJob(mainJobPO.getAttribute("name").toString())
            def newState = mainJobPO.getAttribute("state").toString()
        then:
            newState == JobState.USER_CANCELLED.toString()
    }

    def 'cancelScheduledMainJob fails - multiple jobs, no one in scheduled state'() {
        given: 'a scheduled job'
        def schedule = new HashMap<String, Object>()
        schedule.execMode = "SCHEDULED"
        List<Map<String, Object>> scheduleAttributes = new ArrayList<>()
        Map<String, Object> repeatTypeProperty = new HashMap<>()
        repeatTypeProperty.name = JobSchedulerConstants.SA_REPEAT_TYPE
        repeatTypeProperty.value = JobSchedulerConstants.REPEAT_TYPE_VALUE_WEEKLY
        scheduleAttributes.add(repeatTypeProperty)
        Map<String, Object> repeatCountProperty = new HashMap<>()
        repeatCountProperty.name = JobSchedulerConstants.SA_REPEAT_COUNT
        repeatCountProperty.value = "3"
        scheduleAttributes.add(repeatCountProperty)
        schedule.scheduleAttributes = scheduleAttributes

        PersistenceObject jobTemplatePO = addNpamJobTemplate(jname, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS,
                null, schedule, ['RadioNode01'], new ArrayList<String>(), new ArrayList<>())

        def jobTemplateId = jobTemplatePO.getPoId()

        addNpamJobWithNodes(jobTemplateId, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.COMPLETED,
                null, nodes, new ArrayList<String>(), new ArrayList<String>())
        addNpamJobWithNodes(jobTemplateId, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.COMPLETED,
                null, nodes, new ArrayList<String>(), new ArrayList<String>())
        def mainJobPO = addNpamJobWithNodes(jobTemplateId, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.RUNNING,
                null, nodes, new ArrayList<String>(), new ArrayList<String>())
        when: 'job cancelling is requested'
        objUnderTest.cancelScheduledMainJob(mainJobPO.getAttribute("name").toString())
        then:
            def ex = thrown(NPAMRestErrorException)
            ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_CANCEL_JOB.message)
            ex.getInternalCode().getErrorDetails().equals(String.format(NPAMRestErrorsMessageDetails.UNPROCESSABLE_CANCEL_JOB_WRONG_STATE, mainJobPO.getAttribute("state").toString()))
    }

    def 'cancelScheduledMainJob fails - multiple jobs, all in COMPLETED state'() {
        given: 'a scheduled job'
        addNpamJobWithNodes(0, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.COMPLETED,
                null, nodes, new ArrayList<String>(), new ArrayList<String>())
        addNpamJobWithNodes(0, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.COMPLETED,
                null, nodes, new ArrayList<String>(), new ArrayList<String>())
        addNpamJobWithNodes(0, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.COMPLETED,
                null, nodes, new ArrayList<String>(), new ArrayList<String>())
        when: 'job cancelling is requested'
        objUnderTest.cancelScheduledMainJob(jname)
        then:
            def ex = thrown(NPAMRestErrorException)
            ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_CANCEL_JOB.message)
            ex.getInternalCode().getErrorDetails().equals(String.format(NPAMRestErrorsMessageDetails.UNPROCESSABLE_CANCEL_JOB_WRONG_STATE, JobState.COMPLETED))
    }

    def 'cancelScheduledMainJob fails - multiple jobs, no one in scheduled state, someone completed, more than "all-1" '() {
        given: 'a scheduled job'
        addNpamJobWithNodes(0, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.COMPLETED,
                null, nodes, new ArrayList<String>(), new ArrayList<String>())
        addNpamJobWithNodes(0, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.COMPLETED,
                null, nodes, new ArrayList<String>(), new ArrayList<String>())
        addNpamJobWithNodes(0, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.RUNNING,
                null, nodes, new ArrayList<String>(), new ArrayList<String>())
        def mainJobPO = addNpamJobWithNodes(0, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.RUNNING,
                null, nodes, new ArrayList<String>(), new ArrayList<String>())
        when: 'job cancelling is requested'
        objUnderTest.cancelScheduledMainJob(mainJobPO.getAttribute("name").toString())
        then:
            def ex = thrown(NPAMRestErrorException)
            ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_CANCEL_JOB.message)
            ex.getInternalCode().getErrorDetails().equals(String.format(NPAMRestErrorsMessageDetails.UNPROCESSABLE_CANCEL_JOB_WRONG_STATE, mainJobPO.getAttribute("state").toString()))
    }

    def 'cancelScheduledMainJob failure when job is already completed'() {
        given: 'a job with COMPLETED state'
            mainJobPO = addNpamJobWithNodes(1, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.COMPLETED, null, nodes, new ArrayList<String>(), new ArrayList<String>())
        when: 'a job cancelling is requested'
            objUnderTest.cancelScheduledMainJob(jname)
        then:
            def ex = thrown(NPAMRestErrorException)
            ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_CANCEL_JOB.message)
            ex.getInternalCode().getErrorDetails().equals(String.format(NPAMRestErrorsMessageDetails.UNPROCESSABLE_CANCEL_JOB_WRONG_STATE, JobState.COMPLETED))
    }

    def 'cancelScheduledMainJob failure when job is already deleted'() {
        given: 'a job with USER_CANCELLED state'
            mainJobPO = addNpamJobWithNodes(1, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.USER_CANCELLED, null, nodes, new ArrayList<String>(), new ArrayList<String>())
        when: 'a job cancelling is requested'
          objUnderTest.cancelScheduledMainJob(jname)
        then:
            def ex = thrown(NPAMRestErrorException)
            ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_CANCEL_JOB.message)
            ex.getInternalCode().getErrorDetails().equals(String.format(NPAMRestErrorsMessageDetails.UNPROCESSABLE_CANCEL_JOB_WRONG_STATE, JobState.USER_CANCELLED))
    }

    @Unroll
    def 'cancelScheduledMainJob failure when job is not periodic'(execMode) {
        given: 'a job not in scheduled state'
            def schedule = new HashMap<String, Object>()
            schedule.execMode = execMode
            schedule.scheduleAttributes = []

            PersistenceObject jobTemplatePO = addNpamJobTemplate(jname, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS,
                    null, schedule, ['RadioNode01'], new ArrayList<String>(), new ArrayList<>())

            mainJobPO = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.SUBMITTED, null, nodes, new ArrayList<String>(), new ArrayList<String>())

        when: 'a job cancelling is requested'
            objUnderTest.cancelScheduledMainJob(jname)
        then:
            def ex = thrown(NPAMRestErrorException)
            ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_CANCEL_JOB.message)
            ex.getInternalCode().getErrorDetails().equals(String.format(NPAMRestErrorsMessageDetails.UNPROCESSABLE_CANCEL_JOB_WRONG_STATE, JobState.SUBMITTED))
        where:
            execMode    | _
            "IMMEDIATE" | _
            "SCHEDULED" | _
    }

    def 'cancelScheduledMainJob failure when job is in state $state'(state, exception, errorDetails, detail) {
        given: 'a job scheduled execMode'
            def schedule = new HashMap<String, Object>()
            schedule.execMode = "SCHEDULED"

            List<Map<String, Object>> scheduleAttributes = new ArrayList<>()
            Map<String, Object> repeatTypeProperty = new HashMap<>()
            repeatTypeProperty.name = JobSchedulerConstants.SA_REPEAT_TYPE
            repeatTypeProperty.value = JobSchedulerConstants.REPEAT_TYPE_VALUE_WEEKLY
            scheduleAttributes.add(repeatTypeProperty)
            Map<String, Object> repeatCountProperty = new HashMap<>()
            repeatCountProperty.name = JobSchedulerConstants.SA_REPEAT_COUNT
            repeatCountProperty.value = "3"
            scheduleAttributes.add(repeatCountProperty)
            schedule.scheduleAttributes = scheduleAttributes

            PersistenceObject jobTemplatePO = addNpamJobTemplate(jname, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS,
                    null,  schedule, ['RadioNode01'], new ArrayList<String>(), new ArrayList<>())

        and: 'with state = $state'
            mainJobPO = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, state,
                    null, nodes, new ArrayList<String>(), new ArrayList<String>())

        when: 'a job cancelling is requested'
            objUnderTest.cancelScheduledMainJob(jname)
        then:
            def ex = thrown(NPAMRestErrorException)
            ex.getInternalCode().getMessage().equals(exception)
            ex.getInternalCode().getErrorDetails().equals(String.format(errorDetails, detail))
        where:
            state               | exception                                                     | errorDetails                                                        | detail
            JobState.SUBMITTED  | NPamRestErrorMessage.UNPROCESSABLE_ENTITY_CANCEL_JOB.message  | NPAMRestErrorsMessageDetails.UNPROCESSABLE_CANCEL_JOB_WRONG_STATE | JobState.SUBMITTED
            JobState.RUNNING    | NPamRestErrorMessage.UNPROCESSABLE_ENTITY_CANCEL_JOB.message  | NPAMRestErrorsMessageDetails.UNPROCESSABLE_CANCEL_JOB_WRONG_STATE | JobState.RUNNING
            JobState.CREATED    | NPamRestErrorMessage.UNPROCESSABLE_ENTITY_CANCEL_JOB.message  | NPAMRestErrorsMessageDetails.UNPROCESSABLE_CANCEL_JOB_WRONG_STATE | JobState.CREATED
    }

    def 'cancelScheduledMainJob fails when jobName null '() {
        given: 'a job exists'
            mainJobPO = addNpamJobWithNodes(1, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.USER_CANCELLED,
                    null, nodes, new ArrayList<String>(), new ArrayList<String>())
        when: 'a job cancelling is requested with jobName null'
            objUnderTest.cancelScheduledMainJob(null)
        then:
            def ex = thrown(NPAMRestErrorException)
            ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_NAME.message)
    }

    def 'cancelScheduledMainJob fails when jobName not exist '() {
        given: 'a job exists'
        mainJobPO = addNpamJobWithNodes(1, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.USER_CANCELLED,
                null, nodes, new ArrayList<String>(), new ArrayList<String>())
        when: 'a job cancelling is requested with not existing jobName'
        objUnderTest.cancelScheduledMainJob("nonExistingJobName")
        then:
        thrown(NPAMRestErrorException)
    }

    def 'cancelScheduledMainJob generic failure '() {
        given: 'a job in scheduled state'
            def schedule = new HashMap<String, Object>()
            schedule.execMode = "SCHEDULED"
            mainJobPO = addNpamJobWithNodes(12, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.SCHEDULED,
                    null, nodes, new ArrayList<String>(), new ArrayList<String>())
        and: 'with a wrong parameter'
            mainJobPO.setAttribute(NPamJob.JOB_RESULT, "wrong")
        when: 'a job cancelling is requested'
            objUnderTest.cancelScheduledMainJob(jname)
        then:
            thrown(Exception)
    }

    @Unroll
    def 'createNodePamRequestBasedOnJobType jobType=#jobType'(jobType, expectedClass) {
        when:
            Map<String, List<String>> nodeCredentialsFromFile = new HashMap<>()
            NodePamRequest nodePamRequest = objUnderTest.createNodePamRequestBasedOnJobType("owner", jobType, new ArrayList<>(), 1, "RadioNode1", nodeCredentialsFromFile, 10)
        then:
            expectedClass.isAssignableFrom(nodePamRequest.getClass())
        where:
            jobType                                             || expectedClass
            JobType.ROTATE_NE_ACCOUNT_CREDENTIALS               || NodePamUpdatePasswordRequest.class
            JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE     || NodePamUpdatePasswordRequest.class
            JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED || NodePamAutogeneratePasswordRequest.class
            JobType.CREATE_NE_ACCOUNT                           || NodePamEnableRemoteManagementRequest.class
            JobType.DETACH_NE_ACCOUNT                           || NodePamDisableRemoteManagementRequest.class
            JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION   || NodePamRecoveryConfigurationRequest.class
    }

    def 'createNodePamRequestBasedOnJobType update password with username'() {
        when:
        def List<JobProperty> jpl = new ArrayList()
        def JobProperty jpp = new JobProperty("PASSWORD", "@345CCddllKP")
        jpl.add(jpp)
        def JobProperty jpu = new JobProperty("USERNAME","TestUser")
        jpl.add(jpu)
        NodePamRequest nodePamRequest = (NodePamUpdatePasswordRequest)objUnderTest.createNodePamRequestBasedOnJobType("owner", JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, jpl, 1, "RadioNode1", null, 10)
        then:
        nodePamRequest.getNextUser().equals("TestUser")
        nodePamRequest.getNextPasswd().equals("@345CCddllKP")
    }

    def 'createNodePamRequestBasedOnJobType update password'() {
        when:
        def List<JobProperty> jpl = new ArrayList()
        def JobProperty jpp = new JobProperty("PASSWORD", "@345CCddllKP")
        jpl.add(jpp)
        NodePamRequest nodePamRequest = (NodePamUpdatePasswordRequest)objUnderTest.createNodePamRequestBasedOnJobType("owner", JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, jpl, 1, "RadioNode1", null, 10)
        then:
        nodePamRequest.getNextPasswd().equals("@345CCddllKP")
    }


    def 'sendNodeUpdateMessage with nodePamRequest=null do nothing (for coverage)'() {
        given: 'nodePamRequest=null'
            def nodePamRequest = null
        when:
            objUnderTest.sendNodeUpdateMessage( JobType.CREATE_NE_ACCOUNT, 1, nodePamRequest)
        then:
            true
    }

    def 'sendNodeUpdateMessage with nodePamRequest - success'() {
        given: 'create nPamNeJob'
            def nPamJobPO = addNpamNeJob(1, JobState.CREATED, null, 'RadioNode01')
        and: 'create nodePamRequest'
            def List<JobProperty> jpl = new ArrayList()
            def JobProperty jpp = new JobProperty("PASSWORD", "@345CCddllKP")
            jpl.add(jpp)
            NodePamRequest nodePamRequest = (NodePamUpdatePasswordRequest)objUnderTest.createNodePamRequestBasedOnJobType("owner", JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, jpl, nPamJobPO.getPoId(), "RadioNode1", null, 10)
        when:
            objUnderTest.sendNodeUpdateMessage( JobType.CREATE_NE_ACCOUNT, nPamJobPO.getPoId(), nodePamRequest)
        then:
            1 * nPamQueueChannelMock.createEvent( nodePamRequest , _) >> successMockEvent
            1 * nPamQueueChannelMock.send(successMockEvent, _)
        and: 'check status of nPamNeJob object'
            nPamJobPO.getAttribute('state') == JobState.CREATED.name()
    }

    def 'sendNodeUpdateMessage with nodePamRequest - failed 3 times with NodePamRequestQueue full exception, then success'() {
        given: 'create nPamNeJob'
            def nPamJobPO = addNpamNeJob(1, JobState.CREATED, null, 'RadioNode01')
        and: 'create nodePamRequest'
            def List<JobProperty> jpl = new ArrayList()
            def JobProperty jpp = new JobProperty("PASSWORD", "@345CCddllKP")
            jpl.add(jpp)
            NodePamRequest nodePamRequest = (NodePamUpdatePasswordRequest)objUnderTest.createNodePamRequestBasedOnJobType("owner", JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, jpl, nPamJobPO.getPoId(), "RadioNode1", null, 10)
        when:
            objUnderTest.sendNodeUpdateMessage( JobType.CREATE_NE_ACCOUNT, nPamJobPO.getPoId(), nodePamRequest)
        then:
            4 * nPamQueueChannelMock.createEvent( nodePamRequest , _) >>> [failureMockEvent, failureMockEvent, failureMockEvent, successMockEvent]
            3 * nPamQueueChannelMock.send(failureMockEvent, _) >> { throw new RuntimeException("NodePamRequestQueue is full")}
            1 * nPamQueueChannelMock.send(successMockEvent, _)
        and: 'check status of NpamNeJob object'
            nPamJobPO.getAttribute('state') == JobState.CREATED.name()
    }

    def 'sendNodeUpdateMessage with nodePamRequest - failed 10 times with NodePamRequestQueue full exception'() {
        given: 'create nPamNeJob'
            def nPamJobPO = addNpamNeJob(1, JobState.CREATED, null, 'RadioNode01')
        and: 'create nodePamRequest'
            def List<JobProperty> jpl = new ArrayList()
            def JobProperty jpp = new JobProperty("PASSWORD", "@345CCddllKP")
            jpl.add(jpp)
            NodePamRequest nodePamRequest = (NodePamUpdatePasswordRequest)objUnderTest.createNodePamRequestBasedOnJobType("owner", JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, jpl, nPamJobPO.getPoId(), "RadioNode1", null, 10)
        when:
            objUnderTest.sendNodeUpdateMessage( JobType.CREATE_NE_ACCOUNT, nPamJobPO.getPoId(), nodePamRequest)
        then:
            10 * nPamQueueChannelMock.createEvent( nodePamRequest , _) >> failureMockEvent
            10 * nPamQueueChannelMock.send(failureMockEvent, _) >> { throw new RuntimeException("NodePamRequestQueue is full")}
        and: 'check status of NpamNeJob object'
            nPamJobPO.getAttribute('state') == JobState.COMPLETED.name()
            nPamJobPO.getAttribute('result') == JobResult.FAILED.name()
            nPamJobPO.getAttribute('errorDetails') == 'NodePamRequestQueue Full'
    }

    def 'sendNodeUpdateMessage with nodePamRequest - failed with generic exception'() {
        given: 'create nPamNeJob'
            def nPamJobPO = addNpamNeJob(1, JobState.CREATED, null, 'RadioNode01')
        and: 'create nodePamRequest'
            def List<JobProperty> jpl = new ArrayList()
            def JobProperty jpp = new JobProperty("PASSWORD", "@345CCddllKP")
            jpl.add(jpp)
            NodePamRequest nodePamRequest = (NodePamUpdatePasswordRequest)objUnderTest.createNodePamRequestBasedOnJobType("owner", JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, jpl, nPamJobPO.getPoId(), "RadioNode1", null, 10)
        when:
            objUnderTest.sendNodeUpdateMessage( JobType.CREATE_NE_ACCOUNT, nPamJobPO.getPoId(), nodePamRequest)
        then:
            1 * nPamQueueChannelMock.createEvent( nodePamRequest , _) >> failureMockEvent
            1 * nPamQueueChannelMock.send(failureMockEvent, _) >> { throw new RuntimeException("Generic Exception")}
        and: 'check status of NpamNeJob object'
            nPamJobPO.getAttribute('state') == JobState.COMPLETED.name()
            nPamJobPO.getAttribute('result') == JobResult.FAILED.name()
            nPamJobPO.getAttribute('errorDetails') == 'Generic Exception'
    }

    @Unroll
    def 'createNEJobs rate limit test with #nodeNumber'() {
        given: 'create nPamJob with N nodes'
            List<String> myNode = new ArrayList<>()
            for (int i=0; i<nodeNumber; i++) {
                myNode.add('Node'+i)
            }
            mainJobPO = addNpamJobWithNodes(0, owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.SUBMITTED,
                    null, myNode, new ArrayList<String>(), new ArrayList<String>())
        when: 'run job'
            objUnderTest.runMainJob(mainJobPO.getPoId())
        then:
            nodeNumber * nPamQueueChannelMock.createEvent( _ , _) >> successMockEvent
            nodeNumber * nPamQueueChannelMock.send(successMockEvent, _)
        where:
            nodeNumber || _
            1          || _
            9          || _
            10         || _
            11         || _
            510        || _
    }
}
