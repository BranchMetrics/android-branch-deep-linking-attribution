package io.branch.spike.receiver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "EMT3881_SPIKE_RX"

/**
 * Oracle activity for the EMT-3881 replacement-extras spike.
 *
 * Registers as an ACTION_SEND / text-plain target so it appears in the OS
 * chooser fired by the sender app. Whatever EXTRA_TEXT it actually receives
 * is the ground truth for whether this device's chooser honors
 * Intent.EXTRA_REPLACEMENT_EXTRAS: baseline text = not honored, a
 * "SPIKE|pkg=io.branch.spike.receiver|token=..." string = honored.
 */
class ReceiverActivity : AppCompatActivity() {

    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        logView = TextView(this).apply {
            textSize = 16f
            setTextIsSelectable(true)
        }
        container.addView(logView)
        setContentView(ScrollView(this).apply { addView(container) })

        renderIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        renderIntent(intent)
    }

    private fun renderIntent(receivedIntent: Intent?) {
        val text = receivedIntent?.getStringExtra(Intent.EXTRA_TEXT)
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = "[$timestamp] EXTRA_TEXT = ${text ?: "<null>"}"

        Log.i(TAG, entry)
        logView.text = if (logView.text.isNullOrEmpty()) {
            entry
        } else {
            "$entry\n\n${logView.text}"
        }
    }
}
