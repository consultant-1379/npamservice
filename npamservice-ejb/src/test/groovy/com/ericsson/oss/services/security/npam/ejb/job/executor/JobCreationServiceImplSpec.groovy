package com.ericsson.oss.services.security.npam.ejb.job.executor

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.rule.SpyImplementation
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants
import com.ericsson.oss.services.security.npam.api.exceptions.JobError
import com.ericsson.oss.services.security.npam.api.exceptions.JobExceptionFactory
import com.ericsson.oss.services.security.npam.api.interfaces.JobConfigurationService
import com.ericsson.oss.services.security.npam.api.job.modelentities.ExecMode
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJobTemplate
import com.ericsson.oss.services.security.npam.api.job.modelentities.Schedule
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.security.npam.ejb.job.util.DateFormatterUtil
import com.ericsson.oss.services.security.npam.ejb.message.NodePamQueueMessageSender
import org.joda.time.DateTime

import javax.inject.Inject

class JobCreationServiceImplSpec extends BaseSetupForTestSpecs {

    @ObjectUnderTest
    private JobCreationServiceImpl objUnderTest

    @SpyImplementation
    JobExceptionFactory jobExceptionFactory

    @MockedImplementation
    NodePamQueueMessageSender nodePamQueueMessageSender

    @Inject
    JobConfigurationService jobConfigurationService

    PersistenceObject jobTemplatePO
    def nodes = ['RadioNode01', 'RadioNode02']
    def owner = "owner"
    def jobTemplateName = "JobTemplateName"
    def jobType = JobType.ROTATE_NE_ACCOUNT_CREDENTIALS
    def schedule = new HashMap<String, Object>()


    private static final int FIRST_EXECUTION_INDEX = 0;

    def setup() {
        runtimeDps.withTransactionBoundaries()
    }

    /*
        Complete Flow
    */
    def 'createNewJob IMMEDIATE with success'() {
        given: 'a job template'
        schedule.execMode = "IMMEDIATE"  //JobExecMode.IMMEDIATE
        schedule.scheduleAttributes = [] //SCHEDULE_ATTRIBUTES
        List<Map<String, Object> > scheduleAttributes = new ArrayList<>()

        List<JobProperty> jobProperties = new ArrayList<>()
        JobProperty jobProperty = new JobProperty()
        jobProperty.key = "EMAIL_IDS"
        jobProperty.value = "a.b@c.d"
        jobProperties.add(jobProperty)

        jobTemplatePO = addNpamJobTemplate(jobTemplateName, owner, jobType, jobProperties,
                schedule , nodes, new ArrayList<String>(), new ArrayList<>())

        when: 'job creation from template is requested'
        objUnderTest.createNewJob(jobTemplatePO.getPoId())

        then:
            def attributes = jobConfigurationService.retrieveJob(2)
            attributes != null
            attributes.get(NPamJob.JOB_NAME) == "JobTemplateName"
            attributes.get(NPamJob.JOB_OWNER) == owner
            1 * nodePamQueueMessageSender.sendSubmitMainJobMessage(_)
    }

