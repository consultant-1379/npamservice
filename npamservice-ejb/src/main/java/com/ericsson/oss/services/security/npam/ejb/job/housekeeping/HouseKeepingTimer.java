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
package com.ericsson.oss.services.security.npam.ejb.job.housekeeping;

import com.ericsson.oss.services.security.npam.ejb.database.availability.DatabaseAvailabilityChecker;
import com.ericsson.oss.services.security.npam.ejb.listener.MembershipListenerInterface;
import com.ericsson.oss.services.security.npam.ejb.pib.NpamPibParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.Timer;
import javax.ejb.Timeout;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

@Singleton
@Startup
public class HouseKeepingTimer {

    @Resource
    private TimerService timerService;

    @Inject
    private MembershipListenerInterface membershipListenerInterface;

    @Inject
    private JobsHouseKeepingService jobsHouseKeepingService;

    @Inject
    private DatabaseAvailabilityChecker databaseAvailabilityChecker;

    @Inject
    private NpamPibParameters npamPibParameters;

    private static final  long  STARTUP_TIMER = TimeUnit.MINUTES.toMillis(5);
    private static final  long  INTERVAL_TIMER = TimeUnit.DAYS.toMillis(1);

    private static final Logger LOGGER = LoggerFactory.getLogger(HouseKeepingTimer.class);

    @PostConstruct
    public void setupTimer() {
        configureTimerInMillis(STARTUP_TIMER);
    }

    @Timeout
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void houseKeepingTimer(final Timer timer) {
        try {
            final int maxJobAge = npamPibParameters.getNpamHouseKeepingDays();

            // Launch housekeeping for NPamJobTemplate, NPamJob, NPamNEJob
            if (membershipListenerInterface.isMaster() && databaseAvailabilityChecker.isAvailable()) {
                LOGGER.info("HK-houseKeepingTimer:: Starting Housekeeping for NPAM jobs, templates and files older than {} days", maxJobAge);
                jobsHouseKeepingService.triggerHouseKeepingOfJobsStartingFromTemplates(maxJobAge);

                // Launch housekeeping for files in import folder
                jobsHouseKeepingService.triggerHouseKeepingOfFiles(maxJobAge);
            } else {
                LOGGER.info("HK-houseKeepingTimer:: NPAM HouseKeeping skipped because or it is slave or database is not available. [isMaster = {}; DB.isAvailable: {}]",
                        membershipListenerInterface.isMaster(), databaseAvailabilityChecker.isAvailable());
            }

        } catch (Exception e) {
            LOGGER.info("HK-houseKeepingTimer:: Exception occurred while triggering job housekeeping - e.getMessage={}", e.getMessage());
        } finally {
            configureTimerInMillis(INTERVAL_TIMER);
        }
    }

    private void configureTimerInMillis(final long durationInMillis) {
        LOGGER.info("HK-configureTimerInMillis:: Schedule HouseKeeping will occur in {} milliseconds ", durationInMillis);
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerService.createSingleActionTimer(durationInMillis, timerConfig);
    }
}
