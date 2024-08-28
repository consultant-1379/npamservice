/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.security.npam.rest.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException;
import com.ericsson.oss.services.security.npam.api.cal.CALRecorderDTO;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NEInfo;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEAccount;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEAccountResponse;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NetworkElementStatus;
import com.ericsson.oss.services.security.npam.api.rest.NeAccountService;
import com.ericsson.oss.services.security.npam.rest.api.NPAMErrorJAXB;
import com.ericsson.oss.services.security.npam.rest.api.NPAMExportJAXB;
import com.ericsson.oss.services.security.npam.rest.api.NPAMGetNeAccountJAXB;
import com.ericsson.oss.services.security.npam.rest.api.NPamNEAccountCredentialJAXB;
import com.ericsson.oss.services.security.npam.rest.api.NPamNEAccountJAXB;
import com.ericsson.oss.services.security.npam.rest.api.NPamNEAccountResponseJAXB;

import io.swagger.v3.oas.annotations.Hidden;
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
public class NPAMNeAccountServiceRest {
    private static final String ID_1 = "1";
    private static final String ID_2 = "2";

    @Inject
    private Logger logger;

    @Inject
    NeAccountService neAccountService;

    @Inject
    CALRecorderDTO cALRecorderDTO;

    @POST
    @Path("/neaccount")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "NPAM NE Account List", description = "This operation supports the read of Network Element Accounts for selected Network Elements", tags = {
            "Npam NE Account" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All NE account for every selected  nodes successfully returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NPamNEAccountResponseJAXB.class)))),
            @ApiResponse(responseCode = "400", description = "Bad Request",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))) })
    public Response retrieveNeAccounts(@QueryParam("id") @Parameter(description = "NE Account Id", required = false) final List<String> neAccountIdList,
                                       @QueryParam("neNpamStatus")
                                       @Parameter(description = "NE Account Status", required = false) final List<NetworkElementStatus> neStatusList,
                                       @RequestBody(description = "The selected NEInfo", required = true) final NPAMGetNeAccountJAXB resourcesJSON) {
        cALRecorderDTO.setJsonBody(resourcesJSON);
        final List<NPamNEAccountResponseJAXB> neAccountStateResponseJAXB = new ArrayList<>();
        final List<NPamNEAccountResponse> neAccountStatusResponseList = neAccountService.getNEAccountStatusList(resourcesJSON.getSelectedNEs(),
                neAccountIdList, neStatusList);
        for (final NPamNEAccountResponse elem : neAccountStatusResponseList) {
            final NPamNEAccountResponseJAXB elemJAXB = new NPamNEAccountResponseJAXB();
            elemJAXB.setNeName(elem.getNeName());
            elemJAXB.setNeNpamStatus(elem.getNeNpamStatus());
            final List<NPamNEAccountJAXB> neAccountsJAXB = new ArrayList<>();
            for (final NPamNEAccount neAccount : elem.getNeAccounts()) {
                final NPamNEAccountJAXB nPamNEAccountJAXB = new NPamNEAccountJAXB(neAccount);
                neAccountsJAXB.add(nPamNEAccountJAXB);
            }
            elemJAXB.setNeAccounts(neAccountsJAXB);
            neAccountStateResponseJAXB.add(elemJAXB);
        }
        return Response.ok(neAccountStateResponseJAXB).build();
    }

    @GET
    @Path("/neaccount/details/{neName}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "NPAM NE Account Details", description = "This operation supports the read of Network Element Accounts details for a specific Network Element", tags = {
            "Npam NE Account" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All NE account for the node successfully returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NPamNEAccountCredentialJAXB.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            //@ApiResponse(responseCode = "404", description = "Not Found",
            //content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))) })
    public Response retrievePasswordForNode(@PathParam("neName") @Parameter(description = "The name of the Network Element", required = true) final String neName) {
        final List<NPamNEAccount> neAccounts = neAccountService.getNEAccounts(neName);
        final List<NPamNEAccountCredentialJAXB> nPamNEAccountCredentialsJAXB = new ArrayList<>();

        for (final NPamNEAccount neAccount : neAccounts) {
            if (!neAccount.getNetworkElementAccountId().equals(ID_1) && !neAccount.getNetworkElementAccountId().equals(ID_2)) {
                continue;
            }
            final NPamNEAccountCredentialJAXB nPamNEAccountCredentialJAXB = new NPamNEAccountCredentialJAXB(neAccount);
            final String ipAddress = neAccountService.retrieveIpAddress(neName);
            nPamNEAccountCredentialJAXB.setIpAddress(ipAddress);
            try {
                if (neAccount.getCurrentPswd() != null) {
                    nPamNEAccountCredentialJAXB.setCurrentPswd(neAccountService.getPwdInPlainText(neAccount.getCurrentPswd()));
                }
            } catch (final SecurityViolationException e) {
                nPamNEAccountCredentialJAXB.setCurrentPswd("********");
            }

            nPamNEAccountCredentialsJAXB.add(nPamNEAccountCredentialJAXB);
        }
        return Response.ok(nPamNEAccountCredentialsJAXB).build();
    }

    @POST
    @Path("/retrieveNeFrom")
    @Produces({ MediaType.APPLICATION_JSON })
    @Hidden
    @Operation(summary = "Gets all NetworkElement for a given NEInfo", tags = { "Npam NE Account", "test" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All Network Elements for the given NEInfo is successfully returned",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = String.class)))),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NPAMErrorJAXB.class))) })
    public Response allNeJobForSpecificJob(@RequestBody(description = "The selected NEInfo", required = true,
                                           content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                           schema = @Schema(implementation = NEInfo.class))) final NEInfo neInfo) {
        final Set<String> neNames = neAccountService.getAllNes(neInfo);
        return Response.ok(neNames).build();

    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON })
    @Path("/neaccount/export/")
    @Operation(summary = "NPAM NE Account Export", description = "This operation supports the exporting of Network Element Accounts.", tags = {
            "Npam NE Account" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All NE account for every selected  nodes successfully exported",
                        content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM)),
            @ApiResponse(responseCode = "400", description = "Bad Request",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NPAMErrorJAXB.class))) })
    public Response exportNeAccounts(@QueryParam("id") @Parameter(description = "NE Account Id",
                                             required = false) final List<String> neAccountIdList,
                                     @QueryParam("neNpamStatus") @Parameter(description = "NE Account Status",
                                             required = false) final List<NetworkElementStatus> neStatusList,
                                     @RequestBody(description = "Encryption key and the selected NEInfo", required = false) final NPAMExportJAXB exportData) {
        final List<String> neAccountIds = new ArrayList<>();

        if (neAccountIdList.isEmpty()) {
            neAccountIds.add(ID_1);
        } else {
            neAccountIds.addAll(neAccountIdList);
        }
        final List<NetworkElementStatus> statusList = new ArrayList<>();

        if (neStatusList.isEmpty()) {
            statusList.add(NetworkElementStatus.MANAGED);
        } else {
            statusList.addAll(neStatusList);
        }
        final NEInfo selectedNE = (exportData == null ? null : exportData.getSelectedNEs());
        final String encryptionKey = (exportData == null ? null : exportData.getEncryptionKey());

        final byte[] encryptedData = neAccountService.exportNeAccount(selectedNE, neAccountIds, statusList, encryptionKey);
        String extension = ".enc";
        if (encryptionKey == null) {
            extension = ".csv";
        }
        if (encryptionKey != null) {
            exportData.setEncryptionKey("********");
        }
        cALRecorderDTO.setJsonBody(exportData);
        return Response.ok(encryptedData, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"exportNeAccounts_" + System.currentTimeMillis() + extension + "\"").build();
    }
}
