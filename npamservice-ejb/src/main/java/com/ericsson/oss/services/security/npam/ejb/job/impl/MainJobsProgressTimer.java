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
package com.ericsson.oss.services.security.npam.ejb.job.impl;


import com.ericsson.oss.services.security.npam.ejb.database.availability.DatabaseAvailabilityChecker;
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus;
import com.ericsson.oss.services.security.npam.ejb.listener.MembershipListenerInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.*;
import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

@Singleton
@Startup
public class MainJobsProgressTimer {

    @Resource
    private TimerService timerService;


    private static final long STARTUP_TIMEOUT = TimeUnit.SECONDS.toMillis(90);
    private static final long PROGRESS_UPDATE_SLOW_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    private static final long PROGRESS_UPDATE_FAST_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    @Inject
    private DatabaseAvailabilityChecker databaseAvailabilityChecker;

    @Inject
    private MembershipListenerInterface membershipListenerInterface;
    @Inject
    private MainJobsProgressUpdateHandler mainJobsProgressUpdateHandler;

    @Inject
    private NodePamConfigStatus nodePamConfigStatus;

    private static final Logger LOGGER = LoggerFactory.getLogger(MainJobsProgressTimer.class);

    private boolean mainJobSchedulerPhase = true;


    @PostConstruct
    public void setupTimer() {
        LOGGER.info("MainJobsProgressTimer STARTED with startingTimerInMillisec={}", STARTUP_TIMEOUT);
        configureTimerInSec(STARTUP_TIMEOUT);
    }

    @Timeout
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void mainJobsTimer(final Timer timer) {
        boolean isThereAnyRunningJob = false;
        try {
            if (membershipListenerInterface.isMaster() && databaseAvailabilityChecker.isAvailable() && nodePamConfigStatus.isEnabled()) {
                if (mainJobSchedulerPhase) {
                    mainJobsProgressUpdateHandler.scheduleScheduledMainJobs();
                    mainJobSchedulerPhase = false;
                } else {
                    mainJobSchedulerPhase = true;
                }
                isThereAnyRunningJob = mainJobsProgressUpdateHandler.updateRunningMainJobs();
            } else {
                LOGGER.debug(" Main Job Progress skipped because or it is slave or database is not available ");
            }
        } catch (Exception e) {
            LOGGER.error("MainJobsProgressTimer:: Exception occured during mainJobSchedulerPhase={} e.getMEssage={}", mainJobSchedulerPhase, e.getMessage());
        } finally {
            if (isThereAnyRunningJob) {
                configureTimerInSec(PROGRESS_UPDATE_FAST_TIMEOUT);
            } else {
                configureTimerInSec(PROGRESS_UPDATE_SLOW_TIMEOUT);
            }
        }
    }

    private void configureTimerInSec(final long  duration) {
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerService.createSingleActionTimer(duration, timerConfig);
    }

}
