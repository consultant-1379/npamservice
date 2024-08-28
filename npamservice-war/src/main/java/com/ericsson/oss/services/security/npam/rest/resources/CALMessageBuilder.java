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
package com.ericsson.oss.services.security.npam.rest.resources;

import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.security.npam.api.cal.CALRecorderDTO;
import com.ericsson.oss.services.security.npam.rest.api.NPAMErrorJAXB;

public class CALMessageBuilder {

    @Inject
    private CALRecorderDTO cALRecorderDTO;

    @Inject
    private SystemRecorder systemRecorder;

    void buildMessage(final NPAMErrorJAXB errorJAXB) {
        final String errorMessage = buildErrorMessage(errorJAXB);
        cALRecorderDTO.setErrorResult(errorMessage);
        if (systemRecorder.isCompactAuditEnabled()) {
            systemRecorder.recordCompactAudit(cALRecorderDTO.getUsername(), cALRecorderDTO.getCommandCALDetail(), CommandPhase.FINISHED_WITH_ERROR,
                    cALRecorderDTO.getSource(), cALRecorderDTO.getResource(), cALRecorderDTO.getIp(), cALRecorderDTO.getCookie(),
                    cALRecorderDTO.getAdditionalInfo());
        } else {
            systemRecorder.recordCommand(cALRecorderDTO.getCommandName(), CommandPhase.FINISHED_WITH_ERROR, cALRecorderDTO.getSource(),
                    cALRecorderDTO.getResource(), cALRecorderDTO.getAdditionalInfo());
        }
    }

    private static String buildErrorMessage(final NPAMErrorJAXB errorJAXB) {
        String stringForm = "Error: %s";
        stringForm = errorJAXB.getErrorDetails().isEmpty() ? stringForm : stringForm.concat(" Details: %s");
        return String.format(stringForm, errorJAXB.getUserMessage(), errorJAXB.getErrorDetails().replace("\n", " ").trim());
    }
}
