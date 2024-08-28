package com.ericsson.oss.services.security.npam.rest.resources

import javax.inject.Inject
import javax.ws.rs.core.Response

import com.ericsson.cds.cdi.support.rule.*
import com.ericsson.oss.itpf.sdk.context.ContextService
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage
import com.ericsson.oss.services.security.npam.api.interfaces.CryptoUtilsOpenSSL
import com.ericsson.oss.services.security.npam.api.job.modelentities.NEInfo
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEAccount
import com.ericsson.oss.services.security.npam.api.job.modelentities.NetworkElementStatus
import com.ericsson.oss.services.security.npam.ejb.dao.DpsWriteOperations
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus
import com.ericsson.oss.services.security.npam.ejb.neaccount.executor.NeAccountGetServiceImpl
import com.ericsson.oss.services.security.npam.ejb.rest.NeAccountServiceImpl
import com.ericsson.oss.services.security.npam.ejb.services.NodePamCredentialManagerImpl
import com.ericsson.oss.services.security.npam.ejb.services.NodePamEncryptionManager
import com.ericsson.oss.services.security.npam.ejb.utility.NetworkUtil
import com.ericsson.oss.services.security.npam.ejb.utility.TbacEvaluation
import com.ericsson.oss.services.security.npam.rest.api.NPAMExportJAXB
import com.ericsson.oss.services.security.npam.rest.api.NPAMGetNeAccountJAXB
import com.ericsson.oss.services.security.npam.rest.api.NPamNEAccountCredentialJAXB
import com.ericsson.oss.services.security.npam.rest.api.NPamNEAccountResponseJAXB
import com.ericsson.oss.services.security.npam.rest.testutil.NPAMRestTestUtilSpec

import spock.lang.Unroll

class NPAMNeAccountServiceRestStep1Spec extends NPAMRestTestUtilSpec {

    @ObjectUnderTest
    private NPAMNeAccountServiceRest objUnderTest

    @Inject
    ContextService cxtMock

    @MockedImplementation
    TbacEvaluation tbacEvaluationMock

    @Inject
    NodePamEncryptionManager criptoMock

    @MockedImplementation
    NodePamConfigStatus nodePamConfigStatusMock

    @MockedImplementation
    NeAccountGetServiceImpl neAccountGetServiceMock

    @Inject
    CryptoUtilsOpenSSL cryptoOpenSSLMock

    @ImplementationClasses
    def classes = [
        DpsWriteOperations,
        NeAccountServiceImpl,
        NetworkUtil,
        NodePamCredentialManagerImpl
    ]

