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

/**
 * Base Instrumented test, which will execute on an Android device.
 */
@RunWith(AndroidJUnit4.class)
abstract public class BranchTest {
    private static final String TAG = "BranchTest";

    protected Context mContext;
    protected Branch branch;
    protected PrefHelper prefHelper;
    protected ActivityScenario<MockActivity> activityScenario;

    @Before
    public void setUp() {
        Branch.shutDown();
        mContext = ApplicationProvider.getApplicationContext();
    }

    @After
    public void tearDown() {
        if (activityScenario != null) {
            activityScenario.close();
        }

        if (branch != null) {
            branch.setInitState(Branch.SESSION_STATE.UNINITIALISED);
            Branch.shutDown();
            branch = null;
        }

        mContext = null;
    }

    @Before
    @After
    public void clearSharedPrefs() {
        if (mContext == null) return;
        // Clear the PrefHelper shared preferences
        SharedPreferences sharedPreferences =
                mContext.getSharedPreferences("branch_referral_shared_pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();

        // Clear the ServerRequestQueue shared preferences
        sharedPreferences =
                mContext.getSharedPreferences("BNC_Server_Request_Queue", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
    }

    protected void initBranchInstance() {
        initBranchInstance(null);
    }

    protected void initBranchInstance(String branchKey) {
        if (branch != null) throw new IllegalStateException("sdk already initialized, makes sure initBranchInstance is called just once per test.");
        createActivity();
        if (branchKey == null) {
            branch = Branch.getAutoInstance(getTestContext());
        } else {
            branch = Branch.getAutoInstance(getTestContext(), branchKey);
        }
        Assert.assertEquals(branch, Branch.getInstance());
//        activityScenario.moveToState(Lifecycle.State.RESUMED);
    }

    protected void initTestSession() {
        Branch.sessionBuilder(null).init();
    }

    private void createActivity() {
        if (branch != null) {
            Log.d(TAG, "Warning! Activity is being initialized after SDK initialization. Beware that SDK will self-initialize session.");
        }

        activityScenario = ActivityScenario.launch(MockActivity.class);

        // There is no way to launch Activity into some lifecycle state and halt it there,
        // i.e. ActivityScenario.launch(...) will traverse all lifecycle states and we can only move it to Lifecycle.State.CREATED afterwards
        // https://developer.android.com/reference/androidx/test/core/app/ActivityScenario
        // We move it to Lifecycle.State.CREATED, so we can simulate Activity boot up and test session initialization
        activityScenario.moveToState(Lifecycle.State.CREATED);
    }

    protected Context getTestContext() {
        return mContext;
    }
}
