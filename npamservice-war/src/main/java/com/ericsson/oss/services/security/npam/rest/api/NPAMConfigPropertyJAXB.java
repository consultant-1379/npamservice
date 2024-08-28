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

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NPAMConfigProperty", description = "Npam configuration parameter")
public class NPAMConfigPropertyJAXB {

    @Schema(description = "Name of the parameter. Possible values: 'npam' , 'cbrs'")
    private String name;
    @Schema(description = "Value of the parameter. Possible values : 'enabled' , 'disabled'")
    private String value;

    public NPAMConfigPropertyJAXB() {
    }

    public NPAMConfigPropertyJAXB(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String setName(final String _name) {
        return name = _name;
    }

    public String setValue(final String _value) {
        return value = _value;
    }

    @Override
    public String toString() {
        return "NPAMConfigPropertiesJAXB {name: " + name + ", value: " + value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NPAMConfigPropertyJAXB other = (NPAMConfigPropertyJAXB) obj;
        return Objects.equals(name, other.name);
    }

}
