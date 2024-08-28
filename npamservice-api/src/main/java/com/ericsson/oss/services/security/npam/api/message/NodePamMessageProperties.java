/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.security.npam.api.message;

public class NodePamMessageProperties {

    //topic constants
    public static final String NODE_PAM_TOPIC_ENDPOINT = "jms:/topic/NodePamTopic";
    public static final int NODE_PAM_TOPIC_TTL = 900000;
    public static final String JMS_NOTIFICATION_TYPE_PROPERTY = "notificationType";

    // topic filters
    public static final String JMS_NOTIFICATION_COMMAND_STATUS_PROPERTY = "operation-status";
    public static final String NODE_PAM_TOPIC_FILTER = JMS_NOTIFICATION_TYPE_PROPERTY + "='" + JMS_NOTIFICATION_COMMAND_STATUS_PROPERTY + "'";


    // queue constants
    public static final String NODE_PAM_QUEUE_ENDPOINT = "jms:/queue/NodePamRequestQueue";
    public static final int NODE_PAM_QUEUE_TTL = 900000;
    public static final String NODE_PAM_QUEUE_SELECTOR_KEY = "operation";

    // queue filters
    public static final String NODE_PAM_QUEUE_SELECTOR_JOB_EXECUTOR = "jobExecutor";
    public static final String NODE_PAM_QUEUE_JOB_EXECUTOR_FILTER = NODE_PAM_QUEUE_SELECTOR_KEY + "='" + NODE_PAM_QUEUE_SELECTOR_JOB_EXECUTOR + "'";

    public static final String NODE_PAM_QUEUE_SELECTOR_EVENT_EXECUTOR = "eventExecutor";
    public static final String NODE_PAM_QUEUE_EVENT_EXECUTOR_FILTER = NODE_PAM_QUEUE_SELECTOR_KEY + "='" + NODE_PAM_QUEUE_SELECTOR_EVENT_EXECUTOR + "'";

    public static final String NODE_PAM_QUEUE_SELECTOR_SUBMIT_MAIN_JOB = "submitMainJob";
    public static final String NODE_PAM_QUEUE_SUBMIT_MAIN_JOB_FILTER = NODE_PAM_QUEUE_SELECTOR_KEY + "='" + NODE_PAM_QUEUE_SELECTOR_SUBMIT_MAIN_JOB + "'";

    private NodePamMessageProperties() {}
}
