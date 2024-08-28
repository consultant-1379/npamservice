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
package com.ericsson.oss.services.security.npam.api.constants;

public class JobConfigurationConstants {
    //CLEAN_COMMON: please remove all these constants and use static inside NPamJob,...    START
    public static final String PROPERTY_KEY = "key";
    //CLEAN_COMMON: please remove all these constants and use static inside NPamJob,...    STOP
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_VALUE = "value";
    public static final String JOB_NAME = "name";
    public static final String JOB_TYPE = "jobType";
    public static final String CREATION_TIME = "creationTime";
    public static final String DESCRIPTION = "description";
    public static final String OWNER = "owner";

    //CLEAN_COMMON: please remove all these constants and use static inside NPamJob,...    START
    public static final String JOBPROPERTIES = "jobProperties";
    public static final String TEMPLATEJOBID = "templateJobId";
    public static final String STATE = "state";
    public static final String EXECUTIONINDEX = "executionIndex";
    public static final String SCHEDULEDTIME = "scheduledTime";
    public static final String STARTTIME = "startTime";
    public static final String ENDTIME = "endTime";
    public static final String PROGRESSPERCENTAGE = "progressPercentage";
    public static final String RESULT = "result";
    public static final String NUMBER_OF_NETWORK_ELEMENTS = "numberOfNetworkElements";
    public static final String MAIN_JOB_ID = "mainJobId";
    public static final String NE_NAME = "neName";
    public static final String ERROR_DETAILS = "errorDetails";
    //CLEAN_COMMON: please remove all these constants and use static inside NPamJob,...     STOP

    public static final String MAIN_SCHEDULE = "mainSchedule";
    public static final String EXEC_MODE = "execMode";
    public static final String SCHEDULE_ATTRIBUTES = "scheduleAttributes";


    public static final String SELECTED_NES = "selectedNEs";
    //CLEAN_COMMON: please remove all these constants and use static inside NPamJob,...    START
    public static final String COLLECTION_NAMES = "collectionNames";
    public static final String SAVED_SEARCH_IDS = "savedSearchIds";
    public static final String NENAMES = "neNames";
    //CLEAN_COMMON: please remove all these constants and use static inside NPamJob,...    STOP

    //generic
    public static final String JOB_TEMPLATE_CREATE = "NPAM.JOBTEMPLATE_CREATE ";
    public static final String JOB_CREATE = "NPAM.JOB_CREATE ";

    public static final String NEJOB_CREATE = "NPAM.NEJOB_CREATE";
    public static final String NEJOBS_CREATION_FAILED = "NPAM.NEJOBS_CREATION_FAILED";

    private JobConfigurationConstants() {}
}
