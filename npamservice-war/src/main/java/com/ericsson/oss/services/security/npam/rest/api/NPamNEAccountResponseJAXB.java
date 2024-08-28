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

import java.util.List;

import com.ericsson.oss.services.security.npam.api.job.modelentities.NetworkElementStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NPamNEAccountResponse", description = "A list of Network Element Account for every selected NE")
public class NPamNEAccountResponseJAXB {

    @Schema(description = "Network Element name")
    private String neName;
    private NetworkElementStatus neNpamStatus;
    @Schema(description = "A list of NPAM NEAccount for the involved NE")
    private List<NPamNEAccountJAXB> neAccounts;

    public String getNeName() {
        return neName;
    }

    public void setNeName(final String neName) {
        this.neName = neName;
    }

    public NetworkElementStatus getNeNpamStatus() {
        return neNpamStatus;
    }

    public void setNeNpamStatus(final NetworkElementStatus neStatus) {
        this.neNpamStatus = neStatus;
    }

    public List<NPamNEAccountJAXB> getNeAccounts() {
        return neAccounts;
    }

    public void setNeAccounts(final List<NPamNEAccountJAXB> networkElementAccounts) {
        this.neAccounts = networkElementAccounts;
    }

    @Override
    public String toString() {
        return "NPamNEAccountResponseJAXB [neName=" + neName + ", neNpamStatus=" + neNpamStatus + ", networkElementAccounts=" + neAccounts + "]";
    }
}
