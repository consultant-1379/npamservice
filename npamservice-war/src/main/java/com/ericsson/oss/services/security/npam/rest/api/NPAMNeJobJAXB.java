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

import static com.ericsson.oss.services.security.npam.api.rest.NPamConstants.PATTERN_DATE_FORMAT;

import java.util.Date;

import com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobState;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEJob;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NPAMNeJob", description = "An object with NPAM Ne job details")
public class NPAMNeJobJAXB {

    @Schema(description = "Network Element name associated to the NPAM Ne job")
    private String neName;
    @Schema(description = "State of the NPAM Ne job")
    private JobState state;

    @Schema(description = "The start time of the NPAM Ne job")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = PATTERN_DATE_FORMAT)
    private Date startTime;

    @Schema(description = "The end time of the NPAM Ne job")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = PATTERN_DATE_FORMAT)
    private Date endTime;

    @Schema(description = "Result of the execution of the job")
    private JobResult result;
    @Schema(description = "Details in case of result different from SUCCESS")
    private String errorDetails;

    public NPAMNeJobJAXB() {

    }

    public NPAMNeJobJAXB(final NPamNEJob npamNeJob) {
        if (npamNeJob.getNeName() != null) {
            this.neName = npamNeJob.getNeName();
        } else {
            this.neName = "";
        }
        this.state = npamNeJob.getState();
        this.result = npamNeJob.getResult();

        if (npamNeJob.getStartTime() != null) {
            this.startTime = npamNeJob.getStartTime();
        } else {
            this.startTime = new Date();
        }
        if (npamNeJob.getEndTime() != null) {
            this.endTime = npamNeJob.getEndTime();
        } else {
            this.endTime = new Date();
        }
        if (npamNeJob.getErrorDetails() != null) {
            this.errorDetails = npamNeJob.getErrorDetails();
        } else {
            this.errorDetails = "";
        }
    }

    public String getNeName() {
        return neName;
    }

    public void setNeName(final String neName) {
        this.neName = neName;
    }

    public JobState getState() {
        return state;
    }

    public void setState(final JobState state) {
        this.state = state;
    }

    public JobResult getResult() {
        return result;
    }

    public void setResult(final JobResult result) {
        this.result = result;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(final Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(final Date endTime) {
        this.endTime = endTime;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(final String errorDetails) {
        this.errorDetails = errorDetails;
    }

    @Override
    public String toString() {
        return "NPAMNeJobJAXB [ neName=" + neName + ", state=" + state + ", result=" + result
                + ", startTime=" + startTime + ", endTime=" + endTime + ", errorDetails=" + errorDetails + "]";
    }

}
