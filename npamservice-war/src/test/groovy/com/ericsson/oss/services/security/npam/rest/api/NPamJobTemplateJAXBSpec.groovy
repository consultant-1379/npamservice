package com.ericsson.oss.services.security.npam.rest.api

import com.ericsson.oss.services.security.npam.api.cal.CALRecorderDTO

import static com.ericsson.oss.services.security.npam.api.job.modelentities.JobType.DETACH_NE_ACCOUNT

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage
import com.ericsson.oss.services.security.npam.api.job.modelentities.ExecMode
import com.ericsson.oss.services.security.npam.api.job.modelentities.NEInfo
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJobTemplate
import com.ericsson.oss.services.security.npam.api.job.modelentities.Schedule

class NPamJobTemplateJAXBSpec extends CdiSpecification {

    @ObjectUnderTest
    private NPAMJobTemplateJAXB objUnderTest


    def 'run constructor NPamJobTemplateJAXB'() {
        given: 'an NPamJobTemplate object'
        def npamJobTemplate = new NPamJobTemplate()
        npamJobTemplate.setName("jobName")
        npamJobTemplate.setDescription("This is a NPAM job template")
        npamJobTemplate.setJobType(DETACH_NE_ACCOUNT)
        npamJobTemplate.setOwner("Administrator")
        npamJobTemplate.setJobProperties(new ArrayList())
        npamJobTemplate.setMainSchedule(new Schedule())
        when: 'call NPAMJobTemplateJAXB constructor'
        def nPAMJobTemplateJAXB = new NPAMJobTemplateJAXB(npamJobTemplate)
        then : 'the parameters are correctly filled '
        nPAMJobTemplateJAXB.getName().equals("jobName")
        nPAMJobTemplateJAXB.getDescription().equals("This is a NPAM job template")
        nPAMJobTemplateJAXB.getJobType().equals(DETACH_NE_ACCOUNT)
        nPAMJobTemplateJAXB.getOwner().equals("Administrator")
        nPAMJobTemplateJAXB.getJobProperties().isEmpty()
    }

    def 'run constructor of NPamJobJAXB'() {
        given: 'an NPAMJobTemplateJAXB'
        def neInfo = new NEInfo()
        neInfo.setNeNames(Arrays.asList("Node1"))
        neInfo.setSavedSearchIds(new ArrayList())
        neInfo.setCollectionNames(new ArrayList())
        def schedule = new Schedule()
        schedule.setExecMode(ExecMode.IMMEDIATE)
        schedule.setScheduleAttributes(new ArrayList<>())
        def calRecorderDTO = new CALRecorderDTO();
        def objUnderTest = new NPAMJobTemplateJAXB()
        objUnderTest.setName("jobName")
        objUnderTest.setDescription("This is a NPAM job template")
        objUnderTest.setJobType(DETACH_NE_ACCOUNT)
        objUnderTest.setJobProperties(new ArrayList())
        objUnderTest.setSelectedNEs(neInfo)
        objUnderTest.setMainSchedule(schedule)
        objUnderTest.setOwner("Administrator")
        when: 'convert in NPAMJobTemplate'
        def nPamJobTemplate = objUnderTest.convertToNPamJobTemplate(calRecorderDTO)
        then : ' '
        nPamJobTemplate.getName().equals("jobName")
        nPamJobTemplate.getDescription().equals("This is a NPAM job template")
        nPamJobTemplate.getJobType().equals(DETACH_NE_ACCOUNT)
        nPamJobTemplate.getJobProperties().size() == 2  // the 2 CAL properties
        nPamJobTemplate.getSelectedNEs().getNeNames().contains("Node1")
        nPamJobTemplate.getMainSchedule().getExecMode().equals(ExecMode.IMMEDIATE)
    }

