package com.ericsson.oss.services.security.npam.rest.resources

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_CBRS_DOMAIN
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_EMERGENCY_USER
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_NPAM
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_PARAM_DISABLED
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_PARAM_ENABLED
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_RESTRICT_MAINTENANCE_USER

import javax.ws.rs.core.Response

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfig
import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfigPropertiesEnum
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage
import com.ericsson.oss.services.security.npam.ejb.config.executor.NPamConfigServiceImpl
import com.ericsson.oss.services.security.npam.ejb.rest.ConfigServiceImpl
import com.ericsson.oss.services.security.npam.rest.api.NPAMConfigPropertyJAXB
import com.ericsson.oss.services.security.npam.rest.testutil.EAccessControlBypassAllImpl
import com.ericsson.oss.services.security.npam.rest.testutil.NPAMRestTestUtilSpec

import spock.lang.Unroll

class NPAMConfigServiceRestSpec extends NPAMRestTestUtilSpec {

    @ObjectUnderTest
    private NPAMConfigServiceRest objUnderTest

    @ImplementationClasses
    def classes = [
        ConfigServiceImpl,
        NPamConfigServiceImpl,
        EAccessControlBypassAllImpl
    ]

    def setup() {
    }

    /*
     * updateNpamConfig method
     * */
    def 'run update and read NPAmConfig'() {
        given: 'NPAM_CONFIG configured in dps'
        createConfigMoWithNpamAndCbrs(NPAM_CONFIG_PARAM_DISABLED)
        when: ''
        def List<NPAMConfigPropertyJAXB> properties = createArgument(NPAM_CONFIG_NPAM, NPAM_CONFIG_PARAM_ENABLED)
        def responseUpdate = objUnderTest.updateNpamConfig(properties)
        and:
        def  responseGet = objUnderTest.getNPamConfig()
        def resultGet = (List<NPAMConfigPropertyJAXB>)responseGet.getEntity()
        then:
        responseUpdate.getStatus() == Response.Status.OK.statusCode
        and: ''
        responseGet.getStatus() == Response.Status.OK.statusCode
        resultGet.size() == 2
        resultGet.get(0).getName()  == NPAM_CONFIG_NPAM
        resultGet.get(0).getValue() == NPAM_CONFIG_PARAM_ENABLED
    }

    def 'run update NPAmConfig with wrong parameter name=other'() {
        given: 'NPAM_CONFIG configured in dps'
        createConfigMoWithNpamAndCbrs(NPAM_CONFIG_PARAM_DISABLED)
        when: ''
        def List<NPAMConfigPropertyJAXB> properties = createArgument("other", NPAM_CONFIG_PARAM_ENABLED)
        def responseUpdate = objUnderTest.updateNpamConfig(properties)
        and:
        def  responseGet = objUnderTest.getNPamConfig()
        def resultGet = (List<NPAMConfigPropertyJAXB>)responseGet.getEntity()
        then: 'exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().contains(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_NPAMCONFIG_PROPERTIES.getMessage())
    }

    def 'run update NPAmConfig with wrong parameter value'() {
        given: 'NPAM_CONFIG configured in dps'
        createConfigMoWithNpamAndCbrs(NPAM_CONFIG_PARAM_DISABLED)
        when: ''
        def List<NPAMConfigPropertyJAXB> properties = createArgument(NPAM_CONFIG_NPAM, "wrongstatus")
        def responseUpdate = objUnderTest.updateNpamConfig(properties)
        then: 'exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().contains(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_NPAMCONFIG_PROPERTIES.getMessage())
    }

    def 'run update no NPAmConfig configured in DPS'() {
        given: ''
        when: ''
        def List<NPAMConfigPropertyJAXB> properties = createArgument(NPAM_CONFIG_NPAM, NPAM_CONFIG_PARAM_ENABLED)
        def responseUpdate = objUnderTest.updateNpamConfig(properties)
        then:
        (((NPamConfig)responseUpdate.getEntity()).getPoId()) == -1
    }

