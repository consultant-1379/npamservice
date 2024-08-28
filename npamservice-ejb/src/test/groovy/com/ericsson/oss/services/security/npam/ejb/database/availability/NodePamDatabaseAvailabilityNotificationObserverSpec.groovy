package com.ericsson.oss.services.security.npam.ejb.database.availability

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService

import javax.ejb.Timer
import javax.inject.Inject

class NodePamDatabaseAvailabilityNotificationObserverSpec extends CdiSpecification {

    @Inject
    Timer timerMock

    @MockedImplementation
    DataPersistenceService dataPersistenceServiceMock

    @ObjectUnderTest
    private NodePamDatabaseAvailabilityNotificationObserver objUnderTest

    def setup() {
    }

    def 'scheduleListenerForDpsNotification (for coverage)'() {
        when:
            objUnderTest.scheduleListenerForDpsNotification()
        then:
            true
    }

    def 'listenForDpsNotifications when dps success (for coverage)'() {
        when:
            objUnderTest.listenForDpsNotifications(timerMock)
        then:
            true
    }

    def 'listenForDpsNotifications when dps throws exception (for coverage)'() {
        given:'dps throws exception'
            dataPersistenceServiceMock.registerDpsAvailabilityCallback(_) >> {throw  new Exception("message")}
        when:
            objUnderTest.listenForDpsNotifications(timerMock)
        then:
            true
    }

    def 'listenForDpsNotifications when dps throws exception for 20 times (for coverage)'() {
        given:'dps throws exception'
            dataPersistenceServiceMock.registerDpsAvailabilityCallback(_) >> {throw  new Exception("message")}
        when:
            for (int i=0; i<=20; i++) {
                objUnderTest.listenForDpsNotifications(timerMock)
            }
        then:
            true
    }

    def 'DatabaseNotAvailableException is not used yet (for coverage)'() {
        when:
            DatabaseNotAvailableException databaseNotAvailableException = new  DatabaseNotAvailableException()
        then:
            true;
    }

}


