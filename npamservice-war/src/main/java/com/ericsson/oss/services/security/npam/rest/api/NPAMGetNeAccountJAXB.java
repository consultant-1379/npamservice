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

import com.ericsson.oss.services.security.npam.api.job.modelentities.NEInfo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NPAMGetNeAccount", description = "Network Elements list for which retrieve NEAccounts")
public class NPAMGetNeAccountJAXB {

    private NEInfo selectedNEs;

    /**
     * @return the neInfo
     */
    public NEInfo getSelectedNEs() {
        return selectedNEs;
    }

    /**
     * @param neInfo
     *            the neInfo to set
     */
    public void setSelectedNEs(final NEInfo selectedNEs) {
        this.selectedNEs = selectedNEs;
    }
}
