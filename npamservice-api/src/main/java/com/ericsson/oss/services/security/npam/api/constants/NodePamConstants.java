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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NodePamConstants {

    /*
     *  Generic Variables
     *
     *  */

    public static final String DEFAULT_PAM_USER_ID = "pamUserId";

    public static final String NO_ERRROR_VALUE = null;
    public static final String NULL_SUBJECT_NAME = null;
    public static final String EMPTY_SUBJECT_NAME = "";
    public static final String NULL_PASSWORD = null;
    public static final String NULL_USERNAME = null;

    public static final String NULL_MUID = null;
    public static final String MUID_ONE = "1";
    public static final String MUID_TWO = "2";

    public static final boolean REMOTE_MANAGEMENT_TRUE = true;
    public static final boolean REMOTE_MANAGEMENT_FALSE = false;

    public static final boolean CHECK_IF_REMOTE_MANAGEMENT_IS_TRUE = true;

    public static final Boolean RESTRICT_MAINTENANCE_USER_NULL = null;

    public static final String IMPORT_FOLDER = "/ericsson/config_mgt/npam/import/";
    public static final String IMPORT_FOLDER_WITH_JOBID = "/ericsson/config_mgt/npam/import_job/";
    public static final String FILE_SEPARATOR = ";";
    public static final String END_LINE = "\n";

    // NPamConfig Parameters names
    public static final String NPAM_CONFIG_NPAM = "npam";
    public static final String NPAM_CONFIG_CBRS_DOMAIN = "cbrs";
    public static final String NPAM_CONFIG_EMERGENCY_USER = "emergency_user";
    public static final String NPAM_CONFIG_RESTRICT_MAINTENANCE_USER = "restrict_maintenance_user";

    // NPamConfig values / connstants
    public static final String NPAM_CONFIG_PARAM_ENABLED = "enabled";
    public static final String NPAM_CONFIG_PARAM_DISABLED = "disabled";
    public static final String NPAM_CONFIG_PROPERTIES = "nPamConfigProperties";
    public static final String SUBJECT_NAME_CBRS_DOMAIN = "CN=MaintenanceUser";

    public static final int NUM_OF_NE_ACCOUNTS = 3;

    public static final boolean RESET_CREDENTIALS_FALSE = false;
    public static final boolean RESET_CREDENTIALS_TRUE = true;

    /*
     *  DPS Variables
     *
     *  */

    public static final String CONFIGURATION_LIVE = "Live";

    //NetworkElement
    public static final String NETWORK_ELEMENT_MO = "NetworkElement";
    public static final String NETYPE = "neType";
    public static final String NODEROOTREF = "nodeRootRef";

    //CmFunction
    public static final String CMFUNCTION_MO = "CmFunction";
    public static final String SYNCSTATUS = "syncStatus";
    public static final String SYNCHRONIZED = "SYNCHRONIZED";
    public static final String UNSYNCHRONIZED = "UNSYNCHRONIZED";


    //SecurityFunction
    public static final String SECURITY_FUNCTION_MO= "SecurityFunction";

    //NetworkElementAccount
    public static final String NETWORK_ELEMENT_ACCOUNT_MO_NAMESPACE = "OSS_NE_SEC_DEF";
    public static final String NETWORK_ELEMENT_ACCOUNT_MO = "NetworkElementAccount";

    //MaintenanceUserSecurity
    //Example <...>/ManagedElement=LTE04dg2ERBS00004,SystemFunctions=1,SecM=1,UserManagement=1,UserIdentity=1,MaintenanceUserSecurity=1
    public static final String MAINTENANCE_USER_SECURITY_MO_NAMESPACE = "RcsUser";
    public static final String MAINTENANCE_USER_SECURITY_MO = "MaintenanceUserSecurity";
    public static final String MAINTENANCE_USER_SECURITY_REMOTE_MANAGEMENT_ATTRIBUTE="remoteManagement";

    //MaintenanceUser
    public static final String MAINTENANCE_USER_MO_NAMESPACE = "RcsUser";
    public static final String MAINTENANCE_USER_MO = "MaintenanceUser";   //example SystemFunctions=1,SecM=1,UserManagement=1,UserIdentity=1,MaintenanceUser
    public static final String MAINTENANCE_USER_SUBJECT_NAME = "subjectName";

    //UserIdentity
    public static final String USER_IDENTITY_MO = "UserIdentity";

    //Capabilities & Action
    public static final String NPAM_CONFIG_RESOURCE = "npam_config";
    public static final String NPAM_NEACCOUNT_RESOURCE = "neaccount";
    public static final String NPAM_NEACCOUNT_JOB_RESOURCE = "neaccount_job";
    public static final String NPAM_NEACCOUNT_IMPORT_RESOURCE = "neaccount_import";
    public static final String NPAM_NEACCOUNT_EXPORT_RESOURCE = "neaccount_export";
    public static final String NPAM_NEACCOUNT_PWD_RESOURCE = "neaccount_pwd";

    public static final String READ_ACTION = "read";
    public static final String UPDATE_ACTION = "update";
    public static final String CREATE_ACTION = "create";
    public static final String EXECUTE_ACTION = "execute";
    public static final String QUERY_ACTION = "query";
    public static final String DELETE_ACTION = "delete";
    public static final List<String> NE_TYPES_SUPPORTING_FUNCTIONALITY = Collections.unmodifiableList(Arrays.asList("RadioNode"));
    private NodePamConstants() {}
}
