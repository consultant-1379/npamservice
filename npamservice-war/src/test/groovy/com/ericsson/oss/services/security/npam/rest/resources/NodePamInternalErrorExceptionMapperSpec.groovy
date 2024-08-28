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

class NodePamInternalErrorExceptionMapperSpec extends CdiSpecification {
    @ObjectUnderTest
    NodePamInternalErrorExceptionMapper objUnderTest

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

    def 'run toResponse with message INTERNAL_SERVER_ERROR response'() {
        when: 'execute'
        requestMock.getMethod() >> { return "POST" }
        uriInfoMock.getPath() >> { return "/v1/job/list/test" }

        Exception genericException = new Exception("This is a generic exception just for test coverage");
        def responseRetrieve = objUnderTest.toResponse(genericException)
        then:
        responseRetrieve.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()
    }

    def 'run toResponse with message INTERNAL_SERVER_ERROR response with resource'() {
        given:
         multiMapValue.put("jobName", Arrays.asList("jobEnable"))
         uriInfoMock.getPathParameters() >> { return multiMapValue }

        when: 'execute'
        requestMock.getMethod() >> { return "POST" }
        uriInfoMock.getPath() >> { return "/v1/job/list/test" }
        Exception genericException = new Exception("This is a generic exception just for test coverage");
        def responseRetrieve = objUnderTest.toResponse(genericException)
        then:
           responseRetrieve.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()
    }
}
