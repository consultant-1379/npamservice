package com.ericsson.oss.services.security.npam.ejb.database.availability

import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_NAME
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_VALUE
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_CBRS_DOMAIN
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_EMERGENCY_USER
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_NPAM
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_PARAM_DISABLED
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_PARAM_ENABLED
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_PROPERTIES
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_RESTRICT_MAINTENANCE_USER

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.security.npam.api.constants.ModelsConstants
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs

import spock.lang.Unroll

class NodePamConfigStatusSpec extends BaseSetupForTestSpecs {

    @ObjectUnderTest
    private NodePamConfigStatus objUnderTest

    def setup() {
        runtimeDps.withTransactionBoundaries()
    }

    /*
     *    settingInitialStatus method
     * */
    def 'settingInitialStatus with no config entry already present -> returns expected values'() {
        given: 'no NPAM_CONFIG configured in dps'
        when: ''
        objUnderTest.settingInitialStatus()
        then:
        objUnderTest.isEnabled() == false
        objUnderTest.isCbrsDomainEnabled() == false
        objUnderTest.isEmergencyUserEnabled() == false
        objUnderTest.isRestrictMaintenanceUserEnabled() == false
    }

    @Unroll
    def 'settingInitialStatus with 1 config entry present -> returns expected values'(napmParameterValue, cbrsParameterValue,
            euParameterValue, rmuParameterValue,
            isEnabled, isCbrsDomainEnabled,
            isEmergencyUserEnabled, isRestrictMaintenanceUserEnabled) {
        given: 'NPAM_CONFIG configured in dps'
        createConfigMo(napmParameterValue, cbrsParameterValue, euParameterValue, rmuParameterValue)
        when: ''
        objUnderTest.settingInitialStatus()
        then:
        objUnderTest.isEnabled() == isEnabled
        objUnderTest.isCbrsDomainEnabled() == isCbrsDomainEnabled
        objUnderTest.isEmergencyUserEnabled() == isEmergencyUserEnabled
        objUnderTest.isRestrictMaintenanceUserEnabled() == isRestrictMaintenanceUserEnabled
        where:
        napmParameterValue         | cbrsParameterValue         | euParameterValue            | rmuParameterValue          || isEnabled  ||  isCbrsDomainEnabled | isEmergencyUserEnabled | isRestrictMaintenanceUserEnabled |_
        // test napmParameterValue and cbrsParameterValue
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || true       || true                 | false                  | false                            |_
        NPAM_CONFIG_PARAM_DISABLED | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || false      || true                 | false                  | false                            |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_DISABLED | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || true       || false                | false                  | false                            |_
        NPAM_CONFIG_PARAM_DISABLED | NPAM_CONFIG_PARAM_DISABLED | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || false      || false                | false                  | false                            |_
        null                       | null                       | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || false      || false                | false                  | false                            |_
        null                       | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED   | NPAM_CONFIG_PARAM_ENABLED  || false      || false                | false                  | false                            |_
        NPAM_CONFIG_PARAM_ENABLED  | null                       | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || true       || false                | false                  | false                            |_
        "aa"                       | "bb"                       | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || false      || false                | false                  | false                            |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED   | NPAM_CONFIG_PARAM_ENABLED  || true       || true                 | false                  | false                            |_

        // test euParameterValue and rmuParameterValue
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  || true       || true                 | false                   | false                              |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_DISABLED | NPAM_CONFIG_PARAM_ENABLED  || true       || true                 | false                  | false                              |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_DISABLED || true       || true                 | false                   | false                             |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_DISABLED | NPAM_CONFIG_PARAM_DISABLED || true       || true                 | false                  | false                             |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | null                       | null                       || true       || true                 | false                  | false                             |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | null                       | NPAM_CONFIG_PARAM_ENABLED  || true       || true                 | false                  | false                              |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | null                       || true       || true                 | false                   | false                             |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | "cc"                       | "dd"                       || true       || true                 | false                  | false                             |_
    }

    def 'settingInitialStatus  with invalid config entry -> returns expected values'() {
        given: 'invalid NPAM_CONFIG configured in dps'
        createInvalidConfigMo()
        when: ''
        objUnderTest.settingInitialStatus()
        then:
        objUnderTest.isEnabled() == false
        objUnderTest.isCbrsDomainEnabled() == false
        objUnderTest.isEmergencyUserEnabled() == false
        objUnderTest.isRestrictMaintenanceUserEnabled() == false
    }


