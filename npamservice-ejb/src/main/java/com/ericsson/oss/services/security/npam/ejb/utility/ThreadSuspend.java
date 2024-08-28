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
package com.ericsson.oss.services.security.npam.ejb.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1118"})
public class ThreadSuspend {

    private static final Logger log = LoggerFactory.getLogger(ThreadSuspend.class);

    @SuppressWarnings({"squid:S2142"})
    public static void waitFor(final long millisecond) {
        try {
            Thread.sleep(millisecond);
        } catch (final InterruptedException e) {
            log.error("Exception in sleep {}", e.getMessage());
        }
    }
}
