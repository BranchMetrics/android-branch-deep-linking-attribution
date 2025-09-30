package io.branch.branchandroidtestbed;

import static androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK;

import android.app.Application;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import io.branch.referral.Branch;
import io.branch.referral.BranchLogger;
import io.branch.referral.IBranchRequestTracingCallback;

public final class CustomBranchApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

//        IBranchLoggingCallbacks loggingCallbacks = (message, tag) -> {
//            Log.d("BranchTestbed", message);
//            saveLogToFile(message);
//        };
        Branch.enableLogging(BranchLogger.BranchLogLevel.VERBOSE);
        Branch branch = Branch.getAutoInstance(this);
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                .setColorScheme(COLOR_SCHEME_DARK)
                .build();
        branch.setCustomTabsIntent(customTabsIntent);

        Branch.setCallbackForTracingRequests(new IBranchRequestTracingCallback() {
            @Override
            public void onRequestCompleted(String uri, JSONObject request, JSONObject response, String error, String requestUrl) {
                Log.d("Shortlink_Session_Test",
                        "URI Sent to Branch: " + uri
                        + "\nRequest: " + request
                        + "\nResponse: " + response
                        + "\nError Message: " + error
                        + "\nRequest Url: " + requestUrl
                );
            }
        });
    }

    private void saveLogToFile(String logMessage) {
        File logFile = new File(getFilesDir(), "branchlogs.txt");

        try {
            if (!logFile.exists()) {
                boolean fileCreated = logFile.createNewFile();
                Log.d("BranchTestbed", "Log file created: " + fileCreated);
            }

            try (FileOutputStream fos = new FileOutputStream(logFile, true);
                 OutputStreamWriter writer = new OutputStreamWriter(fos)) {
                writer.write(logMessage + "\n");
            }

        } catch (Exception e) {
            Log.e("BranchTestbed", "Error writing to log file", e);
        }
    }
}
