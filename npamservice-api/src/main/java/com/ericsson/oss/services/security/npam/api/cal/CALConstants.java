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
package com.ericsson.oss.services.security.npam.api.cal;

public class CALConstants {

    private CALConstants() {
    }

    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String IMPORT = "import";
    public static final String NPAMCONFIG = "npamconfig";

    public static final String DESCRIPTION = "description";
    public static final String JOB_TYPE = "job_type";
    public static final String JOB_PROPERTIES = "job_properties";
    public static final String SELECTED_NE = "selectedNe";
    public static final String MAIN_SCHEDULE = "main_schedule";

    public static final String NPAM_JOB_CREATE = "<NPAM_JOB_CREATE>";
    public static final String NPAM_JOB_CANCEL = "<NPAM_JOB_CANCEL>";
    public static final String NPAM_IMPORT_FILE = "<NPAM_IMPORT_FILE>";
    public static final String NPAM_NEACCOUNT_LIST = "<NPAM_NEACCOUNT_LIST>";
    public static final String NPAM_NEACCOUNT_EXPORT = "<NPAM_NEACCOUNT_EXPORT>";
    public static final String NPAM_CONFIG_UPDATE = "<NPAM_CONFIG_UPDATE>";

    public static final String NPAM_JOB_LIST = "<NPAM_JOB_LIST>";
    public static final String NPAM_JOB_CONFIGURATION = "<NPAM_JOB_CONFIGURATION>";
    public static final String NPAM_JOB_NEDETAILS = "<NPAM_JOB_NEDETAILS>";
    public static final String NPAM_IMPORT_FILELIST = "<NPAM_IMPORT_FILELIST>";
    public static final String NPAM_NEACCOUNT_DETAILS = "<NPAM_NEACCOUNT_DETAILS>";
    public static final String NPAM_CONFIG_READ = "<NPAM_CONFIG_READ>";
    public static final String NPAM_CONFIG_STATUS = "<NPAM_CONFIG_STATUS>";

    public static final String CLIENT_IP_ADDRESS = "CLIENT_IP_ADDRESS";
    public static final String CLIENT_SESSION_ID = "CLIENT_SESSION_ID";

    public static final String UNKNOWN_USER = "UNKNOWN_USER";
    public static final String UNKNOWN_IP = "UNKNOWN_IP";
    public static final String UNKNOWN_COOKIE = "UNKNOWN_COOKIE";
}
