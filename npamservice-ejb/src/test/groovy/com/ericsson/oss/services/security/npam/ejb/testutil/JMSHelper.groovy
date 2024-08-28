package com.ericsson.oss.services.security.npam.ejb.testutil

import com.ericsson.oss.itpf.sdk.eventbus.Event
import com.ericsson.oss.itpf.sdk.eventbus.EventConfiguration
import com.ericsson.oss.itpf.sdk.eventbus.EventConfigurationBuilder

/**
 * Helper methods for creating message objects for JMS queues in tests.
 *
 * @author ejonlal.
 */
class JMSHelper {

    /**
     * Creates a JMS {@link javax.jms.ObjectMessage}
     *
     * @param requestId the JMSCorrelationID.
     * @param object the object to be returned when getObject() is called on the ObjectMessage.
     * @param stringProperties the properties to be associated with this message.
     * @return {@link javax.jms.ObjectMessage}.
     */
    static Event createEventForObject(final String requestId, final Object object) {
        [getCorrelationId: { requestId }, getPayload: { object }] as Event
    }

    /**
     * Build an {@link com.ericsson.oss.itpf.sdk.eventbus.EventConfiguration} from properties.
     *
     * @param eventProperties map of properties to apply to the event.
     * @return {@link com.ericsson.oss.itpf.sdk.eventbus.EventConfiguration}.
     */
    static EventConfiguration getEventConfiguration(final Map<String, Object> eventProperties) {
        final EventConfigurationBuilder eventConfigurationBuilder = new EventConfigurationBuilder()
        eventProperties.each { key, value ->
            eventConfigurationBuilder.addEventProperty(key, value)
        }
        eventConfigurationBuilder.build()
    }
}
