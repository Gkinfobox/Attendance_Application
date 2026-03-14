package com.student.attendance.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.volley.toolbox.StringRequest;
import com.student.attendance.network.ApiClient;

public class AbsentMarkReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String today = DateUtils.todayStr();
        // Trigger server-side absent marking for students who haven't checked in
        String url = AppConstants.SCRIPT_URL
                + "?action=" + AppConstants.ACTION_MARK_ABSENT
                + "&date=" + today;

        StringRequest req = new StringRequest(
                com.android.volley.Request.Method.GET, url,
                response -> { /* silent */ },
                error -> { /* silent */ });

        ApiClient.getInstance(context).addToRequestQueue(req);
    }
}
