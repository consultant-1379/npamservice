/*
 * ------------------------------------------------------------------------------
 * *******************************************************************************
 *  COPYRIGHT Ericsson  2017
 *
 *  The copyright to the computer program(s) herein is the property of
 *  Ericsson Inc. The programs may be used and/or copied only with written
 *  permission from Ericsson Inc. or in accordance with the terms and
 *  conditions stipulated in the agreement/contract under which the
 *  program(s) have been supplied.
 * ******************************************************************************
 * ----------------------------------------------------------------------------
 */

package com.ericsson.oss.services.security.npam.ejb.database.availability;

import javax.inject.Inject;

/**
 * Uses {@link NodePamDatabaseStatus} to get the availability of Data Persistence Service/Database.
 */
public class NodePamDatabaseAvailabilityChecker implements DatabaseAvailabilityChecker {

    @Inject
    private NodePamDatabaseStatus databaseStatus;

    @Override
    public boolean isAvailable() {
        return databaseStatus.isAvailable();
    }

    @Override
    public boolean isRegisteredAndAvailable() {
        return databaseStatus.isRegisteredAndAvailable();
    }
}
