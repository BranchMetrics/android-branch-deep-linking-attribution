package io.branch.referral;

import android.app.Application;

/**
 * <p>
 * Default Android Application class for  Branch SDK. You should use this as your application class
 * in your manifest if you are not creating an Application class. If you already have an Application
 * class then you can either extend your Application class with BranchApp or initialize Branch yourself
 * via Branch.getAutoInstance(this);.
 * </p>
 * <p>
 * Add this entry to the manifest if you don't have an Application class :
 * </p>
 * <pre style="background:#fff;padding:10px;border:2px solid silver;">
 *      &lt;application
 *      -----
 *      android:name="io.branch.referral.BranchApp"&gt;
 *</pre>
 */
public class BranchApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Branch.Companion.getAutoInstance(this);
    }
}
