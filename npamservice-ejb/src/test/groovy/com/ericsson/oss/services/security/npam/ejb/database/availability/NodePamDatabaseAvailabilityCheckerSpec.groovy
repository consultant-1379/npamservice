package com.ericsson.oss.services.security.npam.ejb.database.availability

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification

class NodePamDatabaseAvailabilityCheckerSpec extends CdiSpecification {

    @ObjectUnderTest
    private NodePamDatabaseAvailabilityChecker objUnderTest

    def setup() {
    }

    def 'isAvailable'() {
        when:
            def ret = objUnderTest.isAvailable()
        then:
            ret == false
    }

    def 'isRegisteredAndAvailable'() {
        when:
            def ret = objUnderTest.isRegisteredAndAvailable()
        then:
            ret == false
    }
}


