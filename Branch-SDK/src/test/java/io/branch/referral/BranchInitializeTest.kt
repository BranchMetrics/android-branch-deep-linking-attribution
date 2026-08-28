package io.branch.referral

import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.json.JSONObject
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Tests for [Branch.initialize] and its private helpers [applyConfiguration] and [applyBranchKey].
 *
 * These tests cover the wiring layer that was NOT covered by BranchConfigurationBuilderTest
 * (which only verifies values inside the config object). Here we verify that calling
 * [Branch.initialize] actually propagates each config field into PrefHelper, BranchUtil,
 * BranchLogger, and other SDK subsystems.
 *
 * Branch.shutDown() in @After resets the singleton so each test starts clean.
 */
class BranchInitializeTest : BranchTestBase() {

    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    override fun setUpBase() {
        super.setUpBase()
        Branch.shutDown()
    }

    @After
    override fun tearDownBase() {
        super.tearDownBase()
        Branch.shutDown()
        // ProcessLifecycleOwner is process-global; leaving it STARTED makes every later
        // initialize() register an observer that fires OPEN immediately.
        (ProcessLifecycleOwner.get().lifecycle as LifecycleRegistry).currentState =
            Lifecycle.State.CREATED
    }

    // -------------------------------------------------------------------------
    // Network settings wired to PrefHelper
    // -------------------------------------------------------------------------

    @Test
    fun initialize_networkTimeout_reachesPrefHelper() {
        val config = BranchConfiguration.Builder("key_live_test123")
            .setNetworkTimeout(15_000)
            .build()

        Branch.initialize(context, config)

        assertEquals(15_000, PrefHelper.getInstance(context).getTimeout())
    }

    @Test
    fun initialize_networkConnectTimeout_reachesPrefHelper() {
        val config = BranchConfiguration.Builder("key_live_test123")
            .setNetworkConnectTimeout(4_000)
            .build()

        Branch.initialize(context, config)

        assertEquals(4_000, PrefHelper.getInstance(context).getConnectTimeout())
    }

    @Test
    fun initialize_retryCount_reachesPrefHelper() {
        val config = BranchConfiguration.Builder("key_live_test123")
            .setRetryCount(5)
            .build()

        Branch.initialize(context, config)

        assertEquals(5, PrefHelper.getInstance(context).getRetryCount())
    }

    @Test
    fun initialize_retryInterval_reachesPrefHelper() {
        val config = BranchConfiguration.Builder("key_live_test123")
            .setRetryInterval(2_000)
            .build()

        Branch.initialize(context, config)

        assertEquals(2_000, PrefHelper.getInstance(context).getRetryInterval())
    }

    @Test
    fun initialize_noConnectionRetryMax_reachesPrefHelper() {
        val config = BranchConfiguration.Builder("key_live_test123")
            .setNoConnectionRetryMax(4)
            .build()

        Branch.initialize(context, config)

        assertEquals(4, PrefHelper.getInstance(context).getNoConnectionRetryMax())
    }

    // -------------------------------------------------------------------------
    // Automatic open events / process lifecycle observer
    // -------------------------------------------------------------------------

    /**
     * Adding an observer to an already-STARTED lifecycle dispatches onStart synchronously, so
     * registering-then-unregistering still emits the OPEN it was meant to suppress. Driving the
     * process lifecycle to STARTED before initialize() is what exposes that.
     */
    private fun startProcessLifecycle() {
        (ProcessLifecycleOwner.get().lifecycle as LifecycleRegistry).currentState =
            Lifecycle.State.STARTED
    }

    private val openEmitted = "process foregrounded, sending OPEN"

