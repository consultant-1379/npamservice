/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.security.npam.ejb.executor;

import java.util.ArrayList;
import java.util.List;

public class NodeInfo {
    private ManagedObjectInfo userIdentityManagedObjectInfo = null;
    private ManagedObjectInfo singleMaintenanceUserManagedObjectInfo = null;
    private List<ManagedObjectInfo> maintenanceUserManagedObjectInfos = new ArrayList<>();


    public ManagedObjectInfo getUserIdentityManagedObjectInfo() {
        return userIdentityManagedObjectInfo;
    }

    public void setUserIdentityManagedObjectInfo(ManagedObjectInfo userIdentityManagedObjectInfo) {
        this.userIdentityManagedObjectInfo = userIdentityManagedObjectInfo;
    }

    public ManagedObjectInfo getSingleMaintenanceUserManagedObjectInfo() {
        return singleMaintenanceUserManagedObjectInfo;
    }

    public void setSingleMaintenanceUserManagedObjectInfo(ManagedObjectInfo singleMaintenanceUserManagedObjectInfo) {
        this.singleMaintenanceUserManagedObjectInfo = singleMaintenanceUserManagedObjectInfo;
    }

    public List<ManagedObjectInfo> getMaintenanceUserManagedObjectInfos() {
        return maintenanceUserManagedObjectInfos;
    }

    public void setMaintenanceUserManagedObjectInfos(List<ManagedObjectInfo> maintenanceUserManagedObjectInfos) {
        this.maintenanceUserManagedObjectInfos = maintenanceUserManagedObjectInfos;
    }

    @Override
    public String toString() {
        return "NodeInfo{" +
                "userIdentityManagedObjectInfo=" + userIdentityManagedObjectInfo +
                ", singleMaintenanceUserManagedObjectInfo=" + singleMaintenanceUserManagedObjectInfo +
                ", maintenanceUserManagedObjectInfos=" + maintenanceUserManagedObjectInfos +
                '}';
    }
}
