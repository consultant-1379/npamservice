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

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import javax.inject.Inject;

/**
 * Observes database availability notifications
 */
@Singleton
@Startup
public class NodePamDatabaseAvailabilityNotificationObserver {

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private NodePamDatabaseAvailabilityCallback callback;

    @Inject
    private TimerService timerService;

    @Inject
    private Logger logger;

    private static final int MAX_ATTEMPTS = 20;
    private static final int STARTUP_TIMER = 30000;         // 30 sec.
    private static final int ATTEMPT_INTERVAL = 10000;       // 3 sec.

    private int errorCount = 0;

    /**
     * Schedule a job to be executed via the {@code TimerService}
     */
    @PostConstruct
    public void scheduleListenerForDpsNotification() {
        errorCount = 0;
        setAttemptsTimer(STARTUP_TIMER);
    }

    /*
     * Callback method to be called on expiry of timeout registered in {#scheduleListenerForDpsNotification} method.
     */
    @Timeout
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Lock(LockType.READ)
    public void listenForDpsNotifications(final Timer timer) {

        errorCount++;
        if (errorCount > MAX_ATTEMPTS) {
            logger.error("DPS failed to deploy within at least {} seconds", MAX_ATTEMPTS * ATTEMPT_INTERVAL / 1000);
        } else {
            try {
                dataPersistenceService.registerDpsAvailabilityCallback(callback);
                final String message = String
                    .format("Registering DPS availability callback for CM-reader. Attempt %d of %d", errorCount, MAX_ATTEMPTS);
                if (errorCount == 1) {
                    logger.info(message);
                } else {
                    logger.warn(message);
                }
            } catch (Exception e) {
                logger.warn("An unexpected {} occurred during DPS availability callback registration: {}", e.getClass().getCanonicalName(),
                       e.getMessage());
                setAttemptsTimer(ATTEMPT_INTERVAL);
            }
        }
    }

    private void setAttemptsTimer(final long duration) {
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerService.createSingleActionTimer(duration, timerConfig);
    }
}
