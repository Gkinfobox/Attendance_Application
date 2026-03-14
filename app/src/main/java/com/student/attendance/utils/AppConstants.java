package com.student.attendance.utils;

public class AppConstants {

    // ── REPLACE THIS WITH YOUR GOOGLE APPS SCRIPT URL ──────────────────────
    public static final String SCRIPT_URL = "https://script.google.com/macros/s/AKfycbzTQVVAiIf3TYy_Ogespzbm-bttnxOCXVaMFKIpqa51Rt2Vf4hb6gJkZ654jrxgB_f4IQ/exec";
    // ────────────────────────────────────────────────────────────────────────

    // Actions
    public static final String ACTION_LOGIN            = "login";
    public static final String ACTION_CHECKIN          = "checkin";
    public static final String ACTION_CHECKOUT         = "checkout";
    public static final String ACTION_FETCH            = "fetch";
    public static final String ACTION_FETCH_ALL        = "fetchAll";
    public static final String ACTION_FETCH_EMPLOYEES  = "fetchEmployees";
    public static final String ACTION_REGISTER         = "register";
    public static final String ACTION_DELETE_EMPLOYEE  = "deleteEmployee";
    public static final String ACTION_SEND_OTP         = "sendOtp";
    public static final String ACTION_VERIFY_OTP       = "verifyOtp";
    public static final String ACTION_RESET_PASSWORD   = "resetPassword";
    public static final String ACTION_MARK_ABSENT      = "markAbsent";

    // Prefs keys
    public static final String PREF_NAME       = "student_prefs";
    public static final String PREF_SESSION    = "session";
    public static final String PREF_LOGS_CACHE = "logs_cache";
    public static final String PREF_THEME      = "theme_dark";

    // Role prefixes
    public static final String TEACHER_ID_PREFIX = "TEACH";

    // Auto-absent time: 9:30 AM
    public static final int ABSENT_HOUR   = 9;
    public static final int ABSENT_MINUTE = 30;
}
