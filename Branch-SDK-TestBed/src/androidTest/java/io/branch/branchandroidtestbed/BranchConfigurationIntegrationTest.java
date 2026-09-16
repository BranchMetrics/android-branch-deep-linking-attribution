package io.branch.branchandroidtestbed;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.branch.referral.BranchLogger;
import io.branch.referral.PrefHelper;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented test for PrefHelper's on-device storage layer.
 *
 * BranchConfiguration.Builder and DMAParameters are plain in-memory objects with no Android
 * dependency, so their field storage, validation, and construction rules are covered by the JVM
 * suite (BranchConfigurationBuilderTest, DMAParametersTest) instead — running them here would only
 * duplicate that coverage at emulator cost. This class is for behavior that needs a real Context.
 */
@RunWith(AndroidJUnit4.class)
public class BranchConfigurationIntegrationTest {

    private static final String TAG = "BranchConfigTest";
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        BranchLogger.setLoggingEnabled(true);
        BranchLogger.setLoggingLevel(BranchLogger.BranchLogLevel.VERBOSE);
        Log.i(TAG, "=== BranchConfiguration integration tests starting ===");
    }

    @After
    public void tearDown() {
        Log.i(TAG, "=== BranchConfiguration integration tests complete ===");
    }

    @Test
    public void prefHelper_networkSettings_matchBuilder() {
        Log.i(TAG, "--- prefHelper_networkSettings_matchBuilder ---");

        PrefHelper prefHelper = PrefHelper.getInstance(context);

        int timeoutBefore = prefHelper.getTimeout();
        prefHelper.setTimeout(15_000);
        Log.i(TAG, "PrefHelper.timeout before = " + timeoutBefore);
        Log.i(TAG, "PrefHelper.timeout after  = " + prefHelper.getTimeout());

        assertEquals(15_000, prefHelper.getTimeout());

        // restore
        prefHelper.setTimeout(timeoutBefore);
    }
}
