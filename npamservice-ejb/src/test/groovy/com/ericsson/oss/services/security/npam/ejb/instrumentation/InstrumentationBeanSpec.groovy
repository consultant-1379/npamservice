package com.ericsson.oss.services.security.npam.ejb.instrumentation

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification

class InstrumentationBeanSpec extends CdiSpecification {

    @ObjectUnderTest
    InstrumentationBean instrumentationBean;

    def 'Get of EnableRemoteManagementEventReceived have a consistent value'() {
        given: 'receive enable remote Management at least N times'
        int times = 5;
        for (int i=0; i<times; i++) {
            instrumentationBean.increaseEnableRemoteManagementEventsReceived();
        }
        when: 'ddc/ddp get counter of enable RemoteManagement events Received'
        int counter = instrumentationBean.getEnableRemoteManagementEventsReceived();

        then: 'Counter is greater than N '
        counter >= times;
    }

    def 'Get of DisableRemoteManagementEventsReceived have a consistent value'() {
        given: 'receive disable remote Management at least N times'
        int times = 2;
        for (int i=0; i<times; i++) {
            instrumentationBean.increaseDisableRemoteManagementEventsReceived();
        }
        when: 'ddc/ddp get counter of enable RemoteManagement events Received'
        int counter = instrumentationBean.getDisableRemoteManagementEventsReceived();

        then: 'Counter is greater than N '
        counter >= times;
    }
}