    def setup() {
        createNetworkElementMo("TestNode1")
        createNetworkElementMo("TestNode2")
        createNetworkElementMo("TestNode3")
        createNetworkElementMo("TestNode4")
        createNetworkElementMo("TestNode5")
        runtimeDps.withTransactionBoundaries()
        cxtMock.getContextValue("X-Tor-UserID") >> "administrator"
        tbacEvaluationMock.getNodePermission(_,"TestNode1")>> { return true }
        tbacEvaluationMock.getNodePermission(_,"TestNode2")>> { return true }
        tbacEvaluationMock.getNodePermission(_,"TestNode3")>> { return true }
        tbacEvaluationMock.getNodePermission(_,"TestNode4")>> { return true }
        tbacEvaluationMock.getNodePermission(_,"TestNode5")>> { return true }
        tbacEvaluationMock.getNodePermission(_,"FakeNode")>> { return false }

        Map<String, Boolean> result = new HashMap();
        result.put("TestNode1", true)
        result.put("TestNode3", false)
        result.put("TestNode4", true)
        result.put("TestNode5", false)
        neAccountGetServiceMock.findAllRemoteManagementField() >> result
        def attributes1 = new HashMap<>()
        attributes1.put("currentUsername", "TestNode1")
        attributes1.put("currentPassword", "testPassword")
        attributes1.put("lastPasswordChange", new Date())
        attributes1.put("updateStatus", "COMPLETED")
        attributes1.put("networkElementAccountId", "1")
        def attributes3 = new HashMap<>()
        attributes3.put("currentUsername", "TestNode3")
        attributes3.put("currentPassword", "testPassword")
        attributes3.put("lastPasswordChange", new Date())
        attributes3.put("updateStatus", "DETACHED")
        attributes3.put("networkElementAccountId", "1")
        def attributes3id2 = new HashMap<>()
        attributes3id2.put("currentUsername", "TestNode3")
        attributes3id2.put("currentPassword", "testPassword")
        attributes3id2.put("lastPasswordChange", new Date())
        attributes3id2.put("updateStatus", "DETACHED")
        attributes3id2.put("networkElementAccountId", "2")
        neAccountGetServiceMock.findNEAccountObject("TestNode1", _) >> {

            def nPamNeAccount = new NPamNEAccount("TestNode1", attributes1)
            return Arrays.asList(nPamNeAccount)
        }
        neAccountGetServiceMock.findNEAccountObject("TestNode2", _) >> {
            return new ArrayList<>()
        }
        neAccountGetServiceMock.findNEAccountObject("TestNode4", _) >> {
            return new ArrayList<>()
        }
        neAccountGetServiceMock.findNEAccountObject("TestNode5", _) >> {
            return new ArrayList<>()
        }
        neAccountGetServiceMock.findNEAccountObject("TestNode3", _) >> {
            def nPamNeAccount = new NPamNEAccount("TestNode3", attributes3)
            def nPamNeAccountId2 = new NPamNEAccount("TestNode3", attributes3id2)
            return Arrays.asList(nPamNeAccount, nPamNeAccountId2)
        }
        neAccountGetServiceMock.getAllNEAccountsById(_) >> {
            def attributes = new HashMap<>()
            attributes.put("currentUsername", "TestNode1")
            attributes.put("currentPassword", "testPassword")
            attributes.put("lastPasswordChange", new Date())
            attributes.put("updateStatus", "CONFIGURED")
            attributes.put("networkElementAccountId", "1")
            def nPamNeAccount = new NPamNEAccount("TestNode1", attributes)
            return Arrays.asList(nPamNeAccount)
        }
        Map<String, Boolean> neAccountMap = new HashMap();
        neAccountMap.put("TestNode1",  Arrays.asList(new NPamNEAccount("TestNode1", attributes1)))
        neAccountMap.put("TestNode2",  new ArrayList<>())
        neAccountMap.put("TestNode3",  Arrays.asList(new NPamNEAccount("TestNode3", attributes3), new NPamNEAccount("TestNode3", attributes3id2) ))
        neAccountMap.put("TestNode4",  new ArrayList<>())
        neAccountMap.put("TestNode5",  new ArrayList<>())

        neAccountGetServiceMock.getMapAllNEAccountsById(_) >> neAccountMap
        // we simulate simple algo that reverse the string
        criptoMock.encryptPassword(_) >> { args ->
            String passw = new String(args[0])
            char ch;
            String encrypted = ""
            for (int i=0; i<passw.length(); i++) {
                ch=passw.charAt(i)
                encrypted = new String(ch)+encrypted
            }
            return encrypted
        }
        // we simulate simple algo that reverse the string
        criptoMock.decryptPassword(_)  >> { args ->
            String passw = new String(args[0])
            char ch;
            String decrypted = ""
            for (int i=0; i<passw.length(); i++) {
                ch=passw.charAt(i)
                decrypted = new String(ch)+decrypted
            }
            return decrypted
        }
        cryptoOpenSSLMock.encrypt(_,_,_) >> new ByteArrayOutputStream();
    }

    private NEInfo createNeInfo(List<String> names, List<String> collections, List<String> savedSearches) {
        NEInfo neInfo = new NEInfo();
        neInfo.setNeNames(names)
        neInfo.setSavedSearchIds(savedSearches)
        neInfo.setCollectionNames(collections)
        return neInfo;
    }

