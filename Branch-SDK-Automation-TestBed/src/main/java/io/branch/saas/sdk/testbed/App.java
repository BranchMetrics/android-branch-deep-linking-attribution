package io.branch.saas.sdk.testbed;

import android.app.Application;

import io.branch.referral.Branch;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Branch.enableLogging();
        Branch.getAutoInstance(this);
    }
}