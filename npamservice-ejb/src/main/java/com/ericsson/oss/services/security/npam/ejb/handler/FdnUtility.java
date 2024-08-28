/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2015
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.security.npam.ejb.handler;

import com.ericsson.oss.services.security.npam.ejb.executor.ManagedObjectInfo;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.SYNCHRONIZED;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.UNSYNCHRONIZED;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_SUBJECT_NAME;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NULL_SUBJECT_NAME;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.EMPTY_SUBJECT_NAME;

public class FdnUtility {

    private static final String RDN_SEPARATOR = ",";
    private static final String OBJECT_WITHOUT_FDN = null;

    private FdnUtility() {
        throw new IllegalStateException("Fdn Utility class");
    }

    public static boolean isValidFdn(final String associatedNodeRootFdn) {
        return associatedNodeRootFdn != null && associatedNodeRootFdn.contains("=");
    }

    public static String extractFirstRdnIncludingSubnetworks(final String fdn) {
        if (fdn == null) {
            return OBJECT_WITHOUT_FDN; // PO's will be grouped together under this key (null)
        }
        final StringBuilder builder = new StringBuilder();
        final String[] rdns = fdn.split(RDN_SEPARATOR);

        boolean foundNonSubnetworkMo = false;
        for (final String rdn : rdns) {
            if (!rdn.startsWith("SubNetwork")) {
                foundNonSubnetworkMo = true;
            }
            builder.append(rdn);
            builder.append(RDN_SEPARATOR);

            if (foundNonSubnetworkMo) {
                break;
            }
        }
        if (!foundNonSubnetworkMo) {
            return OBJECT_WITHOUT_FDN;
        }

        return builder.toString().substring(0, builder.length() - 1);
    }

    public static String extractTypeFromFdn(final String fdn) {
        final int lastIndexOfRdnSeparator = fdn.lastIndexOf(RDN_SEPARATOR);
        final int lastIndexOfNameSeparator = fdn.lastIndexOf('=');
        return fdn.substring(lastIndexOfRdnSeparator + 1, lastIndexOfNameSeparator);
    }

    public static String extractNameFromFdn(final String fdn) {
        if (fdn == null) {
            return null;
        }
        final int lastIndexOfNameSeparator = fdn.lastIndexOf('=');
        return fdn.substring(lastIndexOfNameSeparator + 1);
    }

    public static String extractNodeNameFromFdn(final String fdn) {
        final String rdn = extractFirstRdnIncludingSubnetworks(fdn);
        if (rdn == null) {
            return null;
        }
        final String[] split = rdn.split("=");
        return split[split.length - 1];
    }

    public static String getNeAccountFdnFromNodeName(final String nodename, final int neAccountId) {
        final StringBuilder neAccountFdn = new StringBuilder();
        neAccountFdn.append("NetworkElement=");
        neAccountFdn.append(nodename);
        neAccountFdn.append(",SecurityFunction=1,NetworkElementAccount=");
        neAccountFdn.append(neAccountId);
        return neAccountFdn.toString();
    }

    public static boolean isNodeSyncronized(final String syncStatus) {
        return SYNCHRONIZED.equals(syncStatus);
    }

    public static boolean isNodeUnsyncronized(final String syncStatus) {
        return UNSYNCHRONIZED.equals(syncStatus);
    }

    public static boolean isNodeSynchronizing(final String syncStatus) {
        return !SYNCHRONIZED.equals(syncStatus);
    }

    public static String getSubjectNameToBeSet(final ManagedObjectInfo maintenanceUserManagedObjectInfo) {
        final String subjectNameValue = (String) maintenanceUserManagedObjectInfo.getAttributes().get(MAINTENANCE_USER_SUBJECT_NAME);
        return subjectNameValue == null ? NULL_SUBJECT_NAME : EMPTY_SUBJECT_NAME;
    }

    /*
     * P R I V A T E - M E T H O D S
     */

}
