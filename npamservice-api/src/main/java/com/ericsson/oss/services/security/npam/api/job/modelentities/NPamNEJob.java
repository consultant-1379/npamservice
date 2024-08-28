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

public class NPamNEJob extends  NPamAbstractJob implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String NEJOB_ID = "neJobId";
    public static final String NEJOB_NE_NAME = "neName";
    public static final String NEJOB_MAIN_JOB_ID = "mainJobId";
    public static final String STEP = "step";

    private Long neJobId;
    private String neName;
    private long mainJobId;
    private Step innerStep;

    public Long getNeJobId() {
        return neJobId;
    }

    public void setNeJobId(Long neJobId) {
        this.neJobId = neJobId;
    }

    public String getNeName() {
        return neName;
    }

    public void setNeName(String neName) {
        this.neName = neName;
    }

    public long getMainJobId() {
        return mainJobId;
    }

    public void setMainJobId(long mainJobId) {
        this.mainJobId = mainJobId;
    }

    public Step getStep() { return innerStep; }

    public void setStep(Step step) { this.innerStep = step; }

    @Override
    public String toString() {
        return "NPamNEJob{" +
                attributeToString(NEJOB_ID, getNeJobId()) +
                appendCommonAttributeToString() +
                attributeToString(NEJOB_NE_NAME, getNeName()) +
                attributeToString(NEJOB_MAIN_JOB_ID, getMainJobId()) +
                attributeToString(STEP, getStep()) +
                '}';
    }

}
