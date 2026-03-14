package com.student.attendance.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.android.volley.toolbox.StringRequest;
import com.student.attendance.R;
import com.student.attendance.activities.MainActivity;
import com.student.attendance.models.AttendanceRecord;
import com.student.attendance.network.ApiClient;
import com.student.attendance.utils.AppConstants;
import com.student.attendance.utils.DateUtils;
import com.student.attendance.utils.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvGreeting, tvName, tvStatusVal, tvStatusSub;
    private TextView tvStatusIcon, tvHomeDate, tvSectionLabel;
    private TextView tvAvatar;
    private View statusCard, divider;
    private LinearLayout logList;
    private Button btnLogout;

    private SessionManager sm;
    private List<AttendanceRecord> dayLogs = new ArrayList<>();
    private boolean isTeacher = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sm = new SessionManager(requireContext());

        tvGreeting    = view.findViewById(R.id.tv_greeting);
        tvName        = view.findViewById(R.id.tv_name);
        tvStatusVal   = view.findViewById(R.id.tv_status_val);
        tvStatusSub   = view.findViewById(R.id.tv_status_sub);
        tvStatusIcon  = view.findViewById(R.id.tv_status_icon);
        tvHomeDate    = view.findViewById(R.id.tv_home_date);
        tvAvatar      = view.findViewById(R.id.tv_avatar);
        tvSectionLabel= view.findViewById(R.id.tv_section_label);
        statusCard    = view.findViewById(R.id.status_card);
        divider       = view.findViewById(R.id.divider);
        logList       = view.findViewById(R.id.log_list);
        btnLogout     = view.findViewById(R.id.btn_logout);

        JSONObject session = sm.getSession();
        if (session != null) {
            String name  = session.optString("name", "Student");
            String empid = session.optString("empid", "");
            isTeacher = empid.startsWith(AppConstants.TEACHER_ID_PREFIX);
            tvName.setText(name + " 👋");
            tvAvatar.setText(name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase());
        }

        tvGreeting.setText("Good " + DateUtils.getGreeting());
        tvHomeDate.setText(DateUtils.displayDate());

        btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).doLogout();
            }
        });

        if (isTeacher) {
            statusCard.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
            tvSectionLabel.setText("Today's Attendance — " + DateUtils.todayStr());
            fetchAllTodayLogs();
        } else {
            tvSectionLabel.setText("Recent Activity");
            loadCache();
            renderStatus();
            renderRecent();
            fetchDayLogs();
        }
    }

    // ── TEACHER: today's all-student attendance ────────────────────────────
    private void fetchAllTodayLogs() {
        logList.removeAllViews();
        TextView loading = new TextView(getContext());
        loading.setText("⏳ Loading today's attendance...");
        loading.setTextColor(getResources().getColor(R.color.muted_light, null));
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(0, 48, 0, 0);
        logList.addView(loading);

        String url = AppConstants.SCRIPT_URL + "?action=" + AppConstants.ACTION_FETCH_ALL;
        StringRequest req = new StringRequest(com.android.volley.Request.Method.GET, url,
                response -> {
                    if (!isAdded()) return;
                    try {
                        JSONObject obj = new JSONObject(response);
                        JSONArray records = obj.optJSONArray("records");
                        String today = DateUtils.todayStr();
                        List<AttendanceRecord> todayLogs = new ArrayList<>();
                        if (records != null) {
                            for (int i = 0; i < records.length(); i++) {
                                AttendanceRecord r = AttendanceRecord.fromJson(records.getJSONObject(i));
                                if (r.isValidDate() && today.equals(r.date.trim())) {
                                    todayLogs.add(r);
                                }
                            }
                        }
                        renderTeacherTodayLogs(todayLogs);
                    } catch (Exception e) {
                        showEmpty("Could not load today's attendance");
                    }
                },
                error -> showEmpty("📡 Cannot connect to server"));

        ApiClient.getInstance(requireContext()).addToRequestQueue(req);
    }

    private void renderTeacherTodayLogs(List<AttendanceRecord> logs) {
        logList.removeAllViews();

        int present = 0, absent = 0, checkedOut = 0;
        for (AttendanceRecord r : logs) {
            if (r.isAbsent()) absent++;
            else if (r.hasCheckOut()) checkedOut++;
            else if (r.hasCheckIn()) present++;
        }

        // Stats row
        LinearLayout statsRow = new LinearLayout(getContext());
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, 16);
        statsRow.setLayoutParams(rowParams);
        statsRow.addView(makeStat("✅ Present", String.valueOf(present), R.color.success));
        View s1 = new View(getContext()); s1.setLayoutParams(new LinearLayout.LayoutParams(12, 1)); statsRow.addView(s1);
        statsRow.addView(makeStat("🚪 Checked Out", String.valueOf(checkedOut), R.color.red));
        View s2 = new View(getContext()); s2.setLayoutParams(new LinearLayout.LayoutParams(12, 1)); statsRow.addView(s2);
        statsRow.addView(makeStat("❌ Absent", String.valueOf(absent), R.color.muted_light));
        logList.addView(statsRow);

        if (logs.isEmpty()) { showEmpty("📭 No attendance records for today yet"); return; }

        for (AttendanceRecord r : logs) {
            View item = LayoutInflater.from(getContext()).inflate(R.layout.item_log, logList, false);
            String icon = r.isAbsent() ? "❌" : r.hasCheckOut() ? "🚪" : "✅";
            ((TextView) item.findViewById(R.id.tv_log_icon)).setText(icon);
            String displayName = (r.name != null && !r.name.isEmpty()) ? r.name : r.empid;
            ((TextView) item.findViewById(R.id.tv_log_name)).setText(displayName);
            String cls  = (r.classSection != null && !r.classSection.isEmpty()) ? " · " + r.classSection : "";
            String dept = (r.dept != null && !r.dept.isEmpty()) ? " · " + r.dept : "";
            String timeInfo = r.isAbsent() ? "Absent"
                    : r.hasCheckIn() ? (r.hasCheckOut()
                    ? "In: " + r.ciTime + " · Out: " + r.coTime
                    : "In: " + r.ciTime) : "—";
            ((TextView) item.findViewById(R.id.tv_log_meta)).setText(r.empid + cls + dept + " · " + timeInfo);
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

    private LinearLayout makeStat(String label, String value, int colorRes) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(12, 16, 12, 16);
        card.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        card.setLayoutParams(p);

        TextView tvNum = new TextView(getContext());
        tvNum.setText(value);
        tvNum.setTextColor(getResources().getColor(colorRes, null));
        tvNum.setTextSize(26);
        tvNum.setTypeface(null, android.graphics.Typeface.BOLD);
        tvNum.setGravity(Gravity.CENTER);

        TextView tvLbl = new TextView(getContext());
        tvLbl.setText(label);
        tvLbl.setTextColor(getResources().getColor(R.color.muted_light, null));
        tvLbl.setTextSize(10);
        tvLbl.setGravity(Gravity.CENTER);

        card.addView(tvNum);
        card.addView(tvLbl);
        return card;
    }

    // ── STUDENT: personal logs ─────────────────────────────────────────────
    private void loadCache() {
        try {
            String cache = sm.getLogsCache();
            JSONArray arr = new JSONArray(cache);
            dayLogs.clear();
            for (int i = 0; i < arr.length(); i++) {
                AttendanceRecord r = AttendanceRecord.fromJson(arr.getJSONObject(i));
                if (r.isValidDate()) dayLogs.add(r);
            }
        } catch (Exception e) { /* ignore */ }
    }

    private void fetchDayLogs() {
        JSONObject session = sm.getSession();
        if (session == null) return;
        String empid = session.optString("empid", "");
        String url = AppConstants.SCRIPT_URL + "?action=" + AppConstants.ACTION_FETCH + "&empid=" + empid;

        StringRequest req = new StringRequest(com.android.volley.Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        JSONArray records = obj.optJSONArray("records");
                        if (records != null) {
                            dayLogs.clear();
                            for (int i = 0; i < records.length(); i++) {
                                AttendanceRecord r = AttendanceRecord.fromJson(records.getJSONObject(i));
                                if (r.isValidDate()) dayLogs.add(r);
                            }
                            sm.saveLogsCache(records.toString());
                            if (isAdded()) { renderStatus(); renderRecent(); }
                        }
                    } catch (Exception e) { /* ignore */ }
                },
                error -> { /* offline — keep cache */ });

        ApiClient.getInstance(requireContext()).addToRequestQueue(req);
    }

    private AttendanceRecord getTodayRec() {
        String today = DateUtils.todayStr();
        for (AttendanceRecord r : dayLogs) {
            if (today.equals(r.date.trim())) return r;
        }
        return null;
    }

    public void renderStatus() {
        AttendanceRecord rec = getTodayRec();
        if (rec == null || (!rec.hasCheckIn() && !rec.isAbsent())) {
            tvStatusVal.setText("Not Checked In");
            tvStatusVal.setTextColor(getResources().getColor(R.color.muted_light, null));
            tvStatusSub.setText("Tap Attend to check in");
            tvStatusIcon.setText("🕐");
        } else if (rec.isAbsent()) {
            tvStatusVal.setText("❌ Marked Absent");
            tvStatusVal.setTextColor(getResources().getColor(R.color.muted_light, null));
            tvStatusSub.setText("Auto-marked at 9:30 AM");
            tvStatusIcon.setText("❌");
        } else if (rec.hasCheckIn() && !rec.hasCheckOut()) {
            tvStatusVal.setText("✅ Present");
            tvStatusVal.setTextColor(getResources().getColor(R.color.success, null));
            tvStatusSub.setText("Checked in at " + rec.ciTime);
            tvStatusIcon.setText("🟢");
        } else {
            tvStatusVal.setText("🚪 Checked Out");
            tvStatusVal.setTextColor(getResources().getColor(R.color.red, null));
            tvStatusSub.setText("At " + rec.coTime);
            tvStatusIcon.setText("⭕");
        }
    }

    private void renderRecent() {
        logList.removeAllViews();
        List<AttendanceRecord> recent = dayLogs.subList(0, Math.min(5, dayLogs.size()));
        if (recent.isEmpty()) { showEmpty("No recent activity"); return; }
        for (AttendanceRecord r : recent) {
            View item = LayoutInflater.from(getContext()).inflate(R.layout.item_log, logList, false);
            String icon = r.isAbsent() ? "❌" : r.hasCheckOut() ? "🚪" : r.hasCheckIn() ? "✅" : "🕐";
            ((TextView) item.findViewById(R.id.tv_log_icon)).setText(icon);
            ((TextView) item.findViewById(R.id.tv_log_name)).setText(r.name != null && !r.name.isEmpty() ? r.name : r.empid);
            String timeInfo = r.isAbsent() ? "Absent"
                    : r.hasCheckIn() ? (r.hasCheckOut()
                    ? "In: " + r.ciTime + " · Out: " + r.coTime
                    : "In: " + r.ciTime) : "—";
            ((TextView) item.findViewById(R.id.tv_log_meta)).setText(r.date + " · " + timeInfo);
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

    private void showEmpty(String msg) {
        logList.removeAllViews();
        TextView tv = new TextView(getContext());
        tv.setText(msg);
        tv.setTextColor(getResources().getColor(R.color.muted_light, null));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 48, 0, 0);
        logList.addView(tv);
    }
}
