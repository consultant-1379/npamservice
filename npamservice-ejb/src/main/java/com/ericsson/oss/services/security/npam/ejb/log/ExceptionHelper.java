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
package com.ericsson.oss.services.security.npam.ejb.log;

import org.slf4j.Logger;

import javax.ejb.EJBException;
import javax.inject.Inject;

public class ExceptionHelper {

    @Inject
    private Logger logger;

    public Throwable getRootCauseAndRewrap(final Throwable throwable) {
        final Throwable cause = getRootCause(throwable);

        /**
         * throwable cannot be null but SonarQube needs this check to pass the code review
         */
        if ((throwable != null) && (throwable.equals(cause))) {
            return throwable;
        }

        /**
         * throwable cannot be null but SonarQube needs this check to pass the code review
         */
        if (cause != null) {
            return new Exception("(" + cause.getMessage() + ")", cause);
        } else {
            // unreachable code...
            return new Exception("(Unexpected throwable cause null)");
        }
    }

    /*
     * P R I V A T E - M E T H O D S
     */
    private Throwable getRootCause(final Throwable throwable) {
        Throwable cause = null;

        if (throwable instanceof EJBException) {
            cause = ((EJBException) throwable).getCausedByException();
        }
        if ((throwable != null) && (throwable.getSuppressed().length > 0)) {
            logger.debug("getRootCause: reading suppressed exception - Lenght of suppressed exception array: {}", throwable.getSuppressed().length);
            logger.debug("getRootCause: reading suppressed exception - Message: {}", throwable.getMessage());
            final Throwable[] throwableArray = throwable.getSuppressed();
            cause = throwableArray[0];
        }
        /**
         * throwable cannot be null but SonarQube needs this check to pass the code review
         */
        if ((throwable != null) && (cause == null)) {
            cause = throwable.getCause();
        }
        if (cause == null) {
            return throwable;
        }
        return getRootCause(cause);
    }
}