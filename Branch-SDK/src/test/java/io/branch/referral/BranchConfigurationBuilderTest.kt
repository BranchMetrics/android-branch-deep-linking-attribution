package io.branch.referral

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [BranchConfiguration.Builder] — covers builder defaults, field round-trips,
 * build() validation rules, and the companion factory methods.
 *
 * All tests are pure JVM (no Android framework needed).
 */
class BranchConfigurationBuilderTest {

    // -----------------------------------------------------------------------------------------
    // Defaults
    // -----------------------------------------------------------------------------------------

    @Test
    fun `build with only key uses expected defaults`() {
        val config = BranchConfiguration.Builder("key_live_abc").build()

        assertEquals("key_live_abc", config.branchKey)
        assertFalse(config.testMode)
        assertNull(config.apiUrl)
        assertNull(config.cdnBaseUrl)
        assertFalse(config.euEndpoint)
        assertEquals(BranchLogger.BranchLogLevel.NONE, config.logLevel)
        assertNull(config.loggingCallback)
        assertNull(config.requestTracingCallback)
        assertEquals(PrefHelper.TIMEOUT, config.networkTimeout)
        assertEquals(PrefHelper.CONNECT_TIMEOUT, config.networkConnectTimeout)
        assertEquals(PrefHelper.MAX_RETRIES, config.retryCount)
        assertNull(config.remoteInterface)
        assertNull(config.attributionLevel)
        assertNull(config.dmaParameters)
        assertFalse(config.limitFacebookAttribution)
        assertFalse(config.adNetworkCalloutsDisabled)
        assertNull(config.facebookAppId)
        assertNull(config.preinstallCampaign)
        assertNull(config.preinstallPartner)
        assertTrue(config.installMetadata.isEmpty())
        assertFalse(config.referringLinkAttributionForPreinstalledApps)
        assertTrue(config.whitelistedSchemes.isEmpty())
        assertTrue(config.uriHostsToSkip.isEmpty())
        assertTrue(config.automaticOpenEvents)
        assertFalse(config.userAgentFetchSync)
    }

    // -----------------------------------------------------------------------------------------
    // Field round-trips
    // -----------------------------------------------------------------------------------------

    @Test
    fun `setTestMode is stored`() {
        val config = BranchConfiguration.Builder("key_live_x").setTestMode(true).build()
        assertTrue(config.testMode)
    }

    @Test
    fun `setApiUrl is stored`() {
        val config = BranchConfiguration.Builder("key_live_x").setApiUrl("https://custom.api.io/").build()
        assertEquals("https://custom.api.io/", config.apiUrl)
    }

    @Test
    fun `setCdnBaseUrl is stored`() {
        val config = BranchConfiguration.Builder("key_live_x").setCdnBaseUrl("https://cdn.custom.io/").build()
        assertEquals("https://cdn.custom.io/", config.cdnBaseUrl)
    }

    @Test
    fun `setEUEndpoint true is stored`() {
        val config = BranchConfiguration.Builder("key_live_x").setEUEndpoint(true).build()
        assertTrue(config.euEndpoint)
    }

    @Test
    fun `setLogLevel is stored`() {
        val config = BranchConfiguration.Builder("key_live_x")
            .setLogLevel(BranchLogger.BranchLogLevel.VERBOSE)
            .build()
        assertEquals(BranchLogger.BranchLogLevel.VERBOSE, config.logLevel)
    }

    @Test
    fun `setNetworkTimeout is stored`() {
        val config = BranchConfiguration.Builder("key_live_x").setNetworkTimeout(10_000).build()
        assertEquals(10_000, config.networkTimeout)
    }

    @Test
    fun `setNetworkConnectTimeout is stored`() {
        val config = BranchConfiguration.Builder("key_live_x").setNetworkConnectTimeout(3_000).build()
        assertEquals(3_000, config.networkConnectTimeout)
    }

    @Test
    fun `setRetryCount is stored`() {
        val config = BranchConfiguration.Builder("key_live_x").setRetryCount(5).build()
        assertEquals(5, config.retryCount)
    }

    @Test
    fun `setRetryInterval is stored`() {
        val config = BranchConfiguration.Builder("key_live_x").setRetryInterval(2_000).build()
        assertEquals(2_000, config.retryInterval)
    }

    @Test
    fun `setNoConnectionRetryMax is stored`() {
        val config = BranchConfiguration.Builder("key_live_x").setNoConnectionRetryMax(5).build()
        assertEquals(5, config.noConnectionRetryMax)
    }

