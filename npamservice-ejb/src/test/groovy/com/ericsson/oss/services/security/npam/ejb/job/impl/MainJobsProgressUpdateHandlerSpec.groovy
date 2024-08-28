package com.ericsson.oss.services.security.npam.ejb.job.impl

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.sdk.eventbus.Event
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder

import com.ericsson.oss.services.security.npam.api.cal.CALConstants
import com.ericsson.oss.services.security.npam.api.interfaces.JobCreationService
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobState
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEJob
import com.ericsson.oss.services.security.npam.api.job.modelentities.Step
import com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccount
import com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccountUpdateStatus
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus
import com.ericsson.oss.services.security.npam.ejb.services.NodePamEncryptionManager
import com.ericsson.oss.services.security.npam.ejb.testutil.DpsQueryUtil
import spock.lang.Unroll

import javax.inject.Inject

import static com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccountUpdateStatus.ONGOING
import static com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccountUpdateStatus.FAILED
import static com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccountUpdateStatus.CONFIGURED
import static com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccountUpdateStatus.DETACHED

import static com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult.*

class MainJobsProgressUpdateHandlerSpec extends BaseSetupForTestSpecs {

    @Inject
    MainJobsProgressUpdateHandler objUnderTest

    @MockedImplementation
    JobCreationService jobCreationServiceMock

    @MockedImplementation
    NodePamConfigStatus nodePamConfigStatus

    @MockedImplementation
    NodePamEncryptionManager criptoMock

    @Inject
    DpsQueryUtil dpsQueryUtil

    @Inject
    SystemRecorder systemRecorder


    def mockedEnableEvent = Mock(Event)

    PersistenceObject mainJobPO
    PersistenceObject neJobPO1
    PersistenceObject neAccountPO1
    PersistenceObject neAccountPO2

    def nodeName = "RadioNode01"
    def nodes = [nodeName]
    def owner = "owner"

    def setup() {
        addNodeTree(null, nodeName, "SYNCHRONIZED",false)
        mainJobPO = addNpamJobWithNodes(0, owner, JobType.CREATE_NE_ACCOUNT, JobState.RUNNING, null, nodes, new ArrayList<>(), new ArrayList<>())
        neJobPO1 = addNpamNeJob(mainJobPO.poId, JobState.RUNNING, SUCCESS, nodeName )
        List<Map<String, Object>> jobPoperties = new ArrayList<>()
        Map<String, Object> ipJobPropMap = new HashMap<>()
        ipJobPropMap.put(JobProperty.JOB_PROPERTY_KEY, CALConstants.CLIENT_IP_ADDRESS)
        ipJobPropMap.put(JobProperty.JOB_PROPERTY_VALUE, "0.20.30.40")
        jobPoperties.add(ipJobPropMap)
        Map<String, Object> seJobPropMap = new HashMap<>()
        seJobPropMap.put(JobProperty.JOB_PROPERTY_KEY, CALConstants.CLIENT_SESSION_ID)
        seJobPropMap.put(JobProperty.JOB_PROPERTY_VALUE, "abc-def")
        jobPoperties.add(seJobPropMap)
        mainJobPO.setAttribute(NPamJob.JOB_PROPERTIES, jobPoperties)
        criptoMock.encryptPassword(_) >> {
            return "encryptedpassword"
        }
    }

    @Unroll
    def 'updateRunningMainJobs with NeJobState LONG RUNNNING  then neJobState COMPLETED and result FAILED '() {
        given: 'configure neJob attributes'
        neJobPO1.setAttribute(NPamJob.JOB_STATE, JobState.RUNNING.name())
        neJobPO1.setAttribute(NPamJob.JOB_START_TIME, null)
        if (neJobResult == null) {
            neJobPO1.setAttribute(NPamJob.JOB_RESULT, neJobResult)
        } else {
            neJobPO1.setAttribute(NPamJob.JOB_RESULT, neJobResult.name())
        }
        when: 'timer is running'
            objUnderTest.updateRunningMainJobs()
        then: ' send'
            def statusRes = neJobPO1.getAttribute(NPamJob.JOB_STATE)
            def resultRes = neJobPO1.getAttribute(NPamJob.JOB_RESULT)
            String errorDetailsRes = neJobPO1.getAttribute(NPamJob.JOB_ERROR_DETAILS)
            statusRes == JobState.COMPLETED.name()
            resultRes == JobResult.FAILED.name()
            errorDetailsRes.startsWith("Node configuration not started")

        where:
        neJobResult        | _
        null               | _
        JobResult.FAILED   | _
        JobResult.SUCCESS  | _
        JobResult.SKIPPED  | _
    }

