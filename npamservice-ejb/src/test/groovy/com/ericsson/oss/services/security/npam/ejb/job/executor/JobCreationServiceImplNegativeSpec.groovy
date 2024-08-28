package com.ericsson.oss.services.security.npam.ejb.job.executor

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.rule.SpyImplementation
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService
import com.ericsson.oss.services.security.npam.api.exceptions.JobExceptionFactory
import com.ericsson.oss.services.security.npam.api.interfaces.JobConfigurationService
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.security.npam.ejb.message.NodePamQueueMessageSender

class JobCreationServiceImplNegativeSpec extends BaseSetupForTestSpecs {

    @ObjectUnderTest
    private JobCreationServiceImpl objUnderTest

    @SpyImplementation
    JobExceptionFactory jobExceptionFactory

    @MockedImplementation
    NodePamQueueMessageSender nodePamQueueMessageSender

    @Inject
    JobConfigurationService jobConfigurationService

    @MockedImplementation
    DataPersistenceService dataPersistenceService


    def setup() {
        runtimeDps.withTransactionBoundaries()
    }

    def 'createNewJobTemplate IMMEDIATE with DPS Error'() {
        given: 'a job template'
        dataPersistenceService.getLiveBucket() >> {throw  new Exception("message")}
        when: 'search for Job Template By Name'
        def result = objUnderTest.getJobTemplatePoIdsByName("testName")

        then: 'empty list returned'
        result.isEmpty()
    }
}
