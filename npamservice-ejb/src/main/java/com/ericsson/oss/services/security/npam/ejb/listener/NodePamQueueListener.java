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
package com.ericsson.oss.services.security.npam.ejb.listener;

import com.ericsson.oss.itpf.sdk.eventbus.Event;
import com.ericsson.oss.itpf.sdk.eventbus.annotation.Consumes;
import com.ericsson.oss.services.security.npam.api.interfaces.JobExecutionService;
import com.ericsson.oss.services.security.npam.api.message.NodePamDisableFeatureRequest;
import com.ericsson.oss.services.security.npam.api.message.NodePamRequest;
import com.ericsson.oss.services.security.npam.api.message.NodePamSubmitMainJobRequest;
import com.ericsson.oss.services.security.npam.ejb.executor.NodePamDisableFeatureRequestExecutor;
import com.ericsson.oss.services.security.npam.ejb.handler.NodePamUpdateHandler;
import org.slf4j.Logger;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import static com.ericsson.oss.services.security.npam.api.message.NodePamMessageProperties.*;

@ApplicationScoped
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class NodePamQueueListener {

    @Inject
    NodePamUpdateHandler nodePamUpdateHandler;
    
    @Inject
    private Logger logger;

    @Inject
    JobExecutionService jobExecutionService;

    @Inject
    NodePamDisableFeatureRequestExecutor nodePamDisableFeatureRequestExecutor;

    public void receiveJobRequest(@Observes @Consumes(endpoint = NODE_PAM_QUEUE_ENDPOINT, filter = NODE_PAM_QUEUE_JOB_EXECUTOR_FILTER) final Event event) {
        updateRequestProcess(event);
    }

    public void receiveEventRequest(@Observes @Consumes(endpoint = NODE_PAM_QUEUE_ENDPOINT, filter = NODE_PAM_QUEUE_EVENT_EXECUTOR_FILTER) final Event event) {
        updateRequestProcess(event);
    }

    private void updateRequestProcess(final Event event) {
        final String requestId = event.getCorrelationId();
        final NodePamRequest nodePamRequest = (NodePamRequest) event.getPayload();
        logger.debug("updateRequestProcess :: received nodePamRequest={}", nodePamRequest);
        nodePamUpdateHandler.processCommandRequest(requestId, nodePamRequest);
    }

    public void receiveSubmitMainJobRequest(@Observes @Consumes(endpoint = NODE_PAM_QUEUE_ENDPOINT, filter = NODE_PAM_QUEUE_SUBMIT_MAIN_JOB_FILTER) final Event event) {
        submitMainJobRequestProcess(event);
    }

    private void submitMainJobRequestProcess(final Event event) {
        if (NodePamDisableFeatureRequest.class.isAssignableFrom(event.getPayload().getClass())) {
            final NodePamDisableFeatureRequest nodePamDisableFeatureRequest = (NodePamDisableFeatureRequest) event.getPayload();
            logger.info("submitMainJobRequestProcess:: NodePamDisableFeatureRequest :: requestProcess :: received message={}", nodePamDisableFeatureRequest);
            nodePamDisableFeatureRequestExecutor.retrieveAndDetachAllNEAccount();
        } else {
            final NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = (NodePamSubmitMainJobRequest) event.getPayload();
            logger.info("NodePam::NodePamSubmitMainJobRequest:: requestProcess :: received message={}", nodePamSubmitMainJobRequest);
            jobExecutionService.runMainJob(nodePamSubmitMainJobRequest.getJobId());
        }

    }
}
