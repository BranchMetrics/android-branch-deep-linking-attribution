package io.branch.referral.test.mock;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import org.json.JSONObject;
import org.junit.Assert;

import java.util.concurrent.CountDownLatch;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.PrefHelper;

public class MockActivity extends Activity {
    private static final String TAG = "MockActivity";
    public String state;
    public boolean initSessionCallbackInvoked = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = "created";
    }

    @Override
    public void onStart() {
        super.onStart();
        state = "started";
    }

    @Override
    public void onResume() {
        super.onResume();
        state = "resumed";
    }

    @Override
    public void onPause() {
        super.onPause();
        state = "paused";
    }

    @Override
    public void onStop() {
        super.onStop();
        state = "stopped";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        state = "destroyed";
    }
}