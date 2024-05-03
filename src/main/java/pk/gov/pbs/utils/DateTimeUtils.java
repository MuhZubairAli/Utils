package pk.gov.pbs.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DateTimeUtils {
    public static final String defaultDateTimeFormat = "dd/MM/yyyy HH:mm:ss";
    public static final String defaultDateOnlyFormat = "dd/MM/yyyy";
    public static final String defaultTimeOnlyFormat = "HH:mm:ss"; //24 hour format
    public static final String sqlTimestampFormat = "yyyy-MM-dd'T'HH:mm:ss";
    private static Calendar calendar;
    private static final Map<String, SimpleDateFormat> cache = new HashMap<>();

    public static Calendar getCalendar(){
        if (calendar == null)
            calendar = Calendar.getInstance();
        return calendar;
    }

    /**
     * get current time stamp in unix time
     * @return number of millis since epoch time
     */
    public static long getCurrentDateTimeUnix(){
        return System.currentTimeMillis() / 1000;
    }

    public static Date getCurrentDateTime() {
        return getCalendar().getTime();
    }

    public static String getCurrentDateTime(String format){
        if (!cache.containsKey(format))
            cache.put(format, new SimpleDateFormat(format, Locale.getDefault()));

        return cache.get(format).format(getCalendar().getTime());
    }

    public static String formatDateTime(String toFormat, long unixTs){
        if(!cache.containsKey(toFormat))
            cache.put(toFormat, new SimpleDateFormat(toFormat, Locale.getDefault()));

        try {
            return cache.get(toFormat).format(getDateFrom(unixTs));
        } catch (NullPointerException e) {
            ExceptionReporter.handle(e);
        }
        return String.valueOf(unixTs);
    }

    public static String formatDateTime(long unix){
        return formatDateTime(defaultDateTimeFormat, unix);
    }

    public static String formatDateTime(String toFormat, Date subject){
        if (subject == null)
            return null;

        if(!cache.containsKey(toFormat))
            cache.put(toFormat, new SimpleDateFormat(toFormat, Locale.UK));

        try {
            return cache.get(toFormat).format(subject);
        } catch (NullPointerException e) {
            ExceptionReporter.handle(e);
        }

        return subject.toString();
    }

    public static String formatDateTime(Date subject){
        return formatDateTime(defaultDateTimeFormat, subject);
    }

    public static String formatDateTime(String fromFormat, String toFormat, String subject){
        if (subject == null)
            return null;

        if(!cache.containsKey(fromFormat))
            cache.put(fromFormat, new SimpleDateFormat(fromFormat, Locale.getDefault()));

        if(!cache.containsKey(toFormat))
            cache.put(toFormat, new SimpleDateFormat(toFormat, Locale.getDefault()));

        try {
            Date date = cache.get(fromFormat).parse(subject);
            return cache.get(toFormat).format(date);
        } catch (ParseException | NullPointerException e) {
            ExceptionReporter.handle(e);
            return subject;
        }
    }

    public static String formatDateTime(String fromFormat, String subject){
        return formatDateTime(fromFormat, defaultDateTimeFormat, subject);
    }

    //=============== Formatting methods only for Dates for commonly used formats =================
    /**
     * This is override of formatDateTime(String, String, String)
     * It formats date from specified format to dd/MM/yyyy
     * @param fromFormat format of subject
     * @param subject string date
     * @return date in dd/MM/yyyy format as string
     */
    public static String formatDateFrom(String fromFormat, String subject){
        return formatDateTime(fromFormat, "dd/MM/yyyy",subject);
    }

    /**
     * This is override of formatDateTime(String, String, String)
     * it will format date to specified format, it assumes current format of date
     * is MM/dd/yyyy
     * @param toFormat target format
     * @param subject date in string format
     * @return date in target format from MM/dd/yyyy
     */
    public static String formatDateTo(String toFormat, String subject){
        return formatDateTime("MM/dd/yyyy", toFormat, subject);
    }

    /**
     * This is override of formatDateTime(String, String, String) for most used date formats conversion
     * it will format date from MM/dd/yyyy to dd/MM/yyyy
     * @param subject date in MM/dd/yyyy format
     * @return date in dd/MM/yyyy format as string
     */
    public static String formatDate(String subject){
        return formatDateTime("MM/dd/yyyy", "dd/MM/yyyy", subject);
    }

    //============================== Construct Date object from ===================================
    public static Date getDateFrom(String format, String subject){
        if(!cache.containsKey(format))
            cache.put(format, new SimpleDateFormat(format, Locale.getDefault()));
        try {
            return cache.get(format).parse(subject);
        } catch (ParseException e) {
            ExceptionReporter.handle(e);
            return null;
        }
    }

    public static Date getDateFrom(long unixInSeconds){
        return new Date(unixInSeconds*1000L);
    }

    public static Date getDateFrom(int year, int month, int day){
        String format = "dd/MM/yyyy";
        if(!cache.containsKey(format))
            cache.put(format, new SimpleDateFormat(format, Locale.getDefault()));
        try {
            return cache.get(format).parse(day + "/" + month + "/" + year);
        } catch (ParseException e) {
            ExceptionReporter.handle(e);
            return null;
        }
    }

    //========================== Duration calculations between two dates ==========================
    public static long getDurationBetweenInMillis(Date fromDate, Date toDate) {
        long difference_In_Time
                = toDate.getTime() - fromDate.getTime();
        difference_In_Time -= (long) getLeapYearCount(fromDate, toDate) * 24 * 60 * 60 * 1000;
        return difference_In_Time;
    }

    public static long getDurationBetweenInSeconds(Date fromDate, Date toDate) {
        return TimeUnit.MILLISECONDS
                .toSeconds(getDurationBetweenInMillis(fromDate, toDate));
    }

    public static long getDurationBetweenInDays(Date fromDate, Date toDate) {
        return TimeUnit.MILLISECONDS.toDays(getDurationBetweenInMillis(fromDate, toDate));
    }

    public static long getDurationBetweenInYears(Date fromDate, Date toDate) {
        return TimeUnit
                .MILLISECONDS
                .toDays(getDurationBetweenInMillis(fromDate, toDate))
                / 365L;
    }

    public static long getDurationBetweenIn(TimeUnit timeUnit, Date fromDate, Date toDate) {
        return timeUnit.convert(getDurationBetweenInMillis(fromDate, toDate), TimeUnit.MILLISECONDS);
    }

    public static int getLeapYearCount(Date fromDate, Date toDate) {
        calendar.setTime(fromDate);
        int fy = calendar.get(Calendar.YEAR);
        calendar.setTime(toDate);
        int ty = calendar.get(Calendar.YEAR);

        int lyc = 0; //leap year count
        for (; fy <= ty; fy++){
            if (fy % 4 == 0 && (fy % 100 != 0 || fy % 400 == 0))
                lyc++;
        }
        return lyc;
    }

    // Function to print difference in (for reference and testing)
    // time start_date and end_date
    private static void printDifference(String start_date, String end_date, String date_format) {
        SimpleDateFormat sdf = new SimpleDateFormat(date_format, Locale.getDefault());
        try {
            Date d1 = sdf.parse(start_date);
            Date d2 = sdf.parse(end_date);

            // Calucalte time difference
            long difference_In_Time
                    = getDurationBetweenInMillis(d1, d2);

            long difference_In_Seconds
                    = TimeUnit.MILLISECONDS
                    .toSeconds(difference_In_Time)
                    % 60;

            long difference_In_Minutes
                    = TimeUnit
                    .MILLISECONDS
                    .toMinutes(difference_In_Time)
                    % 60;

            long difference_In_Hours
                    = TimeUnit
                    .MILLISECONDS
                    .toHours(difference_In_Time)
                    % 24;

            long difference_In_Days
                    = TimeUnit
                    .MILLISECONDS
                    .toDays(difference_In_Time)
                    % 365;

            long difference_In_Years
                    = TimeUnit
                    .MILLISECONDS
                    .toDays(difference_In_Time)
                    / 365l;

            // Print the date difference in
            // years, in days, in hours, in
            // minutes, and in seconds
            System.out.print(
                    "Difference"
                            + " between two dates is: ");

            // Print result
            System.out.println(
                    difference_In_Years
                            + " years, "
                            + difference_In_Days
                            + " days, "
                            + difference_In_Hours
                            + " hours, "
                            + difference_In_Minutes
                            + " minutes, "
                            + difference_In_Seconds
                            + " seconds");

        } catch (ParseException e) {
            ExceptionReporter.handle(e);
        }
    }

}
