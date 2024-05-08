package io.branch.branchandroiddemo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import io.branch.branchandroiddemo.Common;
import io.branch.branchandroiddemo.Constants;
import io.branch.branchandroiddemo.R;

public class LogDataActivity extends AppCompatActivity {

    private String clickType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_data);

        String clickType = getIntent().getStringExtra(Constants.TYPE);
        String message = getIntent().getStringExtra(Constants.MESSAGE);
        String logData = "";

        if((message != null) && ((message.isEmpty() == false) || (message.trim().isEmpty() == false))){
            logData = message;
        } else if (clickType == null || clickType.isEmpty() || clickType.trim().isEmpty()) {
            logData = "Unknown Button clicked";
        } else if (clickType.equalsIgnoreCase(Constants.UNKNOWN )){
            logData = "This feature is not yet implemented.";
        } else {
            logData = Common.getInstance().readLogData();
            Log.d("LogData", "" + logData);
        }

        TextView tvLogData = findViewById(R.id.tv_log_data);
        tvLogData.setText(logData);

        findViewById(R.id.bt_submit).setOnClickListener(view -> {
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}