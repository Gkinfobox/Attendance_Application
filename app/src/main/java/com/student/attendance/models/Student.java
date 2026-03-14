package com.student.attendance.models;

import org.json.JSONObject;

public class Student {
    public String regNo;       // Register Number (replaces empid)
    public String name;
    public String dept;
    public String email;
    public String mobile;
    public String classSection;

    public static Student fromJson(JSONObject obj) {
        Student s = new Student();
        s.regNo        = obj.optString("empid", "");
        s.name         = obj.optString("name", "");
        s.dept         = obj.optString("dept", "");
        s.email        = obj.optString("email", "");
        s.mobile       = obj.optString("mobile", "");
        s.classSection = obj.optString("classSection", "");
        return s;
    }

    public String getInitial() {
        return (name != null && !name.isEmpty())
                ? String.valueOf(name.charAt(0)).toUpperCase()
                : "?";
    }
}
