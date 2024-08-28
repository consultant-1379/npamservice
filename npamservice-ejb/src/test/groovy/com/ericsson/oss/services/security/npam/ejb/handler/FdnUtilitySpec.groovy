package com.ericsson.oss.services.security.npam.ejb.handler

import com.ericsson.cds.cdi.support.spock.CdiSpecification
import spock.lang.Unroll

class FdnUtilitySpec extends CdiSpecification {

    @Unroll
    def 'Verify extractTypeFromFdn method'() {
        when: 'extractTypeFromFdn is invoked'
        def type = FdnUtility.extractTypeFromFdn(fdn)
        then:
        type == expectedType

        where: 'The given parameters'
        fdn                                        | expectedType        | _
        'MeContext=ERBS002,ManagedElement=1'   | 'ManagedElement'  | _
        'MeContext=ERBS002'                      | 'MeContext'        | _
    }

    @Unroll
    def 'Verify extractTypeFromFdn method throws exception'() {
        when: 'extractTypeFromFdn is invoked'
        def type = FdnUtility.extractTypeFromFdn(fdn)

        then: 'exception is thrown'
        def exception = thrown(Exception)

        where: 'The given parameters'
        fdn   |_
        null | _
        ''   |_
    }

    @Unroll
    def 'Verify extractNameFromFdn method'() {
        when: 'extractNameFromFdn is invoked'
        def name = FdnUtility.extractNameFromFdn(fdn)
        then:
        name == expectedName

        where: 'The given parameters'
        fdn                                        | expectedName       | _
        null                                      | null               | _
        ''                                         | ''                | _
        'MeContext=ERBS002,ManagedElement=1'   | '1'               | _
        'MeContext=ERBS002'                      | 'ERBS002'        | _
    }

    def 'asking constructor throws Exception'() {
        when:
        FdnUtility fdnUtility = new FdnUtility()
        then: 'exception is thrown'
        def exception = thrown(Exception)
    }

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
        "".toString()
        then:
        1 == 1
    }

}