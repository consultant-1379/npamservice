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

package com.ericsson.oss.services.security.npam.ejb.instrumentation;

import com.ericsson.oss.itpf.sdk.instrument.annotation.InstrumentedBean;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Category;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.CollectionType;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Interval;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Units;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Visibility;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.atomic.AtomicInteger;

@InstrumentedBean(description = "Remote Management change", displayName = "RemoteManagement AVC")
@ApplicationScoped
public class InstrumentationBean {
    private AtomicInteger enableRemoteManagementEventsReceived = new AtomicInteger();
    private AtomicInteger disableRemoteManagementEventsReceived = new AtomicInteger();

    /*
     * GET NUM of EnableRemoteManagementEvents
     */
    @MonitoredAttribute(visibility = Visibility.ALL, units = Units.NONE, category = Category.PERFORMANCE, interval = Interval.FIFTEEN_MIN, collectionType = CollectionType.TRENDSUP)
    public int getEnableRemoteManagementEventsReceived() {
        return this.enableRemoteManagementEventsReceived.get();
    }

    @MonitoredAttribute(visibility = Visibility.ALL, units = Units.NONE, category = Category.PERFORMANCE, interval = Interval.FIFTEEN_MIN, collectionType = CollectionType.TRENDSUP)
    public int getDisableRemoteManagementEventsReceived() {
        return this.disableRemoteManagementEventsReceived.get();
    }

    public void increaseEnableRemoteManagementEventsReceived() {
        this.enableRemoteManagementEventsReceived.incrementAndGet();
    }

    public void increaseDisableRemoteManagementEventsReceived() {
        this.disableRemoteManagementEventsReceived.incrementAndGet();
    }
}