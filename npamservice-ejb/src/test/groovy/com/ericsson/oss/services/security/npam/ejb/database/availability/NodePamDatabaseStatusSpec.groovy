package com.ericsson.oss.services.security.npam.ejb.database.availability

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification

class NodePamDatabaseStatusSpec extends CdiSpecification {

    @ObjectUnderTest
    private NodePamDatabaseStatus objUnderTest

    def setup() {
    }

    def cleanup() {
        println('Cleaning up after a test!')
        System.clearProperty("hack.for.integration.test.to.work.databasestatus.available")
    }

    def 'ask settingInitialStatus for coverage without property' () {
        when: 'ask onDbAvailable'
            objUnderTest.settingInitialStatus()
        then:
            objUnderTest.isAvailable() == false
            objUnderTest.isRegisteredAndAvailable() == false
    }

    def 'ask settingInitialStatus for coverage with property' () {
        given: 'system property set'
            System.setProperty("hack.for.integration.test.to.work.databasestatus.available", "true")

        when: 'ask onDbAvailable'
            objUnderTest.settingInitialStatus()
        then:
            objUnderTest.isAvailable() == true
            objUnderTest.isRegisteredAndAvailable() == true
    }

    def 'when database become available for coverage' () {
        given: 'database unavailable'
            objUnderTest.setAvailable(false)
        when:  'database becomes available'
            objUnderTest.setAvailable(true)
        then:
            objUnderTest.isAvailable() == true
            objUnderTest.isRegisteredAndAvailable() == true
    }

    def 'when database become unavailable for coverage' () {
        given: 'database available'
            objUnderTest.setAvailable(true)
        when:  'database becomes unavailable'
            objUnderTest.setAvailable(false)
        then:
            objUnderTest.isAvailable() == false
            objUnderTest.isRegisteredAndAvailable() == true
    }
}


