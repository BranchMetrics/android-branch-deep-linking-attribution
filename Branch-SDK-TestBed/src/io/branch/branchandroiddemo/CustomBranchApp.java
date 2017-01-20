
package io.branch.branchandroiddemo;

//import com.squareup.leakcanary.LeakCanary;

import android.app.Application;

import io.branch.referral.Branch;
import io.branch.referral.BranchApp;

public final class CustomBranchApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //Branch.excludeFromShareSheet("com.Slack");
        Branch.includeInShareSheet("com.twitter.android");
        //Branch.includeInShareSheet("com.Slack");
        Branch.getAutoInstance(this);
        // Uncomment to test memory leak
        // LeakCanary.install(this);
    }
}
