package io.branch.referral.mock;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONObject;
import org.junit.Assert;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;

public class MockActivity extends Activity {
    private static final String TAG = "BranchSDK";
    public String state;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MockActivity Created");
        state = "created";
    }

    @Override
    public void onStart() {
        super.onStart();
        state = "started";
        if (Branch.getInstance() != null) {
            Log.d(TAG, "MockActivity Started");
            Branch.sessionBuilder(this).withCallback(new Branch.BranchReferralInitListener() {
                @Override
                public void onInitFinished(@Nullable JSONObject referringParams, @Nullable BranchError error) {
                    Log.d(TAG, "onInitFinished, referringParams: " + referringParams + ", error: " + error);
                    Assert.assertNotNull(referringParams);
                    Assert.assertNull(error);

                }
            }).withData(getIntent() == null ? null : getIntent().getData()).init();
        } else {
            Log.d(TAG, "MockActivity started but session not initialized");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "MockActivity Resumed");
        state = "resumed";
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "MockActivity Paused");
        state = "paused";
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "MockActivity Stopped");
        state = "stopped";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MockActivity Destroyed");
        state = "destroyed";
    }
}
