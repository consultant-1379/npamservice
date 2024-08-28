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

import java.util.HashMap;
import java.util.Map;

public enum JobState {

    CREATED("CREATED"),
    SCHEDULED("SCHEDULED"),

    SUBMITTED("SUBMITTED"),

    RUNNING("RUNNING"),
    COMPLETED("COMPLETED"),
    USER_CANCELLED("USER_CANCELLED");

    private final String jobStateName;

    private static Map<String, JobState> jobStates;

    static {

        jobStates = new HashMap<>();
        for (final JobState jobState : JobState.values()) {
            jobStates.put(jobState.getJobStateName(), jobState);

        }
    }
    /**
     * 
     */
    JobState(final String jobStateName) {
        this.jobStateName = jobStateName;
    }

    /**
     * @return the jobStateName
     */
    public String getJobStateName() {
        return jobStateName;
    }

    public static boolean isJobSubmitted(final JobState jobState) {
        return jobState == SUBMITTED;
    }

    public static boolean isJobScheduled(final JobState jobState) {
        return jobState == SCHEDULED;
    }

    public static boolean isJobRunning(final JobState jobState) {
        return jobState == RUNNING;
    }

    public static boolean isJobUserCancelled(final JobState jobState) {
        return jobState == USER_CANCELLED;
    }

    public static boolean isJobCompleted(final JobState jobState) {
        return jobState == COMPLETED;
    }

    public static boolean isJobAlreadyStarted(final JobState jobState) {
        return jobState == SUBMITTED || jobState == RUNNING;
    }
}
