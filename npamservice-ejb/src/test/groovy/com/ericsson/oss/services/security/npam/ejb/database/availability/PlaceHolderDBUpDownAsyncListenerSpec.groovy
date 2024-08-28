package com.ericsson.oss.services.security.npam.ejb.database.availability

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.security.npam.ejb.database.availability.event.DataBaseEvent

class PlaceHolderDBUpDownAsyncListenerSpec extends CdiSpecification {

    @ObjectUnderTest
    private PlaceHolderDBUpDownAsyncListener objUnderTest

    def setup() {
    }

    def 'onDbAvailable'() {
        when:
            objUnderTest.onDbAvailable(new DataBaseEvent())
        then:
            true
    }

    def 'onDbNotAvailable'() {
        when:
            objUnderTest.onDbNotAvailable(new DataBaseEvent())
        then:
            true
    }
}