    @Test
    fun `setAttributionLevel is stored`() {
        val config = BranchConfiguration.Builder("key_live_x")
            .setAttributionLevel(Defines.BranchAttributionLevel.MINIMAL)
            .build()
        assertEquals(Defines.BranchAttributionLevel.MINIMAL, config.attributionLevel)
    }

    @Test
    fun `setDMAParameters is stored`() {
        val dma = DMAParameters.Builder()
            .setEeaRegion(true)
            .setAdUserDataUsageConsent(true)
            .build()
        val config = BranchConfiguration.Builder("key_live_x").setDMAParameters(dma).build()
        assertNotNull(config.dmaParameters)
        assertEquals(dma, config.dmaParameters)
    }

    @Test
    fun `setLimitFacebookAttribution true is stored`() {
        val config = BranchConfiguration.Builder("key_live_x").setLimitFacebookAttribution(true).build()
        assertTrue(config.limitFacebookAttribution)
    }

    @Test
    fun `setAdNetworkCalloutsDisabled true is stored`() {
        val config = BranchConfiguration.Builder("key_live_x").setAdNetworkCalloutsDisabled(true).build()
        assertTrue(config.adNetworkCalloutsDisabled)
    }

    @Test
    fun `setFacebookAppId is stored`() {
        val config = BranchConfiguration.Builder("key_live_x").setFacebookAppId("fb123").build()
        assertEquals("fb123", config.facebookAppId)
    }

    @Test
    fun `setPreinstallCampaign is stored`() {
        val config = BranchConfiguration.Builder("key_live_x").setPreinstallCampaign("organic").build()
        assertEquals("organic", config.preinstallCampaign)
    }

    @Test
    fun `setPreinstallPartner is stored`() {
        val config = BranchConfiguration.Builder("key_live_x").setPreinstallPartner("samsung").build()
        assertEquals("samsung", config.preinstallPartner)
    }

    @Test
    fun `addInstallMetadata accumulates entries`() {
        val config = BranchConfiguration.Builder("key_live_x")
            .addInstallMetadata("store", "galaxy_store")
            .addInstallMetadata("campaign", "q1")
            .build()
        assertEquals("galaxy_store", config.installMetadata["store"])
        assertEquals("q1", config.installMetadata["campaign"])
        assertEquals(2, config.installMetadata.size)
    }

    @Test
    fun `addInstallMetadata overwrites duplicate key`() {
        val config = BranchConfiguration.Builder("key_live_x")
            .addInstallMetadata("store", "first")
            .addInstallMetadata("store", "second")
            .build()
        assertEquals("second", config.installMetadata["store"])
        assertEquals(1, config.installMetadata.size)
    }

    @Test
    fun `setReferringLinkAttributionForPreinstalledApps true is stored`() {
        val config = BranchConfiguration.Builder("key_live_x")
            .setReferringLinkAttributionForPreinstalledApps(true)
            .build()
        assertTrue(config.referringLinkAttributionForPreinstalledApps)
    }

    @Test
    fun `addWhitelistedScheme accumulates schemes`() {
        val config = BranchConfiguration.Builder("key_live_x")
            .addWhitelistedScheme("myapp://")
            .addWhitelistedScheme("otherapp://")
            .build()
        assertEquals(listOf("myapp://", "otherapp://"), config.whitelistedSchemes)
    }

    @Test
    fun `addUriHostToSkip accumulates hosts`() {
        val config = BranchConfiguration.Builder("key_live_x")
            .addUriHostToSkip("internal.example.com")
            .addUriHostToSkip("skip.example.com")
            .build()
        assertEquals(listOf("internal.example.com", "skip.example.com"), config.uriHostsToSkip)
    }

    @Test
    fun `setAutomaticOpenEvents false is stored`() {
        val config = BranchConfiguration.Builder("key_live_x").setAutomaticOpenEvents(false).build()
        assertFalse(config.automaticOpenEvents)
    }

    @Test
    fun `setUserAgentFetchSync true is stored`() {
        val config = BranchConfiguration.Builder("key_live_x").setUserAgentFetchSync(true).build()
        assertTrue(config.userAgentFetchSync)
    }

    // -----------------------------------------------------------------------------------------
    // build() returns immutable copies of mutable collections
    // -----------------------------------------------------------------------------------------

    @Test
    fun `installMetadata is immutable after build`() {
        val builder = BranchConfiguration.Builder("key_live_x").addInstallMetadata("k", "v")
        val config = builder.build()
        // Modifying builder after build does not affect the config
        builder.addInstallMetadata("k2", "v2")
        assertFalse(config.installMetadata.containsKey("k2"))
    }

