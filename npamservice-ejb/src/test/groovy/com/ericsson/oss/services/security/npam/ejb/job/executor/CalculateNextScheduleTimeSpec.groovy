package com.ericsson.oss.services.security.npam.ejb.job.executor

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.rule.SpyImplementation
import com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants
import com.ericsson.oss.services.security.npam.api.exceptions.JobConfigurationException
import com.ericsson.oss.services.security.npam.api.exceptions.JobError
import com.ericsson.oss.services.security.npam.api.exceptions.JobExceptionFactory
import com.ericsson.oss.services.security.npam.api.job.modelentities.*
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.security.npam.ejb.job.util.DateRecorderUtil
import org.joda.time.DateTime
import spock.lang.Shared
import spock.lang.Unroll
import static com.ericsson.oss.services.security.npam.ejb.job.util.DateFormatterUtil.JODA_DATE_FORMAT_PATTERN_WITH_TIMEZONE
import java.text.SimpleDateFormat

class CalculateNextScheduleTimeSpec extends BaseSetupForTestSpecs {

    @ObjectUnderTest
    private JobCreationServiceImpl objUnderTest

    @MockedImplementation
    DateRecorderUtil dateRecorderUtil;

    @SpyImplementation    //this annotation allow to spy real object exection
    JobExceptionFactory jobExceptionFactory

    def listOfDates = new ArrayList()
    def setup() {
        runtimeDps.withTransactionBoundaries()
        listOfDates.clear()
        dateRecorderUtil.logDate(_ as DateTime) >> { args ->
            DateTime dateTime = args[0]
            listOfDates.add(convertDateToStringWithGMT(dateTime.toDate()))
        }
        dateRecorderUtil.logDate(_ as Date) >> { args ->
            Date date = args[0]
            listOfDates.add(convertDateToStringWithGMT(date))
        }
    }

    @Shared TimeZone dtz = null
    def setupSpec() {
        //Memo original timezone
        dtz = TimeZone.getDefault()
        System.out.println("Original TimeZone="+TimeZone.getDefault().getID());

        //Force timezone to Europe/Rome
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"));
        System.out.println("Force TimeZone="+TimeZone.getDefault().getID());
    }

    def cleanupSpec() {
        //Restore original timezone
        TimeZone.setDefault(dtz);
        System.out.println("Restore original TimeZone="+TimeZone.getDefault().getID());
    }

