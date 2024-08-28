package com.ericsson.oss.services.security.npam.ejb.rest


import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_NAME
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_VALUE
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MUID_ONE
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MUID_TWO
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.INTERNAL_SERVER_ERROR_DECRYPT_PASSWD
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.INTERNAL_SERVER_ERROR_DECRYPT_USER

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.sdk.context.ContextService
import com.ericsson.oss.services.security.npam.api.constants.ModelsConstants
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage
import com.ericsson.oss.services.security.npam.api.job.modelentities.NEInfo
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEAccount
import com.ericsson.oss.services.security.npam.api.job.modelentities.NetworkElementStatus
import com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccountUpdateStatus
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.security.npam.ejb.dao.DpsWriteOperations
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus
import com.ericsson.oss.services.security.npam.ejb.neaccount.executor.NeAccountGetServiceImpl
import com.ericsson.oss.services.security.npam.ejb.services.NodePamEncryptionManager
import com.ericsson.oss.services.security.npam.ejb.utility.NetworkUtil
import com.ericsson.oss.services.security.npam.ejb.utility.TbacEvaluation

import spock.lang.Unroll

class NeAccountServiceImplSpec  extends BaseSetupForTestSpecs {

    @ObjectUnderTest
    private NeAccountServiceImpl objUnderTest

    @Inject
    ContextService cxtMock

    @MockedImplementation
    TbacEvaluation tbacEvaluationMock

    @MockedImplementation
    NodePamEncryptionManager criptoMock

    @MockedImplementation
    NodePamConfigStatus nodePamConfigStatusMock

    @MockedImplementation
    NetworkUtil networkUtilMock

    @ImplementationClasses
    def classes = [
        DpsWriteOperations,
        NeAccountServiceImpl,
        NeAccountGetServiceImpl,
        StubCryptoUtilImpl
    ]

    String header = "Salted__#NetworkElementName;UserName;Password\n";

    def setup() {
        runtimeDps.withTransactionBoundaries()
        cxtMock.getContextValue("X-Tor-UserID") >> "administrator"
        criptoMock.encryptPassword(_) >> {
            return "encryptedpassword"
        }
    }

    def createConfigMo(final String configStatus) {
        final List<Map<String, String>> nPamConfigProperties = new ArrayList<>()

        final Map<String, String> property = new HashMap<>();
        property.put(PROPERTY_NAME, "npam");
        property.put(PROPERTY_VALUE, configStatus);
        nPamConfigProperties.add(property)
        runtimeDps.addPersistenceObject().namespace(ModelsConstants.NAMESPACE)
                .version(ModelsConstants.VERSION)
                .type(ModelsConstants.NPAM_CONFIG)
                .addAttribute("nPamConfigProperties",nPamConfigProperties)
                .create()
    }

    def createNetworkElementMo(final String neName) {
        runtimeDps.addManagedObject().namespace(ModelsConstants.NAMESPACE)
                .withFdn("NetworkElement=TestNode1")
                .version(ModelsConstants.VERSION)
                .type("NetworkElement")
                .addAttribute("name", neName)
                .build()
    }

