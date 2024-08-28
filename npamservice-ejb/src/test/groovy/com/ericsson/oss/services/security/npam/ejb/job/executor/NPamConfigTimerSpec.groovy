package com.ericsson.oss.services.security.npam.ejb.job.executor

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest

import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.security.npam.ejb.config.dao.NPamConfigDpsReader
import com.ericsson.oss.services.security.npam.ejb.config.executor.NPamConfigServiceImpl
import com.ericsson.oss.services.security.npam.ejb.listener.MembershipListenerInterface
import com.ericsson.oss.services.security.npam.ejb.testutil.DpsQueryUtil

import javax.ejb.SessionContext
import javax.ejb.TimerService
import javax.ejb.Timer;
import javax.inject.Inject

class NPamConfigTimerSpec  extends BaseSetupForTestSpecs {

    @ObjectUnderTest
    private NPamConfigTimer objUnderTest

    @Inject
    SessionContext sessionContextMock

    @Inject
    TimerService timerServiceMock

    @Inject
    Timer timerMock

    @Inject
    NPamConfigDpsReader nPamConfigDpsReader


    @MockedImplementation
    MembershipListenerInterface membershipListenerInterfaceMock

    @ImplementationClasses
    def classes = [
            NPamConfigServiceImpl
    ]

    def setup() {
        runtimeDps.withTransactionBoundaries()
        membershipListenerInterfaceMock.isMaster() >> true
    }

    def 'scheduleTimerForInitNPamConfig  create timer scheduled  failed'() {
        given: 'mock sessionContext to return null'
            sessionContextMock.getTimerService() >> null
        when: ''
            objUnderTest.scheduleTimerForInitNPamConfig()
            def result = nPamConfigDpsReader.getNPamConfigObject()
        then:
            result.first().getnPamConfigProperties().first().getName() == 'npam'
            result.first().getnPamConfigProperties().first().getValue() == 'disabled'
    }

    def 'scheduleTimerForInitNPamConfig  create timer scheduled  success'() {
        given: ''
            sessionContextMock.getTimerService() >> timerServiceMock
        when: 'the event is consumed'
            objUnderTest.scheduleTimerForInitNPamConfig()
        then:
            1 * timerServiceMock.createSingleActionTimer(_,_)
    }

    def 'timeoutHandler  create timer scheduled  success'() {
        when: 'the event is consumed'
            objUnderTest.timeoutHandler(timerMock)
            def result = nPamConfigDpsReader.getNPamConfigObject()
        then:
            result.first().getnPamConfigProperties().first().getName() == 'npam'
            result.first().getnPamConfigProperties().first().getValue() == 'disabled'
        and:
            1 * timerMock.cancel()
    }

    def 'timeoutHandler  create timer scheduled  failed'() {
        given:
            timerMock.cancel() >> { throw  new RuntimeException("some message")}
        when: 'the event is consumed'
            objUnderTest.timeoutHandler(timerMock)
        then:
            1 * sessionContextMock.getTimerService()
    }
}