    private NPAMGetNeAccountJAXB createGetNeAccountJAXB(List<String> names, List<String> collections, List<String> savedSearches) {
        NPAMGetNeAccountJAXB getNeAccountJAXB = new NPAMGetNeAccountJAXB()
        NEInfo neInfo = new NEInfo();
        neInfo.setNeNames(names)
        neInfo.setSavedSearchIds(savedSearches)
        neInfo.setCollectionNames(collections)
        getNeAccountJAXB.setSelectedNEs(neInfo)
        return getNeAccountJAXB;
    }

    def 'run retrieve details for node'() {
        given: 'a network element'
        def neName = "TestNode1"
        nodePamConfigStatusMock.isEnabled() >> true
        when: 'execute the rest call to retrieve the neAccount details'
        def responseRetrieve = objUnderTest.retrievePasswordForNode(neName)
        def responseEntity = (List<NPamNEAccountCredentialJAXB>)responseRetrieve.getEntity()
        then:
        responseRetrieve.getStatus() == Response.Status.OK.statusCode
        responseEntity.get(0).currentPswd == "drowssaPtset"
    }

    def 'run retrieve details for networkElementAccount filtering on id=1, without filters on neStatus'() {
        given: 'nodes are present , one has a NeAccount associated'
        nodePamConfigStatusMock.isEnabled() >> true
        def selectedNEs = createGetNeAccountJAXB(new ArrayList(Arrays.asList("TestNode1","TestNode2")), new ArrayList(), new ArrayList())
        when: 'execute the rest call to retrieveNeAccounts'
        def responseRetrieve = objUnderTest.retrieveNeAccounts(new ArrayList(Arrays.asList("1")),new ArrayList(),selectedNEs)
        def responseEntity = (List<NPamNEAccountResponseJAXB>)responseRetrieve.getEntity()
        then:'all info are retrieved '
        responseRetrieve.getStatus() == Response.Status.OK.statusCode
        responseEntity.size == 2
    }

    def 'run retrieve details for networkElementAccount filtering on id=1 and id=2, without filters on neStatus'() {
        given: 'nodes are present , one has a NeAccount associated'
        nodePamConfigStatusMock.isEnabled() >> true
        def selectedNEs = createGetNeAccountJAXB(new ArrayList(Arrays.asList("TestNode1","TestNode3")), new ArrayList(), new ArrayList())
        when: 'execute the rest call to retrieveNeAccounts'
        def responseRetrieve = objUnderTest.retrieveNeAccounts(new ArrayList(Arrays.asList("1", "2")),new ArrayList(),selectedNEs)
        def responseEntity = (List<NPamNEAccountResponseJAXB>)responseRetrieve.getEntity()
        then:'all info are retrieved '
        responseRetrieve.getStatus() == Response.Status.OK.statusCode
        responseEntity.size == 2
        responseEntity.get(0).getNeAccounts().size()==1
        responseEntity.get(1).getNeAccounts().size()==2
    }

    def 'run retrieve details for networkElementAccount without filters'() {
        given: 'nodes are present , no filters applied'
        nodePamConfigStatusMock.isEnabled() >> true
        def selectedNEs = createGetNeAccountJAXB(new ArrayList(Arrays.asList("TestNode1","TestNode2","TestNode3")), new ArrayList(), new ArrayList())
        when: 'execute the rest call to retrieveNeAccounts'
        def responseRetrieve = objUnderTest.retrieveNeAccounts(new ArrayList(),new ArrayList(),selectedNEs)
        def responseEntity = (List<NPamNEAccountResponseJAXB>)responseRetrieve.getEntity()
        then:'all info are retrieved '
        responseRetrieve.getStatus() == Response.Status.OK.statusCode
        responseEntity.size == 3
    }

