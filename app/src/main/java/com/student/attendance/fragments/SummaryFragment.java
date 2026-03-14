package com.student.attendance.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.android.volley.toolbox.StringRequest;
import com.student.attendance.R;
import com.student.attendance.models.AttendanceRecord;
import com.student.attendance.network.ApiClient;
import com.student.attendance.utils.AppConstants;
import com.student.attendance.utils.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class SummaryFragment extends Fragment {

    private TextView tvSumPresent, tvSumAbsent, tvSumOut;
    private Button btnFilterAll, btnFilterPresent, btnFilterAbsent;
    private LinearLayout logList;

    private SessionManager sm;
    private List<AttendanceRecord> allLogs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_summary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sm = new SessionManager(requireContext());

        tvSumPresent    = view.findViewById(R.id.tv_sum_in);
        tvSumAbsent     = view.findViewById(R.id.tv_sum_absent);
        tvSumOut        = view.findViewById(R.id.tv_sum_out);
        btnFilterAll    = view.findViewById(R.id.btn_filter_all);
        btnFilterPresent= view.findViewById(R.id.btn_filter_in);
        btnFilterAbsent = view.findViewById(R.id.btn_filter_out);
        logList         = view.findViewById(R.id.sum_log_list);

        btnFilterAll.setOnClickListener(v    -> { filterLogs("all");     setActiveFilter(btnFilterAll); });
        btnFilterPresent.setOnClickListener(v -> { filterLogs("present"); setActiveFilter(btnFilterPresent); });
        btnFilterAbsent.setOnClickListener(v  -> { filterLogs("absent");  setActiveFilter(btnFilterAbsent); });

        setActiveFilter(btnFilterAll);
        showLoading();
        loadSummary();
    }

    private void loadSummary() {
        JSONObject session = sm.getSession();
        if (session == null) return;
        String empid = session.optString("empid", "");
        String url = AppConstants.SCRIPT_URL + "?action=" + AppConstants.ACTION_FETCH + "&empid=" + empid;

        StringRequest req = new StringRequest(com.android.volley.Request.Method.GET, url,
                response -> {
                    if (!isAdded()) return;
                    try {
                        JSONObject obj = new JSONObject(response);
                        JSONArray records = obj.optJSONArray("records");
                        allLogs.clear();
                        if (records != null) {
                            for (int i = 0; i < records.length(); i++) {
                                AttendanceRecord r = AttendanceRecord.fromJson(records.getJSONObject(i));
                                if (r.isValidDate()) allLogs.add(r);
                            }
                        }
                        updateStats();
                        renderLogs(allLogs);
                    } catch (Exception e) { showEmpty("Cannot load records"); }
                },
                error -> {
                    if (!isAdded()) return;
                    try {
                        String cache = sm.getLogsCache();
                        JSONArray arr = new JSONArray(cache);
                        allLogs.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            AttendanceRecord r = AttendanceRecord.fromJson(arr.getJSONObject(i));
                            if (r.isValidDate()) allLogs.add(r);
                        }
                        updateStats();
                        if (allLogs.isEmpty()) showEmpty("No records found");
                        else renderLogs(allLogs);
                    } catch (Exception e) { showEmpty("Cannot connect"); }
                });

        ApiClient.getInstance(requireContext()).addToRequestQueue(req);
    }

    private void updateStats() {
        int present = 0, absent = 0, out = 0;
        for (AttendanceRecord r : allLogs) {
            if (r.isAbsent()) absent++;
            else if (r.hasCheckOut()) out++;
            else if (r.hasCheckIn()) present++;
        }
        tvSumPresent.setText(String.valueOf(present));
        tvSumAbsent.setText(String.valueOf(absent));
        tvSumOut.setText(String.valueOf(out));
    }

    private void filterLogs(String type) {
        List<AttendanceRecord> filtered = new ArrayList<>();
        for (AttendanceRecord r : allLogs) {
            if ("all".equals(type)) filtered.add(r);
            else if ("present".equals(type) && r.hasCheckIn() && !r.isAbsent()) filtered.add(r);
            else if ("absent".equals(type)  && r.isAbsent()) filtered.add(r);
        }
        renderLogs(filtered);
    }

    private void renderLogs(List<AttendanceRecord> logs) {
        logList.removeAllViews();
        if (logs.isEmpty()) { showEmpty("No records found"); return; }
        for (AttendanceRecord r : logs) {
            View item = LayoutInflater.from(getContext()).inflate(R.layout.item_log, logList, false);
            String icon = r.isAbsent() ? "❌" : r.hasCheckOut() ? "🚪" : r.hasCheckIn() ? "✅" : "🕐";
            ((TextView) item.findViewById(R.id.tv_log_icon)).setText(icon);
            String displayName = (r.name != null && !r.name.isEmpty()) ? r.name : r.empid;
            ((TextView) item.findViewById(R.id.tv_log_name)).setText(displayName);
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

    private void showLoading() {
        logList.removeAllViews();
        TextView tv = new TextView(getContext());
        tv.setText("⏳ Loading...");
        tv.setTextColor(getResources().getColor(R.color.muted_light, null));
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, 80, 0, 0);
        logList.addView(tv);
    }

    private void showEmpty(String msg) {
        logList.removeAllViews();
        TextView tv = new TextView(getContext());
        tv.setText(msg);
        tv.setTextColor(getResources().getColor(R.color.muted_light, null));
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, 80, 0, 0);
        logList.addView(tv);
    }

    private void setActiveFilter(Button active) {
        int white = getResources().getColor(R.color.white, null);
        int muted = getResources().getColor(R.color.muted_light, null);
        for (Button b : new Button[]{btnFilterAll, btnFilterPresent, btnFilterAbsent}) {
            b.setBackgroundResource(R.drawable.bg_card);
            b.setTextColor(muted);
        }
        active.setBackgroundResource(R.drawable.bg_btn_red);
        active.setTextColor(white);
    }
}
