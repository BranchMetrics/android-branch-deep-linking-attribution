package io.branch.spike.sender

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import java.util.UUID
import kotlinx.coroutines.launch

private const val TAG = "EMT3881_SPIKE_TX"
private const val BASELINE_TEXT = "SPIKE-BASELINE (no per-target override applied)"

/**
 * Fires the two probes this spike exists for:
 *  1. A chooser Intent carrying Intent.EXTRA_REPLACEMENT_EXTRAS, to find out
 *     whether this device's chooser actually swaps EXTRA_TEXT per target
 *     package before launch (see README.md for the AOSP behavior assumed).
 *  2. An N-parallel-HTTP-request latency probe, a proxy for "how long would
 *     it take to mint N per-target short links before the sheet can open".
 */
class SenderActivity : AppCompatActivity() {

    private lateinit var chooserExpectationLog: TextView
    private lateinit var latencyCountInput: EditText
    private lateinit var latencyEndpointInput: EditText
    private lateinit var latencyResultView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sender)

        chooserExpectationLog = findViewById(R.id.chooserExpectationLog)
        latencyCountInput = findViewById(R.id.latencyCountInput)
        latencyEndpointInput = findViewById(R.id.latencyEndpointInput)
        latencyResultView = findViewById(R.id.latencyResultView)

        findViewById<Button>(R.id.fireChooserButton).setOnClickListener { fireChooser() }
        findViewById<Button>(R.id.runLatencyButton).setOnClickListener { runLatencyProbe() }
    }

    /** Fires ACTION_SEND wrapped in a chooser with per-target replacement EXTRA_TEXT. */
    private fun fireChooser() {
        val expectations = StringBuilder()
        val replacementExtras = Bundle()

        for (pkg in TargetCatalog.PACKAGES) {
            val token = UUID.randomUUID().toString().take(8)
            val replacementText = "SPIKE|pkg=$pkg|token=$token"
            replacementExtras.putBundle(
                pkg,
                Bundle().apply { putString(Intent.EXTRA_TEXT, replacementText) },
            )
            expectations.append("$pkg -> $replacementText\n")
        }

        chooserExpectationLog.text =
            "Baseline: $BASELINE_TEXT\n\nExpected per-target replacements:\n$expectations"
        Log.i(TAG, chooserExpectationLog.text.toString())

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, BASELINE_TEXT)
        }

        val chooserIntent = Intent.createChooser(sendIntent, "EMT-3881 spike: pick a target").apply {
            putExtra(Intent.EXTRA_REPLACEMENT_EXTRAS, replacementExtras)
        }

        startActivity(chooserIntent)
    }

    /** Fires N parallel GET requests against a configurable endpoint and reports p50/p95. */
    private fun runLatencyProbe() {
        val n = latencyCountInput.text.toString().toIntOrNull() ?: 8
        val endpoint = latencyEndpointInput.text.toString().trim()
        if (endpoint.isEmpty()) {
            latencyResultView.text = "Set an endpoint URL first."
            return
        }

        latencyResultView.text = "Running $n parallel requests against $endpoint ..."
        lifecycleScope.launch {
            val result = LatencyProbe.run(n, endpoint)
            latencyResultView.text = "N=${result.n}  wall=${result.wallClockMs.fmt()}ms  " +
                "p50=${result.p50Ms.fmt()}ms  p95=${result.p95Ms.fmt()}ms  " +
                "failures=${result.failures}\n\n" +
                "Reminder: this is a proxy measurement against $endpoint, NOT real " +
                "Branch short-link creation latency."
            Log.i(TAG, latencyResultView.text.toString())
        }
    }

    private fun Double.fmt(): String = if (isNaN()) "NaN" else "%.0f".format(this)
}
