package com.student.attendance.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.toolbox.StringRequest;
import com.student.attendance.R;
import com.student.attendance.network.ApiClient;
import com.student.attendance.utils.AppConstants;
import com.student.attendance.utils.SessionManager;
import org.json.JSONObject;

public class AuthActivity extends AppCompatActivity {

    private View screenLogin, screenFP;

    private EditText etEmpId, etPwd;
    private Button btnLogin;

    private View fp1, fp2, fp3, fp4;
    private EditText etFpEid, etFpOtp, etFpNp, etFpCp;
    private TextView tvFpTimer;
    private Button btnSendOtp, btnVerifyOtp, btnResendOtp, btnResetPwd, btnGoLogin, btnBackLogin;

    private View loaderView;
    private TextView tvLoaderMsg;

    private SessionManager sm;
    private String fpEmpid = "";
    private CountDownTimer fpCountdown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        sm = new SessionManager(this);

        screenLogin = findViewById(R.id.screen_login);
        screenFP    = findViewById(R.id.screen_fp);
        loaderView  = findViewById(R.id.loader);
        tvLoaderMsg = findViewById(R.id.tv_loader_msg);

        initLoginViews();
        initFPViews();
        showScreen(screenLogin);
    }

    private void initLoginViews() {
        etEmpId  = findViewById(R.id.et_empid);
        etPwd    = findViewById(R.id.et_pwd);
        btnLogin = findViewById(R.id.btn_login);

        ImageButton btnEye = findViewById(R.id.btn_eye_login);
        btnEye.setOnClickListener(v -> togglePasswordVisibility(etPwd, btnEye));

        TextView tvForgot = findViewById(R.id.tv_forgot);
        tvForgot.setOnClickListener(v -> showFP());

        btnLogin.setOnClickListener(v -> doLogin());
    }

    private void doLogin() {
        String empid = etEmpId.getText().toString().trim().toUpperCase();
        String pwd   = etPwd.getText().toString();
        if (empid.isEmpty()) { toast("Enter your Register Number"); return; }
        if (pwd.isEmpty())   { toast("Enter your password"); return; }

        showLoader("Signing in...");
        String url = AppConstants.SCRIPT_URL + "?action=" + AppConstants.ACTION_LOGIN
                + "&empid=" + empid + "&pwd=" + encode(pwd);

        StringRequest req = new StringRequest(com.android.volley.Request.Method.GET, url,
                response -> {
                    hideLoader();
                    try {
                        JSONObject obj = new JSONObject(response);
                        if ("success".equals(obj.optString("status"))) {
                            JSONObject emp    = obj.optJSONObject("employee");
                            String name       = emp != null ? emp.optString("name", empid) : empid;
                            String dept       = emp != null ? emp.optString("dept", "") : "";
                            String email      = emp != null ? emp.optString("email", "") : "";
                            String mobile     = emp != null ? emp.optString("mobile", "") : "";
                            String classSec   = emp != null ? emp.optString("classSection", "") : "";
                            boolean isTeacher = empid.startsWith(AppConstants.TEACHER_ID_PREFIX);
                            sm.saveSession(empid, name, dept, email, mobile, classSec, isTeacher);
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else if ("wrong_password".equals(obj.optString("status"))) {
                            toast("Wrong password");
                        } else if ("not_found".equals(obj.optString("status"))) {
                            toast("Register number not found");
                        } else {
                            toast("Login failed");
                        }
                    } catch (Exception e) { toast("Response error"); }
                },
                error -> { hideLoader(); toast("Cannot connect to server"); });

        ApiClient.getInstance(this).addToRequestQueue(req);
    }

    private void initFPViews() {
        fp1 = findViewById(R.id.fp_step1);
        fp2 = findViewById(R.id.fp_step2);
        fp3 = findViewById(R.id.fp_step3);
        fp4 = findViewById(R.id.fp_step4);

        etFpEid     = findViewById(R.id.et_fp_eid);
        etFpOtp     = findViewById(R.id.et_fp_otp);
        etFpNp      = findViewById(R.id.et_fp_np);
        etFpCp      = findViewById(R.id.et_fp_cp);
        tvFpTimer   = findViewById(R.id.tv_fp_timer);

        btnSendOtp   = findViewById(R.id.btn_send_otp);
        btnVerifyOtp = findViewById(R.id.btn_verify_otp);
        btnResendOtp = findViewById(R.id.btn_resend_otp);
        btnResetPwd  = findViewById(R.id.btn_reset_pwd);
        btnGoLogin   = findViewById(R.id.btn_go_login);
        btnBackLogin = findViewById(R.id.btn_back_login);

        ImageButton eyeNp = findViewById(R.id.btn_eye_np);
        ImageButton eyeCp = findViewById(R.id.btn_eye_cp);
        eyeNp.setOnClickListener(v -> togglePasswordVisibility(etFpNp, eyeNp));
        eyeCp.setOnClickListener(v -> togglePasswordVisibility(etFpCp, eyeCp));

        btnSendOtp.setOnClickListener(v -> fpSendOtp(false));
        btnVerifyOtp.setOnClickListener(v -> fpVerifyOtp());
        btnResendOtp.setOnClickListener(v -> fpSendOtp(true));
        btnResetPwd.setOnClickListener(v -> fpReset());
        btnGoLogin.setOnClickListener(v -> goLogin());
        btnBackLogin.setOnClickListener(v -> goLogin());
    }

    private void showFP() {
        fpEmpid = "";
        etFpEid.setText(""); etFpOtp.setText(""); etFpNp.setText(""); etFpCp.setText("");
        if (fpCountdown != null) { fpCountdown.cancel(); fpCountdown = null; }
        goFpStep(1);
        showScreen(screenFP);
    }

    private void goLogin() {
        if (fpCountdown != null) { fpCountdown.cancel(); fpCountdown = null; }
        showScreen(screenLogin);
    }

    private void goFpStep(int step) {
        fp1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        fp2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        fp3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        fp4.setVisibility(step == 4 ? View.VISIBLE : View.GONE);
    }

    private void startFpTimer() {
        if (fpCountdown != null) fpCountdown.cancel();
        fpCountdown = new CountDownTimer(10 * 60 * 1000, 1000) {
            public void onTick(long ms) {
                long m = ms / 60000, s = (ms % 60000) / 1000;
                tvFpTimer.setText(m + ":" + (s < 10 ? "0" : "") + s);
            }
            public void onFinish() { tvFpTimer.setText("Expired"); }
        }.start();
    }

    private void fpSendOtp(boolean isResend) {
        String empid = etFpEid.getText().toString().trim().toUpperCase();
        if (empid.isEmpty()) { toast("Enter your Register Number"); return; }
        showLoader("Sending OTP...");
        String url = AppConstants.SCRIPT_URL + "?action=" + AppConstants.ACTION_SEND_OTP + "&empid=" + empid;
        StringRequest req = new StringRequest(com.android.volley.Request.Method.GET, url,
                response -> {
                    hideLoader();
                    try {
                        JSONObject obj = new JSONObject(response);
                        if ("success".equals(obj.optString("status"))) {
                            fpEmpid = empid;
                            etFpOtp.setText("");
                            tvFpTimer.setText("10:00");
                            startFpTimer();
                            goFpStep(2);
                            toast(isResend ? "OTP resent!" : "OTP sent to teacher!");
                        } else if ("not_found".equals(obj.optString("status"))) {
                            toast("Register number not found");
                        } else {
                            toast(obj.optString("message", "Failed to send OTP"));
                        }
                    } catch (Exception e) { toast("Response error"); }
                },
                error -> { hideLoader(); toast("Cannot connect"); });
        ApiClient.getInstance(this).addToRequestQueue(req);
    }

    private void fpVerifyOtp() {
        String otp = etFpOtp.getText().toString().trim();
        if (otp.length() != 6) { toast("Enter the 6-digit OTP"); return; }
        if (fpEmpid.isEmpty()) { toast("Session expired"); return; }
        showLoader("Verifying...");
        String url = AppConstants.SCRIPT_URL + "?action=" + AppConstants.ACTION_VERIFY_OTP
                + "&empid=" + fpEmpid + "&otp=" + otp;
        StringRequest req = new StringRequest(com.android.volley.Request.Method.GET, url,
                response -> {
                    hideLoader();
                    try {
                        JSONObject obj = new JSONObject(response);
                        String status = obj.optString("status");
                        if ("success".equals(status)) {
                            if (fpCountdown != null) { fpCountdown.cancel(); fpCountdown = null; }
                            goFpStep(3); toast("OTP verified!");
                        } else if ("expired".equals(status)) {
                            toast("OTP expired! Resend.");
                        } else {
                            toast("Incorrect OTP");
                        }
                    } catch (Exception e) { toast("Response error"); }
                },
                error -> { hideLoader(); toast("Cannot connect"); });
        ApiClient.getInstance(this).addToRequestQueue(req);
    }

    private void fpReset() {
        String np = etFpNp.getText().toString();
        String cp = etFpCp.getText().toString();
        if (np.isEmpty() || cp.isEmpty()) { toast("Fill both fields"); return; }
        if (!np.equals(cp)) { toast("Passwords do not match"); return; }
        if (np.length() < 4) { toast("Minimum 4 characters"); return; }
        if (fpEmpid.isEmpty()) { toast("Session expired"); return; }
        showLoader("Updating...");
        String url = AppConstants.SCRIPT_URL + "?action=" + AppConstants.ACTION_RESET_PASSWORD
                + "&empid=" + fpEmpid + "&newpwd=" + encode(np);
        StringRequest req = new StringRequest(com.android.volley.Request.Method.GET, url,
                response -> {
                    hideLoader();
                    try {
                        JSONObject obj = new JSONObject(response);
                        if ("success".equals(obj.optString("status"))) {
                            fpEmpid = ""; goFpStep(4);
                        } else {
                            toast("Failed to update password");
                        }
                    } catch (Exception e) { toast("Response error"); }
                },
                error -> { hideLoader(); toast("Cannot connect"); });
        ApiClient.getInstance(this).addToRequestQueue(req);
    }

    private void showScreen(View screen) {
        screenLogin.setVisibility(View.GONE);
        screenFP.setVisibility(View.GONE);
        screen.setVisibility(View.VISIBLE);
    }

    private void togglePasswordVisibility(EditText et, ImageButton btn) {
        if (et.getTransformationMethod() instanceof PasswordTransformationMethod) {
            et.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            btn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            et.setTransformationMethod(PasswordTransformationMethod.getInstance());
            btn.setImageResource(android.R.drawable.ic_menu_view);
        }
        et.setSelection(et.getText().length());
    }

    private void showLoader(String msg) { tvLoaderMsg.setText(msg); loaderView.setVisibility(View.VISIBLE); }
    private void hideLoader()           { loaderView.setVisibility(View.GONE); }
    private void toast(String msg)      { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
    private String encode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }
}
