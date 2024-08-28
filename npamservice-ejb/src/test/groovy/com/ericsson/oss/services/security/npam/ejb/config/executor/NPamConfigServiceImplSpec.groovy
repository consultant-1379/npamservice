package com.ericsson.oss.services.security.npam.ejb.config.executor

import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_NAME
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_VALUE
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_CBRS_DOMAIN
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_EMERGENCY_USER
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_NPAM
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_PARAM_DISABLED
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_PARAM_ENABLED
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_PROPERTIES
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_RESTRICT_MAINTENANCE_USER

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfig
import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfigPropertiesEnum
import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfigProperty
import com.ericsson.oss.services.security.npam.api.constants.ModelsConstants
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus
import com.ericsson.oss.services.security.npam.ejb.listener.MembershipListenerInterface
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.itpf.sdk.recording.classic.SystemRecorderBean

import spock.lang.Unroll
class NPamConfigServiceImplSpec  extends BaseSetupForTestSpecs {

    @ObjectUnderTest
    private NPamConfigServiceImpl objUnderTest

    @MockedImplementation
    private NodePamConfigStatus nodePamConfigStatus

    @MockedImplementation
    MembershipListenerInterface membershipListenerInterfaceMock

    @MockedImplementation
    SystemRecorder mockSystemRecorder

    @ImplementationClasses
    def classes = [SystemRecorderBean]

    def setup() {
        runtimeDps.withTransactionBoundaries()
    }

    /*
     getNPamConfig method
     * */
    @Unroll
    def 'getNPamConfig with napmParameterValue=#napmParameterValue cbrsParameterValue=#cbrsParameterValue'(npamParameterValue, cbrsParameterValue,
            euParameterValue, rmuParameterValue,
            expectedNapmParameterValue, expectedCbrsParameterValue,
            expectedEuParameterValue, expectedRmuParameterValue) {
        given: 'NPAM_CONFIG configured in dps'
        createConfigMo( npamParameterValue, cbrsParameterValue, euParameterValue, rmuParameterValue)
        when: ''
        def nPamConfig = objUnderTest.getNPamConfig()
        then:
        nPamConfig.get(0).getPoId() != -1
        NPamConfigProperty nPamConfigProperty1 = getNPamConfigProperty(nPamConfig.get(0), NPAM_CONFIG_NPAM)
        if (expectedNapmParameterValue != null) {
            assert nPamConfigProperty1.name == NPAM_CONFIG_NPAM
            assert nPamConfigProperty1.value == expectedNapmParameterValue
        } else {
            assert nPamConfigProperty1 == null
        }

        NPamConfigProperty nPamConfigProperty2 = getNPamConfigProperty(nPamConfig.get(0), NPAM_CONFIG_CBRS_DOMAIN)
        if (expectedCbrsParameterValue != null) {
            assert nPamConfigProperty2.name == NPAM_CONFIG_CBRS_DOMAIN
            assert nPamConfigProperty2.value == expectedCbrsParameterValue
        } else {
            assert nPamConfigProperty2 == null
        }

        NPamConfigProperty nPamConfigProperty3 = getNPamConfigProperty(nPamConfig.get(0), NPAM_CONFIG_EMERGENCY_USER)
        if (expectedEuParameterValue != null) {
            assert nPamConfigProperty3.name == NPAM_CONFIG_EMERGENCY_USER
            assert nPamConfigProperty3.value == expectedEuParameterValue
        } else {
            assert nPamConfigProperty3 == null
        }

        NPamConfigProperty nPamConfigProperty4 = getNPamConfigProperty(nPamConfig.get(0), NPAM_CONFIG_RESTRICT_MAINTENANCE_USER)
        if (expectedRmuParameterValue != null) {
            assert nPamConfigProperty4.name == NPAM_CONFIG_RESTRICT_MAINTENANCE_USER
            assert nPamConfigProperty4.value == expectedRmuParameterValue
        } else {
            assert nPamConfigProperty4 == null
        }

        where:
        npamParameterValue         | cbrsParameterValue          | euParameterValue            | rmuParameterValue          || expectedNapmParameterValue  || expectedCbrsParameterValue || expectedEuParameterValue   || expectedRmuParameterValue  |_
        // test napmParameterValue and cbrsParameterValue
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED   | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_ENABLED   || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED |_
        NPAM_CONFIG_PARAM_DISABLED | NPAM_CONFIG_PARAM_ENABLED   | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED  || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_ENABLED   || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED |_
        NPAM_CONFIG_PARAM_DISABLED | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED  || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED |_
        null                       | null                        | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || null                        || null                       || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED |_
        null                       | NPAM_CONFIG_PARAM_ENABLED   | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || null                        || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED |_
        NPAM_CONFIG_PARAM_ENABLED  | null                        | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_ENABLED   || null                       || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED |_
        "aa"                       | "bb"                        | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || "aa"                        || "bb"                       || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED |_

        // test euParameterValue and rmuParameterValue
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED   | NPAM_CONFIG_PARAM_ENABLED   | NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_ENABLED   || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_ENABLED  |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED   | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_ENABLED   || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_ENABLED  |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED   | NPAM_CONFIG_PARAM_ENABLED   | NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_ENABLED   || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_DISABLED |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED   | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_ENABLED   || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED   | null                        | null                       || NPAM_CONFIG_PARAM_ENABLED   || NPAM_CONFIG_PARAM_ENABLED  || null                       || null                       |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED   | null                        | NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_ENABLED   || NPAM_CONFIG_PARAM_ENABLED  || null                       || NPAM_CONFIG_PARAM_ENABLED  |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED   | NPAM_CONFIG_PARAM_ENABLED   | null                       || NPAM_CONFIG_PARAM_ENABLED   || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_ENABLED  || null                       |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED   | "cc"                        | "dd"                       || NPAM_CONFIG_PARAM_ENABLED   || NPAM_CONFIG_PARAM_ENABLED  || "cc"                       || "dd"                       |_
    }

