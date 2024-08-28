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

public class JobPropertyConstants {
    //JProperty Keys
    public static final String PK_NETWORK_ELEMENT_ACCOUNT_ID = "ACCOUNT_ID";
    // used by ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE
    public static final String PK_FILENAME = "FILENAME";

    // used by ROTATE_NE_ACCOUNT_CREDENTIALS
    public static final String PK_NEXT_USERNAME = "USERNAME";
    public static final String PK_NEXT_PASSWORD = "PASSWORD";

    private JobPropertyConstants() {}
}
