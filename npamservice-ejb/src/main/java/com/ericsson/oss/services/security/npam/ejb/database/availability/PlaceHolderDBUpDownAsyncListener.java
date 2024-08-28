package com.ericsson.oss.services.security.npam.ejb.database.availability;

import com.ericsson.oss.services.security.npam.ejb.database.availability.event.DataBaseAvailable;
import com.ericsson.oss.services.security.npam.ejb.database.availability.event.DataBaseEvent;
import com.ericsson.oss.services.security.npam.ejb.database.availability.event.DataBaseNotAvailable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.*;
import javax.enterprise.event.Observes;

/*
* This is a placehodler class to receive async up/down event (sent by our callback class via fire( new DataBaseEvent());)
* */
@Singleton
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PlaceHolderDBUpDownAsyncListener {
    private final Logger logger = LoggerFactory.getLogger(PlaceHolderDBUpDownAsyncListener.class);

    public void onDbAvailable(@Observes @DataBaseAvailable final DataBaseEvent event) {
        logger.info("PlaceHolderDBUpDownAsyncListener: Database Available from Event: DO NOTHING (add implementation if you want to do something)");
    }

    public void onDbNotAvailable(@Observes @DataBaseNotAvailable final DataBaseEvent event) {
        logger.info("PlaceHolderDBUpDownAsyncListener: Database Not Available from Event: DO NOTHING (add implementation if you want to do something)");
    }
}
