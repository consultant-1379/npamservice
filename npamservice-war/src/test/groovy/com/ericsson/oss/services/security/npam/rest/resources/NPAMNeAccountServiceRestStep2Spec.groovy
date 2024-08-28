package com.ericsson.oss.services.security.npam.rest.resources

import javax.inject.Inject
import javax.ws.rs.core.Response

import com.ericsson.cds.cdi.support.rule.*
import com.ericsson.oss.itpf.sdk.context.ContextService
import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEAccount
import com.ericsson.oss.services.security.npam.api.rest.NeAccountService
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus
import com.ericsson.oss.services.security.npam.ejb.services.NodePamEncryptionManager
import com.ericsson.oss.services.security.npam.rest.api.NPamNEAccountCredentialJAXB
import com.ericsson.oss.services.security.npam.rest.testutil.NPAMRestTestUtilSpec

class NPAMNeAccountServiceRestStep2Spec extends NPAMRestTestUtilSpec {

    @ObjectUnderTest
    private NPAMNeAccountServiceRest objUnderTest

    @Inject
    ContextService cxtMock

    @Inject
    NodePamEncryptionManager criptoMock

    @MockedImplementation
    NodePamConfigStatus nodePamConfigStatusMock

    @MockedImplementation
    NeAccountService neAccountServiceImplMock

    @ImplementationClasses
    def classes = []

    def setup() {
        createNetworkElementMo("TestNode1")
        createNetworkElementMo("TestNode2")
        runtimeDps.withTransactionBoundaries()
        cxtMock.getContextValue("X-Tor-UserID") >> "administrator"
        neAccountServiceImplMock.getNEAccounts(_)>> {
            def attributes = new HashMap<>()
            attributes.put("currentUsername", "TestNode1")
            attributes.put("currentPassword", "testPassword")
            attributes.put("lastPasswordChange", new Date())
            attributes.put("updateStatus", "COMPLETED")
            attributes.put("networkElementAccountId", "1")
            def nPamNeAccount = new NPamNEAccount("TestNode1", attributes)
            def attributes2 = new HashMap<>()
            attributes2.put("currentUsername", "TestNode2")
            attributes2.put("currentPassword", "testPassword1")
            attributes2.put("lastPasswordChange", new Date())
            attributes2.put("updateStatus", "COMPLETED")
            attributes2.put("networkElementAccountId", "2")
            def nPamNeAccount2 = new NPamNEAccount("TestNode2", attributes2)
            def attributes3 = new HashMap<>()
            attributes3.put("currentUsername", "TestNode2")
            attributes3.put("currentPassword", "testPassword2")
            attributes3.put("lastPasswordChange", new Date())
            attributes3.put("updateStatus", "COMPLETED")
            attributes3.put("networkElementAccountId", "2")
            def nPamNeAccount3 = new NPamNEAccount("TestNode2", attributes3)
            return Arrays.asList(nPamNeAccount, nPamNeAccount2, nPamNeAccount3)
        }
        neAccountServiceImplMock.retrieveIpAddress(_)>> {
            return "610.610.610.1"
        }
    }

    def 'run retrieve details for node'() {
        given: 'a network element'
        def neName = "TestNode1"
        nodePamConfigStatusMock.isEnabled() >> true
        neAccountServiceImplMock.getPwdInPlainText(_)>>{
            return "TestPassw0rd"
        }

        when: 'execute the rest call to retrieve the neAccount details'
        def responseRetrieve = objUnderTest.retrievePasswordForNode(neName)
        def responseEntity = (List<NPamNEAccountCredentialJAXB>)responseRetrieve.getEntity()
        then:
        responseRetrieve.getStatus() == Response.Status.OK.statusCode
        responseEntity.get(0).currentPswd == "TestPassw0rd"
        responseEntity.get(1).currentPswd == "TestPassw0rd"
    }

    def 'run retrieve details for node without correct rights'() {
        given: 'a network element'
        def neName = "TestNode1"
        nodePamConfigStatusMock.isEnabled() >> true
        neAccountServiceImplMock.getPwdInPlainText(_)>>{
            throw new SecurityViolationException()
        }

        when: 'execute the rest call to retrieve the neAccount details'
        def responseRetrieve = objUnderTest.retrievePasswordForNode(neName)
        def responseEntity = (List<NPamNEAccountCredentialJAXB>)responseRetrieve.getEntity()
        then:
        responseRetrieve.getStatus() == Response.Status.OK.statusCode
        responseEntity.get(0).currentPswd == "********"
    }
}
