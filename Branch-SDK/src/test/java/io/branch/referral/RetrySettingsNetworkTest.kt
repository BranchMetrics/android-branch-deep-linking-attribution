package io.branch.referral

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.net.InetAddress
import java.net.ServerSocket
import java.util.Collections
import kotlin.concurrent.thread

/**
 * The retry settings must reach the network layer, which is built before the configuration is
 * applied. Each attempt is one connection to a local socket that closes it at once, so the TLS
 * handshake fails with an IOException, which the network layer retries.
 */
class RetrySettingsNetworkTest : BranchTestBase() {

    private val context get() = RuntimeEnvironment.getApplication()

    private lateinit var server: ServerSocket
    private val attemptTimes: MutableList<Long> = Collections.synchronizedList(mutableListOf())

    @Before
    override fun setUpBase() {
        super.setUpBase()
        Branch.shutDown()
        server = ServerSocket(0, 50, InetAddress.getLoopbackAddress())
        thread(isDaemon = true) {
            while (!server.isClosed) {
                try {
                    server.accept().close()
                    attemptTimes.add(System.nanoTime())
                } catch (e: Exception) {
                    // Server closed in tearDown
                }
            }
        }
    }

    @After
    override fun tearDownBase() {
        server.close()
        super.tearDownBase()
        Branch.shutDown()
    }

    private fun post(): Int {
        try {
            Branch.getInstance().branchRemoteInterface
                .doRestfulPost("https://127.0.0.1:${server.localPort}/v1/test", JSONObject())
        } catch (e: Exception) {
            // Every attempt fails
        }
        return attemptTimes.size
    }

    @Test
    fun initialize_retryCount_reachesTheNetworkLayer() {
        Branch.initialize(context, BranchConfiguration.Builder("key_live_test123").setRetryCount(0).build())

        assertEquals(1, post())
    }

    @Test
    fun updateConfiguration_retryCount_reachesTheNetworkLayer() {
        Branch.initialize(context, BranchConfiguration.Builder("key_live_test123").setRetryCount(0).build())

        Branch.getInstance().updateConfiguration(BranchConfiguration.Builder().setRetryCount(1).build())

        assertEquals(2, post())
    }

    @Test
    fun updateConfiguration_retryInterval_reachesTheNetworkLayer() {
        Branch.initialize(context, BranchConfiguration.Builder("key_live_test123").setRetryCount(1).build())

        Branch.getInstance().updateConfiguration(BranchConfiguration.Builder().setRetryInterval(50).build())

        assertEquals(2, post())
        val gapMs = (attemptTimes[1] - attemptTimes[0]) / 1_000_000
        assertTrue("Retry waited ${gapMs}ms, expected about 50ms", gapMs in 50..500)
    }
}
