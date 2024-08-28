/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.security.npam.rest.resources

import javax.inject.Inject
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.UriInfo

import org.jboss.resteasy.specimpl.MultivaluedMapImpl

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.sdk.recording.CommandPhase
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder

class NPAMRestInputInterceptorSpec extends CdiSpecification {
    @ObjectUnderTest
    NPAMRestInputInterceptor objUnderTest

    @Inject
    SystemRecorder systemRecorder

    @MockedImplementation
    UriInfo uriInfoMock

    @MockedImplementation
    ContainerRequestContext requestMock

    @ImplementationClasses
    def classes = [
        NPAMRestCommandLogMapper
    ]
    def multiMapValue = new MultivaluedHashMap<>();

    def 'run filter Input Interceptor '() {
        given :
        def MultivaluedMap<String, String> headers = new MultivaluedMapImpl();

        headers.add("X-Forwarded-For", "192.168.0.32");
        headers.add("X-Tor-UserID", "userName");
        requestMock.getMethod() >> { return "POST" }
        requestMock.getUriInfo()>> { return uriInfoMock }
        requestMock.getCookies() >> { return null}
        requestMock.getHeaders() >> { return headers; }

        uriInfoMock.getPath() >> { return "/v1/job/list/test" }
        uriInfoMock.getRequestUri() >> { return new URI("https://enmapache.athtem.eei.ericsson.se/npamservice/v1/job/list/test"); }
        multiMapValue.put("jobName", Arrays.asList("jobEnable"))
        uriInfoMock.getPathParameters() >> { return multiMapValue }

        when: 'execute'
        def responseRetrieve = objUnderTest.filter(requestMock)
        then:
        1 * systemRecorder.recordCommand('Unknown command : /v1/job/list/test', CommandPhase.STARTED, '/v1/job/list/test', 'jobEnable', '')
    }
}
