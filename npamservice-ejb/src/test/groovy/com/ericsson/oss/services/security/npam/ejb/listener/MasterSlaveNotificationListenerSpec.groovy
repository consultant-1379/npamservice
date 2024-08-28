package com.ericsson.oss.services.security.npam.ejb.listener

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.oss.itpf.sdk.cluster.MembershipChangeEvent
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import javax.inject.Inject

class MasterSlaveNotificationListenerSpec extends BaseSetupForTestSpecs {

    @Inject
    MasterSlaveNotificationListener objUnderTest

    @MockedImplementation
    MembershipChangeEvent membershipChangeEventMocked

    def setup() {
    }

    def NULL_VALUE = null

    def 'membershipChangeEventMocked with isMaster true'() {
        given: 'event'
            MembershipChangeEvent.ClusterMemberInfo clusterMemberInfo = new MembershipChangeEvent.ClusterMemberInfo("nodeId", "serviceId", "version")
            membershipChangeEventMocked.isMaster() >> true
            membershipChangeEventMocked.getAllClusterMembers() >> [clusterMemberInfo]
        when: 'the event is consumed'
            objUnderTest.listenForMembershipChange(membershipChangeEventMocked)
        then:
            2 * membershipChangeEventMocked.isMaster()
            1 * membershipChangeEventMocked.getCurrentNumberOfMembers()
    }

    def 'membershipChangeEventMocked with isMaster false'() {
        given: 'event'
            MembershipChangeEvent.ClusterMemberInfo clusterMemberInfo = new MembershipChangeEvent.ClusterMemberInfo("nodeId", "serviceId", "version")
            membershipChangeEventMocked.isMaster() >> false
            membershipChangeEventMocked.getAllClusterMembers() >> [clusterMemberInfo]
        when: 'the event is consumed'
            objUnderTest.listenForMembershipChange(membershipChangeEventMocked)
        then:
            2 * membershipChangeEventMocked.isMaster()
            1 * membershipChangeEventMocked.getCurrentNumberOfMembers()
    }

    def 'membershipChangeEvent null'() {
        given: 'a null event'
            membershipChangeEventMocked = NULL_VALUE
        when: 'the event is consumed'
            objUnderTest.listenForMembershipChange(membershipChangeEventMocked)
        then:
            0 * membershipChangeEventMocked.isMaster()
            0 * membershipChangeEventMocked.getCurrentNumberOfMembers()
    }

}
