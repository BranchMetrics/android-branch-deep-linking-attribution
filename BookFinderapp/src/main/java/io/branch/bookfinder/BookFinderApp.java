package io.branch.bookfinder;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import io.branch.referral.Branch;

/**
 * Created by sojanpr on 9/26/16.
 */
public class BookFinderApp extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        Branch.getAutoTestInstance(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
