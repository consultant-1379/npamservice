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

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.sdk.context.ContextService;
import com.ericsson.oss.services.security.npam.api.interfaces.JobConfigurationService;
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult;
import com.ericsson.oss.services.security.npam.api.message.NodePamAutogeneratePasswordRequest;
import com.ericsson.oss.services.security.npam.api.message.NodePamDisableRemoteManagementEventRequest;
import com.ericsson.oss.services.security.npam.api.message.NodePamDisableRemoteManagementRequest;
import com.ericsson.oss.services.security.npam.api.message.NodePamEnableRemoteManagementEventRequest;
import com.ericsson.oss.services.security.npam.api.message.NodePamEnableRemoteManagementRequest;
import com.ericsson.oss.services.security.npam.api.message.NodePamRequest;
import com.ericsson.oss.services.security.npam.api.message.NodePamUpdatePasswordRequest;
import com.ericsson.oss.services.security.npam.api.message.NodePamRecoveryConfigurationRequest;
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus;
import com.ericsson.oss.services.security.npam.ejb.exceptions.ExceptionFactory;
import com.ericsson.oss.services.security.npam.ejb.exceptions.NodePamError;
import com.ericsson.oss.services.security.npam.ejb.exceptions.NodePamValidationException;
import com.ericsson.oss.services.security.npam.ejb.executor.NodePamAutogeneratePasswordRequestExecutor;
import com.ericsson.oss.services.security.npam.ejb.executor.NodePamDisableRemoteManagementEventRequestExecutor;
import com.ericsson.oss.services.security.npam.ejb.executor.NodePamDisableRemoteManagementRequestExecutor;
import com.ericsson.oss.services.security.npam.ejb.executor.NodePamEnableRemoteManagementEventRequestExecutor;
import com.ericsson.oss.services.security.npam.ejb.executor.NodePamEnableRemoteManagementRequestExecutor;
import com.ericsson.oss.services.security.npam.ejb.executor.NodePamUpdatePasswordRequestExecutor;
import com.ericsson.oss.services.security.npam.ejb.executor.NodePamRecoveryConfigurationRequestExecutor;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class NodePamUpdateHandler {
    @Inject
    private Logger logger;

    @Inject
    private ContextService contextService;

    @Inject
    ExceptionFactory exceptionFactory;

    @Inject
    NodePamAutogeneratePasswordRequestExecutor nodePamAutogeneratePasswordRequestExecutor;

    @Inject
    NodePamUpdatePasswordRequestExecutor nodePamUpdatePasswordRequestExecutor;

    @Inject
    NodePamEnableRemoteManagementRequestExecutor nodePamEnableRemoteManagementRequestExecutor;

    @Inject
    NodePamEnableRemoteManagementEventRequestExecutor nodePamEnableRemoteManagementEventRequestExecutor;

    @Inject
    NodePamDisableRemoteManagementRequestExecutor nodePamDisableRemoteManagementRequestExecutor;

    @Inject
    NodePamDisableRemoteManagementEventRequestExecutor nodePamDisableRemoteManagementEventRequestExecutor;

    @Inject
    NodePamRecoveryConfigurationRequestExecutor nodePamRecoveryConfigurationRequestExecutor;

    @Inject
    JobConfigurationService jobConfigurationService;

    @Inject
    NodePamConfigStatus nodePamConfigStatus;

    @Inject
    SendBackToQueueManager sendBackToQueueManager;

    private static final String USER_ID_CONTEXT_VALUE_NAME = "X-Tor-UserID";
    private static final String COMMAND_REQUEST_CONTEXT_ID = "NPAM-Request-ID";

    @SuppressWarnings({ "squid:S3776" })
    public void processCommandRequest(final String requestId, final NodePamRequest nodePamRequest) {
        try {
            setUserContext(nodePamRequest);
            logger.info("processCommandRequest :: nodePamRequest={} START", nodePamRequest);

            //Check if functionality is enabled
            if (!nodePamConfigStatus.isEnabled()) {
                throw exceptionFactory.createValidationException(NodePamError.NPAM_FUNCTIONALITY_DISABLED);
            }

            if (checkIfNodePamRequestCanBeProcessed(requestId, nodePamRequest)) {
                //            NodePamUpdatePasswordRequest should be used for both update password and update with file.
                if (NodePamUpdatePasswordRequest.class.isAssignableFrom(nodePamRequest.getClass())) {
                    nodePamUpdatePasswordRequestExecutor.processCommandRequest(requestId, (NodePamUpdatePasswordRequest) nodePamRequest);
                } else if (NodePamAutogeneratePasswordRequest.class.isAssignableFrom(nodePamRequest.getClass())) {
                    nodePamAutogeneratePasswordRequestExecutor.processCommandRequest(requestId, (NodePamAutogeneratePasswordRequest) nodePamRequest);
                } else if (NodePamEnableRemoteManagementRequest.class.isAssignableFrom(nodePamRequest.getClass())) {
                    nodePamEnableRemoteManagementRequestExecutor.processCommandRequest(requestId,
                            (NodePamEnableRemoteManagementRequest) nodePamRequest);
                } else if (NodePamEnableRemoteManagementEventRequest.class.isAssignableFrom(nodePamRequest.getClass())) {
                    if (!waitNodeIsSynchronizing(requestId, nodePamRequest)) {
                        nodePamEnableRemoteManagementEventRequestExecutor.processCommandRequest(requestId,
                                (NodePamEnableRemoteManagementEventRequest) nodePamRequest);
                    }
                } else if (NodePamDisableRemoteManagementRequest.class.isAssignableFrom(nodePamRequest.getClass())) {
                    nodePamDisableRemoteManagementRequestExecutor.processCommandRequest(requestId,
                            (NodePamDisableRemoteManagementRequest) nodePamRequest);
                } else if (NodePamDisableRemoteManagementEventRequest.class.isAssignableFrom(nodePamRequest.getClass())) {
                    nodePamDisableRemoteManagementEventRequestExecutor
                            .processCommandRequest((NodePamDisableRemoteManagementEventRequest) nodePamRequest);
                } else if (NodePamRecoveryConfigurationRequest.class.isAssignableFrom(nodePamRequest.getClass())) {
                        nodePamRecoveryConfigurationRequestExecutor.processCommandRequest(requestId, (NodePamRecoveryConfigurationRequest) nodePamRequest);
                } else {
                    logger.warn("Unhandled nodePamRequest={}", nodePamRequest);
                }
            }
        } catch (final NodePamValidationException e) {
            logger.info("processCommandRequest:: NodePamValidationException e.getClass={}, e.getMessage={} for nodePamRequest={}", e.getClass(), e.getMessage(), nodePamRequest);
            updateNeJobStateToCompletedFailedLoggingException(nodePamRequest, e);
        } catch (final Exception e) {
            logger.info("processCommandRequest:: exception e.getClass={}, e.getMessage={} for nodePamRequest={}", e.getClass(), e.getMessage(), nodePamRequest);
            updateNeJobStateToCompletedFailedLoggingException(nodePamRequest, e);
        } finally {
            clearUserContext();
            logger.info("processCommandRequest :: nodePamRequest={} STOP", nodePamRequest);
        }
    }

    private void updateNeJobStateToCompletedFailedLoggingException(final NodePamRequest nodePamRequest, final Exception e) {
        if (nodePamRequest.getNeJobId() > 0) {
            try {
                jobConfigurationService.updateNeJobStateToCompleted(nodePamRequest.getNeJobId(), JobResult.FAILED.getJobResult(), e.getMessage());
            } catch (final Exception ee) {
                logger.info(
                        "updateNeJobStateToCompletedLoggingException:: impossible to updateNeJobStateToCompleted due to exception e.getClass={}, e.getMessage={} ",
                        ee.getClass(), ee.getMessage());
            }
        }
    }

    private boolean checkIfNodePamRequestCanBeProcessed(final String requestId, final NodePamRequest nodePamRequest) {
        return sendBackToQueueManager.checkIfNodePamRequestCanBeProcessed(requestId, nodePamRequest, isEventRequest(nodePamRequest));
    }

    private boolean waitNodeIsSynchronizing(final String requestId, final NodePamRequest nodePamRequest) {
        return sendBackToQueueManager.waitNodeIsSynchronizing(requestId, nodePamRequest, isEventRequest(nodePamRequest));
    }

    @SuppressWarnings("squid:CallToDeprecatedMethod")
    private void setUserContext(final NodePamRequest nodePamRequest) {
        contextService.setContextValue(USER_ID_CONTEXT_VALUE_NAME, nodePamRequest.getUserId());
        contextService.setContextValue(COMMAND_REQUEST_CONTEXT_ID, nodePamRequest.getRequestId());
    }

    private void clearUserContext() {
        contextService.setContextValue(USER_ID_CONTEXT_VALUE_NAME, null);
        contextService.setContextValue(COMMAND_REQUEST_CONTEXT_ID, null);
    }

    private boolean isEventRequest(final NodePamRequest nodePamRequest) {
        return NodePamEnableRemoteManagementEventRequest.class.isAssignableFrom(nodePamRequest.getClass()) ||
                NodePamDisableRemoteManagementEventRequest.class.isAssignableFrom(nodePamRequest.getClass());
    }
}