    /*
     * getNPamConfig method
     * */
    def 'read NPAmConfig'() {
        given: 'NPAM_CONFIG configured in dps'
        createConfigMoWithNpamAndCbrs(NPAM_CONFIG_PARAM_DISABLED)
        when: ''
        def  responseGet = objUnderTest.getNPamConfig()
        def resultGet = (List<NPAMConfigPropertyJAXB>)responseGet.getEntity()
        then:
        responseGet.getStatus() == Response.Status.OK.statusCode
        resultGet.size() == 2
        resultGet.get(0).getName()  == NPAM_CONFIG_NPAM
        resultGet.get(0).getValue() == NPAM_CONFIG_PARAM_DISABLED
        resultGet.get(1).getName()  == NPAM_CONFIG_CBRS_DOMAIN
        resultGet.get(1).getValue() == NPAM_CONFIG_PARAM_DISABLED
    }

    /*
     * getNPamConfigCached method
     * */
    def 'read NPAmConfig from cache'() {
        when: ''
        def  responseGet = objUnderTest.getNPamConfigCached()
        def resultGet = (List<NPAMConfigPropertyJAXB>)responseGet.getEntity()
        then:
        responseGet.getStatus() == Response.Status.OK.statusCode
        resultGet.size() == NPamConfigPropertiesEnum.npamConfigPropertiesList().size()
        resultGet.get(0).getName()  == NPAM_CONFIG_NPAM
        resultGet.get(0).getValue() == NPAM_CONFIG_PARAM_DISABLED
        resultGet.get(1).getName()  == NPAM_CONFIG_CBRS_DOMAIN
        resultGet.get(1).getValue() == NPAM_CONFIG_PARAM_DISABLED
    }

    @Unroll
    def 'isKnownParameter name=#name'(name, isKnownParameter) {
        when: ''
        boolean ret = NPamConfigPropertiesEnum.isKnownPropertyName(name)
        then:
        ret == isKnownParameter
        where:
        name                                  || isKnownParameter |_
        NPAM_CONFIG_NPAM                      || true             | _
        NPAM_CONFIG_CBRS_DOMAIN               || true             | _
        NPAM_CONFIG_EMERGENCY_USER            || false            | _
        NPAM_CONFIG_RESTRICT_MAINTENANCE_USER || false            | _
        null                                  || false            | _
        "other"                               || false            | _
    }

    /*
     * getNPamConfig method
     * */
    def 'read NPAmConfig with 2 object'() {
        given: 'NPAM_CONFIG configured in dps'
        createConfigMoWithNpamAndCbrs(NPAM_CONFIG_PARAM_DISABLED)
        createConfigMoWithNpamAndCbrs(NPAM_CONFIG_PARAM_DISABLED)

        when: ''
        def  responseGet = objUnderTest.getNPamConfig()
        then: 'exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().contains(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_DUPLICATED_NPAMCONFIG.getMessage())
    }
    /*
     * equal method
     */
    def 'compare NPAMConfigPropertyJAXB'() {
        given: 'NPAM_CONFIG configured in dps'
        def prop1 = new NPAMConfigPropertyJAXB(NPAM_CONFIG_NPAM, NPAM_CONFIG_PARAM_ENABLED)
        def prop2 = new NPAMConfigPropertyJAXB(NPAM_CONFIG_NPAM, NPAM_CONFIG_PARAM_ENABLED)
        def prop3 = new NPAMConfigPropertyJAXB(NPAM_CONFIG_NPAM, NPAM_CONFIG_PARAM_DISABLED)
        def prop4 = new NPAMConfigPropertyJAXB(NPAM_CONFIG_CBRS_DOMAIN, NPAM_CONFIG_PARAM_DISABLED)

        when: ''
        def compare1 = prop1.equals(prop2)
        def compare2 = prop1.equals(prop3)
        def compare3 = prop1.equals(prop4)
        def compare4 = prop3.equals(prop4)
        then:
        compare1 == true
        compare2 == true
        compare3 == false
        compare4 == false
        (prop4.toString()).contains(NPAM_CONFIG_CBRS_DOMAIN)
    }
    /*
     * private methods
     * */
    private List<NPAMConfigPropertyJAXB>createArgument(final String nameProperty, final String configStatus) {
        final List<NPAMConfigPropertyJAXB> nPamConfigProperties = new ArrayList<>()
        NPAMConfigPropertyJAXB configEl = new NPAMConfigPropertyJAXB();
        configEl.setName(nameProperty);
        configEl.setValue(configStatus);
        nPamConfigProperties.add(configEl);
        return nPamConfigProperties;
    }
}
