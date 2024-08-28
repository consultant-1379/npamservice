/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.security.npam.api.constants;

/**
 * Created by enmadmin on 9/23/21.
 */
public enum NodePamApplication {
    NODEPAM("NODEPAM");

    final String name;


    NodePamApplication(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
