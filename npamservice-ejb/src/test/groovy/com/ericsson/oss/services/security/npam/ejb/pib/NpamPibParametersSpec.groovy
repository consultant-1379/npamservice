package com.ericsson.oss.services.security.npam.ejb.pib


import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.security.npam.ejb.pib.NpamPibParameters

import javax.inject.Inject

class NpamPibParametersSpec extends CdiSpecification {

    @Inject
    NpamPibParameters objUnderTest

    def 'test get pib parameter'() {
       when: 'the event is consumed'
            objUnderTest.listenForHouseKeepingDaysChanges(10)
            def result = objUnderTest.getNpamHouseKeepingDays()
        then:
            result == 10
    }
}
