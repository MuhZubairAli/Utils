package pk.gov.pbs.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DateFormatter {
    public static Calendar calendar = Calendar.getInstance();
    protected static SimpleDateFormat fromFormatter = null;
    protected static SimpleDateFormat toFormatter = null;
    private static Map<String, SimpleDateFormat> cache = new HashMap<>();

    private static String formatDateCached(String fromFormat, String toFormat, String subject){
        if(fromFormatter == null)
            fromFormatter = new SimpleDateFormat(fromFormat, Locale.UK);

        if(toFormatter == null)
            toFormatter = new SimpleDateFormat(toFormat, Locale.UK);

        try {
            Date date = fromFormatter.parse(subject);
            return toFormatter.format(date);
        } catch (ParseException | NullPointerException e) {
            e.printStackTrace();
            return subject;
        }

    }

    public static String formatDate(String fromFormat, String toFormat, String subject){
        try {
            SimpleDateFormat sdfFrom = new SimpleDateFormat(fromFormat, Locale.UK);
            SimpleDateFormat sdfTo = new SimpleDateFormat(toFormat, Locale.UK);
            Date date = sdfFrom.parse(subject);
            return sdfTo.format(date);
        } catch (ParseException | NullPointerException e) {
            e.printStackTrace();
            return subject;
        }
    }

    public static String formatDateFrom(String fromFormat, String subject){
        return formatDate(fromFormat, "dd/MM/yyyy",subject);
    }

    public static String formatDateTo(String toFormat, String subject){
        return formatDate("MM/dd/yyyy", toFormat, subject);
    }

    public static String formatDate(String subject){
        return formatDateCached("MM/dd/yyyy", "dd/MM/yyyy", subject);
    }

    public static String formatDate(long unix){
        if(toFormatter == null)
            toFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);

        try {
            Date ts = new Date(Long.parseLong(unix*1000L+""));
            return toFormatter.format(ts);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return unix + "";
        }
    }

    public static String formatDate(long unix, String format){
        if(!cache.containsKey(format))
            cache.put(format, new SimpleDateFormat(format, Locale.UK));

        try {
            Date ts = new Date(Long.parseLong(unix*1000L+""));
            return cache.get(format).format(ts);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return unix + "";
        }
    }

    // Function to print difference in
    // time start_date and end_date
    private static void findDifference(String start_date, String end_date, String date_format) {
        SimpleDateFormat sdf = new SimpleDateFormat(date_format);
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
            e.printStackTrace();
        }
    }

    public static Date get(int year, int month, int day){
        if(toFormatter == null)
            toFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        try {
            return toFormatter.parse(day + "/" + month + "/" + year);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static long getDurationBetweenInYears(Date fromDate, Date toDate) {
        return TimeUnit
                .MILLISECONDS
                .toDays(getDurationBetweenInMillis(fromDate, toDate))
                / 365L;
    }

    public static long getDurationBetweenInMillis(Date fromDate, Date toDate) {
        long difference_In_Time
                = toDate.getTime() - fromDate.getTime();
        difference_In_Time -= (long) getLeapYearCount(fromDate, toDate) * 24 * 60 * 60 * 1000;
        return difference_In_Time;
    }

    private static int getLeapYearCount(Date fromDate, Date toDate) {
        calendar.setTime(fromDate);
        int fy = calendar.get(Calendar.YEAR);
        calendar.setTime(toDate);
        int ty = calendar.get(Calendar.YEAR);

        int lyc = 0; //leap year count
        for (; fy <= ty; fy++){
            if (fy % 4 == 0)
                lyc++;
        }
        return lyc;
    }
}
