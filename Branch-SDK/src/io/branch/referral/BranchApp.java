package io.branch.referral;

import android.app.Application;

/**
 * <p>
 * Default Android Application class for  Branch SDK. You should use this as your application class
 * in your manifest if you are not creating an Application class.If you already have an Application
 * class then extend your Application class with this.
 * </p>
 * <p/>
 * <p>
 * Add this entry to the manifest if you don't have an Application class :
 * </p>
 * <p/>
 * <pre style="background:#fff;padding:10px;border:2px solid silver;">
 * <application
 * -----
 * android:name="io.branch.referal.BranchApp">
 */
public class BranchApp extends Application {


    @Override
    public void onCreate() {
        super.onCreate();
        Branch.createInstance(this);
    }
}