    @Unroll
    def 'updateRunningMainJobs with NeJobState LONG CREATED then neJobState COMPLETED and result FAILED '() {
        given: 'configure neJob attributes'
            neJobPO1.setAttribute(NPamJob.JOB_STATE, JobState.CREATED.name())

        criptoMock.decryptPassword(_)  >> {
            throw new UnsupportedEncodingException()
        }
            neJobPO1.setAttribute(NPamJob.JOB_START_TIME, null)
            if (neJobResult == null) {
                neJobPO1.setAttribute(NPamJob.JOB_RESULT, neJobResult)
            } else {
                neJobPO1.setAttribute(NPamJob.JOB_RESULT, neJobResult.name())
            }

        when: 'timer is running'
            objUnderTest.updateRunningMainJobs()
        then: ' send'
            def statusRes = neJobPO1.getAttribute(NPamJob.JOB_STATE)
            def resultRes = neJobPO1.getAttribute(NPamJob.JOB_RESULT)
            String errorDetailsRes = neJobPO1.getAttribute(NPamJob.JOB_ERROR_DETAILS)
            statusRes == JobState.COMPLETED.name()
            resultRes == JobResult.FAILED.name()
            errorDetailsRes.startsWith("Node configuration not started")

        where:
        neJobResult        | _
        null               | _
        JobResult.FAILED   | _
        JobResult.SUCCESS  | _
        JobResult.SKIPPED  | _
    }

    @Unroll
    def 'updateRunningMainJobs with NeJobState RUNNING  remains unchanged '() {
        given: 'configure neJob attributes'
            neJobPO1.setAttribute(NPamJob.JOB_STATE, JobState.RUNNING.name())
            neJobPO1.setAttribute(NPamJob.JOB_START_TIME, new Date())
            if (neJobResult == null) {
                neJobPO1.setAttribute(NPamJob.JOB_RESULT, neJobResult)
            } else {
                neJobPO1.setAttribute(NPamJob.JOB_RESULT, neJobResult.name())
            }
        when: 'timer is running'
            objUnderTest.updateRunningMainJobs()
        then: ' send'
            def statusRes = neJobPO1.getAttribute(NPamJob.JOB_STATE)
            def resultRes = neJobPO1.getAttribute(NPamJob.JOB_RESULT)
            statusRes == JobState.RUNNING.name()
            if (expNeJobResult == null) {
                assert resultRes == expNeJobResult
            } else {
                assert resultRes == expNeJobResult.name()
            }
        where:
        neJobResult       || expNeJobResult
        null              || null
        JobResult.SUCCESS || JobResult.SUCCESS
        JobResult.SKIPPED || JobResult.SKIPPED
        JobResult.FAILED  || JobResult.FAILED
    }

