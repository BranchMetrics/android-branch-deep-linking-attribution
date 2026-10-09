package io.branch.referral

import io.branch.interfaces.IBranchLoggingCallbacks
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
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
            setAdNetworkCalloutsDisabled(true)
            setApiUrl("https://proxy.example.com/")
        }

        update { setRetryCount(7) }

        assertEquals(7, prefHelper.getRetryCount())
        assertEquals(15_000, prefHelper.getTimeout())
        assertTrue(prefHelper.getAdNetworkCalloutsDisabled())
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
            setAdNetworkCalloutsDisabled(true)
            setLimitFacebookAttribution(true)
            setEUEndpoint(true)
            setUserAgentFetchSync(true)
        }

        update {
            setAdNetworkCalloutsDisabled(false)
            setLimitFacebookAttribution(false)
            setEUEndpoint(false)
            setUserAgentFetchSync(false)
        }

        assertFalse(prefHelper.getAdNetworkCalloutsDisabled())
        assertFalse(prefHelper.isAppTrackingLimited())
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
}
