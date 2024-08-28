package com.ericsson.oss.services.security.npam.rest.resources

import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.REPEAT_TYPE_VALUE_WEEKLY
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_END_DATE
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_OCCURRENCES
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_REPEAT_COUNT
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_REPEAT_TYPE
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_START_DATE
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.INTERNAL_SERVER_ERROR_FOLDER_NOT_FOUND
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_DUPLICATED_JOBPROPERTIES
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_INVALID_STARTDATE_FORMAT
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_MAINSCHEDULE_CONFLICT
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_MAINSCHEDULE_DATES_MISMATCH
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_MANUAL_NOT_SUPPORTED
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_MISSING_MANDATORY_JOBPROPERTIES
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_MISSING_STARTDATE
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_ONLY_IMMEDIATE_EXECMODE_ALLOWED
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_ONLY_STARTDATE_ALLOWED
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_STARTDATE_IN_THE_PAST
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_UNEXPECTED_ATTRIBUTE_WITH_IMMEDIATE_EXECMODE
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_UNEXPECTED_JOBPROPERTIES
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_UNEXPECTED_PERIODIC

import java.nio.file.Files
import java.nio.file.Paths

import javax.inject.Inject
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

import org.jboss.resteasy.plugins.providers.multipart.InputPart
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput
import org.jboss.resteasy.specimpl.MultivaluedMapImpl

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.sdk.context.ContextService
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.itpf.sdk.recording.classic.SystemRecorderBean
import com.ericsson.oss.services.security.npam.api.constants.NodePamConstantsGetter
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails
import com.ericsson.oss.services.security.npam.api.job.modelentities.ExecMode
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType
import com.ericsson.oss.services.security.npam.api.job.modelentities.NEInfo
import com.ericsson.oss.services.security.npam.api.job.modelentities.Schedule
import com.ericsson.oss.services.security.npam.api.job.modelentities.ScheduleProperty
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus
import com.ericsson.oss.services.security.npam.ejb.job.executor.JobCreationServiceImpl
import com.ericsson.oss.services.security.npam.ejb.job.executor.JobGetServiceImpl
import com.ericsson.oss.services.security.npam.ejb.job.executor.JobImportServiceImpl
import com.ericsson.oss.services.security.npam.ejb.job.impl.JobConfigurationServiceImpl
import com.ericsson.oss.services.security.npam.ejb.rest.JobServiceImpl
import com.ericsson.oss.services.security.npam.ejb.services.NodePamCredentialManagerImpl
import com.ericsson.oss.services.security.npam.ejb.services.NodePamEncryptionManager
import com.ericsson.oss.services.security.npam.ejb.utility.NetworkUtil
import com.ericsson.oss.services.security.npam.rest.api.NPAMJobTemplateJAXB
import com.ericsson.oss.services.security.npam.rest.testutil.EAccessControlBypassAllImpl
import com.ericsson.oss.services.security.npam.rest.testutil.NPAMRestTestUtilSpec

import spock.lang.Unroll

class NPAMJobServiceRestSpec extends NPAMRestTestUtilSpec {

    @ObjectUnderTest
    private NPAMJobServiceRest objUnderTest

    @Inject
    ContextService cxtMock

    @MockedImplementation
    NodePamConstantsGetter nodePamConstantsGetterMock

    @MockedImplementation
    NodePamConfigStatus nodePamConfigStatusMock

    @MockedImplementation
    NetworkUtil NetworkUtilMock

    @MockedImplementation
    MultipartFormDataInput multipartFormDataInputMock

    @MockedImplementation
    InputPart inputPartMock

    @MockedImplementation
    SystemRecorder mockSystemRecorder

    @Inject
    NodePamEncryptionManager criptoMock

    @ImplementationClasses
    def classes = [
        JobServiceImpl,
        EAccessControlBypassAllImpl,
        JobImportServiceImpl,
        JobCreationServiceImpl,
        JobConfigurationServiceImpl,
        JobGetServiceImpl,
        NodePamCredentialManagerImpl,
        SystemRecorderBean
    ]

    def setup() {
        cxtMock.getContextValue("X-Tor-UserID") >> "administrator"
        nodePamConstantsGetterMock.getImportFolder() >> "/tmp/ericsson/config_mgt/npam/import/"
        nodePamConstantsGetterMock.getImportFolderJob() >> "/tmp/ericsson/config_mgt/npam/import_job/"
        NetworkUtilMock.validateNeNames(_,_)>> {
            return Arrays.asList("TestNode1")
        }
        setupFolders()
        createFile()
    }

    def 'run get file name'() {
        given: 'Multivalue Map with filename'
        def MultivaluedMap<String, String> headers = new MultivaluedMapImpl();

        headers.add("Content-Disposition", "filename=import_file.csv");
        when: 'get file name'

        def filename = objUnderTest.getFileName(headers)
        then: 'file name is correctyl returned'
        filename.equals("import_file.csv")
    }

