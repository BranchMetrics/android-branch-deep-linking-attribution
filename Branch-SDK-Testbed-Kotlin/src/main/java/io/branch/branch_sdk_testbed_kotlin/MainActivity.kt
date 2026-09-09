package io.branch.branch_sdk_testbed_kotlin

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.branch.branch_sdk_testbed_kotlin.TestbedApp.Companion.TAG
import io.branch.indexing.BranchUniversalObject
import io.branch.indexing.createLink
import io.branch.referral.Branch
import io.branch.referral.BranchException
import io.branch.referral.BranchShortLinkBuilder
import io.branch.referral.awaitLogEvent
import io.branch.referral.createLink
import io.branch.referral.requestDeepLinkData
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import io.branch.referral.util.BranchEvent
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * Exercises the Kotlin suspend variants against the live Branch API. Every button result is
 * mirrored to Logcat under the "BranchCoroutines" tag and to the on-screen log.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var logView: TextView
    private lateinit var logScroll: ScrollView

    /** Fed by createLink so the deep-link button resolves a link this app actually made. */
    private var lastCreatedLink: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        logView = findViewById(R.id.log)
        logScroll = findViewById(R.id.log_scroll)

        findViewById<Button>(R.id.btn_buo_create_link).setOnClickListener {
            run("BranchUniversalObject.createLink") {
                buo().createLink(this@MainActivity, linkProperties())
                    .also { lastCreatedLink = it }
            }
        }

        findViewById<Button>(R.id.btn_builder_create_link).setOnClickListener {
            run("BranchShortLinkBuilder.createLink") {
                BranchShortLinkBuilder(this@MainActivity)
                    .setChannel("kotlin-testbed")
                    .setFeature("suspend-builder")
                    .createLink()
                    .also { lastCreatedLink = it }
            }
        }

        // A fixed alias plus data that changes every tap is a genuine duplicate-alias
        // conflict, which is what makes the two defaultToLongUrl paths observable.
        findViewById<Button>(R.id.btn_throws).setOnClickListener {
            run("createLink(defaultToLongUrl=false) on alias conflict") {
                buo().createLink(this@MainActivity, conflictingProperties(), defaultToLongUrl = false)
            }
        }

        findViewById<Button>(R.id.btn_long_url_fallback).setOnClickListener {
            run("createLink(defaultToLongUrl=true) on alias conflict") {
                buo().createLink(this@MainActivity, conflictingProperties(), defaultToLongUrl = true)
            }
        }

        findViewById<Button>(R.id.btn_log_event).setOnClickListener {
            run("BranchEvent.awaitLogEvent") {
                BranchEvent(BRANCH_STANDARD_EVENT.PURCHASE)
                    .setCurrency(io.branch.referral.util.CurrencyType.USD)
                    .setRevenue(9.99)
                    .awaitLogEvent(this@MainActivity)
                "logged (no throw)"
            }
        }

        findViewById<Button>(R.id.btn_request_deep_link).setOnClickListener {
            val target = lastCreatedLink ?: FALLBACK_LINK
            run("Branch.requestDeepLinkData($target)") {
                Branch.getInstance().requestDeepLinkData(Uri.parse(target)).toString(2)
            }
        }

        findViewById<Button>(R.id.btn_cancel_deep_link).setOnClickListener {
            cancelInFlightDeepLink()
        }

        findViewById<Button>(R.id.btn_clear).setOnClickListener { logView.text = "" }

        log("ready — session state: ${Branch.getInstance().initState}")
    }

    /** Runs [block] on the main dispatcher, proving the suspend variants are main-safe. */
    private fun run(label: String, block: suspend () -> String): Job = lifecycleScope.launch {
        log("▶ $label")
        try {
            log("✓ $label\n   ${block()}")
        } catch (e: BranchException) {
            log("✗ $label\n   BranchException code=${e.branchError.errorCode} ${e.message}")
        } catch (e: Exception) {
            log("✗ $label\n   ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /**
     * Cancels mid-flight to show the documented de-queue: the callback never fires and the
     * queue drops the unsent request.
     */
    private fun cancelInFlightDeepLink() = lifecycleScope.launch {
        val target = lastCreatedLink ?: FALLBACK_LINK
        log("▶ requestDeepLinkData then cancel")

        val job = launch {
            val params = Branch.getInstance().requestDeepLinkData(Uri.parse(target))
            log("✗ resumed anyway — cancellation did not take: $params")
        }
        // yield() lets the child enqueue and suspend; cancelling on a timer instead races the
        // response and can land after it completes.
        yield()
        job.cancel()
        job.join()
        log("✓ cancelled — queue depth now ${Branch.getInstance().requestQueue_.getSize()}")
    }

    private fun buo() = BranchUniversalObject()
        .setCanonicalIdentifier("kotlin-testbed/item/1")
        .setTitle("Kotlin suspend variants")
        .setContentDescription("Created from the Kotlin testbed")

    /** Fixed alias, changing data — the second tap onward is a real 409 from the server. */
    private fun conflictingProperties() = linkProperties()
        .setAlias("kotlin-suspend-conflict")
        .addControlParameter("nonce", System.currentTimeMillis().toString())

    private fun linkProperties() = LinkProperties()
        .setChannel("kotlin-testbed")
        .setFeature("suspend-buo")
        .addControlParameter("\$desktop_url", "https://branch.io")

    private fun log(message: String) {
        Log.i(TAG, message)
        runOnUiThread {
            logView.append("$message\n")
            logScroll.post { logScroll.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private companion object {
        const val FALLBACK_LINK = "https://bnc.lt/Ojqd"
    }
}
