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

import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_NAME;
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_VALUE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.security.npam.api.constants.NodePamConstants;

/*
 * To add a new property you have to add a new enum  (name, default_value)  and add an item to allowedValues with allowed values
 * If allowedValues doesn't contain enum property name all value will be allowed for that property
 */
public enum NPamConfigPropertiesEnum {

    NPAM_CONFIG_NPAM("npam", NodePamConstants.NPAM_CONFIG_PARAM_DISABLED), NPAM_CONFIG_CBRS_DOMAIN("cbrs",
            NodePamConstants.NPAM_CONFIG_PARAM_DISABLED);

    private final String propertyName;
    private final String propertyDefaultValue;

    public static final String NPAM_CONFIG_PARAM_ENABLED = "enabled";
    public static final String NPAM_CONFIG_PARAM_DISABLED = "disabled";

    private static Map<String, NPamConfigPropertiesEnum> configPropertiesMap;

    static {

        configPropertiesMap = new HashMap<>();
        for (final NPamConfigPropertiesEnum confProp : NPamConfigPropertiesEnum.values()) {
            configPropertiesMap.put(confProp.getPropertyName(), confProp);

        }
    }


    private NPamConfigPropertiesEnum(final String propertyName, final String propertyDefaultValue) {
        this.propertyName = propertyName;
        this.propertyDefaultValue = propertyDefaultValue;
    }

    public static Map<String, NPamConfigPropertiesEnum> getconfigPropertiesMap() {
        return configPropertiesMap;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getPropertyDefaultValue() {
        return propertyDefaultValue;
    }

    public static Map<String, String> getMapProperty(final NPamConfigPropertiesEnum prop) {
        final Map<String, String> propertyElem = new HashMap<>();
        propertyElem.put(PROPERTY_NAME, prop.propertyName);
        propertyElem.put(PROPERTY_VALUE, prop.propertyDefaultValue);
        return propertyElem;

    }

    public static List<NPamConfigProperty> npamConfigPropertiesList() {
        final List<NPamConfigProperty> propertyList = new ArrayList<>();
        for (final NPamConfigPropertiesEnum e : values()) {
            propertyList.add(new NPamConfigProperty(e.propertyName, e.propertyDefaultValue));
        }
        return propertyList;
    }

    public static boolean isKnownPropertyName(final String name) {
        for (final NPamConfigPropertiesEnum elem : values()) {
            if (elem.propertyName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static final Map<String, List<String>> allowedValues = new HashMap<>();

    /**
     * @return the allowedvalues
     */
    public static Map<String, List<String>> getAllowedvalues() {
        return allowedValues;
    }

    static {
        allowedValues.put("npam", new ArrayList<>(Arrays.asList(
                NPAM_CONFIG_PARAM_ENABLED,
                NPAM_CONFIG_PARAM_DISABLED
                )));
        allowedValues.put("cbrs", new ArrayList<>(Arrays.asList(
                NPAM_CONFIG_PARAM_ENABLED,
                NPAM_CONFIG_PARAM_DISABLED
                )));
    }
}
