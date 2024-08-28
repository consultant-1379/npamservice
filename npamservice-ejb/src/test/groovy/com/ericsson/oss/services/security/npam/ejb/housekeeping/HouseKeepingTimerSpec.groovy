package com.ericsson.oss.services.security.npam.ejb.housekeeping

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.SpyImplementation
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.security.npam.ejb.database.availability.DatabaseAvailabilityChecker
import com.ericsson.oss.services.security.npam.ejb.job.housekeeping.HouseKeepingTimer
import com.ericsson.oss.services.security.npam.ejb.job.housekeeping.JobsHouseKeepingService
import com.ericsson.oss.services.security.npam.ejb.listener.MembershipListenerInterface
import com.ericsson.oss.services.security.npam.ejb.pib.NpamPibParameters
import spock.lang.Unroll

import javax.ejb.Timer
import javax.ejb.TimerService
import javax.inject.Inject

class HouseKeepingTimerSpec extends BaseSetupForTestSpecs {

    @Inject
    HouseKeepingTimer objUnderTest

    @Inject
    TimerService timerServiceMock

    @Inject
    Timer timerMock

    @SpyImplementation
    private JobsHouseKeepingService jobsHouseKeepingService

    @MockedImplementation
    private DatabaseAvailabilityChecker databaseAvailabilityCheckerMock

    @MockedImplementation
    private MembershipListenerInterface membershipListenerInterfaceMock

    @MockedImplementation
    private NpamPibParameters npamPibParameters

    def 'setupTimer'() {
        when: 'timer is running'
        objUnderTest.setupTimer()
        then: ' new timer is configured'
        1 * timerServiceMock.createSingleActionTimer(*_)
    }

    @Unroll
    def 'Timer triggerHouseKeepingOfJobsStartingFromTemplates operation is executed only when is master and Database is available '() {
        given:
            databaseAvailabilityCheckerMock.isAvailable() >> isDbAvailable
            membershipListenerInterfaceMock.isMaster() >> isMaster
            npamPibParameters.getNpamHouseKeepingDays() >> 180
        when: 'timer is running'
            objUnderTest.houseKeepingTimer(timerMock)
        then: ' send enableRemoteManagementEvent'
            count * jobsHouseKeepingService.triggerHouseKeepingOfJobsStartingFromTemplates(_)
            count * jobsHouseKeepingService.triggerHouseKeepingOfFiles(_)
        and: ' new timer is configured'
            1 * timerServiceMock.createSingleActionTimer(*_)
        where:
            isDbAvailable   |  isMaster   || count  |_
            true            |    true     || 1      |_
            true            |    false    || 0      |_
            false           |    true     || 0      |_
            false           |    false    || 0      |_
    }


    def 'Timer triggerHouseKeeping operation not executed when Exception occurs '() {
        given:
            databaseAvailabilityCheckerMock.isAvailable() >> true
            membershipListenerInterfaceMock.isMaster() >> { throw new RuntimeException()}
            npamPibParameters.getNpamHouseKeepingDays() >> 180
        when: 'timer is triggered'
            objUnderTest.houseKeepingTimer(timerMock)
        then: ''
            0 * jobsHouseKeepingService.triggerHouseKeepingOfJobsStartingFromTemplates(_)
            0 * jobsHouseKeepingService.triggerHouseKeepingOfFiles(_)
    }

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
            objUnderTest.toString()
        then:
            true
    }
}