    def 'createNewJob SCHEDULED and periodic (WEEKLY) with success'() {
        given: 'a job template'
            schedule.execMode = "SCHEDULED"

            List<Map<String, Object>> scheduleAttributes = new ArrayList<>()
            Map<String, Object> startTimeProperty = new HashMap<>()
            startTimeProperty.name =  JobSchedulerConstants.SA_START_DATE

            startTimeProperty.value = "2022-10-14 03:00:00 GMT+0200"//new Date().toGMTString()
            scheduleAttributes.add(startTimeProperty)

            Map<String, Object> repeatTypeProperty = new HashMap<>()
            repeatTypeProperty.name = JobSchedulerConstants.SA_REPEAT_TYPE
            repeatTypeProperty.value = JobSchedulerConstants.REPEAT_TYPE_VALUE_WEEKLY
            scheduleAttributes.add(repeatTypeProperty)

            Map<String, Object> repeatOnProperty = new HashMap<>()
            repeatOnProperty.name = JobSchedulerConstants.SA_REPEAT_ON
            repeatOnProperty.value =  "2,3"
            scheduleAttributes.add(repeatOnProperty)

            Map<String, Object> repeatCountProperty = new HashMap<>()
            repeatCountProperty.name = JobSchedulerConstants.SA_REPEAT_COUNT
            repeatCountProperty.value = "3"
            scheduleAttributes.add(repeatCountProperty)

            schedule.scheduleAttributes = scheduleAttributes

            List<JobProperty> jobProperties = new ArrayList<>()
            JobProperty jobProperty = new JobProperty()
            jobProperty.key = "EMAIL_IDS"
            jobProperty.value = "a.b@c.d"
            jobProperties.add(jobProperty)

            jobTemplatePO = addNpamJobTemplate(jobTemplateName, owner, jobType, jobProperties, schedule, nodes, new ArrayList<String>(), new ArrayList<>())

        when: 'job creation from template is requested'
            objUnderTest.createNewJobIfNecessary((jobTemplatePO.getPoId()),2)

        then:
            def attributes = jobConfigurationService.retrieveJob(2)
            attributes != null
            attributes.get(NPamJob.JOB_NAME) == "JobTemplateName"
            attributes.get(NPamJob.JOB_OWNER) == owner
            0 * nodePamQueueMessageSender.sendSubmitMainJobMessage(_)
    }

    def 'createNewJob SCHEDULED and periodic (MONTHLY) with success'() {
        given: 'a job template'
        schedule.execMode = "SCHEDULED"

        List<Map<String, Object>> scheduleAttributes = new ArrayList<>()
        Map<String, Object> startTimeProperty = new HashMap<>()
        startTimeProperty.name =  JobSchedulerConstants.SA_START_DATE

        startTimeProperty.value = "2022-10-14 03:00:00 GMT+0200"//new Date().toGMTString()
        scheduleAttributes.add(startTimeProperty)

        Map<String, Object> repeatTypeProperty = new HashMap<>()
        repeatTypeProperty.name = JobSchedulerConstants.SA_REPEAT_TYPE
        repeatTypeProperty.value = JobSchedulerConstants.REPEAT_TYPE_VALUE_MONTHLY
        scheduleAttributes.add(repeatTypeProperty)

        Map<String, Object> repeatOnProperty = new HashMap<>()      // ?? SE c'e`, devo dare errore?
        repeatOnProperty.name = JobSchedulerConstants.SA_REPEAT_ON
        repeatOnProperty.value =  "2,3"
        scheduleAttributes.add(repeatOnProperty)

        Map<String, Object> repeatCountProperty = new HashMap<>()
        repeatCountProperty.name = JobSchedulerConstants.SA_REPEAT_COUNT
        repeatCountProperty.value = "3"
        scheduleAttributes.add(repeatCountProperty)

        schedule.scheduleAttributes = scheduleAttributes

        jobTemplatePO = addNpamJobTemplate(jobTemplateName, owner, jobType, null, schedule, nodes, new ArrayList<String>(), new ArrayList<>())

        when: 'job creation from template is requested'
        objUnderTest.createNewJob(jobTemplatePO.getPoId())

        then:
            def attributes = jobConfigurationService.retrieveJob(2)
            attributes != null
            attributes.get(NPamJob.JOB_NAME) == "JobTemplateName"
            attributes.get(NPamJob.JOB_OWNER) == owner
            0 * nodePamQueueMessageSender.sendSubmitMainJobMessage(_)
    }

