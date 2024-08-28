package com.ericsson.oss.services.security.npam.ejb.listener

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.SpyImplementation
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.sdk.eventbus.Channel
import com.ericsson.oss.itpf.sdk.eventbus.Event
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl
import com.ericsson.oss.itpf.sdk.security.accesscontrol.ESecuritySubject
import com.ericsson.oss.itpf.sdk.security.accesscontrol.ESecurityTarget
import com.ericsson.oss.itpf.security.cryptography.CryptographyService
import com.ericsson.oss.services.security.model.npam.event.NodePamMediationEvent
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobState
import com.ericsson.oss.services.security.npam.api.job.modelentities.Step
import com.ericsson.oss.services.security.npam.api.message.NodePamDisableFeatureRequest
import com.ericsson.oss.services.security.npam.api.message.NodePamRecoveryConfigurationRequest
import com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccountUpdateStatus
import com.ericsson.oss.services.security.npam.api.constants.NodePamEventOperation
import com.ericsson.oss.services.security.npam.api.message.NodePamAutogeneratePasswordRequest
import com.ericsson.oss.services.security.npam.api.message.NodePamDisableRemoteManagementEventRequest
import com.ericsson.oss.services.security.npam.api.message.NodePamDisableRemoteManagementRequest
import com.ericsson.oss.services.security.npam.api.message.NodePamEnableRemoteManagementEventRequest
import com.ericsson.oss.services.security.npam.api.message.NodePamEnableRemoteManagementRequest
import com.ericsson.oss.services.security.npam.api.message.NodePamEndUpdateOperation
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.security.npam.ejb.database.availability.DatabaseAvailabilityChecker
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus
import com.ericsson.oss.services.security.npam.ejb.exceptions.NodePamError
import com.ericsson.oss.services.security.npam.ejb.executor.ManagedObjectInfo
import com.ericsson.oss.services.security.npam.ejb.executor.NodeInfo
import com.ericsson.oss.services.security.npam.ejb.testutil.DpsQueryUtil
import com.ericsson.oss.services.security.npam.api.message.NodePamUpdatePasswordRequest
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType

import static com.ericsson.oss.services.security.npam.api.message.NodePamEndUpdateOperation.getKeyForEvent
import static com.ericsson.oss.services.security.npam.api.message.NodePamEndUpdateOperation.getKeyForJob
import static com.ericsson.oss.services.security.npam.ejb.exceptions.NodePamError.WARNING_MESSAGE_SKIPPED_REMOTE_MANAGEMENT_FALSE
import static com.ericsson.oss.services.security.npam.ejb.exceptions.NodePamError.WARNING_MESSAGE_SKIPPED_REMOTE_MANAGEMENT_TRUE
import static com.ericsson.oss.services.security.npam.ejb.testutil.JMSHelper.createEventForObject

import spock.lang.Unroll

import javax.inject.Inject
import com.ericsson.oss.services.security.npam.ejb.exceptions.ExceptionFactory
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_MO
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_SECURITY_MO_NAMESPACE
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_SECURITY_MO

class NodePamQueueListenerSpec extends BaseSetupForTestSpecs {

    @Inject
    NodePamQueueListener objUnderTest

    @Inject
    MembershipListenerInterface membershipListenerInterfaceMock

    @Inject
    CryptographyService criptoMock

    @Inject
    private Channel mockedNodePamRequestQueueChannel

    @MockedImplementation
    NodePamConfigStatus nodePamConfigStatusMocked

    @Inject
    private EAccessControl mockedAccessControl;

    @Inject
    DpsQueryUtil dpsQueryUtil

    def mockedEvent = Mock(Event)

    @Inject
    NodePamEndUpdateOperationMap endUpdateOperationMap;

    @Inject
    private EventSender<NodePamMediationEvent> mediationEventSenderMock

    @MockedImplementation
    DatabaseAvailabilityChecker databaseAvailabilityCheckerMock

    @SpyImplementation    //this annotation allow to spy real object exection
    ExceptionFactory exceptionFactory

    @ImplementationClasses
    def classes = [
    ]

    def userId = 'userId'
    def requestId = 'requestId'
    PersistenceObject neJobPO1

    private void setOperationFinishedOnNodeForJob(final String nodeName) {
        final String key = getKeyForJob(nodeName);
        NodePamEndUpdateOperation nodePamEndUpdateOperation = new NodePamEndUpdateOperation(requestId, nodeName, NetworkElementAccountUpdateStatus.CONFIGURED.name(), key);
        endUpdateOperationMap.setStatus(nodePamEndUpdateOperation);
    }

    private void setOperationFinishedOnNodeForEvent(final String nodeName) {
        final String key = getKeyForEvent(nodeName);
        NodePamEndUpdateOperation nodePamEndUpdateOperation = new NodePamEndUpdateOperation(requestId, nodeName, NetworkElementAccountUpdateStatus.CONFIGURED.name(), key);
        endUpdateOperationMap.setStatus(nodePamEndUpdateOperation);
    }

    private void setOperationFailedOnNodeForEvent(final String nodeName) {
        final String key = getKeyForEvent(nodeName);
        NodePamEndUpdateOperation nodePamEndUpdateOperation = new NodePamEndUpdateOperation(requestId, nodeName, NetworkElementAccountUpdateStatus.FAILED.name(), key);
        nodePamEndUpdateOperation.setErrorDetails("some error")
        endUpdateOperationMap.setStatus(nodePamEndUpdateOperation);
    }

    def setup() {
        membershipListenerInterfaceMock.isMaster() >> true
        criptoMock.encrypt(_) >> "passwordEncrypted"
        criptoMock.decrypt(_) >> "passwordDecrypted"
        runtimeDps.withTransactionBoundaries()

        //this is necessary to stop test when got assert error inside mediationEventSenderMock.send.
        mediationEventSenderMock.send(_) >> {setOperationFinishedOnNodeForJob("RadioNode1");}
        mediationEventSenderMock.send(_) >> {setOperationFinishedOnNodeForEvent("RadioNode1");}
    }

    /*
          NodePamEnableRemoteManagementEventRequest Section
    * */
    def 'receive NodePamEnableRemoteManagementEventRequest when functionality is disabled do nothing (log error)'() {
        given: 'functionality is disabled'
            nodePamConfigStatusMocked.isEnabled() >> false
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'the event is created'
            NodePamEnableRemoteManagementEventRequest nodePamRequest = new NodePamEnableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
        when: 'the event is consumed'
            objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NPAM_FUNCTIONALITY_DISABLED)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
    }

    def 'receive NodePamEnableRemoteManagementEventRequest when functionality check that throws exception do nothing  (log error)'() {
        given: 'functionality check throws exception'
            nodePamConfigStatusMocked.isEnabled() >> {throw new Exception('some error')}
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'the event is created'
            NodePamEnableRemoteManagementEventRequest nodePamRequest = new NodePamEnableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
        when: 'the event is consumed'
            objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
    }

    def 'receive NodePamEnableRemoteManagementEventRequest when DB unavailable (and retryNumber < 10) send back to queue'() {
        given: 'functionality is enabled'
           nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database not available'
            databaseAvailabilityCheckerMock.isAvailable() >> false
        and: 'the event is created'
            NodePamEnableRemoteManagementEventRequest nodePamRequest = new NodePamEnableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
        when: 'the event is consumed'
            objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'send back event to queue'
            1 * mockedNodePamRequestQueueChannel.createEvent({
            NodePamEnableRemoteManagementEventRequest request = (NodePamEnableRemoteManagementEventRequest)it
            assert request.nodeName == "RadioNode1"
            assert request.retryNumber == 1
            return true
            }, _) >> mockedEvent
            1 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
    }

    def 'receive NodePamEnableRemoteManagementEventRequest when DB unavailable (and retryNumber = 60) do not send back to queue'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database not available'
            databaseAvailabilityCheckerMock.isAvailable() >> false
        and: 'the event is created with retryNumber=60'
            NodePamEnableRemoteManagementEventRequest nodePamRequest = new NodePamEnableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
            nodePamRequest.setRetryNumber(60)
        when: 'the event is consumed'
            objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'no send back event to queue'
            0 * mockedNodePamRequestQueueChannel.createEvent(*_) >> mockedEvent
            0 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
    }

    def 'receive NodePamEnableRemoteManagementEventRequest with node without CmFunction do nothing (log error)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node not configured'
        and: 'the event is created'
            NodePamEnableRemoteManagementEventRequest nodePamRequest = new NodePamEnableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
        when: 'the event is consumed'
            objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NODE_NOT_EXISTENT, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
    }

    def 'receive NodePamEnableRemoteManagementEventRequest with node  with null syncStatus do nothing (log error)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1",null, true)
        and: 'the event is created'
            NodePamEnableRemoteManagementEventRequest nodePamRequest = new NodePamEnableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
        when: 'the event is consumed'
            objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NULL_SYNC_STATUS, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
    }

