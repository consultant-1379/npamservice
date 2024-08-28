package com.ericsson.oss.services.security.npam.ejb.listener

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.oss.itpf.datalayer.dps.notification.event.*
import com.ericsson.oss.itpf.sdk.eventbus.Channel
import com.ericsson.oss.itpf.sdk.eventbus.Event
import com.ericsson.oss.services.security.npam.api.constants.ModelsConstants
import com.ericsson.oss.services.security.npam.api.message.NodePamDisableRemoteManagementEventRequest
import com.ericsson.oss.services.security.npam.api.message.NodePamEnableRemoteManagementEventRequest
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus
import spock.lang.Unroll

import javax.inject.Inject

import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_NAME
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_VALUE
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_PARAM_ENABLED
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_PROPERTIES
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_NPAM

class DpsDataChangedEventListenerSpec extends BaseSetupForTestSpecs {

    @Inject
    DpsDataChangedEventListener objUnderTest

    @MockedImplementation
    MembershipListenerInterface membershipListenerInterfaceMock

    @MockedImplementation
    DpsDataChangedEvent dpsDataChangedEventMocked

    @Inject
    private Channel mockedNodePamRequestQueueChannel

    @MockedImplementation
    NodePamConfigStatus nodePamConfigStatusMocked

    def mockedEvent = Mock(Event)

    def setup() {
        membershipListenerInterfaceMock.isMaster() >> true
    }


    def NULL_VALUE = null

    def 'Notify event with null type do nothing'() {
        given: 'invalid event'
            dpsDataChangedEventMocked.type = null
        when: 'the event is consumed'
            objUnderTest.updateOnEvent(dpsDataChangedEventMocked)
        then:
            0 * mockedNodePamRequestQueueChannel.createEvent(*_)
            0 * mockedNodePamRequestQueueChannel.send(*_)
    }

    def 'Notify unexpected event do nothing'() {
        given: 'invalid event'
        dpsDataChangedEventMocked.type = "someType"
        dpsDataChangedEventMocked.getEventType() >>  EventType.DATA_BUCKET_DELETED
        when: 'the event is consumed'
        objUnderTest.updateOnEvent(dpsDataChangedEventMocked)
        then:
        0 * mockedNodePamRequestQueueChannel.createEvent(*_)
        0 * mockedNodePamRequestQueueChannel.send(*_)
    }


    /*
        CREATE section
    * */
    def 'updateOnEvent CREATE event of MaintenanceUserSecurity with remoteManagement=true send to handler queue'() {
        given: 'event'
            DpsObjectCreatedEvent event = getMaintenanceUserSecurityCreatedEvent("RadioNode1", true)
        when: 'the event is consumed'
            objUnderTest.updateOnEvent(event)
        then:
        1 * mockedNodePamRequestQueueChannel.createEvent({
            NodePamEnableRemoteManagementEventRequest request = (NodePamEnableRemoteManagementEventRequest)it
            assert request.nodeName == "RadioNode1"
            return true
            }, _) >> mockedEvent
            1 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
    }

    def 'updateOnEvent CREATE event of MaintenanceUserSecurity with remoteManagement=false send to handler queue'() {
        given: 'event'
            DpsObjectCreatedEvent event = getMaintenanceUserSecurityCreatedEvent("RadioNode1", false)
        when: 'the event is consumed'
            objUnderTest.updateOnEvent(event)
        then:
        1 * mockedNodePamRequestQueueChannel.createEvent({
            NodePamDisableRemoteManagementEventRequest request = (NodePamDisableRemoteManagementEventRequest)it
            assert request.nodeName == "RadioNode1"
            return true
        }, _) >> mockedEvent
        1 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
    }

    def 'updateOnEvent CREATE event of MaintenanceUserSecurity with remoteManagement=null attribute do nothing'() {
        given: 'event'
            DpsObjectCreatedEvent event = getMaintenanceUserSecurityCreatedEvent("RadioNode1", NULL_VALUE)
        when: 'the event is consumed'
            objUnderTest.updateOnEvent(event)
        then:
            0 * mockedNodePamRequestQueueChannel.createEvent(*_)
            0 * mockedNodePamRequestQueueChannel.send(*_)
    }

