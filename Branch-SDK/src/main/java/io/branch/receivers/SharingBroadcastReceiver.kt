package io.branch.receivers

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_CHOSEN_COMPONENT
import io.branch.referral.BranchLogger
import io.branch.referral.NativeShareLinkManager
import io.branch.referral.util.SharingUtil

class SharingBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val clickedComponent: ComponentName? = intent.getParcelableExtra(EXTRA_CHOSEN_COMPONENT);

        BranchLogger.v("Intent: $intent")
        BranchLogger.v("Clicked component: $clickedComponent")
    }
}