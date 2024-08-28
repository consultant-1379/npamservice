package com.ericsson.oss.services.security.npam.ejb.utility


import com.ericsson.cds.cdi.support.spock.CdiSpecification


import javax.inject.Inject

class ThreadSuspendSpec extends CdiSpecification {

    @Inject
    ThreadSuspend objUnderTest

    def 'test SleepException'() {
       when: 'the event is consumed'
            Thread.currentThread().interrupt()
            objUnderTest.waitFor(100)
        then:
            noExceptionThrown()
    }

    def 'test Normal sleep'() {
        when: 'the event is consumed'
            objUnderTest.waitFor(100)
        then:
            noExceptionThrown()
    }
}
