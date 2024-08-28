package com.ericsson.oss.services.security.npam.rest.api


import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification

class NPamJobImportJAXBSpec extends CdiSpecification {

    @ObjectUnderTest
    private NPAMJobImportResponseJAXB objUnderTest

    def 'run getters and setters of NPAMJobImportResponseJAXB'() {
        when: 'execute'

        def objUnderTest = new NPAMJobImportResponseJAXB();
        // Just for test coverage
        def npamMultipartBodyJAXB = new NPAMMultipartBodyJAXB();
        objUnderTest.setResult("Result content")

        then : ' '
        objUnderTest.getResult().equals("Result content")
        objUnderTest.toString().equals("NPAMJobTemplateResponseJAXB [result = Result content]")
    }
}