    def 'updateOnEvent CREATE event of MaintenanceUserSecurity without remoteManagement attribute do nothing'() {
        given: 'event'
            DpsObjectCreatedEvent event = getMaintenanceUserSecurityCreatedEventWithoutRemoteManagement("RadioNode1")
       when: 'the event is consumed'
            objUnderTest.updateOnEvent(event)
        then:
            0 * mockedNodePamRequestQueueChannel.createEvent(*_)
            0 * mockedNodePamRequestQueueChannel.send(*_)
    }

    def 'updateOnEvent CREATE event of generic MO do nothing'() {
        given: 'event'
            DpsDataChangedEvent event = new DpsObjectCreatedEvent()
            event.setFdn("SampleMo=1")
            event.setType("SampleMo")
            Map<String, Object> attributeValues = new HashMap<>(1)
            event.setAttributeValues(attributeValues)
        when: 'the event is consumed'
            objUnderTest.updateOnEvent(event)
        then:
            0 * mockedNodePamRequestQueueChannel.createEvent(*_)
            0 * mockedNodePamRequestQueueChannel.send(*_)
    }

    def 'updateOnEvent CREATE event of invalid class do nothing'() {
        given: 'invalid event'
            dpsDataChangedEventMocked.type = "someType"
            dpsDataChangedEventMocked.getEventType() >>  EventType.OBJECT_CREATED
        when: 'the event is consumed'
            objUnderTest.updateOnEvent(dpsDataChangedEventMocked)
        then:
            0 * mockedNodePamRequestQueueChannel.createEvent(*_)
            0 * mockedNodePamRequestQueueChannel.send(*_)
    }

    /*
        DELETE section
    * */
    def 'updateOnEvent DELETE event of MaintenanceUserSecurity do nothing'() {
        given: 'event'
            DpsObjectDeletedEvent event = getMaintenanceUserSecurityDeletedEvent("RadioNode1",true )
        when: 'the event is consumed'
            objUnderTest.updateOnEvent(event)
        then:
            0 * mockedNodePamRequestQueueChannel.createEvent(*_)
            0 * mockedNodePamRequestQueueChannel.send(*_)
    }

    def 'updateOnEvent DELETE event of invalid class do nothing'() {
        given: 'invalid event'
            dpsDataChangedEventMocked.type = "someType"
            dpsDataChangedEventMocked.getEventType() >>  EventType.OBJECT_DELETED
        when: 'the event is consumed'
            objUnderTest.updateOnEvent(dpsDataChangedEventMocked)
        then:
            0 * mockedNodePamRequestQueueChannel.createEvent(*_)
            0 * mockedNodePamRequestQueueChannel.send(*_)
    }

    /*
    MODIFY section
    * */
    def 'updateOnEvent MODIFY event of MaintenanceUserSecurity with remoteManagement=true send to handler queue'() {
        given: 'event'
            DpsAttributeChangedEvent event = getMaintenanceUserSecurityAttributeChangedEvent("RadioNode1", true)
        when: 'the event is consumed'
            objUnderTest.updateOnEvent(event)
        then:
        1 * mockedNodePamRequestQueueChannel.createEvent({
            NodePamEnableRemoteManagementEventRequest request = (NodePamEnableRemoteManagementEventRequest)it
            assert request.nodeName == "RadioNode1"
            return true
        }, _) >> mockedEvent
        1 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
    }

    def 'updateOnEvent MODIFY event of MaintenanceUserSecurity with remoteManagement=false send to handler queue'() {
        given: 'event'
            DpsAttributeChangedEvent event = getMaintenanceUserSecurityAttributeChangedEvent("RadioNode1", false)
        when: 'the event is consumed'
            objUnderTest.updateOnEvent(event)
        then:
        1 * mockedNodePamRequestQueueChannel.createEvent({
            NodePamDisableRemoteManagementEventRequest request = (NodePamDisableRemoteManagementEventRequest)it
            assert request.nodeName == "RadioNode1"
            return true
        }, _) >> mockedEvent
        1 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
    }

