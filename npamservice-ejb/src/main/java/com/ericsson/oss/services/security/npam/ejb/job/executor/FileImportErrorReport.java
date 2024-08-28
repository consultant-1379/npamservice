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
package com.ericsson.oss.services.security.npam.ejb.job.executor;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.END_LINE;

import java.util.ArrayList;
import java.util.List;

public class FileImportErrorReport {

    private final List<String> report = new ArrayList<>();

    public void addError(final String error) {
        report.add(error);

    }

    public String buildReport() {
        final StringBuilder toBeRet = new StringBuilder();
        for (final String el : report) {
            toBeRet.append(el).append(END_LINE);
        }
        return toBeRet.toString();
    }

    public int getSize() {
        return report.size();
    }

}
