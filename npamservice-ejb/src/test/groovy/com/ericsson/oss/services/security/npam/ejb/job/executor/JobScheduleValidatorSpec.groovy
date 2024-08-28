package com.ericsson.oss.services.security.npam.ejb.job.executor

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.rule.SpyImplementation
import com.ericsson.oss.services.security.npam.api.exceptions.JobConfigurationException
import com.ericsson.oss.services.security.npam.api.exceptions.JobError
import com.ericsson.oss.services.security.npam.api.exceptions.JobExceptionFactory
import com.ericsson.oss.services.security.npam.api.job.modelentities.ExecMode
import com.ericsson.oss.services.security.npam.api.job.modelentities.Schedule
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.security.npam.ejb.job.util.JobScheduleValidator

class JobScheduleValidatorSpec extends BaseSetupForTestSpecs {

    @ObjectUnderTest
    private JobScheduleValidator objUnderTest

    @SpyImplementation
    JobExceptionFactory jobExceptionFactory

    def schedule = new HashMap<String, Object>()


    private static final int FIRST_EXECUTION_INDEX = 0;

    def setup() {
        runtimeDps.withTransactionBoundaries()
    }

    /*
     Some Validation Errors
     */

    def 'validateMainScheduleParameters  with schedule=null throw exception'() {
        given: 'schedule = null'
        Schedule schedule = null
        when: 'calculateNextScheduledTime'
        def date = objUnderTest.validateMainScheduleParameters(schedule)
        then: 'internally raised exception'
        1 * jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE,_)
        and:
        thrown(JobConfigurationException)
    }

    def 'validateMainScheduleParameters with execMode=null throw exception'() {
        given: 'execMode=null'
        Schedule schedule = new Schedule()
        schedule.setExecMode(null)
        when: 'calculateNextScheduledTime'
        def date = objUnderTest.validateMainScheduleParameters(schedule)
        then: 'internally raised exception'
        1 * jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE,_)
        and:
        thrown(JobConfigurationException)
    }

    def 'validateMainScheduleParameters  with ExecMode=MANUAL throw exception'() {
        given: 'execMode=MANUAL'
        Schedule schedule = new Schedule()
        schedule.setExecMode(ExecMode.MANUAL)
        when: 'calculateNextScheduledTime'
        def date = objUnderTest.validateMainScheduleParameters(schedule)
        then: 'internally raised exception'
        1 * jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_EXEC_MODE,_)
        and:
        thrown(JobConfigurationException)
    }

    def 'getAndValidateRepeatCount with repeatCount=null throw exception'() {
        given: 'repeatCount=null'
        Schedule schedule = new Schedule()
        when: 'calculateNextScheduledTime'
        def date = objUnderTest.getAndValidateRepeatCount(schedule)
        then: 'internally raised exception'
        1 * jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_REPEAT_COUNT,_)
        and:
        thrown(JobConfigurationException)
    }
}
