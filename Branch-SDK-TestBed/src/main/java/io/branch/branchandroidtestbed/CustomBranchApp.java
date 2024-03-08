package io.branch.branchandroidtestbed;

import android.app.Application;
import android.util.Log;

import io.branch.interfaces.IBranchLoggingCallbacks;
import io.branch.referral.Branch;

public final class CustomBranchApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
      
        IBranchLoggingCallbacks iBranchLoggingCallbacks = new IBranchLoggingCallbacks() {
            @Override
            public void onBranchLog(String logMessage, String severityConstantName) {
                Log.v( "CustomTag", logMessage);
            }
        };
        Branch.enableLogging(); // Pass in iBranchLoggingCallbacks to enable logging redirects
        Branch.getAutoInstance(this);
    }
}