//    def 'receive NodePamEnableRemoteManagementEventRequest with node UNSYNCHRONIZED do nothing  (log error)'() {
//        given: 'functionality is enabled'
//            nodePamConfigStatusMocked.isEnabled() >> true
//        and: 'Database is available'
//            databaseAvailabilityCheckerMock.isAvailable() >> true
//        and: 'node configured'
//            addSubnetWork(SUBNETWORK_NAME)
//            addNodeTree(SUBNETWORK_NAME,"RadioNode1","UNSYNCHRONIZED", true)
//        and: 'the event is created'
//            NodePamEnableRemoteManagementEventRequest nodePamRequest = new NodePamEnableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
//        when: 'the event is consumed'
//            objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
//        then: 'internally raised exception'
//            1 * exceptionFactory.createValidationException(NodePamError.UNSYNCHRONIZED_SYNC_STATUS, _)
//        and: 'no nodePamMediationEvent sent'
//            0 * mediationEventSenderMock.send(_)
//    }

    @Unroll
    def 'receive NodePamEnableRemoteManagementEventRequest with node=#syncStatus (and retryNumber < 7) send back to queue'(syncStatus) {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", syncStatus, true)
        and: 'the event is created'
            NodePamEnableRemoteManagementEventRequest nodePamRequest = new NodePamEnableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
        when: 'the event is consumed'
            objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'send back event to queue'
            1 * mockedNodePamRequestQueueChannel.createEvent({
                NodePamEnableRemoteManagementEventRequest request = (NodePamEnableRemoteManagementEventRequest)it
                assert request.nodeName == "RadioNode1"
                assert request.retryNumber == 1
                return true
            }, _) >> mockedEvent
            1 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        where:
        syncStatus       || _
        "TOPOLOGY"       || _
        //we comment below to be faster
        //"ATTRIBUTE"      || _
        //"PENDING"        || _
        //"DELTA"          || _
        //"NOT_SUPPORTED"  || _
    }

    @Unroll
    def 'receive NodePamEnableRemoteManagementEventRequest with node=#syncStatus (retryNumber=300) do nothing  (log error)'(syncStatus) {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", syncStatus, true)
        and: 'the event is created with retryNumber=300'
            NodePamEnableRemoteManagementEventRequest nodePamRequest = new NodePamEnableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
            nodePamRequest.setRetryNumber(300)
        when: 'the event is consumed'
            objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NOT_SYNCHRONIZED_SYNC_STATUS, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        where:
        syncStatus       || _
        "TOPOLOGY"       || _
        "ATTRIBUTE"      || _
        "PENDING"        || _
        "DELTA"          || _
        "NOT_SUPPORTED"  || _
    }

    def 'receive NodePamEnableRemoteManagementEventRequest with node SYNCHRONIZED, remoteManagement=true, no MaintenanceUser instance and NetworkElementAccount=1 send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'CbrsDomain is disabled'
            nodePamConfigStatusMocked.isCbrsDomainEnabled() >> false
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1","SYNCHRONIZED", true)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
        and: 'the event is created'
            NodePamEnableRemoteManagementEventRequest nodePamRequest = new NodePamEnableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
        when: 'the event is consumed'
            objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
                def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
                mo.allAttributes.get("currentUsername") != null
                mo.allAttributes.get("currentPassword") != null
                mo.allAttributes.get("nextUsername") != null
                mo.allAttributes.get("nextPassword") != null
                mo.allAttributes.get("lastPasswordChange") != null
                mo.allAttributes.get("lastFailed") == null
                mo.allAttributes.get("errorDetails") == null
                mo.allAttributes.get("jobType") == null
                mo.allAttributes.get("mainJobId") == null
                mo.allAttributes.get("neJobId") == null
                mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'NetworkElementAccount MO with id=2 is NOT created/modified'
            dpsQueryUtil.getNetworkElementAccount("RadioNode1", 2) == null
        and: 'event is sent to mediation'
        1 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            assert nodePamMediationEvent.nodePamRequestId == requestId
            assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")
            assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
            assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
            assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
            assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()
            assert nodePamMediationEvent.nodePamSubjectName == null
            assert nodePamMediationEvent.nodePamUsername != null
            assert nodePamMediationEvent.nodePamPassword != null
            assert nodePamMediationEvent.nodePamRemoteManagement == null
            assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
            assert nodePamMediationEvent.neJobId == -1
            return true
        }) >> {setOperationFinishedOnNodeForEvent("RadioNode1")}
    }

    def 'receive NodePamEnableRemoteManagementEventRequest with node SYNCHRONIZED, remoteManagement=true and MaintenanceUser=1 send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'CbrsDomain is disabled'
            nodePamConfigStatusMocked.isCbrsDomainEnabled() >> false
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1","SYNCHRONIZED", true)
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 1)
        and: 'the event is created'
            NodePamEnableRemoteManagementEventRequest nodePamRequest = new NodePamEnableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
        when: 'the event is consumed'
            objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
        def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") == null
            mo.allAttributes.get("currentPassword") == null
            mo.allAttributes.get("nextUsername") != null
            mo.allAttributes.get("nextPassword") != null
            mo.allAttributes.get("lastPasswordChange") == null
            mo.allAttributes.get("lastFailed") == null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == null
            mo.allAttributes.get("mainJobId") == null
            mo.allAttributes.get("neJobId") == null
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'NetworkElementAccount MO with id=2 is NOT created/modified'
            dpsQueryUtil.getNetworkElementAccount("RadioNode1", 2) == null
        and: 'event is sent to mediation'
        1 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            assert nodePamMediationEvent.nodePamRequestId == requestId
            assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")
            assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
            assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
            assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
            assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.MODIFY.name()
            assert nodePamMediationEvent.nodePamSubjectName == ""
            assert nodePamMediationEvent.nodePamUsername != null
            assert nodePamMediationEvent.nodePamPassword != null
            assert nodePamMediationEvent.nodePamRemoteManagement == null
            assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
            assert nodePamMediationEvent.neJobId == -1
            return true
        }) >> {setOperationFinishedOnNodeForEvent("RadioNode1")}
    }

    def 'receive NodePamEnableRemoteManagementEventRequest with node SYNCHRONIZED, remoteManagement=true and MaintenanceUser=2 send two nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'CbrsDomain is disabled'
            nodePamConfigStatusMocked.isCbrsDomainEnabled() >> false
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1","SYNCHRONIZED", true)
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 2)
        and: 'the event is created'
            NodePamEnableRemoteManagementEventRequest nodePamRequest = new NodePamEnableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
        when: 'the event is consumed'
            objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") == null
            mo.allAttributes.get("currentPassword") == null
            mo.allAttributes.get("nextUsername") != null
            mo.allAttributes.get("nextPassword") != null
            mo.allAttributes.get("lastPasswordChange") == null
            mo.allAttributes.get("lastFailed") == null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == null
            mo.allAttributes.get("mainJobId") == null
            mo.allAttributes.get("neJobId") == null
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'NetworkElementAccount MO with id=2 is NOT created/modified'
            dpsQueryUtil.getNetworkElementAccount("RadioNode1", 2) == null
        and: 'events are sent to mediation'
        2 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            if (nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()) {
                assert nodePamMediationEvent.nodePamRequestId == requestId
                assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")
                assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()
                assert nodePamMediationEvent.nodePamSubjectName == null
                assert nodePamMediationEvent.nodePamUsername != null
                assert nodePamMediationEvent.nodePamPassword != null
                assert nodePamMediationEvent.nodePamRemoteManagement == null
                assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                assert nodePamMediationEvent.neJobId == -1
            } else if (nodePamMediationEvent.nodePamOperation == NodePamEventOperation.DELETE.name()) {
                assert nodePamMediationEvent.nodePamRequestId == requestId
                assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")
                assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.DELETE.name()
                assert nodePamMediationEvent.nodePamSubjectName == ""
                assert nodePamMediationEvent.nodePamUsername == null
                assert nodePamMediationEvent.nodePamPassword == null
                assert nodePamMediationEvent.nodePamRemoteManagement == null
                assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                assert nodePamMediationEvent.neJobId == -1
            }
            return true
        }) >> {setOperationFinishedOnNodeForEvent("RadioNode1")}

    }

    def 'receive NodePamEnableRemoteManagementEventRequest with node SYNCHRONIZED, remoteManagement=true, no MaintenanceUser instance and NetworkElementAccount=1 and CbrsDomain enabled send two nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'CbrsDomain is enabled'
            nodePamConfigStatusMocked.isCbrsDomainEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1","SYNCHRONIZED", true)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
            addNeAccount("RadioNode1", 2, NetworkElementAccountUpdateStatus.CONFIGURED.name())
        and: 'the event is created'
            NodePamEnableRemoteManagementEventRequest nodePamRequest = new NodePamEnableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
        when: 'the event is consumed'
            objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") != null
            mo.allAttributes.get("currentPassword") != null
            mo.allAttributes.get("nextUsername") != null
            mo.allAttributes.get("nextPassword") != null
            mo.allAttributes.get("lastPasswordChange") != null
            mo.allAttributes.get("lastFailed") == null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == null
            mo.allAttributes.get("mainJobId") == null
            mo.allAttributes.get("neJobId") == null
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'NetworkElementAccount MO with id=2 is created/modified'
            def mo2 = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 2)
            mo2.allAttributes.get("currentUsername") != null
            mo2.allAttributes.get("currentPassword") != null
            mo2.allAttributes.get("nextUsername") == null
            mo2.allAttributes.get("nextPassword") == null
            mo2.allAttributes.get("lastPasswordChange") != null
            mo2.allAttributes.get("lastFailed") == null
            mo2.allAttributes.get("errorDetails") == null
            mo2.allAttributes.get("jobType") == null
            mo2.allAttributes.get("mainJobId") == null
            mo2.allAttributes.get("neJobId") == null
            mo2.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'event is sent to mediation'
        2 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            if (nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")) {
                assert nodePamMediationEvent.nodePamRequestId == requestId
                assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")
                assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()
                assert nodePamMediationEvent.nodePamSubjectName == null
                assert nodePamMediationEvent.nodePamUsername != null
                assert nodePamMediationEvent.nodePamPassword != null
                assert nodePamMediationEvent.nodePamRemoteManagement == null
                assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                assert nodePamMediationEvent.neJobId == -1
            }
            if (nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")) {
                assert nodePamMediationEvent.nodePamRequestId == requestId
                assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")
                assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()
                assert nodePamMediationEvent.nodePamSubjectName == "CN=MaintenanceUser"
                assert nodePamMediationEvent.nodePamUsername == null
                assert nodePamMediationEvent.nodePamPassword == null
                assert nodePamMediationEvent.nodePamRemoteManagement == null
                assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                assert nodePamMediationEvent.neJobId == -1
            }
            return true
        }) >> {setOperationFinishedOnNodeForEvent("RadioNode1")}
    }

    def 'receive NodePamEnableRemoteManagementEventRequest with node SYNCHRONIZED, remoteManagement=true and MaintenanceUser=1 and CbrsDomain enabled send two nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'CbrsDomain is enabled'
            nodePamConfigStatusMocked.isCbrsDomainEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1","SYNCHRONIZED", true)
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 1)
        and: 'the event is created'
            NodePamEnableRemoteManagementEventRequest nodePamRequest = new NodePamEnableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
        when: 'the event is consumed'
            objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") == null
            mo.allAttributes.get("currentPassword") == null
            mo.allAttributes.get("nextUsername") != null
            mo.allAttributes.get("nextPassword") != null
            mo.allAttributes.get("lastPasswordChange") == null
            mo.allAttributes.get("lastFailed") == null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == null
            mo.allAttributes.get("mainJobId") == null
            mo.allAttributes.get("neJobId") == null
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'NetworkElementAccount MO with id=2 is created/modified'
            def mo2 = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 2)
            mo2.allAttributes.get("currentUsername") == null
            mo2.allAttributes.get("currentPassword") == null
            mo2.allAttributes.get("nextUsername") == null
            mo2.allAttributes.get("nextPassword") == null
            mo2.allAttributes.get("lastPasswordChange") == null
            mo2.allAttributes.get("lastFailed") == null
            mo2.allAttributes.get("errorDetails") == null
            mo2.allAttributes.get("jobType") == null
            mo2.allAttributes.get("mainJobId") == null
            mo2.allAttributes.get("neJobId") == null
            mo2.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'event is sent to mediation'
        2 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            if (nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")) {
                assert nodePamMediationEvent.nodePamRequestId == requestId
                assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")
                assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.MODIFY.name()
                assert nodePamMediationEvent.nodePamSubjectName == ""
                assert nodePamMediationEvent.nodePamUsername != null
                assert nodePamMediationEvent.nodePamPassword != null
                assert nodePamMediationEvent.nodePamRemoteManagement == null
                assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                assert nodePamMediationEvent.neJobId == -1
            }
            if (nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")) {
                assert nodePamMediationEvent.nodePamRequestId == requestId
                assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")
                assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()
                assert nodePamMediationEvent.nodePamSubjectName == "CN=MaintenanceUser"
                assert nodePamMediationEvent.nodePamUsername == null
                assert nodePamMediationEvent.nodePamPassword == null
                assert nodePamMediationEvent.nodePamRemoteManagement == null
                assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                assert nodePamMediationEvent.neJobId == -1
            }
            return true
        }) >> {setOperationFinishedOnNodeForEvent("RadioNode1")}
    }

    def 'receive NodePamEnableRemoteManagementEventRequest with node SYNCHRONIZED, remoteManagement=true and MaintenanceUser=3 and CbrsDomain enabled send three nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'CbrsDomain is enabled'
            nodePamConfigStatusMocked.isCbrsDomainEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1","SYNCHRONIZED", true)
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 3)
        and: 'the event is created'
            NodePamEnableRemoteManagementEventRequest nodePamRequest = new NodePamEnableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
        when: 'the event is consumed'
            objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") == null
            mo.allAttributes.get("currentPassword") == null
            mo.allAttributes.get("nextUsername") != null
            mo.allAttributes.get("nextPassword") != null
            mo.allAttributes.get("lastPasswordChange") == null
            mo.allAttributes.get("lastFailed") == null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == null
            mo.allAttributes.get("mainJobId") == null
            mo.allAttributes.get("neJobId") == null
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'NetworkElementAccount MO with id=2 is created/modified'
            def mo2 = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 2)
            mo2.allAttributes.get("currentUsername") == null
            mo2.allAttributes.get("currentPassword") == null
            mo2.allAttributes.get("nextUsername") == null
            mo2.allAttributes.get("nextPassword") == null
            mo2.allAttributes.get("lastPasswordChange") == null
            mo2.allAttributes.get("lastFailed") == null
            mo2.allAttributes.get("errorDetails") == null
            mo2.allAttributes.get("jobType") == null
            mo2.allAttributes.get("mainJobId") == null
            mo2.allAttributes.get("neJobId") == null
            mo2.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'events are sent to mediation'
        3 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            if (nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()) {
                if (nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")) {
                    assert nodePamMediationEvent.nodePamRequestId == requestId
                    assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")
                    assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                    assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                    assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                    assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()
                    assert nodePamMediationEvent.nodePamSubjectName == null
                    assert nodePamMediationEvent.nodePamUsername != null
                    assert nodePamMediationEvent.nodePamPassword != null
                    assert nodePamMediationEvent.nodePamRemoteManagement == null
                    assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                    assert nodePamMediationEvent.neJobId == -1
                }
                if (nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")) {
                    assert nodePamMediationEvent.nodePamRequestId == requestId
                    assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")
                    assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                    assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                    assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                    assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()
                    assert nodePamMediationEvent.nodePamSubjectName == "CN=MaintenanceUser"
                    assert nodePamMediationEvent.nodePamUsername == null
                    assert nodePamMediationEvent.nodePamPassword == null
                    assert nodePamMediationEvent.nodePamRemoteManagement == null
                    assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                    assert nodePamMediationEvent.neJobId == -1
                }
            } else if (nodePamMediationEvent.nodePamOperation == NodePamEventOperation.DELETE.name()) {
                assert nodePamMediationEvent.nodePamRequestId == requestId
                assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=3")
                assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.DELETE.name()
                assert nodePamMediationEvent.nodePamSubjectName == ""
                assert nodePamMediationEvent.nodePamUsername == null
                assert nodePamMediationEvent.nodePamPassword == null
                assert nodePamMediationEvent.nodePamRemoteManagement == null
                assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                assert nodePamMediationEvent.neJobId == -1
            }
            return true
        }) >> {setOperationFinishedOnNodeForEvent("RadioNode1")}

    }


    /*
       NodePamDisableRemoteManagementEventRequest Section
     * */
    def 'receive NodePamDisableRemoteManagementEventRequest when functionality is disabled do nothing (log error)'() {
        given: 'functionality is disabled'
            nodePamConfigStatusMocked.isEnabled() >> false
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'the event is created'
            NodePamDisableRemoteManagementEventRequest nodePamRequest = new NodePamDisableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
        when: 'the event is consumed'
            objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NPAM_FUNCTIONALITY_DISABLED)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
    }

    def 'receive NodePamDisableRemoteManagementEventRequest when functionality check that throws exception do nothing  (log error)'() {
        given: 'functionality check throws exception'
           nodePamConfigStatusMocked.isEnabled() >> {throw new Exception('some error')}
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'the event is created'
            NodePamDisableRemoteManagementEventRequest nodePamRequest = new NodePamDisableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
        when: 'the event is consumed'
            objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
    }

    def 'receive NodePamDisableRemoteManagementEventRequest when DB unavailable (and retryNumber < 10) send back to queue'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database not available'
            databaseAvailabilityCheckerMock.isAvailable() >> false
        and: 'the event is created'
            NodePamDisableRemoteManagementEventRequest nodePamRequest = new NodePamDisableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
        when: 'the event is consumed'
        objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'send back event to queue'
        1 * mockedNodePamRequestQueueChannel.createEvent({
            NodePamDisableRemoteManagementEventRequest request = (NodePamDisableRemoteManagementEventRequest)it
            assert request.nodeName == "RadioNode1"
            assert request.retryNumber == 1
            return true
        }, _) >> mockedEvent
        1 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
        and: 'no nodePamMediationEvent sent'
        0 * mediationEventSenderMock.send(_)
    }

    def 'receive NodePamDisableRemoteManagementEventRequest when DB unavailable (and retryNumber = 60) do not send back to queue'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database not available'
            databaseAvailabilityCheckerMock.isAvailable() >> false
        and: 'the event is created with retryNumber=60'
            NodePamDisableRemoteManagementEventRequest nodePamRequest = new NodePamDisableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
            nodePamRequest.setRetryNumber(60)
        when: 'the event is consumed'
        objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'no send back event to queue'
            0 * mockedNodePamRequestQueueChannel.createEvent(*_) >> mockedEvent
            0 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
    }

    def 'receive NodePamDisableRemoteManagementEventRequest  set status DETACHED in the NetworkElementAccont=1 MO' () {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1","SYNCHRONIZED", true)
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 1)
            def neAccountMo1 = addNeAccount("RadioNode1",1,NetworkElementAccountUpdateStatus.CONFIGURED.name())
            neAccountMo1.allAttributes.put("neJobId", 10)
            def neAccountMo2 = addNeAccount("RadioNode1",2,NetworkElementAccountUpdateStatus.CONFIGURED.name())
            neAccountMo2.allAttributes.put("neJobId", 20)
           def neAccountMo3 = addNeAccount("RadioNode1",3,NetworkElementAccountUpdateStatus.CONFIGURED.name())
            neAccountMo3.allAttributes.put("neJobId", 30)
        and: 'the event is created'
            NodePamDisableRemoteManagementEventRequest nodePamRequest = new NodePamDisableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
        when: 'the event is consumed'
            objUnderTest.receiveEventRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MOs are in DETACHED state'
            def mo1 = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo1.allAttributes.get("jobType") == null
            mo1.allAttributes.get("mainJobId") == null
            mo1.allAttributes.get("neJobId") == null
            mo1.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.DETACHED.name()

            def mo2 = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo2.allAttributes.get("jobType") == null
            mo2.allAttributes.get("mainJobId") == null
            mo2.allAttributes.get("neJobId") == null
            mo2.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.DETACHED.name()

            def mo3 = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo3.allAttributes.get("jobType") == null
            mo3.allAttributes.get("mainJobId") == null
            mo3.allAttributes.get("neJobId") == null
            mo3.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.DETACHED.name()
    }


    /*
      NodePamEnableRemoteManagementRequest Section
    * */
    def 'receive NodePamEnableRemoteManagementRequest when functionality is disabled do nothing (mark job result FAILED)'() {
        given: 'functionality is disabled'
            nodePamConfigStatusMocked.isEnabled() >> false
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamEnableRemoteManagementRequest nodePamRequest = new NodePamEnableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CREATE_NE_ACCOUNT, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NPAM_FUNCTIONALITY_DISABLED)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamEnableRemoteManagementRequest when functionality check that throws exception do nothing (mark job result FAILED)'() {
        given: 'functionality check throws exception'
            nodePamConfigStatusMocked.isEnabled() >> {throw new Exception('some error')}
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamEnableRemoteManagementRequest nodePamRequest = new NodePamEnableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CREATE_NE_ACCOUNT, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamEnableRemoteManagementRequest when DB unavailable (and retryNumber < 10) send back to queue'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database not available'
            databaseAvailabilityCheckerMock.isAvailable() >> false
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamEnableRemoteManagementRequest nodePamRequest = new NodePamEnableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CREATE_NE_ACCOUNT, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'send back event to queue'
          1 * mockedNodePamRequestQueueChannel.createEvent({
            NodePamEnableRemoteManagementRequest request = (NodePamEnableRemoteManagementRequest)it
            assert request.nodeName == "RadioNode1"
            assert request.retryNumber == 1
            return true
            }, _) >> mockedEvent
            1 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
    }

    def 'receive NodePamEnableRemoteManagementRequest when DB unavailable (and retryNumber = 60) do not send back to queue'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database not available'
            databaseAvailabilityCheckerMock.isAvailable() >> false
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created with retryNumber=60'
            NodePamEnableRemoteManagementRequest nodePamRequest = new NodePamEnableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CREATE_NE_ACCOUNT, 10)
            nodePamRequest.setRetryNumber(60)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'no send back event to queue'
            0 * mockedNodePamRequestQueueChannel.createEvent(*_) >> mockedEvent
            0 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
    }

    def 'receive NodePamEnableRemoteManagementRequest with node not authorized do nothing (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is NOT authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> false
        and: 'the event is created'
           NodePamEnableRemoteManagementRequest nodePamRequest = new NodePamEnableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CREATE_NE_ACCOUNT, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.TBAC_SINGLE_NODE_ACCESS_DENIED, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamEnableRemoteManagementRequest with node without CmFunction do nothing (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamEnableRemoteManagementRequest nodePamRequest = new NodePamEnableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CREATE_NE_ACCOUNT, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NODE_NOT_EXISTENT, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamEnableRemoteManagementRequest with node  with null syncStatus do nothing (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1",null, true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamEnableRemoteManagementRequest nodePamRequest = new NodePamEnableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CREATE_NE_ACCOUNT, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NULL_SYNC_STATUS, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    @Unroll
    def 'receive NodePamEnableRemoteManagementRequest with node=#syncStatus do nothing do nothing (mark job result FAILED)'(syncStatus) {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", syncStatus, true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamEnableRemoteManagementRequest nodePamRequest = new NodePamEnableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CREATE_NE_ACCOUNT, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NOT_SYNCHRONIZED_SYNC_STATUS, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
        where:
        syncStatus       || _
        "UNSYNCHRONIZED" || _
        "TOPOLOGY"       || _
        "ATTRIBUTE"      || _
        "PENDING"        || _
        "DELTA"          || _
        "NOT_SUPPORTED"  || _
    }

    def 'receive NodePamEnableRemoteManagementRequest with with node SYNCHRONIZED, remoteManagement=null do nothing  (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", null)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamEnableRemoteManagementRequest nodePamRequest = new NodePamEnableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CREATE_NE_ACCOUNT, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.REMOTE_MANAGEMENT_VALUE_NULL, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamEnableRemoteManagementRequest with with node SYNCHRONIZED, remoteManagement=true do nothing (mark job result SUCCESS)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamEnableRemoteManagementRequest nodePamRequest = new NodePamEnableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CREATE_NE_ACCOUNT, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            0 * exceptionFactory.createValidationException(*_)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SKIPPED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") == WARNING_MESSAGE_SKIPPED_REMOTE_MANAGEMENT_TRUE
    }

    def 'receive NodePamEnableRemoteManagementRequestExecutor with node SYNCHRONIZED, remoteManagement=false send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", false)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamEnableRemoteManagementRequest nodePamRequest = new NodePamEnableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CREATE_NE_ACCOUNT, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'event is sent to mediation'
        1 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            assert nodePamMediationEvent.nodePamRequestId == requestId
            assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUserSecurity=1")
            assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_SECURITY_MO
            assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
            assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
            assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.MODIFY_REMOTE_MANAGEMENT.name()
            assert nodePamMediationEvent.nodePamSubjectName == null
            assert nodePamMediationEvent.nodePamUsername == null
            assert nodePamMediationEvent.nodePamPassword == null
            assert nodePamMediationEvent.nodePamRemoteManagement == Boolean.TRUE
            assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
            assert nodePamMediationEvent.neJobId == neJobPO1.getPoId()
            return true
        }) >> {setOperationFinishedOnNodeForJob("RadioNode1")}
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.RUNNING.getJobStateName()
    }

    def 'receive NodePamEnableRemoteManagementRequestExecutor with node SYNCHRONIZED, remoteManagement=false and NetworkElementAccount=1 send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", false)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamEnableRemoteManagementRequest nodePamRequest = new NodePamEnableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CREATE_NE_ACCOUNT, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'event is sent to mediation'
        1 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            assert nodePamMediationEvent.nodePamRequestId == requestId
            assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUserSecurity=1")
            assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_SECURITY_MO
            assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
            assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
            assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.MODIFY_REMOTE_MANAGEMENT.name()
            assert nodePamMediationEvent.nodePamSubjectName == null
            assert nodePamMediationEvent.nodePamUsername == null
            assert nodePamMediationEvent.nodePamPassword == null
            assert nodePamMediationEvent.nodePamRemoteManagement == Boolean.TRUE
            assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
            assert nodePamMediationEvent.neJobId == neJobPO1.getPoId()
            return true
        }) >> {setOperationFinishedOnNodeForJob("RadioNode1")}
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.RUNNING.getJobStateName()
    }

    def 'receive NodePamEnableRemoteManagementRequestExecutor with node SYNCHRONIZED, remoteManagement=false and RestrictMaintenanceUser enabled send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'RestrictMaintenanceUser is enabled'
            nodePamConfigStatusMocked.isRestrictMaintenanceUserEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", false)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamEnableRemoteManagementRequest nodePamRequest = new NodePamEnableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CREATE_NE_ACCOUNT, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'event is sent to mediation'
        1 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            assert nodePamMediationEvent.nodePamRequestId == requestId
            assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUserSecurity=1")
            assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_SECURITY_MO
            assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
            assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
            assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.MODIFY_REMOTE_MANAGEMENT.name()
            assert nodePamMediationEvent.nodePamSubjectName == null
            assert nodePamMediationEvent.nodePamUsername == null
            assert nodePamMediationEvent.nodePamPassword == null
            assert nodePamMediationEvent.nodePamRemoteManagement == Boolean.TRUE
            assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == Boolean.TRUE
            assert nodePamMediationEvent.neJobId == neJobPO1.getPoId()
            return true
        }) >> {setOperationFinishedOnNodeForJob("RadioNode1")}
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.RUNNING.getJobStateName()
    }

    def 'receive NodePamEnableRemoteManagementRequestExecutor with node SYNCHRONIZED, remoteManagement=false and NetworkElementAccount=1 and RestrictMaintenanceUser enabled send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'RestrictMaintenanceUser is enabled'
            nodePamConfigStatusMocked.isRestrictMaintenanceUserEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", false)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamEnableRemoteManagementRequest nodePamRequest = new NodePamEnableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CREATE_NE_ACCOUNT, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'event is sent to mediation'
        1 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            assert nodePamMediationEvent.nodePamRequestId == requestId
            assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUserSecurity=1")
            assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_SECURITY_MO
            assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
            assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
            assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.MODIFY_REMOTE_MANAGEMENT.name()
            assert nodePamMediationEvent.nodePamSubjectName == null
            assert nodePamMediationEvent.nodePamUsername == null
            assert nodePamMediationEvent.nodePamPassword == null
            assert nodePamMediationEvent.nodePamRemoteManagement == Boolean.TRUE
            assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == Boolean.TRUE
            assert nodePamMediationEvent.neJobId == neJobPO1.getPoId()
            return true
        }) >> {setOperationFinishedOnNodeForJob("RadioNode1")}
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.RUNNING.getJobStateName()
    }

    /*
      NodePamDisableRemoteManagementRequestExecutor Section
    * */
    def 'receive NodePamDisableRemoteManagementRequest when functionality is disabled do nothing (mark job result FAILED)'() {
        given: 'functionality is disabled'
            nodePamConfigStatusMocked.isEnabled() >> false
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamDisableRemoteManagementRequest nodePamRequest = new NodePamDisableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.DETACH_NE_ACCOUNT, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NPAM_FUNCTIONALITY_DISABLED)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamDisableRemoteManagementRequest when functionality check that throws exception do nothing (mark job result FAILED)'() {
        given: 'functionality check throws exception'
            nodePamConfigStatusMocked.isEnabled() >> {throw new Exception('some error')}
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamDisableRemoteManagementRequest nodePamRequest = new NodePamDisableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.DETACH_NE_ACCOUNT, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    @Unroll
    def 'receive NodePamDisableRemoteManagementRequest when DB unavailable (and retryNumber < 10) send back to queue'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database not available'
            databaseAvailabilityCheckerMock.isAvailable() >> false
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamDisableRemoteManagementRequest nodePamRequest = new NodePamDisableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.DETACH_NE_ACCOUNT, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'send back event to queue'
            1 * mockedNodePamRequestQueueChannel.createEvent({
                NodePamDisableRemoteManagementRequest request = (NodePamDisableRemoteManagementRequest)it
                assert request.nodeName == "RadioNode1"
                assert request.retryNumber == 1
                return true
            }, _) >> mockedEvent
            1 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
    }

    @Unroll
    def 'receive NodePamDisableRemoteManagementRequest when DB unavailable (and retryNumber = 60) do not send back to queue'() {
        given: 'functionality is enabled'
           nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database not available'
            databaseAvailabilityCheckerMock.isAvailable() >> false
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created with retryNumber=60'
            NodePamDisableRemoteManagementRequest nodePamRequest = new NodePamDisableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.DETACH_NE_ACCOUNT, 10)
            nodePamRequest.setRetryNumber(60)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'no send back event to queue'
            0 * mockedNodePamRequestQueueChannel.createEvent(*_) >> mockedEvent
            0 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
    }

    def 'receive NodePamDisableRemoteManagementRequest with node not authorized do nothing (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamDisableRemoteManagementRequest nodePamRequest = new NodePamDisableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.DETACH_NE_ACCOUNT, 10)
        and: 'node is NOT authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> false
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.TBAC_SINGLE_NODE_ACCESS_DENIED, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamDisableRemoteManagementRequest with node without CmFunction do nothing (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamDisableRemoteManagementRequest nodePamRequest = new NodePamDisableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.DETACH_NE_ACCOUNT, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NODE_NOT_EXISTENT, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamDisableRemoteManagementRequest with node  with null syncStatus do nothing (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1",null, true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamDisableRemoteManagementRequest nodePamRequest = new NodePamDisableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.DETACH_NE_ACCOUNT, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NULL_SYNC_STATUS, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    @Unroll
    def 'receive NodePamDisableRemoteManagementRequest with node=#syncStatus do nothing do nothing (mark job result FAILED)'(syncStatus) {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", syncStatus, true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamDisableRemoteManagementRequest nodePamRequest = new NodePamDisableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.DETACH_NE_ACCOUNT, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NOT_SYNCHRONIZED_SYNC_STATUS, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
        where:
        syncStatus       || _
        "UNSYNCHRONIZED" || _
        "TOPOLOGY"       || _
        "ATTRIBUTE"      || _
        "PENDING"        || _
        "DELTA"          || _
        "NOT_SUPPORTED"  || _
    }

    def 'receive NodePamDisableRemoteManagementRequest with with node SYNCHRONIZED, remoteManagement=null do nothing  (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", null)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamDisableRemoteManagementRequest nodePamRequest = new NodePamDisableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.DETACH_NE_ACCOUNT, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.REMOTE_MANAGEMENT_VALUE_NULL, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamDisableRemoteManagementRequest with with node SYNCHRONIZED, remoteManagement=false do nothing (mark job result SUCCESS)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", false)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamDisableRemoteManagementRequest nodePamRequest = new NodePamDisableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.DETACH_NE_ACCOUNT, 10)

        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            0 * exceptionFactory.createValidationException(*_)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SKIPPED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") == WARNING_MESSAGE_SKIPPED_REMOTE_MANAGEMENT_FALSE
    }

    def 'receive NodePamDisableRemoteManagementRequest with node SYNCHRONIZED, remoteManagement=true send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamDisableRemoteManagementRequest nodePamRequest = new NodePamDisableRemoteManagementRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.DETACH_NE_ACCOUNT, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'event is sent to mediation'
        1 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            assert nodePamMediationEvent.nodePamRequestId == requestId
            assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUserSecurity=1")
            assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_SECURITY_MO
            assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
            assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
            assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.MODIFY_REMOTE_MANAGEMENT.name()
            assert nodePamMediationEvent.nodePamSubjectName == null
            assert nodePamMediationEvent.nodePamUsername == null
            assert nodePamMediationEvent.nodePamPassword == null
            assert nodePamMediationEvent.nodePamRemoteManagement == Boolean.FALSE
            assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
            assert nodePamMediationEvent.neJobId == neJobPO1.getPoId()
            return true
        }) >> {setOperationFinishedOnNodeForJob("RadioNode1")}
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.RUNNING.getJobStateName()
    }



    /*
          NodePamAutogeneratePasswordRequestExecutor Section
    * */
    def 'receive NodePamAutogeneratePasswordRequest when functionality is disabled do nothing (mark job result FAILED)'() {
        given: 'functionality is disabled'
            nodePamConfigStatusMocked.isEnabled() >> false
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamAutogeneratePasswordRequest nodePamRequest = new NodePamAutogeneratePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, 10)
        when: 'the event is consumed'
           objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NPAM_FUNCTIONALITY_DISABLED)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamAutogeneratePasswordRequest when functionality check that throws exception do nothing (mark job result FAILED)'() {
        given: 'functionality check throws exception'
            nodePamConfigStatusMocked.isEnabled() >> {throw new Exception('some error')}
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamAutogeneratePasswordRequest nodePamRequest = new NodePamAutogeneratePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamAutogeneratePasswordRequest when DB unavailable (and retryNumber < 10) send back to queue'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database not available'
            databaseAvailabilityCheckerMock.isAvailable() >> false
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamAutogeneratePasswordRequest nodePamRequest = new NodePamAutogeneratePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'send back event to queue'
            1 * mockedNodePamRequestQueueChannel.createEvent({
                NodePamAutogeneratePasswordRequest request = (NodePamAutogeneratePasswordRequest)it
                assert request.nodeName == "RadioNode1"
                assert request.retryNumber == 1
                return true
            }, _) >> mockedEvent
            1 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
    }

    def 'receive NodePamAutogeneratePasswordRequest when DB unavailable (and retryNumber = 60) send back to queue'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database not available'
            databaseAvailabilityCheckerMock.isAvailable() >> false
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created with retryNumber=60'
            NodePamAutogeneratePasswordRequest nodePamRequest = new NodePamAutogeneratePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, 10)
            nodePamRequest.setRetryNumber(60)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'no send back event to queue'
            0 * mockedNodePamRequestQueueChannel.createEvent(*_) >> mockedEvent
            0 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
    }

    def 'receive NodePamAutogeneratePasswordRequest with node not authorized do nothing (mark job result FAILED)'() {
        given: 'functionality is enabled'
        nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is NOT authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> false
        and: 'the event is created'
            NodePamAutogeneratePasswordRequest nodePamRequest = new NodePamAutogeneratePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.TBAC_SINGLE_NODE_ACCESS_DENIED, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamAutogeneratePasswordRequest with node without CmFunction do nothing (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamAutogeneratePasswordRequest nodePamRequest = new NodePamAutogeneratePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NODE_NOT_EXISTENT, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamAutogeneratePasswordRequest with node  with null syncStatus do nothing (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1",null, true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamAutogeneratePasswordRequest nodePamRequest = new NodePamAutogeneratePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NULL_SYNC_STATUS, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    @Unroll
    def 'receive NodePamAutogeneratePasswordRequest with node=#syncStatus do nothing do nothing (mark job result FAILED)'(syncStatus) {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", syncStatus, true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamAutogeneratePasswordRequest nodePamRequest = new NodePamAutogeneratePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NOT_SYNCHRONIZED_SYNC_STATUS, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
        where:
        syncStatus       || _
        "UNSYNCHRONIZED" || _
        "TOPOLOGY"       || _
        "ATTRIBUTE"      || _
        "PENDING"        || _
        "DELTA"          || _
        "NOT_SUPPORTED"  || _
    }

    def 'receive NodePamAutogeneratePasswordRequest with with node SYNCHRONIZED, remoteManagement=false do nothing  (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", false)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamAutogeneratePasswordRequest nodePamRequest = new NodePamAutogeneratePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.REMOTE_MANAGEMENT_VALUE_MISMATCH, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamAutogeneratePasswordRequest with with node SYNCHRONIZED, remoteManagement=true and no MaintenanceUser=1 do nothing  (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamAutogeneratePasswordRequest nodePamRequest = new NodePamAutogeneratePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NOT_FOUND_MAINTENANCE_USER, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
           def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
           po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
           po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
           po.allAttributes.get("step") == Step.NONE.name()
           po.allAttributes.get("endTime") != null
           po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamAutogeneratePasswordRequest with node SYNCHRONIZED, remoteManagement=true and MaintenanceUser=1 send nodePamMediationEvent'() {
        given: 'functionality is enabled'
        nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1","SYNCHRONIZED", true)
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 1)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamAutogeneratePasswordRequest nodePamRequest = new NodePamAutogeneratePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") == null
            mo.allAttributes.get("currentPassword") == null
            mo.allAttributes.get("nextUsername") != null
            mo.allAttributes.get("nextPassword") != null
            mo.allAttributes.get("lastPasswordChange") == null
            mo.allAttributes.get("lastFailed") == null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED.getJobTypeName()
            mo.allAttributes.get("mainJobId") == 10
            mo.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'event is sent to mediation'
        1 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            assert nodePamMediationEvent.nodePamRequestId == requestId
            assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")
            assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
            assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
            assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
            assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.MODIFY.name()
            assert nodePamMediationEvent.nodePamSubjectName == ""
            assert nodePamMediationEvent.nodePamUsername != null
            assert nodePamMediationEvent.nodePamPassword != null
            assert nodePamMediationEvent.nodePamRemoteManagement == null
            assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
            assert nodePamMediationEvent.neJobId == neJobPO1.getPoId()
            return true
        }) >> {setOperationFinishedOnNodeForJob("RadioNode1")}
    }

    def 'receive NodePamAutogeneratePasswordRequest with node SYNCHRONIZED, remoteManagement=true and MaintenanceUser=1 and NetworkElementAccount=1 send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1","SYNCHRONIZED", true)
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 1)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamAutogeneratePasswordRequest nodePamRequest = new NodePamAutogeneratePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") != null
            mo.allAttributes.get("currentPassword") != null
            mo.allAttributes.get("nextUsername") != null
            mo.allAttributes.get("nextPassword") != null
            mo.allAttributes.get("lastPasswordChange") != null
            mo.allAttributes.get("lastFailed") == null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED.getJobTypeName()
            mo.allAttributes.get("mainJobId") == 10
            mo.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'event is sent to mediation'
        1 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            assert nodePamMediationEvent.nodePamRequestId == requestId
            assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")
            assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
            assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
            assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
            assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.MODIFY.name()
            assert nodePamMediationEvent.nodePamSubjectName == ""
            assert nodePamMediationEvent.nodePamUsername != null
            assert nodePamMediationEvent.nodePamPassword != null
            assert nodePamMediationEvent.nodePamRemoteManagement == null
            assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
            assert nodePamMediationEvent.neJobId == neJobPO1.getPoId()
            return true
        }) >> {setOperationFinishedOnNodeForJob("RadioNode1")}
    }

    /*
      NodePamUpdatePasswordRequest Section
    * */
    def 'receive NodePamUpdatePasswordRequest when functionality is disabled do nothing (mark job result FAILED)'() {
        given: 'functionality is disabled'
            nodePamConfigStatusMocked.isEnabled() >> false
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamUpdatePasswordRequest nodePamRequest = new NodePamUpdatePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NPAM_FUNCTIONALITY_DISABLED)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamUpdatePasswordRequest when functionality check that throws exception do nothing (mark job result FAILED)'() {
        given: 'functionality check throws exception'
            nodePamConfigStatusMocked.isEnabled() >> {throw new Exception('some error')}
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamUpdatePasswordRequest nodePamRequest = new NodePamUpdatePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamUpdatePasswordRequest when DB unavailable (and retryNumber < 10) send back to queue'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database not available'
            databaseAvailabilityCheckerMock.isAvailable() >> false
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamUpdatePasswordRequest nodePamRequest = new NodePamUpdatePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, 10)
            nodePamRequest.setNextUser("RadioNode1")
            nodePamRequest.setNextPasswd("despicablePass")
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'send back event to queue'
        1 * mockedNodePamRequestQueueChannel.createEvent({
            NodePamUpdatePasswordRequest request = (NodePamUpdatePasswordRequest)it
            assert request.nodeName == "RadioNode1"
            assert request.retryNumber == 1
            return true
        }, _) >> mockedEvent
        1 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
        and: 'no nodePamMediationEvent sent'
        0 * mediationEventSenderMock.send(_)
    }

    def 'receive NodePamUpdatePasswordRequest when DB unavailable (and retryNumber = 60) send back to queue'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database not available'
            databaseAvailabilityCheckerMock.isAvailable() >> false
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created with retryNumber=60'
            NodePamUpdatePasswordRequest nodePamRequest = new NodePamUpdatePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, 10)
            nodePamRequest.setNextUser("RadioNode1")
            nodePamRequest.setNextPasswd("despicablePass")
            nodePamRequest.setRetryNumber(60)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'no send back event to queue'
            0 * mockedNodePamRequestQueueChannel.createEvent(*_) >> mockedEvent
            0 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
    }

    def 'receive NodePamUpdatePasswordRequest with node not authorized do nothing (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is NOT authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> false
        and: 'the event is created'
            NodePamUpdatePasswordRequest nodePamRequest = new NodePamUpdatePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, 10)
            nodePamRequest.setNextUser("RadioNode1")
            nodePamRequest.setNextPasswd("despicablePass")
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.TBAC_SINGLE_NODE_ACCESS_DENIED, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamUpdatePasswordRequest with node without CmFunction do nothing (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamUpdatePasswordRequest nodePamRequest = new NodePamUpdatePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, 10)
            nodePamRequest.setNextUser("RadioNode1")
            nodePamRequest.setNextPasswd("despicablePass")
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NODE_NOT_EXISTENT, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamUpdatePasswordRequest with node  with null syncStatus do nothing (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1",null, true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamUpdatePasswordRequest nodePamRequest = new NodePamUpdatePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, 10)
            nodePamRequest.setNextUser("RadioNode1")
            nodePamRequest.setNextPasswd("despicablePass")
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NULL_SYNC_STATUS, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    @Unroll
    def 'receive NodePamUpdatePasswordRequest with node=#syncStatus do nothing do nothing (mark job result FAILED)'(syncStatus) {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", syncStatus, true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamUpdatePasswordRequest nodePamRequest = new NodePamUpdatePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, 10)
            nodePamRequest.setNextUser("RadioNode1")
            nodePamRequest.setNextPasswd("despicablePass")
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NOT_SYNCHRONIZED_SYNC_STATUS, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
        where:
        syncStatus       || _
        "UNSYNCHRONIZED" || _
        "TOPOLOGY"       || _
        "ATTRIBUTE"      || _
        "PENDING"        || _
        "DELTA"          || _
        "NOT_SUPPORTED"  || _
    }


    def 'receive NodePamUpdatePasswordRequest with with node SYNCHRONIZED, remoteManagement=false do nothing  (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", false)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamUpdatePasswordRequest nodePamRequest = new NodePamUpdatePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, 10)
            nodePamRequest.setNextUser("RadioNode1")
            nodePamRequest.setNextPasswd("despicablePass")
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.REMOTE_MANAGEMENT_VALUE_MISMATCH, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamUpdatePasswordRequest with with node SYNCHRONIZED, remoteManagement=true and no MaintenanceUser=1 do nothing  (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamUpdatePasswordRequest nodePamRequest = new NodePamUpdatePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, 10)
            nodePamRequest.setNextUser("RadioNode1")
            nodePamRequest.setNextPasswd("despicablePass")
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NOT_FOUND_MAINTENANCE_USER, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    @Unroll
    def 'receive NodePamUpdatePasswordRequest with invalid credentials nextUserName=#nextUserName, nextPassword=#nextPassword do nothing (mark job result FAILED)'(nextUserName, nextPassword) {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 1)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is NOT authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamUpdatePasswordRequest nodePamRequest = new NodePamUpdatePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, 10)
            nodePamRequest.setNextUser(nextUserName)
            nodePamRequest.setNextPasswd(nextPassword)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NULL_PASSWORD_FOR_NODE, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
        where:
            nextUserName | nextPassword     || _
            null         | null             || _
            "RadioNode1" | null             || _
    }

    @Unroll
    def 'receive NodePamUpdatePasswordRequest with node SYNCHRONIZED, remoteManagement=true and MaintenanceUser=1 send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1","SYNCHRONIZED", true)
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 1)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamUpdatePasswordRequest nodePamRequest = new NodePamUpdatePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, 10)
            nodePamRequest.setNextUser(nextUserName)
            nodePamRequest.setNextPasswd(nextPassword)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") == null
            mo.allAttributes.get("currentPassword") == null
            mo.allAttributes.get("nextUsername") != null
            mo.allAttributes.get("nextPassword") != null
            mo.allAttributes.get("lastPasswordChange") == null
            mo.allAttributes.get("lastFailed") == null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == JobType.ROTATE_NE_ACCOUNT_CREDENTIALS.getJobTypeName()
            mo.allAttributes.get("mainJobId") == 10
            mo.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'event is sent to mediation'
        1 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            assert nodePamMediationEvent.nodePamRequestId == requestId
            assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")
            assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
            assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
            assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
            assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.MODIFY.name()
            assert nodePamMediationEvent.nodePamSubjectName == ""
            assert nodePamMediationEvent.nodePamUsername != null
            assert nodePamMediationEvent.nodePamPassword != null
            assert nodePamMediationEvent.nodePamRemoteManagement == null
            assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
            assert nodePamMediationEvent.neJobId == neJobPO1.getPoId()
            return true
        }) >> {setOperationFinishedOnNodeForJob("RadioNode1")}
        where:
            nextUserName | nextPassword     || _
            null         | "despicablePass" || _
            "test1"      | "despicablePass" || _
    }

    @Unroll
    def 'receive NodePamUpdatePasswordRequest with node SYNCHRONIZED, remoteManagement=true and MaintenanceUser=1 and NetworkElementAccount=1 send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1","SYNCHRONIZED", true)
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 1)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamUpdatePasswordRequest nodePamRequest = new NodePamUpdatePasswordRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId() , "1" ,JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, 10)
            nodePamRequest.setNextUser(nextUserName)
            nodePamRequest.setNextPasswd(nextPassword)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") != null
            mo.allAttributes.get("currentPassword") != null
            mo.allAttributes.get("nextUsername") != null
            mo.allAttributes.get("nextPassword") != null
            mo.allAttributes.get("lastPasswordChange") != null
            mo.allAttributes.get("lastFailed") == null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == JobType.ROTATE_NE_ACCOUNT_CREDENTIALS.getJobTypeName()
            mo.allAttributes.get("mainJobId") == 10
            mo.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'event is sent to mediation'
        1 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            assert nodePamMediationEvent.nodePamRequestId == requestId
            assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")
            assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
            assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
            assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
            assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.MODIFY.name()
            assert nodePamMediationEvent.nodePamSubjectName == ""
            assert (nodePamMediationEvent.nodePamUsername == 'userNameEncrypted' || nodePamMediationEvent.nodePamUsername == 'test1')
            assert nodePamMediationEvent.nodePamPassword != null
            assert nodePamMediationEvent.nodePamRemoteManagement == null
            assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
            assert nodePamMediationEvent.neJobId == neJobPO1.getPoId()
            return true
        }) >> {setOperationFinishedOnNodeForJob("RadioNode1")}
        where:
            nextUserName | nextPassword     || _
            null         | "despicablePass" || _
            "test1"      | "despicablePass" || _
    }


    /*
      NodePamRecoveryConfigurationRequest Section
    * */
    def 'receive NodePamRecoveryConfigurationRequest when functionality is disabled do nothing (mark job result FAILED)'() {
        given: 'functionality is disabled'
            nodePamConfigStatusMocked.isEnabled() >> false
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NPAM_FUNCTIONALITY_DISABLED)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
           po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamRecoveryConfigurationRequest when functionality check that throws exception do nothing (mark job result FAILED)'() {
        given: 'functionality check throws exception'
            nodePamConfigStatusMocked.isEnabled() >> {throw new Exception('some error')}
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        when: 'the event is consumed'
           objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamRecoveryConfigurationRequest when DB unavailable (and retryNumber < 10) send back to queue'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database not available'
            databaseAvailabilityCheckerMock.isAvailable() >> false
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'send back event to queue'
        1 * mockedNodePamRequestQueueChannel.createEvent({
            NodePamRecoveryConfigurationRequest request = (NodePamRecoveryConfigurationRequest)it
            assert request.nodeName == "RadioNode1"
            assert request.retryNumber == 1
            return true
        }, _) >> mockedEvent
        1 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
        and: 'no nodePamMediationEvent sent'
        0 * mediationEventSenderMock.send(_)
    }

    def 'receive NodePamRecoveryConfigurationRequest when DB unavailable (and retryNumber = 60) do not send back to queue'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database not available'
            databaseAvailabilityCheckerMock.isAvailable() >> false
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created with retryNumber=60'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
            nodePamRequest.setRetryNumber(60)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'no send back event to queue'
            0 * mockedNodePamRequestQueueChannel.createEvent(*_) >> mockedEvent
            0 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
    }

    def 'receive NodePamRecoveryConfigurationRequest with node not authorized do nothing (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is NOT authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> false
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.TBAC_SINGLE_NODE_ACCESS_DENIED, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamRecoveryConfigurationRequest with node without CmFunction do nothing (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NODE_NOT_EXISTENT, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamRecoveryConfigurationRequest with node  with null syncStatus do nothing (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1",null, true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NULL_SYNC_STATUS, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    @Unroll
    def 'receive NodePamRecoveryConfigurationRequest with node=#syncStatus do nothing do nothing (mark job result FAILED)'(syncStatus) {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", syncStatus, true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.NOT_SYNCHRONIZED_SYNC_STATUS, _)
        then: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
        where:
        syncStatus       || _
        "UNSYNCHRONIZED" || _
        "TOPOLOGY"       || _
        "ATTRIBUTE"      || _
        "PENDING"        || _
        "DELTA"          || _
        "NOT_SUPPORTED"  || _
    }

    def 'receive NodePamRecoveryConfigurationRequest with with node SYNCHRONIZED, remoteManagement=null do nothing  (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", null)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.REMOTE_MANAGEMENT_VALUE_NULL, _)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamRecoveryConfigurationRequest with with node SYNCHRONIZED, remoteManagement=true and failure returned by mediation (mark job result FAILED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            1 * exceptionFactory.createValidationException(NodePamError.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION_MEDIATION_ERROR, _)
        and: 'nodePamMediationEvent sent but return failure'
            1 * mediationEventSenderMock.send(_) >> {setOperationFailedOnNodeForEvent("RadioNode1")}
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            ((String)po.allAttributes.get("errorDetails")).contains("some error")
    }

    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=true, NetworkElementAccount=1 (CONFIGURED) do nothing (mark job result SKIPPED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            0 * exceptionFactory.createValidationException(*_)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SKIPPED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=true, NetworkElementAccount=1 (ONGOING) do nothing (mark job result SKIPPED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.ONGOING.name())
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            0 * exceptionFactory.createValidationException(*_)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SKIPPED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }


    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=true, NetworkElementAccount=1 (CONFIGURED) and MaintenanceUser=2 and MaintenanceUser=4 send nodePamMediationEvent'() {
        given: 'functionality is enabled'
                nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 2)
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 4)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is NOT created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.CONFIGURED.name()
        and: 'event is sent to mediation'
        2 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            if (nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")) {
                assert nodePamMediationEvent.nodePamRequestId == requestId
                assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")
                assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.DELETE.name()
                assert nodePamMediationEvent.nodePamSubjectName == ""
                assert nodePamMediationEvent.nodePamUsername == null
                assert nodePamMediationEvent.nodePamPassword == null
                assert nodePamMediationEvent.nodePamRemoteManagement == null
                assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                assert nodePamMediationEvent.neJobId == -1
            }
            if (nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=4")) {
                assert nodePamMediationEvent.nodePamRequestId == requestId
                assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=4")
                assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.DELETE.name()
                assert nodePamMediationEvent.nodePamSubjectName == ""
                assert nodePamMediationEvent.nodePamUsername == null
                assert nodePamMediationEvent.nodePamPassword == null
                assert nodePamMediationEvent.nodePamRemoteManagement == null
                assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                assert nodePamMediationEvent.neJobId == -1
            }
            return true
        }) >>  {setOperationFinishedOnNodeForEvent("RadioNode1")}
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SUCCESS.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") == null
    }

    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=true, cbrs disabled, NetworkElementAccount=1 (CONFIGURED), NetworkElementAccount=2 (CONFIGURED)  and MaintenanceUser=2 and MaintenanceUser=4 send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'cbrs is disabled'
            nodePamConfigStatusMocked.isCbrsDomainEnabled() >> false
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
            addNeAccount("RadioNode1", 2, NetworkElementAccountUpdateStatus.CONFIGURED.name())
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 2)
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 4)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is NOT created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.CONFIGURED.name()
        and: 'NetworkElementAccount MO with id=2 is deleted'
            def mo2 = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 2)
            mo2 == null
        and: 'event is sent to mediation'
        2 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            if (nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")) {
                assert nodePamMediationEvent.nodePamRequestId == requestId
                assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")
                assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.DELETE.name()
                assert nodePamMediationEvent.nodePamSubjectName == ""
                assert nodePamMediationEvent.nodePamUsername == null
                assert nodePamMediationEvent.nodePamPassword == null
                assert nodePamMediationEvent.nodePamRemoteManagement == null
                assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                assert nodePamMediationEvent.neJobId == -1
            }
            if (nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=4")) {
                assert nodePamMediationEvent.nodePamRequestId == requestId
                assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=4")
                assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.DELETE.name()
                assert nodePamMediationEvent.nodePamSubjectName == ""
                assert nodePamMediationEvent.nodePamUsername == null
                assert nodePamMediationEvent.nodePamPassword == null
                assert nodePamMediationEvent.nodePamRemoteManagement == null
                assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                assert nodePamMediationEvent.neJobId == -1
            }
            return true
        }) >>  {setOperationFinishedOnNodeForEvent("RadioNode1")}
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SUCCESS.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") == null
    }

    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=true, no NetworkElementAccount(s) send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") == null
            mo.allAttributes.get("currentPassword") == null
            mo.allAttributes.get("nextUsername") != null
            mo.allAttributes.get("nextPassword") != null
            mo.allAttributes.get("lastPasswordChange") == null
            mo.allAttributes.get("lastFailed") == null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION.name()
            mo.allAttributes.get("mainJobId") == 10
            mo.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'event is sent to mediation'
        1 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            assert nodePamMediationEvent.nodePamRequestId == requestId
            assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")
            assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
            assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
            assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
            assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()
            assert nodePamMediationEvent.nodePamSubjectName == null
            assert nodePamMediationEvent.nodePamUsername != null
            assert nodePamMediationEvent.nodePamPassword != null
            assert nodePamMediationEvent.nodePamRemoteManagement == null
            assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
            assert nodePamMediationEvent.neJobId == -1
            return true
        }) >>  {setOperationFinishedOnNodeForEvent("RadioNode1")}
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SUCCESS.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") == null
    }

    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=true, NetworkElementAccount=1 (DETACHED) send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.DETACHED.name())
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") != null
            mo.allAttributes.get("currentPassword") != null
            mo.allAttributes.get("nextUsername") != null
            mo.allAttributes.get("nextPassword") != null
            mo.allAttributes.get("lastPasswordChange") == null
            mo.allAttributes.get("lastFailed") == null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION.name()
            mo.allAttributes.get("mainJobId") == 10
            mo.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'event is sent to mediation'
        1 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            assert nodePamMediationEvent.nodePamRequestId == requestId
            assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")
            assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
            assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
            assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
            assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()
            assert nodePamMediationEvent.nodePamSubjectName == null
            assert nodePamMediationEvent.nodePamUsername != null
            assert nodePamMediationEvent.nodePamPassword != null
            assert nodePamMediationEvent.nodePamRemoteManagement == null
            assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
            assert nodePamMediationEvent.neJobId == -1
            return true
        }) >>  {setOperationFinishedOnNodeForEvent("RadioNode1")}
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SUCCESS.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") == null
    }

    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=true, NetworkElementAccount=1 (FAILED) send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.FAILED.name())
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") != null
            mo.allAttributes.get("currentPassword") != null
            mo.allAttributes.get("nextUsername") == "nextUserNameEncryptedFailed"
            mo.allAttributes.get("nextPassword") == "nextPasswordEncryptedFailed"
            mo.allAttributes.get("lastPasswordChange") == null
            mo.allAttributes.get("lastFailed") != null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION.name()
            mo.allAttributes.get("mainJobId") == 10
            mo.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'event is sent to mediation'
        1 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            assert nodePamMediationEvent.nodePamRequestId == requestId
            assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")
            assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
            assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
            assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
            assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()
            assert nodePamMediationEvent.nodePamSubjectName == null
            assert nodePamMediationEvent.nodePamUsername != null
            assert nodePamMediationEvent.nodePamPassword != null
            assert nodePamMediationEvent.nodePamRemoteManagement == null
            assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
            assert nodePamMediationEvent.neJobId == -1
            return true
        }) >>  {setOperationFinishedOnNodeForEvent("RadioNode1")}
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SUCCESS.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") == null
    }

    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=true, NetworkElementAccount=1 (DETACHED) and MaintenanceUser=2 and MaintenanceUser=4 send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.DETACHED.name())
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 2)
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 4)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") != null
            mo.allAttributes.get("currentPassword") != null
            mo.allAttributes.get("nextUsername") != null
            mo.allAttributes.get("nextPassword") != null
            mo.allAttributes.get("lastPasswordChange") == null
            mo.allAttributes.get("lastFailed") == null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION.name()
            mo.allAttributes.get("mainJobId") == 10
            mo.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'event is sent to mediation'
        3 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            if (nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()) {
                assert nodePamMediationEvent.nodePamRequestId == requestId
                assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")
                assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()
                assert nodePamMediationEvent.nodePamSubjectName == null
                assert nodePamMediationEvent.nodePamUsername != null
                assert nodePamMediationEvent.nodePamPassword != null
                assert nodePamMediationEvent.nodePamRemoteManagement == null
                assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                assert nodePamMediationEvent.neJobId == -1
            } else if (nodePamMediationEvent.nodePamOperation == NodePamEventOperation.DELETE.name()) {
                if (nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")) {
                    assert nodePamMediationEvent.nodePamRequestId == requestId
                    assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")
                    assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                    assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                    assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                    assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.DELETE.name()
                    assert nodePamMediationEvent.nodePamSubjectName == ""
                    assert nodePamMediationEvent.nodePamUsername == null
                    assert nodePamMediationEvent.nodePamPassword == null
                    assert nodePamMediationEvent.nodePamRemoteManagement == null
                    assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                    assert nodePamMediationEvent.neJobId == -1
                }
                if (nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=4")) {
                    assert nodePamMediationEvent.nodePamRequestId == requestId
                    assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=4")
                    assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                    assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                    assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                    assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.DELETE.name()
                    assert nodePamMediationEvent.nodePamSubjectName == ""
                    assert nodePamMediationEvent.nodePamUsername == null
                    assert nodePamMediationEvent.nodePamPassword == null
                    assert nodePamMediationEvent.nodePamRemoteManagement == null
                    assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                    assert nodePamMediationEvent.neJobId == -1
                }
            }
            return true
        }) >>  {setOperationFinishedOnNodeForEvent("RadioNode1")}
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SUCCESS.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") == null
    }

    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=true, NetworkElementAccount=1 (CONFIGURED) and NetworkElementAccount=2 (CONFIGURED) and MaintenanceUser=2 and CbrsDomain enabled do nothing (mark job result SKIPPED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'CbrsDomain is enabled'
            nodePamConfigStatusMocked.isCbrsDomainEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
            addNeAccount("RadioNode1", 2, NetworkElementAccountUpdateStatus.CONFIGURED.name())
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 2)
        and: 'NpamNeJob configured'
           neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            0 * exceptionFactory.createValidationException(*_)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SKIPPED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=true, NetworkElementAccount=1 (CONFIGURED) and CbrsDomain enabled send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'CbrsDomain is enabled'
            nodePamConfigStatusMocked.isCbrsDomainEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is NOT created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.CONFIGURED.name()
        and: 'NetworkElementAccount MO with id=2 is created/modified'
            def mo2 = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 2)
            mo2.allAttributes.get("currentUsername") == null
            mo2.allAttributes.get("currentPassword") == null
            mo2.allAttributes.get("nextUsername") == null
            mo2.allAttributes.get("nextPassword") == null
            mo2.allAttributes.get("lastPasswordChange") == null
            mo2.allAttributes.get("lastFailed") == null
            mo2.allAttributes.get("errorDetails") == null
            mo2.allAttributes.get("jobType") == JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION.name()
            mo2.allAttributes.get("mainJobId") == 10
            mo2.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo2.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'event is sent to mediation'
        1 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            assert nodePamMediationEvent.nodePamRequestId == requestId
            assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")
            assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
            assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
            assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
            assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()
            assert nodePamMediationEvent.nodePamSubjectName == "CN=MaintenanceUser"
            assert nodePamMediationEvent.nodePamUsername == null
            assert nodePamMediationEvent.nodePamPassword == null
            assert nodePamMediationEvent.nodePamRemoteManagement == null
            assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
            assert nodePamMediationEvent.neJobId == -1
            return true
        }) >>  {setOperationFinishedOnNodeForEvent("RadioNode1")}
        and: 'NPamNEJob PO modified'
        def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
        po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
        po.allAttributes.get("result") == JobResult.SUCCESS.getJobResult()
        po.allAttributes.get("step") == Step.NONE.name()
        po.allAttributes.get("endTime") != null
        po.allAttributes.get("errorDetails") == null
    }

    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=true, NetworkElementAccount=1 (CONFIGURED) and  NetworkElementAccount=2 (DETACHED) and CbrsDomain enabled send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'CbrsDomain is enabled'
            nodePamConfigStatusMocked.isCbrsDomainEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
            addNeAccount("RadioNode1", 2, NetworkElementAccountUpdateStatus.DETACHED.name())
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is NOT created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.CONFIGURED.name()
        and: 'NetworkElementAccount MO with id=2 is created/modified'
            def mo2 = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 2)
            mo2.allAttributes.get("currentUsername") != null
            mo2.allAttributes.get("currentPassword") != null
            mo2.allAttributes.get("nextUsername") == null
            mo2.allAttributes.get("nextPassword") == null
            mo2.allAttributes.get("lastPasswordChange") == null
            mo2.allAttributes.get("lastFailed") == null
            mo2.allAttributes.get("errorDetails") == null
            mo2.allAttributes.get("jobType") == JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION.name()
            mo2.allAttributes.get("mainJobId") == 10
            mo2.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo2.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'event is sent to mediation'
        1 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            assert nodePamMediationEvent.nodePamRequestId == requestId
            assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")
            assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
            assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
            assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
            assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()
            assert nodePamMediationEvent.nodePamSubjectName == "CN=MaintenanceUser"
            assert nodePamMediationEvent.nodePamUsername == null
            assert nodePamMediationEvent.nodePamPassword == null
            assert nodePamMediationEvent.nodePamRemoteManagement == null
            assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
            assert nodePamMediationEvent.neJobId == -1
            return true
        }) >>  {setOperationFinishedOnNodeForEvent("RadioNode1")}
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SUCCESS.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") == null
    }

    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=true, no NetworkElementAccount(s) and CbrsDomain enabled send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'CbrsDomain is enabled'
            nodePamConfigStatusMocked.isCbrsDomainEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") == null
            mo.allAttributes.get("currentPassword") == null
            mo.allAttributes.get("nextUsername") != null
            mo.allAttributes.get("nextPassword") != null
            mo.allAttributes.get("lastPasswordChange") == null
            mo.allAttributes.get("lastFailed") == null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION.name()
            mo.allAttributes.get("mainJobId") == 10
            mo.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'NetworkElementAccount MO with id=2 is created/modified'
            def mo2 = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 2)
            mo2.allAttributes.get("currentUsername") == null
            mo2.allAttributes.get("currentPassword") == null
            mo2.allAttributes.get("nextUsername") == null
            mo2.allAttributes.get("nextPassword") == null
            mo2.allAttributes.get("lastPasswordChange") == null
            mo2.allAttributes.get("lastFailed") == null
            mo2.allAttributes.get("errorDetails") == null
            mo2.allAttributes.get("jobType") == JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION.name()
            mo2.allAttributes.get("mainJobId") == 10
            mo2.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo2.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'event is sent to mediation'
        2 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            if (nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")) {
                assert nodePamMediationEvent.nodePamRequestId == requestId
                assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")
                assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()
                assert nodePamMediationEvent.nodePamSubjectName == null
                assert nodePamMediationEvent.nodePamUsername != null
                assert nodePamMediationEvent.nodePamPassword != null
                assert nodePamMediationEvent.nodePamRemoteManagement == null
                assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                assert nodePamMediationEvent.neJobId == -1
            }
            if (nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")) {
                assert nodePamMediationEvent.nodePamRequestId == requestId
                assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")
                assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()
                assert nodePamMediationEvent.nodePamSubjectName == "CN=MaintenanceUser"
                assert nodePamMediationEvent.nodePamUsername == null
                assert nodePamMediationEvent.nodePamPassword == null
                assert nodePamMediationEvent.nodePamRemoteManagement == null
                assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                assert nodePamMediationEvent.neJobId == -1
            }
            return true
        }) >>  {setOperationFinishedOnNodeForEvent("RadioNode1")}
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SUCCESS.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") == null
    }

    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=true, no NetworkElementAccount(s) and MaintenanceUser=2 and MaintenanceUser=4 and CbrsDomain enabled send nodePamMediationEvent'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'CbrsDomain is enabled'
            nodePamConfigStatusMocked.isCbrsDomainEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", true)
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 2)
            addMaintenanceUser(SUBNETWORK_NAME, "RadioNode1", 4)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") == null
            mo.allAttributes.get("currentPassword") == null
            mo.allAttributes.get("nextUsername") != null
            mo.allAttributes.get("nextPassword") != null
            mo.allAttributes.get("lastPasswordChange") == null
            mo.allAttributes.get("lastFailed") == null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION.name()
            mo.allAttributes.get("mainJobId") == 10
            mo.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'NetworkElementAccount MO with id=2 is created/modified'
            def mo2 = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 2)
            mo2.allAttributes.get("currentUsername") == null
            mo2.allAttributes.get("currentPassword") == null
            mo2.allAttributes.get("nextUsername") == null
            mo2.allAttributes.get("nextPassword") == null
            mo2.allAttributes.get("lastPasswordChange") == null
            mo2.allAttributes.get("lastFailed") == null
            mo2.allAttributes.get("errorDetails") == null
            mo2.allAttributes.get("jobType") == JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION.name()
            mo2.allAttributes.get("mainJobId") == 10
            mo2.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo2.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.ONGOING.name()
        and: 'event is sent to mediation'
        4 * mediationEventSenderMock.send({ it ->
            NodePamMediationEvent nodePamMediationEvent = (NodePamMediationEvent)it
            if (nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()) {
                if (nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")) {
                    assert nodePamMediationEvent.nodePamRequestId == requestId
                    assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=1")
                    assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                    assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                    assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                    assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()
                    assert nodePamMediationEvent.nodePamSubjectName == null
                    assert nodePamMediationEvent.nodePamUsername != null
                    assert nodePamMediationEvent.nodePamPassword != null
                    assert nodePamMediationEvent.nodePamRemoteManagement == null
                    assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                    assert nodePamMediationEvent.neJobId == -1
                }
                if (nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")) {
                    assert nodePamMediationEvent.nodePamRequestId == requestId
                    assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")
                    assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                    assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                    assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                    assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.CREATE.name()
                    assert nodePamMediationEvent.nodePamSubjectName == "CN=MaintenanceUser"
                    assert nodePamMediationEvent.nodePamUsername == null
                    assert nodePamMediationEvent.nodePamPassword == null
                    assert nodePamMediationEvent.nodePamRemoteManagement == null
                    assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                    assert nodePamMediationEvent.neJobId == -1
                }
            } else if (nodePamMediationEvent.nodePamOperation == NodePamEventOperation.DELETE.name()) {
                if (nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")) {
                    assert nodePamMediationEvent.nodePamRequestId == requestId
                    assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=2")
                    assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                    assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                    assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                    assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.DELETE.name()
                    assert nodePamMediationEvent.nodePamSubjectName == ""
                    assert nodePamMediationEvent.nodePamUsername == null
                    assert nodePamMediationEvent.nodePamPassword == null
                    assert nodePamMediationEvent.nodePamRemoteManagement == null
                    assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                    assert nodePamMediationEvent.neJobId == -1
                }
                if (nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=4")) {
                    assert nodePamMediationEvent.nodePamRequestId == requestId
                    assert nodePamMediationEvent.nodePamFdn.endsWith("MaintenanceUser=4")
                    assert nodePamMediationEvent.nodePamMoType == MAINTENANCE_USER_MO
                    assert nodePamMediationEvent.nodePamNameSpace == MAINTENANCE_USER_SECURITY_MO_NAMESPACE
                    assert nodePamMediationEvent.nodePamNameSpaceVersion == '6.2.2'
                    assert nodePamMediationEvent.nodePamOperation == NodePamEventOperation.DELETE.name()
                    assert nodePamMediationEvent.nodePamSubjectName == ""
                    assert nodePamMediationEvent.nodePamUsername == null
                    assert nodePamMediationEvent.nodePamPassword == null
                    assert nodePamMediationEvent.nodePamRemoteManagement == null
                    assert nodePamMediationEvent.nodePamRestrictMaintenanceUser == null
                    assert nodePamMediationEvent.neJobId == -1
                }
            }
            return true
        }) >>  {setOperationFinishedOnNodeForEvent("RadioNode1")}
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SUCCESS.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") == null
    }

    //EMARDEP DELETE
    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=false, no NetworkElementAccount(s) do nothing (mark job result SKIPPED)'() {
        given: 'functionality is enabled'
           nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", false)
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            0 * exceptionFactory.createValidationException(*_)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SKIPPED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=false, cbrs enabled, NetworkElementAccount=1 (DETACHED) and NetworkElementAccount=2 (DETACHED) do nothing (mark job result SKIPPED)'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'functionality is enabled'
            nodePamConfigStatusMocked.isCbrsDomainEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", false)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.DETACHED.name())
            addNeAccount("RadioNode1", 2, NetworkElementAccountUpdateStatus.DETACHED.name())
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'internally raised exception'
            0 * exceptionFactory.createValidationException(*_)
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SKIPPED.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") != null
    }

    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=false, NetworkElementAccount=1 (CONFIGURED) set status DETACHED in the NetworkElementAccont=1 MO'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", false)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") != null
            mo.allAttributes.get("currentPassword") != null
            mo.allAttributes.get("nextUsername") == null
            mo.allAttributes.get("nextPassword") == null
            mo.allAttributes.get("lastPasswordChange") != null
            mo.allAttributes.get("lastFailed") == null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION.name()
            mo.allAttributes.get("mainJobId") == 10
            mo.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.DETACHED.name()
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SUCCESS.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") == null
    }

    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=false, NetworkElementAccount=1 (CONFIGURED) and  NetworkElementAccount=2 (DETACHED) set status DETACHED in the NetworkElementAccont=1 MO'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", false)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
            addNeAccount("RadioNode1", 2, NetworkElementAccountUpdateStatus.DETACHED.name())
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") != null
            mo.allAttributes.get("currentPassword") != null
            mo.allAttributes.get("nextUsername") == null
            mo.allAttributes.get("nextPassword") == null
            mo.allAttributes.get("lastPasswordChange") != null
            mo.allAttributes.get("lastFailed") == null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION.name()
            mo.allAttributes.get("mainJobId") == 10
            mo.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.DETACHED.name()
        and: 'NetworkElementAccount MO with id=2 is NOT created/modified'
        def mo2 = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 2)
            mo2 == null
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SUCCESS.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") == null
    }

    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=false, cbrs disabled, NetworkElementAccount=1 (CONFIGURED) and  NetworkElementAccount=2 (CONFIGURED) set status DETACHED in the NetworkElementAccont=1 and delete NetworkElementAccont=2 MO'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'cbrs is disabled'
            nodePamConfigStatusMocked.isCbrsDomainEnabled() >> false
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", false)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
            addNeAccount("RadioNode1", 2, NetworkElementAccountUpdateStatus.CONFIGURED.name())
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") != null
            mo.allAttributes.get("currentPassword") != null
            mo.allAttributes.get("nextUsername") == null
            mo.allAttributes.get("nextPassword") == null
            mo.allAttributes.get("lastPasswordChange") != null
            mo.allAttributes.get("lastFailed") == null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION.name()
            mo.allAttributes.get("mainJobId") == 10
            mo.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.DETACHED.name()
        and: 'NetworkElementAccount MO with id=2 is created/modified'
            def mo2 = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 2)
            mo2 == null
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SUCCESS.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") == null
    }

    def 'receive NodePamRecoveryConfigurationRequest with node SYNCHRONIZED, remoteManagement=false, cbrs enabled NetworkElementAccount=1 (CONFIGURED) and  NetworkElementAccount=2 (CONFIGURED) set status DETACHED in the NetworkElementAccont=1 and NetworkElementAccont=2 MO'() {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'cbrs is disabled'
            nodePamConfigStatusMocked.isCbrsDomainEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", false)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
            addNeAccount("RadioNode1", 2, NetworkElementAccountUpdateStatus.CONFIGURED.name())
        and: 'NpamNeJob configured'
            neJobPO1 = addNpamNeJob(10, JobState.CREATED, null, 'RadioNode1' )
        and: 'the event is created'
            NodePamRecoveryConfigurationRequest nodePamRequest = new NodePamRecoveryConfigurationRequest(userId, requestId, "RadioNode1", neJobPO1.getPoId(), JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, 10)
        and: 'node is authorized'
            def secSubject = new ESecuritySubject(userId)
            final ESecurityTarget target = new ESecurityTarget("RadioNode1");
            mockedAccessControl.isAuthorized(secSubject, target) >> true
        when: 'the event is consumed'
            objUnderTest.receiveJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("currentUsername") != null
            mo.allAttributes.get("currentPassword") != null
            mo.allAttributes.get("nextUsername") == null
            mo.allAttributes.get("nextPassword") == null
            mo.allAttributes.get("lastPasswordChange") != null
            mo.allAttributes.get("lastFailed") == null
            mo.allAttributes.get("errorDetails") == null
            mo.allAttributes.get("jobType") == JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION.name()
            mo.allAttributes.get("mainJobId") == 10
            mo.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.DETACHED.name()
        and: 'NetworkElementAccount MO with id=2 is created/modified'
            def mo2 = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 2)
            mo2.allAttributes.get("currentUsername") != null
            mo2.allAttributes.get("currentPassword") != null
            mo2.allAttributes.get("nextUsername") == null
            mo2.allAttributes.get("nextPassword") == null
            mo2.allAttributes.get("lastPasswordChange") != null
            mo2.allAttributes.get("lastFailed") == null
            mo2.allAttributes.get("errorDetails") == null
            mo2.allAttributes.get("jobType") == JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION.name()
            mo2.allAttributes.get("mainJobId") == 10
            mo2.allAttributes.get("neJobId") == neJobPO1.getPoId()
            mo2.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.DETACHED.name()
        and: 'no nodePamMediationEvent sent'
            0 * mediationEventSenderMock.send(_)
        and: 'NPamNEJob PO modified'
            def po = dpsQueryUtil.findPersistentObject(neJobPO1.getPoId())
            po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
            po.allAttributes.get("result") == JobResult.SUCCESS.getJobResult()
            po.allAttributes.get("step") == Step.NONE.name()
            po.allAttributes.get("endTime") != null
            po.allAttributes.get("errorDetails") == null
    }

    def 'coverage only on toString'() {
        when:
            NodePamAutogeneratePasswordRequest request1 = new NodePamAutogeneratePasswordRequest(userId, requestId, "RadioNode1", -1, "1", null, null)
            NodePamDisableRemoteManagementEventRequest request2 = new NodePamDisableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
            NodePamDisableRemoteManagementRequest request3 = new NodePamDisableRemoteManagementRequest(userId, requestId, "RadioNode1", -1, null, null)
            NodePamEnableRemoteManagementEventRequest request4 = new NodePamEnableRemoteManagementEventRequest(userId, requestId, "RadioNode1")
            NodePamEnableRemoteManagementRequest request5 = new NodePamEnableRemoteManagementRequest(userId, requestId, "RadioNode1", -1, null, null)
            NodePamUpdatePasswordRequest request6 = new NodePamUpdatePasswordRequest(userId, requestId, "RadioNode1", -1, "1", null, null)
            NodePamDisableFeatureRequest request7 = new NodePamDisableFeatureRequest()
            request6.setNextUser('userName')
            request6.setNextPasswd('nextPasssword')
        NodeInfo nodeInfo = new NodeInfo()
            ManagedObjectInfo managedObjectInfo = new ManagedObjectInfo("fdn", "type", "nameSpace", "nameSpaceVersion")
        then:
            request1.toString() != null
            request2.toString() != null
            request3.toString() != null
            request4.toString() != null
            request5.toString() != null
            request6.toString() != null
            request7.toString() != null
            request6.toStringDebug() != null
            nodeInfo.toString() != null
            managedObjectInfo.toString() != null
    }

    /*
        NodePamDisableFeatureRequest Section
    * */
    def 'receive NodePamDisableFeatureRequest set status DETACHED in the NetworkElementAccont=1 MO and NetworkElementAccont=2 MO' () {
        given: 'functionality is enabled'
            nodePamConfigStatusMocked.isEnabled() >> true
        and: 'Database is available'
            databaseAvailabilityCheckerMock.isAvailable() >> true
        and: 'node configured'
            addSubnetWork(SUBNETWORK_NAME)
            addNodeTree(SUBNETWORK_NAME,"RadioNode1", "SYNCHRONIZED", false)
            addNeAccount("RadioNode1", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
            addNeAccount("RadioNode1", 2, NetworkElementAccountUpdateStatus.CONFIGURED.name())
        and: 'the event is created'
            NodePamDisableFeatureRequest nodePamRequest = new NodePamDisableFeatureRequest()
        when: 'the event is consumed'
            objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamRequest))
        then: 'NetworkElementAccount MO is created/modified'
            def mo = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 1)
            mo.allAttributes.get("jobType") == null
            mo.allAttributes.get("mainJobId") == null
            mo.allAttributes.get("neJobId") == null
            mo.allAttributes.get("updateStatus") == NetworkElementAccountUpdateStatus.DETACHED.name()
        and: 'NetworkElementAccount MO with id=2 is deleted'
            def mo2 = dpsQueryUtil.getNetworkElementAccount("RadioNode1", 2)
            mo2 == null
    }
}
