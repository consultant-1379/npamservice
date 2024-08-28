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
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.UriInfo

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.sdk.recording.CommandPhase
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.itpf.sdk.recording.classic.SystemRecorderBean
import com.ericsson.oss.services.security.npam.api.cal.CALConstants
import com.ericsson.oss.services.security.npam.api.cal.CALRecorderDTO

class NPAMRestOutputInterceptorSpec extends CdiSpecification {
    @ObjectUnderTest
    NPAMRestOutputInterceptor objUnderTest

    @Inject
    SystemRecorder systemRecorder

    @MockedImplementation
    UriInfo uriInfoMock

    @MockedImplementation
    ContainerRequestContext requestMock

    @MockedImplementation
    ContainerResponseContext responseMock

    @MockedImplementation
    SystemRecorder mockSystemRecorder

    @Inject
    CALRecorderDTO cALRecorderDTO;

    @ImplementationClasses
    def classes = [
        NPAMRestCommandLogMapper,
        SystemRecorderBean
    ]
    def multiMapValue = new MultivaluedHashMap<>();

    def 'run filter Output Interceptor with success response'() {
        given :
        mockSystemRecorder.isCompactAuditEnabled() >> false
        responseMock.getStatus() >> { return Status.OK.getStatusCode() }

        cALRecorderDTO.setCommandName(CALConstants.NPAM_JOB_LIST)
        cALRecorderDTO.setSource("/v1/job/list/test")
        cALRecorderDTO.setResource("jobEnable")

        when: 'execute'
        def responseRetrieve = objUnderTest.filter(requestMock, responseMock)
        then:
        1 * systemRecorder.recordCommand(CALConstants.NPAM_JOB_LIST, CommandPhase.FINISHED_WITH_SUCCESS, '/v1/job/list/test', 'jobEnable', '')
    }

    def 'run filter Output Interceptor with success response and CAL enabled'() {
        given :
        mockSystemRecorder.isCompactAuditEnabled() >> true
        responseMock.getStatus() >> { return Status.OK.getStatusCode() }
        cALRecorderDTO.setUsername("userName")
        cALRecorderDTO.setCommandName(CALConstants.NPAM_JOB_LIST)
        cALRecorderDTO.setSource("/v1/job/list/test")
        cALRecorderDTO.setResource("jobEnable")
        cALRecorderDTO.setHttpMethod("POST")
        cALRecorderDTO.setCookie("myCookie")
        cALRecorderDTO.setIp("192.168.0.32")

        when: 'execute'
        def responseRetrieve = objUnderTest.filter(requestMock, responseMock)
        then:
        1 * systemRecorder.recordCompactAudit('userName', '<NPAM_JOB_LIST> - POST Resource: ', CommandPhase.FINISHED_WITH_SUCCESS, '/v1/job/list/test', 'jobEnable', '192.168.0.32', 'myCookie', '')
    }

    def 'run filter Output Interceptor with BAD_REQUEST response'() {
        given :
        responseMock.getStatus() >> { return Status.BAD_REQUEST.getStatusCode() }
        when: 'execute'
        def responseRetrieve = objUnderTest.filter(requestMock, responseMock)
        then:
        0 * systemRecorder.recordCommand(CALConstants.NPAM_JOB_LIST, CommandPhase.FINISHED_WITH_SUCCESS, '/v1/job/list/test', 'jobEnable', '')
    }
}
