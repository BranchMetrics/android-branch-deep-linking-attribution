package io.branch.branchandroiddemo;

//import com.squareup.leakcanary.LeakCanary;

import io.branch.referral.Branch;
import io.branch.referral.BranchApp;

public final class CustomBranchApp extends BranchApp {
    @Override
    public void onCreate() {
        super.onCreate();
        //Branch.enableSimulateInstalls();
        // Uncomment to test memory leak
        // LeakCanary.install(this);
    }
}