    def 'createNewJob SCHEDULED and periodic (YEARLY) with success'() {
        given: 'a job template'
        schedule.execMode = "SCHEDULED"

        List<Map<String, Object>> scheduleAttributes = new ArrayList<>()
        Map<String, Object> startTimeProperty = new HashMap<>()
        startTimeProperty.name =  JobSchedulerConstants.SA_START_DATE

        startTimeProperty.value = "2022-10-14 03:00:00 GMT+0200"//new Date().toGMTString()
        scheduleAttributes.add(startTimeProperty)

        Map<String, Object> repeatTypeProperty = new HashMap<>()
        repeatTypeProperty.name = JobSchedulerConstants.SA_REPEAT_TYPE
        repeatTypeProperty.value = JobSchedulerConstants.REPEAT_TYPE_VALUE_YEARLY
        scheduleAttributes.add(repeatTypeProperty)

        Map<String, Object> repeatOnProperty = new HashMap<>()      // ?? SE c'e`, devo dare errore?
        repeatOnProperty.name = JobSchedulerConstants.SA_REPEAT_ON
        repeatOnProperty.value =  "2,3"
        scheduleAttributes.add(repeatOnProperty)

        Map<String, Object> repeatCountProperty = new HashMap<>()
        repeatCountProperty.name = JobSchedulerConstants.SA_REPEAT_COUNT
        repeatCountProperty.value = "3"
        scheduleAttributes.add(repeatCountProperty)

        schedule.scheduleAttributes = scheduleAttributes

        jobTemplatePO = addNpamJobTemplate(jobTemplateName, owner, jobType, null, schedule, nodes, new ArrayList<String>(), new ArrayList<>())

        when: 'job creation from template is requested'
        objUnderTest.createNewJob(jobTemplatePO.getPoId())

        then:
            def attributes = jobConfigurationService.retrieveJob(2)
            attributes != null
            attributes.get(NPamJob.JOB_NAME) == "JobTemplateName"
            attributes.get(NPamJob.JOB_OWNER) == owner
            0 * nodePamQueueMessageSender.sendSubmitMainJobMessage(_)
    }

    def 'createNewJob SCHEDULED and non periodic with success'() {
        given: 'a scheduled job template'
            schedule.execMode = "SCHEDULED"

            List<Map<String, Object>> scheduleAttributes = new ArrayList<>()
            Map<String, Object> startTimeProperty = new HashMap<>()
            startTimeProperty.name =  JobSchedulerConstants.SA_START_DATE

            startTimeProperty.value = "2022-10-14 03:00:00 GMT+0200"//new Date().toGMTString()
            scheduleAttributes.add(startTimeProperty)

            schedule.scheduleAttributes = scheduleAttributes

            jobTemplatePO = addNpamJobTemplate(jobTemplateName, owner, jobType, null, schedule, nodes, new ArrayList<String>(), new ArrayList<>())

        when: 'job creation from template is requested'
            objUnderTest.createNewJob(jobTemplatePO.getPoId())

        then:
            def attributes = jobConfigurationService.retrieveJob(2)
            attributes != null
            attributes.get(NPamJob.JOB_NAME) == "JobTemplateName"
            attributes.get(NPamJob.JOB_OWNER) == owner
            0 * nodePamQueueMessageSender.sendSubmitMainJobMessage(_)
    }

    def 'createNewJobIfNecessary SCHEDULED and non periodic with success'() {
        given: 'a scheduled job template'
        schedule.execMode = "SCHEDULED"

        List<Map<String, Object>> scheduleAttributes = new ArrayList<>()
        Map<String, Object> startTimeProperty = new HashMap<>()
        startTimeProperty.name =  JobSchedulerConstants.SA_START_DATE

        startTimeProperty.value = "2022-10-14 03:00:00 GMT+0200"//new Date().toGMTString()
        scheduleAttributes.add(startTimeProperty)

        schedule.scheduleAttributes = scheduleAttributes

        jobTemplatePO = addNpamJobTemplate(jobTemplateName, owner, jobType, null, schedule, nodes, new ArrayList<String>(), new ArrayList<>())

        when: 'job creation from template is requested'
        objUnderTest.createNewJobIfNecessary(jobTemplatePO.getPoId(), 2)

        then:
        jobConfigurationService.retrieveJob(2).isEmpty()
    }


