package com.ericsson.oss.services.security.npam.ejb.job.util


import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_MISSING_MANDATORY_JOBPROPERTIES

import java.nio.file.Files;
import java.nio.file.Paths

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.security.npam.api.constants.NodePamConstantsGetter
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJobTemplate
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs


class JobTemplateValidatorSpec extends BaseSetupForTestSpecs {

    @ObjectUnderTest
    private JobTemplateValidator objUnderTest

    @MockedImplementation
    NodePamConstantsGetter nodePamConstantsGetterMock

    @ImplementationClasses
    def classes = []

    def setup() {
        runtimeDps.withTransactionBoundaries()
        nodePamConstantsGetterMock.getImportFolder() >> "/tmp/ericsson/config_mgt/npam/import/"
        setupFolders()
    }

    def 'Test validate JobTemplate with Name null'() {
        given: 'a JobTempate with name null'
        def nPAMJobTemplate = new NPamJobTemplate()
        nPAMJobTemplate.setName(null)

        when: 'call validateJobTemplateName method'
        def result = objUnderTest.validateJobTemplateName(nPAMJobTemplate )
        then:
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_NAME.message)
    }

    def 'Test validate JobTemplate with Name empty'() {
        given: 'a JobTempate with name empty'
        def nPAMJobTemplate = new NPamJobTemplate()
        nPAMJobTemplate.setName("")

        when: 'call validateJobTemplateName method'
        def result = objUnderTest.validateJobTemplateName(nPAMJobTemplate )
        then:
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_NAME.message)
    }

    def 'Test validate FileName with Name null'() {
        given: 'a FileName with name null'
        def jobProperties = Arrays.asList(new JobProperty("FILENAME_WRONG", "testFile"))

        when: 'call validateFileName method'
        def result = objUnderTest.validateFileName(jobProperties)
        then:
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_PROPERTIES.message)
        ex.getInternalCode().getErrorDetails().equals(UNPROCESSABLE_MISSING_MANDATORY_JOBPROPERTIES + " FILENAME")
    }

    def 'Test validate FileName with Name is directory'() {
        given: 'a FileName that is a directory'
        def jobProperties = Arrays.asList(new JobProperty("FILENAME", "file_directory"))

        when: 'call validateFileName method'
        def result = objUnderTest.validateFileName(jobProperties)
        then:
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_FILE_NOT_FOUND.message)
        ex.getInternalCode().getErrorDetails().contains("file_directory")
    }

    private void setupFolders() {
        Files.createDirectories(Paths.get("/tmp/ericsson/config_mgt/npam/import"));
        Files.createDirectories(Paths.get("/tmp/ericsson/config_mgt/npam/import/file_directory"));
    }
}
