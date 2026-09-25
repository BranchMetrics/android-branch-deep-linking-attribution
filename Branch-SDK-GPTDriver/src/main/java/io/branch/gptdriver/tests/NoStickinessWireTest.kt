package io.branch.gptdriver.tests

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.branch.branchandroidtestbed.MainActivity
import java.io.File
import org.junit.After
import org.junit.Test

/** no_stickiness: delivers warm and confirms the chained open. Background, return and the verdict live in NoStickinessReturn. */
class NoStickinessWireTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val captureFile = File(context.filesDir, "branchlogs.txt")
    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun tearDown() {
        // Do not close(): after the warm-delivery startActivity call, the scenario can no
        // longer be driven to DESTROYED and close() throws. The runner reclaims the process.
    }

    @Test
    fun noStickinessAfterReturn() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val runId = System.currentTimeMillis().toString()
        Log.i(
            TAG,
            "apk_testbed=${NoStickinessSupport.apkIdentifier(context, context.packageName)} " +
                "apk_gptdriver=${NoStickinessSupport.apkIdentifier(context, instrumentation.context.packageName)} run_id=$runId"
        )

        check(NoStickinessSupport.awaitTokens(context)) {
            "No randomized tokens within the token wait; the install/open did not complete"
        }
        NoStickinessSupport.setAttributionFull(scenario!!, context)

        val link = NoStickinessSupport.generateLinkWithRetry(scenario!!, runId)
        check(link.startsWith("https://")) { "Expected an https link, got '$link'" }

        NoStickinessSupport.deliverWarmAndConfirm(context, captureFile, link, runId)

        // Background, return and the verdict read are added in later work; nothing past
        // delivery is asserted here yet.
        Log.i(TAG, "delivery and chained open confirmed for run_id=$runId")
    }

    private companion object {
        const val TAG = "NoStickinessWireTest"
    }
}
