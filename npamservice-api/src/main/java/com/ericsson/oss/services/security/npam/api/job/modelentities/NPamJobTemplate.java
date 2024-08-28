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

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class NPamJobTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String owner;
    private Date creationTime;
    private String description;
    private JobType jobType;
    private List<JobProperty> jobProperties;
    private NEInfo selectedNEs;
    private Schedule mainSchedule;
    private Long jobTemplateId;

    /**
     * @return the creationTime
     */
    public Date getCreationTime() {
        return creationTime;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the jobType
     */
    public JobType getJobType() {
        return jobType;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }


    /**
     * @param creationTime
     *            the creationTime to set
     */
    public void setCreationTime(final Date creationTime) {
        this.creationTime = creationTime;
    }

    /**
     * @param description
     *            the description to set
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * @param jobType
     *            the jobType to set
     */
    public void setJobType(final JobType jobType) {
        this.jobType = jobType;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @param owner
     *            the owner to set
     */
    public void setOwner(final String owner) {
        this.owner = owner;
    }

    /**
     * @return the jobTemplateId
     */
    public Long getJobTemplateId() {
        return jobTemplateId;
    }

    /**
     * @param jobTemplateId
     *            the jobTemplateId to set
     */
    public void setJobTemplateId(final Long jobTemplateId) {
        this.jobTemplateId = jobTemplateId;
    }

    public List<JobProperty> getJobProperties() {
        return jobProperties;
    }

    public void setJobProperties(final List<JobProperty> jobProperties) {
        this.jobProperties = jobProperties;
    }

    public NEInfo getSelectedNEs() {
        return selectedNEs;
    }

    public void setSelectedNEs(final NEInfo selectedNEs) {
        this.selectedNEs = selectedNEs;
    }

    public Schedule getMainSchedule() {
        return mainSchedule;
    }

    public void setMainSchedule(final Schedule mainSchedule) {
        this.mainSchedule = mainSchedule;
    }

    @Override
    public String toString() {
        return "JobTemplate{" +
                "name='" + name + '\'' +
                ", owner='" + owner + '\'' +
                ", creationTime=" + creationTime +
                ", description='" + description + '\'' +
                ", jobType=" + jobType +
                ", jobProperties=" + jobProperties +
                ", selectedNEs=" + selectedNEs +
                ", mainSchedule=" + mainSchedule +
                ", jobTemplateId=" + jobTemplateId +
                '}';
    }
}