    @Unroll
    def 'run retrieve details for networkElementAccount filtering on #status and id 1'() {
        given: 'three nodes'
        nodePamConfigStatusMock.isEnabled() >> true
        def selectedNEs = createGetNeAccountJAXB(new ArrayList(Arrays.asList("TestNode1","TestNode2","TestNode3")), new ArrayList(), new ArrayList())
        when: 'execute the rest call to retrieveNeAccounts'
        def responseRetrieve = objUnderTest.retrieveNeAccounts(new ArrayList(Arrays.asList("1")),status,selectedNEs)
        def responseEntity = (List<NPamNEAccountResponseJAXB>)responseRetrieve.getEntity()
        then:'nodes are correctly filtered according to their status'
        responseRetrieve.getStatus() == Response.Status.OK.statusCode
        responseEntity.size == 1
        responseEntity.get(0).getNeNpamStatus() == expectedOutputStatus
        responseEntity.get(0).neName == expectedNodeName
        where:
        status                                                              | expectedOutputStatus                | expectedNodeName
        new ArrayList(Arrays.asList(NetworkElementStatus.MANAGED))          | NetworkElementStatus.MANAGED        | "TestNode1"
        new ArrayList(Arrays.asList(NetworkElementStatus.NOT_MANAGED))      | NetworkElementStatus.NOT_MANAGED    | "TestNode3"
        new ArrayList(Arrays.asList(NetworkElementStatus.NOT_SUPPORTED))    | NetworkElementStatus.NOT_SUPPORTED  | "TestNode2"
    }

    def 'run retrieve details of neAccounts filtering on id 1 asking for any status'() {
        given: 'different values of RemoteManagement'
        nodePamConfigStatusMock.isEnabled() >> true
        def selectedNEs = createGetNeAccountJAXB(new ArrayList(Arrays.asList("TestNode1","TestNode2","TestNode3","TestNode4","TestNode5")), new ArrayList(), new ArrayList())
        when: 'execute the rest call to retrieveNeAccounts'
        def responseRetrieve = objUnderTest.retrieveNeAccounts(new ArrayList(Arrays.asList("1")),new ArrayList(Arrays.asList(NetworkElementStatus.MANAGED,NetworkElementStatus.NOT_MANAGED,NetworkElementStatus.NOT_SUPPORTED)),selectedNEs)
        def responseEntity = (List<NPamNEAccountResponseJAXB>)responseRetrieve.getEntity()
        then:'all info regarding the nodes and networkElementAccount(if any) are retrieved with the correct status'
        responseRetrieve.getStatus() == Response.Status.OK.statusCode
        for (NPamNEAccountResponseJAXB elem:responseEntity){
            if (elem.neName.equals("TestNode1") || elem.neName.equals("TestNode4")){
                assert elem.neNpamStatus == NetworkElementStatus.MANAGED
            }
            else if (elem.neName.equals("TestNode2")){
                assert elem.neNpamStatus == NetworkElementStatus.NOT_SUPPORTED
                assert elem.neAccounts.size == 0
            }
            else{
                assert elem.neNpamStatus == NetworkElementStatus.NOT_MANAGED
                if(elem.neName.equals("TestNode5")){
                    assert elem.neAccounts.size == 0
                }
                else{
                    assert elem.neAccounts.size == 2
                }
            }
        }
    }

