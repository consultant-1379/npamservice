package com.ericsson.oss.services.security.npam.rest.api

import static com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult.SUCCESS
import static com.ericsson.oss.services.security.npam.api.job.modelentities.JobState.COMPLETED

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification

class NPamNeJobJAXBSpec extends CdiSpecification {

    @ObjectUnderTest
    private NPAMNeJobJAXB objUnderTest

    def 'run marshal unmarshal of a date'() {
        when: 'execute'
        def now = new Date()
        NPAMNeJobJAXB objUnderTest = new NPAMNeJobJAXB();
        objUnderTest.setNeName("NeName")
        objUnderTest.setState(COMPLETED)
        objUnderTest.setResult(SUCCESS)
        objUnderTest.setStartTime(now)
        objUnderTest.setEndTime(now)
        objUnderTest.setErrorDetails("errors")
        then : ' '
        objUnderTest.getNeName().equals("NeName")
        objUnderTest.getState().equals(COMPLETED)
        objUnderTest.getResult().equals(SUCCESS)
        objUnderTest.getStartTime().equals(now)

        objUnderTest.getEndTime().equals(now)
        objUnderTest.getErrorDetails().equals("errors")
    }
}
