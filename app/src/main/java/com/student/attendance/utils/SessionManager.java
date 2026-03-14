package com.student.attendance.utils;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONException;
import org.json.JSONObject;

public class SessionManager {
    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String regNo, String name, String dept, String email, String mobile,
                            String classSection, boolean isTeacher) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("empid", regNo);
            obj.put("name", name);
            obj.put("dept", dept != null ? dept : "");
            obj.put("email", email != null ? email : "");
            obj.put("mobile", mobile != null ? mobile : "");
            obj.put("classSection", classSection != null ? classSection : "");
            obj.put("isTeacher", isTeacher);
            prefs.edit().putString(AppConstants.PREF_SESSION, obj.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject getSession() {
        String s = prefs.getString(AppConstants.PREF_SESSION, null);
        if (s == null) return null;
        try { return new JSONObject(s); } catch (JSONException e) { return null; }
    }

    public boolean isLoggedIn() { return getSession() != null; }

    public boolean isTeacher() {
        JSONObject s = getSession();
        if (s == null) return false;
        return s.optString("empid", "").startsWith(AppConstants.TEACHER_ID_PREFIX);
    }

    public void clearSession() {
        prefs.edit().remove(AppConstants.PREF_SESSION).apply();
    }

    public boolean isDarkTheme() {
        return prefs.getBoolean(AppConstants.PREF_THEME, false);
    }

    public void setDarkTheme(boolean dark) {
        prefs.edit().putBoolean(AppConstants.PREF_THEME, dark).apply();
    }

    public void saveLogsCache(String json) {
        prefs.edit().putString(AppConstants.PREF_LOGS_CACHE, json).apply();
    }

    public String getLogsCache() {
        return prefs.getString(AppConstants.PREF_LOGS_CACHE, "[]");
    }
}
