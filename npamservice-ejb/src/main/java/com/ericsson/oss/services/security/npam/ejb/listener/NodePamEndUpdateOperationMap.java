package com.ericsson.oss.services.security.npam.ejb.listener;

import com.ericsson.oss.services.security.npam.api.message.NodePamEndUpdateOperation;
import org.slf4j.Logger;

import javax.ejb.*;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class NodePamEndUpdateOperationMap {

    private Map<String, NodePamEndUpdateOperation> statusMap = new HashMap<>();

    @Inject
    private Logger logger;

    @Lock(LockType.READ)
    public NodePamEndUpdateOperation getStatus(final String key) {
        return statusMap.get(key);
    }

    @Lock(LockType.WRITE)
    public void setStatus(NodePamEndUpdateOperation nodePamEndUpdateOperation) {
        statusMap.put(nodePamEndUpdateOperation.getKey(), nodePamEndUpdateOperation);
    }
}