    def 'createNewJob IMMEDIATE and non periodic (ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE) with success'() {
        given: 'an immediate job template'
        schedule.execMode = "IMMEDIATE"
        schedule.scheduleAttributes = new ArrayList<>()

        def jobtype = JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE

        jobTemplatePO = addNpamJobTemplate(jobTemplateName, owner, jobtype, null, schedule, nodes, new ArrayList<String>(), new ArrayList<>())

        when: 'job creation from template is requested'
        objUnderTest.createNewJob(jobTemplatePO.getPoId())

        then:
            def attributes = jobConfigurationService.retrieveJob(2)
            attributes != null
            attributes.get(NPamJob.JOB_NAME) == "JobTemplateName"
            attributes.get(NPamJob.JOB_OWNER) == owner
            1 * nodePamQueueMessageSender.sendSubmitMainJobMessage(_)
    }

    def 'createNewJob IMMEDIATE and non periodic (ROTATE_NE_ACCOUNT_CREDENTIALS) with success'() {
        given: 'an immediate job template'
        schedule.execMode = "IMMEDIATE"
        schedule.scheduleAttributes = new ArrayList<>()

        def jobtype = JobType.ROTATE_NE_ACCOUNT_CREDENTIALS

        jobTemplatePO = addNpamJobTemplate(jobTemplateName, owner, jobtype, null, schedule, nodes, new ArrayList<String>(), new ArrayList<>())

        when: 'job creation from template is requested'
        objUnderTest.createNewJob(jobTemplatePO.getPoId())

        then:
            def attributes = jobConfigurationService.retrieveJob(2)
            attributes != null
            attributes.get(NPamJob.JOB_NAME) == "JobTemplateName"
            attributes.get(NPamJob.JOB_OWNER) == owner
            1 * nodePamQueueMessageSender.sendSubmitMainJobMessage(_)
    }


    def "prepareMainJob of SCHEDULED expired job doesn't create a new job"() {
        given: 'a job template'
        schedule.execMode = "SCHEDULED"

        List<Map<String, Object>> scheduleAttributes = new ArrayList<>()

        Map<String, Object> startTimeProperty = new HashMap<>()
        startTimeProperty.name =  JobSchedulerConstants.SA_START_DATE
        startTimeProperty.value = "2022-10-14 03:00:00 GMT+0200"
        scheduleAttributes.add(startTimeProperty)

        Map<String, Object> repeatTypeProperty = new HashMap<>()
        //ScheduleProperty repeatTypeProperty = new ScheduleProperty()
        repeatTypeProperty.name = JobSchedulerConstants.SA_REPEAT_TYPE
        repeatTypeProperty.value = JobSchedulerConstants.REPEAT_TYPE_VALUE_WEEKLY
        scheduleAttributes.add(repeatTypeProperty)

        Map<String, Object> repeatOnProperty = new HashMap<>()
        repeatOnProperty.name = JobSchedulerConstants.SA_REPEAT_ON
        repeatOnProperty.value =  "2, 3"
        scheduleAttributes.add(repeatOnProperty)

        Map<String, Object> repeatCountProperty = new HashMap<>()
        repeatCountProperty.name = JobSchedulerConstants.SA_REPEAT_COUNT
        repeatCountProperty.value = "3"
        scheduleAttributes.add(repeatCountProperty)

        Map<String, Object> occurrences = new HashMap<>()
        occurrences.name =  JobSchedulerConstants.SA_OCCURRENCES
        occurrences.value = "5"
        scheduleAttributes.add(occurrences)

        Map<String, Object> endTimeProperty = new HashMap<>()
        endTimeProperty.name =  JobSchedulerConstants.SA_END_DATE
        endTimeProperty.value = "2022-10-20 03:00:00 GMT+0200"
        scheduleAttributes.add(endTimeProperty)

        schedule.scheduleAttributes = scheduleAttributes

        jobTemplatePO = addNpamJobTemplate(jobTemplateName, owner, jobType, null, schedule, nodes, new ArrayList<String>(), new ArrayList<>())

        when: 'job creation from template is requested'
        objUnderTest.prepareMainJob(jobTemplatePO.getPoId(), 2)

        then:
        jobConfigurationService.retrieveJob(2).isEmpty()
        0 * nodePamQueueMessageSender.sendSubmitMainJobMessage(_)
    }