    @Test
    fun `whitelistedSchemes is immutable after build`() {
        val builder = BranchConfiguration.Builder("key_live_x").addWhitelistedScheme("myapp://")
        val config = builder.build()
        builder.addWhitelistedScheme("other://")
        assertEquals(1, config.whitelistedSchemes.size)
    }

    @Test
    fun `uriHostsToSkip is immutable after build`() {
        val builder = BranchConfiguration.Builder("key_live_x").addUriHostToSkip("a.com")
        val config = builder.build()
        builder.addUriHostToSkip("b.com")
        assertEquals(1, config.uriHostsToSkip.size)
    }

    // -----------------------------------------------------------------------------------------
    // build() validation — Branch key
    // -----------------------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `empty branch key throws IllegalArgumentException`() {
        BranchConfiguration.Builder("").build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank branch key throws IllegalArgumentException`() {
        BranchConfiguration.Builder("   ").build()
    }

    @Test
    fun `empty key error message mentions dashboard`() {
        val ex = runCatching { BranchConfiguration.Builder("").build() }
            .exceptionOrNull() as? IllegalArgumentException
        assertNotNull(ex)
        assertTrue(ex!!.message!!.contains("dashboard"))
    }

    // -----------------------------------------------------------------------------------------
    // build() validation — network timeout
    // -----------------------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `zero network timeout throws`() {
        BranchConfiguration.Builder("key_live_x").setNetworkTimeout(0).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative network timeout throws`() {
        BranchConfiguration.Builder("key_live_x").setNetworkTimeout(-1).build()
    }

    @Test
    fun `large network timeout is accepted`() {
        val config = BranchConfiguration.Builder("key_live_x").setNetworkTimeout(120_000).build()
        assertEquals(120_000, config.networkTimeout)
    }

    @Test
    fun `network timeout error message includes invalid value`() {
        val ex = runCatching {
            BranchConfiguration.Builder("key_live_x").setNetworkTimeout(-500).build()
        }.exceptionOrNull() as? IllegalArgumentException
        assertNotNull(ex)
        assertTrue(ex!!.message!!.contains("-500"))
    }

    // -----------------------------------------------------------------------------------------
    // build() validation — connect timeout
    // -----------------------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `zero connect timeout throws`() {
        BranchConfiguration.Builder("key_live_x").setNetworkConnectTimeout(0).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative connect timeout throws`() {
        BranchConfiguration.Builder("key_live_x").setNetworkConnectTimeout(-100).build()
    }

    @Test
    fun `large connect timeout is accepted`() {
        val config = BranchConfiguration.Builder("key_live_x").setNetworkConnectTimeout(120_000).build()
        assertEquals(120_000, config.networkConnectTimeout)
    }

    // -----------------------------------------------------------------------------------------
    // build() validation — retry count
    // -----------------------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `negative retry count throws`() {
        BranchConfiguration.Builder("key_live_x").setRetryCount(-1).build()
    }

    @Test
    fun `zero retry count is accepted`() {
        val config = BranchConfiguration.Builder("key_live_x").setRetryCount(0).build()
        assertEquals(0, config.retryCount)
    }

    // -----------------------------------------------------------------------------------------
    // build() validation — retry interval
    // -----------------------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `zero retry interval throws`() {
        BranchConfiguration.Builder("key_live_x").setRetryInterval(0).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative retry interval throws`() {
        BranchConfiguration.Builder("key_live_x").setRetryInterval(-1).build()
    }

    @Test
    fun `large retry interval is accepted`() {
        val config = BranchConfiguration.Builder("key_live_x").setRetryInterval(120_000).build()
        assertEquals(120_000, config.retryInterval)
    }

    @Test
    fun `retry interval error message includes invalid value`() {
        val ex = runCatching {
            BranchConfiguration.Builder("key_live_x").setRetryInterval(0).build()
        }.exceptionOrNull() as? IllegalArgumentException
        assertNotNull(ex)
        assertTrue(ex!!.message!!.contains("0"))
    }

