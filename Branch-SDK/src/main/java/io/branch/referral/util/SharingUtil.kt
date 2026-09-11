package io.branch.referral.util

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import io.branch.receivers.SharingBroadcastReceiver

object SharingUtil {
    var sharedURL: String? = ""

    @JvmStatic @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun share(text: String, title: String?, subject: String?, activity: Activity) {
        share(text, title, subject, activity, null)
    }

    /**
     * Shares [text] through the OS chooser, optionally overriding the shared text per target app.
     *
     * [replacementExtras] maps a target package name to a [Bundle] whose `EXTRA_TEXT` replaces the
     * default one when the user picks that package, which is how a channel-tagged link reaches the
     * chosen app. Targets absent from the map receive [text] unchanged.
     *
     * Note this is per package, never per component: a package exposing several share activities
     * gets one variant for all of them.
     */
    @JvmStatic @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun share(
        text: String,
        title: String?,
        subject: String?,
        activity: Activity,
        replacementExtras: Bundle?
    ) {
        sharedURL = text
        val immutabilityIntentFlags: Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        val chooserIntent =
            Intent.createChooser(
                shareIntent,
                title,
                PendingIntent.getBroadcast(
                    activity.applicationContext,
                    0,
                    Intent(activity.applicationContext, SharingBroadcastReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or immutabilityIntentFlags
                ).intentSender
            )

        // Must be set on the chooser intent, not the payload intent: the chooser reads it while
        // building each target's launch intent.
        if (replacementExtras != null && !replacementExtras.isEmpty) {
            chooserIntent.putExtra(Intent.EXTRA_REPLACEMENT_EXTRAS, replacementExtras)
        }

        activity.startActivityForResult(chooserIntent, 1002)
    }
}