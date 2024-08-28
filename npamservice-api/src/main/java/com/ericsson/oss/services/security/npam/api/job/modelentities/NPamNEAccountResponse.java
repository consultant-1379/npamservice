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

import java.util.List;

public class NPamNEAccountResponse {

    private String neName;

    private NetworkElementStatus neNpamStatus;

    private List<NPamNEAccount> neAccounts;

    /**
     * @return the neName
     */
    public String getNeName() {
        return neName;
    }

    /**
     * @param neName
     *            the neName to set
     */
    public void setNeName(final String neName) {
        this.neName = neName;
    }

    /**
     * @return the neState
     */
    public NetworkElementStatus getNeNpamStatus() {
        return neNpamStatus;
    }

    /**
     * @param neState
     *            the neState to set
     */
    public void setNeNpamStatus(final NetworkElementStatus neStatus) {
        this.neNpamStatus = neStatus;
    }

    /**
     * @return the networkElementAccounts
     */
    public List<NPamNEAccount> getNeAccounts() {
        return neAccounts;
    }

    /**
     * @param networkElementAccounts
     *            the networkElementAccounts to set
     */
    public void setNeAccounts(final List<NPamNEAccount> networkElementAccounts) {
        this.neAccounts = networkElementAccounts;
    }

    @Override
    public String toString() {
        return "NPamNEAccountResponseJAXB [neName=" + neName + ", neNpamStatus=" + neNpamStatus + ", networkElementAccounts=" + neAccounts + "]";
    }
}
