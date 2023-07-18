package io.branch.branchandroidtestbed;

import android.app.Application;

import io.branch.referral.Branch;

public final class CustomBranchApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Branch.enableLogging();
        Branch.getAutoInstance(this);
    }
}
