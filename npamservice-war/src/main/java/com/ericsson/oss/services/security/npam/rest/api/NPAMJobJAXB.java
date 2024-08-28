/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
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

import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NPAMJob", description = "An object with NPAM Job details")
public class NPAMJobJAXB {
    @Schema(description = "Job identifier")
    private Long jobInstanceId;
    @Schema(description = "Job Name")
    private String name;
    @Schema(description = "Job state")
    private String state;
    @Schema(description = "Result of job creation")
    private String result;
    @Schema(description = "The job start time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = PATTERN_DATE_FORMAT)
    private Date startTime;
    @Schema(description = "The job end time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = PATTERN_DATE_FORMAT)
    private Date endTime;

    private JobType jobType;
    private int numberOfNetworkElements;
    private double progressPercentage;
    private String errorDetails;
    private String owner;

    public NPAMJobJAXB() {
    }

    public NPAMJobJAXB(final NPamJob pamJob) {
        fillTimingAttributes(pamJob);
        if (pamJob.getName() != null) {
            this.name = pamJob.getName();
        } else {
            this.name = "";
        }
        if (pamJob.getOwner() != null) {
            this.owner = pamJob.getOwner();
        } else {
            this.owner = "";
        }
        if (pamJob.getJobId() != null) {
            this.jobInstanceId = pamJob.getJobId();
        }
        if (pamJob.getState() != null) {
            this.state = pamJob.getState().getJobStateName();
        } else {
            this.state = "";
        }
        if (pamJob.getResult() != null) {
            this.result = pamJob.getResult().toString();
        } else {
            this.result = "";
        }

        if (pamJob.getErrorDetails() != null) {
            this.errorDetails = pamJob.getErrorDetails();
        } else {
            this.errorDetails = "";
        }
        this.jobType = pamJob.getJobType();
        this.numberOfNetworkElements = pamJob.getNumberOfNetworkElements();
        this.progressPercentage = pamJob.getProgressPercentage();

    }

    private void fillTimingAttributes(final NPamJob pamJob) {
        if (pamJob.getStartTime() != null) {
            this.startTime = pamJob.getScheduledTime();
        } else {
            this.startTime = new Date();
        }
        if (pamJob.getEndTime() != null) {
            this.endTime = pamJob.getEndTime();
        }
    }


    public Long getJobInstanceId() {
        return jobInstanceId;
    }

    public void setJobInstanceId(final Long jobInstanceId) {
        this.jobInstanceId = jobInstanceId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public String getResult() {
        return result;
    }

    public void setResult(final String result) {
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

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(final JobType type) {
        this.jobType = type;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(final String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public int getNumberOfNetworkElements() {
        return numberOfNetworkElements;
    }

    public void setNumberOfNetworkElements(final int numberOfNetworkElements) {
        this.numberOfNetworkElements = numberOfNetworkElements;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(final double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "NPAMJobJAXB [iterationId =" + jobInstanceId + ", name =" + name + ", state =" + state + ", result =" + result + ", startTime ="
                + startTime + ", endTime="
                + endTime + ", type =" + jobType + " , numberOfNetworkElements =" + numberOfNetworkElements
                + ", progressPercentage =" + progressPercentage + ", errorDetails =" + errorDetails + ", owner =" + owner + "]";
    }
}
