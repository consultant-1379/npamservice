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
package com.ericsson.oss.services.security.npam.api.job.modelentities;

public enum NetworkElementStatus {
    MANAGED("MANAGED"), NOT_MANAGED("NOT_MANAGED"), NOT_SUPPORTED("NOT_SUPPORTED");

    private final String state;

    private NetworkElementStatus(final String attribute) {
        this.state = attribute;
    }

    /**
     * @return the state
     */
    public String getState() {
        return state;
    }
}
