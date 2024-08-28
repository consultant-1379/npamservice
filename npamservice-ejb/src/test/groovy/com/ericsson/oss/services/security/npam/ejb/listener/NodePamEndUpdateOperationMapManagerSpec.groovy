package com.ericsson.oss.services.security.npam.ejb.listener

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccountUpdateStatus
import com.ericsson.oss.services.security.npam.api.message.NodePamEndUpdateOperation
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs

import spock.lang.Unroll

import javax.inject.Inject

class NodePamEndUpdateOperationMapManagerSpec extends BaseSetupForTestSpecs {

    @Inject
    NodePamEndUpdateOperationMapManager objUnderTest

    @MockedImplementation
    NodePamEndUpdateOperationMap nodePamEndUpdateOperationMapMock;

    final String nodename = "node1"
    final String requestId = "requestId"
    final String key = "key"
    NodePamEndUpdateOperation endUpdateWithStatusConfigured =  new NodePamEndUpdateOperation(requestId, nodename, NetworkElementAccountUpdateStatus.CONFIGURED.name(), key)

    @Unroll
    def 'test EndOperation cycle with EndOperationStatus = #updateStatus'() {
        given: ' '
            NodePamEndUpdateOperation endUpdateWithStatus =  new NodePamEndUpdateOperation(requestId, nodename, updateStatus, key)
        when: 'the event is consumed'
            objUnderTest.pollRequestStatus(key)
        then:
            expectedCycle * nodePamEndUpdateOperationMapMock.getStatus(key) >>  null >> endUpdateWithStatus >> endUpdateWithStatusConfigured
        where:
        updateStatus                                            || expectedCycle
        NetworkElementAccountUpdateStatus.CONFIGURED.name()     || 2
        NetworkElementAccountUpdateStatus.FAILED.name()         || 2
        NetworkElementAccountUpdateStatus.DETACHED.name()       || 2
        NetworkElementAccountUpdateStatus.ONGOING.name()        || 3
    }

    @Unroll
    def 'test checkStatus with EndOperationStatus = #updateStatus'() {
        given: ' '
            NodePamEndUpdateOperation nodePamEndUpdateOperation = new NodePamEndUpdateOperation(requestId, nodename, updateStatus, key)
        when: 'the event is consumed'
            objUnderTest.checkStatus(nodePamEndUpdateOperation, "some phase")
        then: 'raise exception'
            thrown(Exception)
        where:
            updateStatus                                            ||_
            NetworkElementAccountUpdateStatus.FAILED.name()         ||_
            NetworkElementAccountUpdateStatus.ONGOING.name()        ||_
    }

    @Unroll
    def 'test checkStatus with EndOperationStatus = #updateStatus do not throw exception'() {
        given: ' '
            NodePamEndUpdateOperation nodePamEndUpdateOperation = new NodePamEndUpdateOperation(requestId, nodename, updateStatus, key)
        when: 'the event is consumed'
            objUnderTest.checkStatus(nodePamEndUpdateOperation, "some phase")
        then: 'raise exception'
            notThrown(Exception)
        where:
        updateStatus                                                ||_
            NetworkElementAccountUpdateStatus.CONFIGURED.name()     ||_
            NetworkElementAccountUpdateStatus.DETACHED.name()       ||_
    }

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
            objUnderTest.toString()
        then:
            true
    }
  }
