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

import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.inject.Inject;

/**
 * <p>Holds the database status.</p>
 * <p>This singleton bean ensures one single instance in the server with the appropriate locks on the read and write methods</p>
 */
@Singleton
public class NodePamDatabaseStatus {

    private boolean available = false;

    private Long startUnavailability = null;
    private boolean registeredAndAvailable = false;
    @Inject
    private Logger logger;

    // workaround only for Arquillian test.
    @PostConstruct
    public void settingInitialStatus() {
        if (System.getProperty("hack.for.integration.test.to.work.databasestatus.available") != null) {
            setAvailable(true);
        }
    }

    /**
     * Retrieves the database current status.
     * Any thread can read as long as there's no write thread running
     *
     * @return database current status
     */
    @Lock(LockType.READ)
    public boolean isAvailable() {
        return available;
    }

    /**
     * Retrieves the information about the database registration. The registration happens only when cm-reader is fully deployed.
     * Once registered, dps gets unregistered when cm-reader is undeployed.
     *
     * @return if data persistence service is registered. By default, it is unregistered (false). Once it is registered (true), it is never
     * unregistered in the life cycle (it stays in true).
     */
    @Lock(LockType.READ)
    public boolean isRegisteredAndAvailable() {
        return registeredAndAvailable;
    }

    /**
     * Updates the database status.
     * Concurrent access to this method is blocked.
     *
     * @param available
     *            new status (available = true; unavailable = false)
     */
    @Lock(LockType.WRITE)
    public void setAvailable(final boolean available) {

        // If this is the start of the unavailability, starts the timer.
        if (databaseHasBecomeUnavailable(available)) {
            startUnavailability = System.currentTimeMillis();
        }
        if (available && !registeredAndAvailable) {
            registeredAndAvailable = true;
            logger.info("setAvailable:: [NodePam Unavailability] Database comes Available for the first time");
        }
        // If the database is available again and there's a timer running, stop and log it.
        if (databaseHasBecomeAvailableAgain(available)) {
            final long unavailableTime = System.currentTimeMillis() - startUnavailability;
            startUnavailability = null;
            logger.warn("setAvailable:: [NodePam Unavailability] Database comes again Available after {} ms", unavailableTime);
        }

        this.available = available;
    }

    private boolean databaseHasBecomeAvailableAgain(final boolean available) {
        return available && startUnavailability != null;
    }

    private boolean databaseHasBecomeUnavailable(final boolean available) {
        return !available && startUnavailability == null;
    }
}
