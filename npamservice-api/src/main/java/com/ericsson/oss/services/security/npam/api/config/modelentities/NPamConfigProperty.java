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
package com.ericsson.oss.services.security.npam.api.config.modelentities;

import java.io.Serializable;
import java.util.Objects;

public class NPamConfigProperty implements Serializable {

    private static final long serialVersionUID = -577219512913346351L;
    private String name;
    private String value;

    public NPamConfigProperty() {
        this.name = "";
        this.value = "";
    }

    public NPamConfigProperty(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value
     *            the value to set
     */
    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "{\"name:\"" + name + "\", \"value:\"" + value + "\"}";
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
        final NPamConfigProperty other = (NPamConfigProperty) obj;
        return Objects.equals(name, other.name);
    }

}
