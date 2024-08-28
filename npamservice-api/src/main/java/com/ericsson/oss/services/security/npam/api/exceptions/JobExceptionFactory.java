/*
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 */
package com.ericsson.oss.services.security.npam.api.exceptions;

import javax.inject.Inject;

public class JobExceptionFactory {

    @Inject
    private JobErrorHandler errorHandler;

    public JobConfigurationException createJobConfigurationException(final JobError error, final Object... additionalInfo) {
        final String errorMessage = errorHandler.createErrorMessage(error, additionalInfo);
        return new JobConfigurationException(error.getCode(), errorMessage);
    }

}

