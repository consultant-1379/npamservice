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

public class JobSchedulerConstants {
   //To see scheduling description see "About NPAM scheduling" inside https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/BSY/%5BSPIKE%5D+%5BTORF-613106%5D+-+ESUM+-+Analysis+about+the+end+of+NHC+job

    //Scheduled Keys
    public static final String SA_START_DATE = "START_DATE";
    public static final String SA_END_DATE = "END_DATE";
    public static final String SA_REPEAT_TYPE = "REPEAT_TYPE";
    public static final String SA_REPEAT_COUNT = "REPEAT_COUNT";
    public static final String SA_REPEAT_ON = "REPEAT_ON";
    public static final String SA_OCCURRENCES = "OCCURRENCES";
    public static final int SCHEDULE_JOB_ERROR_START_INT = 100;

    //Scheduled Values

    //SA_REPEAT_TYPE possible values:
    public static final String REPEAT_TYPE_VALUE_WEEKLY = "Weekly";
    public static final String REPEAT_TYPE_VALUE_MONTHLY = "Monthly";
    public static final String REPEAT_TYPE_VALUE_YEARLY = "Yearly";

    //SA_REPEAT_COUNT:  how many frequency (example "2")
    //SA_REPEAT_ON: days in week (only when SA_REPEAT_TYPE="Weekly") specified as list (example "1,3,4")
    //SA_OCCURRENCES: max number of occurrence (example "10")
    private JobSchedulerConstants() {}
}
