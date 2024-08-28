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
package com.ericsson.oss.services.security.npam.ejb.job.util;

import static com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants.PK_FILENAME;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.END_LINE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.FILE_SEPARATOR;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.INTERNAL_SERVER_ERROR_SAVE_FILE;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.security.npam.api.constants.NodePamConstantsGetter;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty;
import com.ericsson.oss.services.security.npam.ejb.services.NodePamEncryptionManager;

public class FileResource {

    private static final Logger logger = LoggerFactory.getLogger(FileResource.class);
    public static final String FILE_OWNER_RW_PERMISSION = "rw-r-----";

    @Inject
    NodePamEncryptionManager nodePamEncryptionManager;

    @Inject
    NodePamConstantsGetter nodePamConstantsGetter;

    public String getFilenameBeforeJob(final List<JobProperty> properties) {
        for (final JobProperty jobproperty : properties) {
            if (PK_FILENAME.equals(jobproperty.getKey())) {
                final StringBuilder str = new StringBuilder();
                final StringBuilder importedAbsFilename = str.append(nodePamConstantsGetter.getImportFolder()).append(jobproperty.getValue());
                return importedAbsFilename.toString();
            }
        }
        return null;
    }

    public String getFilenameAfterJob(final List<JobProperty> properties) {
        for (final JobProperty jobproperty : properties) {
            if (PK_FILENAME.equals(jobproperty.getKey())) {
                final StringBuilder str = new StringBuilder();
                final StringBuilder importedAbsFilename = str.append(nodePamConstantsGetter.getImportFolderJob()).append(jobproperty.getValue());
                return importedAbsFilename.toString();
            }
        }
        return null;
    }

    public Map<String, List<String>> readCredentialsFromFile(final String filename) {
        final Map<String, List<String>> map = new HashMap<>();
        try (Stream<String> lines = Files.lines(Paths.get(filename))) {
            lines.filter(line -> line.contains(FILE_SEPARATOR)).forEach(line -> {
                final String[] keyValuePair = line.split(FILE_SEPARATOR, 3);
                final String targetName = keyValuePair[0].trim();
                final String encryptedUserName = keyValuePair[1].trim();
                final String encryptedCred = keyValuePair[2].trim();
                final List<String> credentials = new ArrayList<>();
                credentials.add(encryptedUserName);
                credentials.add(encryptedCred);
                map.put(targetName, credentials);
            });
        } catch (final IOException e) {
            logger.info("No credentials found in imported file");
            return map;
        }
        return map;

    }

    public List<String> readTargetsFromFile(final String filename) {
        final List<String> targets = new ArrayList<>();
        try (Stream<String> lines = Files.lines(Paths.get(filename))) {
            lines.filter(line -> line.contains(FILE_SEPARATOR)).forEach(line -> {
                final String[] keyValuePair = line.split(FILE_SEPARATOR, 3);
                final String key = keyValuePair[0].trim();
                targets.add(key);

            });
        } catch (final IOException e) {
            return targets;
        }
        return targets;

    }

    @SuppressWarnings({"squid:S4042"})
    public void writeEncryptedCredentialsToFile(final Map<String, List<String>> output, final File myOutput, final boolean overWrite) {
        try  {
            if (myOutput.exists() && overWrite) {
                logger.warn("Import File Already present, overwrite force = true");
                final boolean deleted = myOutput.delete();
                logger.debug("Import File removed : {}", deleted);
            }
            if (!myOutput.createNewFile()) {
                logger.error("Problem with import file creation.");
                throw new NPAMRestErrorException(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_NFS_RWISSUE, INTERNAL_SERVER_ERROR_SAVE_FILE);
            }
            final Set<PosixFilePermission> perms = PosixFilePermissions.fromString(FILE_OWNER_RW_PERMISSION);
            Files.setPosixFilePermissions(myOutput.toPath(), perms);

            try (DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(myOutput))) {
                for (final Entry<String, List<String>> rowByNode : output.entrySet()) {
                    final String user = rowByNode.getValue().get(0);
                    final String cred = rowByNode.getValue().get(1);
                    final String userEncrypted = nodePamEncryptionManager.encryptPassword(user);
                    final String credEncrypted = nodePamEncryptionManager.encryptPassword(cred);
                    outputStream.writeBytes(rowByNode.getKey() + FILE_SEPARATOR + userEncrypted + FILE_SEPARATOR + credEncrypted + END_LINE);
                }
            }
        } catch (final IOException e) {
            logger.error("Problem with import file");
            throw new NPAMRestErrorException(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_NFS_RWISSUE, INTERNAL_SERVER_ERROR_SAVE_FILE);
            }
    }
}
