/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.security.npam.ejb.job.executor;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.END_LINE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.FILE_SEPARATOR;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.INTERNAL_SERVER_ERROR_FOLDER_NOT_FOUND;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.security.npam.api.constants.NodePamConstantsGetter;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage;
import com.ericsson.oss.services.security.npam.api.interfaces.JobImportService;
import com.ericsson.oss.services.security.npam.ejb.job.util.FileResource;
import com.ericsson.oss.services.security.npam.ejb.services.NodePamCredentialManager;

public class JobImportServiceImpl implements JobImportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobImportServiceImpl.class);
    private static final String SYNTAX_ERROR = "Syntax Error on row %d";
    private static final String INVALID_CREDENTIAL = "Invalid Credential on row %d";
    private static final String DUPLICATED_NODE_NAME = "Duplicated node name on row %d";
    private static final String INVALID_FILE_CONTENT = "File contains only empty lines.";

    @Inject
    NodePamCredentialManager nodePamCredentialManager;

    @Inject
    FileResource fileResource;

    @Inject
    NodePamConstantsGetter nodePamConstantsGetter;

    @Override
    public void updalodInputFileFromContent(final String fileContent, final String fileName, final boolean overWrite) {
        final FileImportErrorReport report = new FileImportErrorReport();

        final File myOutput = new File(nodePamConstantsGetter.getImportFolder() + fileName);
        if (myOutput.exists() && !overWrite) {
            report.addError("File Already present and overwrite = false");
            LOGGER.error("File Already present and overwrite = false");
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_IMPORT_FILE_PRESENT, report.buildReport());
        }
        if (fileContent.length() == 0) {
            report.addError("Imported File is empty.");
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_IMPORT_FILE, report.buildReport());
        }
        // The file content is validated and stored in "output" structure
        final Map<String, List<String>> mappedFileContent = validateAndMapFileContent(fileContent, report);

        if (report.getSize() != 0) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_IMPORT_FILE, report.buildReport());
        }
        fileResource.writeEncryptedCredentialsToFile(mappedFileContent, myOutput, overWrite);

    }

    @Override
    public String[] importedJobFilelist() {
        String[] importedFile = null;
        final File importedDir = new File(nodePamConstantsGetter.getImportFolder());
        importedFile = importedDir.list();
        if (importedFile == null) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_NFS_RWISSUE, INTERNAL_SERVER_ERROR_FOLDER_NOT_FOUND);
        }
        return importedFile;
    }

    private Map<String, List<String>> validateAndMapFileContent(final String fileContent, final FileImportErrorReport report) {
        final String[] rows = fileContent.split(END_LINE);
        final Map<String, List<String>> output = new HashMap<>();
        for (int indexRow = 0; indexRow < rows.length; indexRow++) {
            // Skipping empty rows or header if present 
            if (rows[indexRow].trim().length() == 0 || (indexRow == 0 && rows[indexRow].startsWith("#"))) {
                continue;
            }
            final String[] rowFields = (rows[indexRow].trim()).split(FILE_SEPARATOR);
            validateRowContentAndAddToOutput(report, output, indexRow, rowFields);
            if (report.getSize() >= 5) {
                report.addError("File contains too many errors.");
                return output;
            }
        }
        if (output.isEmpty() && report.getSize() == 0) {
            report.addError(String.format(INVALID_FILE_CONTENT));
            LOGGER.warn("Found only empty rows in file.");
        }
        return output;
    }

    private void validateRowContentAndAddToOutput(final FileImportErrorReport report, final Map<String, List<String>> output, final int indexRow,
                                                  final String[] rowFields) {
        if (rowFields.length != 3) {
            report.addError(String.format(SYNTAX_ERROR, indexRow + 1));
            LOGGER.warn("Found invalid syntax in file at row {}", indexRow + 1);
        } else {
            for (final String el : rowFields) {
                if (el.trim().isEmpty()) {
                    report.addError(String.format(SYNTAX_ERROR, indexRow + 1));
                    LOGGER.warn("Found invalid syntax in file at row {}", indexRow + 1);
                    return;
                }
            }
            if (!nodePamCredentialManager.validateCredentialString(rowFields[2].trim(), rowFields[1].trim())) {
                report.addError(String.format(INVALID_CREDENTIAL, indexRow + 1));
                LOGGER.warn("Found invalid credential in file at row {}", indexRow + 1);
            } else {
                if (output.put(rowFields[0].trim(), Arrays.asList(rowFields[1].trim(), rowFields[2].trim())) != null) {
                    report.addError(String.format(DUPLICATED_NODE_NAME, indexRow + 1));
                    LOGGER.warn("Imported file contains duplicated node names.");
                }
            }
        }
    }

}
