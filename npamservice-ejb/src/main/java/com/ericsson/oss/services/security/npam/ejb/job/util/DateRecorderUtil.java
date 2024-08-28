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
package com.ericsson.oss.services.security.npam.ejb.job.util;

import org.joda.time.DateTime;

import java.util.Date;

/**
 * This class is used to facilitate spock tests where dates are stored and verified via mock.
 */
@SuppressWarnings({"squid:S1186"})
public class DateRecorderUtil {

    public void logDate(DateTime dateTime) {}
    public void logDate(Date date) {}

}
