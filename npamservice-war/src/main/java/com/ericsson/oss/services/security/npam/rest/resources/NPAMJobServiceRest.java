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

import static com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants.PK_FILENAME;
import static com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants.PK_NEXT_PASSWORD;
import static com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants.PK_NEXT_USERNAME;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException;
import com.ericsson.oss.services.security.npam.api.cal.CALConstants;
import com.ericsson.oss.services.security.npam.api.cal.CALDetailResultJSON;
import com.ericsson.oss.services.security.npam.api.cal.CALRecorderDTO;
import com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJobTemplate;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEJob;
import com.ericsson.oss.services.security.npam.api.rest.JobService;
import com.ericsson.oss.services.security.npam.api.rest.NeAccountService;
import com.ericsson.oss.services.security.npam.rest.api.NPAMErrorJAXB;
import com.ericsson.oss.services.security.npam.rest.api.NPAMJobImportResponseJAXB;
import com.ericsson.oss.services.security.npam.rest.api.NPAMJobJAXB;
import com.ericsson.oss.services.security.npam.rest.api.NPAMJobTemplateJAXB;
import com.ericsson.oss.services.security.npam.rest.api.NPAMJobTemplateResponseJAXB;
import com.ericsson.oss.services.security.npam.rest.api.NPAMMultipartBodyJAXB;
import com.ericsson.oss.services.security.npam.rest.api.NPAMNeJobJAXB;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * @author DespicableUs
 */
@Path("/v1")
@RequestScoped
public class NPAMJobServiceRest {
    private static final String EQUAL_TO = "=";
    private static final String SEMICOLON = ";";
    private static final String MULTIPART_FILENAME = "filename";
    private static final String CONTENT_DISPOSITION = "Content-Disposition";

    @Inject
    private Logger logger;

    @Inject
    JobService jobService;

    @Inject
    NeAccountService neAccountService;

    @Inject
    CALRecorderDTO cALRecorderDTO;

    @Inject
    SystemRecorder systemRecorder;

