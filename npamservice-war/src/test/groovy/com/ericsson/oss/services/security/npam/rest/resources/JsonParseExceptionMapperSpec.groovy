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
import com.fasterxml.jackson.core.JsonParseException

class JsonParseExceptionMapperSpec extends CdiSpecification {
    @MockedImplementation
    JsonParseException jsonParseExceptionMock

    @ObjectUnderTest
    JsonParseExceptionMapper objUnderTest

    @Inject
    SystemRecorder systemRecorder

    @MockedImplementation
    UriInfo uriInfoMock

    @MockedImplementation
    HttpServletRequest requestMock

    @ImplementationClasses
    def classes = [
        NPAMRestCommandLogMapper
    ]
    def multiMapValue = new MultivaluedHashMap<>();

    def 'run toResponse with message BAD REQUEST response'() {
        when: 'execute'
        requestMock.getMethod() >> { return "POST" }
        uriInfoMock.getPath() >> { return "/v1/job/list/test" }
        jsonParseExceptionMock.getMessage()>> { return "ErrorDetails" }
        def responseRetrieve = objUnderTest.toResponse(jsonParseExceptionMock)
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
        jsonParseExceptionMock.getMessage()>> { return "ErrorDetails" }
        def responseRetrieve = objUnderTest.toResponse(jsonParseExceptionMock)
        then:
           responseRetrieve.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()
    }
}
