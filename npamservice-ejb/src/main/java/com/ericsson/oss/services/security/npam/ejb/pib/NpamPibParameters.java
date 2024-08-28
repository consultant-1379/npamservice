package com.ericsson.oss.services.security.npam.ejb.pib;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class NpamPibParameters {

    @Inject
    @Configured(propertyName = "houseKeepingDays")
    private int npamHouseKeepingDays;

    public int getNpamHouseKeepingDays() {
        return npamHouseKeepingDays;
    }

    void listenForHouseKeepingDaysChanges(@Observes @ConfigurationChangeNotification(propertyName = "houseKeepingDays") final int npamHouseKeepingDays) {
        this.npamHouseKeepingDays = npamHouseKeepingDays;
    }
}
