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
package com.ericsson.oss.services.security.npam.ejb.message;

import com.ericsson.oss.itpf.sdk.eventbus.Channel;
import com.ericsson.oss.itpf.sdk.eventbus.Event;
import com.ericsson.oss.itpf.sdk.eventbus.EventConfiguration;
import com.ericsson.oss.itpf.sdk.eventbus.EventConfigurationBuilder;
import com.ericsson.oss.itpf.sdk.eventbus.annotation.Endpoint;
import com.ericsson.oss.services.security.npam.api.message.NodePamDisableFeatureRequest;
import com.ericsson.oss.services.security.npam.api.message.NodePamRequest;
import com.ericsson.oss.services.security.npam.api.message.NodePamSubmitMainJobRequest;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import static com.ericsson.oss.services.security.npam.api.message.NodePamMessageProperties.*;

/**
 * Created by enmadmin on 9/7/22.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class NodePamQueueMessageSender {

        @Inject
        @Endpoint(value = NODE_PAM_QUEUE_ENDPOINT, timeToLive = NODE_PAM_QUEUE_TTL)
        private Channel channel;

        public void sendJobExecutorMessage(final String requestId, final NodePamRequest nodePamRequest) {
            final EventConfigurationBuilder eventConfigurationBuilder = new EventConfigurationBuilder();
            eventConfigurationBuilder.addEventProperty(NODE_PAM_QUEUE_SELECTOR_KEY, NODE_PAM_QUEUE_SELECTOR_JOB_EXECUTOR);
            EventConfiguration eventConfiguration = eventConfigurationBuilder.build();
            final Event event = channel.createEvent(nodePamRequest, requestId);
            channel.send(event, eventConfiguration);
        }

    public void sendEventExecutorMessage(final String requestId, final NodePamRequest nodePamRequest) {
        final EventConfigurationBuilder eventConfigurationBuilder = new EventConfigurationBuilder();
        eventConfigurationBuilder.addEventProperty(NODE_PAM_QUEUE_SELECTOR_KEY, NODE_PAM_QUEUE_SELECTOR_EVENT_EXECUTOR);
        EventConfiguration eventConfiguration = eventConfigurationBuilder.build();
        final Event event = channel.createEvent(nodePamRequest, requestId);
        channel.send(event, eventConfiguration);
    }

    public void sendSubmitMainJobMessage(final NodePamSubmitMainJobRequest nodePamSubmitMainJob) {
        final EventConfigurationBuilder eventConfigurationBuilder = new EventConfigurationBuilder();
        eventConfigurationBuilder.addEventProperty(NODE_PAM_QUEUE_SELECTOR_KEY, NODE_PAM_QUEUE_SELECTOR_SUBMIT_MAIN_JOB);
        EventConfiguration eventConfiguration = eventConfigurationBuilder.build();
        final Event event = channel.createEvent(nodePamSubmitMainJob, "dummyString");
        channel.send(event, eventConfiguration);
    }

    public void sendDisableFeatureMessage(final NodePamDisableFeatureRequest nodePamDisableFeature) {
        final EventConfigurationBuilder eventConfigurationBuilder = new EventConfigurationBuilder();
        eventConfigurationBuilder.addEventProperty(NODE_PAM_QUEUE_SELECTOR_KEY, NODE_PAM_QUEUE_SELECTOR_SUBMIT_MAIN_JOB);
        EventConfiguration eventConfiguration = eventConfigurationBuilder.build();
        final Event event = channel.createEvent(nodePamDisableFeature, "dummyString");
        channel.send(event, eventConfiguration);
    }
}