    def "prepareMainJob of SCHEDULED job when occurrences are exceeded doesn't create a new job"() {
        given: 'a job template'
        schedule.execMode = "SCHEDULED"

        List<Map<String, Object>> scheduleAttributes = new ArrayList<>()

        Map<String, Object> startTimeProperty = new HashMap<>()
        startTimeProperty.name =  JobSchedulerConstants.SA_START_DATE
        startTimeProperty.value = "2022-10-14 03:00:00 GMT+0200"
        scheduleAttributes.add(startTimeProperty)

        Map<String, Object> repeatTypeProperty = new HashMap<>()
        repeatTypeProperty.name = JobSchedulerConstants.SA_REPEAT_TYPE
        repeatTypeProperty.value = JobSchedulerConstants.REPEAT_TYPE_VALUE_WEEKLY
        scheduleAttributes.add(repeatTypeProperty)

        Map<String, Object> repeatOnProperty = new HashMap<>()
        repeatOnProperty.name = JobSchedulerConstants.SA_REPEAT_ON
        repeatOnProperty.value =  "2, 3"
        scheduleAttributes.add(repeatOnProperty)

        Map<String, Object> repeatCountProperty = new HashMap<>()
        repeatCountProperty.name = JobSchedulerConstants.SA_REPEAT_COUNT
        repeatCountProperty.value = "3"
        scheduleAttributes.add(repeatCountProperty)

        Map<String, Object> occurrences = new HashMap<>()
        occurrences.name =  JobSchedulerConstants.SA_OCCURRENCES
        occurrences.value = "5"
        scheduleAttributes.add(occurrences)

        Map<String, Object> endTimeProperty = new HashMap<>()
        endTimeProperty.name =  JobSchedulerConstants.SA_END_DATE
        endTimeProperty.value = "2022-10-20 03:00:00 GMT+0200"
        scheduleAttributes.add(endTimeProperty)

        schedule.scheduleAttributes = scheduleAttributes

        jobTemplatePO = addNpamJobTemplate(jobTemplateName, owner, jobType, null, schedule, nodes, new ArrayList<String>(), new ArrayList<>())

        when: 'job creation from template is requested'
        objUnderTest.prepareMainJob(jobTemplatePO.getPoId(), 5)

        then:
        jobConfigurationService.retrieveJob(2).isEmpty()
        0 * nodePamQueueMessageSender.sendSubmitMainJobMessage(_)
    }

