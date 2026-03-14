package com.student.attendance.models;

import org.json.JSONObject;

public class AttendanceRecord {
    public String date;
    public String empid;      // register number
    public String name;
    public String dept;
    public String classSection;
    public String ciTime;
    public String coTime;
    public String workhours;
    public String status;     // "present", "absent", "late"
    public double ciLat, ciLng;
    public double coLat, coLng;

    public AttendanceRecord() {}

    public static AttendanceRecord fromJson(JSONObject obj) {
        AttendanceRecord r = new AttendanceRecord();
        r.date         = obj.optString("date", "");
        r.empid        = obj.optString("empid", "");
        r.name         = obj.optString("name", "");
        r.dept         = obj.optString("dept", "");
        r.classSection = obj.optString("classSection", "");
        r.ciTime       = obj.optString("ciTime", "");
        r.coTime       = obj.optString("coTime", "");
        r.workhours    = obj.optString("workhours", "");
        r.status       = obj.optString("status", "");
        r.ciLat        = obj.optDouble("ciLat", 0);
        r.ciLng        = obj.optDouble("ciLng", 0);
        r.coLat        = obj.optDouble("coLat", 0);
        r.coLng        = obj.optDouble("coLng", 0);
        return r;
    }

    public boolean hasCheckIn()  { return ciTime != null && !ciTime.isEmpty(); }
    public boolean hasCheckOut() { return coTime != null && !coTime.isEmpty(); }
    public boolean isAbsent()    { return "absent".equalsIgnoreCase(status); }

    public boolean isValidDate() {
        return date != null && date.contains("/") && date.length() <= 12;
    }
}
