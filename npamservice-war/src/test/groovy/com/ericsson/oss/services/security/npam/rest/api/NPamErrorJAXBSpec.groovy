package com.ericsson.oss.services.security.npam.rest.api


import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification

class NPamErrorJAXBSpec extends CdiSpecification {

    @ObjectUnderTest
    private NPAMErrorJAXB objUnderTest


    def 'run getters and setters of NPamErrorJAXBSpec'() {
        when: 'execute'

        def objUnderTest = new NPAMErrorJAXB();

        objUnderTest.setUserMessage("userMessage")
        objUnderTest.setInternalErrorCode(401)
        objUnderTest.setErrorDetails("ErrorDetails")

        then : ' '
        objUnderTest.getUserMessage().equals("userMessage")
        objUnderTest.getInternalErrorCode() ==  401
        objUnderTest.getErrorDetails().equals("ErrorDetails")
    }
    
    def 'run constructor with 2 parameters and getters of NPamErrorJAXB'() {
        when: 'execute'

        def objUnderTest = new NPAMErrorJAXB("userMessage", 401);

        objUnderTest.setErrorDetails("ErrorDetails")

        then : ' '
        objUnderTest.getUserMessage().equals("userMessage")
        objUnderTest.getInternalErrorCode() == 401
    }

    def 'run constructor with 3 parameters and getters of NPamErrorJAXB'() {
        when: 'execute'

        def objUnderTest = new NPAMErrorJAXB("userMessage", 401, "ErrorDetails");

        then : ' '
        objUnderTest.getUserMessage().equals("userMessage")
        objUnderTest.getInternalErrorCode() == 401
        objUnderTest.getErrorDetails().equals("ErrorDetails")
    }
}
