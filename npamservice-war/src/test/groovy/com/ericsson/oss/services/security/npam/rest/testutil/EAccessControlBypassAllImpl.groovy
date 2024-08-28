package com.ericsson.oss.services.security.npam.rest.testutil

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.*;


public class EAccessControlBypassAllImpl implements EAccessControl {

    private static final Logger logger = LoggerFactory.getLogger(EAccessControlBypassAllImpl.class);

    @Override
    public ESecuritySubject getAuthUserSubject() throws SecurityViolationException {
        logger.warn("************************************************************");
        logger.warn("AccessControlBypassAllImpl IS NOT FOR PRODUCTION USE.");
        logger.warn("AccessControlBypassAllImpl: getAuthUserSubject called.");
        logger.warn("************************************************************");

        // get userid from currentAuthUser file in tmpDir
        final String tmpDir = System.getProperty("java.io.tmpdir");
        final String useridFile = String.format("%s/currentAuthUser", tmpDir);
        String toruser;
        try {
            toruser = new String(Files.readAllBytes(Paths.get(useridFile)));
        } catch (IOException ioe) {
            logger.error("Error reading {}, Details: {}", useridFile, ioe.getMessage());
            toruser = "ioerror";
        }

        logger.info("AccessControlBypassAllImpl: getAuthUserSubject: toruser is <{}>", toruser);
        return new ESecuritySubject(toruser);
    }

    @Override
    public void setAuthUserSubject(String s) {

    }

    @Override
    public boolean isUserInRole(String s) {
        return true;
    }

    @Override
    public Set<ESecurityTarget> getTargetsForSubject() {
        return null;
    }

    @Override
    public Set<ESecurityTarget> getTargetsForSubject(ESecuritySubject eSecuritySubject) {
        return null;
    }

    public boolean isAuthorized(final ESecuritySubject secSubject, final ESecurityResource secResource, final ESecurityAction secAction,
                                final EPredefinedRole[] roles) throws SecurityViolationException, IllegalArgumentException {
        logger.warn("************************************************************");
        logger.warn("AccessControlBypassAllImpl IS NOT FOR PRODUCTION USE.");
        logger.warn("AccessControlBypassAllImpl: isAuthorized 1 called");
        logger.warn("************************************************************");
        return true;
    }

    public boolean isAuthorized(final ESecuritySubject secSubject, final ESecurityResource secResource, final ESecurityAction secAction)
            throws SecurityViolationException, IllegalArgumentException {
        logger.warn("************************************************************");
        logger.warn("AccessControlBypassAllImpl IS NOT FOR PRODUCTION USE.");
        logger.warn("AccessControlBypassAllImpl: isAuthorized 2 called");
        logger.warn("************************************************************");
        return true;
    }

    public boolean isAuthorized(final ESecurityResource secResource, final ESecurityAction secAction, final EPredefinedRole[] roles)
            throws SecurityViolationException, IllegalArgumentException {
        logger.warn("************************************************************");
        logger.warn("AccessControlBypassAllImpl IS NOT FOR PRODUCTION USE.");
        logger.warn("AccessControlBypassAllImpl: isAuthorized 3 called");
        logger.warn("************************************************************");
        return true;
    }

    public boolean isAuthorized(final ESecurityResource secResource, final ESecurityAction secAction) throws SecurityViolationException,
            IllegalArgumentException {
        logger.warn("************************************************************");
        logger.warn("AccessControlBypassAllImpl IS NOT FOR PRODUCTION USE.");
        logger.warn("AccessControlBypassAllImpl: isAuthorized 4 called");
        logger.warn("************************************************************");
        return true;
    }

    @Override
    public boolean isAuthorized(ESecuritySubject eSecuritySubject, ESecurityResource eSecurityResource, ESecurityAction eSecurityAction, Set<ESecurityTarget> set) throws SecurityViolationException {
        return true;
    }

    @Override
    public boolean isAuthorized(ESecurityResource eSecurityResource, ESecurityAction eSecurityAction, Set<ESecurityTarget> set) throws SecurityViolationException {
        return true;
    }

    @Override
    public boolean isAuthorized(ESecuritySubject eSecuritySubject, Set<ESecurityTarget> set) throws SecurityViolationException {
        return true;
    }

    @Override
    public boolean isAuthorized(Set<ESecurityTarget> set) throws SecurityViolationException {
        return true;
    }

    @Override
    public boolean isAuthorized(ESecuritySubject eSecuritySubject, ESecurityTarget eSecurityTarget) throws SecurityViolationException {
        return true;
    }

    @Override
    public boolean isAuthorized(ESecurityTarget eSecurityTarget) throws SecurityViolationException {
        return true;
    }

	@Override
	public boolean checkUserExists(ESecuritySubject arg0) {
		return true;
	}

	@Override
	public Map<ESecurityResource, Set<ESecurityAction>> getActionsForResources(Set<ESecurityResource> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<ESecurityResource, Set<ESecurityAction>> getActionsForResources(ESecuritySubject arg0,
			Set<ESecurityResource> arg1) {
		// TODO Auto-generated method stub
		return null;
	}
}
