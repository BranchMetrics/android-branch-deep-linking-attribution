package io.branch.saas.sdk.testbed.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import io.branch.saas.sdk.testbed.Common;
import io.branch.saas.sdk.testbed.Constants;
import io.branch.saas.sdk.testbed.R;

public class LogDataActivity extends AppCompatActivity {

    private String clickType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_data);
        clickType = getIntent().getStringExtra(Constants.TYPE);
        findViewById(R.id.bt_submit).setOnClickListener(view -> {
            if (!clickType.equals(Constants.TRACK_USER) &&
                    !clickType.equals(Constants.CREATE_SEND_NOTIFICATION) &&
                    !clickType.equals(Constants.LOG_DATA)) {
                Intent intent;
                switch (clickType) {
                    case Constants.CREATE_SEND_READ_DEEP_LINK:
                        intent = new Intent(LogDataActivity.this, UrlPreviewActivity.class);
                        break;
                    case Constants.HANDLE_LINKS:
                        intent = new Intent(LogDataActivity.this, ReadDeepLinkActivity.class);
                        intent.putExtra(Constants.TYPE, clickType);
                        intent.putExtra(Constants.FORCE_NEW_SESSION, true);
                        break;
                    case Constants.AFTER_READ_DEEP_LINK:
                    case Constants.SET_DMA_Params:
                        intent = new Intent(LogDataActivity.this, MainActivity.class);
                        break;
                    case Constants.TRACK_CONTENT_DATA:
                        intent = new Intent(LogDataActivity.this, GenerateUrlActivity.class);
                        intent.putExtra(Constants.TYPE, Constants.TRACK_CONTENT);
                        break;
                    default:
                        intent = new Intent(LogDataActivity.this, ReadDeepLinkActivity.class);
                        intent.putExtra(Constants.TYPE, clickType);
                        break;
                }
                intent.putExtra(Constants.ANDROID_URL, getIntent().getStringExtra(Constants.ANDROID_URL));
                startActivity(intent);
            }
            finish();
        });
        TextView tvSuccessFailTitle = findViewById(R.id.tv_success_fail_title);
        TextView tvLogData = findViewById(R.id.tv_log_data);
        String status = getIntent().getStringExtra(Constants.STATUS);
        tvSuccessFailTitle.setText(getIntent().getStringExtra(Constants.STATUS));
        String message = getIntent().getStringExtra(Constants.MESSAGE);
        if (status.equals(Constants.SUCCESS)) {
            String logData = Common.getInstance().readLogData();
            tvLogData.setVisibility(View.VISIBLE);
            Log.d("LogData", "" + logData);
            if (!TextUtils.isEmpty(message)) {
                tvLogData.setText(message + "\n\n" + logData);
            } else {
                tvLogData.setText(logData);
            }
        } else {
            if (!TextUtils.isEmpty(message)) {
                tvLogData.setVisibility(View.VISIBLE);
                tvLogData.setText(message);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (clickType.equals(Constants.AFTER_READ_DEEP_LINK)) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        super.onBackPressed();
    }
}