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
package com.ericsson.oss.services.security.npam.ejb.utility;


import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.ESecuritySubject;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.ESecurityTarget;

public class TbacEvaluation {
    @Inject
    private EAccessControl eAccessControl;

    public boolean getNodePermission(final String userId, final String targetName) {
        final ESecuritySubject subject = new ESecuritySubject(userId);
        final ESecurityTarget target = new ESecurityTarget(targetName);

        return eAccessControl.isAuthorized(subject, target);
    }
}
