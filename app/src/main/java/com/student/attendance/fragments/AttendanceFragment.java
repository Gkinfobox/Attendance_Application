package com.student.attendance.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import com.android.volley.toolbox.StringRequest;
import com.student.attendance.R;
import com.student.attendance.activities.MainActivity;
import com.student.attendance.network.ApiClient;
import com.student.attendance.utils.AppConstants;
import com.student.attendance.utils.DateUtils;
import com.student.attendance.utils.SessionManager;
import com.google.android.gms.location.*;
import org.json.JSONObject;

public class AttendanceFragment extends Fragment {

    private View formCi, formCo;
    private Button btnTabCi, btnTabCo;
    private Button btnGetLocCi, btnGetLocCo;
    private Button btnCheckin, btnCheckout;
    private TextView tvLocStatusCi, tvLocCoordsCi;
    private TextView tvLocStatusCo, tvLocCoordsCo;
    private TextView tvCiRegNo, tvCiName, tvCiClass;
    private TextView tvCoRegNo, tvCoName, tvCoClass;
    private View loaderView;
    private TextView tvLoaderMsg;
    // Auto-absent warning banner
    private TextView tvAbsentWarning;

    private SessionManager sm;
    private FusedLocationProviderClient locationClient;
    private double ciLat, ciLng, ciAcc;
    private double coLat, coLng, coAcc;
    private boolean hasCiLoc = false, hasCoLoc = false;

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_attendance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sm = new SessionManager(requireContext());
        locationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        loaderView      = view.findViewById(R.id.loader);
        tvLoaderMsg     = view.findViewById(R.id.tv_loader_msg);
        tvAbsentWarning = view.findViewById(R.id.tv_absent_warning);

        formCi   = view.findViewById(R.id.form_ci);
        formCo   = view.findViewById(R.id.form_co);
        btnTabCi = view.findViewById(R.id.btn_tab_ci);
        btnTabCo = view.findViewById(R.id.btn_tab_co);

        tvCiRegNo = view.findViewById(R.id.tv_ci_empid);
        tvCiName  = view.findViewById(R.id.tv_ci_name);
        tvCiClass = view.findViewById(R.id.tv_ci_dept);
        tvCoRegNo = view.findViewById(R.id.tv_co_empid);
        tvCoName  = view.findViewById(R.id.tv_co_name);
        tvCoClass = view.findViewById(R.id.tv_co_dept);

        tvLocStatusCi = view.findViewById(R.id.tv_loc_status_ci);
        tvLocCoordsCi = view.findViewById(R.id.tv_loc_coords_ci);
        tvLocStatusCo = view.findViewById(R.id.tv_loc_status_co);
        tvLocCoordsCo = view.findViewById(R.id.tv_loc_coords_co);

        btnGetLocCi = view.findViewById(R.id.btn_get_loc_ci);
        btnGetLocCo = view.findViewById(R.id.btn_get_loc_co);
        btnCheckin  = view.findViewById(R.id.btn_checkin);
        btnCheckout = view.findViewById(R.id.btn_checkout);

        JSONObject s = sm.getSession();
        if (s != null) {
            String regNo = s.optString("empid", "");
            String name  = s.optString("name", "");
            String dept  = s.optString("dept", "");
            String cls   = s.optString("classSection", "");
            String display = cls.isEmpty() ? dept : dept + (dept.isEmpty() ? "" : " · ") + cls;
            tvCiRegNo.setText(regNo); tvCiName.setText(name); tvCiClass.setText(display);
            tvCoRegNo.setText(regNo); tvCoName.setText(name); tvCoClass.setText(display);
        }

        // Show warning if past 9:30 AM
        if (DateUtils.isPastAbsentTime()) {
            tvAbsentWarning.setVisibility(View.VISIBLE);
            tvAbsentWarning.setText("⚠️ Past 9:30 AM — you may be marked absent if not checked in");
        } else {
            tvAbsentWarning.setVisibility(View.GONE);
        }

        btnTabCi.setOnClickListener(v -> switchTab("ci"));
        btnTabCo.setOnClickListener(v -> switchTab("co"));
        btnGetLocCi.setOnClickListener(v -> getGPS("ci"));
        btnGetLocCo.setOnClickListener(v -> getGPS("co"));
        btnCheckin.setOnClickListener(v -> doCheckin());
        btnCheckout.setOnClickListener(v -> doCheckout());

