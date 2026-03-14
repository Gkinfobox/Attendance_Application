package com.student.attendance.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.student.attendance.R;
import com.student.attendance.fragments.*;
import com.student.attendance.utils.AppConstants;
import com.student.attendance.utils.SessionManager;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private LinearLayout navHome, navAttend, navSummary, navTeacher;
    private TextView tvNavHome, tvNavAttend, tvNavSummary, tvNavTeacher;

    private SessionManager sm;
    private boolean isTeacher = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sm = new SessionManager(this);
        JSONObject session = sm.getSession();
        if (session == null) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        isTeacher = session.optString("empid", "").startsWith(AppConstants.TEACHER_ID_PREFIX);

        initBottomNav();

        if (isTeacher) {
            navAttend.setVisibility(View.GONE);
            navSummary.setVisibility(View.GONE);
            navTeacher.setVisibility(View.VISIBLE);
        } else {
            navTeacher.setVisibility(View.GONE);
        }

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), false);
            setNavActive(navHome, tvNavHome);
        }
    }

    private void initBottomNav() {
        navHome    = findViewById(R.id.nav_home);
        navAttend  = findViewById(R.id.nav_attend);
        navSummary = findViewById(R.id.nav_summary);
        navTeacher = findViewById(R.id.nav_admin);

        tvNavHome    = findViewById(R.id.tv_nav_home);
        tvNavAttend  = findViewById(R.id.tv_nav_attend);
        tvNavSummary = findViewById(R.id.tv_nav_summary);
        tvNavTeacher = findViewById(R.id.tv_nav_admin);

        navHome.setOnClickListener(v -> {
            loadFragment(new HomeFragment(), false);
            setNavActive(navHome, tvNavHome);
        });
        navAttend.setOnClickListener(v -> {
            loadFragment(new AttendanceFragment(), false);
            setNavActive(navAttend, tvNavAttend);
        });
        navSummary.setOnClickListener(v -> {
            loadFragment(new SummaryFragment(), false);
            setNavActive(navSummary, tvNavSummary);
        });
        navTeacher.setOnClickListener(v -> {
            loadFragment(new TeacherFragment(), false);
            setNavActive(navTeacher, tvNavTeacher);
        });
    }

    public void loadFragment(Fragment fragment, boolean addToBack) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        ft.replace(R.id.fragment_container, fragment);
        if (addToBack) ft.addToBackStack(null);
        ft.commit();
    }

    public void navigateTo(String screen) {
        switch (screen) {
            case "home":
                loadFragment(new HomeFragment(), false);
                setNavActive(navHome, tvNavHome);
                break;
            case "attend":
                loadFragment(new AttendanceFragment(), false);
                setNavActive(navAttend, tvNavAttend);
                break;
            case "summary":
                loadFragment(new SummaryFragment(), false);
                setNavActive(navSummary, tvNavSummary);
                break;
            case "teacher":
                loadFragment(new TeacherFragment(), false);
                setNavActive(navTeacher, tvNavTeacher);
                break;
        }
    }

    private void setNavActive(LinearLayout activeNav, TextView activeTv) {
        int red   = getResources().getColor(R.color.red, null);
        int muted = getResources().getColor(R.color.muted_light, null);
        for (TextView tv : new TextView[]{tvNavHome, tvNavAttend, tvNavSummary, tvNavTeacher}) {
            if (tv != null) tv.setTextColor(muted);
        }
        activeTv.setTextColor(red);
    }

    public void doLogout() {
        sm.clearSession();
        startActivity(new Intent(this, AuthActivity.class));
        finish();
    }
}
