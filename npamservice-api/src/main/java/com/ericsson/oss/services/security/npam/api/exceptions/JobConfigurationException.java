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
package com.ericsson.oss.services.security.npam.api.exceptions;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class JobConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 5616557036361539638L;
    private final int code;

    public JobConfigurationException(final int code, final String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
