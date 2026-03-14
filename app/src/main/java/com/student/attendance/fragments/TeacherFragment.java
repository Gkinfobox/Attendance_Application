package com.student.attendance.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.android.volley.toolbox.StringRequest;
import com.student.attendance.R;
import com.student.attendance.models.AttendanceRecord;
import com.student.attendance.models.Student;
import com.student.attendance.network.ApiClient;
import com.student.attendance.utils.AppConstants;
import com.student.attendance.utils.DateUtils;
import com.student.attendance.utils.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class TeacherFragment extends Fragment {

    private TextView tvTotal, tvToday, tvAbsent;
    private Button btnTabSum, btnTabStudents;
    private View panelSum, panelStudents;
    private LinearLayout logList, studentList;
    private Button btnAddStudent, btnApplyFilter, btnClearFilter, btnPickDate, btnMarkAbsent;
    private Spinner spinStudent, spinType, spinClassFilter;
    private View loaderView;
    private TextView tvLoaderMsg;

    private String selectedDate = "";

    private SessionManager sm;
    private List<AttendanceRecord> allLogs      = new ArrayList<>();
    private List<Student>          allStudents  = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sm = new SessionManager(requireContext());

        loaderView    = view.findViewById(R.id.loader);
        tvLoaderMsg   = view.findViewById(R.id.tv_loader_msg);
        tvTotal       = view.findViewById(R.id.tv_adm_total);
        tvToday       = view.findViewById(R.id.tv_adm_today);
        tvAbsent      = view.findViewById(R.id.tv_adm_absent);
        btnTabSum      = view.findViewById(R.id.btn_adm_tab_sum);
        btnTabStudents = view.findViewById(R.id.btn_adm_tab_emp);
        panelSum       = view.findViewById(R.id.panel_sum);
        panelStudents  = view.findViewById(R.id.panel_emp);
        logList        = view.findViewById(R.id.adm_log_list);
        studentList    = view.findViewById(R.id.adm_emp_list);
        btnAddStudent  = view.findViewById(R.id.btn_add_emp);
        btnApplyFilter = view.findViewById(R.id.btn_apply_filter);
        btnClearFilter = view.findViewById(R.id.btn_clear_filter);
        btnPickDate    = view.findViewById(R.id.btn_pick_date);
        btnMarkAbsent  = view.findViewById(R.id.btn_mark_absent);
        spinStudent    = view.findViewById(R.id.spin_emp);
        spinType       = view.findViewById(R.id.spin_type);
        spinClassFilter= view.findViewById(R.id.spin_class_filter);

        btnTabSum.setOnClickListener(v -> showTab("sum"));
        btnTabStudents.setOnClickListener(v -> showTab("students"));
        btnAddStudent.setOnClickListener(v -> showAddStudentDialog());
        btnApplyFilter.setOnClickListener(v -> applyFilter());
        btnClearFilter.setOnClickListener(v -> clearFilter());
        btnMarkAbsent.setOnClickListener(v -> confirmMarkAbsent());

        btnPickDate.setOnClickListener(v -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            new DatePickerDialog(requireContext(), (dp, y, m, d) -> {
                selectedDate = String.format("%02d/%02d/%04d", d, m + 1, y);
                btnPickDate.setText(selectedDate);
            }, cal.get(java.util.Calendar.YEAR),
               cal.get(java.util.Calendar.MONTH),
               cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        showTab("sum");
        loadData();
    }

    private void loadData() {
        showLoader("Loading...");
        String url1 = AppConstants.SCRIPT_URL + "?action=" + AppConstants.ACTION_FETCH_ALL;
        String url2 = AppConstants.SCRIPT_URL + "?action=" + AppConstants.ACTION_FETCH_EMPLOYEES;

        StringRequest reqLogs = new StringRequest(com.android.volley.Request.Method.GET, url1,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        JSONArray records = obj.optJSONArray("records");
                        allLogs.clear();
                        if (records != null)
                            for (int i = 0; i < records.length(); i++)
                                allLogs.add(AttendanceRecord.fromJson(records.getJSONObject(i)));
                    } catch (Exception e) { /* ignore */ }
                    fetchStudents(url2);
                },
                error -> fetchStudents(url2));

        ApiClient.getInstance(requireContext()).addToRequestQueue(reqLogs);
    }

    private void fetchStudents(String url) {
        StringRequest req = new StringRequest(com.android.volley.Request.Method.GET, url,
                response -> {
                    hideLoader();
                    if (!isAdded()) return;
                    try {
                        JSONObject obj = new JSONObject(response);
                        JSONArray emps = obj.optJSONArray("employees");
                        allStudents.clear();
                        if (emps != null)
                            for (int i = 0; i < emps.length(); i++)
                                allStudents.add(Student.fromJson(emps.getJSONObject(i)));
                    } catch (Exception e) { /* ignore */ }
                    updateStats();
                    setupSpinners();
                    renderLogs(allLogs);
                    renderStudents();
                },
                error -> { hideLoader(); if (isAdded()) toast("Error loading data"); });

        ApiClient.getInstance(requireContext()).addToRequestQueue(req);
    }

    private void updateStats() {
        String today = DateUtils.todayStr();
        tvTotal.setText(String.valueOf(allStudents.size()));
        long todayPresent = allLogs.stream()
                .filter(l -> today.equals(l.date != null ? l.date.trim() : "") && l.hasCheckIn() && !l.isAbsent())
                .count();
        long todayAbsent = allLogs.stream()
                .filter(l -> today.equals(l.date != null ? l.date.trim() : "") && l.isAbsent())
                .count();
        tvToday.setText(String.valueOf(todayPresent));
        tvAbsent.setText(String.valueOf(todayAbsent));
    }

    private void setupSpinners() {
        // Student spinner
        List<String> studentItems = new ArrayList<>();
        studentItems.add("All Students");
        for (Student s : allStudents) studentItems.add(s.name + " (" + s.regNo + ")");
        ArrayAdapter<String> sa = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, studentItems);
        sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinStudent.setAdapter(sa);

        // Class/section spinner — collect unique values
        List<String> classes = new ArrayList<>();
        classes.add("All Classes");
        for (Student s : allStudents) {
            if (s.classSection != null && !s.classSection.isEmpty() && !classes.contains(s.classSection))
                classes.add(s.classSection);
        }
        ArrayAdapter<String> ca = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, classes);
        ca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinClassFilter.setAdapter(ca);
    }

    private void applyFilter() {
        int stuIdx   = spinStudent.getSelectedItemPosition();
        int typeIdx  = spinType.getSelectedItemPosition();
        int classIdx = spinClassFilter.getSelectedItemPosition();

        String selRegNo = stuIdx > 0 ? allStudents.get(stuIdx - 1).regNo : "";
        String selClass = classIdx > 0
                ? spinClassFilter.getItemAtPosition(classIdx).toString()
                : "";

        List<AttendanceRecord> filtered = new ArrayList<>(allLogs);

        if (!selRegNo.isEmpty())
            filtered.removeIf(l -> !selRegNo.equals(l.empid));

        if (!selClass.isEmpty())
            filtered.removeIf(l -> !selClass.equals(l.classSection != null ? l.classSection.trim() : ""));

        if (typeIdx == 1) filtered.removeIf(l -> !l.hasCheckIn() || l.isAbsent());
        else if (typeIdx == 2) filtered.removeIf(l -> !l.isAbsent());
        else if (typeIdx == 3) filtered.removeIf(l -> !l.hasCheckOut());

        if (!selectedDate.isEmpty())
            filtered.removeIf(l -> !selectedDate.equals(l.date != null ? l.date.trim() : ""));

        renderLogs(filtered);
    }

    private void clearFilter() {
        spinStudent.setSelection(0);
        spinType.setSelection(0);
        spinClassFilter.setSelection(0);
        selectedDate = "";
        btnPickDate.setText("Select Date");
        renderLogs(allLogs);
    }

    private void confirmMarkAbsent() {
        String dateToMark = selectedDate.isEmpty() ? DateUtils.todayStr() : selectedDate;
        new AlertDialog.Builder(requireContext())
                .setTitle("Mark Absent")
                .setMessage("Auto-mark all students who haven't checked in on " + dateToMark + " as Absent?")
                .setPositiveButton("Mark Absent", (d, w) -> doMarkAbsent(dateToMark))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doMarkAbsent(String date) {
        showLoader("Marking absent...");
        String url = AppConstants.SCRIPT_URL + "?action=" + AppConstants.ACTION_MARK_ABSENT + "&date=" + date;
        StringRequest req = new StringRequest(com.android.volley.Request.Method.GET, url,
                response -> {
                    hideLoader();
                    if (!isAdded()) return;
                    try {
                        JSONObject obj = new JSONObject(response);
                        if ("success".equals(obj.optString("status"))) {
                            toast("✅ Absent marked for " + date);
                            loadData();
                        } else {
                            toast("Error: " + obj.optString("message", "Failed"));
                        }
                    } catch (Exception e) { toast("Error"); }
                },
                error -> { hideLoader(); toast("Cannot connect"); });
        ApiClient.getInstance(requireContext()).addToRequestQueue(req);
    }

    private void renderLogs(List<AttendanceRecord> logs) {
        logList.removeAllViews();
        if (logs.isEmpty()) {
            TextView tv = new TextView(getContext());
            tv.setText("No records found");
            tv.setTextColor(getResources().getColor(R.color.muted_light, null));
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setPadding(0, 48, 0, 0);
            logList.addView(tv);
            return;
        }
        for (AttendanceRecord r : logs) {
            View item = LayoutInflater.from(getContext()).inflate(R.layout.item_log, logList, false);
            String icon = r.isAbsent() ? "❌" : r.hasCheckOut() ? "🚪" : r.hasCheckIn() ? "✅" : "🕐";
            ((TextView) item.findViewById(R.id.tv_log_icon)).setText(icon);
            String displayName = (r.name != null && !r.name.isEmpty()) ? r.name : r.empid;
            ((TextView) item.findViewById(R.id.tv_log_name)).setText(displayName);
            String cls  = (r.classSection != null && !r.classSection.isEmpty()) ? " · " + r.classSection : "";
            String dept = (r.dept != null && !r.dept.isEmpty()) ? " · " + r.dept : "";
            String timeInfo = r.isAbsent() ? "Absent"
                    : r.hasCheckIn() ? (r.hasCheckOut()
                    ? "In: " + r.ciTime + " · Out: " + r.coTime
                    : "In: " + r.ciTime) : "—";
            ((TextView) item.findViewById(R.id.tv_log_meta)).setText(
                    (r.date != null ? r.date : "") + cls + dept + " · " + timeInfo);
            TextView badge = item.findViewById(R.id.tv_log_badge);
            if (r.isAbsent()) {
                badge.setText("Absent"); badge.setVisibility(View.VISIBLE);
                badge.setBackgroundResource(R.drawable.bg_badge_out);
                badge.setTextColor(getResources().getColor(R.color.muted_light, null));
            } else if (r.hasCheckOut()) {
                badge.setText("Checked Out"); badge.setVisibility(View.VISIBLE);
                badge.setBackgroundResource(R.drawable.bg_badge_out);
                badge.setTextColor(getResources().getColor(R.color.red, null));
            } else if (r.hasCheckIn()) {
                badge.setText("Present"); badge.setVisibility(View.VISIBLE);
                badge.setBackgroundResource(R.drawable.bg_badge_in);
                badge.setTextColor(getResources().getColor(R.color.success, null));
            } else {
                badge.setVisibility(View.GONE);
            }
            logList.addView(item);
        }
    }

    private void renderStudents() {
        studentList.removeAllViews();
        if (allStudents.isEmpty()) {
            TextView tv = new TextView(getContext());
            tv.setText("No students found");
            tv.setTextColor(getResources().getColor(R.color.muted_light, null));
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setPadding(0, 48, 0, 0);
            studentList.addView(tv);
            return;
        }
        for (Student s : allStudents) {
            View item = LayoutInflater.from(getContext()).inflate(R.layout.item_employee, studentList, false);
            ((TextView) item.findViewById(R.id.tv_emp_initial)).setText(s.getInitial());
            ((TextView) item.findViewById(R.id.tv_emp_name)).setText(s.name);
            String cls = s.classSection != null && !s.classSection.isEmpty() ? " · " + s.classSection : "";
            ((TextView) item.findViewById(R.id.tv_emp_meta)).setText(
                    s.regNo + cls + " · " + (s.dept.isEmpty() ? "—" : s.dept)
                            + " · " + (s.mobile.isEmpty() ? "—" : s.mobile));
            Button btnRemove = item.findViewById(R.id.btn_remove_emp);
            btnRemove.setOnClickListener(v -> confirmDelete(s));
            studentList.addView(item);
        }
    }

    private void confirmDelete(Student s) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Student")
                .setMessage("Remove " + s.name + " (" + s.regNo + ")?")
                .setPositiveButton("Remove", (d, w) -> deleteStudent(s))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteStudent(Student s) {
        showLoader("Removing...");
        String url = AppConstants.SCRIPT_URL + "?action=" + AppConstants.ACTION_DELETE_EMPLOYEE + "&empid=" + s.regNo;
        StringRequest req = new StringRequest(com.android.volley.Request.Method.GET, url,
                response -> {
                    hideLoader();
                    if (!isAdded()) return;
                    try {
                        JSONObject obj = new JSONObject(response);
                        if ("success".equals(obj.optString("status"))) { toast("Removed"); loadData(); }
                        else toast("Error removing student");
                    } catch (Exception ex) { toast("Error"); }
                },
                error -> { hideLoader(); toast("Error"); });
        ApiClient.getInstance(requireContext()).addToRequestQueue(req);
    }

    private void showAddStudentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_student, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        EditText etName     = dialogView.findViewById(R.id.et_ae_name);
        EditText etRegNo    = dialogView.findViewById(R.id.et_ae_empid);
        EditText etMobile   = dialogView.findViewById(R.id.et_ae_mobile);
        EditText etEmail    = dialogView.findViewById(R.id.et_ae_email);
        EditText etClass    = dialogView.findViewById(R.id.et_ae_class);
        Spinner  spinDept   = dialogView.findViewById(R.id.spin_ae_dept);
        EditText etDeptOther= dialogView.findViewById(R.id.et_ae_dept_other);
        EditText etPwd      = dialogView.findViewById(R.id.et_ae_pwd);
        Button   btnAdd     = dialogView.findViewById(R.id.btn_ae_add);
        Button   btnCancel  = dialogView.findViewById(R.id.btn_ae_cancel);

        spinDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                etDeptOther.setVisibility("Other".equals(spinDept.getSelectedItem().toString()) ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnAdd.setOnClickListener(v -> {
            String name   = etName.getText().toString().trim();
            String regNo  = etRegNo.getText().toString().trim().toUpperCase();
            String mobile = etMobile.getText().toString().trim();
            String email  = etEmail.getText().toString().trim();
            String cls    = etClass.getText().toString().trim();
            String deptSel= spinDept.getSelectedItem().toString();
            String dept   = "Other".equals(deptSel) ? etDeptOther.getText().toString().trim() : deptSel;
            String pwd    = etPwd.getText().toString();

            if (name.isEmpty() || regNo.isEmpty() || mobile.isEmpty() || email.isEmpty() || pwd.isEmpty()) {
                toast("Please fill all required fields"); return;
            }
            dialog.dismiss();
            showLoader("Adding...");
            try {
                String url = AppConstants.SCRIPT_URL + "?action=" + AppConstants.ACTION_REGISTER
                        + "&name="         + java.net.URLEncoder.encode(name, "UTF-8")
                        + "&empid="        + regNo
                        + "&mobile="       + mobile
                        + "&email="        + java.net.URLEncoder.encode(email, "UTF-8")
                        + "&dept="         + java.net.URLEncoder.encode(dept, "UTF-8")
                        + "&classSection=" + java.net.URLEncoder.encode(cls, "UTF-8")
                        + "&pwd="          + java.net.URLEncoder.encode(pwd, "UTF-8");

                StringRequest req = new StringRequest(com.android.volley.Request.Method.GET, url,
                        response -> {
                            hideLoader();
                            if (!isAdded()) return;
                            try {
                                JSONObject obj = new JSONObject(response);
                                if ("success".equals(obj.optString("status"))) { toast("Student added!"); loadData(); }
                                else if ("duplicate".equals(obj.optString("status"))) toast("Register number already exists");
                                else toast("Error adding student");
                            } catch (Exception ex) { toast("Error"); }
                        },
                        error -> { hideLoader(); toast("Error adding student"); });
                ApiClient.getInstance(requireContext()).addToRequestQueue(req);
            } catch (Exception ex) { hideLoader(); toast("Encoding error"); }
        });
    }

    private void showTab(String tab) {
        boolean isSum = "sum".equals(tab);
        panelSum.setVisibility(isSum ? View.VISIBLE : View.GONE);
        panelStudents.setVisibility(isSum ? View.GONE : View.VISIBLE);

        int white = getResources().getColor(R.color.white, null);
        int muted = getResources().getColor(R.color.muted_light, null);

        if (isSum) {
            btnTabSum.setBackgroundResource(R.drawable.bg_btn_red); btnTabSum.setTextColor(white);
            btnTabStudents.setBackgroundResource(android.R.color.transparent); btnTabStudents.setTextColor(muted);
        } else {
            btnTabStudents.setBackgroundResource(R.drawable.bg_btn_red); btnTabStudents.setTextColor(white);
            btnTabSum.setBackgroundResource(android.R.color.transparent); btnTabSum.setTextColor(muted);
        }
    }

    private void showLoader(String msg) { tvLoaderMsg.setText(msg); loaderView.setVisibility(View.VISIBLE); }
    private void hideLoader()           { loaderView.setVisibility(View.GONE); }
    private void toast(String msg)      { Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show(); }
}
