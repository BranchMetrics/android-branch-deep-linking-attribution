
package io.branch.branchandroiddemo;

//import com.squareup.leakcanary.LeakCanary;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import io.branch.referral.Branch;

public final class CustomBranchApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Branch.enableLogging();
        Branch.getAutoInstance(this);
        // Uncomment to test memory leak
        // LeakCanary.install(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
