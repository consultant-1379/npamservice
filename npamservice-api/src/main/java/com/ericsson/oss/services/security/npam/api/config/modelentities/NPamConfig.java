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

import java.util.ArrayList;
import java.util.List;

public class NPamConfig {
    private List<NPamConfigProperty> nPamConfigProperties;
    private long poId;

    public NPamConfig() {
        this.nPamConfigProperties = new ArrayList<>();
        this.poId = -1;
    }

    /**
     * @return the nPamConfigProperties
     */
    public List<NPamConfigProperty> getnPamConfigProperties() {
        return nPamConfigProperties;
    }

    /**
     * @param nPamConfigProperties
     *            the nPamConfigProperties to set
     */
    public void setnPamConfigProperties(final List<NPamConfigProperty> nPamConfigProperties) {
        this.nPamConfigProperties = nPamConfigProperties;
    }

    /**
     * @return the poId
     */
    public long getPoId() {
        return poId;
    }

    /**
     * @param poId
     *            the poId to set
     */
    public void setPoId(final long poId) {
        this.poId = poId;
    }

    @Override
    public String toString() {
        return "NPamConfig [nPamConfigProperties=" + nPamConfigProperties + ", poId=" + poId + "]";
    }

}
