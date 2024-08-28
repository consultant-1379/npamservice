package com.ericsson.oss.services.security.npam.ejb.testutil

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.security.npam.ejb.job.util.DateFormatterUtil
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import spock.lang.Unroll

class DateFormatterUtilSpec extends CdiSpecification {

    @ObjectUnderTest
    private DateFormatterUtil objUnderTest

    @Unroll
    def 'test parseUIDateString'() {
        when: 'parseUIDateString'
            def parsedDate= objUnderTest.parseUIDateString(dateString)
        then:
            parsedDate != null
            System.out.println(dateString + " -> " + parsedDate)

        where: ''
            dateString         |  _
          "2022-10-14 00:00:00 GMT+0200" | _
          "2022-10-22 20:00:00 GMT+0200" | _
          "2022-10-22 20:00:00 GMT+0400" | _
          "2022-10-22 10:00:00 GMT+0600" | _
    }

    @Unroll
    def 'test parseUIDateString throw exception'() {
        when: 'parseUIDateString'
            def parsedDate= objUnderTest.parseUIDateString(dateString)
        then:
            thrown(Exception)

        where: ''
        dateString         |  _
        "invalid"          | _
    }

    @Unroll
    def 'test parseUIDateStringWithJoda'() {
        when: 'parseUIDateStringWithJoda'
            def parsedDate= objUnderTest.parseUIDateStringWithJoda(dateString)
        then:
            parsedDate != null
            System.out.println(dateString + " -> " + parsedDate)

        where: ''
        dateString         |  _
        "2022-10-14 00:00:00 GMT+0200" | _
        "2022-10-22 20:00:00 GMT+0400" | _
        "2022-10-22 10:00:00 GMT+0600" | _
        "2022-11-20 10:10:00 GMT+0200" | _
    }

    @Unroll
    def 'test parseUIDateStringWithJoda throw exception'() {
        when: 'parseUIDateStringWithJoda'
            def parsedDate= objUnderTest.parseUIDateStringWithJoda(dateString)
        then:
            thrown(Exception)

        where: ''
        dateString         |  _
        "invalid"          | _
    }

    @Unroll
    def 'test parseUIDateStringWithJoda and use new timezone'() {
        when: 'the event is consumed'
            def parsedDate= objUnderTest.parseUIDateStringWithJoda(dateString)
            final DateTime dateConverted = new DateTime(parsedDate);  //poi si usa endTime.toDate per avere la data
            def prague = dateConverted.withZone(DateTimeZone.forID("Europe/Istanbul"))
        then:
            parsedDate != null
            System.out.println(dateString + " -> " + parsedDate  + " -> " + prague)

        where: ''
        dateString         |  _
        "2022-10-14 00:00:00 GMT+0200" | _
        "2022-10-22 20:00:00 GMT+0400" | _
        "2022-10-22 10:00:00 GMT+0600" | _
    }

    //Standard ISO 8601
    // This https://jenkov.com/tutorials/java-internationalization/simpledateformat.html explains valid SimpleDateFormat pattern (as said above GMT+0600 is invalid , +0600 is valid)
    // About ISO 8601 there is a chapter that explain that with 2 customized patter we can manage some use cases for example of possible time zone strings:
    // This is the ISO 8601 pseudo-pattern yyyy-MM-ddTHH:mm:ss:sssZ where we have T inside and time zone Z could be:
       //Z  itseld   (i.e. UTC)
       //±hh:mm
       //±hhmm
       //±hh
    // Joda library seems to manage ISO 8601 natively with DateTime.parse(dateString) cause it uses internally ISODateTimeFormat
    def 'test parseIso8610StringWithJoda parsing'() {
        when: 'dateString is parsed'
            def parsedDate= objUnderTest.parseIso8610StringWithJoda(dateString)
        then:
            parsedDate != null
            System.out.println(dateString + " -> " + parsedDate)

        where: ''
        dateString         |  _
        // use millisec
        "2022-11-20T16:50:21+04:00" | _
        "2022-11-20T16:50:21.2+04:00" | _
        "2022-11-20T16:50:21.25+04:00" | _
        "2022-11-20T16:50:21.253+04:00" | _

        // use seconds
        "2022-11-20T16:50:21+04:00:11" | _

        // use Z
        "2022-11-20T16:50:21Z" | _
        "2022-11-20T16:50:21.2Z" | _
        "2022-11-20T16:50:21.25Z" | _
        "2022-11-20T16:50:21.253Z" | _

        // use Z
        "2022-11-20T16:50:21Z" | _
        "2022-11-20T16:50:21+00:00" | _

        //another timezone format
        "2022-11-20T16:50:21+0100" | _
        "2022-11-20T16:50:21+0200" | _
        "2022-11-20T16:50:21.253+0400" | _
    }


    @Unroll
    def 'test parseIso8610StringWithJoda throw exception'() {
        when: 'parseIso8610StringWithJoda'
            def parsedDate= objUnderTest.parseIso8610StringWithJoda(dateString)
        then:
            thrown(Exception)

        where: ''
            dateString         |  _
            "invalid"          | _
    }

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
        objUnderTest.toString()
        System.out.println(new Date())
        then:
        true
    }
}