    def 'updateOnEvent MODIFY event of MaintenanceUserSecurity with remoteManagement=null do nothing'() {
        given: 'event'
            DpsAttributeChangedEvent event = getMaintenanceUserSecurityAttributeChangedEvent("RadioNode1", NULL_VALUE)
        when: 'the event is consumed'
            objUnderTest.updateOnEvent(event)
        then:
            0 * mockedNodePamRequestQueueChannel.createEvent(*_)
            0 * mockedNodePamRequestQueueChannel.send(*_)
    }

    def 'updateOnEvent MODIFY event of MaintenanceUserSecurity without remoteManagement attribute  do nothing'() {
        given: 'event'
            DpsAttributeChangedEvent event = getMaintenanceUserSecurityAttributeChangedEventWithoutRemoteManagement("RadioNode1")
        when: 'the event is consumed'
            objUnderTest.updateOnEvent(event)
        then:
            0 * mockedNodePamRequestQueueChannel.createEvent(*_)
            0 * mockedNodePamRequestQueueChannel.send(*_)
    }

    def 'Notify MODIFY event of generic MO do nothing'() {
        given: 'event'
           DpsAttributeChangedEvent event = new DpsAttributeChangedEvent()
            event.setFdn("SampleMo=1")
            event.setType("SampleMo")
            Set<AttributeChangeData> changedAttributes = new HashSet<>()
            event.setChangedAttributes(changedAttributes)
        when: 'the event is consumed'
            objUnderTest.updateOnEvent(event)
        then:
            0 * mockedNodePamRequestQueueChannel.createEvent(*_)
            0 * mockedNodePamRequestQueueChannel.send(*_)
    }

    def 'updateOnEvent MODIFY event of invalid class do nothing'() {
        given: 'invalid event'
            dpsDataChangedEventMocked.type = "someType"
            dpsDataChangedEventMocked.getEventType() >>  EventType.ATTRIBUTE_CHANGED
        when: 'the event is consumed'
            objUnderTest.updateOnEvent(dpsDataChangedEventMocked)
        then:
            0 * mockedNodePamRequestQueueChannel.createEvent(*_)
            0 * mockedNodePamRequestQueueChannel.send(*_)
    }

    @Unroll
    def 'updateOnEvent MODIFY event of NPAM_CONFIG class configure '() {
        given: 'invalid event'
            DpsAttributeChangedEvent event = getNodePamConfigStatusAttributeChangedEvent(attributeName)
        when: 'the event is consumed'
            objUnderTest.updateOnEvent(event)
        then:
            0 * mockedNodePamRequestQueueChannel.createEvent(*_)
            0 * mockedNodePamRequestQueueChannel.send(*_)
        and:
            callMethodNumber * nodePamConfigStatusMocked.setConfig(_)
        where:
        attributeName           ||  callMethodNumber
        NPAM_CONFIG_PROPERTIES   ||  1
        "fakeAttributeName"     ||  0
    }


    private DpsObjectCreatedEvent getMaintenanceUserSecurityCreatedEvent(String nodeName, Boolean remoteManagement) {
        DpsObjectCreatedEvent event = getMaintenanceUserSecurityCreatedEventWithoutRemoteManagement(nodeName)
        event.attributeValues.put('remoteManagement', remoteManagement)  //this is the interesting attribute
        event
    }

    private DpsObjectCreatedEvent getMaintenanceUserSecurityCreatedEventWithoutRemoteManagement(String nodeName) {
        DpsObjectCreatedEvent event = new DpsObjectCreatedEvent()
        event.setFdn("ManagedElement="+nodeName+",SystemFunctions=1,SecM=1,UserManagement=1,UserIdentity=1,MaintenanceUserSecurity=1")
        event.setType("MaintenanceUserSecurity")
        Map<String, Object> attributeValues = new HashMap<>(1)
        attributeValues.put('maintenanceUserSecurityId', 1)
        attributeValues.put('failedLoginAttemptPeriod', 5)
        attributeValues.put('loginDelayPolicy', "RANDOM")
        attributeValues.put('noOfFailedLoginAttempts', 0)
        attributeValues.put('restrictMaintenanceUser', false)
        attributeValues.put('userLockoutPeriod', 5)
        event.setAttributeValues(attributeValues)
        event
    }

