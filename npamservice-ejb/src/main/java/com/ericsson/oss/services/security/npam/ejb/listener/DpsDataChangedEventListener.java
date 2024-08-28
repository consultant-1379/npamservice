package com.ericsson.oss.services.security.npam.ejb.listener;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.sdk.eventbus.annotation.Consumes;
import com.ericsson.oss.services.security.npam.api.message.NodePamMessageProperties;
import com.ericsson.oss.services.security.npam.ejb.handler.DpsEventManager;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;


@ApplicationScoped
public class DpsDataChangedEventListener {

    @Inject
    MembershipListenerInterface membershipListenerInterface;

    @Inject
    private Logger logger;

    @Inject
    private DpsEventManager dpsEventManager;

    public static final String NPAM_CONFIG_TYPE = "NPamConfig";

    /**
     * Callback method to listen the DPS event about CmFunction object.
     *
     * @param event the DPS event generated
     */
        public void updateOnEvent(@Observes @Consumes(endpoint = NodePamMessageProperties.NODE_PAM_TOPIC_ENDPOINT) final DpsDataChangedEvent event) {
            boolean isMaster = isMaster();
            if ((event.getType() != null) && (isMaster || NPAM_CONFIG_TYPE.equals(event.getType())) ) {
                dpsEventManager.notifyEvent(event);
            } else {
                logger.info("NodePam::DpsDataChangedEventListener:: onEvent:: isMaster={} and event.getType={} so nothing to do ", isMaster, event.getType());
            }
    }

    /******************************
      P R I V A T E  M E T H O D S
     ******************************/
    private boolean isMaster() {
        return membershipListenerInterface.isMaster();
    }

}