    /*
     *    setConfig method
     * */
    @Unroll
    def 'setConfigStatus -> returns expected values'(napmParameterValue, cbrsParameterValue,
            euParameterValue, rmuParameterValue,
            isEnabled, isCbrsDomainEnabled,
            isEmergencyUserEnabled, isRestrictMaintenanceUserEnabled) {
        when: ''
        objUnderTest.setConfig(createNPamConfigProperties(napmParameterValue, cbrsParameterValue, euParameterValue, rmuParameterValue))
        then:
        objUnderTest.isEnabled() == isEnabled
        objUnderTest.isCbrsDomainEnabled() == isCbrsDomainEnabled
        objUnderTest.isEmergencyUserEnabled() == isEmergencyUserEnabled
        objUnderTest.isRestrictMaintenanceUserEnabled() == isRestrictMaintenanceUserEnabled
        where:
        napmParameterValue         | cbrsParameterValue         | euParameterValue            | rmuParameterValue          || isEnabled  ||  isCbrsDomainEnabled | isEmergencyUserEnabled | isRestrictMaintenanceUserEnabled |_
        // test napmParameterValue and cbrsParameterValue
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || true       || true                 | false                  | false                            |_
        NPAM_CONFIG_PARAM_DISABLED | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || false      || true                 | false                  | false                            |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_DISABLED | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || true       || false                | false                  | false                            |_
        NPAM_CONFIG_PARAM_DISABLED | NPAM_CONFIG_PARAM_DISABLED | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || false      || false                | false                  | false                            |_
        null                       | null                       | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || false      || false                | false                  | false                            |_
        null                       | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED   | NPAM_CONFIG_PARAM_ENABLED  || false      || false                | false                  | false                            |_
        NPAM_CONFIG_PARAM_ENABLED  | null                       | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || true       || false                | false                  | false                            |_
        "aa"                       | "bb"                       | NPAM_CONFIG_PARAM_DISABLED  | NPAM_CONFIG_PARAM_DISABLED || false      || false                | false                  | false                            |_

        // test euParameterValue and rmuParameterValue
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  || true       || true                 | false                   | false                             |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_DISABLED | NPAM_CONFIG_PARAM_ENABLED  || true       || true                 | false                   | false                             |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_DISABLED || true       || true                 | false                   | false                             |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_DISABLED | NPAM_CONFIG_PARAM_DISABLED || true       || true                 | false                   | false                             |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | null                       | null                       || true       || true                 | false                   | false                             |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | null                       | NPAM_CONFIG_PARAM_ENABLED  || true       || true                 | false                   | false                             |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | null                       || true       || true                 | false                   | false                             |_
        NPAM_CONFIG_PARAM_ENABLED  | NPAM_CONFIG_PARAM_ENABLED  | "cc"                       | "dd"                       || true       || true                 | false                   | false                             |_
    }

    /*
     *   private methods
     * */
    private void createInvalidConfigMo() {
        final List<Map<String, String>> nPamConfigProperties = null

        runtimeDps.addPersistenceObject().namespace(ModelsConstants.NAMESPACE)
                .version(ModelsConstants.VERSION)
                .type(ModelsConstants.NPAM_CONFIG)
                .addAttribute(NPAM_CONFIG_PROPERTIES,nPamConfigProperties)
                .create()
    }

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

    private List<Map<String, String>> createNPamConfigProperties(final String napmParameterValue, final String cbrsParameterValue,
            final String euParameterValue, final String rmuParameterValue) {
        final List<Map<String, String>> npamConfigProperties = new ArrayList<>()

        if (napmParameterValue != null) {
            Map<String, String> properties = new HashMap();
            properties.put(PROPERTY_NAME, NPAM_CONFIG_NPAM);
            properties.put(PROPERTY_VALUE, napmParameterValue);
            npamConfigProperties.add(properties)
        }

        if (cbrsParameterValue != null) {
            Map<String, String> properties = new HashMap();
            properties.put(PROPERTY_NAME, NPAM_CONFIG_CBRS_DOMAIN);
            properties.put(PROPERTY_VALUE, cbrsParameterValue);
            npamConfigProperties.add(properties)
        }

        if (euParameterValue != null) {
            Map<String, String> properties = new HashMap();
            properties.put(PROPERTY_NAME, NPAM_CONFIG_EMERGENCY_USER);
            properties.put(PROPERTY_VALUE, euParameterValue);
            npamConfigProperties.add(properties)
        }

        if (rmuParameterValue != null) {
            Map<String, String> properties = new HashMap();
            properties.put(PROPERTY_NAME, NPAM_CONFIG_RESTRICT_MAINTENANCE_USER);
            properties.put(PROPERTY_VALUE, rmuParameterValue);
            npamConfigProperties.add(properties)
        }

        return npamConfigProperties;
    }
}