    def "prepareMainJob of SCHEDULED job that expires before the 2nd run doesn't create a new job"() {
        given: 'a job template'
        schedule.execMode = "SCHEDULED"

        List<Map<String, Object>> scheduleAttributes = new ArrayList<>()

        Map<String, Object> startTimeProperty = new HashMap<>()
        startTimeProperty.name =  JobSchedulerConstants.SA_START_DATE

        Date nowDate = new Date()
        String myStartDateString = DateFormatterUtil.convertDateToStringWithExplicitGMTString(nowDate)
        startTimeProperty.value = myStartDateString
        scheduleAttributes.add(startTimeProperty)

        Map<String, Object> repeatTypeProperty = new HashMap<>()
        repeatTypeProperty.name = JobSchedulerConstants.SA_REPEAT_TYPE
        repeatTypeProperty.value = JobSchedulerConstants.REPEAT_TYPE_VALUE_WEEKLY
        scheduleAttributes.add(repeatTypeProperty)

        Map<String, Object> repeatCountProperty = new HashMap<>()
        repeatCountProperty.name = JobSchedulerConstants.SA_REPEAT_COUNT
        repeatCountProperty.value = "1"
        scheduleAttributes.add(repeatCountProperty)

        Map<String, Object> endTimeProperty = new HashMap<>()
        endTimeProperty.name =  JobSchedulerConstants.SA_END_DATE
        Date endDate = (new DateTime(nowDate).plusDays(1)).toDate()
        String myEndDateString = DateFormatterUtil.convertDateToStringWithExplicitGMTString(endDate)
        endTimeProperty.value = myEndDateString
        scheduleAttributes.add(endTimeProperty)

        schedule.scheduleAttributes = scheduleAttributes

        List<JobProperty> jobProperties = new ArrayList<>()
        JobProperty jobProperty = new JobProperty()
        jobProperty.key = "EMAIL_IDS"
        jobProperty.value = "a.b@c.d"
        jobProperties.add(jobProperty)

        jobTemplatePO = addNpamJobTemplate(jobTemplateName, owner, jobType, jobProperties, schedule, nodes, new ArrayList<String>(), new ArrayList<>())

        when: 'job creation from template is requested'
        objUnderTest.prepareMainJob(jobTemplatePO.getPoId(), 2)

        then:
        jobConfigurationService.retrieveJob(2).isEmpty()
        0 * nodePamQueueMessageSender.sendSubmitMainJobMessage(_)
    }


