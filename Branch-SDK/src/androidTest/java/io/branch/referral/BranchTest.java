package io.branch.referral;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.branch.referral.test.mock.MockActivity;
import io.branch.referral.test.mock.MockRemoteInterface;

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
            branch.setInitState(Branch.SessionState.UNINITIALISED);
            Branch.shutDown();
            branch = null;
        }

        clearSharedPrefs(mContext);
        mContext = null;
    }


    public void clearSharedPrefs(Context mContext) {
        BranchLogger.v("clearSharedPrefs");
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

        Branch.enableLogging();

        if (branchKey == null) {
            branch = Branch.getInstance();
        } else {
            branch = Branch.getInstance();
        }
        Assert.assertEquals(branch, Branch.getInstance());

        activityScenario = ActivityScenario.launch(MockActivity.class);

        branch.setBranchRemoteInterface(new MockRemoteInterface());
    }

    protected synchronized void initSessionResumeActivity(final Runnable pretest, final Runnable posttest) {
        final CountDownLatch sessionLatch = new CountDownLatch(1);

        activityScenario.onActivity(new ActivityScenario.ActivityAction<MockActivity>() {
            @Override
            public void perform(final MockActivity activity) {
                Branch.sessionBuilder(activity).withCallback(new Branch.BranchReferralInitListener() {
                    @Override
                    public void onInitFinished(@Nullable JSONObject referringParams, @Nullable BranchError error) {
                        // this isn't really a test, just makes sure that we are indeed using `MockRemoteInterface` and getting success responses
                        BranchLogger.v(TAG + " onInitFinished, referringParams: " + referringParams + ", error: " + error);
                        Assert.assertNotNull(referringParams);
                        if (error != null) {
                            if (error.getErrorCode() != BranchError.ERR_BRANCH_REQ_TIMED_OUT) {
                                Assert.fail("error should be null unless we are testing timeouts" + error.getMessage());
                            }
                        }
                        sessionLatch.countDown();
                    }
                }).withData(activity.getIntent() == null ? null : activity.getIntent().getData()).init();

                // `pretest`s, are typically gonna test request post body. The request is retrieved from the queue while it's
                // waiting for session to be initialized. MockRemoteInterface will purposefully delay session initialization
                // request for TEST_TIMEOUT/2 millis, to mimic real life delay and to allow for pretests to be executed. Note,
                // pretests must complete in TEST_TIMEOUT/2 millis.
                if (pretest != null) {
                    pretest.run();
                }

                Log.d(TAG, "initSessionResumeActivity completed");
            }
        });

        try {
            Assert.assertTrue(sessionLatch.await(TEST_INIT_SESSION_TIMEOUT, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            Assert.fail("session initialization timeout");
        }

        if (posttest != null) {
            posttest.run();
        }
    }

    protected Context getTestContext() {
        return mContext;
    }
}
