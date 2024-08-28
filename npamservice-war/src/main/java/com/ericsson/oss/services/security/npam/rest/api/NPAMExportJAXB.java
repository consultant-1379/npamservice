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

@Schema(name = "NPAMExport", description = "Contains the encryption key and the Network Elements involved in the export.")
public class NPAMExportJAXB {

    @Schema(description = "Provided key used to encrypt the exported file")
    private String encryptionKey;
    private NEInfo selectedNEs;

    /**
     * @return the encryptionKey
     */
    public String getEncryptionKey() {
        return encryptionKey;
    }

    /**
     * @param encryptionKey
     *            the encryptionKey to set
     */
    public void setEncryptionKey(final String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

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
