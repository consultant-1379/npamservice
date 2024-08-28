package com.ericsson.oss.services.security.npam.rest.testutil

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_PROPERTIES
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_NPAM
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_CBRS_DOMAIN
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_PARAM_DISABLED
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_NAME
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_VALUE

import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.security.npam.api.constants.ModelsConstants

class NPAMRestTestUtilSpec extends CdiSpecification {


    RuntimeConfigurableDps runtimeDps

    def setup() {
        runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        runtimeDps.withTransactionBoundaries()
    }

    def createConfigMoWithNpamAndCbrs(final String configStatus) {
        final List<Map<String, String>> nPamConfigProperties = new ArrayList<>()

        final Map<String, String> property = new HashMap<>();
        property.put(PROPERTY_NAME, NPAM_CONFIG_NPAM);
        property.put(PROPERTY_VALUE, configStatus);
        nPamConfigProperties.add(property)

        final Map<String, String> property1 = new HashMap<>();
        property1.put(PROPERTY_NAME, NPAM_CONFIG_CBRS_DOMAIN);
        property1.put(PROPERTY_VALUE, NPAM_CONFIG_PARAM_DISABLED);
        nPamConfigProperties.add(property1)

        runtimeDps.addPersistenceObject().namespace(ModelsConstants.NAMESPACE)
                .version(ModelsConstants.VERSION)
                .type(ModelsConstants.NPAM_CONFIG)
                .addAttribute(NPAM_CONFIG_PROPERTIES,nPamConfigProperties)
                .create()
    }

    def createNetworkElementMo(final String neName) {
        runtimeDps.addManagedObject().namespace(ModelsConstants.NAMESPACE)
                .withFdn("NetworkElement="+ neName)
                .addAttribute('neType','RadioNode')
                .addAttribute("name", neName)
                .namespace("OSS_NE_DEF")
                .version("2.0.0")
                .type("NetworkElement")
                .build()
    }
}
