/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.security.npam.ejb.handler;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.DEFAULT_PAM_USER_ID;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_PROPERTIES;
import static com.ericsson.oss.services.security.npam.ejb.handler.FdnUtility.extractNodeNameFromFdn;
import static com.ericsson.oss.services.security.npam.ejb.listener.DpsDataChangedEventListener.NPAM_CONFIG_TYPE;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import com.ericsson.oss.services.security.npam.ejb.instrumentation.InstrumentationBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsObjectCreatedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsObjectDeletedEvent;
import com.ericsson.oss.services.security.npam.api.constants.NodePamApplication;
import com.ericsson.oss.services.security.npam.api.message.NodePamDisableRemoteManagementEventRequest;
import com.ericsson.oss.services.security.npam.api.message.NodePamEnableRemoteManagementEventRequest;
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus;
import com.ericsson.oss.services.security.npam.ejb.message.NodePamQueueMessageSender;

@Singleton
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class DpsEventManager {

    @Inject
    NodePamQueueMessageSender nodePamUpdateRequestQueueSender;

    @Inject
    NodePamConfigStatus nodePamConfigStatus;

    @Inject
    InstrumentationBean instrumentationBean;

    private final Logger logger = LoggerFactory.getLogger(DpsEventManager.class);

    private static final String MAINTENANCE_USER_SECURITY_TYPE = "MaintenanceUserSecurity";
    private static final String REMOTE_MANAGEMENT_ATTRIBUTE = "remoteManagement";

    public void notifyEvent(final DpsDataChangedEvent event) {
        manageDpsDataChangedEvent(event);
    }

    /*******************************
     * P R I V A T E  M E T H O D S
     *******************************/

    private void manageDpsDataChangedEvent(final DpsDataChangedEvent event) {
        final String nodeName = extractNodeNameFromFdn(event.getFdn());
        logger.info("DpsEventManager::manageDpsDataChangedEvent:: received eventType={} class={} event={} for nodeName={}", event.getEventType(), event.getClass(), event, nodeName);

        switch (event.getEventType()) {
            case OBJECT_CREATED:
                objectCreateDpsEvent(event, nodeName);
                break;
            case OBJECT_DELETED:
                objectDeleteDpsEvent(event, nodeName);
                break;
            case ATTRIBUTE_CHANGED:
                objectAttributeChangedDpsEvent(event, nodeName);
                break;
            default:
                logger.error("DpsEventManager::manageDpsDataChangedEvent UNEXPECTED eventType={} for nodeName={}", event.getEventType(), nodeName);
                break;
        }
    }

    private void objectCreateDpsEvent (final DpsDataChangedEvent event, final String nodeName) {
        if (DpsObjectCreatedEvent.class.isAssignableFrom(event.getClass())) {
            DpsObjectCreatedEvent dpsObjectCreatedEvent = DpsObjectCreatedEvent.class.cast(event);
            Map<String, Object> attributeValues = dpsObjectCreatedEvent.getAttributeValues();
            logger.info("DpsEventManager::manageDpsDataChangedEvent:: eventType=OBJECT_CREATED :: print getAttributeValues Map={} for nodeName={}", attributeValues, nodeName);

            if (MAINTENANCE_USER_SECURITY_TYPE.equals(dpsObjectCreatedEvent.getType())) {
                for (final Map.Entry<String,Object> entry : attributeValues.entrySet()) {
                    String attributeName = entry.getKey();
                    if (REMOTE_MANAGEMENT_ATTRIBUTE.equalsIgnoreCase(attributeName)) {
                        Boolean remoteManagementValue = (Boolean) attributeValues.get(attributeName);
                        manageRemoteManagementAttribute(nodeName, remoteManagementValue);
                    }
                }

            }  //end type switch
        } else {
            logger.info("DpsEventManager::manageDpsDataChangedEvent:: eventType=OBJECT_CREATED is NOT assignable to DpsObjectCreatedEvent for nodeName={}", nodeName);
        }
    }

    private void objectDeleteDpsEvent(final DpsDataChangedEvent event, final String nodeName) {
        // NOTHING TO DO
        if (DpsObjectDeletedEvent.class.isAssignableFrom(event.getClass())) {
            final DpsObjectDeletedEvent dpsObjectDeletedEvent = DpsObjectDeletedEvent.class.cast(event);
            Map<String, Object> attributeValues = dpsObjectDeletedEvent.getAttributeValues();
            logger.info("DpsEventManager::manageDpsDataChangedEvent:: eventType=OBJECT_DELETED :: print getAttributeValues Map={} for nodeName={}", attributeValues, nodeName);
        } else {
            logger.info("DpsEventManager::manageDpsDataChangedEvent:: eventType=OBJECT_DELETED is NOT assignable to DpsObjectDeletedEvent for nodeName={}", nodeName);
        }
    }

    private void objectAttributeChangedDpsEvent(final DpsDataChangedEvent event, final String nodeName) {
        if (DpsAttributeChangedEvent.class.isAssignableFrom(event.getClass())) {
            final DpsAttributeChangedEvent dpsAttributeChangedEvent = DpsAttributeChangedEvent.class.cast(event);
            final Set<AttributeChangeData> changedAttributes = dpsAttributeChangedEvent.getChangedAttributes();

            switch (event.getType()) {
                case MAINTENANCE_USER_SECURITY_TYPE :
                    maintenanceUserSecurityChangeAttributes(nodeName, changedAttributes);
                    break;
                case NPAM_CONFIG_TYPE:
                    npamConfigChangeAttributes(changedAttributes);
                    break;
                default :
                    logger.error("DpsEventManager::manageDpsDataChangedEvent:: UNEXPECTED TYPE :: event={} for nodeName={}", event, nodeName);
                    break;
            }  //end type switch

        } else {
            logger.info("DpsEventManager::manageDpsDataChangedEvent:: eventType=ATTRIBUTE_CHANGED is NOT assignable to DpsAttributeChangedEvent for nodeName={}", nodeName);
        }
    }

    private void npamConfigChangeAttributes(Set<AttributeChangeData> changedAttributes) {
        for (final AttributeChangeData changedAttribute : changedAttributes) {
            if (NPAM_CONFIG_PROPERTIES.equalsIgnoreCase(changedAttribute.getName())) {
                nodePamConfigStatus.setConfig((List<Map<String, String>>) changedAttribute.getNewValue());
            }
        }
    }

    private void maintenanceUserSecurityChangeAttributes(String nodeName, Set<AttributeChangeData> changedAttributes) {
        for (final AttributeChangeData changedAttribute : changedAttributes) {
            if (REMOTE_MANAGEMENT_ATTRIBUTE.equalsIgnoreCase(changedAttribute.getName())) {
                Boolean remoteManagementValue = (Boolean) changedAttribute.getNewValue();
                manageRemoteManagementAttribute(nodeName, remoteManagementValue);
            }
        }
    }

    public String generateNbiRequestId() {
        return NodePamApplication.NODEPAM.getName().toLowerCase(Locale.ENGLISH) + ":"+ UUID.randomUUID().toString();
    }

    private void manageRemoteManagementAttribute(String nodeName, Boolean remoteManagementValue) {
        if (remoteManagementValue != null) {
            if (remoteManagementValue.booleanValue()) {
                final String requestId = generateNbiRequestId();
                final String userId = DEFAULT_PAM_USER_ID;
                final NodePamEnableRemoteManagementEventRequest nodePamEnableRemoteManagementEventRequest = new NodePamEnableRemoteManagementEventRequest(userId, requestId, nodeName);
                logger.info("DpsEventManager::manageRemoteManagementAttribute:: send nodePamEnableRemoteManagementEventRequest message={}", nodePamEnableRemoteManagementEventRequest);
                instrumentationBean.increaseEnableRemoteManagementEventsReceived();
                nodePamUpdateRequestQueueSender.sendEventExecutorMessage(requestId, nodePamEnableRemoteManagementEventRequest);
            } else {
                final String requestId = generateNbiRequestId();
                final String userId = DEFAULT_PAM_USER_ID;
                final NodePamDisableRemoteManagementEventRequest nodePamDisableRemoteManagementEventRequest = new NodePamDisableRemoteManagementEventRequest(userId, requestId, nodeName);
                logger.info("DpsEventManager::manageRemoteManagementAttribute:: send nodePamDisableRemoteManagementEventRequest message={}", nodePamDisableRemoteManagementEventRequest);
                instrumentationBean.increaseDisableRemoteManagementEventsReceived();
                nodePamUpdateRequestQueueSender.sendEventExecutorMessage(requestId, nodePamDisableRemoteManagementEventRequest);
            }
        } else {
            logger.info("DpsEventManager::manageRemoteManagementAttribute:: remoteManagement attribute value is null so nothing to do");
        }
    }
}