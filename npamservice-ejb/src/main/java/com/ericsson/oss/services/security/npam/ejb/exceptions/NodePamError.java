/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.security.npam.ejb.exceptions;

public enum NodePamError {

    /*
      S E R V E R - E R R O R
     */
    //UNEXPECTED_ERROR( "This is an unhandled system error, please check the error log for more details"),
    //SERVER_ERROR("{0}"),

    /*
     V A L I D A T I O N - E R R O R S
     */

    //DB or access validation
    //DATABASE_NOT_AVAILABLE( "Service is currently unavailable."),
    UNIMPEMENTED_USE_CASE("Not yet implemented UC {0}"),
    NODE_NOT_EXISTENT("The supplied Node is not configured"),
    FDN_NOT_FOUND("The supplied FDN={0} does not exist in the database"),
    UNSUPPORTED_NE_TYPE("Network Privileged Access Management functionality does not support neType={0}"),
    INVALID_NODE_ROOT("The supplied FDN={0} has invalid nodeRoot={1}"),
    NO_ENTRIES("No entries found for supplied Mo={0} with base FDN={1}"),
    TOO_MANY_ENTRIES("Too many entries found for supplied Mo={0} with base FDN={1}"),
    REMOTE_MANAGEMENT_VALUE_MISMATCH("Operation failed because of NE account(s) not yet configured"),
    REMOTE_MANAGEMENT_VALUE_NULL("Network Privileged Access Management functionality is not supported by Node with version less than 22Q4"),
    NOT_FOUND_MAINTENANCE_USER("The supplied FDN={0} does not have Maintenance User with id={1}"),
    NPAM_FUNCTIONALITY_DISABLED("Network Privileged Access Management functionality is disabled"),
    NULL_SYNC_STATUS("Invalid syncStatus null for node={0}"),
//    UNSYNCHRONIZED_SYNC_STATUS("Invalid syncStatus={0} for node={1}"),
    NOT_SYNCHRONIZED_SYNC_STATUS("Invalid syncStatus={0} for node={1}"),
    TBAC_SINGLE_NODE_ACCESS_DENIED("Access Denied. You do not have access to this node={0}"),
    NULL_PASSWORD_FOR_NODE("No password specified for node={0}"),
    CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION_MEDIATION_ERROR("{0} {1}"),
    CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION_TIMEOUT("Timeout: Operation exceed the timeout of 2 minutes {0}");

    private final String message;

    public static final String WARNING_MESSAGE_SKIPPED_REMOTE_MANAGEMENT_TRUE = "WARNING: Operation skipped because of NE account(s) already configured";
    public static final String WARNING_MESSAGE_SKIPPED_REMOTE_MANAGEMENT_FALSE = "WARNING: Operation skipped because of NE account(s) already detached";
    public static final String WARNING_MESSAGE_SKIPPED_CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION_TRUE  = "WARNING: Check and update operation skipped because of NE account(s) already configured";
    public static final String WARNING_MESSAGE_SKIPPED_CHECK_AND_UPDATE_NE_ACCOUNT_CONFIGURATION_FALSE = "WARNING: Check and update operation skipped because of NE account(s) already detached";

    NodePamError(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