    @Unroll
    def 'updateRunningMainJobs JobType=CREATE_NE_ACCOUNT NPamNEJob.STEP=WFE  check expected result '() {
        systemRecorder.isCompactAuditEnabled() >> true
        given: 'configure Job attributes'
            mainJobPO.setAttribute(NPamJob.JOB_TYPE, JobType.CREATE_NE_ACCOUNT.name())
        criptoMock.decryptPassword(_)  >> {
            throw new UnsupportedEncodingException()
        }
        and: ' configure neJob attributes'
            neJobPO1.setAttribute(NPamNEJob.STEP,Step.WFE.name())
            if (isLongOnGoing) {
                neJobPO1.setAttribute(NPamJob.JOB_END_TIME, null)
            } else {
                neJobPO1.setAttribute(NPamJob.JOB_END_TIME, new Date())
            }

        and: 'configure neAccount attributes with lastPasswordChange and lastFailed after job start'
            if (neAccountExist) {
                neAccountPO1 = addNeAccount(nodeName, 1, neAccountStatus.name())
                neAccountPO1.setAttribute("lastPasswordChange",new Date(System.currentTimeMillis() + 3000))
                neAccountPO1.setAttribute("lastFailed",new Date(System.currentTimeMillis() + 3000))
        }
        when: 'timer is running'
            objUnderTest.updateRunningMainJobs()
        then: ' send'
            def statusRes = neJobPO1.getAttribute(NPamJob.JOB_STATE)
            def resultRes = neJobPO1.getAttribute(NPamJob.JOB_RESULT)
            statusRes == expNeJobStatus.name()
            resultRes == expNeJobResult.name()
            checkNeAccountStatusForEnable (1 , neAccountExist, expNeJobStatus, expNeAccountStatus)

        where:
        neAccountExist  |  neAccountStatus  | isLongOnGoing  || expNeJobStatus                 | expNeJobResult     | expNeAccountStatus
        true            |  ONGOING          | false          || JobState.RUNNING               | JobResult.SUCCESS  | ONGOING
        true            |  ONGOING          | true           || JobState.COMPLETED             | JobResult.FAILED   | FAILED
        true            |  CONFIGURED       | false          || JobState.COMPLETED             | JobResult.SUCCESS  | CONFIGURED
        true            |  FAILED           | false          || JobState.COMPLETED             | JobResult.FAILED   | FAILED
        true            |  DETACHED         | false          || JobState.RUNNING               | JobResult.SUCCESS | DETACHED
        true            |  DETACHED         | true           || JobState.COMPLETED             | JobResult.FAILED  | FAILED
        false           |  _                | false          || JobState.RUNNING               | JobResult.SUCCESS  | null
        false           |  _                | true           || JobState.COMPLETED             | JobResult.FAILED   | FAILED
    }

    @Unroll
    def 'updateRunningMainJobs JobType=CREATE_NE_ACCOUNT NPamNEJob.STEP=WFE  check expected result with timeout exception '() {
        given: 'configure Job attributes'
            mainJobPO.setAttribute(NPamJob.JOB_TYPE, JobType.CREATE_NE_ACCOUNT.name())
        and: ' configure neJob attrubutes'
            neJobPO1.setAttribute(NPamNEJob.STEP,Step.WFE.name())
            if (isLongOnGoing) {
                neJobPO1.setAttribute(NPamJob.JOB_END_TIME, null)
            } else {
                neJobPO1.setAttribute(NPamJob.JOB_END_TIME, new Date())
            }

        and: 'configure neAccount attributes with lastPasswordChange and lastFailed before job start'
            if (neAccountExist) {
                neAccountPO1 = addNeAccount(nodeName, 1, neAccountStatus.name())
                neAccountPO1.setAttribute("lastPasswordChange",new Date(System.currentTimeMillis() - 3000))
                neAccountPO1.setAttribute("lastFailed",new Date(System.currentTimeMillis() - 3000))
            }
        when: 'timer is running'
            objUnderTest.updateRunningMainJobs()
        then: ' send'
            def statusRes = neJobPO1.getAttribute(NPamJob.JOB_STATE)
            def resultRes = neJobPO1.getAttribute(NPamJob.JOB_RESULT)
            statusRes == expNeJobStatus.name()
            resultRes == expNeJobResult.name()
            checkNeAccountStatusForEnable (1 , neAccountExist, expNeJobStatus, expNeAccountStatus)

        where:
        neAccountExist  |  neAccountStatus  | isLongOnGoing  || expNeJobStatus                 | expNeJobResult     | expNeAccountStatus
        true            |  CONFIGURED       | true           || JobState.COMPLETED             | JobResult.FAILED   | FAILED
        true            |  FAILED           | true           || JobState.COMPLETED             | JobResult.FAILED   | FAILED
    }