    def 'calculateNextScheduledTime with ExecMode=MANUAL throw exception'() {
        given:
            Schedule schedule = new Schedule()
            schedule.setExecMode(ExecMode.MANUAL)
        when: 'calculateNextScheduledTime'
            def date = objUnderTest.calculateNextScheduledTime(schedule, 1)
        then: 'internally raised exception'
            1 * jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_EXEC_MODE,_)
        and:
            thrown(JobConfigurationException)
    }

    def 'computeNextDateFromBaseDate with repeatType=null throw exception'() {
        given: 'repeatType = null'
            String repeatType = null
        when: 'computeNextDateFromBaseDate'
           objUnderTest.computeNextDateFromBaseDate(new DateTime(), repeatType, null, 1)
        then: 'internally raised exception'
            1 * jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_REPEAT_TYPE,_)
        and:
            thrown(JobConfigurationException)
    }

    /*
        SCHEDULED non periodic (SA_REPEAT_TYPE or SA_REPEAT_COUNT == null)
* */
    @Unroll
    def 'calculateNextScheduledTime IMMEDIATE dateString=#dateString, nextExecutionInde=#nextExecutionIndex with success'(nextExecutionIndex) {
        given:
            Schedule schedule = new Schedule()
            schedule.setExecMode(ExecMode.IMMEDIATE)
            Date currentDate = new Date()
        when: 'calculateNextScheduledTime'
            def date = objUnderTest.calculateNextScheduledTime(schedule, nextExecutionIndex)
        then: 'internally algo produces an expected sequence of date'
            assert listOfDates.size() == 1
            date.getTime() >= currentDate.getTime()
        where:
        nextExecutionIndex | _
        1                  | _
        2                  | _
        3                  | _
    }

    /*
            SCHEDULED non periodic (SA_REPEAT_TYPE or SA_REPEAT_COUNT == null)
    * */
    @Unroll
    def 'calculateNextScheduledTime SCHEDULED and non periodic dateString=#dateString, nextExecutionInde=#nextExecutionIndex with success'(dateString, nextExecutionIndex, expectedDates) {
        given:
            Schedule schedule = new Schedule()
            schedule.setExecMode(ExecMode.SCHEDULED)

            ArrayList<ScheduleProperty> schedulePropertyList = new ArrayList()
            schedule.setScheduleAttributes(schedulePropertyList)

            final ScheduleProperty scheduleProperty = new ScheduleProperty();
            scheduleProperty.setName(JobSchedulerConstants.SA_START_DATE);
            scheduleProperty.setValue(dateString);
            schedulePropertyList.add(scheduleProperty)
        when: 'calculateNextScheduledTime'
            def date = objUnderTest.calculateNextScheduledTime(schedule, nextExecutionIndex)
        then: 'internally algo produces an expected sequence of date'
            assert listOfDates.size() == 1
            listOfDatesContainsExpectedDates(listOfDates, expectedDates)
        where:
        dateString                     | nextExecutionIndex | expectedDates                   | _
        "2022-10-14 00:00:00 GMT+0200" | 1                  | [ "2022-10-14 00:00:00 +0200" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 1                  | [ "2022-10-22 18:00:00 +0200" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 1                  | [ "2022-10-22 06:00:00 +0200" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 1                  | [ "2023-02-10 12:47:15 +0100" ] | _

        "2022-10-14 00:00:00 GMT+0200" | 2                  | [ "2022-10-14 00:00:00 +0200" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 2                  | [ "2022-10-22 18:00:00 +0200" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 2                  | [ "2022-10-22 06:00:00 +0200" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 2                  | [ "2023-02-10 12:47:15 +0100" ] | _
    }

    /*
            SCHEDULED periodic (SA_REPEAT_TYPE and SA_REPEAT_COUNT != null)
    * */
    @Unroll
    def 'calculateNextScheduledTime SCHEDULED and periodic WEEKLY dateString=#dateString, nextExecutionInde=#nextExecutionIndex, repeatCount=#repeatCount with success'(dateString, nextExecutionIndex, repeatCount, expectedDates) {
        given:
            Schedule schedule = new Schedule()
            schedule.setExecMode(ExecMode.SCHEDULED)

            ArrayList<ScheduleProperty> schedulePropertyList = new ArrayList()
            schedule.setScheduleAttributes(schedulePropertyList)

            final ScheduleProperty scheduleProperty = new ScheduleProperty();
            scheduleProperty.setName(JobSchedulerConstants.SA_START_DATE);
            scheduleProperty.setValue(dateString);
            schedulePropertyList.add(scheduleProperty)

            scheduleProperty = new ScheduleProperty();
            scheduleProperty.setName(JobSchedulerConstants.SA_REPEAT_TYPE);
            scheduleProperty.setValue(JobSchedulerConstants.REPEAT_TYPE_VALUE_WEEKLY);
            schedulePropertyList.add(scheduleProperty)

            if (repeatCount != null) {
                scheduleProperty = new ScheduleProperty();
                scheduleProperty.setName(JobSchedulerConstants.SA_REPEAT_COUNT);
                scheduleProperty.setValue(repeatCount);
                schedulePropertyList.add(scheduleProperty)
            }
        when: 'calculateNextScheduledTime'
            def date = objUnderTest.calculateNextScheduledTime(schedule, nextExecutionIndex)
        then: 'internally algo produces an expected sequence of date'
             if (nextExecutionIndex == 1) {
                 assert listOfDates.size() == 1
             }
             listOfDatesContainsExpectedDates(listOfDates, expectedDates)
        and: 'calculated date is > now'
             if (nextExecutionIndex > 1) {
               assert date.getTime() > System.currentTimeMillis()
             }
        where:
        dateString                     | nextExecutionIndex | repeatCount | expectedDates                  | _
        //when nextExecutionIndex = 1 we always expect dateString
        "2022-10-14 00:00:00 GMT+0200" | 1                  | "1"         | [ "2022-10-14 00:00:00 +0200" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 1                  | "1"         | [ "2022-10-22 18:00:00 +0200" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 1                  | "1"         | [ "2022-10-22 06:00:00 +0200" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 1                  | "1"         | [ "2023-02-10 12:47:15 +0100" ] | _

        "2022-10-14 00:00:00 GMT+0200" | 1                  | "2"         | [ "2022-10-14 00:00:00 +0200" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 1                  | "2"         | [ "2022-10-22 18:00:00 +0200" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 1                  | "2"         | [ "2022-10-22 06:00:00 +0200" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 1                  | "2"         | [ "2023-02-10 12:47:15 +0100" ] | _

        "2022-10-14 00:00:00 GMT+0200" | 1                  | "4"         | [ "2022-10-14 00:00:00 +0200" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 1                  | "4"         | [ "2022-10-22 18:00:00 +0200" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 1                  | "4"         | [ "2022-10-22 06:00:00 +0200" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 1                  | "4"         | [ "2023-02-10 12:47:15 +0100" ] | _

        //when nextExecutionIndex > 1 we always expect a sequence of date
        "2022-10-14 00:00:00 GMT+0200" | 2                  | "1"         | [ "2022-10-14 00:00:00 +0200", "2022-10-21 00:00:00 +0200", "2022-10-28 00:00:00 +0200", "2022-11-04 00:00:00 +0100" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 2                  | "1"         | [ "2022-10-22 18:00:00 +0200", "2022-10-29 18:00:00 +0200", "2022-11-05 18:00:00 +0100", "2022-11-12 18:00:00 +0100" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 2                  | "1"         | [ "2022-10-22 06:00:00 +0200", "2022-10-29 06:00:00 +0200", "2022-11-05 06:00:00 +0100", "2022-11-12 06:00:00 +0100" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 2                  | "1"         | [ "2023-02-10 12:47:15 +0100", "2023-02-17 12:47:15 +0100" ] | _

        "2022-10-14 00:00:00 GMT+0200" | 2                  | "2"         | [ "2022-10-14 00:00:00 +0200", "2022-10-28 00:00:00 +0200", "2022-11-11 00:00:00 +0100", "2022-11-25 00:00:00 +0100" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 2                  | "2"         | [ "2022-10-22 18:00:00 +0200", "2022-11-05 18:00:00 +0100", "2022-11-19 18:00:00 +0100", "2022-12-03 18:00:00 +0100" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 2                  | "2"         | [ "2022-10-22 06:00:00 +0200", "2022-11-05 06:00:00 +0100", "2022-11-19 06:00:00 +0100", "2022-12-03 06:00:00 +0100" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 2                  | "2"         | [ "2023-02-10 12:47:15 +0100", "2023-02-24 12:47:15 +0100" ] | _

        "2022-10-14 00:00:00 GMT+0200" | 2                  | "4"         | [ "2022-10-14 00:00:00 +0200", "2022-11-11 00:00:00 +0100", "2022-12-09 00:00:00 +0100", "2023-01-06 00:00:00 +0100" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 2                  | "4"         | [ "2022-10-22 18:00:00 +0200", "2022-11-19 18:00:00 +0100", "2022-12-17 18:00:00 +0100", "2023-01-14 18:00:00 +0100" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 2                  | "4"         | [ "2022-10-22 06:00:00 +0200", "2022-11-19 06:00:00 +0100", "2022-12-17 06:00:00 +0100", "2023-01-14 06:00:00 +0100" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 2                  | "4"         | [ "2023-02-10 12:47:15 +0100", "2023-03-10 12:47:15 +0100" ] | _
    }

    @Unroll
    def 'calculateNextScheduledTime SCHEDULED and periodic MONTHLY dateString=#dateString, nextExecutionInde=#nextExecutionIndex, repeatCount=#repeatCount with success'(dateString, nextExecutionIndex, repeatCount, expectedDates) {
        given:
            Schedule schedule = new Schedule()
            schedule.setExecMode(ExecMode.SCHEDULED)

            ArrayList<ScheduleProperty> schedulePropertyList = new ArrayList()
            schedule.setScheduleAttributes(schedulePropertyList)

            final ScheduleProperty scheduleProperty = new ScheduleProperty();
            scheduleProperty.setName(JobSchedulerConstants.SA_START_DATE);
            scheduleProperty.setValue(dateString);
            schedulePropertyList.add(scheduleProperty)

            scheduleProperty = new ScheduleProperty();
            scheduleProperty.setName(JobSchedulerConstants.SA_REPEAT_TYPE);
            scheduleProperty.setValue(JobSchedulerConstants.REPEAT_TYPE_VALUE_MONTHLY);
            schedulePropertyList.add(scheduleProperty)

            if (repeatCount != null) {
                scheduleProperty = new ScheduleProperty();
                scheduleProperty.setName(JobSchedulerConstants.SA_REPEAT_COUNT);
                scheduleProperty.setValue(repeatCount);
                schedulePropertyList.add(scheduleProperty)
            }

        when: 'calculateNextScheduledTime'
            def date = objUnderTest.calculateNextScheduledTime(schedule, nextExecutionIndex)
        then: 'internally algo produces an expected sequence of date'
            if (nextExecutionIndex == 1) {
                assert listOfDates.size() == 1
            }
            listOfDatesContainsExpectedDates(listOfDates, expectedDates)
        and: 'calculated date is > now'
            if (nextExecutionIndex > 1) {
                assert date.getTime() > System.currentTimeMillis()
            }
        where:
        dateString                     | nextExecutionIndex | repeatCount | expectedDates                  | _
        //when nextExecutionIndex = 1 we always expect dateString
        "2022-10-14 00:00:00 GMT+0200" | 1                  | "1"         | [ "2022-10-14 00:00:00 +0200" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 1                  | "1"         | [ "2022-10-22 18:00:00 +0200" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 1                  | "1"         | [ "2022-10-22 06:00:00 +0200" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 1                  | "1"         | [ "2023-02-10 12:47:15 +0100" ] | _

        "2022-10-14 00:00:00 GMT+0200" | 1                  | "2"         | [ "2022-10-14 00:00:00 +0200" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 1                  | "2"         | [ "2022-10-22 18:00:00 +0200" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 1                  | "2"         | [ "2022-10-22 06:00:00 +0200" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 1                  | "2"         | [ "2023-02-10 12:47:15 +0100" ] | _

        "2022-10-14 00:00:00 GMT+0200" | 1                  | "3"         | [ "2022-10-14 00:00:00 +0200" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 1                  | "3"         | [ "2022-10-22 18:00:00 +0200" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 1                  | "3"         | [ "2022-10-22 06:00:00 +0200" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 1                  | "3"         | [ "2023-02-10 12:47:15 +0100" ] | _

        //when nextExecutionIndex > 1 we always expect a sequence of date
        "2022-10-14 00:00:00 GMT+0200" | 2                  | "1"         | [ "2022-10-14 00:00:00 +0200", "2022-11-14 00:00:00 +0100", "2022-12-14 00:00:00 +0100", "2023-01-14 00:00:00 +0100" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 2                  | "1"         | [ "2022-10-22 18:00:00 +0200", "2022-11-22 18:00:00 +0100", "2022-12-22 18:00:00 +0100", "2023-01-22 18:00:00 +0100" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 2                  | "1"         | [ "2022-10-22 06:00:00 +0200", "2022-11-22 06:00:00 +0100", "2022-12-22 06:00:00 +0100", "2023-01-22 06:00:00 +0100" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 2                  | "1"         | [ "2023-02-10 12:47:15 +0100", "2023-03-10 12:47:15 +0100" ] | _

        "2022-10-14 00:00:00 GMT+0200" | 2                  | "2"         | [ "2022-10-14 00:00:00 +0200", "2022-12-14 00:00:00 +0100", "2023-02-14 00:00:00 +0100" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 2                  | "2"         | [ "2022-10-22 18:00:00 +0200", "2022-12-22 18:00:00 +0100", "2023-02-22 18:00:00 +0100" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 2                  | "2"         | [ "2022-10-22 06:00:00 +0200", "2022-12-22 06:00:00 +0100", "2023-02-22 06:00:00 +0100" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 2                  | "2"         | [ "2023-02-10 12:47:15 +0100", "2023-04-10 12:47:15 +0200" ] | _

        "2022-10-14 00:00:00 GMT+0200" | 2                  | "3"         | [ "2022-10-14 00:00:00 +0200", "2023-01-14 00:00:00 +0100", "2023-04-14 00:00:00 +0200" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 2                  | "3"         | [ "2022-10-22 18:00:00 +0200", "2023-01-22 18:00:00 +0100", "2023-04-22 18:00:00 +0200" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 2                  | "3"         | [ "2022-10-22 06:00:00 +0200", "2023-01-22 06:00:00 +0100", "2023-04-22 06:00:00 +0200" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 2                  | "3"         | [ "2023-02-10 12:47:15 +0100", "2023-05-10 12:47:15 +0200" ] | _
        "2500-02-10 12:47:15 GMT+0100" | 2                  | "3"         | [ "2500-02-10 12:47:15 +0100" ] | _
    }

    @Unroll
    def 'calculateNextScheduledTime SCHEDULED and periodic YEARLY dateString=#dateString, nextExecutionInde=#nextExecutionIndex, repeatCount=#repeatCount with success'(dateString, nextExecutionIndex, repeatCount, expectedDates) {
        given:
            Schedule schedule = new Schedule()
            schedule.setExecMode(ExecMode.SCHEDULED)

            ArrayList<ScheduleProperty> schedulePropertyList = new ArrayList()
            schedule.setScheduleAttributes(schedulePropertyList)

            final ScheduleProperty scheduleProperty = new ScheduleProperty();
            scheduleProperty.setName(JobSchedulerConstants.SA_START_DATE);
            scheduleProperty.setValue(dateString);
            schedulePropertyList.add(scheduleProperty)

            scheduleProperty = new ScheduleProperty();
            scheduleProperty.setName(JobSchedulerConstants.SA_REPEAT_TYPE);
            scheduleProperty.setValue(JobSchedulerConstants.REPEAT_TYPE_VALUE_YEARLY);
            schedulePropertyList.add(scheduleProperty)

            scheduleProperty = new ScheduleProperty();
            scheduleProperty.setName(JobSchedulerConstants.SA_REPEAT_COUNT);
            scheduleProperty.setValue(repeatCount);
            schedulePropertyList.add(scheduleProperty)

        when: 'calculateNextScheduledTime'
            def date = objUnderTest.calculateNextScheduledTime(schedule, nextExecutionIndex)
        then: 'internally algo produces an expected sequence of date'
            if (nextExecutionIndex == 1) {
                assert listOfDates.size() == 1
            }
            listOfDatesContainsExpectedDates(listOfDates, expectedDates)
        and: 'calculated date is > now'
            if (nextExecutionIndex > 1) {
                assert date.getTime() > System.currentTimeMillis()
            }
        where:
        dateString                     | nextExecutionIndex | repeatCount | expectedDates                  | _
        //when nextExecutionIndex = 1 we always expect dateString
        "2022-10-14 00:00:00 GMT+0200" | 1                  | "1"         | [ "2022-10-14 00:00:00 +0200" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 1                  | "1"         | [ "2022-10-22 18:00:00 +0200" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 1                  | "1"         | [ "2022-10-22 06:00:00 +0200" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 1                  | "1"         | [ "2023-02-10 12:47:15 +0100" ] | _

        "2022-10-14 00:00:00 GMT+0200" | 1                  | "2"         | [ "2022-10-14 00:00:00 +0200" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 1                  | "2"         | [ "2022-10-22 18:00:00 +0200" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 1                  | "2"         | [ "2022-10-22 06:00:00 +0200" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 1                  | "2"         | [ "2023-02-10 12:47:15 +0100" ] | _

        "2022-10-14 00:00:00 GMT+0200" | 1                  | "3"         | [ "2022-10-14 00:00:00 +0200" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 1                  | "3"         | [ "2022-10-22 18:00:00 +0200" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 1                  | "3"         | [ "2022-10-22 06:00:00 +0200" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 1                  | "3"         | [ "2023-02-10 12:47:15 +0100" ] | _

        //when nextExecutionIndex > 1 we always expect a sequence of date
        "2022-10-14 00:00:00 GMT+0200" | 2                  | "1"         | [ "2022-10-14 00:00:00 +0200", "2023-10-14 00:00:00 +0200" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 2                  | "1"         | [ "2022-10-22 18:00:00 +0200", "2023-10-22 18:00:00 +0200" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 2                  | "1"         | [ "2022-10-22 06:00:00 +0200", "2023-10-22 06:00:00 +0200" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 2                  | "1"         | [ "2023-02-10 12:47:15 +0100", "2024-02-10 12:47:15 +0100" ] | _

        "2022-10-14 00:00:00 GMT+0200" | 2                  | "2"         | [ "2022-10-14 00:00:00 +0200", "2024-10-14 00:00:00 +0200" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 2                  | "2"         | [ "2022-10-22 18:00:00 +0200", "2024-10-22 18:00:00 +0200" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 2                  | "2"         | [ "2022-10-22 06:00:00 +0200", "2024-10-22 06:00:00 +0200" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 2                  | "2"         | [ "2023-02-10 12:47:15 +0100", "2025-02-10 12:47:15 +0100" ] | _

        "2022-10-14 00:00:00 GMT+0200" | 2                  | "3"         | [ "2022-10-14 00:00:00 +0200", "2025-10-14 00:00:00 +0200" ] | _
        "2022-10-22 20:00:00 GMT+0400" | 2                  | "3"         | [ "2022-10-22 18:00:00 +0200", "2025-10-22 18:00:00 +0200" ] | _
        "2022-10-22 10:00:00 GMT+0600" | 2                  | "3"         | [ "2022-10-22 06:00:00 +0200", "2025-10-22 06:00:00 +0200" ] | _
        "2023-02-10 12:47:15 GMT+0100" | 2                  | "3"         | [ "2023-02-10 12:47:15 +0100", "2026-02-10 12:47:15 +0100" ] | _
    }


    private void listOfDatesContainsExpectedDates(List listOfDates, List expectedDates) {
        for (int i=0; i<expectedDates.size(); i++) {
            assert listOfDates[i] == expectedDates[i]
        }
    }


    private String convertDateToStringWithGMT(Date date) throws Exception {
        try {
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(JODA_DATE_FORMAT_PATTERN_WITH_TIMEZONE);
            return simpleDateFormat.format(date);
        } catch (Exception e) {
            throw e;
        }
    }

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
            objUnderTest.toString()
        then:
            true
    }
}