    // -----------------------------------------------------------------------------------------
    // build() validation — no-connection retry max
    // -----------------------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `zero no-connection retry max throws`() {
        BranchConfiguration.Builder("key_live_x").setNoConnectionRetryMax(0).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative no-connection retry max throws`() {
        BranchConfiguration.Builder("key_live_x").setNoConnectionRetryMax(-1).build()
    }

    @Test
    fun `no-connection retry max of one is accepted`() {
        val config = BranchConfiguration.Builder("key_live_x").setNoConnectionRetryMax(1).build()
        assertEquals(1, config.noConnectionRetryMax)
    }

    // -----------------------------------------------------------------------------------------
    // build() reports every failure at once
    // -----------------------------------------------------------------------------------------

    @Test
    fun `build reports every invalid field in one exception`() {
        val ex = runCatching {
            BranchConfiguration.Builder("")
                .setNetworkTimeout(-1)
                .setRetryInterval(0)
                .build()
        }.exceptionOrNull() as? IllegalArgumentException

        assertNotNull(ex)
        val message = ex!!.message!!
        assertTrue("branch key failure missing: $message", message.contains("Branch key cannot be empty"))
        assertTrue("timeout failure missing: $message", message.contains("Network timeout"))
        assertTrue("retry interval failure missing: $message", message.contains("Retry interval"))
    }

    @Test
    fun `build reports a single failure without listing others`() {
        val ex = runCatching {
            BranchConfiguration.Builder("key_live_x").setRetryInterval(0).build()
        }.exceptionOrNull() as? IllegalArgumentException

        assertNotNull(ex)
        val message = ex!!.message!!
        assertTrue(message.contains("Retry interval"))
        assertFalse(message.contains("Network timeout"))
        assertFalse(message.contains("Branch key"))
    }

    // -----------------------------------------------------------------------------------------
    // Builder chaining
    // -----------------------------------------------------------------------------------------

    @Test
    fun `all setters return the builder for chaining`() {
        // Verifies that the fluent API compiles and produces a valid config
        val config = BranchConfiguration.Builder("key_live_x")
            .setTestMode(true)
            .setApiUrl("https://api.example.com/")
            .setCdnBaseUrl("https://cdn.example.com/")
            .setEUEndpoint(false)
            .setLogLevel(BranchLogger.BranchLogLevel.DEBUG)
            .setNetworkTimeout(15_000)
            .setNetworkConnectTimeout(5_000)
            .setRetryCount(3)
            .setRetryInterval(1_500)
            .setNoConnectionRetryMax(2)
            .setAttributionLevel(Defines.BranchAttributionLevel.FULL)
            .setDMAParameters(DMAParameters.Builder().setEeaRegion(true).setAdPersonalizationConsent(true).build())
            .setLimitFacebookAttribution(true)
            .setAdNetworkCalloutsDisabled(false)
            .setFacebookAppId("fb999")
            .setPreinstallCampaign("spring")
            .setPreinstallPartner("samsung")
            .addInstallMetadata("source", "oem")
            .setReferringLinkAttributionForPreinstalledApps(true)
            .addWhitelistedScheme("example://")
            .addUriHostToSkip("skip.example.com")
            .setAutomaticOpenEvents(false)
            .setUserAgentFetchSync(true)
            .build()

        assertEquals("key_live_x", config.branchKey)
        assertTrue(config.testMode)
        assertEquals(Defines.BranchAttributionLevel.FULL, config.attributionLevel)
        assertFalse(config.automaticOpenEvents)
    }

    // -------------------------------------------------------------------------
    // toString(): the DEBUG summary line
    // -------------------------------------------------------------------------

    @Test
    fun `toString lists only non-default settings`() {
        val summary = BranchConfiguration.Builder("key_live_abcdefghijklmnop")
            .setNetworkTimeout(15_000)
            .build()
            .toString()

        assertTrue("must name the setting that was changed: $summary",
            summary.contains("networkTimeout=15000"))
        assertFalse("must omit settings left at their default: $summary",
            summary.contains("retryCount="))
        assertFalse("must omit settings left at their default: $summary",
            summary.contains("euEndpoint="))
    }

    @Test
    fun `toString masks the branch key`() {
        val key = "key_live_abcdefghijklmnop"
        val summary = BranchConfiguration.Builder(key).build().toString()

        assertFalse("the whole key must not appear: $summary", summary.contains(key))
        assertTrue("the key prefix is the useful part: $summary", summary.contains("key_live_"))
        assertTrue("the last few characters identify the key: $summary", summary.contains("mnop"))
    }

    @Test
    fun `toString on an unconfigured builder lists only the key`() {
        val summary = BranchConfiguration.Builder("key_live_abcdefghijklmnop").build().toString()

        assertEquals("BranchConfiguration(branchKey=key_live_...mnop)", summary)
    }
}
