package com.ericsson.oss.services.security.npam.ejb.database.availability

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.security.npam.ejb.database.availability.event.DataBaseEvent

class NodePamDatabaseAvailabilityCallbackSpec extends CdiSpecification {

    @ObjectUnderTest
    private NodePamDatabaseAvailabilityCallback objUnderTest

    def setup() {
    }

    def 'onServiceAvailable'() {
        when:
            objUnderTest.onServiceAvailable()
        then:
            true
    }

    def 'onServiceUnavailable'() {
        when:
            objUnderTest.onServiceUnavailable()
        then:
            true
    }

    def 'getCallbackName'() {
        when:
            objUnderTest.getCallbackName()
        then:
            true
    }
}


