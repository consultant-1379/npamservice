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
package com.ericsson.oss.services.security.npam.rest.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NPAMJobTemplateResponse", description = "The NPamJob name")
public class NPAMJobTemplateResponseJAXB {
    @Schema(description = "NPAMJob name")
    private String name;

    public NPAMJobTemplateResponseJAXB() {
    }

    public NPAMJobTemplateResponseJAXB(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "NPAMJobTemplateResponseJAXB [name=" + name + "]";
    }


}
