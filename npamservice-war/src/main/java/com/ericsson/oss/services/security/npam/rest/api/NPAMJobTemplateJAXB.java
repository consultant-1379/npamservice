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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ericsson.oss.services.security.npam.api.cal.CALConstants;
import com.ericsson.oss.services.security.npam.api.cal.CALRecorderDTO;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NEInfo;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJobTemplate;
import com.ericsson.oss.services.security.npam.api.job.modelentities.Schedule;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Schema(name = "NPAMJobTemplate", description = "Object with NPAM job creation details.")
public class NPAMJobTemplateJAXB {
    @Schema(description = "Job name")
    private String name;
    @Schema(description = "Free text job description")
    private String description;
    @Schema(description = "Kind of job")
    private JobType jobType;
    @Schema(description = "Required properties depending on the job type")
    private List<JobProperty> jobProperties;
    //@Schema(description = "Network Elements list involved in the job.") //not working as expected in openapi
    private NEInfo selectedNEs;
    //@Schema(description = "schedule configuration.")  //not working as expected in openapi
    private Schedule mainSchedule;
    @Schema(description = "NPAMJob owner.")
    private String owner;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = PATTERN_DATE_FORMAT)
    private Date creationTime;

    private static final Logger logger = LoggerFactory.getLogger(NPAMJobTemplateJAXB.class);

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(final JobType jobType) {
        this.jobType = jobType;
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

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(final Date creationTime) {
        this.creationTime = creationTime;
    }

    public NPAMJobTemplateJAXB() {
    }

    public NPAMJobTemplateJAXB(final NPamJobTemplate nPamJobTemplate) {
        if (nPamJobTemplate.getName() != null) {
            this.setName(nPamJobTemplate.getName());
        }
        if (nPamJobTemplate.getDescription() != null) {
            this.setDescription(nPamJobTemplate.getDescription());
        }
        if (nPamJobTemplate.getJobType() != null) {
            this.setJobType(nPamJobTemplate.getJobType());
        }

        if (nPamJobTemplate.getJobProperties() != null) {
            this.setJobProperties(nPamJobTemplate.getJobProperties());
        }

        if (nPamJobTemplate.getSelectedNEs() != null) {
            this.setSelectedNEs(nPamJobTemplate.getSelectedNEs());
        }
        if (nPamJobTemplate.getMainSchedule() != null) {
            this.setMainSchedule(nPamJobTemplate.getMainSchedule());
        }
        if (nPamJobTemplate.getOwner() != null) {
            this.setOwner(nPamJobTemplate.getOwner());
        }
        if (nPamJobTemplate.getCreationTime() != null) {
            this.setCreationTime(nPamJobTemplate.getCreationTime());
        }
    }

    public NPamJobTemplate convertToNPamJobTemplate(CALRecorderDTO calRecorderDTO) {
        logger.info("CALLL::convertToNPamJobTemplate");  // N.B. Logger, se Injected, non funziona
        final NPamJobTemplate jobTemplate = new NPamJobTemplate();
        if (this.getName() != null) {
            jobTemplate.setName(this.getName());
        } else {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_NAME);
        }
        if (this.getDescription() != null) {
            jobTemplate.setDescription(this.getDescription());
        } else {
            jobTemplate.setDescription("");
        }

        if (this.getJobProperties() != null) {
            jobTemplate.setJobProperties(this.getJobProperties());
        } else {
            jobTemplate.setJobProperties(new ArrayList<>());
        }

        jobTemplate.setJobProperties(getCALItems(jobTemplate.getJobProperties(), calRecorderDTO));

        if (this.getJobType() != null) {
            jobTemplate.setJobType(this.getJobType());
        } else {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_JOB_TYPE);
        }
        //TODO validate properties fields
        if (this.getMainSchedule() != null && this.getMainSchedule().getScheduleAttributes() != null
                && this.getMainSchedule().getExecMode() != null) {
            jobTemplate.setMainSchedule(this.getMainSchedule());
        } else {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MAIN_SCHEDULE);
        }
        if (this.getSelectedNEs() != null && this.getSelectedNEs().getNeNames() != null && this.getSelectedNEs().getCollectionNames() != null
                && this.getSelectedNEs().getSavedSearchIds() != null) {
            jobTemplate.setSelectedNEs(this.getSelectedNEs());
        } else {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES);
        }
        return jobTemplate;
    }

    @Override
    public String toString() {
        return "NPAMJobTemplateJAXB [name=" + name + ", description=" + description + ", jobType=" + jobType
                + ", jobProperties=" + jobProperties + ", selectedNEs=" + selectedNEs + ", mainSchedule=" + mainSchedule + ", owner=" + owner
                + ", creationTime=" + creationTime + "]";
    }

    private List<JobProperty> getCALItems(final List<JobProperty> jobProperties, CALRecorderDTO calRecorderDTO) {
        List<JobProperty> newJobProperties = new ArrayList<>(jobProperties);
        newJobProperties.add(new JobProperty(CALConstants.CLIENT_IP_ADDRESS, (calRecorderDTO.getIp() != null ? calRecorderDTO.getIp() : CALConstants.UNKNOWN_IP)));
        newJobProperties.add(new JobProperty(CALConstants.CLIENT_SESSION_ID, (calRecorderDTO.getCookie() != null ? calRecorderDTO.getCookie() : CALConstants.UNKNOWN_COOKIE)));
        return newJobProperties;
    }

}