    @Unroll
    def 'updateRunningMainJobs JobType=CREATE_NE_ACCOUNT nNPamNEJob.STEP=WFE and isCBRSDomain Enabled  check expected result '() {
        given: 'configure Job attributes'
            mainJobPO.setAttribute(NPamJob.JOB_TYPE, JobType.CREATE_NE_ACCOUNT.name())
        and: ' configure neJob attrubutes'
            neJobPO1.setAttribute(NPamNEJob.STEP,Step.WFE.name())
            if (isLongOnGoing) {
                neJobPO1.setAttribute(NPamJob.JOB_END_TIME, null)
            } else {
                neJobPO1.setAttribute(NPamJob.JOB_END_TIME, new Date())
            }

        and: 'configure neAccount attributes with lastPasswordChange and lastFailed after job start'
            nodePamConfigStatus.isCbrsDomainEnabled() >> true
            if (neAcc_1_Exist) {
                neAccountPO1 = addNeAccount(nodeName, 1, neAcc_1_Status.name())
                neAccountPO1.setAttribute("lastPasswordChange",new Date(System.currentTimeMillis() + 3000))
                neAccountPO1.setAttribute("lastFailed",new Date(System.currentTimeMillis() + 3000))
            }
            if (neAcc_2_Exist) {
                neAccountPO2 = addNeAccount(nodeName, 2, neAcc_2_Status.name())
                neAccountPO2.setAttribute("lastPasswordChange",new Date(System.currentTimeMillis() + 3000))
                neAccountPO2.setAttribute("lastFailed",new Date(System.currentTimeMillis() + 3000))
            }
        when: 'timer is running'
            objUnderTest.updateRunningMainJobs()
        then: ' send'
            def statusRes = neJobPO1.getAttribute(NPamJob.JOB_STATE)
            def resultRes = neJobPO1.getAttribute(NPamJob.JOB_RESULT)
            statusRes == expNeJobStatus.name()
            resultRes == expNeJobResult.name()
            checkNeAccountStatusForEnable (1 , neAcc_1_Exist, expNeJobStatus, expNeAcc_1_Status)
            checkNeAccountStatusForEnable (2 , neAcc_2_Exist, expNeJobStatus, expNeAcc_2_Status)


        where:
        neAcc_1_Exist  |  neAcc_1_Status  | neAcc_2_Exist  |  neAcc_2_Status | isLongOnGoing  || expNeJobStatus                 | expNeJobResult     | expNeAcc_1_Status | expNeAcc_2_Status
        true           |  ONGOING         |   true         |  ONGOING        |   false        || JobState.RUNNING               | JobResult.SUCCESS  |      ONGOING      |  ONGOING
        true           |  ONGOING         |   true         |  ONGOING        |   true         || JobState.COMPLETED             | JobResult.FAILED   |      FAILED       |  FAILED
        true           |  ONGOING         |   false        |      _          |   true         || JobState.COMPLETED             | JobResult.FAILED   |      FAILED       |  FAILED
        true           |  ONGOING         |   false        |      _          |   false        || JobState.RUNNING               | JobResult.SUCCESS  |      ONGOING      |     null
        true           |  CONFIGURED      |   false        |      _          |   false        || JobState.RUNNING               | JobResult.SUCCESS  |      CONFIGURED   |     null
        true           |  CONFIGURED      |   false        |      _          |   true         || JobState.COMPLETED             | JobResult.FAILED   |      CONFIGURED   |  FAILED
        true           |  CONFIGURED      |   true         |   FAILED        |   false        || JobState.COMPLETED             | JobResult.FAILED   |      CONFIGURED   |  FAILED
        true           |  CONFIGURED      |   true         |   CONFIGURED    |   false        || JobState.COMPLETED             | JobResult.SUCCESS  |      CONFIGURED   |  CONFIGURED
        true           |  FAILED          |   true         |   CONFIGURED    |   false        || JobState.COMPLETED             | JobResult.FAILED   |      FAILED       |  CONFIGURED
        true           |  DETACHED        |   true         |   CONFIGURED    |   false        || JobState.RUNNING               | JobResult.SUCCESS  |      DETACHED     |  CONFIGURED
        true           |  DETACHED        |   true         |   CONFIGURED    |   true         || JobState.COMPLETED             | JobResult.FAILED   |      FAILED       |  CONFIGURED
        true           |  DETACHED        |   true         |   FAILED        |   false        || JobState.RUNNING               | JobResult.SUCCESS  |      DETACHED     |  FAILED
        true           |  DETACHED        |   true         |   FAILED        |   true         || JobState.COMPLETED             | JobResult.FAILED   |      FAILED       |  FAILED
    }



