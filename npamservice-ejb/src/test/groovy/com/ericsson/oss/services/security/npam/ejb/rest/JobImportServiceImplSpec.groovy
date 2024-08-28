package com.ericsson.oss.services.security.npam.ejb.rest


import java.nio.file.Files;
import java.nio.file.Paths
import java.util.stream.Stream

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.security.cryptography.CryptographyService
import com.ericsson.oss.services.security.npam.api.constants.NodePamConstantsGetter
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus

class JobImportServiceImplSpec  extends BaseSetupForTestSpecs {

    @ObjectUnderTest
    private JobServiceImpl objUnderTest

    @Inject
    CryptographyService criptoMock

    @MockedImplementation
    NodePamConstantsGetter nodePamConstantsGetterMock

    @MockedImplementation
    NodePamConfigStatus nodePamConfigStatusMock

    @ImplementationClasses
    def classes = []

    String testFile = "TestFile";

    def setup() {
        runtimeDps.withTransactionBoundaries()
        nodePamConstantsGetterMock.getImportFolder() >> "/tmp/ericsson/config_mgt/npam/import/"
        nodePamConstantsGetterMock.getImportFolderJob() >> "/tmp/ericsson/config_mgt/npam/import_job/"
        setupFolders()


        // we simulate simple algo that add +1
        criptoMock.encrypt(_) >> { args ->
            byte[] bytes = args[0]
            for (int i=0; i<bytes.length; i++) {
                bytes[i]++;
            }
            return bytes
        }

        // we simulate simple algo that add -1
        criptoMock.decrypt(_)  >> { args ->
            byte[] bytes = args[0]
            for (int i=0; i<bytes.length; i++) {
                bytes[i]--;
            }
            return bytes
        }
    }

    def 'updalodInputFileFromContent with correct file'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def fileContent = createFileContent("correctFile")