    def 'run retrieve details for networkElementAccount for a node that does not exist'() {
        given: 'a node not existent'
        nodePamConfigStatusMock.isEnabled() >> true
        def selectedNEs = createGetNeAccountJAXB(new ArrayList(Arrays.asList("FakeNode")), new ArrayList(), new ArrayList())
        when: 'execute the rest call to retrieveNeAccounts'
        def responseRetrieve = objUnderTest.retrieveNeAccounts(Arrays.asList("1"),Arrays.asList(NetworkElementStatus.MANAGED),selectedNEs)
        def responseEntity = (List<NPamNEAccountResponseJAXB>)responseRetrieve.getEntity()
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_NOT_EXISTING_NETWORK_ELEMENT.message)
    }

    def 'run retrieve details for networkElementAccount for an empty neInfo'() {
        given: 'an empty NEInfo'
        nodePamConfigStatusMock.isEnabled() >> true
        def selectedNEs = createGetNeAccountJAXB(new ArrayList(), new ArrayList(), new ArrayList())
        when: 'execute the rest call to retrieveNeAccounts'
        def responseRetrieve = objUnderTest.retrieveNeAccounts(Arrays.asList("1"),new ArrayList(),selectedNEs)
        def responseEntity = (List<NPamNEAccountResponseJAXB>)responseRetrieve.getEntity()
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().contains(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES.getMessage())
        ex.getInternalCode().getHttpStatusCode().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES.httpStatusCode)
    }

    def 'run retrieve details for networkElementAccount with id=3'() {
        given: 'nodes are present , no filters applied'
        nodePamConfigStatusMock.isEnabled() >> true
        def selectedNEs = createGetNeAccountJAXB(new ArrayList(Arrays.asList("TestNode1","TestNode2","TestNode3")), new ArrayList(), new ArrayList())
        when: 'execute the rest call to retrieveNeAccounts'
        def responseRetrieve = objUnderTest.retrieveNeAccounts(Arrays.asList("3"),new ArrayList(),selectedNEs)
        def responseEntity = (List<NPamNEAccountResponseJAXB>)responseRetrieve.getEntity()
        then:'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().contains(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MUID.getMessage())
        ex.getInternalCode().getHttpStatusCode().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MUID.httpStatusCode)
    }


    def 'run export details for networkElementAccount filtering on id=1, without filters on neStatus'() {
        given: 'nodes are present , one has a NeAccount associated'
        nodePamConfigStatusMock.isEnabled() >> true
        def selectedNEs = createNeInfo(new ArrayList(Arrays.asList("TestNode1","TestNode2")), new ArrayList(), new ArrayList())
        def exportData = new NPAMExportJAXB()
        exportData.setEncryptionKey("keyForTest")
        exportData.setSelectedNEs(selectedNEs)

        when: 'execute the rest call to exportNeAccounts'
        def responseRetrieve = objUnderTest.exportNeAccounts(new ArrayList(),new ArrayList(),exportData)
        def responseEntity = (List<NPamNEAccountResponseJAXB>)responseRetrieve.getEntity()
        then:'all info are retrieved '
        responseRetrieve.getStatus() == Response.Status.OK.statusCode
    }

    def 'run export details for all filtering on id=1, with filters on neStatus'() {
        given: 'nodes are present , one has a NeAccount associated'
        nodePamConfigStatusMock.isEnabled() >> true
        def selectedNEs = createNeInfo(new ArrayList(Arrays.asList("TestNode1","TestNode2")), new ArrayList(), new ArrayList())
        def exportData = new NPAMExportJAXB()
        exportData.setEncryptionKey("keyForTest")

        when: 'execute the rest call to exportNeAccounts'
        def responseRetrieve = objUnderTest.exportNeAccounts(new ArrayList(Arrays.asList("1")),
                new ArrayList(Arrays.asList(NetworkElementStatus.MANAGED)), exportData)
        def responseEntity = (List<NPamNEAccountResponseJAXB>)responseRetrieve.getEntity()
        then:'all info are retrieved '
        responseRetrieve.getStatus() == Response.Status.OK.statusCode
    }

    def 'run export details with wrong neInfo'() {
        given: 'Npam Config is enabled'
        nodePamConfigStatusMock.isEnabled() >> true
        NEInfo neInfo = new NEInfo();
        neInfo.setSavedSearchIds(new ArrayList())
        neInfo.setCollectionNames(new ArrayList())
        def exportData = new NPAMExportJAXB()
        exportData.setEncryptionKey("keyForTest")
        exportData.setSelectedNEs(neInfo)
        when: 'execute the rest call to exportNeAccounts'
        def responseRetrieve = objUnderTest.exportNeAccounts(new ArrayList(Arrays.asList("1")),
                new ArrayList(Arrays.asList(NetworkElementStatus.MANAGED)), exportData)
        then:
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().contains(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES.getMessage())
    }
}
