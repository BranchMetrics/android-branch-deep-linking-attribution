package io.branch.referral;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import io.branch.referral.mock.MockActivity;
import io.branch.referral.mock.MockRemoteInterface;

/**
 * Base Instrumented test, which will execute on an Android device.
 */
@RunWith(AndroidJUnit4.class)
abstract public class BranchTest extends BranchTestRequestUtil {
    private static final String TAG = "BranchTest";
    protected static final String TEST_KEY = "key_live_testing_only";

    protected Context mContext;
    protected Branch branch;
    protected ActivityScenario<MockActivity> activityScenario;

    @Before
    public void setUp() {
        Branch.shutDown();
        mContext = ApplicationProvider.getApplicationContext();
        clearSharedPrefs(mContext);
    }

    @After
    public void tearDown() throws InterruptedException {
        if (activityScenario != null) {
            activityScenario.close();
            Thread.sleep(TEST_REQUEST_TIMEOUT);
        }

        if (branch != null) {
            branch.setInitState(Branch.SESSION_STATE.UNINITIALISED);
            Branch.shutDown();
            branch = null;
        }

        clearSharedPrefs(mContext);
        mContext = null;
    }


    public void clearSharedPrefs(Context mContext) {
        PrefHelper.Debug("clearSharedPrefs");
        // Clear the PrefHelper shared preferences
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("branch_referral_shared_pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();

        // Clear the ServerRequestQueue shared preferences
        sharedPreferences = mContext.getSharedPreferences("BNC_Server_Request_Queue", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
    }

    protected void initBranchInstance() {
        initBranchInstance(null);
    }

    protected void initBranchInstance(String branchKey) {
        if (branch != null) {
            throw new IllegalStateException("sdk already initialized, makes sure initBranchInstance is called just once per test.");
        }
        activityScenario = ActivityScenario.launch(MockActivity.class);

        // There is no way to launch Activity into some lifecycle state and halt it there,
        // i.e. ActivityScenario.launch(...) will traverse all lifecycle states and we can only move it to Lifecycle.State.CREATED afterwards
        // https://developer.android.com/reference/androidx/test/core/app/ActivityScenario
        // We move it to Lifecycle.State.CREATED, so we can simulate Activity boot up and test session initialization
        activityScenario.moveToState(Lifecycle.State.CREATED);

        Branch.enableLogging();
        if (branchKey == null) {
            branch = Branch.getAutoInstance(getTestContext());
        } else {
            branch = Branch.getAutoInstance(getTestContext(), branchKey);
        }
        Assert.assertEquals(branch, Branch.getInstance());

        branch.setBranchRemoteInterface(new MockRemoteInterface());
    }

    protected void initSessionResumeActivity() throws InterruptedException {
        initSessionResumeActivity(null);
    }

    protected void initSessionResumeActivity(Runnable subtest) throws InterruptedException {
        activityScenario.moveToState(Lifecycle.State.RESUMED);
        // MockRemoteInterface will purposefully delay session initialization request for TEST_TIMEOUT/2 millis,
        // `subtest`, which typically tests properties of requests retrieved from the queue must complete
        // in that amount of time
        if (subtest != null) {
            subtest.run();
        }
        // activityScenario.moveToState is async, so we stop the test's thread for Activity to complete the
        // lifecycle transitions (and session initialization)
        Thread.sleep(TEST_REQUEST_TIMEOUT * 3);
        Log.d(TAG, "initSessionResumeActivity completed");
    }

    protected Context getTestContext() {
        return mContext;
    }
}