        when: 'Upload a correct file'
        objUnderTest.updalodInputFileFromContent(fileContent, testFile, true)
        def result = readFile("/tmp/ericsson/config_mgt/npam/import/" + testFile)
        then:
        result.get("LTE11dg2ERBS00001").size()==2
        result.get("LTE11dg2ERBS00002").size()==2
    }

    def 'updalodInputFileFromContent with correct file already present overwrite false'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def fileContent = createFileContent("correctFile")

        when: 'Upload a correct file and file already present'
        objUnderTest.updalodInputFileFromContent(fileContent, testFile, false)

        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_IMPORT_FILE_PRESENT.message)
        ex.getInternalCode().getErrorDetails().contains("File Already present and overwrite = false")
    }

    def 'updalodInputFileFromContent with emptyFile'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def fileContent = createFileContent("emptyFile")

        when: 'Upload a file with emptyFile'
        objUnderTest.updalodInputFileFromContent(fileContent, testFile, true)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_IMPORT_FILE.message)
        ex.getInternalCode().getErrorDetails().contains("Imported File is empty.")
    }


    def 'updalodInputFileFromContent with duplicated nodes'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def fileContent = createFileContent("duplicatedNode")

        when: 'Upload a file with duplicated node'
        objUnderTest.updalodInputFileFromContent(fileContent, testFile, true)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_IMPORT_FILE.message)
        ex.getInternalCode().getErrorDetails().contains("Duplicated node name on row 2")
    }

    def 'updalodInputFileFromContent with wrongCredential'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def fileContent = createFileContent("wrongCredential")

        when: 'Upload a file with wrongCredential'
        objUnderTest.updalodInputFileFromContent(fileContent, testFile, true)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_IMPORT_FILE.message)
        ex.getInternalCode().getErrorDetails().contains("Invalid Credential on row 2")
    }

    def 'updalodInputFileFromContent with manyErrors'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def fileContent = createFileContent("manyErrors")

        when: 'Upload a file with manyErrors'
        objUnderTest.updalodInputFileFromContent(fileContent, testFile, true)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_IMPORT_FILE.message)
        ex.getInternalCode().getErrorDetails().contains("Invalid Credential on row 2")
        ex.getInternalCode().getErrorDetails().contains("File contains too many errors.")
    }

    def 'updalodInputFileFromContent with emptyRows'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def fileContent = createFileContent("emptyRows")

        when: 'Upload a file with emptyRows'
        objUnderTest.updalodInputFileFromContent(fileContent, testFile, true)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_IMPORT_FILE.message)
        ex.getInternalCode().getErrorDetails().contains("File contains only empty lines.")
    }

    def 'updalodInputFileFromContent with missingField'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def fileContent = createFileContent("missingField")

        when: 'Upload a file with missingField'
        objUnderTest.updalodInputFileFromContent(fileContent, testFile, true)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_IMPORT_FILE.message)
        ex.getInternalCode().getErrorDetails().contains("Syntax Error on row 2")
    }

    def 'updalodInputFileFromContent with tooManyFields'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def fileContent = createFileContent("tooManyFields")

        when: 'Upload a file with tooManyFields'
        objUnderTest.updalodInputFileFromContent(fileContent, testFile, true)
        then: 'an exception about import file is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_IMPORT_FILE.message)
        ex.getInternalCode().getErrorDetails().contains("Syntax Error on row 2")
    }

    def 'updalodInputFileFromContent with fileWithHeader'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def fileContent = createFileContent("fileWithHeader")

        when: 'Upload a file with fileWithHeader'
        objUnderTest.updalodInputFileFromContent(fileContent, testFile, true)
        def result = readFile("/tmp/ericsson/config_mgt/npam/import/" + testFile)
        then:
        result.get("LTE11dg2ERBS00001").size()==2
    }

    def 'updalodInputFileFromContent with feature disabled'() {
        given: 'NPAM_CONFIG not configured in dps'
        nodePamConfigStatusMock.isEnabled() >> false
        def fileContent = createFileContent("emptyRows")

        when: 'Upload a correct file'
        objUnderTest.updalodInputFileFromContent(fileContent, testFile, true)
        then: 'an exception about feature disabled is thrown'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.FORBIDDEN_FEATURE_DISABLED.message)
    }

    def 'create job with wrong id'() {
        given: 'NPAM_CONFIG configured in dps'
        nodePamConfigStatusMock.isEnabled() >> true
        def fileContent = createFileContent("emptyRows")

        when: 'Upload a correct file'
        objUnderTest.createNewJob(-1)
        then: 'an exception about feature disabled is thrown'
        NPAMRestErrorException ex = thrown()
    }

    private String createFileContent(final String fileType) {
        StringBuilder fileContent = new StringBuilder();
        switch (fileType) {
            case "correctFile":
                fileContent.append("LTE11dg2ERBS00001; test1 ;@322abcB*T4iR&k\n");
                fileContent.append("\n");
                fileContent.append("LTE11dg2ERBS00002 ; test2 ; @322abcB*T4iR&k \n");
                break;
            case "duplicatedNode":
                fileContent.append("LTE11dg2ERBS00001; test1 ;@322abcB*T4iR&k\n");
                fileContent.append("LTE11dg2ERBS00001; test1 ;@322abcB*T4iR&k\n");
                break;
            case "wrongCredential":
                fileContent.append("LTE11dg2ERBS00001; test1 ;@322abcB*T4iR&k\n");
                fileContent.append("LTE11dg2ERBS00002; test2 ;@322abcB*T4i\n");
                break;
            case "manyErrors":
                fileContent.append("LTE11dg2ERBS00001; @322B?T4iR&k ;@322B*T4iR&k\n");
                fileContent.append("LTE11dg2ERBS00002; test2 ;@322B*T4i\n");
                fileContent.append("LTE11dg2ERBS00003; test2\n");
                fileContent.append("LTE11dg2ERBS00004;\n");
                fileContent.append(";; \n");
                fileContent.append("LTE11dg2ERBS00005; test1 ;@322abcB*T4iR&k\n");
                break;
            case "emptyFile":
                fileContent.append("");
                break;
            case "emptyRows":
                fileContent.append("\n");
                fileContent.append("\n");
                break;
            case "missingField":
                fileContent.append("LTE11dg2ERBS00001; @322B*T4iR&k ;@322abcB*T4iR&k\n");
                fileContent.append("LTE11dg2ERBS00002;  ;@322B*T4i\n");
                fileContent.append("LTE11dg2ERBS00003; test2\n");
                break;
            case "tooManyFields":
                fileContent.append("LTE11dg2ERBS00001; @322B*T4iR&k ;@322abcB*T4iR&k\n");
                fileContent.append("LTE11dg2ERBS00002;  test1; test2 ;@322B*T4i\n");
                fileContent.append("LTE11dg2ERBS00003; test2\n");
                break;
            case "fileWithHeader" :
                fileContent.append("#NetworkElementName;UserName;Password\n");
                fileContent.append("LTE11dg2ERBS00001; test1 ;@322abcB*T4iR&k\n");
                break;
        }
        return fileContent.toString();
    }

    private void setupFolders() {
        Files.createDirectories(Paths.get("/tmp/ericsson/config_mgt/npam/import"));
        Files.createDirectories(Paths.get("/tmp/ericsson/config_mgt/npam/import_job"));
    }

    private Map<String, List<String>> readFile(String filename) {
        final Map<String, List<String>> map = new HashMap<>();
        try {
            Stream<String> lines = Files.lines(Paths.get(filename));
            for ( String line : lines) {
                final String[] keyValuePair = line.split(";", 3);
                final String targetName = keyValuePair[0].trim();
                final String encryptedUserName = keyValuePair[1].trim();
                final String encryptedCred = keyValuePair[2].trim();
                final List<String> credentials = new ArrayList<>();
                credentials.add(encryptedUserName);
                credentials.add(encryptedCred);
                map.put(targetName, credentials);
            }
        } catch (final IOException e) {
            return map;
        }
        return map;
    }
}


