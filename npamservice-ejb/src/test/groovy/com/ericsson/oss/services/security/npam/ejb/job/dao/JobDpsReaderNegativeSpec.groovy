package com.ericsson.oss.services.security.npam.ejb.job.dao

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService
import com.ericsson.oss.itpf.datalayer.dps.exception.general.ObjectNotInContextException
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs

class JobDpsReaderNegativeSpec extends BaseSetupForTestSpecs {

    @ObjectUnderTest
    private JobDpsReader objUnderTest

    @MockedImplementation
    DataPersistenceService dataPersistenceServiceMock;

    def setup() {
        dataPersistenceServiceMock.getLiveBucket() >> {throw new ObjectNotInContextException("Error")}
    }


    def 'test to cover exception in isMainJob'() {
        given: 'an exception thrown when calling getLiveBucket'
        when: 'call isNpamJob'
        def isMainJob = objUnderTest.isMainJob(9999)
        then:
        isMainJob == false
    }

    def 'test to cover exception in fetchJobsFromJobTemplate'() {
        given: 'getLiveBucket throwing an exception'
        when: 'call fetchJobsFromJobTemplate with jobName as argument'
        def npamJobList = objUnderTest.fetchJobsFromJobTemplate("jobName")
        then: 'an empty list is returned'
        npamJobList.size() == 0
    }

}

