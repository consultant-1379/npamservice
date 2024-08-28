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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class CALDetailResultJSON {
    private String opType;
    private String id;

    @JsonInclude(Include.NON_NULL)
    private Map<String, Object> currentValues;

    @JsonInclude(Include.NON_NULL)
    private Map<String, Object> oldValues;

    public CALDetailResultJSON() {
    }

    public CALDetailResultJSON(final String opType, final String id) {
        this.opType = opType;
        this.id = id;
    }

    /**
     * @param opType
     * @param id
     * @param currentValues
     * @param oldValues
     */
    public CALDetailResultJSON(final String opType, final String id, final Map<String, Object> currentValues, final Map<String, Object> oldValues) {
        this.opType = opType;
        this.id = id;
        this.currentValues = new HashMap<>(currentValues);
        this.oldValues = new HashMap<>(oldValues);
    }

    /**
     * @return the opType
     */
    public String getOpType() {
        return opType;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the currentValues
     */
    public Map<String, Object> getCurrentValues() {
        return currentValues == null ? null : Collections.unmodifiableMap(currentValues);
    }

    /**
     * @return the oldValues
     */
    public Map<String, Object> getOldValues() {
        return oldValues == null ? null : Collections.unmodifiableMap(oldValues);
    }

    /**
     * @param opType
     *            the opType to set
     */
    public void setOpType(final String opType) {
        this.opType = opType;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * @param currentValues
     *            the currentValues to set
     */
    public void setCurrentValues(final Map<String, Object> currentValues) {
        this.currentValues = new HashMap<>(currentValues);
    }

    /**
     * @param oldValues
     *            the oldValues to set
     */
    public void setOldValues(final Map<String, Object> oldValues) {
        this.oldValues = new HashMap<>(oldValues);
    }

    /**
     * @param key
     *            the key to add to the map currentValues
     * @param value
     *            the value to add to the map currentValues
     */
    public void addCurrentValues(final String key, final String value) {
        if (this.currentValues == null) {
            this.currentValues = new HashMap<>();
        }
        this.currentValues.put(key, value);
    }

    /**
     * @param key
     *            the key to add to the map currentValues
     * @param values
     *            the list of values to add to the map currentValues
     */
    public void addCurrentValues(final String key, final List<String> values) {
        if (this.currentValues == null) {
            this.currentValues = new HashMap<>();
        }
        this.currentValues.put(key, values);
    }

    /**
     * @param key
     *            the key to add to the map oldValues
     * @param value
     *            the value to add to the map oldValues
     */
    public void addOldValues(final String key, final String value) {
        if (this.oldValues == null) {
            this.oldValues = new HashMap<>();
        }
        this.oldValues.put(key, value);
    }

}