    def 'retrieve NEAccounts details for a target'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        addNodeTree(null, "TestNode1", "SYNCHRONIZED", true)
        addNeAccount("TestNode1", 1, "CONFIGURED")
        when: 'retrieve the NEAccount details'
        def result = objUnderTest.getNEAccounts("TestNode1")
        then:
        result.get(0).currentPswd == "passwordEncrypted"
    }

    def 'get all nes'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        networkUtilMock.getAllNetworkElementFromNeInfo(_,_,_) >> {
            return new HashSet<>(Arrays.asList("TestNode1"))
        }
        def neInfo = new NEInfo()
        neInfo.setNeNames(new ArrayList<>(Arrays.asList("TestNode1")))
        neInfo.setSavedSearchIds(new ArrayList<String> ())
        neInfo.setCollectionNames(new ArrayList<String> ())
        when: 'retrieve the NEAccount details'
        def result = objUnderTest.getAllNes(neInfo)
        then:
        result.size() > 0
    }

    def 'retrieve NEAccounts with npam disabled'() {
        given: 'NPAM_CONFIG disabled'
        nodePamConfigStatusMock.isEnabled() >> false

        when: 'retrieve the NEAccount details'
        def result = objUnderTest.getNEAccounts("TestNode1")
        then:
        thrown(NPAMRestErrorException)
    }

    def 'retrieve NEAccounts details for a target without NeAccount'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        networkUtilMock.getAllNetworkElementFromNeInfo(_,_,_) >> {
            return new HashSet<>()
        }
        criptoMock.decryptPassword(_)  >> {
            return "decryptedpassword"
        }
        addNodeTree(null, "TestNode1", "SYNCHRONIZED", true)
        when: 'retrieve NEAccount'

        def result = objUnderTest.getNEAccounts("TestNode1")
        then:'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().contains(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_NEACCOUNT_NOT_EXISTS.getMessage())
    }

    def 'retrieve password in plain text'() {
        given: 'an encrypted password'
        criptoMock.decryptPassword(_)  >> {
            return "decryptedpassword"
        }
        when: 'retrieve password in plain text'
        def result = objUnderTest.getPwdInPlainText("encryptedpassword")
        then:
        result == "decryptedpassword"
    }

    def 'retrieve password in plain text with exception'() {
        given: 'an encrypted password'
        criptoMock.decryptPassword(_)  >> {
            throw new UnsupportedEncodingException()
        }
        when: 'retrieve password in plain text with exception'
        def result = objUnderTest.getPwdInPlainText("encryptedpassword")
        then:
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().contains(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_DECRYPT.getMessage())
        ex.getInternalCode().getErrorDetails().equals(INTERNAL_SERVER_ERROR_DECRYPT_PASSWD)
    }

    @Unroll
    def 'createSelectedNeParameter throw exception '() {
        given: 'create an neInfo'
        def neInfoForTest = createSelectedNeParameter(neInfoExist, neEmpty, collectionEmpty, saveSearchEmpty)
        when: 'retrieve password in plain text with exception'
        objUnderTest.validateSelectNeInfoParameter(neInfoForTest)
        then:
        thrown(NPAMRestErrorException)
        where:
        neInfoExist |  neEmpty | collectionEmpty  |  saveSearchEmpty || _
        true    |  true    |   true           |    true          || _
        false   |  false   |   true           |    true          || _
    }

    @Unroll
    def 'createSelectedNeParameter NOT throw exception '() {
        given: 'create an neInfo'
        def neInfoForTest = createSelectedNeParameter(true, neEmpty, collectionEmpty, saveSearchEmpty)
        when: 'retrieve password in plain text with exception'
        objUnderTest.validateSelectNeInfoParameter(neInfoForTest)
        then:
        noExceptionThrown()
        where:
        neEmpty | collectionEmpty  |  saveSearchEmpty  || _
        true    |   true           |    false          || _
        true    |   false          |    true           || _
        false   |   true           |     true          || _
    }

    def 'retrieve remote management value from radio node'() {
        given: 'an NE with NeAccount and remoteManagement true'
        addNodeTree(null, "node01", "SYNCHRONIZED", true)
        networkUtilMock.getAllNetworkElementFromNeInfo(_,_,_) >> {
            return new HashSet<>(Arrays.asList("node01"))
        }
        def neInfo = new NEInfo()
        neInfo.setNeNames(new ArrayList<>(Arrays.asList("node01")))
        neInfo.setSavedSearchIds(new ArrayList<String> ())
        neInfo.setCollectionNames(new ArrayList<String> ())
        when: 'retrieve neAccount Response for this NE'
        def result = objUnderTest.getAllNEAccountStatusElements(neInfo, new ArrayList<String> (),
                new ArrayList<NetworkElementStatus> ())
        then:
        result.get(0).getNeNpamStatus() == NetworkElementStatus.MANAGED
    }

    def 'ask for neAccount with wrong muid'() {
        given: 'NPamConfig set true'
        nodePamConfigStatusMock.isEnabled() >> true
        def neInfo = new NEInfo()
        neInfo.setNeNames(new ArrayList<>(Arrays.asList("node01")))
        neInfo.setSavedSearchIds(new ArrayList<String> ())
        neInfo.setCollectionNames(new ArrayList<String> ())
        when: 'retrieve neAccount Response for this NE'
        def result = objUnderTest.getNEAccountStatusList(neInfo, new ArrayList<String> (Arrays.asList("3")),
                new ArrayList<NetworkElementStatus> ())
        then:
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().contains(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MUID.getMessage())
    }

    def 'retrieve remote management value from No radio node'() {
        given: 'an NE with NeAccount and remoteManagement true'
        addNodeNoRadioNode("node01")
        networkUtilMock.getAllNetworkElementFromNeInfo(_,_,_) >> {
            return new HashSet<>(Arrays.asList("node01"))
        }
        def neInfo = new NEInfo()
        neInfo.setNeNames(new ArrayList<>(Arrays.asList("node01")))
        neInfo.setSavedSearchIds(new ArrayList<String> ())
        neInfo.setCollectionNames(new ArrayList<String> ())
        when: 'retrieve neAccount Response for this NE'
        def result = objUnderTest.getAllNEAccountStatusElements(neInfo, new ArrayList<String> (),
                new ArrayList<NetworkElementStatus> ())
        then:
        result.get(0).getNeNpamStatus() == NetworkElementStatus.NOT_SUPPORTED
    }

    def 'export credentials for selected NEs'() {
        given: 'list of nodes'

        addNodeTree(null, "node01", "SYNCHRONIZED", true)
        addNeAccount("node01", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())

        networkUtilMock.getAllNetworkElementFromNeInfo(_,_,_) >> {
            return new HashSet<>(Arrays.asList("node01"))
        }
        tbacEvaluationMock.getNodePermission(*_) >> true
        nodePamConfigStatusMock.isEnabled() >> true
        def neInfo = new NEInfo()
        neInfo.setNeNames(new ArrayList<>(Arrays.asList("node01")))
        neInfo.setSavedSearchIds(new ArrayList<String> ())
        neInfo.setCollectionNames(new ArrayList<String> ())
        when: 'retrieve the credentials for this NE to export'
        def result = objUnderTest.exportNeAccount(neInfo, new ArrayList<String> (Arrays.asList("1")),
                new ArrayList<NetworkElementStatus> (Arrays.asList(NetworkElementStatus.MANAGED)), "myNewKey")
        then:
        result.length > header.length()
    }

    def 'export credentials for selected NEs with invalid username or password'() {
        given: 'node with invalid username or password'
        addNodeTree(null, "node01", "SYNCHRONIZED", true)
        def po = addNeAccount("node01", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
        po.setAttribute("currentUsername", null)
        po.setAttribute("currentPassword", null)

        networkUtilMock.getAllNetworkElementFromNeInfo(_,_,_) >> {
            return new HashSet<>(Arrays.asList("node01"))
        }
        tbacEvaluationMock.getNodePermission(*_) >> true
        nodePamConfigStatusMock.isEnabled() >> true
        def neInfo = new NEInfo()
        neInfo.setNeNames(new ArrayList<>(Arrays.asList("node01")))
        neInfo.setSavedSearchIds(new ArrayList<String> ())
        neInfo.setCollectionNames(new ArrayList<String> ())
        when: 'retrieve the credentials for this NE to export'
        def result = objUnderTest.exportNeAccount(neInfo, new ArrayList<String> (Arrays.asList("1")),
                new ArrayList<NetworkElementStatus> (Arrays.asList(NetworkElementStatus.MANAGED)), "myNewKey")
        then:
        result.length == header.length()
    }

    def 'export credentials for all MANAGED NEs'() {
        given: 'list of nodes'
        addNodeTree(null, "node01", "SYNCHRONIZED", true)
        addNeAccount("node01", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
        addNodeTree(null, "node02", "SYNCHRONIZED", false)
        addNeAccount("node02", 1, NetworkElementAccountUpdateStatus.DETACHED.name())
        networkUtilMock.getAllNetworkElementFromNeInfo(_,_,_) >> {
            return new HashSet<>(Arrays.asList("node01", "node02"))
        }
        tbacEvaluationMock.getNodePermission(*_) >> true
        nodePamConfigStatusMock.isEnabled() >> true
        when: 'retrieve the credentials for this NE to export'
        def result = objUnderTest.exportNeAccount(null, new ArrayList<String> (Arrays.asList("1")),
                new ArrayList<NetworkElementStatus> (Arrays.asList(NetworkElementStatus.MANAGED)), "myNewKey")
        then:
        result.length > header.length()
    }

    def 'export credentials for all NOT MANAGED NEs'() {
        given: 'list of nodes'
        addNodeTree(null, "node02", "SYNCHRONIZED", false)
        addNeAccount("node02", 1, NetworkElementAccountUpdateStatus.DETACHED.name())
        networkUtilMock.getAllNetworkElementFromNeInfo(_,_,_) >> {
            return new HashSet<>(Arrays.asList("node02"))
        }
        tbacEvaluationMock.getNodePermission(*_) >> true
        nodePamConfigStatusMock.isEnabled() >> true
        when: 'retrieve the credentials for this NE to export'
        def result = objUnderTest.exportNeAccount(null, new ArrayList<String> (Arrays.asList("1")),
                new ArrayList<NetworkElementStatus> (Arrays.asList(NetworkElementStatus.NOT_MANAGED)), "myNewKey")
        then:
        result.length > header.length()
    }

    def 'export  exception thrown'() {
        given: 'list of nodes'
        addNodeTree(null, "node01", "SYNCHRONIZED", true)
        addNeAccount("node01", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())

        networkUtilMock.getAllNEAccountsById(_) >> {
            return new HashSet<>(Arrays.asList("node01"))
        }
        criptoMock.decryptPassword(_)  >> {
            throw new UnsupportedEncodingException()
        }
        tbacEvaluationMock.getNodePermission(*_) >> true
        nodePamConfigStatusMock.isEnabled() >> true

        when: 'retrieve the credentials for this NE to export'

        def result = objUnderTest.exportNeAccount(null, new ArrayList<String> (Arrays.asList("1")),
                new ArrayList<NetworkElementStatus> (Arrays.asList(NetworkElementStatus.MANAGED)), "myNewKey")
        then:
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().contains(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_DECRYPT.getMessage())
        ex.getInternalCode().getErrorDetails().equals(INTERNAL_SERVER_ERROR_DECRYPT_USER)
    }

    def 'export selected nes exception thrown'() {
        given: 'list of nodes'
        addNodeTree(null, "node01", "SYNCHRONIZED", true)
        addNeAccount("node01", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
        def neInfo = new NEInfo()
        neInfo.setNeNames(new ArrayList<String> (Arrays.asList("node01")))
        neInfo.setSavedSearchIds(new ArrayList<String> ())
        neInfo.setCollectionNames(new ArrayList<String> ())
        networkUtilMock.getAllNetworkElementFromNeInfo(_,_,_) >> {
            return new HashSet<>(Arrays.asList("node01"))
        }
        criptoMock.decryptPassword("userNameEncrypted")  >> {
            throw new UnsupportedEncodingException()
        }
        tbacEvaluationMock.getNodePermission(*_) >> true
        nodePamConfigStatusMock.isEnabled() >> true

        when: 'retrieve the credentials for this NE to export'
        def result = objUnderTest.exportNeAccount(neInfo, new ArrayList<String> (Arrays.asList("1")),
                new ArrayList<NetworkElementStatus> (Arrays.asList(NetworkElementStatus.MANAGED)), "myNewKey")
        then:
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().contains(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_DECRYPT.getMessage())
        ex.getInternalCode().getErrorDetails().equals(INTERNAL_SERVER_ERROR_DECRYPT_USER)
    }

    def 'export  exception thrown for invalid Status'() {
        given: 'list of nodes'
        addNodeTree(null, "node01", "SYNCHRONIZED", true)
        addNeAccount("node01", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())

        criptoMock.decryptPassword(_)  >> {
            throw new UnsupportedEncodingException()
        }
        tbacEvaluationMock.getNodePermission(*_) >> true
        nodePamConfigStatusMock.isEnabled() >> true

        when: 'retrieve the credentials for this NE to export'
        def result = objUnderTest.exportNeAccount(null, new ArrayList<String> (Arrays.asList("1")),
                new ArrayList<NetworkElementStatus> (Arrays.asList(NetworkElementStatus.NOT_SUPPORTED)), "myNewKey")
        then:
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().contains(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_NE_NPAM_STATUS.getMessage())
    }

    def 'export for empty key'() {
        given: 'list of nodes'
        addNodeTree(null, "node02", "SYNCHRONIZED", false)
        addNeAccount("node02", 1, NetworkElementAccountUpdateStatus.DETACHED.name())
        networkUtilMock.getAllNetworkElementFromNeInfo(_,_,_) >> {
            return new HashSet<>(Arrays.asList("node02"))
        }
        tbacEvaluationMock.getNodePermission(*_) >> true
        nodePamConfigStatusMock.isEnabled() >> true
        when: 'retrieve the credentials for this NE to export'
        def result = objUnderTest.exportNeAccount(null, new ArrayList<String> (Arrays.asList("1")),
                new ArrayList<NetworkElementStatus> (Arrays.asList(NetworkElementStatus.NOT_MANAGED)), null)
        then:
        result.length > header.length()
    }

    def 'export  exception thrown for low key'() {
        given: 'npam config true'
        tbacEvaluationMock.getNodePermission(*_) >> true
        nodePamConfigStatusMock.isEnabled() >> true

        when: 'retrieve the credentials for this NE to export'
        def result = objUnderTest.exportNeAccount(null, new ArrayList<String> (Arrays.asList("1")),
                new ArrayList<NetworkElementStatus> (Arrays.asList(NetworkElementStatus.MANAGED)), "myKey")
        then:
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().contains(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_ENCRYPTION_KEY.getMessage())
    }

    def 'export  exception thrown for long key'() {
        given: 'npam config true'
        tbacEvaluationMock.getNodePermission(*_) >> true
        nodePamConfigStatusMock.isEnabled() >> true

        when: 'retrieve the credentials for this NE to export'
        def result = objUnderTest.exportNeAccount(null, new ArrayList<String> (Arrays.asList("1")),
                new ArrayList<NetworkElementStatus> (Arrays.asList(NetworkElementStatus.MANAGED)), "myKeymyKeymyKeymyKeymyKeymyKeymyKey")
        then:
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().contains(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_ENCRYPTION_KEY.getMessage())
    }

    def 'export  exception thrown for invalid key'() {
        given: 'npam config true'
        tbacEvaluationMock.getNodePermission(*_) >> true
        nodePamConfigStatusMock.isEnabled() >> true

        when: 'retrieve the credentials for this NE to export'
        def result = objUnderTest.exportNeAccount(null, new ArrayList<String> (Arrays.asList("1")),
                new ArrayList<NetworkElementStatus> (Arrays.asList(NetworkElementStatus.MANAGED)), "myKey;")
        then:
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().contains(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_ENCRYPTION_KEY.getMessage())
    }

    def 'export  exception thrown for invalid NEInfo'() {
        given: 'list of nodes'
        addNodeTree(null, "node01", "SYNCHRONIZED", true)
        addNeAccount("node01", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())

        criptoMock.decryptPassword(_)  >> {
            throw new UnsupportedEncodingException()
        }
        tbacEvaluationMock.getNodePermission(*_) >> true
        nodePamConfigStatusMock.isEnabled() >> true

        def neInfo = new NEInfo()
        neInfo.setNeNames(new ArrayList<String> ())
        neInfo.setSavedSearchIds(new ArrayList<String> ())
        neInfo.setCollectionNames(new ArrayList<String> ())
        when: 'retrieve the credentials for this NE to export'
        def result = objUnderTest.exportNeAccount(neInfo, new ArrayList<String> (Arrays.asList("1")),
                new ArrayList<NetworkElementStatus> (Arrays.asList(NetworkElementStatus.MANAGED)), "myNewKey")
        then:
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().contains(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES.getMessage())
        ex.getInternalCode().getHttpStatusCode().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES.httpStatusCode)
    }

    def 'check IP'() {
        given: 'a node'
        addNodeTree(null, "node01", "SYNCHRONIZED", true)
        addNeAccount("node01", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
        tbacEvaluationMock.getNodePermission(*_) >> true
        when: 'retrieve IP for this NE '
        def result = objUnderTest.retrieveIpAddress("node01")
        then:
        result.length() == 0
    }

    def 'export requested for invalid ne'() {
        given: 'list of nodes'
        addNodeTree(null, "node01", "SYNCHRONIZED", true)
        addNeAccount("node01", 1, NetworkElementAccountUpdateStatus.CONFIGURED.name())
        tbacEvaluationMock.getNodePermission(*_) >> false
        nodePamConfigStatusMock.isEnabled() >> true
        when: 'retrieve the credentials for this NE to export'
        def result = objUnderTest.exportNeAccount(null, new ArrayList<String> (Arrays.asList("1")),
                new ArrayList<NetworkElementStatus> (Arrays.asList(NetworkElementStatus.MANAGED)), "myNewKey")
        then:
        result.length == header.length()
    }

    def 'export with npam disabled'() {
        given: 'npam config disabled'
        tbacEvaluationMock.getNodePermission(*_) >> false
        nodePamConfigStatusMock.isEnabled() >> false

        when: 'retrieve the credentials to export'
        def result = objUnderTest.exportNeAccount(null, new ArrayList<String> (Arrays.asList("1")),
                new ArrayList<NetworkElementStatus> (Arrays.asList(NetworkElementStatus.MANAGED)), "myNewKey")
        then:
        result.length == header.length()
    }

    @Unroll
    def 'test isValidCurrentUserNameAndCurrentPassword #currentUserName #currentPassword'(currentUserName, currentPassword, isValid) {
        given:
        final NPamNEAccount neAccount = new NPamNEAccount()
        neAccount.setCurrentUser(currentUserName)
        neAccount.setCurrentPswd(currentPassword)

        when: 'isValidCurrentUserNameAndCurrentPassword'
        def result = objUnderTest.isValidCurrentUserNameAndCurrentPassword(neAccount)
        then:
        result == isValid
        where:
        currentUserName | currentPassword || isValid
        null            | "password1"     || false
        "user1"         | null            || false
        null            | null            || false
        ""              | "password1"     || false
        "user1"         | ""              || false
        ""             | null            || false
        null           | ""              || false
        ""             | ""              || false
        "user1"        | ""              || false
        "user1"        | "password1"      || true
    }


    @Unroll
    def 'test toBeAddedNEAccount #neStatus #remoteManagement'(neStatus, remoteManagement, expectedToBeAdded) {
        given:
        final List<NetworkElementStatus> neStatusList = new ArrayList<>()
        if (neStatus != null) {
            neStatusList.add(neStatus)
        }

        when: 'toBeAddedNEAccount'
        def result = objUnderTest.toBeAddedNEAccount(neStatusList, remoteManagement)

        then:
        result == expectedToBeAdded
        where:
        neStatus                           | remoteManagement  || expectedToBeAdded
        null                               | Boolean.TRUE      || true
        null                               | Boolean.FALSE     || true
        null                               | null              || true

        NetworkElementStatus.NOT_SUPPORTED | Boolean.TRUE      || false
        NetworkElementStatus.NOT_SUPPORTED | Boolean.FALSE     || false
        NetworkElementStatus.NOT_SUPPORTED | null              || true

        NetworkElementStatus.MANAGED       | Boolean.TRUE      || true
        NetworkElementStatus.MANAGED       | Boolean.FALSE     || false
        NetworkElementStatus.MANAGED       | null              || false

        NetworkElementStatus.NOT_MANAGED   | Boolean.TRUE      || false
        NetworkElementStatus.NOT_MANAGED   | Boolean.FALSE     || true
        NetworkElementStatus.NOT_MANAGED   | null              || false
    }

    @Unroll
    def 'errorDetails update'() {
        given: 'NPamNEAccount'
        addNodeTree(null, "node01", "SYNCHRONIZED", true)

        final NPamNEAccount neAccount = new NPamNEAccount()
        neAccount.setNetworkElementAccountId(neAccountId)
        neAccount.setStatus(neUpdateStatus.name())

        if (!isErrorDetailsEmpty) {
            neAccount.setErrorDetails("some error")
        } else {
            neAccount.setErrorDetails(null)
        }

        tbacEvaluationMock.getNodePermission(*_) >> true
        nodePamConfigStatusMock.isEnabled() >> isFeatureEnabled
        nodePamConfigStatusMock.isCbrsDomainEnabled() >> isCbrsEnabled

        when: 'updates error details'
        objUnderTest.updateNeAccountDetailsIfNeeded(neAccount, remoteManagement)
        then:
            if (neAccount.getErrorDetails() != null) {
                neAccount.getErrorDetails().contains(messageLabel)
            }

        where:
        neAccountId   |      neUpdateStatus                                | isErrorDetailsEmpty |   isFeatureEnabled         | isCbrsEnabled      | remoteManagement  || messageLabel
        MUID_ONE      |      NetworkElementAccountUpdateStatus.DETACHED    |    Boolean.TRUE     |    Boolean.TRUE            | Boolean.TRUE       | Boolean.TRUE      || "Warning"
        MUID_ONE      |      NetworkElementAccountUpdateStatus.CONFIGURED  |    Boolean.TRUE     |    Boolean.TRUE            | Boolean.TRUE       | Boolean.TRUE      ||  _
        MUID_ONE      |      NetworkElementAccountUpdateStatus.DETACHED    |    Boolean.TRUE     |    Boolean.FALSE           | Boolean.TRUE       | Boolean.TRUE      ||  _
        MUID_ONE      |      NetworkElementAccountUpdateStatus.CONFIGURED  |    Boolean.TRUE     |    Boolean.TRUE            | Boolean.TRUE       | Boolean.FALSE     || "Warning"
        MUID_ONE      |      NetworkElementAccountUpdateStatus.DETACHED    |    Boolean.TRUE     |    Boolean.TRUE            | Boolean.TRUE       | Boolean.FALSE     ||  _
        MUID_TWO      |      NetworkElementAccountUpdateStatus.DETACHED    |    Boolean.TRUE     |    Boolean.TRUE            | Boolean.TRUE       | Boolean.TRUE      || "Warning"
        MUID_TWO      |      NetworkElementAccountUpdateStatus.CONFIGURED  |    Boolean.TRUE     |    Boolean.TRUE            | Boolean.TRUE       | Boolean.TRUE      ||  _
        MUID_TWO      |      NetworkElementAccountUpdateStatus.CONFIGURED  |    Boolean.TRUE     |    Boolean.TRUE            | Boolean.TRUE       | Boolean.FALSE     || "Warning"
        MUID_TWO      |      NetworkElementAccountUpdateStatus.CONFIGURED  |    Boolean.TRUE     |    Boolean.TRUE            | Boolean.TRUE       | Boolean.FALSE     || "Warning"
        MUID_TWO      |      NetworkElementAccountUpdateStatus.DETACHED    |    Boolean.TRUE     |    Boolean.TRUE            | Boolean.TRUE       | Boolean.FALSE     ||  _
        MUID_TWO      |      NetworkElementAccountUpdateStatus.DETACHED    |    Boolean.TRUE     |    Boolean.TRUE            | Boolean.FALSE      | Boolean.TRUE      || "Warning"
        MUID_TWO      |      NetworkElementAccountUpdateStatus.CONFIGURED  |    Boolean.TRUE     |    Boolean.TRUE            | Boolean.FALSE      | Boolean.TRUE      || "Warning"
        MUID_TWO      |      NetworkElementAccountUpdateStatus.DETACHED    |    Boolean.TRUE     |    Boolean.TRUE            | Boolean.FALSE      | Boolean.FALSE     || "Warning"
        MUID_TWO      |      NetworkElementAccountUpdateStatus.CONFIGURED  |    Boolean.TRUE     |    Boolean.TRUE            | Boolean.FALSE      | Boolean.FALSE     || "Warning"
        MUID_ONE      |      NetworkElementAccountUpdateStatus.FAILED      |    Boolean.FALSE    |    Boolean.TRUE            | Boolean.TRUE       | Boolean.TRUE      || "Error"
        MUID_TWO      |      NetworkElementAccountUpdateStatus.FAILED      |    Boolean.FALSE    |    Boolean.TRUE            | Boolean.TRUE       | Boolean.TRUE      || "Error"

    }

    private NEInfo createSelectedNeParameter(final boolean neInfoExist, final boolean neEmpty, final boolean collectionEmpty, final boolean saveSearchEmpty) {
        if (neInfoExist) {
            final NEInfo neInfo = new NEInfo();
            neInfo.neNames =  new ArrayList<>();
            neInfo.collectionNames = new ArrayList<>();
            neInfo.savedSearchIds = new ArrayList<>();
            if (!neEmpty) {
                neInfo.neNames.add("fakeNe")
            }
            if (!collectionEmpty) {
                neInfo.collectionNames.add("fakeNe")
            }
            if (!saveSearchEmpty) {
                neInfo.savedSearchIds.add("fakeNe")
            }
            return neInfo;
        }
        return null;
    }
}