    def 'createNewJob IMMEDIATE with startDate set receive exception'() {
        given: 'a job template'
        schedule.execMode = "IMMEDIATE"  //JobExecMode.IMMEDIATE
        schedule.scheduleAttributes = [] //SCHEDULE_ATTRIBUTES
        List<Map<String, Object> > scheduleAttributes = new ArrayList<>()
        Map<String, Object> startTimeProperty = new HashMap<>()
        startTimeProperty.name =  JobSchedulerConstants.SA_START_DATE
        startTimeProperty.value = "2022-10-14 03:00:00 GMT+0200"
        scheduleAttributes.add(startTimeProperty)
        schedule.scheduleAttributes = scheduleAttributes

        jobTemplatePO = addNpamJobTemplate(jobTemplateName, owner, jobType, null,
                schedule , nodes, new ArrayList<String>(), new ArrayList<>())

        when: 'job creation from template is requested'
        objUnderTest.createNewJob(jobTemplatePO.getPoId())

        then: 'internally raised exception'
        1 * jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_IMMEDIATE)
        then:
        thrown(Exception)
    }

    def 'createNewJob with wrong execMode set receive exception'() {
        given: 'a job template'
        schedule.execMode = "EXEC_MODE"
        schedule.scheduleAttributes = []
        List<Map<String, Object> > scheduleAttributes = new ArrayList<>()
        Map<String, Object> startTimeProperty = new HashMap<>()
        startTimeProperty.name =  JobSchedulerConstants.SA_START_DATE
        startTimeProperty.value = "2022-10-14 03:00:00 GMT+0200"
        scheduleAttributes.add(startTimeProperty)
        schedule.scheduleAttributes = scheduleAttributes

        jobTemplatePO = addNpamJobTemplate(jobTemplateName, owner, jobType, null,
                schedule , nodes, new ArrayList<String>(), new ArrayList<>())

        when: 'job creation from template is requested'
        objUnderTest.createNewJob(jobTemplatePO.getPoId())

        then:
        thrown(IllegalArgumentException)
    }

    def "prepareMainJob of SCHEDULED job with SA_REPEAT_COUNT=null and receive Exception"() {
        given: 'a job template'
        schedule.execMode = "SCHEDULED"

        List<Map<String, Object>> scheduleAttributes = new ArrayList<>()

        Map<String, Object> startTimeProperty = new HashMap<>()
        startTimeProperty.name =  JobSchedulerConstants.SA_START_DATE
        startTimeProperty.value = "2022-10-14 03:00:00 GMT+0200"//new Date().toGMTString()
        scheduleAttributes.add(startTimeProperty)

        Map<String, Object> repeatTypeProperty = new HashMap<>()
        repeatTypeProperty.name = JobSchedulerConstants.SA_REPEAT_TYPE
        repeatTypeProperty.value = JobSchedulerConstants.REPEAT_TYPE_VALUE_WEEKLY
        scheduleAttributes.add(repeatTypeProperty)

        Map<String, Object> occurrences = new HashMap<>()
        occurrences.name =  JobSchedulerConstants.SA_OCCURRENCES
        occurrences.value = "5"
        scheduleAttributes.add(occurrences)

        Map<String, Object> endTimeProperty = new HashMap<>()
        endTimeProperty.name =  JobSchedulerConstants.SA_END_DATE
        endTimeProperty.value = "2022-10-20 03:00:00 GMT+0200"
        scheduleAttributes.add(endTimeProperty)

        schedule.scheduleAttributes = scheduleAttributes

        jobTemplatePO = addNpamJobTemplate(jobTemplateName, owner, jobType, null, schedule, nodes, new ArrayList<String>(), new ArrayList<>())

        when: 'job creation from template is requested'
        objUnderTest.prepareMainJob(jobTemplatePO.getPoId(), 5)

        then:
          1 * jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_NON_PERIODIC,_)
        and: 'raise exception'
          thrown(Exception)
    }

    def "prepareMainJob of SCHEDULED job with wrong SA_REPEAT_TYPE and receive Exception"() {
        given: 'a job template'
        schedule.execMode = "SCHEDULED"

        List<Map<String, Object>> scheduleAttributes = new ArrayList<>()

        Map<String, Object> startTimeProperty = new HashMap<>()
        startTimeProperty.name =  JobSchedulerConstants.SA_START_DATE
        startTimeProperty.value = "2022-10-14 03:00:00 GMT+0200"//new Date().toGMTString()
        scheduleAttributes.add(startTimeProperty)

        Map<String, Object> repeatTypeProperty = new HashMap<>()
        repeatTypeProperty.name = JobSchedulerConstants.SA_REPEAT_TYPE
        repeatTypeProperty.value = "wrongRepeatType"
        scheduleAttributes.add(repeatTypeProperty)

        Map<String, Object> repeatCountProperty = new HashMap<>()
        repeatCountProperty.name = JobSchedulerConstants.SA_REPEAT_COUNT
        repeatCountProperty.value = "3"
        scheduleAttributes.add(repeatCountProperty)

        schedule.scheduleAttributes = scheduleAttributes

        jobTemplatePO = addNpamJobTemplate(jobTemplateName, owner, jobType, null, schedule, nodes, new ArrayList<String>(), new ArrayList<>())

        when: 'job creation from template is requested'
        objUnderTest.prepareMainJob(jobTemplatePO.getPoId(), 5)

        then:
        1 * jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_REPEAT_TYPE,_)
        and: 'raise exception'
        thrown(Exception)
    }

    def "prepareMainJob of SCHEDULED job with wrong schedule params will receive Exception"(start_date, repeat_count, occurrence, end_date, jobError) {
        given: 'a job template'
        schedule.execMode = "SCHEDULED"

        List<Map<String, Object>> scheduleAttributes = new ArrayList<>()

        Map<String, Object> startTimeProperty = new HashMap<>()
        startTimeProperty.name =  JobSchedulerConstants.SA_START_DATE
        startTimeProperty.value = start_date
        scheduleAttributes.add(startTimeProperty)

        Map<String, Object> repeatTypeProperty = new HashMap<>()
        repeatTypeProperty.name = JobSchedulerConstants.SA_REPEAT_TYPE
        repeatTypeProperty.value = JobSchedulerConstants.REPEAT_TYPE_VALUE_WEEKLY
        scheduleAttributes.add(repeatTypeProperty)

        Map<String, Object> repeatCountProperty = new HashMap<>()
        repeatCountProperty.name = JobSchedulerConstants.SA_REPEAT_COUNT
        repeatCountProperty.value = repeat_count
        scheduleAttributes.add(repeatCountProperty)

        Map<String, Object> occurrences = new HashMap<>()
        occurrences.name =  JobSchedulerConstants.SA_OCCURRENCES
        occurrences.value = occurrence
        scheduleAttributes.add(occurrences)

        Map<String, Object> endDateProperty = new HashMap<>()
        endDateProperty.name =  JobSchedulerConstants.SA_END_DATE
        endDateProperty.value = end_date
        scheduleAttributes.add(endDateProperty)

        schedule.scheduleAttributes = scheduleAttributes

        jobTemplatePO = addNpamJobTemplate(jobTemplateName, owner, jobType, null, schedule, nodes, new ArrayList<String>(), new ArrayList<>())

        when: 'job creation from template is requested'
        objUnderTest.prepareMainJob(jobTemplatePO.getPoId(), 5)

        then:
        1 * jobExceptionFactory.createJobConfigurationException(jobError,_)
        and: 'raise exception'
        thrown(Exception)

        where:
        start_date                      | repeat_count       | occurrence    | end_date                       | jobError |_
        "2022-10-14 03:00:00 GMT+0200"  | "0"                | "5"           | "2022-10-20 03:00:00 GMT+0200" | JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_REPEAT_COUNT  | _
        "2022-10-14 03:00:00 GMT+0200"  | "wrongRepeatcount" | "5"           | "2022-10-20 03:00:00 GMT+0200" | JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_REPEAT_COUNT  | _
        "wrongDate"                     | "1"                | "5"           | "2022-10-20 03:00:00 GMT+0200" | JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_START_DATE    | _
        "2022-10-144 03:00:00 GMT+0200" | "1"                | "5"           | "2022-10-20 03:00:00 GMT+0200" | JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_START_DATE    | _
        "2022-10-14 03:00:00 GMT+0200"  | "1"                | "0"           | "2022-10-20 03:00:00 GMT+0200" | JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_OCCURRENCES  | _
        "2022-10-14 03:00:00 GMT+0200"  | "1"                | "wrongOccurr" | "2022-10-20 03:00:00 GMT+0200" | JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_OCCURRENCES  | _
        "2022-10-14 03:00:00 GMT+0200"  | "1"                | "5"           | "wrongDate"                    | JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_END_DATE  | _
        "2022-10-14 03:00:00 GMT+0200"  | "1"                | "5"           | "2022-10-20-03:00:00 GMT+0200" | JobError.CONFIGURATION_ERROR_WRONG_SCHEDULE_END_DATE  | _
    }

    def 'createNewJob with wrong templateId Exception'() {
        given: 'a job template'
            schedule.execMode = "IMMEDIATE"
            schedule.scheduleAttributes = null
            jobTemplatePO = addNpamJobTemplate(jobTemplateName, owner, jobType, null, schedule, nodes, new ArrayList<String>(), new ArrayList<>())
            long wrongJobTemplatePoId = jobTemplatePO.getPoId()+1

        when: 'job creation from template is requested'
            objUnderTest.createNewJob(wrongJobTemplatePoId)

        then:
            1 * jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_NOT_FOUND_TEMPLATE_ID, jobTemplatePO.getPoId()+1)
        and: 'raise exception'
            thrown(Exception)
    }

    def 'haveToCreateNewMainJob returns false if job is IMMEDIATE'() {
        given: 'an NPAMJobTemplate'
            NPamJobTemplate nPamJobTemplate = new NPamJobTemplate()
            def schedule = new Schedule()
            schedule.setExecMode(ExecMode.IMMEDIATE)
            nPamJobTemplate.setMainSchedule(schedule)

        when: 'job creation from template is requested'
            def ret = objUnderTest.haveToCreateNewMainJob(nPamJobTemplate, 2)

        then:
            !ret
    }
}
