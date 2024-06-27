package io.branch.branchandroidtestbed;

import android.app.Application;
import android.util.Log;

import io.branch.interfaces.IBranchLoggingCallbacks;
import io.branch.referral.Branch;
import io.branch.referral.BranchLogger;
import io.branch.referral.Defines;

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
        Branch.enableLogging(BranchLogger.BranchLogLevel.VERBOSE); // Pass in iBranchLoggingCallbacks to enable logging redirects
        Branch.getAutoInstance(this);

        Branch.getInstance().setConsumerProtectionAttributionLevel(Defines.BranchAttributionLevel.MINIMAL);

    }
}