    def 'run constructor of NPamJobJAXB with name null'() {
        given: 'an NPAMJobTemplateJAXB'
        Date now = new Date()
        def calRecorderDTO = new CALRecorderDTO();
        def objUnderTest = new NPAMJobTemplateJAXB();
        objUnderTest.setDescription("This is a NPAM job template")
        objUnderTest.setJobType(DETACH_NE_ACCOUNT)
        objUnderTest.setJobProperties(new ArrayList())
        objUnderTest.setSelectedNEs(new NEInfo())
        objUnderTest.setMainSchedule(new Schedule())
        objUnderTest.setOwner("Administrator")
        when: 'convert in NPAMJobTemplate'
        def nPamJobTemplate = objUnderTest.convertToNPamJobTemplate(calRecorderDTO)
        then : ' '
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_NAME.message)
        ex.getInternalCode().getHttpStatusCode().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_NAME.httpStatusCode)
        ex.getInternalCode().getCode() == NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_NAME.internalCode
    }

    def 'run constructor of NPamJobJAXB with jobType null'() {
        given: 'an NPAMJobTemplateJAXB'
        Date now = new Date()
        def calRecorderDTO = new CALRecorderDTO();
        def objUnderTest = new NPAMJobTemplateJAXB();
        objUnderTest.setName("jobName")
        objUnderTest.setDescription("This is a NPAM job template")
        objUnderTest.setJobProperties(new ArrayList())
        objUnderTest.setSelectedNEs(new NEInfo())
        objUnderTest.setMainSchedule(new Schedule())
        objUnderTest.setOwner("Administrator")
        when: 'convert in NPAMJobTemplate'
        def nPamJobTemplate = objUnderTest.convertToNPamJobTemplate(calRecorderDTO)
        then : ' '
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_TYPE.message)
        ex.getInternalCode().getHttpStatusCode().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_TYPE.httpStatusCode)
        ex.getInternalCode().getCode() == NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_TYPE.internalCode
    }

    def 'run constructor of NPamJobJAXB with mainSchedule null'() {
        given: 'an NPAMJobTemplateJAXB'
        Date now = new Date()
        def calRecorderDTO = new CALRecorderDTO();
        def objUnderTest = new NPAMJobTemplateJAXB();
        objUnderTest.setName("jobName")
        objUnderTest.setDescription("This is a NPAM job template")
        objUnderTest.setJobType(DETACH_NE_ACCOUNT)
        objUnderTest.setJobProperties(new ArrayList())
        objUnderTest.setSelectedNEs(new NEInfo())
        objUnderTest.setOwner("Administrator")
        when: 'convert in NPAMJobTemplate'
        def nPamJobTemplate = objUnderTest.convertToNPamJobTemplate(calRecorderDTO)
        then : ' '
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE.message)
        ex.getInternalCode().getHttpStatusCode().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE.httpStatusCode)
        ex.getInternalCode().getCode() == NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE.internalCode
    }

    def 'run constructor of NPamJobJAXB with nelist null'() {
        given: 'an NPAMJobTemplateJAXB'
        def calRecorderDTO = new CALRecorderDTO();
        def objUnderTest = new NPAMJobTemplateJAXB();
        objUnderTest.setName("jobName")
        objUnderTest.setDescription("This is a NPAM job template")
        objUnderTest.setJobType(DETACH_NE_ACCOUNT)
        objUnderTest.setJobProperties(new ArrayList())
        def schedule = new Schedule()
        schedule.setScheduleAttributes(new ArrayList<>())
        schedule.setExecMode(ExecMode.SCHEDULED)
        objUnderTest.setMainSchedule(schedule)
        objUnderTest.setOwner("Administrator")
        when: 'convert in NPAMJobTemplate'
        objUnderTest.convertToNPamJobTemplate(calRecorderDTO)
        then : ' '
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES.message)
        ex.getInternalCode().getHttpStatusCode().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES.httpStatusCode)
        ex.getInternalCode().getCode() == NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES.internalCode
    }
}
