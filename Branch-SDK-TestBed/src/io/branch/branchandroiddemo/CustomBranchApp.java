
package io.branch.branchandroiddemo;

//import com.squareup.leakcanary.LeakCanary;

import android.app.Application;

import io.branch.referral.Branch;
import io.branch.referral.BranchApp;

public final class CustomBranchApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Branch.enableLogging();
        Branch.enableMatchGuaranteed();
        Branch.getAutoInstance(this);
        // Uncomment to test memory leak
        // LeakCanary.install(this);
    }
}