    @Unroll
    def 'updateRunningMainJobs JobType=DETACH_NE_ACCOUNT nNPamNEJob.STEP=WFE  check expected result '() {
        given: 'configure Job attributes'
            mainJobPO.setAttribute(NPamJob.JOB_TYPE, JobType.DETACH_NE_ACCOUNT.name())
        and: ' configure neJob attrubutes'
            neJobPO1.setAttribute(NPamNEJob.STEP,Step.WFE.name())
        if (isLongOnGoing) {
            neJobPO1.setAttribute(NPamJob.JOB_END_TIME, null)
        } else {
            neJobPO1.setAttribute(NPamJob.JOB_END_TIME, new Date())
        }

        and: 'configure neAccount attributes'
        if (neAccountExist) {
            neAccountPO1 = addNeAccount(nodeName, 1, neAccountStatus.name())
        }
        when: 'timer is running'
        objUnderTest.updateRunningMainJobs()
        then: ' send'
        def statusRes = neJobPO1.getAttribute(NPamJob.JOB_STATE)
        def resultRes = neJobPO1.getAttribute(NPamJob.JOB_RESULT)
        statusRes == expNeJobStatus.name()
        if (expNeJobResult != null) {
            resultRes == expNeJobResult.name()
        }
        checkNeAccountStatusForDisable (1 , neAccountExist, expNeAccountStatus)

        where:
        neAccountExist  |  neAccountStatus  | isLongOnGoing  || expNeJobStatus                 | expNeJobResult      | expNeAccountStatus
        true            |  ONGOING          | false          || JobState.RUNNING               | JobResult.SUCCESS   | ONGOING
        true            |  ONGOING          | true           || JobState.COMPLETED             | JobResult.FAILED    | FAILED
        true            |  CONFIGURED       | false          || JobState.RUNNING               | JobResult.SUCCESS   | CONFIGURED
        true            |  CONFIGURED       | true           || JobState.COMPLETED             | JobResult.FAILED    |  FAILED
        true            |  FAILED           | false          || JobState.RUNNING               | JobResult.SUCCESS   | FAILED      // fake expected result
        true            |  FAILED           | true           || JobState.COMPLETED             | JobResult.FAILED    | FAILED
        true            |  DETACHED         | false          || JobState.COMPLETED             | JobResult.SUCCESS   | DETACHED
        false           |  _                | false          || JobState.COMPLETED             | JobResult.SUCCESS   | null
    }

