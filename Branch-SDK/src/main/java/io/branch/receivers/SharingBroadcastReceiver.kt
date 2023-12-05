package io.branch.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_CHOSEN_COMPONENT
import io.branch.referral.BranchLogger
import io.branch.referral.util.SharingUtil

class SharingBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val clickedComponent : ComponentName? = intent.getParcelableExtra(EXTRA_CHOSEN_COMPONENT);

        BranchLogger.v("Intent: $intent")
        BranchLogger.v("Clicked component: $clickedComponent")

        val sharedIntent : Intent? = SharingUtil.chooserIntent?.getParcelableExtra(Intent.EXTRA_INTENT);
        val shareIntentdExtraText = sharedIntent?.getStringExtra(Intent.EXTRA_TEXT)

        BranchLogger.v("Intent Shared: $sharedIntent");
        BranchLogger.v("Intent Shared Text: $shareIntentdExtraText");

        sharedIntent?.putExtra(Intent.EXTRA_TEXT, "test Nidhi")

        SharingUtil.chooserIntent?.putExtra(Intent.EXTRA_INTENT,sharedIntent)

        val sharedIntent2 : Intent? = SharingUtil.chooserIntent?.getParcelableExtra(Intent.EXTRA_INTENT);
        val shareIntentdExtraText2 = sharedIntent2?.getStringExtra(Intent.EXTRA_TEXT)

        BranchLogger.v("Intent Shared Updated: $sharedIntent2");
        BranchLogger.v("Intent Shared Text Updated: $shareIntentdExtraText2");

        // TODO : Get Pointer to BranchLinkShareListener and call function onChannelSelected

       // val shareIntent3 = Intent.createChooser(shareIntent, null)
        // context.startActivity(shareIntent3)



    }
}