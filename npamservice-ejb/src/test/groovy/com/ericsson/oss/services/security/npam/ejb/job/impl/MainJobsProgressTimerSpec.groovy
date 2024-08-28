package com.ericsson.oss.services.security.npam.ejb.job.impl

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.sdk.eventbus.Channel
import com.ericsson.oss.itpf.sdk.eventbus.Event
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobState
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob
import com.ericsson.oss.services.security.npam.api.message.NodePamSubmitMainJobRequest
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.security.npam.ejb.database.availability.DatabaseAvailabilityChecker
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus
import com.ericsson.oss.services.security.npam.ejb.listener.MembershipListenerInterface
import org.joda.time.DateTime
import com.ericsson.cds.cdi.support.rule.MockedImplementation

import static com.ericsson.oss.services.security.npam.api.message.NodePamMessageProperties.NODE_PAM_QUEUE_SELECTOR_KEY
import static com.ericsson.oss.services.security.npam.api.message.NodePamMessageProperties.NODE_PAM_QUEUE_SELECTOR_SUBMIT_MAIN_JOB;

import static com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult.SUCCESS
import static com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult.FAILED
import static com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult.SKIPPED

import spock.lang.Unroll

import javax.ejb.Timer
import javax.ejb.TimerService
import javax.inject.Inject


class MainJobsProgressTimerSpec extends BaseSetupForTestSpecs {

    @Inject
    MainJobsProgressTimer objUnderTest

    @Inject
    TimerService timerServiceMock

    @Inject
    Timer timrMock

    @MockedImplementation
    private DatabaseAvailabilityChecker databaseAvailabilityChecker

    @MockedImplementation
    private MembershipListenerInterface membershipListenerInterfaceMock

    @MockedImplementation
    private NodePamConfigStatus nodePamConfigStatusMocked

    @Inject
    private Channel mockedNodePamRequestQueueChannel

    def mockedEnableEvent = Mock(Event)

    PersistenceObject mainJobPO
    PersistenceObject neJobPO1
    PersistenceObject neJobPO2
    PersistenceObject neAccountPO1
    PersistenceObject neAccountPO2


    def nodes = ['RadioNode01', 'RadionNode02']

    def owner = "owner"

    def setup() {
        mainJobPO = addNpamJobWithNodes(0, owner, JobType.CREATE_NE_ACCOUNT, JobState.SCHEDULED, null, nodes, new ArrayList<>(), new ArrayList<>())
        neJobPO1 = addNpamNeJob(mainJobPO.poId, JobState.RUNNING, SUCCESS, 'RadioNode01' )
        neJobPO2 = addNpamNeJob(mainJobPO.poId, JobState.RUNNING, SUCCESS, 'RadioNode02' )
        neAccountPO1 = addNeAccount('RadioNode01', 1, "ONGOING")
        neAccountPO2 = addNeAccount('RadioNode02', 1, "COMPLETED")
    }

    @Unroll
    def 'scheduleScheduledMainJobs :  verify if the job is sent in execution when the jobStatus is #jobState and scheduledTime postponed #delayScheduleTime'() {
        given: 'is master and npam feature is enabled'
            mainJobPO.setAttribute("state",jobState.getJobStateName())
            if (delayScheduleTime) {
                DateTime endTime = new DateTime(new Date());
                endTime = endTime.plusDays(1)
                mainJobPO.setAttribute("scheduledTime", endTime.toDate() )
            }
        and:
            databaseAvailabilityChecker.isAvailable() >> true
            membershipListenerInterfaceMock.isMaster() >> true
            nodePamConfigStatusMocked.isEnabled() >> true
        when: 'timer is running'
            objUnderTest.mainJobsTimer(timrMock)
        then: ' send enableRemoteManagementEvent'
            sendInExecution * mockedNodePamRequestQueueChannel.createEvent( { mainJobRequest ->
                mainJobRequest as NodePamSubmitMainJobRequest && mainJobRequest.getJobId() == mainJobPO.poId }, _ as String) >> mockedEnableEvent
            sendInExecution * mockedNodePamRequestQueueChannel.send(mockedEnableEvent, { eventConfiguration ->
                eventConfiguration.eventProperties.get(NODE_PAM_QUEUE_SELECTOR_KEY) == NODE_PAM_QUEUE_SELECTOR_SUBMIT_MAIN_JOB })
        and: "check main Job status"
            def statusRes = mainJobPO.getAttribute(NPamJob.JOB_STATE)
             statusRes == expectedOutputJobState.name()
        where:
        jobState                    | delayScheduleTime     || sendInExecution  | expectedOutputJobState
        JobState.SCHEDULED          |    false              ||  1               |   JobState.SUBMITTED
        JobState.CREATED            |    false              ||  0               |   JobState.CREATED
        JobState.COMPLETED          |    false              ||  0               |   JobState.COMPLETED
        JobState.RUNNING            |    false              ||  0               |   JobState.RUNNING
        JobState.SUBMITTED          |    false              ||  0               |   JobState.SUBMITTED
        JobState.USER_CANCELLED     |    false              ||  0               |   JobState.USER_CANCELLED
        JobState.SCHEDULED          |    true               ||  0               |   JobState.SCHEDULED
        JobState.CREATED            |    true               ||  0               |   JobState.CREATED
        JobState.COMPLETED          |    true               ||  0               |   JobState.COMPLETED
        JobState.RUNNING            |    true               ||  0               |    JobState.RUNNING
        JobState.SUBMITTED          |    true               ||  0               |   JobState.SUBMITTED
        JobState.USER_CANCELLED     |    true               ||  0               |   JobState.USER_CANCELLED
    }

