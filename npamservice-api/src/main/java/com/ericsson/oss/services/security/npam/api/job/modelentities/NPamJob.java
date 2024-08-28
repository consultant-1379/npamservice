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
import java.util.Date;
import java.util.List;

public class NPamJob  extends  NPamAbstractJob implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String JOB_ID = "jobId";
    public static final String JOB_TYPE = "jobType";
    public static final String JOB_EXECUTION_INDEX = "executionIndex";
    public static final String JOB_SELECTED_NES = "selectedNEs";
    public static final String JOB_TEMPLATE_ID = "templateJobId";
    public static final String JOB_NUMBER_NETWORK_ELEMENTS = "numberOfNetworkElements";
    public static final String JOB_SCHEDULED_TIME = "scheduledTime";
    public static final String JOB_PROPERTIES = "jobProperties";
    public static final String JOB_PROGRESS_PERCENTAGE = "progressPercentage";
    public static final String JOB_NAME = "name";
    public static final String JOB_OWNER = "owner";

    private Long jobId;
    private JobType jobType;
    private int executionIndex;
    private NEInfo selectedNEs;
    private long templateJobId;
    private int numberOfNetworkElements;
    private Date scheduledTime;
    private List<JobProperty> jobProperties;
    private double progressPercentage;
    private String name;
    private String owner;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public List<JobProperty> getJobProperties() {
        return jobProperties;
    }

    public void setJobProperties(List<JobProperty> jobProperties) {
        this.jobProperties = jobProperties;
    }

    public int getExecutionIndex() {
        return executionIndex;
    }

    public void setExecutionIndex(int executionIndex) {
        this.executionIndex = executionIndex;
    }

    public NEInfo getSelectedNEs() {
        return selectedNEs;
    }

    public void setSelectedNEs(NEInfo selectedNEs) {
        this.selectedNEs = selectedNEs;
    }

    public long getTemplateJobId() {
        return templateJobId;
    }

    public void setTemplateJobId(long templateJobId) {
        this.templateJobId = templateJobId;
    }

    public int getNumberOfNetworkElements() {
        return numberOfNetworkElements;
    }

    public void setNumberOfNetworkElements(int numberOfNetworkElements) {
        this.numberOfNetworkElements = numberOfNetworkElements;
    }

    public Date getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(Date scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public double getProgressPercentage() { return progressPercentage; }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getOwner() { return owner; }

    public void setOwner(String owner) { this.owner = owner; }

    @Override
    public String toString() {
        return "NPamJob{" +
                attributeToString(JOB_ID,getJobId()) +
                appendCommonAttributeToString() +
                attributeToString(JOB_TYPE,getJobType()) +
                attributeToString(JOB_EXECUTION_INDEX,getExecutionIndex()) +
                attributeToString(JOB_SELECTED_NES,getSelectedNEs()) +
                attributeToString(JOB_TEMPLATE_ID,getTemplateJobId()) +
                attributeToString(JOB_NUMBER_NETWORK_ELEMENTS,getNumberOfNetworkElements()) +
                attributeToString(JOB_SCHEDULED_TIME,getScheduledTime()) +
                attributeToString(JOB_PROPERTIES,getJobProperties()) +
                attributeToString(JOB_PROGRESS_PERCENTAGE, getProgressPercentage()) +
                attributeToString(JOB_NAME, getName()) +
                attributeToString(JOB_OWNER, getOwner()) +
                '}';
    }

}
