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

package com.ericsson.oss.services.security.npam.api.cal;

import javax.annotation.PostConstruct;
import java.io.Serializable;

public abstract class AbstractRecorderDTO implements Serializable {

    private static final long serialVersionUID = 1;

    private String ip;
    private String username;
    private String cookie;

    @PostConstruct
    public void init() {

        this.username = CALConstants.UNKNOWN_USER;
        this.ip = CALConstants.UNKNOWN_IP;
        this.cookie = CALConstants.UNKNOWN_COOKIE;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public void setCookie(String cookie) {
        this.cookie = cookie;
    }
    public String getUsername() {
        return this.username;
    }
    public String getCookie() {
        return this.cookie;
    }

    public String getIp() {
        return ip;
    }
}
