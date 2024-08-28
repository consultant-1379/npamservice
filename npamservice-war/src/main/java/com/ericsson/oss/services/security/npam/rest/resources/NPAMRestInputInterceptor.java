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
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.security.npam.api.cal.CALRecorderDTO;

@Provider
public class NPAMRestInputInterceptor implements ContainerRequestFilter {
    public static final String FORWARDED_HEADER = "X-Forwarded-For";
    public static final String SSO_COOKIE_NAME = "iPlanetDirectoryPro";
    public static final String USER_NAME = "X-Tor-UserID";

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private CALRecorderDTO cALRecorderDTO;

    @Inject
    Logger logger;

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        logger.info("NPAMRestInputInterceptor");

        final String commandName = NPAMRestCommandLogMapper.getCommandName(requestContext.getMethod(), requestContext.getUriInfo());
        final String resource = NPAMRestCommandLogMapper.getPathParamValue(requestContext.getUriInfo().getPathParameters());
        final String source = requestContext.getUriInfo().getPath();
        final String resourceStr = !resource.isEmpty() ? resource : null;
        systemRecorder.recordCommand(commandName, CommandPhase.STARTED, source, resourceStr, "");
        Cookie sessionCookie = null;
        final Map<String, Cookie> cookies = requestContext.getCookies();
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();
        final String ip = headers.get(FORWARDED_HEADER) != null ? headers.get(FORWARDED_HEADER).get(0) : "";
        final String auth = headers.get(USER_NAME) != null ? headers.get(USER_NAME).get(0) : "";

        if (cookies != null) {
            sessionCookie = cookies.get(SSO_COOKIE_NAME) != null ? cookies.get(SSO_COOKIE_NAME) : null;
        }
        cALRecorderDTO.storeContext(ip, auth, sessionCookie);
        cALRecorderDTO.storeCommandBase(commandName, requestContext.getMethod(), requestContext.getUriInfo());
        cALRecorderDTO.setSource(source);
        cALRecorderDTO.setResource(resourceStr);
        cALRecorderDTO.setResource(resourceStr);
    }
}
