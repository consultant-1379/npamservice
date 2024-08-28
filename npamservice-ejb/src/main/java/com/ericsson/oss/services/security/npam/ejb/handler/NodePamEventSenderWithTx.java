/*
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 */
package com.ericsson.oss.services.security.npam.ejb.handler;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.services.security.model.npam.event.NodePamMediationEvent;
import com.ericsson.oss.services.security.npam.api.constants.NodePamEventOperation;
import com.ericsson.oss.services.security.npam.ejb.executor.ManagedObjectInfo;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.*;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class NodePamEventSenderWithTx {
    @Inject
    private Logger logger;

    @Inject
    @Modeled
    private EventSender<NodePamMediationEvent> nodePamEventSender;

    @SuppressWarnings({"squid:S107"})
    public void createAndSendNodePamEvent(final String requestId,
                                          final String nodeName,
                                          final String subjectName,
                                          final String nextUserName,
                                          final String nextPassword,
                                          final long neJobId,
                                          NodePamEventOperation nodePamEventOperation,
                                          final ManagedObjectInfo maintenanceUserManagedObjectInfo
                                         ) {
        final NodePamMediationEvent event = createNodePamEvent(requestId, nodeName, maintenanceUserManagedObjectInfo, nodePamEventOperation, subjectName, nextUserName, nextPassword, neJobId);
         nodePamEventSender.send(event);
        logger.info("createAndSendNodePamEvent:: sent message to mediation via MTR NodePamEvent={}" , event);
    }

    @SuppressWarnings({"squid:S107"})
    public void createAndSendNodePamEventForRemoteManagement(final String requestId,
                                                             final String nodeName,
                                                             final long neJobId,
                                                             final ManagedObjectInfo maintenanceUserSecurityManagedObjectInfo,
                                                             final boolean remoteManagement,
                                                             final Boolean restrictMaintenanceUser
                                                             ) {
        //manca remoteManagement nell' NodePamEvent
        final NodePamMediationEvent event = createNodePamEvent(requestId, nodeName, maintenanceUserSecurityManagedObjectInfo, NodePamEventOperation.MODIFY_REMOTE_MANAGEMENT, NULL_SUBJECT_NAME, NULL_USERNAME, NULL_PASSWORD, neJobId);
        event.setNodePamRemoteManagement(remoteManagement);
        event.setNodePamRestrictMaintenanceUser(restrictMaintenanceUser);
        nodePamEventSender.send(event);
        logger.info("NodePam::NodePamEventSenderWithTx::sendNodePamEvent:: sent message to mediation via MTR NodePamEvent={}" , event);
    }

    @SuppressWarnings({"squid:S107"})
    private static NodePamMediationEvent createNodePamEvent(final String requestId,
                                                            final String nodeName,
                                                            ManagedObjectInfo managedObjectInfo,
                                                            final NodePamEventOperation nodePamEventOperation,
                                                            String subjectName,
                                                            String nextUserName,
                                                            String nextPassword,
                                                            long neJobId) {
        final NodePamMediationEvent event = new NodePamMediationEvent();
        event.setNodeAddress("NetworkElement=" + nodeName);
        event.setProtocolInfo("CM");

        event.setNodePamRequestId(requestId);

        event.setNodePamFdn(managedObjectInfo.getFdn());
        event.setNodePamMoType(managedObjectInfo.getType());
        event.setNodePamNameSpace(managedObjectInfo.getNameSpace());
        event.setNodePamNameSpaceVersion(managedObjectInfo.getNameSpaceVersion());
        event.setNodePamOperation(nodePamEventOperation.name());

        event.setNodePamSubjectName(subjectName);
        event.setNodePamUsername(nextUserName);
        event.setNodePamPassword(nextPassword);

        event.setNeJobId(neJobId);
        return event;
    }
}
