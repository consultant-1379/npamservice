package com.ericsson.oss.services.security.npam.ejb.listener

import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccountUpdateStatus
import com.ericsson.oss.services.security.npam.api.message.*
import spock.lang.Unroll
import javax.inject.Inject

class NodePamEndUpdateOperationListenerSpec extends CdiSpecification {

    @Inject
    NodePamEndUpdateOperationListener objUnderTest

    @Inject
    NodePamEndUpdateOperationMap endUpdateOperationMap;

    def requestId = 'requestId'
    String key = "key"

    def setup() {}

    @Unroll
    def 'receive NodePamEndUpdateOperation with status=#status store in endUpdateOperationMap'(status, errorDetails) {
        when: 'the event is consumed'
            NodePamEndUpdateOperation receivedNodePamEndUpdateOperation = new NodePamEndUpdateOperation(requestId, "RadioNode1", status, key)
            receivedNodePamEndUpdateOperation.setErrorDetails(errorDetails)
            objUnderTest.receiveRequest(receivedNodePamEndUpdateOperation)
        then:
            NodePamEndUpdateOperation nodePamEndUpdateOperation = endUpdateOperationMap.getStatus(key)
            nodePamEndUpdateOperation.requestId == requestId
            nodePamEndUpdateOperation.nodeName == "RadioNode1"
            nodePamEndUpdateOperation.status == status
            nodePamEndUpdateOperation.errorDetails == errorDetails
            nodePamEndUpdateOperation.key == key
        where:
        status                                              | errorDetails || _
        NetworkElementAccountUpdateStatus.ONGOING.name()    | null         || _
        NetworkElementAccountUpdateStatus.CONFIGURED.name() | null         || _
        NetworkElementAccountUpdateStatus.FAILED.name()     | "some error" || _
        NetworkElementAccountUpdateStatus.DETACHED.name()   | null         || _
    }

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
           objUnderTest.toString()
        then:
            true
    }
}
