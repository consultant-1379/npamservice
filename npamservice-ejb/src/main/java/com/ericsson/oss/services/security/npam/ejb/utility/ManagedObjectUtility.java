package com.ericsson.oss.services.security.npam.ejb.utility;

import com.ericsson.oss.services.security.npam.ejb.executor.ManagedObjectInfo;

import java.util.ArrayList;
import java.util.List;

public class ManagedObjectUtility {

    private ManagedObjectUtility() {}

    public static ManagedObjectInfo filterForMoId(final List<ManagedObjectInfo> managedObjectInfos, final String moId) {
        ManagedObjectInfo foundManagedObjectInfo = null;

        if (managedObjectInfos == null || managedObjectInfos.isEmpty()) {
            return foundManagedObjectInfo;
        }

        for (final ManagedObjectInfo managedObjectInfo:managedObjectInfos) {
            if (managedObjectInfo.getFdn().endsWith("=" + moId)) {
                foundManagedObjectInfo = managedObjectInfo;
                break;
            }
        }
        return foundManagedObjectInfo;
    }

    public static List<ManagedObjectInfo> removeMoIdFromList(final List<ManagedObjectInfo> maintenanceUserManagedObjectInfos, final String moId) {
        if (maintenanceUserManagedObjectInfos == null || maintenanceUserManagedObjectInfos.isEmpty()) {
            return maintenanceUserManagedObjectInfos;
        }

        List<ManagedObjectInfo>  newList = new ArrayList<>();
        for (final ManagedObjectInfo managedObjectInfo:maintenanceUserManagedObjectInfos) {
            if (!managedObjectInfo.getFdn().endsWith("=" + moId)) {
                newList.add(managedObjectInfo);
            }
        }
        return newList;
    }

}
