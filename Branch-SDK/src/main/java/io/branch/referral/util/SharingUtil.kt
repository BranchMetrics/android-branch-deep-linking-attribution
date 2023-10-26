package io.branch.referral.util

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import io.branch.receivers.SharingBroadcastReceiver


class SharingUtil {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun share(text: String, subject: String?, activity: Activity) {

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
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        val chooserIntent =
            Intent.createChooser(
                shareIntent,
                null, // dialog title optional
                PendingIntent.getBroadcast(
                    activity.applicationContext,
                    0,
                    Intent(activity.applicationContext, SharingBroadcastReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or immutabilityIntentFlags
                ).intentSender
            )

        activity.startActivityForResult(chooserIntent, 1002)

    }
}