package com.ericsson.oss.services.security.npam.rest.api

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEAccount

class NPamNEAccountCredentialJAXBSpec extends CdiSpecification {

    @ObjectUnderTest
    private NPamNEAccountCredentialJAXB objUnderTest


    def 'run constructor NPamNEAccountCredentialJAXB with all fields'() {
        given: 'an NPamNEAccount object'
        def neAccount = new NPamNEAccount()
        neAccount.setNeName("neName")
        neAccount.setCurrentPswd("acdredd1212!!!")
        neAccount.setCurrentUser("Administrator")
        neAccount.setErrorDetails("this is an error detail")
        neAccount.setNetworkElementAccountId("1")
        neAccount.setStatus("CONFIGURED")
        when: 'call NPamNEAccountCredentialJAXB constructor'
        def nPamNEAccountCredentialJAXB = new NPamNEAccountCredentialJAXB(neAccount)
        then : 'the parameters are correctly filled '
        nPamNEAccountCredentialJAXB.getNeName().equals("neName")
        nPamNEAccountCredentialJAXB.getCurrentPswd().equals("acdredd1212!!!")
        nPamNEAccountCredentialJAXB.getCurrentUser().equals("Administrator")
        nPamNEAccountCredentialJAXB.getStatus().equals("CONFIGURED")
        nPamNEAccountCredentialJAXB.getErrorDetails().equals("this is an error detail")
    }

    def 'run constructor NPamNEAccountCredentialJAXB with some fields null'() {
        given: 'an NPamNEAccount object'
        def neAccount = new NPamNEAccount()
        neAccount.setNeName(null)
        neAccount.setCurrentPswd(null)
        neAccount.setCurrentUser(null)
        neAccount.setErrorDetails(null)
        neAccount.setNetworkElementAccountId(null)
        neAccount.setStatus(null)
        when: 'call NPamNEAccountCredentialJAXB constructor'
        def nPamNEAccountCredentialJAXB = new NPamNEAccountCredentialJAXB(neAccount)
        then : 'the parameters are correctly filled '
        nPamNEAccountCredentialJAXB.getNeName().equals("")
        nPamNEAccountCredentialJAXB.getCurrentPswd().equals("")
        nPamNEAccountCredentialJAXB.getCurrentUser().equals("")
        nPamNEAccountCredentialJAXB.getStatus().equals("")
        nPamNEAccountCredentialJAXB.getErrorDetails().equals("")
    }

    def 'run setter and getter NPamNEAccountCredentialJAXB'() {
        given: 'an NPamNEAccountCredentialJAXB object'
        def nPamNEAccountCredentialJAXB = new NPamNEAccountCredentialJAXB()
        def now = new Date()
        when: 'call all the getters methods'
        nPamNEAccountCredentialJAXB.setNeName("neName")
        nPamNEAccountCredentialJAXB.setCurrentPswd("verysecretpass")
        nPamNEAccountCredentialJAXB.setCurrentUser("administrator")
        nPamNEAccountCredentialJAXB.setErrorDetails("this is the error details")
        nPamNEAccountCredentialJAXB.setId("2")
        nPamNEAccountCredentialJAXB.setStatus("CONFIGURED")
        nPamNEAccountCredentialJAXB.setIpAddress("1.1.1.1")
        nPamNEAccountCredentialJAXB.setLastUpdate(now)
        then : 'the parameters are correctly retrieved with the getters methods '
        nPamNEAccountCredentialJAXB.getNeName().equals("neName")
        nPamNEAccountCredentialJAXB.getCurrentPswd().equals("verysecretpass")
        nPamNEAccountCredentialJAXB.getCurrentUser().equals("administrator")
        nPamNEAccountCredentialJAXB.getStatus().equals("CONFIGURED")
        nPamNEAccountCredentialJAXB.getErrorDetails().equals("this is the error details")
        nPamNEAccountCredentialJAXB.getId().equals("2")
        nPamNEAccountCredentialJAXB.getIpAddress().equals("1.1.1.1")
        nPamNEAccountCredentialJAXB.getLastUpdate().equals(now)
    }

    def 'run setter with null and getter NPamNEAccountCredentialJAXB'() {
        given: 'an NPamNEAccountCredentialJAXB object'
        def nPamNEAccountCredentialJAXB = new NPamNEAccountCredentialJAXB()
        def now = new Date()
        when: 'call all the getters methods'
        nPamNEAccountCredentialJAXB.setNeName(null)
        nPamNEAccountCredentialJAXB.setCurrentPswd(null)
        nPamNEAccountCredentialJAXB.setCurrentUser(null)
        nPamNEAccountCredentialJAXB.setErrorDetails(null)
        nPamNEAccountCredentialJAXB.setId(null)
        nPamNEAccountCredentialJAXB.setStatus(null)
        nPamNEAccountCredentialJAXB.setIpAddress(null)
        nPamNEAccountCredentialJAXB.setLastUpdate(now)
        then : 'the parameters are correctly retrieved with the getters methods '
        nPamNEAccountCredentialJAXB.getNeName().equals("")
        nPamNEAccountCredentialJAXB.getCurrentPswd().equals("")
        nPamNEAccountCredentialJAXB.getCurrentUser().equals("")
        nPamNEAccountCredentialJAXB.getStatus().equals("")
        nPamNEAccountCredentialJAXB.getErrorDetails().equals("")
        nPamNEAccountCredentialJAXB.getId().equals("")
        nPamNEAccountCredentialJAXB.getIpAddress().equals("")
        nPamNEAccountCredentialJAXB.getLastUpdate().equals(now)
    }
}
