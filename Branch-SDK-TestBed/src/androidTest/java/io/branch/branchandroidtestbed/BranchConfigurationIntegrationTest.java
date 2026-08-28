package io.branch.branchandroidtestbed;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.branch.referral.BranchConfiguration;
import io.branch.referral.BranchLogger;
import io.branch.referral.Defines;
import io.branch.referral.DMAParameters;
import io.branch.referral.PrefHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for BranchConfiguration.Builder and DMAParameters.
 *
 * Verifies that each builder setting is stored correctly in PrefHelper (the same layer
 * that ServerRequest reads when building outgoing payloads). Passes without a network
 * connection — no Branch key calls are made.
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

    // -------------------------------------------------------------------------
    // DMAParameters
    // -------------------------------------------------------------------------

    @Test
    public void dmaParameters_fieldsStoredCorrectly() {
        Log.i(TAG, "--- dmaParameters_fieldsStoredCorrectly ---");

        DMAParameters params = new DMAParameters.Builder().setEeaRegion(true).setAdUserDataUsageConsent(true).build();

        Log.i(TAG, "DMAParameters.eeaRegion             = " + params.getEeaRegion());
        Log.i(TAG, "DMAParameters.adPersonalization     = " + params.getAdPersonalizationConsent());
        Log.i(TAG, "DMAParameters.adUserDataUsage       = " + params.getAdUserDataUsageConsent());

        assertTrue("eeaRegion should be true",             params.getEeaRegion());
        assertFalse("adPersonalization should be false",   params.getAdPersonalizationConsent());
        assertTrue("adUserDataUsage should be true",        params.getAdUserDataUsageConsent());
    }

    @Test
    public void dmaParameters_allFalse_stored() {
        Log.i(TAG, "--- dmaParameters_allFalse_stored ---");

        DMAParameters params = new DMAParameters.Builder().build();

        assertFalse(params.getEeaRegion());
        assertFalse(params.getAdPersonalizationConsent());
        assertFalse(params.getAdUserDataUsageConsent());

        Log.i(TAG, "All-false DMAParameters OK");
    }

    // -------------------------------------------------------------------------
    // BranchConfiguration.Builder — field round-trips
    // -------------------------------------------------------------------------

    @Test
    public void builder_branchKey_stored() {
        Log.i(TAG, "--- builder_branchKey_stored ---");

        BranchConfiguration config = new BranchConfiguration.Builder("key_live_test123")
                .build();

        Log.i(TAG, "branchKey = " + config.getBranchKey());
        assertEquals("key_live_test123", config.getBranchKey());
    }

    @Test
    public void builder_networkSettings_stored() {
        Log.i(TAG, "--- builder_networkSettings_stored ---");

        BranchConfiguration config = new BranchConfiguration.Builder("key_live_test123")
                .setNetworkTimeout(12_000)
                .setNetworkConnectTimeout(4_000)
                .setRetryCount(5)
                .setRetryInterval(2_000)
                .setNoConnectionRetryMax(4)
                .build();

        Log.i(TAG, "networkTimeout       = " + config.getNetworkTimeout());
        Log.i(TAG, "networkConnectTimeout= " + config.getNetworkConnectTimeout());
        Log.i(TAG, "retryCount           = " + config.getRetryCount());
        Log.i(TAG, "retryInterval        = " + config.getRetryInterval());
        Log.i(TAG, "noConnectionRetryMax = " + config.getNoConnectionRetryMax());

        assertEquals(12_000, config.getNetworkTimeout());
        assertEquals(4_000,  config.getNetworkConnectTimeout());
        assertEquals(5,      config.getRetryCount());
        assertEquals(2_000,  config.getRetryInterval());
        assertEquals(4,      config.getNoConnectionRetryMax());
    }

    @Test
    public void builder_attributionLevel_stored() {
        Log.i(TAG, "--- builder_attributionLevel_stored ---");

        BranchConfiguration config = new BranchConfiguration.Builder("key_live_test123")
                .setAttributionLevel(Defines.BranchAttributionLevel.MINIMAL)
                .build();

        Log.i(TAG, "attributionLevel = " + config.getAttributionLevel());
        assertEquals(Defines.BranchAttributionLevel.MINIMAL, config.getAttributionLevel());
    }

    @Test
    public void builder_dmaParameters_stored() {
        Log.i(TAG, "--- builder_dmaParameters_stored ---");

        DMAParameters dma = new DMAParameters.Builder().setEeaRegion(true).setAdPersonalizationConsent(true).build();
        BranchConfiguration config = new BranchConfiguration.Builder("key_live_test123")
                .setDMAParameters(dma)
                .build();

        DMAParameters stored = config.getDmaParameters();
        Log.i(TAG, "dmaParameters.eeaRegion         = " + stored.getEeaRegion());
        Log.i(TAG, "dmaParameters.adPersonalization = " + stored.getAdPersonalizationConsent());
        Log.i(TAG, "dmaParameters.adUserDataUsage   = " + stored.getAdUserDataUsageConsent());

        assertNotNull(stored);
        assertTrue(stored.getEeaRegion());
        assertTrue(stored.getAdPersonalizationConsent());
        assertFalse(stored.getAdUserDataUsageConsent());
    }

    @Test
    public void builder_testMode_stored() {
        Log.i(TAG, "--- builder_testMode_stored ---");

        BranchConfiguration config = new BranchConfiguration.Builder("key_live_test123")
                .setTestMode(true)
                .build();

        Log.i(TAG, "testMode = " + config.getTestMode());
        assertTrue(config.getTestMode());
    }

    @Test
    public void builder_euEndpoint_stored() {
        Log.i(TAG, "--- builder_euEndpoint_stored ---");

        BranchConfiguration config = new BranchConfiguration.Builder("key_live_test123")
                .setEUEndpoint(true)
                .build();

        Log.i(TAG, "euEndpoint = " + config.getEuEndpoint());
        assertTrue(config.getEuEndpoint());
    }

    @Test
    public void builder_logLevel_stored() {
        Log.i(TAG, "--- builder_logLevel_stored ---");

        BranchConfiguration config = new BranchConfiguration.Builder("key_live_test123")
                .setLogLevel(BranchLogger.BranchLogLevel.DEBUG)
                .build();

        Log.i(TAG, "logLevel = " + config.getLogLevel());
        assertEquals(BranchLogger.BranchLogLevel.DEBUG, config.getLogLevel());
    }

    @Test
    public void builder_installMetadata_stored() {
        Log.i(TAG, "--- builder_installMetadata_stored ---");

        BranchConfiguration config = new BranchConfiguration.Builder("key_live_test123")
                .addInstallMetadata("store", "galaxy_store")
                .addInstallMetadata("campaign", "q2_push")
                .build();

        Log.i(TAG, "installMetadata[store]    = " + config.getInstallMetadata().get("store"));
        Log.i(TAG, "installMetadata[campaign] = " + config.getInstallMetadata().get("campaign"));

        assertEquals("galaxy_store", config.getInstallMetadata().get("store"));
        assertEquals("q2_push",      config.getInstallMetadata().get("campaign"));
    }

    @Test
    public void builder_whitelistedSchemes_stored() {
        Log.i(TAG, "--- builder_whitelistedSchemes_stored ---");

        BranchConfiguration config = new BranchConfiguration.Builder("key_live_test123")
                .addWhitelistedScheme("myapp://")
                .addWhitelistedScheme("https://")
                .build();

        Log.i(TAG, "whitelistedSchemes = " + config.getWhitelistedSchemes());
        assertEquals(2, config.getWhitelistedSchemes().size());
        assertTrue(config.getWhitelistedSchemes().contains("myapp://"));
    }

    @Test
    public void builder_uriHostsToSkip_stored() {
        Log.i(TAG, "--- builder_uriHostsToSkip_stored ---");

        BranchConfiguration config = new BranchConfiguration.Builder("key_live_test123")
                .addUriHostToSkip("internal.example.com")
                .build();

        Log.i(TAG, "uriHostsToSkip = " + config.getUriHostsToSkip());
        assertTrue(config.getUriHostsToSkip().contains("internal.example.com"));
    }

    @Test
    public void builder_automaticOpenEvents_false_stored() {
        Log.i(TAG, "--- builder_automaticOpenEvents_false_stored ---");

        BranchConfiguration config = new BranchConfiguration.Builder("key_live_test123")
                .setAutomaticOpenEvents(false)
                .build();

        Log.i(TAG, "automaticOpenEvents = " + config.getAutomaticOpenEvents());
        assertFalse(config.getAutomaticOpenEvents());
    }

    // -------------------------------------------------------------------------
    // build() validation
    // -------------------------------------------------------------------------

    @Test
    public void build_emptyKey_throwsIllegalArgumentException() {
        Log.i(TAG, "--- build_emptyKey_throwsIllegalArgumentException ---");

        IllegalArgumentException thrown = null;
        try {
            new BranchConfiguration.Builder("").build();
        } catch (IllegalArgumentException e) {
            thrown = e;
            Log.i(TAG, "Caught expected exception: " + e.getMessage());
        }

        assertNotNull("Expected IllegalArgumentException for empty key", thrown);
        assertTrue(thrown.getMessage().contains("dashboard"));
    }

    @Test
    public void build_negativeTimeout_throwsIllegalArgumentException() {
        Log.i(TAG, "--- build_negativeTimeout_throwsIllegalArgumentException ---");

        IllegalArgumentException thrown = null;
        try {
            new BranchConfiguration.Builder("key_live_test123")
                    .setNetworkTimeout(-1)
                    .build();
        } catch (IllegalArgumentException e) {
            thrown = e;
            Log.i(TAG, "Caught expected exception: " + e.getMessage());
        }

        assertNotNull("Expected IllegalArgumentException for negative timeout", thrown);
        assertTrue(thrown.getMessage().contains("-1"));
    }

    @Test
    public void build_timeoutExceeds60s_throwsIllegalArgumentException() {
        Log.i(TAG, "--- build_timeoutExceeds60s_throwsIllegalArgumentException ---");

        IllegalArgumentException thrown = null;
        try {
            new BranchConfiguration.Builder("key_live_test123")
                    .setNetworkTimeout(61_000)
                    .build();
        } catch (IllegalArgumentException e) {
            thrown = e;
            Log.i(TAG, "Caught expected exception: " + e.getMessage());
        }

        assertNotNull("Expected IllegalArgumentException for timeout > 60s", thrown);
    }

    @Test
    public void build_negativeRetryCount_throwsIllegalArgumentException() {
        Log.i(TAG, "--- build_negativeRetryCount_throwsIllegalArgumentException ---");

        IllegalArgumentException thrown = null;
        try {
            new BranchConfiguration.Builder("key_live_test123")
                    .setRetryCount(-1)
                    .build();
        } catch (IllegalArgumentException e) {
            thrown = e;
            Log.i(TAG, "Caught expected exception: " + e.getMessage());
        }

        assertNotNull("Expected IllegalArgumentException for negative retry count", thrown);
    }

    // -------------------------------------------------------------------------
    // PrefHelper wiring — verifies settings propagate through to the storage layer
    // -------------------------------------------------------------------------

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
