package com.student.attendance.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    public static String todayStr() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

    public static String displayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

    public static String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "morning";
        else if (hour < 17) return "afternoon";
        else return "evening";
    }

    public static String fmtTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(date);
    }

    public static String calcHours(String ciTime, String coTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date in = sdf.parse(ciTime);
            Date out = sdf.parse(coTime);
            if (in == null || out == null) return "";
            long diff = out.getTime() - in.getTime();
            long hours = diff / (1000 * 60 * 60);
            long minutes = (diff % (1000 * 60 * 60)) / (1000 * 60);
            return hours + "h " + minutes + "m";
        } catch (Exception e) {
            return "";
        }
    }

    /** Returns true if current time is past 9:30 AM */
    public static boolean isPastAbsentTime() {
        Calendar now = Calendar.getInstance();
        int hour   = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        return (hour > AppConstants.ABSENT_HOUR)
                || (hour == AppConstants.ABSENT_HOUR && minute >= AppConstants.ABSENT_MINUTE);
    }
}
