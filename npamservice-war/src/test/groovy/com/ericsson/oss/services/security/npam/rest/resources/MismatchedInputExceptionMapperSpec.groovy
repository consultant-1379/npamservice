package com.ericsson.oss.services.security.npam.rest.resources

import javax.inject.Inject
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.fasterxml.jackson.databind.exc.MismatchedInputException

class MismatchedInputExceptionMapperSpec extends CdiSpecification {
    @ObjectUnderTest
    MismatchedInputExceptionMapper objUnderTest

    @Inject
    SystemRecorder systemRecorder

    @MockedImplementation
    UriInfo uriInfoMock

    @MockedImplementation
    HttpServletRequest requestMock

    @MockedImplementation
    MismatchedInputException mismatchedInputExceptionMock

    @ImplementationClasses
    def classes = [
        NPAMRestCommandLogMapper
    ]
    def multiMapValue = new MultivaluedHashMap<>();

    def 'run toResponse with message BAD_REQUEST response'() {
        when: 'execute'
        requestMock.getMethod() >> { return "POST" }
        uriInfoMock.getPath() >> { return "/v1/job/list/test" }
        mismatchedInputExceptionMock.getMessage()>> { return "ErrorDetails" }
        def responseRetrieve = objUnderTest.toResponse(mismatchedInputExceptionMock)
        then:
        responseRetrieve.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()
    }

    def 'run toResponse with message BAD REQUEST response with resource'() {
        given:
        multiMapValue.put("jobName", Arrays.asList("jobEnable"))
        uriInfoMock.getPathParameters() >> { return multiMapValue }

        when: 'execute'
        requestMock.getMethod() >> { return "POST" }
        uriInfoMock.getPath() >> { return "/v1/job/list/test" }
        mismatchedInputExceptionMock.getMessage()>> { return "ErrorDetails" }
        def responseRetrieve = objUnderTest.toResponse(mismatchedInputExceptionMock)
        then:
        responseRetrieve.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()
    }
}
