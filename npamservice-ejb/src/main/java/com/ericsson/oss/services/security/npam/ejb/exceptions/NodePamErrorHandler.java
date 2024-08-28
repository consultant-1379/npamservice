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

import java.text.MessageFormat;

public class NodePamErrorHandler {

    public String createErrorMessage(final NodePamError error, final Object... objectsForError) {
        final String errorMessage = error.getMessage();
        final MessageFormat messageFormat = new MessageFormat(errorMessage);
        return messageFormat.format(objectsForError);
    }
}