    @Unroll
    def 'Timer scheduleScheduledMainJobs : validate the timer phase and if the timer is always reactivated '() {
        given: 'is master and npam feature is enabled'
            mainJobPO.setAttribute("state",JobState.SCHEDULED.name())
            if (forceException) {
                mainJobPO.setAttribute("selectedNEs", nodes)
            }
        and: 'configure phase'
            objUnderTest.mainJobSchedulerPhase = phase
        and:
            databaseAvailabilityChecker.isAvailable() >> true
            membershipListenerInterfaceMock.isMaster() >> true
            nodePamConfigStatusMocked.isEnabled() >> true
        when: 'timer is running'
            objUnderTest.mainJobsTimer(timrMock)
        then: ' send enableRemoteManagementEvent'
            sendInExecution * mockedNodePamRequestQueueChannel.createEvent( { mainJobRequest ->
            mainJobRequest as NodePamSubmitMainJobRequest && mainJobRequest.getJobId() == mainJobPO.poId }, _ as String) >> mockedEnableEvent
            sendInExecution * mockedNodePamRequestQueueChannel.send(mockedEnableEvent, { eventConfiguration ->
            eventConfiguration.eventProperties.get(NODE_PAM_QUEUE_SELECTOR_KEY) == NODE_PAM_QUEUE_SELECTOR_SUBMIT_MAIN_JOB })
        and: ' new timer is configured'
            1 * timerServiceMock.createSingleActionTimer(*_)
        where:
            phase       |  forceException   ||  sendInExecution
            true        |    false          ||      1
            true        |    true           ||      0
            false       |     false         ||      0
    }

    @Unroll
    def 'Timer updateRunningMainJobs : update job Result and progressPercentage with NeJob 1 = #ne1_JobStatus,#ne1_JobResult and NeJob 2 = #ne2_JobStatus,#ne2_JobResult '() {
        given: ' configure  Ne Job 1  status and result'
            neJobPO1.setAttribute("state",ne1_JobStatus.name())
            neJobPO1.setAttribute("result", ne1_JobResult)
        and: 'configure Ne Job 2  status and result'
            neJobPO2.setAttribute("state",ne2_JobStatus.name())
            neJobPO2.setAttribute("result", ne2_JobResult)
        and: 'configure main job status to RUNNING'
            mainJobPO.setAttribute("state",JobState.RUNNING.name())
            objUnderTest.mainJobSchedulerPhase = false
        and:
            databaseAvailabilityChecker.isAvailable() >> true
            membershipListenerInterfaceMock.isMaster() >> true
            nodePamConfigStatusMocked.isEnabled() >> true
        when: 'timer is running'
            objUnderTest.mainJobsTimer(timrMock)
        then: ' send enableRemoteManagementEvent'
            def stateRes = mainJobPO.getAttribute("state")
            def resultRes = mainJobPO.getAttribute("result")
            def progressRes = mainJobPO.getAttribute("progressPercentage")
            stateRes == mainJobExpectedState.name()
            resultRes == mainJobExpectedResult
            progressRes == mainJobExpectedProgress
        and: ' new timer is configured'
        1 * timerServiceMock.createSingleActionTimer(*_)
        where:
        ne1_JobStatus       |  ne1_JobResult  |   ne2_JobStatus       | ne2_JobResult     ||  mainJobExpectedState  |  mainJobExpectedResult  |  mainJobExpectedProgress
        JobState.COMPLETED  |  SUCCESS.name() |   JobState.COMPLETED  |  SUCCESS.name()   ||  JobState.COMPLETED    |   SUCCESS.name()        |  100.0
        JobState.COMPLETED  |  SUCCESS.name() |   JobState.RUNNING    |  null             ||  JobState.RUNNING      |   null                  |  50.0
        JobState.RUNNING    |    null         |   JobState.RUNNING    |  null             ||  JobState.RUNNING      |   null                  |  0.0
        JobState.COMPLETED  |  SUCCESS.name() |   JobState.COMPLETED  |  FAILED.name()    ||  JobState.COMPLETED    |   FAILED.name()         |  100.0
        JobState.COMPLETED  |  FAILED.name()  |   JobState.COMPLETED  |  FAILED.name()    ||  JobState.COMPLETED    |   FAILED.name()         |  100.0
        JobState.COMPLETED  |  FAILED.name()  |   JobState.COMPLETED  |  SKIPPED.name()   ||  JobState.COMPLETED    |   FAILED.name()         |  100.0
        JobState.COMPLETED  |  FAILED.name()  |   JobState.RUNNING    |  null             ||  JobState.RUNNING      |   null                  |  50.0
        JobState.COMPLETED  |  SUCCESS.name() |   JobState.COMPLETED  |  SKIPPED.name()   ||  JobState.COMPLETED    |   SUCCESS.name()        |  100.0
    }

    @Unroll
    def 'Timer scheduleScheduledMainJobs : no operation is executed when is NOT master and&or Database is not available '() {
        given: ' phase is true'
            objUnderTest.mainJobSchedulerPhase = true
        and: ''
            databaseAvailabilityChecker.isAvailable() >> isDbAvailable
            membershipListenerInterfaceMock.isMaster() >> isMaster
            nodePamConfigStatusMocked.isEnabled() >> isEnabled
        when: 'timer is running'
            objUnderTest.mainJobsTimer(timrMock)
        then: ' send enableRemoteManagementEvent'
        objUnderTest.mainJobSchedulerPhase == expectedPhase
        and: ' new timer is configured'
        1 * timerServiceMock.createSingleActionTimer(*_)
        where:
        isDbAvailable   |  isMaster   | isEnabled   ||  expectedPhase
        true            |    true     |   true      ||      false
        true            |    true     |   false     ||      true
        true            |    false    |   true      ||      true
        false           |    true     |   true      ||      true
    }


    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
            objUnderTest.toString()
        then:
            true
    }
}
