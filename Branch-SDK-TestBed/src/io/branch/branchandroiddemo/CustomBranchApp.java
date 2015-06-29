package io.branch.branchandroiddemo;

import com.squareup.leakcanary.LeakCanary;

import io.branch.referral.BranchApp;

public final class CustomBranchApp extends BranchApp {
    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
    }
}
