package io.branch.branchandroidtestbed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import io.branch.referral.Branch;
import io.branch.referral.BranchConfiguration;
import io.branch.referral.BranchLogger;
import io.branch.referral.Defines;

/**
 * Sets the attribution level with the same call as the consent dialog, for the L1 scenarios:
 * adb shell am broadcast -n &lt;package&gt;/io.branch.branchandroidtestbed.L1ConsentReceiver --es level FULL
 */
public class L1ConsentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String level = intent.getStringExtra("level");
        if (level == null) {
            return;
        }
        Defines.BranchAttributionLevel attributionLevel = Defines.BranchAttributionLevel.valueOf(level);
        BranchLogger.d("L1 consent: " + attributionLevel);
        Branch.getInstance().updateConfiguration(
                new BranchConfiguration.Builder().setAttributionLevel(attributionLevel).build());
    }
}
