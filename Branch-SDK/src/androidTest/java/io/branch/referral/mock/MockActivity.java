package io.branch.referral.mock;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MockActivity extends Activity {
    private static final String TAG = "BranchMockActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MockActivity Started");
    }
}
