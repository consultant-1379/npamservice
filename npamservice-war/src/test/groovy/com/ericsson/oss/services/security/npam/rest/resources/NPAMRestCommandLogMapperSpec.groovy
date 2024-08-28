package com.ericsson.oss.services.security.npam.rest.resources

import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.UriInfo

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.security.npam.api.cal.CALConstants

class NPAMRestCommandLogMapperSpec extends CdiSpecification {
    @MockedImplementation
    UriInfo uriInfoMock
    def multiMapValue = new MultivaluedHashMap<>();
    def setup() {
        multiMapValue.put("jobName", Arrays.asList("jobEnable"))
        uriInfoMock.getPath() >> { return "/v1/job/list/jobEnable" }
        uriInfoMock.getPathParameters() >> { return multiMapValue }
        uriInfoMock.getQueryParameters() >> { return multiMapValue }
    }

    def 'run getCommandName get rest'() {
        given: 'uri info'
        when: ''
        def  response = NPAMRestCommandLogMapper.getCommandName("GET", uriInfoMock)
        then:
        response.equals(CALConstants.NPAM_JOB_LIST)
    }

    def 'run getCommandName post rest'() {
        given: 'uri info'
        when: ''
        def  response = NPAMRestCommandLogMapper.getCommandName("POST", uriInfoMock)
        then:
        uriInfoMock.getPath() >> { return "/v1/job/cancel/jobName" }
        response.equals(CALConstants.NPAM_JOB_CANCEL)
    }

    def 'run getCommandName not supported rest'() {
        given: 'uri info'
        when: ''
        def  response = NPAMRestCommandLogMapper.getCommandName("POST", uriInfoMock)
        then:
        response.contains("Unknown command : ")
    }

    def 'run getCommandName not supported DELETE rest'() {
        given: 'uri info'
        when: ''
        def  response = NPAMRestCommandLogMapper.getCommandName("DELETE", uriInfoMock)
        then:
        response.contains("Unknown command : ")
    }

    def 'run getPathParamValue'() {
        given: 'uri info'
        when: ''
        def  response = NPAMRestCommandLogMapper.getPathParamValue(multiMapValue)
        then:
        response.equals("jobEnable")
    }

    def 'run getPathParamValue with params null'() {
        given: 'uri info'
        when: ''
        def  response = NPAMRestCommandLogMapper.getPathParamValue(null)
        then:
        response.equals("")
    }

    def 'run getPathParamValue with params empty'() {
        given: 'uri info'
        when: ''
        def  response = NPAMRestCommandLogMapper.getPathParamValue(new MultivaluedHashMap<>())
        then:
        response.equals("")
    }

    def 'run getQueryParamValue'() {
        given: 'uri info'
        when: ''
        def  response = NPAMRestCommandLogMapper.getQueryParameters(multiMapValue)
        then:
        response.equals("jobName:jobEnable")
    }

    def 'run getQueryParamValue with params null'() {
        given: 'uri info'
        when: ''
        def  response = NPAMRestCommandLogMapper.getQueryParameters(null)
        then:
        response.equals("")
    }

    def 'run getQueryParamValue with params empty'() {
        given: 'uri info'
        when: ''
        def  response = NPAMRestCommandLogMapper.getQueryParameters(new MultivaluedHashMap<>())
        then:
        response.equals("")
    }

    def 'run empty constructor just for coverage'() {
        given: 'uri info'
        when: ''
        def  response = new NPAMRestCommandLogMapper()
        then:
        notThrown(Exception)
    }
}
