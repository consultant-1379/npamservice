package com.ericsson.oss.services.security.npam.ejb.executor;

import java.util.HashMap;
import java.util.Map;

public class ManagedObjectInfo {
    private String fdn = null;
    private String type = null;
    private String nameSpace = null;
    private String nameSpaceVersion = null;
    private Map<String, Object> attributes = new HashMap<>();

    public ManagedObjectInfo(String fdn, String type, String nameSpace, String nameSpaceVersion) {
        this.fdn = fdn;
        this.type = type;
        this.nameSpace = nameSpace;
        this.nameSpaceVersion = nameSpaceVersion;
    }

    public String getFdn() {
        return fdn;
    }

    public String getType() {
        return type;
    }

    public String getNameSpace() {
        return nameSpace;
    }

    public String getNameSpaceVersion() {
        return nameSpaceVersion;
    }

    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        Map<String, Object> newAttributes = new HashMap<>();
        newAttributes.putAll(attributes);
        this.attributes = newAttributes;
    }

    @Override
    public String toString() {
        return "ManagedObjectInfo{" +
                "fdn='" + fdn + '\'' +
                ", type='" + type + '\'' +
                ", nameSpace='" + nameSpace + '\'' +
                ", nameSpaceVersion='" + nameSpaceVersion + '\'' +
                '}';
    }
}