    def 'getNPamConfig without entries'() {
        given: 'no NPAM_CONFIG configured in dps'

        when: ''
        def nPamConfig = objUnderTest.getNPamConfig()
        then:
        nPamConfig.get(0).getPoId() == -1
        nPamConfig.get(0).getnPamConfigProperties().size() == 0
    }

    /*
     getNPamConfigCached method
     * */
    @Unroll
    def 'getNPamConfigCached with napmEnabled=#napmEnabled cbrsParameterValue=#cbrsEnabled'(isEnabled, isCbrsDomainEnabled,
            isEmergencyUserEnabled, isRestrictMaintenanceUserEnabled,
            expectedNapmParameterValue, expectedCbrsParameterValue,
            expectedEuParameterValue, expectedRmuParameterValue) {
        given:
        if (isEnabled != null) {
            nodePamConfigStatus.isEnabled() >> isEnabled
        }

        if (isCbrsDomainEnabled != null) {
            nodePamConfigStatus.isCbrsDomainEnabled() >> isCbrsDomainEnabled
        }

        if (isEmergencyUserEnabled != null) {
            nodePamConfigStatus.isEmergencyUserEnabled() >> isEmergencyUserEnabled
        }

        if (isRestrictMaintenanceUserEnabled != null) {
            nodePamConfigStatus.isRestrictMaintenanceUserEnabled() >> isRestrictMaintenanceUserEnabled
        }
        when: ''
        def nPamConfig = objUnderTest.getNPamConfigCached()
        then:
        nPamConfig.getPoId() == -1
        NPamConfigProperty nPamConfigProperty1 = getNPamConfigProperty(nPamConfig, NPAM_CONFIG_NPAM)
        if (expectedNapmParameterValue != null) {
            assert nPamConfigProperty1.name == NPAM_CONFIG_NPAM
            assert nPamConfigProperty1.value == expectedNapmParameterValue
        } else {
            assert nPamConfigProperty1 == null
        }

        NPamConfigProperty nPamConfigProperty2 = getNPamConfigProperty(nPamConfig, NPAM_CONFIG_CBRS_DOMAIN)
        if (expectedCbrsParameterValue != null) {
            assert nPamConfigProperty2.name == NPAM_CONFIG_CBRS_DOMAIN
            assert nPamConfigProperty2.value == expectedCbrsParameterValue
        } else {
            assert nPamConfigProperty2 == null
        }

        NPamConfigProperty nPamConfigProperty3 = getNPamConfigProperty(nPamConfig, NPAM_CONFIG_EMERGENCY_USER)
        if (expectedEuParameterValue != null) {
            assert nPamConfigProperty3.name == NPAM_CONFIG_EMERGENCY_USER
            assert nPamConfigProperty3.value == expectedEuParameterValue
        } else {
            assert nPamConfigProperty3 == null
        }

        NPamConfigProperty nPamConfigProperty4 = getNPamConfigProperty(nPamConfig, NPAM_CONFIG_RESTRICT_MAINTENANCE_USER)
        if (expectedRmuParameterValue != null) {
            assert nPamConfigProperty4.name == NPAM_CONFIG_RESTRICT_MAINTENANCE_USER
            assert nPamConfigProperty4.value == expectedRmuParameterValue
        } else {
            assert nPamConfigProperty4 == null
        }

        where:
        isEnabled                  | isCbrsDomainEnabled         | isEmergencyUserEnabled | isRestrictMaintenanceUserEnabled || expectedNapmParameterValue  || expectedCbrsParameterValue || expectedEuParameterValue   || expectedRmuParameterValue  |_
        // test isEnabled and isCbrsDomainEnabled
        true                       | true                        | false                  | false                            || NPAM_CONFIG_PARAM_ENABLED   || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED |_
        true                       | false                       | false                  | false                            || NPAM_CONFIG_PARAM_ENABLED   || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED |_
        false                      | true                        | false                  | false                            || NPAM_CONFIG_PARAM_DISABLED  || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED |_
        false                      | false                       | false                  | false                            || NPAM_CONFIG_PARAM_DISABLED  || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED |_
        null                       | null                        | false                  | false                            || NPAM_CONFIG_PARAM_DISABLED  || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED |_

        // test isEmergencyUserEnabled and isRestrictMaintenanceUserEnabled
        true                       | true                        | true                   | true                             || NPAM_CONFIG_PARAM_ENABLED   || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_ENABLED  |_
        true                       | true                        | true                   | false                            || NPAM_CONFIG_PARAM_ENABLED   || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_DISABLED |_
        true                       | true                        | false                  | true                             || NPAM_CONFIG_PARAM_ENABLED   || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_ENABLED  |_
        true                       | true                        | false                  | false                            || NPAM_CONFIG_PARAM_ENABLED   || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED |_
        true                       | true                        | null                   | null                             || NPAM_CONFIG_PARAM_ENABLED   || NPAM_CONFIG_PARAM_ENABLED  || NPAM_CONFIG_PARAM_DISABLED || NPAM_CONFIG_PARAM_DISABLED |_
    }

