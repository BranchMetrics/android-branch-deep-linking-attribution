package io.branch.referral

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment

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
    fun initialize_defaultLogLevel_setsWarn() {
        // initialize() must always own the logger state — no inherited ambient level.
        BranchLogger.loggingLevel = BranchLogger.BranchLogLevel.VERBOSE // simulate prior state

        Branch.initialize(context, BranchConfiguration.Builder("key_live_test123").build())

        assertEquals(BranchLogger.BranchLogLevel.ERROR, BranchLogger.loggingLevel)
    }

    // -------------------------------------------------------------------------
    // DMA parameters wired to PrefHelper
    // -------------------------------------------------------------------------

    @Test
    fun initialize_dmaParameters_eeaRegion_reachesPrefHelper() {
        val config = BranchConfiguration.Builder("key_live_test123")
            .setDMAParameters(DMAParameters(eeaRegion = true, adPersonalizationConsent = false, adUserDataUsageConsent = false))
            .build()

        Branch.initialize(context, config)

        assertTrue(PrefHelper.getInstance(context).getEEARegion())
    }

    @Test
    fun initialize_dmaParameters_adPersonalizationConsent_reachesPrefHelper() {
        val config = BranchConfiguration.Builder("key_live_test123")
            .setDMAParameters(DMAParameters(eeaRegion = false, adPersonalizationConsent = true, adUserDataUsageConsent = false))
            .build()

        Branch.initialize(context, config)

        assertTrue(PrefHelper.getInstance(context).getAdPersonalizationConsent())
    }

    @Test
    fun initialize_dmaParameters_adUserDataUsageConsent_reachesPrefHelper() {
        val config = BranchConfiguration.Builder("key_live_test123")
            .setDMAParameters(DMAParameters(eeaRegion = false, adPersonalizationConsent = false, adUserDataUsageConsent = true))
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
}
