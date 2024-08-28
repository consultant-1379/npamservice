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
package com.ericsson.oss.services.security.npam.ejb.executor;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEAccount;
import com.ericsson.oss.services.security.npam.ejb.handler.NodePamUpdateHandlerWithTx;
import com.ericsson.oss.services.security.npam.ejb.neaccount.executor.NeAccountGetServiceImpl;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.List;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MUID_TWO;

public class NodePamDisableFeatureRequestExecutor {
    @Inject
    NeAccountGetServiceImpl neAccountGetServiceImpl;

    @Inject
    NodePamUpdateHandlerWithTx nodePamUpdateHandlerWithTx;

    @Inject
    private Logger logger;
    private static final List<String> ALL_NE_ACCOUNT_IDS = null;

    public void retrieveAndDetachAllNEAccount() {
        logger.info("retrieveAndDetachAllNEAccount:: START");
        try {
            List<NPamNEAccount> neAccountList = neAccountGetServiceImpl.getAllNEAccountsById(ALL_NE_ACCOUNT_IDS);
            for (final NPamNEAccount neAccount : neAccountList) {
                String muId = neAccount.getNetworkElementAccountId();

                if (MUID_TWO.equals(muId)) {
                    nodePamUpdateHandlerWithTx.deleteNetworkElementAccount(neAccount.getNeName(), muId);
                } else {
                    nodePamUpdateHandlerWithTx.setNetworkElementAccountToDetachedState(neAccount.getNeName(), muId);
                }
            }
            logger.info("retrieveAndDetachAllNEAccount:: END  neAccountLis.size={} ", neAccountList.size());
        } catch (final Exception e) {
            logger.info("retrieveAndDetachAllNEAccount:: e.getMessage={} occured deleting neAccountS  ", e.getMessage());
        }
    }

}
