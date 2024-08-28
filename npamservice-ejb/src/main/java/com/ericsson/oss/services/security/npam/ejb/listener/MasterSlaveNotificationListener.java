/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.security.npam.ejb.listener;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import com.ericsson.oss.itpf.sdk.cluster.MembershipChangeEvent;
import com.ericsson.oss.itpf.sdk.cluster.annotation.ServiceCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//Non CDI cluster event listeners must implement MembershipChangeListener
@ApplicationScoped
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class MasterSlaveNotificationListener implements MembershipListenerInterface {

    private static final Logger log = LoggerFactory.getLogger(MasterSlaveNotificationListener.class);

    volatile boolean master;

    // observer method will be invoked by ServiceFramework every time there are membership changes in service cluster named NodePAMCluster
    void listenForMembershipChange(@Observes @ServiceCluster("NodePAMCluster") final MembershipChangeEvent mce) {
        if (mce != null) {
            log.info("Catch MemberShip NodePAMCluster Change isMaster ={}] ", mce.isMaster());
            setMaster(mce.isMaster());
            final int numberOfMembers = mce.getCurrentNumberOfMembers();
            log.info("MemberShip: {}", numberOfMembers);
            for (final MembershipChangeEvent.ClusterMemberInfo cmi : mce.getAllClusterMembers()) {
                log.info("NodeId: {} ServiceId: {} Version:{}", cmi.getNodeId(), cmi.getServiceId(), cmi.getVersion());
            }
        } else {
            log.info("MemberShipChangeEvent NULL!!!");
        }
    }

    @Override
    public boolean isMaster() {
        return master;
    }

    private void setMaster(final boolean master) {
        this.master = master;
    }

}
