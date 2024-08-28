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

public class NEInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String NEINFO_COLLECTION_NAMES = "collectionNames";
    public static final String NEINFO_NE_NAMES = "neNames";
    public static final String NEINFO_SAVED_SEARCH_IDS = "savedSearchIds";

    private List<String> collectionNames;
    private List<String> neNames;
    private List<String> savedSearchIds;

    public List<String> getCollectionNames() {
        return collectionNames;
    }

    public void setCollectionNames(final List<String> collectionNames) {
        this.collectionNames = collectionNames;
    }

    public List<String> getNeNames() {
        return neNames;
    }

    public void setNeNames(final List<String> neNames) {
        this.neNames = neNames;
    }

    public List<String> getSavedSearchIds() {
        return savedSearchIds;
    }

    public void setSavedSearchIds(final List<String> savedSearchIds) {
        this.savedSearchIds = savedSearchIds;
    }

    @Override
    public String toString() {
        return "NEInfo [" +
                attributeToString(NEINFO_COLLECTION_NAMES, getCollectionNames()) +
                attributeToString(NEINFO_NE_NAMES, getNeNames()) +
                attributeToString(NEINFO_SAVED_SEARCH_IDS, getSavedSearchIds()) +
                "]";
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
