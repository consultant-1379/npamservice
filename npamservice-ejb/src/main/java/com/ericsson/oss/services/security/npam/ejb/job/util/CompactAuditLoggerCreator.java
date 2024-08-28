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

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.security.npam.api.cal.CALConstants;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob;
import com.ericsson.oss.services.security.npam.ejb.services.NodePamEncryptionManager;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CompactAuditLoggerCreator {

    @Inject
    NodePamEncryptionManager nodePamEncryptionManager;

    @Inject
    private SystemRecorder systemRecorder;

    private static final Logger LOGGER = LoggerFactory.getLogger(CompactAuditLoggerCreator.class);

    public void logCompactAuditLog(final NPamJob mainJob, final JobResult jobResult, final int totalJobs,
                                    final int totalSuccessJobs, final int calJobSuccess, final int calJobSkipped) {

        if (systemRecorder.isCompactAuditEnabled()) {
            List<JobProperty> jobProps = mainJob.getJobProperties();
            String ipAddr = preparePropertyValue(jobProps, CALConstants.CLIENT_IP_ADDRESS);
            String session = preparePropertyValue(jobProps, CALConstants.CLIENT_SESSION_ID);

            CommandPhase commandPhase = CommandPhase.EXECUTED;
            if (jobResult == JobResult.FAILED) {
                commandPhase = CommandPhase.FINISHED_WITH_ERROR;
            }

            final String result = "{\"total\":" + totalJobs + ",\"success\":" + calJobSuccess + ",\"failed\":" + (totalJobs - totalSuccessJobs) + ",\"skipped\":" + calJobSkipped + "}";
            final String jsonCAL = "{" + prepareCALSummaryResult(mainJob, result) + "}";

            systemRecorder.recordCompactAudit(mainJob.getOwner(), "<EXECUTED JOB> - Job Result jobId:" + mainJob.getName(),
                    commandPhase, "NPAM appl", "Job", ipAddr, session, jsonCAL);
        }
    }

    private String preparePropertyValue(List<JobProperty> jobProps, String propName) {
        String propValue = null;
        for (final JobProperty jobProp : jobProps) {
            String propKey = jobProp.getKey();
            if (propName.equals(propKey)) {
                try {
                    propValue = nodePamEncryptionManager.decryptPassword(jobProp.getValue());
                    break;
                } catch (UnsupportedEncodingException e) {
                    LOGGER.warn("Exception '{}' caught while decrypting {} in jobProperties", e.getMessage(), CALConstants.CLIENT_IP_ADDRESS);
                }
            }
        }
        return propValue;
    }

    private String prepareCALSummaryResult(NPamJob nPamJob, String result) {
        String sumRes = "\"summaryResult\":[{\"id\":\"" + nPamJob.getName() + ",\"opType\":\"" + getOpTypeFromJobType(nPamJob.getJobType().toString()) + "\",\"entity\":\"Job\"";

        if (result != null) {
            sumRes += ", \"result\": " + result;
        }
        return sumRes + "}]";
    }

    private String getOpTypeFromJobType(String jobTypeStr) {
        if (jobTypeStr != null) {
            if (jobTypeStr.startsWith("ROTATE")) {
                return (jobTypeStr.replace("_NE_ACCOUNT", "")).toLowerCase(Locale.ENGLISH);
            } else {
                return jobTypeStr.toLowerCase(Locale.ENGLISH);
            }
        }
        return "unknown";
    }

}
