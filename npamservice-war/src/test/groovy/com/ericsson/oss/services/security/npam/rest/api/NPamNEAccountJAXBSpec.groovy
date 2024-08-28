package com.ericsson.oss.services.security.npam.rest.api

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEAccount

class NPamNEAccountJAXBSpec extends CdiSpecification {

    @ObjectUnderTest
    private NPamNEAccountJAXB objUnderTest


    def 'run constructor NPamNEAccountJAXB with all fields'() {
        given: 'an NPamNEAccount object'
        def now = new Date()
        def neAccount = new NPamNEAccount()
        neAccount.setNeName("neName")
        neAccount.setCurrentUser("Administrator")
        neAccount.setErrorDetails("this is an error detail")
        neAccount.setNetworkElementAccountId("1")
        neAccount.setStatus("CONFIGURED")
        neAccount.setLastUpdate(now)
        when: 'call NPamNEAccountCredentialJAXB constructor'
        def nPamNEAccountJAXB = new NPamNEAccountJAXB(neAccount)
        then : 'the parameters are correctly filled '
        nPamNEAccountJAXB.getNeName().equals("neName")
        nPamNEAccountJAXB.getCurrentUser().equals("Administrator")
        nPamNEAccountJAXB.getStatus().equals("CONFIGURED")
        nPamNEAccountJAXB.getErrorDetails().equals("this is an error detail")
        nPamNEAccountJAXB.getId().equals("1")
        nPamNEAccountJAXB.getLastUpdate().equals(now)
    }

    def 'run constructor NPamNEAccountJAXB with all fields null'() {
        given: 'an NPamNEAccount object'
        def neAccount = new NPamNEAccount()
        neAccount.setNeName(null)
        neAccount.setCurrentUser(null)
        neAccount.setErrorDetails(null)
        neAccount.setNetworkElementAccountId(null)
        neAccount.setStatus(null)
        neAccount.setLastUpdate(null)
        when: 'call NPamNEAccountJAXB constructor'
        def nPamNEAccountJAXB = new NPamNEAccountJAXB(neAccount)
        then : 'the parameters are correctly filled '
        nPamNEAccountJAXB.getNeName().equals("")
        nPamNEAccountJAXB.getCurrentUser().equals("")
        nPamNEAccountJAXB.getStatus().equals("")
        nPamNEAccountJAXB.getErrorDetails().equals("")
        nPamNEAccountJAXB.getLastUpdate().equals(null)
    }

    def 'run setter and getter NPamNEAccountJAXB'() {
        given: 'an NPamNEAccountJAXB object'
        def nPamNEAccountJAXB = new NPamNEAccountJAXB()
        def now = new Date()
        when: 'call all the getters methods'
        nPamNEAccountJAXB.setNeName("neName")
        nPamNEAccountJAXB.setCurrentUser("administrator")
        nPamNEAccountJAXB.setErrorDetails("this is the error details")
        nPamNEAccountJAXB.setId("2")
        nPamNEAccountJAXB.setStatus("CONFIGURED")
        nPamNEAccountJAXB.setLastUpdate(now)
        then : 'the parameters are correctly retrieved with the getters methods '
        nPamNEAccountJAXB.getNeName().equals("neName")
        nPamNEAccountJAXB.getCurrentUser().equals("administrator")
        nPamNEAccountJAXB.getStatus().equals("CONFIGURED")
        nPamNEAccountJAXB.getErrorDetails().equals("this is the error details")
        nPamNEAccountJAXB.getId().equals("2")
        nPamNEAccountJAXB.getLastUpdate().equals(now)
    }

    def 'run setter with null and getter NPamNEAccountJAXB'() {
        given: 'an NPamNEAccountJAXB object'
        def nPamNEAccountJAXB = new NPamNEAccountJAXB()
        def now = new Date()
        when: 'call all the getters methods'
        nPamNEAccountJAXB.setNeName(null)
        nPamNEAccountJAXB.setCurrentUser(null)
        nPamNEAccountJAXB.setErrorDetails(null)
        nPamNEAccountJAXB.setId(null)
        nPamNEAccountJAXB.setStatus(null)
        nPamNEAccountJAXB.setLastUpdate(now)
        then : 'the parameters are correctly retrieved with the getters methods '
        nPamNEAccountJAXB.getNeName().equals("")
        nPamNEAccountJAXB.getCurrentUser().equals("")
        nPamNEAccountJAXB.getStatus().equals("")
        nPamNEAccountJAXB.getErrorDetails().equals("")
        nPamNEAccountJAXB.getId().equals("")
        nPamNEAccountJAXB.getLastUpdate().equals(now)
    }
}
