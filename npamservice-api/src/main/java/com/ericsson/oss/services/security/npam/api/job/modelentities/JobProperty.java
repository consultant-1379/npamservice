/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.security.npam.api.job.modelentities;

import java.io.Serializable;
import java.util.List;

public class JobProperty implements Serializable {
    //Possible JProperty Keys under JobPropertyUtility
    private static final long serialVersionUID = 1L;

    public static final String JOB_PROPERTY_KEY = "key";
    public static final String JOB_PROPERTY_VALUE = "value";

    private String key;
    private String value;

    public JobProperty() {
        this.key = "";
        this.value = "";
    }

    public JobProperty(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public static String getPropertyValue(final String propertyName, final List<JobProperty> jobProperties) {
        for (final JobProperty jobproperty : jobProperties) {
            if (propertyName.equals(jobproperty.getKey())) {
                return jobproperty.getValue();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "JobProperty{" +
                attributeToString(JOB_PROPERTY_KEY, getKey()) +
                attributeToString(JOB_PROPERTY_VALUE, getValue()) +
                '}';
    }

    protected String attributeToString(final String attributeName, final Object attributeValue) {
        return  new StringBuilder()
                .append(attributeName)
                .append("=")
                .append(attributeValue)
                .append(",")
                .toString();
    }
}