    /*
     updateNPamConfig method
     * */
    def 'updateNPamConfig from npam=enabled to disabled'() {
        given: 'NPAM_CONFIG configured in dps'
        createConfigMo(NPAM_CONFIG_PARAM_ENABLED, NPAM_CONFIG_PARAM_DISABLED, NPAM_CONFIG_PARAM_DISABLED, NPAM_CONFIG_PARAM_DISABLED)
        // enabling CAL for coverage
        mockSystemRecorder.isCompactAuditEnabled() >> true
        when: ''
        objUnderTest.updateNPamConfig(createNPamConfig(NPAM_CONFIG_PARAM_DISABLED, null, null, null))
        def nPamConfig = objUnderTest.getNPamConfig()
        then:
        nPamConfig.get(0).getPoId() != -1
        nPamConfig.get(0).getnPamConfigProperties().size() == NPamConfigPropertiesEnum.npamConfigPropertiesList().size()
        NPamConfigProperty nPamConfigProperty = getNPamConfigProperty(nPamConfig .get(0), NPAM_CONFIG_NPAM)
        nPamConfigProperty.name == NPAM_CONFIG_NPAM
        nPamConfigProperty.value == NPAM_CONFIG_PARAM_DISABLED
    }

    def 'updateNPamConfig with disabled fetaure'() {
        given: 'NPAM_CONFIG configured in dps disabled'
        createConfigMo(NPAM_CONFIG_PARAM_DISABLED, NPAM_CONFIG_PARAM_DISABLED, null, null)
        when: ''
        objUnderTest.updateNPamConfig(createNPamConfig(null, NPAM_CONFIG_PARAM_ENABLED, null, null))
        def nPamConfig = objUnderTest.getNPamConfig()
        then:
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.FORBIDDEN_FEATURE_DISABLED.message)
    }

    def 'updateNPamConfig from cbrs=disabled to enabled'() {
        given: 'NPAM_CONFIG configured in dps'
        createConfigMo(NPAM_CONFIG_PARAM_ENABLED, NPAM_CONFIG_PARAM_DISABLED, NPAM_CONFIG_PARAM_DISABLED , NPAM_CONFIG_PARAM_DISABLED)
        when: ''
        objUnderTest.updateNPamConfig(createNPamConfig(null, NPAM_CONFIG_PARAM_ENABLED, null, null))
        def nPamConfig = objUnderTest.getNPamConfig()
        then:
        nPamConfig.get(0).getPoId() != -1
        nPamConfig.get(0).getnPamConfigProperties().size() == NPamConfigPropertiesEnum.npamConfigPropertiesList().size()
        NPamConfigProperty nPamConfigProperty = getNPamConfigProperty(nPamConfig.get(0), NPAM_CONFIG_CBRS_DOMAIN)
        nPamConfigProperty.name == NPAM_CONFIG_CBRS_DOMAIN
        nPamConfigProperty.value == NPAM_CONFIG_PARAM_ENABLED
    }

    def 'updateNPamConfig from npam=disabled to npam=enabled amd cbrs=enabled'() {
        given: 'NPAM_CONFIG configured in dps'
        createConfigMo(NPAM_CONFIG_PARAM_DISABLED, null, null, null)
        when: ''
        objUnderTest.updateNPamConfig(createNPamConfig(NPAM_CONFIG_PARAM_ENABLED, NPAM_CONFIG_PARAM_ENABLED, null, null))
        def nPamConfig = objUnderTest.getNPamConfig()
        then:
        nPamConfig.get(0).getPoId() != -1
        nPamConfig.get(0).getnPamConfigProperties().size() == 2
        NPamConfigProperty nPamConfigProperty1 = getNPamConfigProperty(nPamConfig.get(0), NPAM_CONFIG_NPAM)
        nPamConfigProperty1.name == NPAM_CONFIG_NPAM
        nPamConfigProperty1.value == NPAM_CONFIG_PARAM_ENABLED

        NPamConfigProperty nPamConfigProperty2 = getNPamConfigProperty(nPamConfig.get(0), NPAM_CONFIG_CBRS_DOMAIN)
        nPamConfigProperty2.name == NPAM_CONFIG_CBRS_DOMAIN
        nPamConfigProperty2.value == NPAM_CONFIG_PARAM_ENABLED
    }

    def 'updateNPamConfig from npam=disabled to npam=enabled and emergencyuser=enabled,restrictmaintenanceuser=disabled'() {
        given: 'NPAM_CONFIG configured in dps'
        createConfigMo(NPAM_CONFIG_PARAM_DISABLED, null, null, null)
        when: ''
        objUnderTest.updateNPamConfig(createNPamConfig(NPAM_CONFIG_PARAM_ENABLED, null, NPAM_CONFIG_PARAM_ENABLED, NPAM_CONFIG_PARAM_DISABLED))
        def nPamConfig = objUnderTest.getNPamConfig()
        then:
        nPamConfig.get(0).getPoId() != -1
        nPamConfig.get(0).getnPamConfigProperties().size() == 3
        NPamConfigProperty nPamConfigProperty1 = getNPamConfigProperty(nPamConfig.get(0), NPAM_CONFIG_NPAM)
        nPamConfigProperty1.name == NPAM_CONFIG_NPAM
        nPamConfigProperty1.value == NPAM_CONFIG_PARAM_ENABLED

        NPamConfigProperty nPamConfigProperty2 = getNPamConfigProperty(nPamConfig.get(0), NPAM_CONFIG_EMERGENCY_USER)
        nPamConfigProperty2.name == NPAM_CONFIG_EMERGENCY_USER
        nPamConfigProperty2.value == NPAM_CONFIG_PARAM_ENABLED

        NPamConfigProperty nPamConfigProperty3 = getNPamConfigProperty(nPamConfig.get(0), NPAM_CONFIG_RESTRICT_MAINTENANCE_USER)
        nPamConfigProperty3.name == NPAM_CONFIG_RESTRICT_MAINTENANCE_USER
        nPamConfigProperty3.value == NPAM_CONFIG_PARAM_DISABLED
    }

    /*
     createNPamConfig method
     * */

    def 'createNPamConfig when no entry already present and isMaster=false does not create a new one'() {
        given: 'isMaster=false'
        membershipListenerInterfaceMock.isMaster() >> false
        when: ''
        def poId = objUnderTest.createNPamConfig()
        def nPamConfig = objUnderTest.getNPamConfig()
        then: 'no entry created'
        poId == 0
        nPamConfig.get(0).getPoId() == -1
        nPamConfig.get(0).getnPamConfigProperties().size() == 0
    }


    def 'createNPamConfig when no entry already present and isMaster=true creates one entry (with default values)'() {
        given: 'isMaster=true'
        membershipListenerInterfaceMock.isMaster() >> true
        when: ''
        def poId = objUnderTest.createNPamConfig()
        def nPamConfig = objUnderTest.getNPamConfig()
        then: 'new entry created and poId returned'
        poId != 0
        nPamConfig.get(0).getPoId() != -1
        nPamConfig.get(0).getnPamConfigProperties().size() == NPamConfigPropertiesEnum.npamConfigPropertiesList().size()
        NPamConfigProperty nPamConfigProperty1 = getNPamConfigProperty(nPamConfig.get(0), NPAM_CONFIG_NPAM)
        nPamConfigProperty1.name == NPAM_CONFIG_NPAM
        nPamConfigProperty1.value == NPAM_CONFIG_PARAM_DISABLED

        NPamConfigProperty nPamConfigProperty2 = getNPamConfigProperty(nPamConfig.get(0), NPAM_CONFIG_CBRS_DOMAIN)
        nPamConfigProperty2.name == NPAM_CONFIG_CBRS_DOMAIN
        nPamConfigProperty2.value == NPAM_CONFIG_PARAM_DISABLED

        //        NPamConfigProperty nPamConfigProperty3 = getNPamConfigProperty(nPamConfig.get(0), NPAM_CONFIG_EMERGENCY_USER)
        //        nPamConfigProperty3.name == NPAM_CONFIG_EMERGENCY_USER
        //        nPamConfigProperty3.value == NPAM_CONFIG_PARAM_DISABLED
        //
        //        NPamConfigProperty nPamConfigProperty4 = getNPamConfigProperty(nPamConfig.get(0), NPAM_CONFIG_RESTRICT_MAINTENANCE_USER)
        //        nPamConfigProperty4.name == NPAM_CONFIG_RESTRICT_MAINTENANCE_USER
        //        nPamConfigProperty4.value == NPAM_CONFIG_PARAM_DISABLED
    }


    @Unroll
    def 'createNPamConfig when 1 entry already present and isMaster=#isMaster does not create a new one'(isMaster) {
        given: 'NPAM_CONFIG configured in dps'
        createConfigMo(NPAM_CONFIG_PARAM_ENABLED, null, null, null)
        and: 'isMaster=true'
        membershipListenerInterfaceMock.isMaster() >> isMaster
        when: ''
        def poId = objUnderTest.createNPamConfig()
        def nPamConfig = objUnderTest.getNPamConfig()
        then: 'same entry as before'
        assert isMaster ? poId != 0 : poId == 0
        nPamConfig.get(0).getPoId() != -1
        isMaster ? nPamConfig.get(0).getnPamConfigProperties().size() == NPamConfigPropertiesEnum.npamConfigPropertiesList().size() : nPamConfig.get(0).getnPamConfigProperties().size() == 1
        NPamConfigProperty nPamConfigProperty1 = getNPamConfigProperty(nPamConfig.get(0), NPAM_CONFIG_NPAM)
        nPamConfigProperty1.name == NPAM_CONFIG_NPAM
        nPamConfigProperty1.value == NPAM_CONFIG_PARAM_ENABLED

        NPamConfigProperty nPamConfigProperty2 = getNPamConfigProperty(nPamConfig.get(0), NPAM_CONFIG_CBRS_DOMAIN)
        isMaster ? NPAM_CONFIG_PARAM_DISABLED.equals(nPamConfigProperty2.getValue()) : nPamConfigProperty2 == null
        where:
        isMaster | _
        false    |_
        true     |_
    }

    def 'createNPamConfig when 1 entry already present with 4 parameters (nothing to do)' () {
        given: 'NPAM_CONFIG configured in dps'
        createConfigMo(NPAM_CONFIG_PARAM_ENABLED, NPAM_CONFIG_PARAM_ENABLED, NPAM_CONFIG_PARAM_ENABLED, NPAM_CONFIG_PARAM_ENABLED)

        and:
        membershipListenerInterfaceMock.isMaster() >> true
        when: ''
        def poId = objUnderTest.createNPamConfig()
        def nPamConfig = objUnderTest.getNPamConfig()
        then: 'nothing to do'
        nPamConfig.get(0).getPoId() != -1
        nPamConfig.get(0).getnPamConfigProperties().size() == 4
    }

    def 'createNPamConfig when 2 entry already present' () {
        given: 'NPAM_CONFIG configured in dps'
        createConfigMo(NPAM_CONFIG_PARAM_ENABLED, null, null, null)
        createConfigMo(NPAM_CONFIG_PARAM_ENABLED, null, null, null)

        and:
        membershipListenerInterfaceMock.isMaster() >> true
        when: ''
        def poId = objUnderTest.createNPamConfig()
        def nPamConfig = objUnderTest.getNPamConfig()
        then: 'delete all and create default object'
        nPamConfig.get(0).getPoId() != -1
        nPamConfig.get(0).getnPamConfigProperties().size() == NPamConfigPropertiesEnum.npamConfigPropertiesList().size()
    }
    /*
     *   private methods
     * */
    private void createConfigMo(final String napmParameterValue, final String cbrsParameterValue,
            final String euParameterValue, final String rmuParameterValue) {
        final List<Map<String, String>> nPamConfigProperties = new ArrayList<>()
        Map<String, String> property = new HashMap<>();
        if (napmParameterValue != null) {
            property = new HashMap<>();
            property.put(PROPERTY_NAME, NPAM_CONFIG_NPAM);
            property.put(PROPERTY_VALUE, napmParameterValue);
            nPamConfigProperties.add(property)
        }

        if (cbrsParameterValue != null) {
            property = new HashMap<>();
            property.put(PROPERTY_NAME, NPAM_CONFIG_CBRS_DOMAIN);
            property.put(PROPERTY_VALUE, cbrsParameterValue);
            nPamConfigProperties.add(property)
        }

        if (euParameterValue != null) {
            property = new HashMap<>();
            property.put(PROPERTY_NAME, NPAM_CONFIG_EMERGENCY_USER);
            property.put(PROPERTY_VALUE, euParameterValue);
            nPamConfigProperties.add(property)
        }

        if (rmuParameterValue != null) {
            property = new HashMap<>();
            property.put(PROPERTY_NAME, NPAM_CONFIG_RESTRICT_MAINTENANCE_USER);
            property.put(PROPERTY_VALUE, rmuParameterValue);
            nPamConfigProperties.add(property)
        }

        runtimeDps.addPersistenceObject().namespace(ModelsConstants.NAMESPACE)
                .version(ModelsConstants.VERSION)
                .type(ModelsConstants.NPAM_CONFIG)
                .addAttribute(NPAM_CONFIG_PROPERTIES,nPamConfigProperties)
                .create()
    }

    private NPamConfig createNPamConfig(final String napmParameterValue, final String cbrsParameterValue,
            final String euParameterValue, final String rmuParameterValue ) {
        NPamConfig npamConfig = new NPamConfig()
        final List<NPamConfigProperty> nPamConfigProperties = new ArrayList<>()
        npamConfig.setnPamConfigProperties(nPamConfigProperties)

        NPamConfigProperty nPamConfigProperty = new NPamConfigProperty();

        if (napmParameterValue != null) {
            nPamConfigProperty = new NPamConfigProperty();
            nPamConfigProperty.setName(NPAM_CONFIG_NPAM);
            nPamConfigProperty.setValue(napmParameterValue);
            nPamConfigProperties.add(nPamConfigProperty);
        }
        if (cbrsParameterValue != null) {
            nPamConfigProperty = new NPamConfigProperty();
            nPamConfigProperty.setName(NPAM_CONFIG_CBRS_DOMAIN);
            nPamConfigProperty.setValue(cbrsParameterValue);
            nPamConfigProperties.add(nPamConfigProperty);
        }
        if (euParameterValue != null) {
            nPamConfigProperty = new NPamConfigProperty();
            nPamConfigProperty.setName(NPAM_CONFIG_EMERGENCY_USER);
            nPamConfigProperty.setValue(euParameterValue);
            nPamConfigProperties.add(nPamConfigProperty);
        }
        if (rmuParameterValue != null) {
            nPamConfigProperty = new NPamConfigProperty();
            nPamConfigProperty.setName(NPAM_CONFIG_RESTRICT_MAINTENANCE_USER);
            nPamConfigProperty.setValue(rmuParameterValue);
            nPamConfigProperties.add(nPamConfigProperty);
        }
        return npamConfig;
    }

    private NPamConfigProperty getNPamConfigProperty(NPamConfig nPamConfig, String name) {
        for (final NPamConfigProperty nPamConfigProperty:nPamConfig.getnPamConfigProperties()) {
            if (name.equals(nPamConfigProperty.getName())) {
                return nPamConfigProperty
            }
        }
        return null
    }
}