        switchTab("ci");
    }

    private void switchTab(String tab) {
        boolean isCi = "ci".equals(tab);
        formCi.setVisibility(isCi ? View.VISIBLE : View.GONE);
        formCo.setVisibility(isCi ? View.GONE : View.VISIBLE);

        int white = getResources().getColor(R.color.white, null);
        int muted = getResources().getColor(R.color.muted_light, null);

        if (isCi) {
            btnTabCi.setBackgroundResource(R.drawable.bg_btn_red); btnTabCi.setTextColor(white);
            btnTabCo.setBackgroundResource(android.R.color.transparent); btnTabCo.setTextColor(muted);
        } else {
            btnTabCo.setBackgroundResource(R.drawable.bg_btn_red); btnTabCo.setTextColor(white);
            btnTabCi.setBackgroundResource(android.R.color.transparent); btnTabCi.setTextColor(muted);
        }
    }

    private void getGPS(String type) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        TextView tvStatus = "ci".equals(type) ? tvLocStatusCi : tvLocStatusCo;
        TextView tvCoords = "ci".equals(type) ? tvLocCoordsCi : tvLocCoordsCo;
        tvStatus.setText("Fetching...");
        tvCoords.setText("⏳");

        LocationRequest locReq = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(1)
                .setInterval(0);

        locationClient.requestLocationUpdates(locReq, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                locationClient.removeLocationUpdates(this);
                if (!isAdded()) return;
                android.location.Location loc = result.getLastLocation();
                if (loc == null) { tvStatus.setText("Failed"); tvCoords.setText("⚠️"); return; }
                double lat = loc.getLatitude();
                double lng = loc.getLongitude();
                float  acc = loc.getAccuracy();
                if ("ci".equals(type)) { ciLat = lat; ciLng = lng; ciAcc = acc; hasCiLoc = true; }
                else                   { coLat = lat; coLng = lng; coAcc = acc; hasCoLoc = true; }
                tvStatus.setText("📍 Captured (±" + (int) acc + "m)");
                tvCoords.setText(String.format("%.6f, %.6f", lat, lng));
            }
        }, android.os.Looper.getMainLooper());
    }

    private void doCheckin() {
        if (!hasCiLoc) { toast("Capture your location first"); return; }
        JSONObject s = sm.getSession();
        if (s == null) { toast("Please login first"); return; }

        showLoader("Checking in...");
        String url = AppConstants.SCRIPT_URL + "?action=" + AppConstants.ACTION_CHECKIN
                + "&empid="   + s.optString("empid")
                + "&name="    + encode(s.optString("name"))
                + "&dept="    + encode(s.optString("dept"))
                + "&classSection=" + encode(s.optString("classSection"))
                + "&lat="     + ciLat
                + "&lng="     + ciLng
                + "&accuracy=" + (int) ciAcc;

        StringRequest req = new StringRequest(com.android.volley.Request.Method.GET, url,
                response -> {
                    hideLoader();
                    try {
                        JSONObject obj = new JSONObject(response);
                        String status = obj.optString("status");
                        if ("success".equals(status)) {
                            hasCiLoc = false;
                            tvLocStatusCi.setText("Location not captured");
                            tvLocCoordsCi.setText("—");
                            toast("✅ Checked In at " + obj.optString("time"));
                            if (getActivity() instanceof MainActivity)
                                ((MainActivity) getActivity()).navigateTo("home");
                        } else if ("already_checkedin".equals(status)) {
                            toast("Already checked in today!");
                        } else {
                            toast("Error: " + obj.optString("message", "Unknown error"));
                        }
                    } catch (Exception e) { toast("Response error"); }
                },
                error -> {
                    hideLoader();
                    hasCiLoc = false;
                    toast("Checked in (offline)");
                    if (getActivity() instanceof MainActivity)
                        ((MainActivity) getActivity()).navigateTo("home");
                });

        ApiClient.getInstance(requireContext()).addToRequestQueue(req);
    }

    private void doCheckout() {
        if (!hasCoLoc) { toast("Capture your location first"); return; }
        JSONObject s = sm.getSession();
        if (s == null) { toast("Please login first"); return; }

        showLoader("Checking out...");
        String url = AppConstants.SCRIPT_URL + "?action=" + AppConstants.ACTION_CHECKOUT
                + "&empid="    + s.optString("empid")
                + "&date="     + DateUtils.todayStr()
                + "&lat="      + coLat
                + "&lng="      + coLng
                + "&accuracy=" + (int) coAcc;

        StringRequest req = new StringRequest(com.android.volley.Request.Method.GET, url,
                response -> {
                    hideLoader();
                    try {
                        JSONObject obj = new JSONObject(response);
                        String status = obj.optString("status");
                        if ("success".equals(status)) {
                            hasCoLoc = false;
                            tvLocStatusCo.setText("Location not captured");
                            tvLocCoordsCo.setText("—");
                            String wh = obj.optString("workhours", "");
                            toast("🚪 Checked Out!" + (wh.isEmpty() ? "" : " ⏱ " + wh));
                            if (getActivity() instanceof MainActivity)
                                ((MainActivity) getActivity()).navigateTo("home");
                        } else if ("no_checkin".equals(status)) {
                            toast("No check-in found for today");
                        } else {
                            toast("Error: " + obj.optString("message", "Unknown error"));
                        }
                    } catch (Exception e) { toast("Response error"); }
                },
                error -> {
                    hideLoader();
                    hasCoLoc = false;
                    toast("Checked out (offline)");
                    if (getActivity() instanceof MainActivity)
                        ((MainActivity) getActivity()).navigateTo("home");
                });

        ApiClient.getInstance(requireContext()).addToRequestQueue(req);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                toast("Location permission granted. Tap Get Location again.");
            else
                toast("Location permission denied");
        }
    }

    private void showLoader(String msg) { tvLoaderMsg.setText(msg); loaderView.setVisibility(View.VISIBLE); }
    private void hideLoader()           { loaderView.setVisibility(View.GONE); }
    private void toast(String msg)      { Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show(); }
    private String encode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; }
    }
}