    @Unroll
    def 'updateRunningMainJobs JobType=DETACH_NE_ACCOUNT nNPamNEJob.STEP=WFE  and isCBRSDomain Enabled check expected result '() {
        given: 'configure Job attributes'
            mainJobPO.setAttribute(NPamJob.JOB_TYPE, JobType.DETACH_NE_ACCOUNT.name())
        and: ' configure neJob attrubutes'
            neJobPO1.setAttribute(NPamNEJob.STEP,Step.WFE.name())
            if (isLongOnGoing) {
                neJobPO1.setAttribute(NPamJob.JOB_END_TIME, null)
            } else {
                neJobPO1.setAttribute(NPamJob.JOB_END_TIME, new Date())
            }

        and: 'configure neAccount attributes'
            nodePamConfigStatus.isCbrsDomainEnabled() >> true
            if (neAcc_1_Exist) {
                neAccountPO1 = addNeAccount(nodeName, 1, neAcc_1_Status.name())
            }
            if (neAcc_2_Exist) {
                neAccountPO2 = addNeAccount(nodeName, 2, neAcc_2_Status.name())
            }
        when: 'timer is running'
            objUnderTest.updateRunningMainJobs()
        then: ' send'
            def statusRes = neJobPO1.getAttribute(NPamJob.JOB_STATE)
            def resultRes = neJobPO1.getAttribute(NPamJob.JOB_RESULT)
            statusRes == expNeJobStatus.name()
            resultRes == expNeJobResult.name()
            checkNeAccountStatusForDisable (1 , neAcc_1_Exist, expNeAcc_1_Status)
            checkNeAccountStatusForDisable (2 , neAcc_2_Exist, expNeAcc_2_Status)

        where:
        neAcc_1_Exist  |  neAcc_1_Status  | neAcc_2_Exist  |  neAcc_2_Status | isLongOnGoing  || expNeJobStatus                 | expNeJobResult     | expNeAcc_1_Status | expNeAcc_2_Status
        true           |  ONGOING         |   true         |  ONGOING        |   false        || JobState.RUNNING               | JobResult.SUCCESS  |      ONGOING      |  ONGOING
        true           |  ONGOING         |   true         |  ONGOING        |   true         || JobState.COMPLETED             | JobResult.FAILED   |      FAILED       |  FAILED
        true           |  ONGOING         |   false        |      _          |   false        || JobState.RUNNING               | JobResult.SUCCESS  |      ONGOING      |     null
        true           |  CONFIGURED      |   false        |      _          |   false        || JobState.RUNNING               | JobResult.SUCCESS  |      CONFIGURED   |     null
        true           |  CONFIGURED      |   true         |   DETACHED      |   true         || JobState.COMPLETED             | JobResult.FAILED   |      FAILED       |  DETACHED
        true           |  CONFIGURED      |   true         |   FAILED        |   false        || JobState.RUNNING               | JobResult.SUCCESS  |      CONFIGURED   |  FAILED
        true           |  CONFIGURED      |   true         |   CONFIGURED    |   false        || JobState.RUNNING               | JobResult.SUCCESS  |      CONFIGURED   |  CONFIGURED
        true           |  FAILED          |   true         |   CONFIGURED    |   false        || JobState.RUNNING               | JobResult.SUCCESS  |      FAILED       |  CONFIGURED
        true           |  DETACHED        |   true         |   CONFIGURED    |   false        || JobState.RUNNING               | JobResult.SUCCESS  |      DETACHED     |  CONFIGURED
        true           |  DETACHED        |   true         |   CONFIGURED    |   true         || JobState.COMPLETED             | JobResult.FAILED   |      DETACHED     |  FAILED
        true           |  DETACHED        |   true         |   FAILED        |   false        || JobState.RUNNING               | JobResult.SUCCESS  |      DETACHED     |  FAILED
        true           |  DETACHED        |   true         |   FAILED        |   true         || JobState.COMPLETED             | JobResult.FAILED   |      DETACHED     |  FAILED
    }


    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
            objUnderTest.toString()
        then:
            true
    }

    private void checkNeAccountStatusForEnable(final int neAccountId, final boolean neAccountExist, final JobState expNeJobStatus, final NetworkElementAccountUpdateStatus expNeAccountStatus) {
        def neAccountObject = dpsQueryUtil.getNetworkElementAccount(nodeName, neAccountId)
        if ((! neAccountExist) && (expNeJobStatus != JobState.COMPLETED)) {
            assert neAccountObject == null
        } else {
            def status = neAccountObject.getAttribute(NetworkElementAccount.NEA_UPDATE_STATUS)
            assert status == expNeAccountStatus.name()

            if (expNeAccountStatus == FAILED) {
                assert neAccountObject.getAttribute(NetworkElementAccount.NEA_CURRENT_USER_NAME) == null
                assert neAccountObject.getAttribute(NetworkElementAccount.NEA_CURRENT_PASSWORD) == null
            } else {
                assert neAccountObject.getAttribute(NetworkElementAccount.NEA_CURRENT_USER_NAME) != null
                assert neAccountObject.getAttribute(NetworkElementAccount.NEA_CURRENT_PASSWORD) != null
            }

        }
    }

    private void checkNeAccountStatusForDisable(final int neAccountId, final boolean neAccountExist, final NetworkElementAccountUpdateStatus expNeAccountStatus) {
        def neAccountObject = dpsQueryUtil.getNetworkElementAccount(nodeName, neAccountId)
        if (! neAccountExist) {
            assert neAccountObject == null
        } else {
            def status = neAccountObject.getAttribute(NetworkElementAccount.NEA_UPDATE_STATUS)
            assert status == expNeAccountStatus.name()

            assert neAccountObject.getAttribute(NetworkElementAccount.NEA_CURRENT_USER_NAME) != null
            assert neAccountObject.getAttribute(NetworkElementAccount.NEA_CURRENT_PASSWORD) != null
        }
    }
}
