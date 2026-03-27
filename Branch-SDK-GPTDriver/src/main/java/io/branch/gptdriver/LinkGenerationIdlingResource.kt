package io.branch.gptdriver

import android.widget.EditText
import androidx.test.espresso.IdlingResource

/**
 * Espresso IdlingResource that waits until the URL EditText is populated
 * with a valid Branch link (non-empty, starts with "https://").
 *
 * Usage:
 *   val idling = LinkGenerationIdlingResource(editText)
 *   IdlingRegistry.getInstance().register(idling)
 *   // ... perform assertions ...
 *   IdlingRegistry.getInstance().unregister(idling)
 */
class LinkGenerationIdlingResource(
    private val editText: EditText
) : IdlingResource {

    private var callback: IdlingResource.ResourceCallback? = null

    override fun getName(): String = "LinkGenerationIdlingResource"

    override fun isIdleNow(): Boolean {
        val text = editText.text?.toString().orEmpty()
        val idle = text.startsWith("https://")
        if (idle) {
            callback?.onTransitionToIdle()
        }
        return idle
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
    }
}
