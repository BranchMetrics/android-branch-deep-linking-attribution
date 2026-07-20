package io.branch.receivers

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_CHOSEN_COMPONENT
import io.branch.referral.BranchLogger
import io.branch.referral.NativeShareLinkManager
import io.branch.referral.util.BranchChannelMap
import io.branch.referral.util.PerTargetChannelConfig
import io.branch.referral.util.SharingUtil

class SharingBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val clickedComponent: ComponentName? = intent.getParcelableExtra(EXTRA_CHOSEN_COMPONENT);

        BranchLogger.v("Intent: $intent")
        BranchLogger.v("Clicked component: $clickedComponent")

        NativeShareLinkManager.getInstance().linkShareListenerCallback?.onChannelSelected(
            resolveChannelName(clickedComponent)
        )

        NativeShareLinkManager.getInstance().linkShareListenerCallback?.onLinkShareResponse(SharingUtil.sharedURL, null);
    }

    /**
     * Names the chosen target for the SHARE event.
     *
     * With per-target channel attribution enabled the link itself already carries a friendly
     * `~channel` such as "WhatsApp", so the event reports the same name rather than a raw
     * `ComponentName`, keeping the sender-side and recipient-side records consistent. With the
     * feature off, or for a target Branch has no mapping for, the existing `ComponentName` string
     * is preserved so current integrations see no change.
     */
    private fun resolveChannelName(component: ComponentName?): String {
        val raw = component.toString()
        if (!PerTargetChannelConfig.isEnabled() || component == null) {
            return raw
        }
        return BranchChannelMap.channelForPackage(component.packageName) ?: raw
    }
}