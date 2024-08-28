package com.ericsson.oss.services.security.npam.rest.resources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import com.ericsson.oss.services.security.npam.api.cal.CALConstants;

public class NPAMRestCommandLogMapper {
    private static final String UNKNOWN_COMMAND = "Unknown command : ";
    private static Map<String, String> commandNamePOST;
    private static Map<String, String> commandNameGET;

    static {
        commandNamePOST = new HashMap<>();
        commandNameGET = new HashMap<>();

        commandNamePOST.put("/v1/job/create", CALConstants.NPAM_JOB_CREATE);
        commandNamePOST.put("/v1/job/cancel/jobName", CALConstants.NPAM_JOB_CANCEL);
        commandNamePOST.put("/v1/job/import/file", CALConstants.NPAM_IMPORT_FILE);
        commandNamePOST.put("/v1/neaccount", CALConstants.NPAM_NEACCOUNT_LIST);
        commandNamePOST.put("/v1/neaccount/export", CALConstants.NPAM_NEACCOUNT_EXPORT);
        commandNamePOST.put("/v1/npamconfig", CALConstants.NPAM_CONFIG_UPDATE);

        commandNameGET.put("/v1/job/list", CALConstants.NPAM_JOB_LIST);
        commandNameGET.put("/v1/job/list/jobName", CALConstants.NPAM_JOB_LIST);
        commandNameGET.put("/v1/job/configuration/jobName", CALConstants.NPAM_JOB_CONFIGURATION);
        commandNameGET.put("/v1/job/nedetails/jobInstanceId", CALConstants.NPAM_JOB_NEDETAILS);
        commandNameGET.put("/v1/job/import/filelist", CALConstants.NPAM_IMPORT_FILELIST);
        commandNameGET.put("/v1/neaccount/details/neName", CALConstants.NPAM_NEACCOUNT_DETAILS);
        commandNameGET.put("/v1/npamconfig", CALConstants.NPAM_CONFIG_READ);
        commandNameGET.put("/v1/npamconfigstatus", CALConstants.NPAM_CONFIG_STATUS);
    }

    private NPAMRestCommandLogMapper() {
    }

    public static String getCommandName(final String httpReq, final UriInfo uriInfo) {
        final MultivaluedMap<String, String> pathParams = uriInfo.getPathParameters();
        String url = uriInfo.getPath();
        if (pathParams != null) {
            for (final Entry<String, List<String>> param : pathParams.entrySet()) {
                url = url.replace(param.getValue().get(0), param.getKey());
            }
        }

        if (httpReq.equals("POST")) {
            return commandNamePOST.get(url) != null ? commandNamePOST.get(url) : UNKNOWN_COMMAND + url;
        } else if (httpReq.equals("GET")) {
            return commandNameGET.get(url) != null ? commandNameGET.get(url) : UNKNOWN_COMMAND + url;
        }
        return UNKNOWN_COMMAND + url;
    }

    public static String getPathParamValue(final MultivaluedMap<String, String> pathParam) {
        final String pathParameters = "";
        if (pathParam != null && !pathParam.isEmpty()) {
            final StringBuilder resource = new StringBuilder();
            for (final Entry<String, List<String>> param : pathParam.entrySet()) {
                for (final String el : param.getValue()) {
                    resource.append(el).append(" ");
                }
            }
            return resource.toString().trim();
        }
        return pathParameters;
    }

    public static String getQueryParameters(final MultivaluedMap<String, String> queryParam) {
        String queryParameters = "";
        if (queryParam != null && !queryParam.isEmpty()) {
            for (final Entry<String, List<String>> param : queryParam.entrySet()) {
                final StringBuilder resource = new StringBuilder();
                resource.append(",");
                resource.append(param.getKey()).append(":");
                for (final String el : param.getValue()) {
                    resource.append(el).append(" ");
                }
                queryParameters = queryParameters.concat(resource.toString().trim());
            }

            if (!queryParameters.isEmpty()) {
                queryParameters = queryParameters.substring(1, queryParameters.length());
            }
        }
        return queryParameters;
    }

}