    def 'run create job update with file immediate'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.IMMEDIATE)
        when: 'create a job immediate with correct parameters'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'the job is successfully created'
        responseUpdate.getStatus() == Response.Status.OK.statusCode
    }

    def 'run create job update with file scheduled'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.SCHEDULED)
        when: 'create a job deferred with correct parameters'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'the job is successfully created'
        responseUpdate.getStatus() == Response.Status.OK.statusCode
    }

    def 'run create job update with file, wrong main schedule (periodic)'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.SCHEDULED)

        ScheduleProperty repeatType = new ScheduleProperty()
        repeatType.setName(SA_REPEAT_TYPE)
        repeatType.setValue(REPEAT_TYPE_VALUE_WEEKLY)
        ScheduleProperty repeatCount = new ScheduleProperty()
        repeatCount.setName(SA_REPEAT_COUNT)
        repeatCount.setValue("1")
        npamJobTemplate.getMainSchedule().getScheduleAttributes().add(repeatType)
        npamJobTemplate.getMainSchedule().getScheduleAttributes().add(repeatCount)

        when: 'create a job scheduled periodically'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE.getMessage())
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_UNEXPECTED_PERIODIC)
    }

    def 'run create job update with file, wrong main schedule (immediate with start date)'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.IMMEDIATE)

        ScheduleProperty scheduleProperty = new ScheduleProperty()
        List<ScheduleProperty> scheduleAttributes = new ArrayList<>()
        scheduleProperty.setName("START_DATE")
        scheduleProperty.setValue("2050-02-03 13:33:00")
        scheduleAttributes.add(scheduleProperty)
        npamJobTemplate.getMainSchedule().setScheduleAttributes(scheduleAttributes)

        when: 'create a job immediate with also a start date'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE.getMessage())
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_UNEXPECTED_ATTRIBUTE_WITH_IMMEDIATE_EXECMODE)
    }

    def 'run create job update with file, wrong main schedule (scheduled more than 1 attributes)'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.SCHEDULED)

        ScheduleProperty scheduleProperty = new ScheduleProperty();
        scheduleProperty.setName(SA_REPEAT_COUNT);
        scheduleProperty.setValue("2");
        npamJobTemplate.getMainSchedule().getScheduleAttributes().add(scheduleProperty)

        when: 'create a job scheduled with other that start date parameter'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE.getMessage())
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_ONLY_STARTDATE_ALLOWED)
    }

    def 'run create job update with file, wrong main schedule (START_DATE previous)'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.SCHEDULED)
        npamJobTemplate.getMainSchedule().getScheduleAttributes().get(0).setValue("2020-02-03 13:33:00")

        when: 'create a job scheduled with start date in the past'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE.getMessage())
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_STARTDATE_IN_THE_PAST)
    }

    def 'run create job update with file, wrong main schedule (START_DATE invalid format)'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.SCHEDULED)
        npamJobTemplate.getMainSchedule().getScheduleAttributes().get(0).setValue("2020-02- 13:33:00")

        when: 'create a job scheduled with start date wrongly formatted'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE.getMessage())
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_INVALID_STARTDATE_FORMAT)
    }

    def 'run create job update with file, wrong main schedule with an attribute different from START_DATE'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.SCHEDULED)
        npamJobTemplate.getMainSchedule().getScheduleAttributes().get(0).setName(SA_REPEAT_COUNT)
        npamJobTemplate.getMainSchedule().getScheduleAttributes().get(0).setValue("2")

        when: 'create a job scheduled with different parameter than start date'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE.getMessage())
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_MISSING_STARTDATE)
    }

    def 'run create job update with file invalid REST body (NEnames not empty)'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.IMMEDIATE)
        npamJobTemplate.getSelectedNEs().setNeNames(new ArrayList(Arrays.asList("TestNode1")))

        when: 'create a job import with file passing also NeInfo'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES.message)
    }

    def 'run create job update with file invalid REST body (Collections not empty)'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.IMMEDIATE)
        npamJobTemplate.getSelectedNEs().setCollectionNames(new ArrayList(Arrays.asList("Fake collection")))

        when: 'create a job import with file passing also Collection'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES.message)
    }

    def 'run create job update with file invalid REST body (SavedSearch not empty)'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true

        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.IMMEDIATE)
        npamJobTemplate.getSelectedNEs().setSavedSearchIds(new ArrayList(Arrays.asList("Fake savedSearch")))

        when: 'create a job import with file passing also savedSearch'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES.message)
    }

    def 'run create job update with file, exec mode manual'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.MANUAL)
        when: 'create a job  with incorrect exec mode'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception about execution mode not supported is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE.message)
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_MANUAL_NOT_SUPPORTED)
    }

    def 'run create job with missing job name'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile( ExecMode.IMMEDIATE)
        when: 'create a job  without passing the job template name'
        npamJobTemplate.setName("")
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception about job template name is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_NAME.message)
    }

    def 'run job update with file with a parameter not expected'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.IMMEDIATE)
        npamJobTemplate.getJobProperties().add(new JobProperty("wrongkey", "wrongvalue"))

        when: 'job update with file with a parameter not expected'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_PROPERTIES.getMessage())
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_UNEXPECTED_JOBPROPERTIES)
    }

    def 'run job update with file with not existing filename'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.IMMEDIATE)
        List<JobProperty> jobProperties = new ArrayList<>()
        jobProperties.add(new JobProperty("FILENAME", "notExistingFile"))
        npamJobTemplate.setJobProperties(jobProperties)

        when: 'job update with file with not existing filename'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_FILE_NOT_FOUND.message)
    }

    def 'run job update with file with parameter null'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.IMMEDIATE)
        npamJobTemplate.setJobProperties(null)

        when: 'job update with file with parameter null'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_PROPERTIES.getMessage())
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_MISSING_MANDATORY_JOBPROPERTIES)
    }

    def 'run job update with file with a parameter not unique'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.IMMEDIATE)
        npamJobTemplate.getJobProperties().add(new JobProperty("FILENAME", "wrongvalue"))

        when: 'job update with file with a parameter not unique'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_PROPERTIES.getMessage())
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_DUPLICATED_JOBPROPERTIES)
    }

    def 'run job update with file with a wrong parameters'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.IMMEDIATE)
        List<JobProperty> jobProperties = new ArrayList<>()
        jobProperties.add(new JobProperty("filename", "import_file"))
        npamJobTemplate.setJobProperties(jobProperties)

        when: 'job update with file with a wrong parameters'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_PROPERTIES.getMessage())
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_MISSING_MANDATORY_JOBPROPERTIES)
    }

    def 'run job update with file without parameters'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        List<JobProperty> jobProperties = new ArrayList<>()
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.IMMEDIATE)
        npamJobTemplate.setJobProperties(jobProperties)

        when: 'creating the job update with file without parameters'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_PROPERTIES.getMessage())
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_MISSING_MANDATORY_JOBPROPERTIES)
    }

    def 'run job update with file with an empty saved file'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdateWithFile(ExecMode.IMMEDIATE)
        List<JobProperty> jobProperties = new ArrayList<>()
        jobProperties.add(new JobProperty("FILENAME", "empty_import_test"))
        npamJobTemplate.setJobProperties(jobProperties)

        when: 'job update with file with an empty file'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_FILE_NOT_CONTAIN_ANY_NETWORK_ELEMENT.getMessage())
    }

    def 'run list imported files'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true

        when: 'exec import job file list'
        def responseList = objUnderTest.importedJobFilelist()
        then: 'one file is returned'
        responseList.getStatus() == Response.Status.OK.statusCode
    }

    def 'run list imported files with feature disabled'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> false

        when: 'exec import job file list'
        def responseList = objUnderTest.importedJobFilelist()
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.FORBIDDEN_FEATURE_DISABLED.message)
    }

    def 'run list imported files with exception'() {
        given: 'a not existing folder'
        nodePamConfigStatusMock.isEnabled() >> true

        when: 'exec import job file list'
        def responseList = objUnderTest.importedJobFilelist()
        then: 'an exception is returned'
        nodePamConstantsGetterMock.getImportFolder() >> "/tmp/ericsson/config_mgt/npam/import/fake"
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_NFS_RWISSUE.message)
        ex.getInternalCode().getErrorDetails().equals(INTERNAL_SERVER_ERROR_FOLDER_NOT_FOUND)
    }

    def 'run create immediate job enable remote management'() {
        given: 'IMMEDIATE CREATE_NE_ACCOUNT job is correctly set'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateEnableDisableCheck(JobType.CREATE_NE_ACCOUNT,ExecMode.IMMEDIATE)

        when: 'creating the job template'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'the job is successfully created'
        responseUpdate.getStatus() == Response.Status.OK.statusCode
    }

    def 'run deferred job enable remote management'() {
        given: 'DEFERRED CREATE_NE_ACCOUNT job is correctly set'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateEnableDisableCheck(JobType.CREATE_NE_ACCOUNT,ExecMode.SCHEDULED)

        when: 'creating the job  enable remote management'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'the job is successfully created'
        responseUpdate.getStatus() == Response.Status.OK.statusCode
    }

    def 'run job enable remote management with wrong parameters'() {
        given: 'Wrong CREATE_NE_ACCOUNT job is set'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateEnableDisableCheck(JobType.CREATE_NE_ACCOUNT,ExecMode.SCHEDULED)
        JobProperty fileName = new JobProperty("FILENAME", "import_test")
        List<JobProperty> jobProperties = new ArrayList<>()
        jobProperties.add(fileName)
        npamJobTemplate.setJobProperties(jobProperties)

        when: 'creating job enable remote management with wrong parameters'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_PROPERTIES.getMessage())
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_UNEXPECTED_JOBPROPERTIES)
    }

    def 'run deferred job enable remote management with feature disabled'() {
        given: 'DEFERRED CREATE_NE_ACCOUNT job is correctly set'
        nodePamConfigStatusMock.isEnabled() >> false
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateEnableDisableCheck(JobType.CREATE_NE_ACCOUNT,ExecMode.SCHEDULED)

        when: 'creating the job enable remote management with feature disabled'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.FORBIDDEN_FEATURE_DISABLED.getMessage())
    }

    def 'run create immediate job check and update ne account configuration '() {
        given: 'IMMEDIATE CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION job is correctly set'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateEnableDisableCheck(JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, ExecMode.IMMEDIATE)

        when: 'creating the job template'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'the job is successfully created'
        responseUpdate.getStatus() == Response.Status.OK.statusCode
    }

    def 'run create immediate job check and update ne account configuration no immediate '() {
        given: 'NO IMMEDIATE CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION job exception is thrown'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateEnableDisableCheck(JobType.CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION, ExecMode.SCHEDULED)

        when: 'creating the job template'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is returned'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE.message)
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_ONLY_IMMEDIATE_EXECMODE_ALLOWED)
    }

    def 'run create job update password immediate'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdatePassword(ExecMode.IMMEDIATE)
        when: 'create a job update password immediate with correct parameters'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'the job is successfully created'
        responseUpdate.getStatus() == Response.Status.OK.statusCode
    }

    def 'run create job update password immediate with collections '() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdatePassword(ExecMode.IMMEDIATE)
        npamJobTemplate.getSelectedNEs().setCollectionNames(new ArrayList(Arrays.asList("collection")))
        when: 'create a job update password immediate with correct parameters'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'the job is successfully created'
        responseUpdate.getStatus() == Response.Status.OK.statusCode
    }

    def 'run create job update password immediate with savedSearch '() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdatePassword(ExecMode.IMMEDIATE)
        npamJobTemplate.getSelectedNEs().setSavedSearchIds(new ArrayList(Arrays.asList("savedSearch")))
        when: 'create a job update password immediate with correct parameters'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'the job is successfully created'
        responseUpdate.getStatus() == Response.Status.OK.statusCode
    }

    def 'run create job update password immediate without neinfo '() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdatePassword(ExecMode.IMMEDIATE)
        npamJobTemplate.getSelectedNEs().setNeNames(new ArrayList())
        when: 'create a job update password immediate with correct parameters'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES.getMessage())
    }

    @Unroll
    def 'run create job update password immediate with #testtype '() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdatePassword(ExecMode.IMMEDIATE)
        npamJobTemplate.getSelectedNEs().setNeNames(neNames)
        npamJobTemplate.getSelectedNEs().setCollectionNames(collection)
        npamJobTemplate.getSelectedNEs().setSavedSearchIds(savedSearch)
        when: 'create a job update password immediate with correct parameters'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'the job is successfully created'
        responseUpdate.getStatus() == Response.Status.OK.statusCode
        where: 'PROVA'
        testtype              |      neNames                                      |  collection                                       |  savedSearch                                 //| result
        "collection"          | new ArrayList()                              | new ArrayList(Arrays.asList("collection"))        | new ArrayList()
        "savedSearch"         | new ArrayList()                              | new ArrayList()                                   | new ArrayList(Arrays.asList("savedSearch"))
        "collection-Saved"    | new ArrayList()                              | new ArrayList(Arrays.asList("collection"))        | new ArrayList(Arrays.asList("savedSearch"))
        "Ne"                  | new ArrayList(Arrays.asList("Ne"))           | new ArrayList()                                   | new ArrayList()
        "Ne-Collection"       | new ArrayList(Arrays.asList("Ne"))           | new ArrayList(Arrays.asList("collection"))        | new ArrayList()
        "Ne-Saved"            | new ArrayList(Arrays.asList("Ne"))           | new ArrayList()                                   | new ArrayList(Arrays.asList("savedSearch"))
        "Ne-Collection-Saved" | new ArrayList(Arrays.asList("Ne"))           | new ArrayList(Arrays.asList("collection"))        | new ArrayList(Arrays.asList("savedSearch"))
    }

    def 'run create job update password immediate with username'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdatePassword(ExecMode.IMMEDIATE)
        npamJobTemplate.getJobProperties().add(new JobProperty("USERNAME", "test1"))

        when: 'create a job update password immediate with username'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'the job is successfully created'
        responseUpdate.getStatus() == Response.Status.OK.statusCode
    }

    def 'run create job update password immediate with invalid password'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdatePassword(ExecMode.IMMEDIATE)
        npamJobTemplate.setJobProperties(Arrays.asList(new JobProperty("PASSWORD", "invalidPassword")))

        when: 'create a job update password immediate with invalid password'
        objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_CREDENTIALS.getMessage())
    }

    def 'run create job update password immediate with invalid job property key'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdatePassword(ExecMode.IMMEDIATE)
        npamJobTemplate.setJobProperties(Arrays.asList(new JobProperty("PASSWRD", "@322B?T4iR&k")))

        when: 'create a job update password immediate with invalid job property key'
        objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_PROPERTIES.getMessage())
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_MISSING_MANDATORY_JOBPROPERTIES)
    }

    def 'run create job update password deferred'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdatePassword(ExecMode.SCHEDULED)
        when: 'create a job update password deferred with correct parameters'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'the job is successfully created'
        responseUpdate.getStatus() == Response.Status.OK.statusCode
    }

    def 'run create job update password deferred, wrong main schedule without START_DATE'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true

        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdatePassword(ExecMode.SCHEDULED)
        npamJobTemplate.getMainSchedule().setScheduleAttributes(new ArrayList())

        when: 'create job update password deferred, wrong main schedule without START_DATE'
        objUnderTest.createJob(npamJobTemplate)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE.getMessage())
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_MISSING_STARTDATE)
    }

    def 'run create job update password, wrong main schedule (periodic)'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdatePassword(ExecMode.SCHEDULED)

        def scheduleProperties = npamJobTemplate.getMainSchedule().getScheduleAttributes();

        ScheduleProperty repeatType = new ScheduleProperty();
        repeatType.setName(SA_REPEAT_TYPE);
        repeatType.setValue(REPEAT_TYPE_VALUE_WEEKLY);
        scheduleProperties.add(repeatType)
        ScheduleProperty repeatCount = new ScheduleProperty();
        repeatCount.setName(SA_REPEAT_COUNT);
        repeatCount.setValue("1");
        scheduleProperties.add(repeatCount)
        when: 'create job update password, wrong main schedule (periodic)'
        objUnderTest.createJob(npamJobTemplate)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE.getMessage())
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_UNEXPECTED_PERIODIC)
    }

    def 'run create job update password, encoding error'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        // we simulate simple algo that add +1
        criptoMock.encryptPassword(_) >> {
            throw new UnsupportedEncodingException()
        }
        NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateUpdatePassword(ExecMode.IMMEDIATE)

        when: 'create job update password, encoding error'
        objUnderTest.createJob(npamJobTemplate)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_DECRYPT.getMessage())
    }

    def 'run delete job for coverage only'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        when:
        def responseUpdate = objUnderTest.deleteJob("jobName")
        then:
        responseUpdate.getStatus() == Response.Status.OK.statusCode
    }

    def 'run create immediate job type auto generation'() {
        given: 'IMMEDIATE auto generation job is correctly set'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateEnableDisableCheck(JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, ExecMode.IMMEDIATE)

        when: 'creating the job template'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'response OK'
        responseUpdate.getStatus() == Response.Status.OK.statusCode
    }

    def 'run create wrong job with type auto generation'() {
        given: 'auto generation job wrongly scheduled: IMMEDIATE with parameter'
        nodePamConfigStatusMock.isEnabled() >> true
        NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateEnableDisableCheck(JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, ExecMode.IMMEDIATE)
        npamJobTemplate.setMainSchedule(returnWrongSchedule1())
        when: 'creating the job template'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_JOB_CONFIGURATION_ERROR.message)
        ex.getInternalCode().getErrorDetails().contains("Wrong immediate schedule")
    }

    def 'run create wrong scheduled job with type auto generation'() {
        given: 'auto generation job wrongly scheduled: SCHEDULED with occurrens and end-date'
        nodePamConfigStatusMock.isEnabled() >> true
        NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateEnableDisableCheck(JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, ExecMode.SCHEDULED)
        npamJobTemplate.setMainSchedule(returnWrongSchedule2())
        when: 'creating the job template'
        objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE.message)
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_MAINSCHEDULE_CONFLICT)
    }

    def 'run create never ending job with type auto generation'() {
        given: 'auto generation never ending job scheduled'
        nodePamConfigStatusMock.isEnabled() >> true
        NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateEnableDisableCheck(JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, ExecMode.SCHEDULED)
        npamJobTemplate.setMainSchedule(returnNeverEndingSchedule(ExecMode.SCHEDULED))
        when: 'creating the job template'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'response OK'
        responseUpdate.getStatus() == Response.Status.OK.statusCode
    }

    def 'run create wrong scheduled job with type auto generation without repeat type'() {
        given: 'auto generation job wrongly scheduled: SCHEDULED without repeat type'
        nodePamConfigStatusMock.isEnabled() >> true
        NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateEnableDisableCheck(JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, ExecMode.SCHEDULED)
        npamJobTemplate.setMainSchedule(returnWrongSchedule3())
        when: 'creating the job template'
        objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_JOB_CONFIGURATION_ERROR.message)
        ex.getInternalCode().getErrorDetails().contains("Wrong not periodic")
    }


    def 'run create wrong scheduled job with type auto generation without repeat count'() {
        given: 'auto generation job wrongly scheduled: SCHEDULED without repeat count'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateEnableDisableCheck(JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, ExecMode.SCHEDULED)
        npamJobTemplate.setMainSchedule(returnWrongSchedule4())
        when: 'creating the job template'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_JOB_CONFIGURATION_ERROR.message)
        ex.getInternalCode().getErrorDetails().contains("Wrong not periodic")
    }

    def 'run create wrong scheduled job with type auto generation with wrong end date'() {
        given: 'auto generation job wrongly scheduled: SCHEDULED with wrong end date'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateEnableDisableCheck(JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, ExecMode.SCHEDULED)
        npamJobTemplate.setMainSchedule(returnWrongSchedule5())
        when: 'creating the job template'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE.message)
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_MAINSCHEDULE_DATES_MISMATCH)
    }

    def 'run create wrong scheduled job with type auto generation with a bad end date'() {
        given: 'auto generation job wrongly scheduled: SCHEDULED with a bad end date'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateEnableDisableCheck(JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, ExecMode.SCHEDULED)
        npamJobTemplate.setMainSchedule(returnWrongEndDateSchedule())
        when: 'creating the job template'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_JOB_CONFIGURATION_ERROR.message)
        ex.getInternalCode().getErrorDetails().contains("Wrong schedule")
    }

    def 'run create wrong scheduled job with type auto generation with a bad exec mode'() {
        given: 'auto generation job wrongly scheduled: SCHEDULED with a bad exec mode'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateEnableDisableCheck(JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, ExecMode.MANUAL)
        npamJobTemplate.setMainSchedule(returnNeverEndingSchedule(ExecMode.MANUAL))
        when: 'creating the job template'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_JOB_CONFIGURATION_ERROR.message)
        ex.getInternalCode().getErrorDetails().contains("Wrong schedule")
    }

    def 'run create wrong scheduled job with type auto generation with a bad repeat count'() {
        given: 'auto generation job wrongly scheduled: SCHEDULED with a bad repeat count'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateEnableDisableCheck(JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, ExecMode.SCHEDULED )
        npamJobTemplate.setMainSchedule(returnWrongScheduleRepeatCount())
        when: 'creating the job template'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_JOB_CONFIGURATION_ERROR.message)
    }


    def 'run create wrong scheduled job with type auto generation with a bad attribute'() {
        given: 'auto generation job wrongly scheduled: SCHEDULED with a bad attribute'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateEnableDisableCheck(JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, ExecMode.SCHEDULED )
        npamJobTemplate.setMainSchedule(returnNeverEndingSchedule(ExecMode.SCHEDULED))
        ScheduleProperty scheduleProperty = new ScheduleProperty()
        scheduleProperty.setName("wrongProperty")
        scheduleProperty.setValue("0")
        npamJobTemplate.getMainSchedule().getScheduleAttributes().add(scheduleProperty)
        when: 'creating the job template'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE.message)
        ex.getInternalCode().getErrorDetails().equals("wrongProperty")
    }

    def 'run create two jobs with same name'() {
        given: 'auto generation job wrongly with name already used'
        nodePamConfigStatusMock.isEnabled() >> true
        def NPAMJobTemplateJAXB npamJobTemplate = createNpamJobTemplateEnableDisableCheck(JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, ExecMode.SCHEDULED)
        npamJobTemplate.setMainSchedule(returnNeverEndingSchedule(ExecMode.SCHEDULED))
        when: 'creating the job template'
        def responseUpdate = objUnderTest.createJob(npamJobTemplate)
        def responseUpdate1 = objUnderTest.createJob(npamJobTemplate)
        then: 'an exception is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_JOB_NAME_ALREADY_PRESENT.message)
    }

    //Import file
    def 'run importJobFile with input = null'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true

        when: 'exec importJobFile with null'
        def response = objUnderTest.importJobFile(null, false)

        then: 'an exception about feature disabled is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.BAD_REQUEST_INVALID_INPUT_JSON.message)
        ex.getInternalCode().getErrorDetails().equals(NPAMRestErrorsMessageDetails.BAD_REQUEST_INVALID_INPUT_DATA)
    }

    def 'run importJobFile with File = null'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        Map<String, List<InputPart>> uploadForm = new HashMap<>()
        uploadForm.put("File", null)
        multipartFormDataInputMock.getFormDataMap() >> {return uploadForm}
        when: 'exec importJobFile with File null'
        def response = objUnderTest.importJobFile(multipartFormDataInputMock, false)
        then: 'an exception about feature disabled is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.BAD_REQUEST_INVALID_INPUT_JSON.message)
        ex.getInternalCode().getErrorDetails().equals(NPAMRestErrorsMessageDetails.BAD_REQUEST_INVALID_INPUT_DATA)
    }

    def 'run importJobFile with FileName = null'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true

        Map<String, List<InputPart>> uploadForm = new HashMap<>()
        def inputParts = new ArrayList<>()
        inputParts.add(inputPartMock)
        uploadForm.put("File", inputParts)
        multipartFormDataInputMock.getFormDataMap() >> {return uploadForm}
        MultivaluedMap<String, String> headers = new MultivaluedMapImpl<>()
        headers.put("Content-Disposition", Arrays.asList("key1=\"value1\";key2=\"value2\""))
        inputPartMock.getHeaders() >> {return headers}

        when: 'exec importJobFile with File null'
        def response = objUnderTest.importJobFile(multipartFormDataInputMock, false)
        then: 'an exception about feature disabled is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.BAD_REQUEST_INVALID_INPUT_JSON.message)
        ex.getInternalCode().getErrorDetails().equals(NPAMRestErrorsMessageDetails.BAD_REQUEST_INVALID_INPUT_DATA)
    }

    def 'run importJobFile'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        mockSystemRecorder.isCompactAuditEnabled() >> true

        Map<String, List<InputPart>> uploadForm = new HashMap<>()
        def inputParts = new ArrayList<>()
        inputParts.add(inputPartMock)
        uploadForm.put("File", inputParts)
        multipartFormDataInputMock.getFormDataMap() >> {return uploadForm}
        MultivaluedMap<String, String> headers = new MultivaluedMapImpl<>()
        headers.put("Content-Disposition", Arrays.asList("filename=\"value1\";key2=\"value2\""))
        inputPartMock.getHeaders() >> {return headers}
        inputPartMock.getBodyAsString() >> {return "LTE11dg2ERBS00001; test4 ;B6zkY2ne7~OL"}

        when: 'exec importJobFile with correct Filename'
        def response = objUnderTest.importJobFile(multipartFormDataInputMock, true)
        then: 'an OK response is returned'
        response.getStatus() == Response.Status.OK.statusCode
    }

    private createMainSchedule(NPAMJobTemplateJAXB jobTemplate) {
        if (jobTemplate.getMainSchedule().getExecMode() == ExecMode.SCHEDULED ) {
            ScheduleProperty scheduleProperty = new ScheduleProperty()
            List<ScheduleProperty> scheduleAttributes = new ArrayList<>()
            scheduleProperty.setName("START_DATE")
            scheduleProperty.setValue("2050-02-03 13:33:00")
            scheduleAttributes.add(scheduleProperty)
            jobTemplate.getMainSchedule().setScheduleAttributes(scheduleAttributes)
        }
    }

    private Schedule returnWrongSchedule1() {
        Schedule mainSchedule = new Schedule()
        mainSchedule.setExecMode(ExecMode.IMMEDIATE);
        ScheduleProperty scheduleProperty = new ScheduleProperty()
        List<ScheduleProperty> scheduleAttributes = new ArrayList<>()
        scheduleProperty.setName("START_DATE")
        scheduleProperty.setValue("2050-02-03 13:33:00")
        scheduleAttributes.add(scheduleProperty)
        mainSchedule.setScheduleAttributes(scheduleAttributes)
        return mainSchedule;
    }

    private Schedule returnWrongSchedule2() {
        Schedule mainSchedule = new Schedule()
        mainSchedule.setExecMode(ExecMode.SCHEDULED);
        List<ScheduleProperty> scheduleAttributes = new ArrayList<>()
        ScheduleProperty scheduleProperty1 = new ScheduleProperty()
        scheduleProperty1.setName(SA_START_DATE)
        scheduleProperty1.setValue("2050-02-03 13:33:00")
        scheduleAttributes.add(scheduleProperty1)
        ScheduleProperty scheduleProperty2 = new ScheduleProperty()
        scheduleProperty2.setName(SA_OCCURRENCES)
        scheduleProperty2.setValue("4")
        scheduleAttributes.add(scheduleProperty2)
        ScheduleProperty scheduleProperty3 = new ScheduleProperty()
        scheduleProperty3.setName(SA_END_DATE)
        scheduleProperty3.setValue("2050-02-07 13:33:00")
        scheduleAttributes.add(scheduleProperty3)
        ScheduleProperty scheduleProperty4 = new ScheduleProperty()
        scheduleProperty4.setName(SA_REPEAT_COUNT)
        scheduleProperty4.setValue("1")
        scheduleAttributes.add(scheduleProperty4)
        ScheduleProperty scheduleProperty5 = new ScheduleProperty()
        scheduleProperty5.setName(SA_REPEAT_TYPE)
        scheduleProperty5.setValue(REPEAT_TYPE_VALUE_WEEKLY)
        scheduleAttributes.add(scheduleProperty5)
        mainSchedule.setScheduleAttributes(scheduleAttributes)
        return mainSchedule;
    }

    private Schedule returnWrongSchedule3() {
        Schedule mainSchedule = new Schedule()
        mainSchedule.setExecMode(ExecMode.SCHEDULED);
        List<ScheduleProperty> scheduleAttributes = new ArrayList<>()
        ScheduleProperty scheduleProperty1 = new ScheduleProperty()
        scheduleProperty1.setName(SA_START_DATE)
        scheduleProperty1.setValue("2050-02-03 13:33:00")
        scheduleAttributes.add(scheduleProperty1)
        ScheduleProperty scheduleProperty2 = new ScheduleProperty()
        scheduleProperty2.setName(SA_OCCURRENCES)
        scheduleProperty2.setValue("4")
        scheduleAttributes.add(scheduleProperty2)
        ScheduleProperty scheduleProperty4 = new ScheduleProperty()
        scheduleProperty4.setName(SA_REPEAT_COUNT)
        scheduleProperty4.setValue("1")
        scheduleAttributes.add(scheduleProperty4)
        mainSchedule.setScheduleAttributes(scheduleAttributes)
        return mainSchedule;
    }

    private Schedule returnWrongSchedule4() {
        Schedule mainSchedule = new Schedule()
        mainSchedule.setExecMode(ExecMode.SCHEDULED);
        List<ScheduleProperty> scheduleAttributes = new ArrayList<>()
        ScheduleProperty scheduleProperty1 = new ScheduleProperty()
        scheduleProperty1.setName(SA_START_DATE)
        scheduleProperty1.setValue("2050-02-03 13:33:00")
        scheduleAttributes.add(scheduleProperty1)
        ScheduleProperty scheduleProperty2 = new ScheduleProperty()
        scheduleProperty2.setName(SA_OCCURRENCES)
        scheduleProperty2.setValue("4")
        scheduleAttributes.add(scheduleProperty2)
        ScheduleProperty scheduleProperty5 = new ScheduleProperty()
        scheduleProperty5.setName(SA_REPEAT_TYPE)
        scheduleProperty5.setValue(REPEAT_TYPE_VALUE_WEEKLY)
        scheduleAttributes.add(scheduleProperty5)
        mainSchedule.setScheduleAttributes(scheduleAttributes)
        return mainSchedule;
    }

    private Schedule returnWrongSchedule5() {
        Schedule mainSchedule = new Schedule()
        mainSchedule.setExecMode(ExecMode.SCHEDULED);
        List<ScheduleProperty> scheduleAttributes = new ArrayList<>()
        ScheduleProperty scheduleProperty1 = new ScheduleProperty()
        scheduleProperty1.setName(SA_START_DATE)
        scheduleProperty1.setValue("2050-02-03 13:33:00")
        scheduleAttributes.add(scheduleProperty1)
        ScheduleProperty scheduleProperty3 = new ScheduleProperty()
        scheduleProperty3.setName(SA_END_DATE)
        scheduleProperty3.setValue("2030-02-07 12:33:00")
        scheduleAttributes.add(scheduleProperty3)
        ScheduleProperty scheduleProperty4 = new ScheduleProperty()
        scheduleProperty4.setName(SA_REPEAT_COUNT)
        scheduleProperty4.setValue("1")
        scheduleAttributes.add(scheduleProperty4)
        ScheduleProperty scheduleProperty5 = new ScheduleProperty()
        scheduleProperty5.setName(SA_REPEAT_TYPE)
        scheduleProperty5.setValue(REPEAT_TYPE_VALUE_WEEKLY)
        scheduleAttributes.add(scheduleProperty5)
        mainSchedule.setScheduleAttributes(scheduleAttributes)
        return mainSchedule;
    }

    private Schedule returnNeverEndingSchedule(final ExecMode execMode) {
        Schedule mainSchedule = new Schedule()
        mainSchedule.setExecMode(execMode);
        List<ScheduleProperty> scheduleAttributes = new ArrayList<>()
        ScheduleProperty scheduleProperty1 = new ScheduleProperty()
        scheduleProperty1.setName(SA_START_DATE)
        scheduleProperty1.setValue("2050-02-03 13:33:00")
        scheduleAttributes.add(scheduleProperty1)
        ScheduleProperty scheduleProperty4 = new ScheduleProperty()
        scheduleProperty4.setName(SA_REPEAT_COUNT)
        scheduleProperty4.setValue("1")
        scheduleAttributes.add(scheduleProperty4)
        ScheduleProperty scheduleProperty5 = new ScheduleProperty()
        scheduleProperty5.setName(SA_REPEAT_TYPE)
        scheduleProperty5.setValue(REPEAT_TYPE_VALUE_WEEKLY)
        scheduleAttributes.add(scheduleProperty5)
        mainSchedule.setScheduleAttributes(scheduleAttributes)
        return mainSchedule;
    }

    private Schedule returnWrongEndDateSchedule() {
        Schedule mainSchedule = new Schedule()
        mainSchedule.setExecMode(ExecMode.SCHEDULED);
        List<ScheduleProperty> scheduleAttributes = new ArrayList<>()
        ScheduleProperty scheduleProperty1 = new ScheduleProperty()
        scheduleProperty1.setName(SA_START_DATE)
        scheduleProperty1.setValue("2050-02-03 13:33:00")
        scheduleAttributes.add(scheduleProperty1)
        ScheduleProperty scheduleProperty3 = new ScheduleProperty()
        scheduleProperty3.setName(SA_END_DATE)
        scheduleProperty3.setValue("thatsnotdate")
        scheduleAttributes.add(scheduleProperty3)
        ScheduleProperty scheduleProperty4 = new ScheduleProperty()
        scheduleProperty4.setName(SA_REPEAT_COUNT)
        scheduleProperty4.setValue("1")
        scheduleAttributes.add(scheduleProperty4)
        ScheduleProperty scheduleProperty5 = new ScheduleProperty()
        scheduleProperty5.setName(SA_REPEAT_TYPE)
        scheduleProperty5.setValue(REPEAT_TYPE_VALUE_WEEKLY)
        scheduleAttributes.add(scheduleProperty5)
        mainSchedule.setScheduleAttributes(scheduleAttributes)
        return mainSchedule;
    }

    private Schedule returnWrongScheduleRepeatCount() {
        Schedule mainSchedule = new Schedule()
        mainSchedule.setExecMode(ExecMode.SCHEDULED);
        List<ScheduleProperty> scheduleAttributes = new ArrayList<>()
        ScheduleProperty scheduleProperty1 = new ScheduleProperty()
        scheduleProperty1.setName(SA_START_DATE)
        scheduleProperty1.setValue("2050-02-03 13:33:00")
        scheduleAttributes.add(scheduleProperty1)
        ScheduleProperty scheduleProperty2 = new ScheduleProperty()
        scheduleProperty2.setName(SA_OCCURRENCES)
        scheduleProperty2.setValue("4")
        scheduleAttributes.add(scheduleProperty2)
        ScheduleProperty scheduleProperty4 = new ScheduleProperty()
        scheduleProperty4.setName(SA_REPEAT_COUNT)
        scheduleProperty4.setValue("300")
        scheduleAttributes.add(scheduleProperty4)
        ScheduleProperty scheduleProperty5 = new ScheduleProperty()
        scheduleProperty5.setName(SA_REPEAT_TYPE)
        scheduleProperty5.setValue(REPEAT_TYPE_VALUE_WEEKLY)
        scheduleAttributes.add(scheduleProperty5)
        mainSchedule.setScheduleAttributes(scheduleAttributes)
        return mainSchedule;
    }
    private NPAMJobTemplateJAXB createNpamJobTemplateBase(final JobType jobType, ExecMode execMode) {
        NPAMJobTemplateJAXB jobTemplate = new NPAMJobTemplateJAXB();
        jobTemplate.setName("Test1")
        jobTemplate.setJobType(jobType)

        Schedule mainSchedule = new Schedule()
        mainSchedule.setExecMode(execMode)
        mainSchedule.setScheduleAttributes(new ArrayList<>())
        jobTemplate.setMainSchedule(mainSchedule)

        jobTemplate.setJobProperties(new ArrayList<>())

        NEInfo selectedNEs = new NEInfo()
        selectedNEs.setNeNames(new ArrayList())
        selectedNEs.setCollectionNames(new ArrayList())
        selectedNEs.setSavedSearchIds(new ArrayList())
        jobTemplate.setSelectedNEs(selectedNEs)

        return jobTemplate
    }

    private NPAMJobTemplateJAXB createNpamJobTemplateUpdateWithFile(ExecMode execMode) {

        NPAMJobTemplateJAXB jobTemplate = createNpamJobTemplateBase(JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE, execMode)
        createMainSchedule(jobTemplate)

        JobProperty fileName = new JobProperty("FILENAME", "import_test")
        List<JobProperty> jobProperties = new ArrayList<>()
        jobProperties.add(fileName)
        jobTemplate.setJobProperties(jobProperties)
        return jobTemplate
    }

    private NPAMJobTemplateJAXB createNpamJobTemplateUpdatePassword(ExecMode execMode) {

        NPAMJobTemplateJAXB jobTemplate = createNpamJobTemplateBase(JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, execMode)
        createMainSchedule(jobTemplate)

        JobProperty pwd = new JobProperty("PASSWORD", "@322abcB*T4iR&k")
        List<JobProperty> jobProperties = new ArrayList<>()
        jobProperties.add(pwd)
        jobTemplate.setJobProperties(jobProperties)

        jobTemplate.selectedNEs.setNeNames(new ArrayList(Arrays.asList("TestNode1")))
        return jobTemplate
    }

    private NPAMJobTemplateJAXB createNpamJobTemplateEnableDisableCheck(JobType jobType, ExecMode execMode) {
        NPAMJobTemplateJAXB jobTemplate = createNpamJobTemplateBase(jobType, execMode)
        createMainSchedule(jobTemplate)

        jobTemplate.selectedNEs.setNeNames(new ArrayList(Arrays.asList("TestNode1")))
        return jobTemplate
    }

    private void setupFolders() {
        Files.createDirectories(Paths.get("/tmp/ericsson/config_mgt/npam/import/"));
        Files.createDirectories(Paths.get("/tmp/ericsson/config_mgt/npam/import_job/"));
    }

    private void createFile() {
        StringBuilder fileContent = new StringBuilder();
        fileContent.append("LTE11dg2ERBS00001;JCQwMSQkiw9JqibrNfXRbXySb9KpeQcUk0hDx5T5gKTSrTJcFVJK;JCQwMSQkXc/34afGb/ir1lxaKR8KBrqLyz+wXMVu7xPbpriiB/JhMa3YGkW6Aw==\n");
        fileContent.append("\n");

        File import_file = new File("/tmp/ericsson/config_mgt/npam/import/import_test");
        DataOutputStream fos = new DataOutputStream(new FileOutputStream(import_file));
        fos.writeBytes(fileContent.toString());

        File empty_import_file = new File("/tmp/ericsson/config_mgt/npam/import/empty_import_test");
        fos = new DataOutputStream(new FileOutputStream(empty_import_file));
        fos.writeBytes("");
    }
}
