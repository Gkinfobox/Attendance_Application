package com.student.attendance.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.student.attendance.R;
import com.student.attendance.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            SessionManager sm = new SessionManager(this);
            Intent intent = sm.isLoggedIn()
                    ? new Intent(this, MainActivity.class)
                    : new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
        }, 2000);
    }
}
