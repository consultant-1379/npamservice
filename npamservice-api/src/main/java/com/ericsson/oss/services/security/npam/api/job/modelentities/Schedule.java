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


import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_END_DATE;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_OCCURRENCES;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_REPEAT_COUNT;
import static com.ericsson.oss.services.security.npam.api.constants.JobSchedulerConstants.SA_REPEAT_TYPE;

import java.io.Serializable;
import java.util.List;

public class Schedule implements Serializable {
    //see  Scheduled Attributes for valus

    private ExecMode execMode;
    private List<ScheduleProperty> scheduleAttributes;
    /**
     * @return the execMode
     */
    public ExecMode getExecMode() {
        return execMode;
    }
    /**
     * @param execMode the execMode to set
     */
    public void setExecMode(final ExecMode execMode) {
        this.execMode = execMode;
    }
    /**
     * @return the scheduleAttributes
     */
    public List<ScheduleProperty> getScheduleAttributes() {
        return scheduleAttributes;
    }
    /**
     * @param scheduleAttributes the scheduleAttributes to set
     */
    public void setScheduleAttributes(final List<ScheduleProperty> scheduleAttributes) {
        this.scheduleAttributes = scheduleAttributes;
    }

    public boolean checkIsPeriodic() {
        if (scheduleAttributes != null) {
            return (getScheduledAttributeValue(SA_REPEAT_TYPE) != null && getScheduledAttributeValue(SA_REPEAT_COUNT) != null);
        }
        return false;
    }

    public boolean checkIsWrongImmediate() {
        return (execMode == ExecMode.IMMEDIATE && scheduleAttributes != null && !scheduleAttributes.isEmpty());
    }
    
    public boolean checkIsImmediate() {
        return (execMode == ExecMode.IMMEDIATE && scheduleAttributes != null && scheduleAttributes.isEmpty());
    }


    public boolean checkIsWrongNonPeriodic() {
        return (execMode == ExecMode.SCHEDULED && scheduleAttributes != null &&
                (getScheduledAttributeValue(SA_REPEAT_TYPE)  != null ||
                getScheduledAttributeValue(SA_REPEAT_COUNT) != null ||
                getScheduledAttributeValue(SA_OCCURRENCES)  != null ||
                getScheduledAttributeValue(SA_END_DATE)     != null));
    }

    public String getScheduledAttributeValue(final String attributeName) {
        if (scheduleAttributes != null) {
            for (final ScheduleProperty scheduleProperty:scheduleAttributes) {
                if (scheduleProperty.getName().equalsIgnoreCase(attributeName)) {
                    return scheduleProperty.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Schedule{" +
                "execMode=" + execMode +
                ", scheduleAttributes=" + scheduleAttributes +
                '}';
    }
}
