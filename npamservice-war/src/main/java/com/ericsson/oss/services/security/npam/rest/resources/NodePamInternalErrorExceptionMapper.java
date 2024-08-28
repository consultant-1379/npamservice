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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage;
import com.ericsson.oss.services.security.npam.rest.api.NPAMErrorJAXB;

@Provider
public class NodePamInternalErrorExceptionMapper implements ExceptionMapper<Exception> {
    @Inject
    private Logger logger;

    @Inject
    CALMessageBuilder cALMessageBuilder;

    @Override
    public Response toResponse(final Exception exception) {
        final NPAMErrorJAXB errorJAXB = new NPAMErrorJAXB(NPamRestErrorMessage.INTERNAL_SERVER_ERROR.getMessage(),
                NPamRestErrorMessage.INTERNAL_SERVER_ERROR.getCode(), "See logs for Error Details");
        cALMessageBuilder.buildMessage(errorJAXB);
        logger.error("NodePamInternalErrorException");
        return Response.status(NPamRestErrorMessage.INTERNAL_SERVER_ERROR.getHttpStatusCode()).entity(errorJAXB).type(MediaType.APPLICATION_JSON)
                .build();
    }
}
