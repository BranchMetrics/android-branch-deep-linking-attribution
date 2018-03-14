package io.branch.branchandroiddemo.test;

import android.app.Activity;
import android.content.Intent;

import java.util.UUID;

import io.branch.referral.Branch;
import io.branch.referral.Defines;
import io.branch.referral.PrefHelper;

/**
 * Created by sojanpr on 3/11/18.
 * <p>
 * Test cases for QA internal tests
 * </p>
 */

public class QAInternalTestCases {
    private String KEY_KILL_ON_OPEN = "kill_on_open";
    private final PrefHelper prefHelper;
    private final Activity activity;
    
    public QAInternalTestCases(Activity activity) {
        this.prefHelper = PrefHelper.getInstance(activity);
        this.activity = activity;
    }
    
    //1. v1/install
    public void simulateInstall() {
        prefHelper.setIdentityID(PrefHelper.NO_STRING_VALUE);
        Intent intent = new Intent(activity, QAInternalActivity.class);
        intent.putExtra(Defines.Jsonkey.ForceNewBranchSession.getKey(), true);
        intent.putExtra(KEY_KILL_ON_OPEN, true);
        activity.startActivity(intent);
    }
    
    //1. v1/open
    public void simulateOpen() {
        Intent intent = new Intent(activity, QAInternalActivity.class);
        intent.putExtra(Defines.Jsonkey.ForceNewBranchSession.getKey(), true);
        intent.putExtra(KEY_KILL_ON_OPEN, true);
        activity.startActivity(intent);
    }
    
    // v1/url
    public void simulateLinkCreate() {
        QAInternalTestData.testBuo.generateShortUrl(activity, QAInternalTestData.linkProperties, null);
    }
    
    // v1/register_view
    public void simulateRegisterView() {
        QAInternalTestData.testBuo.registerView();
    }
    // v2/event
    public void simulateBranchEvent() {
        QAInternalTestData.branchEvent.addContentItems(QAInternalTestData.testBuo).logEvent(activity);
    }
    
    // v1/credithistory
    public void simulateCredit(){
        Branch.getInstance().getCreditHistory(null);
    }
    
    // v1/profile
    public void simulateIdentifyUser(){
        Branch.getInstance().setIdentity(UUID.randomUUID().toString());
    }
}
