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

public enum JobResult {

    SUCCESS("SUCCESS"), FAILED("FAILED"), SKIPPED("SKIPPED");

    private String result;

    private JobResult(final String jobResult) {
        this.result = jobResult;
    }

    public String getJobResult() {
        return result;
    }

}
