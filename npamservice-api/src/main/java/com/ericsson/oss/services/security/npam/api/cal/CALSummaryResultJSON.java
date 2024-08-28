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
package com.ericsson.oss.services.security.npam.api.cal;

public class CALSummaryResultJSON {
    private String opType;
    private String entity;
    private String totalNE;

    public CALSummaryResultJSON() {
    }

    public CALSummaryResultJSON(final String opType, final String entity, final String totalNE) {
        this.opType = opType;
        this.entity = entity;
        this.totalNE = totalNE;
    }

    /**
     * @return the opType
     */
    public String getOpType() {
        return opType;
    }

    /**
     * @param opType
     *            the opType to set
     */
    public void setOpType(final String opType) {
        this.opType = opType;
    }

    /**
     * @return the entity
     */
    public String getEntity() {
        return entity;
    }

    /**
     * @param entity
     *            the entity to set
     */
    public void setEntity(final String entity) {
        this.entity = entity;
    }

    /**
     * @return the totalNE
     */
    public String getTotalNE() {
        return totalNE;
    }

    /**
     * @param totalNE
     *            the totalNE to set
     */
    public void setTotalNE(final String totalNE) {
        this.totalNE = totalNE;
    }

}
