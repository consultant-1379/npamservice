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

import java.util.Date;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.security.npam.api.interfaces.NPamConfigService;

@LocalBean
@Stateless
public class NPamConfigTimer {
    @Resource
    private SessionContext ctx;

    @Inject
    NPamConfigService nPamConfigService;

    private static final Logger logger = LoggerFactory.getLogger(NPamConfigTimer.class);

    private static final long TIMEOUT = 10000;

    public void scheduleTimerForInitNPamConfig() {
        logger.info("Scheduling single action timer for init NPam Configuration ");
        final TimerConfig config = new TimerConfig();
        config.setInfo("INIT_PHASE");
        config.setPersistent(false);
        final TimerService timeService = ctx.getTimerService();
        if (timeService != null) { //This is only for test purpose
            timeService.createSingleActionTimer(new Date(new Date().getTime() + TIMEOUT), config);
        } else {
            try {
                nPamConfigService.createNPamConfig();
            } catch (final Exception e) {
                logger.error("Init NPamConfig exception", e);
            }
        }
    }

    @Timeout
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void timeoutHandler(final Timer timer) {
        logger.info("Starting init NPam Configuration.");
        try {
            nPamConfigService.createNPamConfig();
            // TODO
            // Verify what happens when both instances of NPAMservice create the same NPamConfig. An exception is returned, or two object are created?
            timer.cancel();
        } catch (final Exception e) {
            logger.debug("Init NPamConfig exception", e);
            logger.error("Error on Initialization of NPam Configuration Procedure");
            scheduleTimerForInitNPamConfig();
        }
    }
}
