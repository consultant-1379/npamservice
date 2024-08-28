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
package com.ericsson.oss.services.security.npam.ejb.listener;

import com.ericsson.oss.itpf.sdk.eventbus.annotation.Consumes;
import com.ericsson.oss.services.security.npam.api.message.NodePamEndUpdateOperation;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import static com.ericsson.oss.services.security.npam.api.message.NodePamMessageProperties.NODE_PAM_TOPIC_ENDPOINT;
import static com.ericsson.oss.services.security.npam.api.message.NodePamMessageProperties.NODE_PAM_TOPIC_FILTER;

@ApplicationScoped
public class NodePamEndUpdateOperationListener {

    @Inject
    private Logger logger;

    @Inject
    NodePamEndUpdateOperationMapManager nodePamEndUpdateOperationMapManager;

    public void receiveRequest(@Observes @Consumes(endpoint = NODE_PAM_TOPIC_ENDPOINT, filter = NODE_PAM_TOPIC_FILTER)  final NodePamEndUpdateOperation nodePamEndUpdateOperation) {
        logger.debug("receiveRequest for nodePamEndUpdateOperation={}", nodePamEndUpdateOperation);
        nodePamEndUpdateOperationMapManager.setStatus(nodePamEndUpdateOperation);
    }
}

