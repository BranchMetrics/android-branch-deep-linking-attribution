package io.branch.branchandroiddemo.activities;

import  io.branch.branchandroiddemo.BranchWrapper;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import io.branch.branchandroiddemo.BranchWrapper;
import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.BranchLogger;
import io.branch.referral.util.LinkProperties;
import io.branch.branchandroiddemo.Common;
import io.branch.branchandroiddemo.Constants;
import io.branch.branchandroiddemo.R;
import io.branch.branchandroiddemo.TestData;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button  btCreateDeepLink, btNativeShare, btTrackUser,
             btCreateQrCode, btSetDMAParams, btLogEvent, btReadLogs, btInitSession, btSetAttributionLevel;
    private TextView tvMessage, tvUrl;
    ToggleButton trackingCntrlBtn;

    BranchWrapper branchWrapper;
    protected void initialize(){
        btCreateDeepLink = findViewById(R.id.bt_create_deep_link);
        btNativeShare = findViewById(R.id.bt_native_share);
        tvMessage = findViewById(R.id.tv_message);
        tvUrl = findViewById(R.id.tv_url);
        btCreateQrCode = findViewById(R.id.bt_create_qr_code);
        btSetDMAParams = findViewById(R.id.bt_set_dma_params);
        btLogEvent = findViewById(R.id.bt_logEvent_with_callback);
        btReadLogs = findViewById(R.id.bt_Read_Logs);
        btTrackUser = findViewById(R.id.bt_track_user);
        btInitSession = findViewById(R.id.bt_init_session);
        trackingCntrlBtn = findViewById(R.id.tracking_cntrl_btn);
        btSetAttributionLevel = findViewById(R.id.bt_set_attribution_level);

        btCreateDeepLink.setOnClickListener(this);
        btNativeShare.setOnClickListener(this);
        btCreateQrCode.setOnClickListener(this);
        btSetDMAParams.setOnClickListener(this);
        btLogEvent.setOnClickListener(this);
        btReadLogs.setOnClickListener(this);
        btTrackUser.setOnClickListener(this);
        btInitSession.setOnClickListener(this);
        btSetAttributionLevel.setOnClickListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Common.getInstance().clearLog();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();

        branchWrapper = new BranchWrapper(getApplicationContext());
        branchWrapper.delayInitializationIfRequired(getIntent());

        Branch.enableLogging(BranchLogger.BranchLogLevel.VERBOSE);

        trackingCntrlBtn.setChecked(Branch.getInstance().isTrackingDisabled());
        trackingCntrlBtn.setOnCheckedChangeListener((buttonView, isChecked) -> Branch.getInstance().disableTracking(isChecked));
    }

    @Override
    public void onClick(View view) {
        
        if (view == btCreateDeepLink) {
            branchWrapper.createDeepLink(getIntent(), MainActivity.this);
        } else if (view == btNativeShare) {
            branchWrapper.nativeShare(this, getIntent(), MainActivity.this);
        } else if (view == btLogEvent) {
            branchWrapper.logEvent(getIntent(), MainActivity.this);
        } else if (view == btTrackUser) {
            branchWrapper.setIdentity(getIntent(), MainActivity.this);
        } else if (view == btSetDMAParams) {
            branchWrapper.setDMAParams(getIntent());
        } else if (view == btInitSession) {
            branchWrapper.initSession(this, getIntent(), MainActivity.this);
        } else if (view == btReadLogs) {
            branchWrapper.showLogWindow("",false, this, Constants.LOG_DATA);
        } else if (view == btCreateQrCode) {
            branchWrapper.getQRCode(this, getIntent(), MainActivity.this);
        } else if (view == btSetAttributionLevel) {
            branchWrapper.setAttributionLevel(getIntent());
        }else {
            branchWrapper.showLogWindow("",false, this, Constants.UNKNOWN);
        }
    }
}