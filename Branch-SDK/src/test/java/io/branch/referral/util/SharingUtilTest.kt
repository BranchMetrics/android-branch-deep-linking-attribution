package io.branch.referral.util

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import io.branch.referral.BranchTestBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * EMT-3881: [SharingUtil.share] builds the chooser [Intent] Robolectric can inspect via
 * [org.robolectric.shadows.ShadowActivity], so no mocking is needed here.
 *
 * Pinned to SDK 22 and 33 rather than inheriting the base class default. `Intent.createChooser`'s
 * three argument overload only exists from API 22, which is why [SharingUtil.share] carries
 * `@RequiresApi(LOLLIPOP_MR1)`; running at the module's minSdk of 21 throws NoSuchMethodError.
 * Exercising the lowest supported level and a modern one proves the contract holds across the range.
 */
@Config(manifest = Config.NONE, sdk = [22, 33])
class SharingUtilTest : BranchTestBase() {

    private fun buildActivity(): Activity = Robolectric.buildActivity(Activity::class.java).create().get()

    @Test
    fun `share with a null replacement bundle does not set EXTRA_REPLACEMENT_EXTRAS`() {
        val activity = buildActivity()

        SharingUtil.share("https://example.app.link/abc", "title", "subject", activity, null)

        val chooserIntent = shadowOf(activity).nextStartedActivityForResult.intent
        assertFalse(chooserIntent.hasExtra(Intent.EXTRA_REPLACEMENT_EXTRAS))
    }

    @Test
    fun `share with an empty replacement bundle does not set EXTRA_REPLACEMENT_EXTRAS`() {
        val activity = buildActivity()

        SharingUtil.share("https://example.app.link/abc", "title", "subject", activity, Bundle())

        val chooserIntent = shadowOf(activity).nextStartedActivityForResult.intent
        assertFalse(chooserIntent.hasExtra(Intent.EXTRA_REPLACEMENT_EXTRAS))
    }

    @Test
    fun `share with a non-empty replacement bundle sets EXTRA_REPLACEMENT_EXTRAS on the chooser intent`() {
        val activity = buildActivity()
        val replacementExtras = Bundle().apply {
            putBundle(
                "com.whatsapp",
                Bundle().apply { putString(Intent.EXTRA_TEXT, "https://example.app.link/whatsapp") }
            )
        }

        SharingUtil.share("https://example.app.link/abc", "title", "subject", activity, replacementExtras)

        val chooserIntent = shadowOf(activity).nextStartedActivityForResult.intent
        assertTrue(chooserIntent.hasExtra(Intent.EXTRA_REPLACEMENT_EXTRAS))

        val actual = chooserIntent.getBundleExtra(Intent.EXTRA_REPLACEMENT_EXTRAS)
        assertNotNull(actual)
        assertEquals(
            "https://example.app.link/whatsapp",
            actual!!.getBundle("com.whatsapp")?.getString(Intent.EXTRA_TEXT)
        )
    }
}
