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

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.ericsson.oss.services.security.npam.api.cal.CALRecorderDTO;
import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfig;
import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfigPropertiesEnum;
import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfigProperty;
import com.ericsson.oss.services.security.npam.api.rest.ConfigService;
import com.ericsson.oss.services.security.npam.rest.api.NPAMConfigPropertyJAXB;
import com.ericsson.oss.services.security.npam.rest.api.NPAMErrorJAXB;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
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
public class NPAMConfigServiceRest {

    @Inject
    ConfigService configService;

    @Inject
    CALRecorderDTO cALRecorderDTO;

    @POST
    @Path("/npamconfig")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    @Operation(summary = "NPAM Configuration Update", description = "This operation supports the update of NPAM configuration parameters.", tags = "Npam Config")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of pairs '{ name, value }'",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NPAMConfigPropertyJAXB.class)))),
            @ApiResponse(responseCode = "400", description = "Bad Request",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class)))})
    public Response updateNpamConfig(@RequestBody(description = "List of pairs '{ name, value }'", required = true) final List<NPAMConfigPropertyJAXB> nPamConfigProperties) {
        final NPamConfig nPamConfig = new NPamConfig();
        cALRecorderDTO.setJsonBody(nPamConfigProperties);
        //Parameter validation
        for (final NPAMConfigPropertyJAXB el : nPamConfigProperties) {
            nPamConfig.getnPamConfigProperties().add(new NPamConfigProperty(el.getName(), el.getValue()));
        }
        final NPamConfig updatedNpamConfig = configService.updateNPamConfig(nPamConfig);
        final List<NPAMConfigPropertyJAXB> responseNPamConfigProperties = new ArrayList<>();
        for (final NPamConfigProperty npamconfiProperty : updatedNpamConfig.getnPamConfigProperties()) {
            if (NPamConfigPropertiesEnum.isKnownPropertyName(npamconfiProperty.getName())) {
                responseNPamConfigProperties.add(new NPAMConfigPropertyJAXB(npamconfiProperty.getName(), npamconfiProperty.getValue()));
            }
        }
        return Response.status(Response.Status.OK).entity(responseNPamConfigProperties).build();
    }

    @GET
    @Path("/npamconfig")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "NPAM Configuration Read", description = "This operation supports the reading of NPAM application configuration parameters.", tags = "Npam Config")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of pairs '{ name, value }'",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NPAMConfigPropertyJAXB.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = NPAMErrorJAXB.class)))})
    public Response getNPamConfig() {
        final List<NPAMConfigPropertyJAXB> nPamConfigProperties = new ArrayList<>();
        for (final NPamConfigProperty npamconfigProperty : configService.getNPamConfig().getnPamConfigProperties()) {
            if (NPamConfigPropertiesEnum.isKnownPropertyName(npamconfigProperty.getName())) {
                nPamConfigProperties.add(new NPAMConfigPropertyJAXB(npamconfigProperty.getName(), npamconfigProperty.getValue()));
            }
        }
        return Response.ok(nPamConfigProperties).build();
    }

    @GET
    @Path("/npamconfigstatus")
    @Produces({ MediaType.APPLICATION_JSON })
    @Hidden
    @Operation(summary = "NPAM Configuration Read Cached Data", description = "This operation supports the reading of NPAM application configuration parameters from internal cached data.", tags = "Npam Config")
    public Response getNPamConfigCached() {
        final List<NPAMConfigPropertyJAXB> nPamConfigProperties = new ArrayList<>();
        for (final NPamConfigProperty npamconfiProperty : configService.getNPamConfigCached().getnPamConfigProperties()) {
            if (NPamConfigPropertiesEnum.isKnownPropertyName(npamconfiProperty.getName())) {
                nPamConfigProperties.add(new NPAMConfigPropertyJAXB(npamconfiProperty.getName(), npamconfiProperty.getValue()));
            }
        }
        return Response.ok(nPamConfigProperties).build();
    }
}
