package io.branch.referral

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.branch.coroutines.fetchLatestInstallReferrer
import io.branch.coroutines.getGooglePlayStoreReferrerDetails
import io.branch.data.InstallReferrerResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Fetches from Branch-SDK-TestPlayStore, which stalls 10 s, and checks the main thread stays free.
 * Skips unless it's installed (adb install --force-queryable); requireTestPlayStore=true fails instead.
 */
@RunWith(AndroidJUnit4::class)
class InstallReferrerSlowPlayStoreTests : BranchTest() {

    // Must match GetInstallReferrerService.
    private val testStoreVersionName = "test-play-store"
    private val testStoreDelayMs = 10_000L
    private val testStoreReferrer = "utm_source=test_play_store&utm_medium=test"
    private val testStoreResult = InstallReferrerResult(Defines.Jsonkey.Google_Play_Store.key,
        1790000060L, testStoreReferrer, 1790000000L, 1790000061L, 1790000001L)
    private val testStoreService = "com.android.vending/com.google.android.finsky.externalreferrer.GetInstallReferrerService"

    private val maxMainThreadGapMs = 2_000L

    @Before
    fun requireTestPlayStore() {
        val installed = try {
            mContext.packageManager.getPackageInfo("com.android.vending", 0).versionName == testStoreVersionName
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        if (InstrumentationRegistry.getArguments().getString("requireTestPlayStore") == "true") {
            assertTrue("Branch-SDK-TestPlayStore is not installed as com.android.vending", installed)
        } else {
            assumeTrue(installed)
        }
        configureTestStore("--ez reset true")
    }

    @Test
    fun slowPlayStoreDoesNotBlockTheMainThreadInGooglePlayFetch() {
        assertFetchKeepsMainThreadFree { getGooglePlayStoreReferrerDetails(mContext) }
    }

    @Test
    fun slowPlayStoreDoesNotBlockTheMainThreadInLatestReferrerFetch() {
        assertFetchKeepsMainThreadFree { fetchLatestInstallReferrer(mContext) }
    }

    @Test
    fun slowPlayStoreTimeoutReturnsOnTimeWithoutBlockingTheMainThread() {
        val timeoutMs = 3_000L
        PrefHelper.getInstance(mContext).installReferrerTimeout = timeoutMs.toInt()
        val heartbeat = MainThreadHeartbeat()
        heartbeat.start()
        val startedAt = SystemClock.uptimeMillis()
        val result = runBlocking { withTimeout(60_000) { fetchLatestInstallReferrer(mContext) } }
        val elapsedMs = SystemClock.uptimeMillis() - startedAt
        val stallingAtTimeout = testStoreIsBound()
        // Keep beating until the abandoned read finishes.
        val released = testStoreReleasedWithin(testStoreDelayMs + 5_000)
        val longestGapMs = heartbeat.stop()
        Log.i("InstallReferrerSlowPlayStoreTests", "timed out fetch took ${elapsedMs}ms, longest main thread gap ${longestGapMs}ms")

        assertTrue("the fetch took ${elapsedMs}ms with a ${timeoutMs}ms timeout", elapsedMs < timeoutMs + 1_500)
        assertTrue("the test Play Store was not stalling when the timeout fired", stallingAtTimeout)
        assertNull("the fetch returned a referrer despite the timeout", result)
        assertTrue("the main thread was blocked for ${longestGapMs}ms", longestGapMs < maxMainThreadGapMs)
        assertTrue("the connection to the Play Store was not released", released)
    }

    @Test
    fun configuredReferrerAndDelayAreAnswered() {
        // No spaces: executeShellCommand splits on whitespace without a shell.
        val referrer = "link_click_id=test-click-id&utm_source=configured"
        configureTestStore("--es referrer $referrer --el delay_ms 0")
        try {
            val startedAt = SystemClock.uptimeMillis()
            val result = runBlocking { withTimeout(30_000) { getGooglePlayStoreReferrerDetails(mContext) } }
            val elapsedMs = SystemClock.uptimeMillis() - startedAt

            assertEquals(referrer, result?.installReferrer)
            assertTrue("the fetch took ${elapsedMs}ms with no delay configured", elapsedMs < testStoreDelayMs / 2)
        } finally {
            configureTestStore("--ez reset true")
        }
    }

    private fun assertFetchKeepsMainThreadFree(fetch: suspend () -> InstallReferrerResult?) {
        val heartbeat = MainThreadHeartbeat()
        heartbeat.start()
        val startedAt = SystemClock.uptimeMillis()
        val result = runBlocking { withTimeout(60_000) { fetch() } }
        val elapsedMs = SystemClock.uptimeMillis() - startedAt
        val longestGapMs = heartbeat.stop()
        Log.i("InstallReferrerSlowPlayStoreTests", "fetch took ${elapsedMs}ms, longest main thread gap ${longestGapMs}ms, referrer ${result?.installReferrer}")

        // The first two checks prove the test Play Store answered and stalled.
        assertEquals("the test Play Store did not answer", testStoreReferrer, result?.installReferrer)
        assertTrue("the fetch took ${elapsedMs}ms, so the test Play Store did not stall", elapsedMs >= testStoreDelayMs - 500)
        assertTrue("the main thread was blocked for ${longestGapMs}ms", longestGapMs < maxMainThreadGapMs)
        assertEquals("the referrer fields do not match what the test Play Store answered", testStoreResult, result)
        assertTrue("the connection to the Play Store was not released", testStoreReleasedWithin(5_000))
    }

    private fun shell(command: String): String {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().use { it.readText() }
    }

    private fun configureTestStore(extras: String) {
        val output = shell("am broadcast --include-stopped-packages -n com.android.vending/.ConfigureReferrer $extras")
        assertTrue("the test Play Store was not configured: $output", output.contains("result=-1"))
    }

    private fun testStoreIsBound(): Boolean {
        return shell("dumpsys activity services $testStoreService").lines().any { it.contains("ServiceRecord{") && it.contains(testStoreService) }
    }

    private fun testStoreReleasedWithin(timeoutMs: Long): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (testStoreIsBound()) {
            if (SystemClock.uptimeMillis() > deadline) return false
            SystemClock.sleep(100)
        }
        return true
    }

    private class MainThreadHeartbeat {
        private val handler = Handler(Looper.getMainLooper())
        @Volatile private var running = true
        // Only touched on the main thread.
        private var lastBeatAt = 0L
        private var longestGapMs = 0L

        private val beat = object : Runnable {
            override fun run() {
                val now = SystemClock.uptimeMillis()
                if (lastBeatAt != 0L) longestGapMs = maxOf(longestGapMs, now - lastBeatAt)
                lastBeatAt = now
                if (running) handler.postDelayed(this, 50)
            }
        }

        fun start() {
            handler.post(beat)
        }

        fun stop(): Long {
            running = false
            val longest = AtomicLong()
            val measured = CountDownLatch(1)
            handler.post {
                longest.set(maxOf(longestGapMs, SystemClock.uptimeMillis() - lastBeatAt))
                measured.countDown()
            }
            assertTrue("the main thread did not respond within 60 s", measured.await(60, TimeUnit.SECONDS))
            return longest.get()
        }
    }
}
