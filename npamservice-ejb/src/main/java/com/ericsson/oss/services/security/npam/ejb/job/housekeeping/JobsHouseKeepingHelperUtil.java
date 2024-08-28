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
package com.ericsson.oss.services.security.npam.ejb.job.housekeeping;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants;
import com.ericsson.oss.services.security.npam.api.constants.NodePamConstantsGetter;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobState;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob;
import com.ericsson.oss.services.security.npam.ejb.job.dao.JobDpsWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class JobsHouseKeepingHelperUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobsHouseKeepingHelperUtil.class);

    @Inject
    private NodePamConstantsGetter nodePamConstantsGetter;

    @Inject
    private JobDpsWriter jobDpsWriter;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void deleteJobsAndTemplateIfNecessary(final String name, List<NPamJob> jobsToBeChecked, Date dateToCompare, Long templatePoId) {
            boolean isTemplateDeletable = true;
            for (final NPamJob npamJob : jobsToBeChecked) {

                if (isJobDeletable(dateToCompare, npamJob)) {
                    LOGGER.debug("HK-deleteJobsAndTemplateIfNecessary:: NPamJob with poId:{} and JobState:{} can be deleted.", npamJob.getJobId() , npamJob.getState());
                    jobDpsWriter.deleteJobHierarchy(npamJob.getJobId(), name);
                } else {
                    isTemplateDeletable = false;
                }
            }
            if (isTemplateDeletable) {
                deleteTemplate(templatePoId, name);
            }
    }

    @SuppressWarnings({"squid:S4042"})
    public void triggerHouseKeepingOfFiles(final int maxFileAge) {
        String importDir = nodePamConstantsGetter.getImportFolder();
        File fold = new File(importDir);
        if (fold.exists()) {
            File[] listAllFiles = fold.listFiles();
            LOGGER.debug("HK-triggerHouseKeepingOfFiles:: Found {} files in import directory", listAllFiles.length);
            long deletionDateInMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(maxFileAge);

            for (File file: listAllFiles) {
                if (file.lastModified() < deletionDateInMillis) {
                    if (file.delete()) {
                        LOGGER.info("HK-triggerHouseKeepingOfFiles:: File {} deleted from NPAM import folder", file);
                    } else {
                        LOGGER.info("HK-triggerHouseKeepingOfFiles:: Cannot delete:{} from NPAM import folder", file);
                    }
                }
            }
        } else {
            LOGGER.info("HK-triggerHouseKeepingOfFiles: Import directory {} does not exists", importDir);
        }
    }

    /***
     *  PRIVATE methods
     ***/

    private boolean isJobDeletable(Date dateToCompare, final NPamJob nPamJob) {
        return (JobState.COMPLETED == nPamJob.getState() && nPamJob.getEndTime().compareTo(dateToCompare) < 0) ||
                (JobState.USER_CANCELLED == nPamJob.getState() && nPamJob.getScheduledTime().compareTo(dateToCompare) < 0);
    }

    private void deleteTemplate(final Long jobTemplateId, String name) {
        PersistenceObject template = jobDpsWriter.findPoById(jobTemplateId);
        final List <HashMap<String, String>> jobProperties = template.getAttribute(JobConfigurationConstants.JOBPROPERTIES);//  it is a java.util.ArrayList
        LOGGER.debug("HK-deleteTemplate:: {} JobPropertie(s) found: {}", jobProperties.size(), jobProperties); // e.g.: [{value=import.csv_114000, key=FILENAME}]

        String filename = null;
        for (final HashMap<String, String> jobproperty : jobProperties) {
            if (jobproperty.containsValue("FILENAME")) {
                filename = jobproperty.get("value");
                break;
            }
        }
        if (filename != null) {
            LOGGER.info("HK-deleteTemplate:: try to delete  filename: {}", filename);
            deleteFile(filename);
        }
        jobDpsWriter.deletePO(template);
        LOGGER.info("HK-deleteTemplate:: JobTemplate named {}, with id:{} deleted", name, jobTemplateId);
    }

    @SuppressWarnings({"squid:S4042"})
    private void deleteFile(final String filename) {
        if (filename != null) {
            if (!filename.isEmpty()) {
                final File myOutput = new File(nodePamConstantsGetter.getImportFolderJob() + filename);
                if (myOutput.exists()) {
                    if (myOutput.delete()) {
                        LOGGER.info("HK-deleteFile:: file {} has been deleted", filename);
                    } else {
                        LOGGER.info("HK-deleteFile:: file {} has NOT been deleted", filename);
                    }
                } else {
                    LOGGER.info("HK-deleteFile:: file {} does not exist (why???), so it CANNOT be deleted", filename);
                    // the file associated to template does not exist (was it maybe deleted manually?)
                    // anyway, here the template can be safely deleted
                }
            } else {
                LOGGER.info("HK-deleteFile:: no need to delete any file");
            }
        }
    }
}
