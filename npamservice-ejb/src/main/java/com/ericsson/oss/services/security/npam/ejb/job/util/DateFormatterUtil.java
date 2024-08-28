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
package com.ericsson.oss.services.security.npam.ejb.job.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * This class is used for setting process variables of schedule jobs.
 * 
 * @author xnitpar
 * 
 */
@SuppressWarnings({"squid:S1118"})
public class DateFormatterUtil {

    // SimpleDateFormat patterns
    private static final String SIMPLE_DATE_FORMAT_PATTERN_WITHOUT_TIMEZONE = "yyyy-MM-dd HH:mm:ss";
    public static final String SIMPLE_DATE_FORMAT_PATTERN_WITH_TIMEZONE = "yyyy-MM-dd HH:mm:ssZ";

    // questo con Joda
    private static final String JODA_DATE_FORMAT_PATTERN_WITH_TIMEZONE = "yyyy-MM-dd HH:mm:ss Z";
    public static final String GMT = "GMT";

    private static final Logger logger = LoggerFactory.getLogger(DateFormatterUtil.class);

    //This method parse UI date string like 2022-10-14 03:00:00 GMT+0200 removing time zone info (so date is local)
    @SuppressWarnings({"squid:S2139","squid:S112"})
    public static Date parseUIDateString(final String dateString) throws Exception {
        Date parsedDate = null;
        try {
            final String formattedStartTime = getFormattedTime(dateString);
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN_WITHOUT_TIMEZONE);
            parsedDate = simpleDateFormat.parse(formattedStartTime);
        } catch (Exception e) {
            logger.error("parseUIDateString: Unable to parse data due to:{}", e.getMessage());
            throw e;
        }
        return parsedDate;
    }

   // remove time zone
    private static String getFormattedTime(final String dateString) {
        final String delims = " ";
        final StringTokenizer st = new StringTokenizer(dateString, delims);
        final String formattedScheduleTime = st.nextToken() + " " + st.nextToken();
        logger.debug("formattedScheduleTime {}", formattedScheduleTime);
        return formattedScheduleTime;
    }

    //This method parse UI date string like 2022-10-14 03:00:00 GMT+0200 using time zone info and converting in proper one
    @SuppressWarnings({"squid:S2139","squid:S112"})
    public static Date parseUIDateStringWithJoda(final String dateString) throws Exception {
        Date parsedDate = null;
        try {
            final String scheduledDate = prepareScheduledDate(dateString);
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN_WITH_TIMEZONE);
            parsedDate = simpleDateFormat.parse(scheduledDate);
        } catch (Exception e) {
            logger.info("parseUIDateStringWithJoda: Unable to parse data due to exception:{}", e.getMessage());
            throw e;
        }
        return parsedDate;
    }

    private static String prepareScheduledDate(String dateString) {
        final String timeZoneId = getTimeZoneIdFromDate(dateString);
        final String delims = " ";
        final StringTokenizer st = new StringTokenizer(dateString, delims);
        final String formattedScheduleTime = st.nextToken() + "T" + st.nextToken();
        return convertTimeZones(timeZoneId, TimeZone.getDefault().getID(), formattedScheduleTime);
    }

    /**
     * Temporary fix to avoid Exceptions w.r.t timezonechanges.Need to be removed later after the improvement w.r.t date format change in UI and server side
     *
     * @param dateString
     * @return
     */
    private static String getTimeZoneIdFromDate(final String dateString) {
        String timeZoneId = null;
        if (dateString.contains(GMT)) {
            timeZoneId = dateString.substring(dateString.indexOf(GMT) + 3);
        }
        return timeZoneId;
    }

    /**
     * This method converts the date from client time zone to local time zone.
     *
     * @param fromTimeZoneString
     * @param toTimeZoneString
     * @param fromDateTime
     *
     * @return String
     */
    private static String convertTimeZones(final String fromTimeZoneString, final String toTimeZoneString, final String fromDateTime) {
        final DateTimeZone fromTimeZone = DateTimeZone.forID(fromTimeZoneString);
        final DateTimeZone toTimeZone = DateTimeZone.forID(toTimeZoneString);
        final DateTime dateTime = new DateTime(fromDateTime, fromTimeZone);
        final DateTimeFormatter outputFormatter = DateTimeFormat.forPattern(JODA_DATE_FORMAT_PATTERN_WITH_TIMEZONE).withZone(toTimeZone);
        return outputFormatter.print(dateTime);
    }

    //Standard ISO 8601
    // this https://jenkov.com/tutorials/java-internationalization/simpledateformat.html explains valid SimpleDateFormat pattern (as said above GMT+0600 is invalid , +0600 is valid)
    // About ISO 8601 there is a chapter that explain that with 2 customized patter we can manage some use cases for example of possible time zone strings:
    // This is the ISO 8601 pseudo-pattern yyyy-MM-ddTHH:mm:ss:sssZ where we have T inside and time zone Z could be:
    //Z  itseld   (i.e. UTC)
    //±hh:mm
    //±hhmm
    //±hh
    //Joda library seems to manage ISO 8601 natively with DateTime.parse(dateString) cause it uses internally ISODateTimeFormat
    //So this method parse standard iso 8601 string like 2022-11-20T16:50:21.253+04:00
    @SuppressWarnings({"squid:S2139","squid:S112"})
    public static Date parseIso8610StringWithJoda(final String dateString) {
        Date date = null;
        try {
            DateTime dt = DateTime.parse(dateString);
            date = dt.toDate();
        } catch (Exception e) {
            logger.error("Unable to parse data {} due to:{}", dateString, e.getMessage());
            throw e;
        }
        return date;
    }

    public static String convertDateToStringWithGMT(Date date) {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN_WITH_TIMEZONE);
        return simpleDateFormat.format(date);
    }

    public static String convertDateToStringWithExplicitGMTString(Date date) {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN_WITH_TIMEZONE);
        String tmp = simpleDateFormat.format(date);
        final StringTokenizer st = new StringTokenizer(tmp, "+");
        return st.nextToken() + " GMT+" + st.nextToken();
    }
}