    @Test
    fun initialize_automaticOpenEventsFalse_emitsNoOpenWhenAlreadyForegrounded() {
        startProcessLifecycle()

        val logs = captureInitLogs(BranchLogger.BranchLogLevel.VERBOSE) {
            setAutomaticOpenEvents(false)
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertFalse(
            "automaticOpenEvents=false must not emit an OPEN, even when the process is already " +
                "foregrounded at initialize()",
            logs.any { it.contains(openEmitted) }
        )
    }

    @Test
    fun initialize_automaticOpenEventsDefault_emitsOpenWhenAlreadyForegrounded() {
        startProcessLifecycle()

        val logs = captureInitLogs(BranchLogger.BranchLogLevel.VERBOSE)
        // register() posts to the main looper when off-thread; drain it so the assertion does not
        // depend on which path was taken.
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertTrue(
            "the default must still emit an OPEN when the process is already foregrounded",
            logs.any { it.contains(openEmitted) }
        )
    }

    // -------------------------------------------------------------------------
    // Request tracing callback
    // -------------------------------------------------------------------------

    @Test
    fun initialize_requestTracingCallback_reachesBranch() {
        val callback = IBranchRequestTracingCallback { _, _, _, _, _ -> }
        val config = BranchConfiguration.Builder("key_live_test123")
            .setRequestTracingCallback(callback)
            .build()

        Branch.initialize(context, config)

        assertEquals(callback, Branch.getCallbackForTracingRequests())
    }

    @Test
    fun shutDown_clearsRequestTracingCallback() {
        val config = BranchConfiguration.Builder("key_live_test123")
            .setRequestTracingCallback(IBranchRequestTracingCallback { _, _, _, _, _ -> })
            .build()
        Branch.initialize(context, config)

        Branch.shutDown()

        assertNull(Branch.getCallbackForTracingRequests())
    }

    @Test
    fun initialize_withoutTracingCallback_doesNotInheritPreviousOne() {
        Branch.initialize(
            context,
            BranchConfiguration.Builder("key_live_test123")
                .setRequestTracingCallback(IBranchRequestTracingCallback { _, _, _, _, _ -> })
                .build()
        )
        Branch.shutDown()

        Branch.initialize(context, BranchConfiguration.Builder("key_live_test123").build())

        assertNull(Branch.getCallbackForTracingRequests())
    }

    // -------------------------------------------------------------------------
    // Test mode wired to BranchUtil
    // -------------------------------------------------------------------------

    @Test
    fun initialize_testModeTrue_setsTestModeInBranchUtil() {
        val config = BranchConfiguration.Builder("key_test_abc123")
            .setTestMode(true)
            .build()

        Branch.initialize(context, config)

        assertTrue(BranchUtil.isTestModeEnabled())
    }

    @Test
    fun initialize_testModeFalse_doesNotEnableTestMode() {
        val config = BranchConfiguration.Builder("key_live_test123")
            .setTestMode(false)
            .build()

        Branch.initialize(context, config)

        assertFalse(BranchUtil.isTestModeEnabled())
    }

    // -------------------------------------------------------------------------
    // Log level wired to BranchLogger
    // -------------------------------------------------------------------------

    @Test
    fun initialize_explicitLogLevel_reachesBranchLogger() {
        val config = BranchConfiguration.Builder("key_live_test123")
            .setLogLevel(BranchLogger.BranchLogLevel.ERROR)
            .build()

        Branch.initialize(context, config)

        assertEquals(BranchLogger.BranchLogLevel.ERROR, BranchLogger.loggingLevel)
    }

    @Test
    fun initialize_defaultLogLevel_setsNone() {
        // initialize() must always own the logger state — no inherited ambient level.
        BranchLogger.loggingLevel = BranchLogger.BranchLogLevel.VERBOSE // simulate prior state

        Branch.initialize(context, BranchConfiguration.Builder("key_live_test123").build())

        assertEquals(BranchLogger.BranchLogLevel.NONE, BranchLogger.loggingLevel)
    }

    // -------------------------------------------------------------------------
    // DMA parameters at runtime
    // -------------------------------------------------------------------------

    @Test
    fun setDMAParameters_afterInitialize_reachesPrefHelper() {
        Branch.initialize(
            context,
            BranchConfiguration.Builder("key_live_test123")
                .setDMAParameters(DMAParameters.Builder().setEeaRegion(true).build())
                .build()
        )

        Branch.getInstance().setDMAParameters(
            DMAParameters.Builder()
                .setEeaRegion(true)
                .setAdPersonalizationConsent(true)
                .setAdUserDataUsageConsent(true)
                .build()
        )

        val prefHelper = PrefHelper.getInstance(context)
        assertTrue(prefHelper.getEEARegion())
        assertTrue(prefHelper.getAdPersonalizationConsent())
        assertTrue(prefHelper.getAdUserDataUsageConsent())
    }

    @Test
    fun setDMAParameters_canRevokeConsentGrantedAtInit() {
        Branch.initialize(
            context,
            BranchConfiguration.Builder("key_live_test123")
                .setDMAParameters(
                    DMAParameters.Builder()
                        .setEeaRegion(true)
                        .setAdPersonalizationConsent(true)
                        .setAdUserDataUsageConsent(true)
                        .build()
                )
                .build()
        )

        Branch.getInstance().setDMAParameters(
            DMAParameters.Builder().setEeaRegion(true).build()
        )

        val prefHelper = PrefHelper.getInstance(context)
        assertTrue(prefHelper.getEEARegion())
        assertFalse(prefHelper.getAdPersonalizationConsent())
        assertFalse(prefHelper.getAdUserDataUsageConsent())
    }

    @Test
    fun setDMAParameters_emitsTheInconsistencyWarning() {
        val logs = captureInitLogs(BranchLogger.BranchLogLevel.VERBOSE)

        Branch.getInstance().setDMAParameters(
            DMAParameters.Builder().setAdUserDataUsageConsent(true).build()
        )

        assertTrue(
            "the runtime path must drain the warning too, not just applyTo()",
            logs.any { it.contains("consent was granted with eeaRegion false") }
        )
    }

    @Test
    fun initialize_emitsTheInconsistencyWarningFromApplyTo() {
        val logs = captureInitLogs(BranchLogger.BranchLogLevel.VERBOSE) {
            setDMAParameters(DMAParameters.Builder().setAdUserDataUsageConsent(true).build())
        }

        assertTrue(logs.any { it.contains("consent was granted with eeaRegion false") })
    }

    // -------------------------------------------------------------------------
    // DMA parameters wired to PrefHelper
    // -------------------------------------------------------------------------

    @Test
    fun initialize_dmaParameters_eeaRegion_reachesPrefHelper() {
        val config = BranchConfiguration.Builder("key_live_test123")
            .setDMAParameters(DMAParameters.Builder().setEeaRegion(true).build())
            .build()

        Branch.initialize(context, config)

        assertTrue(PrefHelper.getInstance(context).getEEARegion())
    }

    @Test
    fun initialize_dmaParameters_adPersonalizationConsent_reachesPrefHelper() {
        val config = BranchConfiguration.Builder("key_live_test123")
            .setDMAParameters(DMAParameters.Builder().setAdPersonalizationConsent(true).build())
            .build()

        Branch.initialize(context, config)

        assertTrue(PrefHelper.getInstance(context).getAdPersonalizationConsent())
    }

    @Test
    fun initialize_dmaParameters_adUserDataUsageConsent_reachesPrefHelper() {
        val config = BranchConfiguration.Builder("key_live_test123")
            .setDMAParameters(DMAParameters.Builder().setAdUserDataUsageConsent(true).build())
            .build()

        Branch.initialize(context, config)

        assertTrue(PrefHelper.getInstance(context).getAdUserDataUsageConsent())
    }

    @Test
    fun initialize_noDmaParameters_leavesEeaRegionFalse() {
        val config = BranchConfiguration.Builder("key_live_test123").build()

        Branch.initialize(context, config)

        assertFalse(PrefHelper.getInstance(context).getEEARegion())
    }

    // -------------------------------------------------------------------------
    // Branch key wired to PrefHelper (applyBranchKey)
    // -------------------------------------------------------------------------

    @Test
    fun initialize_branchKey_reachesPrefHelper() {
        val config = BranchConfiguration.Builder("key_live_test123").build()

        Branch.initialize(context, config)

        assertEquals("key_live_test123", PrefHelper.getInstance(context).getBranchKey())
    }

    // -------------------------------------------------------------------------
    // Singleton guard — second initialize() call is a no-op
    // -------------------------------------------------------------------------

    @Test
    fun initialize_calledTwice_doesNotReinitialize() {
        val first  = BranchConfiguration.Builder("key_live_first").setNetworkTimeout(5_000).build()
        val second = BranchConfiguration.Builder("key_live_second").setNetworkTimeout(9_000).build()

        Branch.initialize(context, first)
        Branch.initialize(context, second) // should be ignored

        // First call's key and timeout must survive.
        assertEquals("key_live_first", PrefHelper.getInstance(context).getBranchKey())
        assertEquals(5_000, PrefHelper.getInstance(context).getTimeout())
    }

    // -------------------------------------------------------------------------
    // getInstance returns the singleton after initialize
    // -------------------------------------------------------------------------

    @Test
    fun initialize_then_getInstanceReturnsNonNull() {
        val config = BranchConfiguration.Builder("key_live_test123").build()

        Branch.initialize(context, config)

        assertNotNull(Branch.getInstance())
    }

    @Test
    fun beforeInitialize_getInstanceReturnsNull() {
        assertNull(Branch.getInstance())
    }

    // -------------------------------------------------------------------------
    // Logging emitted by initialize()
    //
    // initialize() emits exactly two machine-readable lines, each a complete single-line JSON
    // object: "branch_configuration_applied" (what the caller asked for) and
    // "branch_initialize_complete" (what is actually in force afterwards).
    // -------------------------------------------------------------------------

    private val allConfigurationFields = listOf(
        "event", "branchKey", "testMode", "apiUrl", "cdnBaseUrl", "euEndpoint", "logLevel",
        "loggingCallback", "requestTracingCallback", "networkTimeout", "networkConnectTimeout",
        "retryCount", "retryInterval", "noConnectionRetryMax", "remoteInterface",
        "attributionLevel", "dmaParameters", "limitFacebookAttribution",
        "adNetworkCalloutsDisabled", "facebookAppId", "preinstallCampaign", "preinstallPartner",
        "installMetadata", "referringLinkAttributionForPreinstalledApps", "whitelistedSchemes",
        "uriHostsToSkip", "automaticOpenEvents", "userAgentFetchSync"
    )

    /**
     * applyTo() installs the config's own logging callback, so a capture callback has to be
     * supplied through the builder to see what initialize() emits.
     */
    private fun captureInitLogs(
        level: BranchLogger.BranchLogLevel = BranchLogger.BranchLogLevel.DEBUG,
        configure: BranchConfiguration.Builder.() -> Unit = {}
    ): List<String> {
        val captured = CopyOnWriteArrayList<String>()
        val config = BranchConfiguration.Builder(TEST_KEY)
            .setLogLevel(level)
            .setLoggingCallback { message, _ -> captured.add(message) }
            .apply(configure)
            .build()

        Branch.initialize(context, config)
        return captured
    }

    /** Every logged line that parses as a JSON object carrying the given event name. */
    private fun jsonEvents(logs: List<String>, event: String): List<JSONObject> =
        logs.mapNotNull { line ->
            runCatching { JSONObject(line) }.getOrNull()?.takeIf { it.optString("event") == event }
        }

    private fun singleEvent(logs: List<String>, event: String): JSONObject {
        val matches = jsonEvents(logs, event)
        assertEquals("expected exactly one \"$event\" line, got: $logs", 1, matches.size)
        return matches.single()
    }

    @Test
    fun initialize_logsAppliedConfigurationAsOneParseableJsonObject() {
        val logs = captureInitLogs {
            setNetworkTimeout(15_000)
            setPreinstallPartner("samsung")
            addWhitelistedScheme("myapp://")
            addInstallMetadata("store", "galaxy_store")
            setDMAParameters(DMAParameters.Builder().setEeaRegion(true).setAdUserDataUsageConsent(true).build())
        }

        val applied = singleEvent(logs, BranchConfiguration.EVENT_CONFIGURATION_APPLIED)

        assertEquals(15_000, applied.getInt("networkTimeout"))
        assertEquals("samsung", applied.getString("preinstallPartner"))
        assertEquals("myapp://", applied.getJSONArray("whitelistedSchemes").getString(0))
        assertEquals("galaxy_store", applied.getJSONObject("installMetadata").getString("store"))
        assertTrue(applied.getJSONObject("dmaParameters").getBoolean("eeaRegion"))
        assertFalse(applied.getJSONObject("dmaParameters").getBoolean("adPersonalizationConsent"))
        assertTrue(applied.getJSONObject("dmaParameters").getBoolean("adUserDataUsageConsent"))
    }

    @Test
    fun initialize_appliedConfigurationJson_alwaysCarriesEveryField() {
        val applied = singleEvent(
            captureInitLogs(),
            BranchConfiguration.EVENT_CONFIGURATION_APPLIED
        )

        val present = applied.keys().asSequence().toList()
        assertEquals(
            "the applied-configuration object must be a fixed shape so assertions can rely on it",
            allConfigurationFields.sorted(),
            present.sorted()
        )
    }

    @Test
    fun initialize_appliedConfigurationJson_reportsUnsetFieldsAsNull() {
        val applied = singleEvent(
            captureInitLogs(),
            BranchConfiguration.EVENT_CONFIGURATION_APPLIED
        )

        assertTrue("apiUrl was never set", applied.isNull("apiUrl"))
        assertTrue("attributionLevel was never set", applied.isNull("attributionLevel"))
        assertTrue("dmaParameters was never set", applied.isNull("dmaParameters"))
        assertEquals("installMetadata should be an empty object, not null",
            0, applied.getJSONObject("installMetadata").length())
        assertEquals("whitelistedSchemes should be an empty array, not null",
            0, applied.getJSONArray("whitelistedSchemes").length())
    }

    @Test
    fun initialize_appliedConfigurationJson_escapesAwkwardValues() {
        val awkward = "a\"b\\c\nd"
        val applied = singleEvent(
            captureInitLogs { addInstallMetadata("weird\"key", awkward) },
            BranchConfiguration.EVENT_CONFIGURATION_APPLIED
        )

        assertEquals(awkward, applied.getJSONObject("installMetadata").getString("weird\"key"))
    }

    @Test
    fun initialize_logsCompletionAsOneParseableJsonObject() {
        val complete = singleEvent(captureInitLogs(), Branch.EVENT_INITIALIZE_COMPLETE)

        assertEquals("DEBUG", complete.getString("logLevel"))
        assertTrue("completion must report the effective API URL",
            complete.getString("apiUrl").isNotEmpty())
        assertTrue("completion must report the SDK version",
            complete.getString("sdkVersion").isNotEmpty())
        assertTrue("completion must report whether auto-open is on",
            complete.getBoolean("automaticOpenEvents"))
    }

    @Test
    fun initialize_completionJson_reportsAutomaticOpenEventsDisabled() {
        val complete = singleEvent(
            captureInitLogs { setAutomaticOpenEvents(false) },
            Branch.EVENT_INITIALIZE_COMPLETE
        )

        assertFalse(complete.getBoolean("automaticOpenEvents"))
    }

    @Test
    fun initialize_logsNothingPerSettingAcrossMultipleLines() {
        val logs = captureInitLogs(BranchLogger.BranchLogLevel.VERBOSE) {
            setNetworkTimeout(15_000)
            setPreinstallPartner("samsung")
        }

        assertTrue(
            "configuration detail must stay on the JSON lines, not spread across many, got: $logs",
            logs.none { it.startsWith("BranchConfiguration:") }
        )
    }

    @Test
    fun initialize_errorLevel_logsNoConfigurationAtAll() {
        val logs = captureInitLogs(BranchLogger.BranchLogLevel.ERROR) { setNetworkTimeout(15_000) }

        assertTrue(
            "the quiet default must not narrate initialization, got: $logs",
            jsonEvents(logs, BranchConfiguration.EVENT_CONFIGURATION_APPLIED).isEmpty()
                    && jsonEvents(logs, Branch.EVENT_INITIALIZE_COMPLETE).isEmpty()
        )
    }

    @Test
    fun initialize_neverLogsTheWholeBranchKey() {
        val logs = captureInitLogs(BranchLogger.BranchLogLevel.VERBOSE)

        assertTrue("the branch key must be masked in logs, got: $logs",
            logs.none { it.contains(TEST_KEY) })
        assertEquals("key_test_...5678",
            singleEvent(logs, BranchConfiguration.EVENT_CONFIGURATION_APPLIED).getString("branchKey"))
    }

    private companion object {
        const val TEST_KEY = "key_test_abcdefghijklmnop12345678"
    }
}