    private DpsObjectDeletedEvent getMaintenanceUserSecurityDeletedEvent(String nodeName, Boolean remoteManagement) {
        DpsObjectDeletedEvent event = new DpsObjectDeletedEvent()
        event.setFdn("ManagedElement="+nodeName+",SystemFunctions=1,SecM=1,UserManagement=1,UserIdentity=1,MaintenanceUserSecurity=1")
        event.setType("MaintenanceUserSecurity")
        Map<String, Object> attributeValues = new HashMap<>(1)
        attributeValues.put('maintenanceUserSecurityId', 1)
        attributeValues.put('failedLoginAttemptPeriod', 5)
        attributeValues.put('loginDelayPolicy', "RANDOM")
        attributeValues.put('noOfFailedLoginAttempts', 0)
        attributeValues.put('restrictMaintenanceUser', false)
        attributeValues.put('userLockoutPeriod', 5)
        attributeValues.put('remoteManagement', remoteManagement)  //this is the interesting attribute
        event.setAttributeValues(attributeValues)
        event
    }

    private DpsAttributeChangedEvent getMaintenanceUserSecurityAttributeChangedEvent(String nodeName, Boolean remoteManagement) {
        DpsAttributeChangedEvent event = getMaintenanceUserSecurityAttributeChangedEventWithoutRemoteManagement(nodeName)

        //this is the interesting attribute
        AttributeChangeData attr = new AttributeChangeData()
        attr.setName("remoteManagement")
        if  (remoteManagement != NULL_VALUE) {
            attr.setOldValue(!remoteManagement)
            attr.setNewValue(remoteManagement)
        } else {
            attr.setOldValue(NULL_VALUE)
            attr.setNewValue(NULL_VALUE)
        }
        event.changedAttributes.add(attr)

        event
    }

    private DpsAttributeChangedEvent getNodePamConfigStatusAttributeChangedEvent(final String attributeName) {
        DpsAttributeChangedEvent event = new DpsAttributeChangedEvent()

        event.setType(ModelsConstants.NPAM_CONFIG)
        event.setNamespace(ModelsConstants.NAMESPACE)
        //this is the interesting attribute
        event.changedAttributes = new HashSet<>(1)
        AttributeChangeData attr = new AttributeChangeData()
        attr.setName(attributeName)
        final List<Map<String, String>> nPamConfigProperties = new ArrayList<>();
        final Map<String, String> property = new HashMap<>();
        property.put(PROPERTY_NAME, NPAM_CONFIG_NPAM);
        property.put(PROPERTY_VALUE, NPAM_CONFIG_PARAM_ENABLED);
        nPamConfigProperties.add(property);
        attr.setNewValue(nPamConfigProperties)
        attr.setOldValue(null)
        event.changedAttributes.add(attr)
        event
    }

    private DpsAttributeChangedEvent getMaintenanceUserSecurityAttributeChangedEventWithoutRemoteManagement(String nodeName) {
        DpsAttributeChangedEvent event = new DpsAttributeChangedEvent()
        event.setFdn("ManagedElement="+nodeName+",SystemFunctions=1,SecM=1,UserManagement=1,UserIdentity=1,MaintenanceUserSecurity=1")
        event.setType("MaintenanceUserSecurity")

        Set<AttributeChangeData> changedAttributes = new HashSet<>()

        AttributeChangeData attr = new AttributeChangeData()
        attr.setName("loginDelayPolicy")
        attr.setOldValue("RANDOM")
        attr.setNewValue("FIXED")
        changedAttributes.add(attr)

        event.setChangedAttributes(changedAttributes)
        event
    }
}
