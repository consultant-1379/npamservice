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

package com.ericsson.oss.services.security.npam.api.cal;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@RequestScoped
public class CALRecorderDTO extends AbstractRecorderDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    Logger logger;

    private String commandName;
    private String httpMethod;
    private String commandPath;
    private String jsonBody;

    private String source;
    private String resource;
    private String additionalInfo;

    private boolean isLogSystemRecorder;

    @Override
    @PostConstruct
    public void init() {
        super.init();
        this.commandName = "";
        this.httpMethod = "";
        this.commandPath = "";
        this.jsonBody = null;

        this.source = "";
        this.resource = "";
        this.additionalInfo = "";

        this.setLogSystemRecorder(false);
    }

    public String getCommandName() {
        return commandName;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getCommandPath() {
        return commandPath;
    }

    public String getJsonBody() {
        return jsonBody;
    }

    public String getSource() {
        return source;
    }

    public String getResource() {
        return resource;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public String getCommandCALDetail() {
        final StringBuilder ret = new StringBuilder();
        ret.append(commandName).append(" - ").append(httpMethod);
        ret.append(" Resource: ").append(commandPath);
        if (jsonBody != null) {
            ret.append(" Body: ").append(jsonBody);
        }
        return ret.toString();
    }

    public void setCommandName(final String commandName) {
        this.commandName = commandName;
    }

    public void setHttpMethod(final String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public void setCommandPath(final String commandPath) {
        this.commandPath = commandPath;
    }

    public void setCommandPath(final UriInfo uriInfo) {
        final String requestUri = uriInfo.getRequestUri().toString();
        final int index = requestUri.indexOf(uriInfo.getPath());
        setCommandPath(requestUri.substring(index, requestUri.length()));
    }

    public void setJsonBody(final Object jsonBody) {
        final ObjectWriter ow = new ObjectMapper().writer();
        try {
            final String json = ow.writeValueAsString(jsonBody);
            this.jsonBody = json;
        } catch (final JsonProcessingException e) {
            logger.info("JsonProcessing Exception: ", e);
        }
    }

    public void setSource(final String source) {
        this.source = source;
    }

    public void setResource(final String resource) {
        this.resource = resource;
    }

    public void setDetailResult(final List<CALDetailResultJSON> cALDetailResultJSON) {
        final ObjectMapper om = new ObjectMapper();
        final StringBuilder result = new StringBuilder();
        result.append("{\"detailResult\":[");
        for (final CALDetailResultJSON cALDetailResult : cALDetailResultJSON) {
            try {
                result.append(om.writeValueAsString(cALDetailResult)).append(",");
            } catch (final JsonProcessingException e) {
                logger.info("Issue during JSON to String conversion.", e);
            }
        }
        if (result.length() != 0 && result.charAt(result.length() - 1) == ',') {
            result.deleteCharAt(result.length() - 1);
        }
        result.append("]}");
        this.additionalInfo = result.toString();
    }

    public void setSummaryResult(final List<CALSummaryResultJSON> cALSummaryResult) {
        final ObjectMapper om = new ObjectMapper();
        final StringBuilder result = new StringBuilder();
        result.append("{\"summaryResult\":[");
        for (final CALSummaryResultJSON cALSummary : cALSummaryResult) {
            try {
                result.append(om.writeValueAsString(cALSummary)).append(",");
            } catch (final JsonProcessingException e) {
                logger.info("Issue during JSON to String conversion.", e);
            }
        }
        if (result.length() != 0 && result.charAt(result.length() - 1) == ',') {
            result.deleteCharAt(result.length() - 1);
        }
        result.append("]}");
        this.additionalInfo = result.toString();
    }

    public void setErrorResult(final String errorResult) {
        final StringBuilder result = new StringBuilder();
        result.append("{\"errorResult\":\"");
        result.append(errorResult);
        result.append("\"}");
        this.additionalInfo = result.toString();
    }

    public void setSysRecorderDetails(final String sysRecorderDetails) {
        this.setLogSystemRecorder(true);
        this.additionalInfo = sysRecorderDetails;
    }

    public void storeCommandBase(final String commandName, final String httpMethod, final String commandPath) {
        this.commandName = commandName;
        this.httpMethod = httpMethod;
        this.commandPath = commandPath;
    }

    public void storeCommandBase(final String commandName, final String httpMethod, final UriInfo uriInfo) {
        this.commandName = commandName;
        this.httpMethod = httpMethod;
        setCommandPath(uriInfo);
    }

    public void storeContext(final String ip, final String name, final Cookie cookie) {
        if ((ip != null) && (!ip.isEmpty())) {
            setIp(ip);
        }
        if ((name != null) && (!name.isEmpty())) {
            setUsername(name);
        }
        if (cookie != null) {
            setCookie(cookie.getValue());
        }
    }

    public boolean isLogSystemRecorder() {
        return isLogSystemRecorder;
    }

    public void setLogSystemRecorder(final boolean isLogSystemRecorder) {
        this.isLogSystemRecorder = isLogSystemRecorder;
    }

}