    @POST
    @Path("/job/create")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    @Operation(summary = "NPAM Job Create", description = "This operation supports the creation of NPAM job.", tags = { "Npam Job" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Job correctly created, jobName is returned",
                    content = @Content(schema = @Schema(implementation = NPAMJobTemplateResponseJAXB.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))) })
    public Response createJob(@RequestBody(description = "The NPAMJobTemplate representing the Job to be created", required = true)
                                final NPAMJobTemplateJAXB nPAMJobTemplateJAXB) {
        final long jobTemplateId = jobService.createJobTemplate(nPAMJobTemplateJAXB.convertToNPamJobTemplate(cALRecorderDTO));
        jobService.createNewJob(jobTemplateId);

        if (systemRecorder.isCompactAuditEnabled()) {
            if (nPAMJobTemplateJAXB.getJobProperties() != null) {
                cryptJsonProperties(nPAMJobTemplateJAXB.getJobProperties());
            }

            cALRecorderDTO.setJsonBody(nPAMJobTemplateJAXB);

            final CALDetailResultJSON cALDetailResultJSON = new CALDetailResultJSON(CALConstants.CREATE, nPAMJobTemplateJAXB.getName());
            final Map<String, Object> currentValues = new HashMap<>();
            currentValues.put(CALConstants.DESCRIPTION, nPAMJobTemplateJAXB.getDescription());
            currentValues.put(CALConstants.JOB_TYPE, nPAMJobTemplateJAXB.getJobType());
            currentValues.put(CALConstants.JOB_PROPERTIES, nPAMJobTemplateJAXB.getJobProperties());
            currentValues.put(CALConstants.SELECTED_NE, nPAMJobTemplateJAXB.getSelectedNEs());
            currentValues.put(CALConstants.MAIN_SCHEDULE, nPAMJobTemplateJAXB.getMainSchedule());
            cALDetailResultJSON.setCurrentValues(currentValues);
            cALRecorderDTO.setDetailResult(Arrays.asList(cALDetailResultJSON));
        }
        return Response.status(Response.Status.OK).entity(new NPAMJobTemplateResponseJAXB(nPAMJobTemplateJAXB.getName())).build();
    }

    private void cryptJsonProperties(final List<JobProperty> jobProperties) {
        for (final JobProperty jobProp : jobProperties) {
            if (jobProp.getKey().equals(JobPropertyConstants.PK_NEXT_PASSWORD) || jobProp.getKey().equals(JobPropertyConstants.PK_NEXT_USERNAME)) {
                jobProp.setValue("********");
            }
        }
    }

    @GET
    @Path("/job/list")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "NPAM Job List", description = "This operation supports the reading of NPAM jobs", tags = { "Npam Job" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "NPamJob list is correctly returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NPAMJobJAXB.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))) })
    public Response jobList() {
        final String jobName = null;
        final List<NPAMJobJAXB> npamjobsJAXB = getNpamJobByName(jobName);
        return Response.ok(npamjobsJAXB).build();
    }

    @GET
    @Path("/job/list/{jobName}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "NPAM Job List", description = "This operation supports the reading of NPAM jobs for the specific jobName.", tags = {
            "Npam Job" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "NPamJob list for the given jobName is correctly returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NPAMJobJAXB.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))) })
    public Response jobList(@PathParam("jobName") @Parameter(description = "The name of the NPamJob.", required = true) final String jobName) {
        final List<NPAMJobJAXB> npamjobsJAXB = getNpamJobByName(jobName);
        return Response.ok(npamjobsJAXB).build();
    }

    @GET
    @Path("/job/configuration/{jobName}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "NPAM Job Configuration Details", description = "This operation supports the reading of NPAM job configuration detail.", tags = {
            "Npam Job" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "A NPAMJobTemplate is correctly returned",
                    content = @Content(schema = @Schema(implementation = NPAMJobTemplateJAXB.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))) })
    public Response jobTemplateDetails(@PathParam("jobName") @Parameter(description = "The name of the NPamJob.", required = true) final String jobName) {
        final NPamJobTemplate npamjobTemplate = jobService.getJobTemplate(jobName);
        final List<JobProperty> newJobProperties = new ArrayList<>();
        for (final JobProperty jobProp : npamjobTemplate.getJobProperties()) {

            if (jobProp.getKey().equals(PK_NEXT_PASSWORD)) {
                try {
                    newJobProperties.add(new JobProperty(PK_NEXT_PASSWORD, neAccountService.getPwdInPlainText(jobProp.getValue())));
                } catch (final SecurityViolationException e) {
                    newJobProperties.add(new JobProperty(PK_NEXT_PASSWORD, "********"));
                }
            } else if (jobProp.getKey().equals(PK_NEXT_USERNAME)) {
                newJobProperties.add(new JobProperty(PK_NEXT_USERNAME, neAccountService.getPwdInPlainTextNoRBAC(jobProp.getValue())));
            } else if (jobProp.getKey().equals(PK_FILENAME)) {
                newJobProperties.add(new JobProperty(PK_FILENAME, jobProp.getValue().substring(0, jobProp.getValue().lastIndexOf("_"))));
            } else {
                newJobProperties.add(jobProp);
            }
        }
        npamjobTemplate.setJobProperties(newJobProperties);
        final NPAMJobTemplateJAXB npamjobTemplateJAXB = new NPAMJobTemplateJAXB(npamjobTemplate);
        return Response.ok(npamjobTemplateJAXB).build();
    }

    // retrieve the status of each NEJobs providing a jobId
    @GET
    @Path("/job/nedetails/{jobInstanceId}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "NPAM NE Job Details", description = "This operation supports the reading of NPAM Ne job details.", tags = { "Npam Job" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "NPamNeJob list for the given jobInstanceId is correctly returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NPAMNeJobJAXB.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))) })
    public Response allNeJobForSpecificJob(@PathParam("jobInstanceId") @Parameter(description = "The NPamJob id.", required = true) final long jobInstanceId) {
        logger.info("get all NEJobs per specifico jobInstanceId");
        final List<NPamNEJob> npamNeJobs = jobService.getNeJobForJobId(jobInstanceId);
        final List<NPAMNeJobJAXB> npamNeJobsJAXB = new ArrayList<>();
        for (final NPamNEJob elem : npamNeJobs) {
            final NPAMNeJobJAXB npamNeJobJAXB = new NPAMNeJobJAXB(elem);
            npamNeJobsJAXB.add(npamNeJobJAXB);
        }
        return Response.ok(npamNeJobsJAXB).build();
    }

    // delete a specific NPamJob providing a jobName
    @POST
    @Path("/job/cancel/{jobName}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "NPAM Job Cancel", description = "This operation supports the cancellation of a single NPAM Job.", tags = { "Npam Job" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "NPamJob is correctly deleted.", content = @Content(mediaType = MediaType.APPLICATION_JSON,
                              schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))) })
    public Response deleteJob(@PathParam("jobName") @Parameter(description = "The name of the NPamJob.", required = true) String jobName) {
        logger.info("delete scheduled Job per specific jobName");
        // Replace pattern-breaking characters
        jobName = jobName.replaceAll("[\n\r\t]", "_");
        jobService.deleteJob(jobName);
        return Response.ok("Job " + jobName + " correctly deleted.").build();
    }

    @POST
    @Path("/job/import/file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({ MediaType.APPLICATION_JSON })
    @RequestBody(description = "The file to be imported.", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = NPAMMultipartBodyJAXB.class)))
    @Operation(summary = "NPAM Job Import File", description = "This operation supports file import to update credentials.", tags = { "Npam Job" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File is correctly imported.", content = @Content(mediaType = MediaType.APPLICATION_JSON,
                              schema = @Schema(implementation = NPAMJobImportResponseJAXB.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))) })
    public Response importJobFile(@Parameter(hidden = true) @MultipartForm final MultipartFormDataInput input,
                                  @DefaultValue("false") @QueryParam("overwrite")
                                  @Parameter(description = "A true/false optional (false) flag indicating whether the file "
                                          + "should be overwritten if already present", required = false) final Boolean overwrite) {
        if (input == null) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.BAD_REQUEST_INVALID_INPUT_JSON,
                    NPAMRestErrorsMessageDetails.BAD_REQUEST_INVALID_INPUT_DATA);
        }
        final Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
        final List<InputPart> inputParts = uploadForm.get("File");
        if (inputParts == null) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.BAD_REQUEST_INVALID_INPUT_JSON,
                    NPAMRestErrorsMessageDetails.BAD_REQUEST_INVALID_INPUT_DATA);
        }
        final String fileName = getFileName(inputParts.get(0).getHeaders());
        if (fileName == null) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.BAD_REQUEST_INVALID_INPUT_JSON,
                    NPAMRestErrorsMessageDetails.BAD_REQUEST_INVALID_INPUT_DATA);
        }
        if (systemRecorder.isCompactAuditEnabled()) {
            final CALDetailResultJSON cALDetailResultJSON = new CALDetailResultJSON(CALConstants.IMPORT, fileName);
            cALRecorderDTO.setDetailResult(Arrays.asList(cALDetailResultJSON));
        }
        logger.info("Importing fileName: {} , overwrite: {}", fileName, overwrite);
        try {
            jobService.updalodInputFileFromContent(inputParts.get(0).getBodyAsString(), fileName, overwrite);
        } catch (final IOException e) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_NFS_RWISSUE, e.getMessage());
        }
        final NPAMJobImportResponseJAXB response  = new NPAMJobImportResponseJAXB("File " + fileName + " correctly imported.");
        return Response.ok(response).build();
    }

    @GET
    @Path("/job/import/filelist")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "NPAM Job Imported Files List", description = "This operation retrieves the list of the imported file.", tags = {
            "Npam Job" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File list is correctly returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))) })
    public Response importedJobFilelist() {
        final String[] toRet = jobService.importedJobFilelist();
        logger.debug("NPAMImportedFileList : {} ", toRet != null ? Arrays.toString(toRet) : "[]");
        return Response.ok(toRet).build();
    }

    private String getFileName(final MultivaluedMap<String, String> headers) {
        final String[] contentDispositionHeader = headers.getFirst(CONTENT_DISPOSITION).split(SEMICOLON);
        for (final String name : contentDispositionHeader) {
            if ((name.trim().startsWith(MULTIPART_FILENAME))) {
                final String[] tmp = name.split(EQUAL_TO);
                return tmp[1].trim().replace("\"", "");
            }
        }
        return null;
    }

    private List<NPAMJobJAXB> getNpamJobByName(final String jobName) {
        final List<NPamJob> npamjobs = jobService.getMainJobs(jobName);
        final List<NPAMJobJAXB> npamjobsJAXB = new ArrayList<>();
        NPAMJobJAXB pamJobJAXB = null;
        for (final NPamJob elem : npamjobs) {
            pamJobJAXB = new NPAMJobJAXB(elem);
            npamjobsJAXB.add(pamJobJAXB);
        }
        return npamjobsJAXB;
    }
}
