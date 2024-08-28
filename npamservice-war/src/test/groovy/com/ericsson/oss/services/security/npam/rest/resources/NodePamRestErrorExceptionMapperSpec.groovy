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
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage

class NodePamRestErrorExceptionMapperSpec extends CdiSpecification {
    @ObjectUnderTest
    NodePamRestErrorExceptionMapper objUnderTest

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

    def 'run toResponse with message FORBIDDEN response'() {
        when: 'execute'
        requestMock.getMethod() >> { return "POST" }
        uriInfoMock.getPath() >> { return "/v1/job/list/test" }
        NPAMRestErrorException nodePamRestErrorException = new NPAMRestErrorException(NPamRestErrorMessage.FORBIDDEN_FEATURE_DISABLED);
        def responseRetrieve = objUnderTest.toResponse(nodePamRestErrorException)
        then:
        responseRetrieve.getStatus() == Response.Status.FORBIDDEN.getStatusCode()
    }

    def 'run toResponse with message FORBIDDEN response with resource'() {
        given:
        multiMapValue.put("jobName", Arrays.asList("jobEnable"))
        uriInfoMock.getPathParameters() >> { return multiMapValue }

        when: 'execute'
        requestMock.getMethod() >> { return "POST" }
        uriInfoMock.getPath() >> { return "/v1/job/list/test" }
        NPAMRestErrorException nodePamRestErrorException = new NPAMRestErrorException(NPamRestErrorMessage.FORBIDDEN_FEATURE_DISABLED);
        def responseRetrieve = objUnderTest.toResponse(nodePamRestErrorException)
        then:
        responseRetrieve.getStatus() == Response.Status.FORBIDDEN.getStatusCode()
    }
}
