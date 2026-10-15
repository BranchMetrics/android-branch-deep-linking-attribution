package io.branch.referral

import io.branch.interfaces.IBranchLoggingCallbacks
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.json.JSONObject
import org.robolectric.RuntimeEnvironment

class BranchUpdateConfigurationTest : BranchTestBase() {

    private val context get() = RuntimeEnvironment.getApplication()
    private val prefHelper get() = PrefHelper.getInstance(context)

    @Before
    override fun setUpBase() {
        super.setUpBase()
        Branch.shutDown()
        resetLogger()
    }

    @After
    override fun tearDownBase() {
        super.tearDownBase()
        Branch.shutDown()
        resetLogger()
    }

    private fun resetLogger() {
        BranchLogger.loggingEnabled = true
        BranchLogger.loggingLevel = BranchLogger.BranchLogLevel.DEBUG
        BranchLogger.loggerCallback = null
    }

    private fun initialize(configure: BranchConfiguration.Builder.() -> Unit = {}) {
        Branch.initialize(context, BranchConfiguration.Builder("key_live_test123").apply(configure).build())
    }

    private fun update(configure: BranchConfiguration.Builder.() -> Unit) {
        Branch.getInstance().updateConfiguration(BranchConfiguration.Builder().apply(configure).build())
    }

    @Test
    fun updateConfiguration_changesOnlyTheSettingsItSets() {
        initialize {
            setNetworkTimeout(15_000)
            setUserAgentFetchSync(true)
            setApiUrl("https://proxy.example.com/")
        }

        update { setRetryCount(7) }

        assertEquals(7, prefHelper.getRetryCount())
        assertEquals(15_000, prefHelper.getTimeout())
        assertTrue(Branch.userAgentSync)
        assertEquals("https://proxy.example.com/", prefHelper.getAPIBaseUrl())
    }

    @Test
    fun updateConfiguration_apiUrl_reachesPrefHelper() {
        initialize()

        update { setApiUrl("https://proxy.example.com/") }

        assertEquals("https://proxy.example.com/", prefHelper.getAPIBaseUrl())
    }

    @Test
    fun updateConfiguration_turnsOffSettingsTurnedOnAtInitialize() {
        initialize {
            setEUEndpoint(true)
            setUserAgentFetchSync(true)
        }

        update {
            setEUEndpoint(false)
            setUserAgentFetchSync(false)
        }

        assertEquals(PrefHelper.BRANCH_BASE_URL_V2, prefHelper.getAPIBaseUrl())
        assertFalse(Branch.userAgentSync)
    }

    @Test
    fun updateConfiguration_attributionLevelFull_afterNone_turnsTrackingBackOn() {
        initialize { setAttributionLevel(Defines.BranchAttributionLevel.NONE) }
        assertTrue(Branch.getInstance().isTrackingDisabled)

        update { setAttributionLevel(Defines.BranchAttributionLevel.FULL) }

        assertEquals(Defines.BranchAttributionLevel.FULL, prefHelper.getConsumerProtectionAttributionLevel())
        assertFalse(Branch.getInstance().isTrackingDisabled)
    }

    @Test
    fun updateConfiguration_withoutAttributionLevel_keepsNone() {
        initialize { setAttributionLevel(Defines.BranchAttributionLevel.NONE) }

        update { setNetworkTimeout(9_000) }

        assertEquals(Defines.BranchAttributionLevel.NONE, prefHelper.getConsumerProtectionAttributionLevel())
        assertTrue(Branch.getInstance().isTrackingDisabled)
    }

    @Test
    fun updateConfiguration_dmaParameters_canRevokeConsentGrantedAtInitialize() {
        initialize {
            setDMAParameters(
                DMAParameters.Builder()
                    .setEeaRegion(true)
                    .setAdPersonalizationConsent(true)
                    .setAdUserDataUsageConsent(true)
                    .build()
            )
        }

        update { setDMAParameters(DMAParameters.Builder().setEeaRegion(true).build()) }

        assertTrue(prefHelper.getEEARegion())
        assertFalse(prefHelper.getAdPersonalizationConsent())
        assertFalse(prefHelper.getAdUserDataUsageConsent())
    }

    @Test
    fun updateConfiguration_logLevelOnly_keepsTheLoggingCallback() {
        val callback = IBranchLoggingCallbacks { _, _ -> }
        initialize { setLoggingCallback(callback) }

        update { setLogLevel(BranchLogger.BranchLogLevel.WARN) }

        assertSame(callback, BranchLogger.loggerCallback)
        assertEquals(BranchLogger.BranchLogLevel.WARN, BranchLogger.loggingLevel)
    }

    @Test
    fun updateConfiguration_loggingCallbackOnly_keepsTheLogLevel() {
        val callback = IBranchLoggingCallbacks { _, _ -> }
        initialize { setLogLevel(BranchLogger.BranchLogLevel.WARN) }

        update { setLoggingCallback(callback) }

        assertSame(callback, BranchLogger.loggerCallback)
        assertEquals(BranchLogger.BranchLogLevel.WARN, BranchLogger.loggingLevel)
    }

    @Test
    fun updateConfiguration_apiUrl_appliesToARequestCreatedBeforeTheUpdate() {
        initialize()
        val request = ServerRequestLogEvent(
            context, Defines.RequestPath.TrackCustomEvent, "my_event",
            HashMap(), JSONObject(), JSONObject(), emptyList()
        )

        update { setApiUrl("https://proxy.example.com/") }

        assertTrue(request.requestUrl, request.requestUrl.startsWith("https://proxy.example.com/"))
    }

    @Test
    fun updateConfiguration_whitelistedScheme_isAddedToTheOnesSetAtInitialize() {
        initialize { addWhitelistedScheme("first://.*") }

        update { addWhitelistedScheme("second://.*") }

        val analyser = UniversalResourceAnalyser.getInstance(context)
        assertEquals("first://a", analyser.getStrippedURL("first://a"))
        assertEquals("second://b", analyser.getStrippedURL("second://b"))
        assertNull(analyser.getStrippedURL("other://c"))
    }

    @Test
    fun updateConfiguration_initializeOnlySettings_areIgnoredWithAWarning() {
        val messages = mutableListOf<String>()
        initialize {
            setLogLevel(BranchLogger.BranchLogLevel.NONE)
            setLoggingCallback(IBranchLoggingCallbacks { message, _ -> messages.add(message) })
        }

        Branch.getInstance().updateConfiguration(
            BranchConfiguration.Builder("key_live_other").setTestMode(true).setPreinstallCampaign("campaign").build()
        )

        assertEquals("key_live_test123", prefHelper.getBranchKey())
        assertFalse(BranchUtil.isTestModeEnabled())
        assertTrue(messages.any {
            it.contains("ignored settings that only apply at initialize: [branchKey, testMode, preinstallCampaign]")
        })
    }

    @Test
    fun updateConfiguration_withoutInitializeOnlySettings_doesNotWarn() {
        val messages = mutableListOf<String>()
        initialize { setLoggingCallback(IBranchLoggingCallbacks { message, _ -> messages.add(message) }) }

        update { setRetryCount(1) }

        assertFalse(messages.any { it.contains("only apply at initialize") })
    }
}
