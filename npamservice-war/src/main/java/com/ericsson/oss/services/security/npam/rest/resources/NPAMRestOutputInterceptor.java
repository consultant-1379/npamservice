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

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.security.npam.api.cal.CALRecorderDTO;

@Provider
public class NPAMRestOutputInterceptor implements ContainerResponseFilter {
    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    CALRecorderDTO cALRecorderDTO;

    @Override
    public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext) throws IOException {
        if (responseContext.getStatus() == Status.OK.getStatusCode()) {
            if (!systemRecorder.isCompactAuditEnabled()) {
                systemRecorder.recordCommand(cALRecorderDTO.getCommandName(), CommandPhase.FINISHED_WITH_SUCCESS, cALRecorderDTO.getSource(),
                        cALRecorderDTO.getResource(), "");
            } else {
                systemRecorder.recordCompactAudit(cALRecorderDTO.getUsername(), cALRecorderDTO.getCommandCALDetail(),
                        CommandPhase.FINISHED_WITH_SUCCESS, cALRecorderDTO.getSource(), cALRecorderDTO.getResource(), cALRecorderDTO.getIp(),
                        cALRecorderDTO.getCookie(), cALRecorderDTO.getAdditionalInfo());
            }
        }
    }

}
