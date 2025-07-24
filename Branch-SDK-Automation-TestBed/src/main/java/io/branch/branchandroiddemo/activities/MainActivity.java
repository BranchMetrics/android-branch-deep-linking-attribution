package io.branch.branchandroiddemo.activities;

import  io.branch.branchandroiddemo.BranchWrapper;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import io.branch.referral.Branch;
import io.branch.referral.BranchLogger;
import io.branch.branchandroiddemo.Common;
import io.branch.branchandroiddemo.Constants;
import io.branch.branchandroiddemo.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button  btCreateDeepLink, btNativeShare, btTrackUser,
             btCreateQrCode, btSetDMAParams, btLogEvent, btReadLogs, btInitSession;
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

        btCreateDeepLink.setOnClickListener(this);
        btNativeShare.setOnClickListener(this);
        btCreateQrCode.setOnClickListener(this);
        btSetDMAParams.setOnClickListener(this);
        btLogEvent.setOnClickListener(this);
        btReadLogs.setOnClickListener(this);
        btTrackUser.setOnClickListener(this);
        btInitSession.setOnClickListener(this);
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

        trackingCntrlBtn.setChecked(Branch.init().isTrackingDisabled());
        trackingCntrlBtn.setOnCheckedChangeListener((buttonView, isChecked) -> Branch.init().disableTracking(isChecked));
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
        } else {
            branchWrapper.showLogWindow("",false, this, Constants.UNKNOWN);
        }
    }
}