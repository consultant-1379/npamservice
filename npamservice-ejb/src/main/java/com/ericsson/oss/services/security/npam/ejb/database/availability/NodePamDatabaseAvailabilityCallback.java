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

import com.ericsson.oss.itpf.datalayer.dps.availability.DpsAvailabilityCallback;
import com.ericsson.oss.services.security.npam.ejb.database.availability.event.DataBaseAvailable;
import com.ericsson.oss.services.security.npam.ejb.database.availability.event.DataBaseEvent;
import com.ericsson.oss.services.security.npam.ejb.database.availability.event.DataBaseNotAvailable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

/**
 * Checks database availability
 */
@ApplicationScoped
public class NodePamDatabaseAvailabilityCallback implements DpsAvailabilityCallback {

    private static final Logger logger = LoggerFactory.getLogger(NodePamDatabaseAvailabilityCallback.class);

    @Inject
    private NodePamDatabaseStatus databaseStatus;

    @Inject
    @DataBaseAvailable
    private Event<DataBaseEvent> dbAvailable;

    @Inject
    @DataBaseNotAvailable
    private Event<DataBaseEvent> dbNotAvailable;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceAvailable() {
        logger.warn("onServiceAvailable:: [NodePam Unavailability] DPS is available again.");
        databaseStatus.setAvailable(true);
        dbAvailable.fire( new DataBaseEvent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceUnavailable() {
        logger.warn("onServiceAvailable:: [NodePam Unavailability] DPS is unavailable.");
        databaseStatus.setAvailable(false);
        dbNotAvailable.fire( new DataBaseEvent());
    }

    @Override
    public String getCallbackName() {
        return NodePamDatabaseAvailabilityCallback.class.getCanonicalName();
    }
}