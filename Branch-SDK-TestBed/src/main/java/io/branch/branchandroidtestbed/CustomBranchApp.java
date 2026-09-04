package io.branch.branchandroidtestbed;

import static androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import io.branch.referral.Branch;
import io.branch.referral.BranchConfiguration;
import io.branch.referral.BranchLogger;
import io.branch.referral.IBranchRequestTracingCallback;

public final class CustomBranchApp extends Application {

    /**
     * The builder takes exactly one key, so the app picks it rather than the SDK inferring one from
     * the two {@code io.branch.sdk.BranchKey} manifest entries. {@code setTestMode(true)} no longer
     * swaps the key for you — select it here.
     */
    private static final boolean USE_TEST_KEY = true;
    private static final String LIVE_KEY = "key_live_hcnegAumkH7Kv18M8AOHhfgiohpXq5tB";
    private static final String TEST_KEY = "key_test_hdcBLUy1xZ1JD0tKg7qrLcgirFmPPVJc";

    @Override
    public void onCreate() {
        super.onCreate();

        BranchConfiguration.Builder config =
                new BranchConfiguration.Builder(USE_TEST_KEY ? TEST_KEY : LIVE_KEY)
                        .setTestMode(USE_TEST_KEY)
                        .setLogLevel(BranchLogger.BranchLogLevel.VERBOSE)
                        .setLoggingCallback((message, severity) -> {
                            Log.d("BranchTestbed", message);
                            saveLogToFile(message);
                        })
                        .setRequestTracingCallback(tracingCallback());

        // Operator-set override from SettingsActivity, applied at launch because the API URL is a
        // pre-init decision.
        String apiUrl = TestBedSettings.getApiUrl(this);
        if (!TextUtils.isEmpty(apiUrl)) {
            config.setApiUrl(apiUrl);
        }

        Branch.initialize(this, config.build());

        // Runtime appearance setting — stays an instance method, not part of the configuration.
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                .setColorScheme(COLOR_SCHEME_DARK)
                .build();
        Branch.getInstance().setCustomTabsIntent(customTabsIntent);
    }

    private IBranchRequestTracingCallback tracingCallback() {
        return new IBranchRequestTracingCallback() {
            @Override
            public void onRequestCompleted(String uri, JSONObject request, JSONObject response, String error, String requestUrl) {
                String entry = "URI Sent to Branch: " + uri
                        + "\nRequest: " + request
                        + "\nResponse: " + response
                        + "\nError Message: " + error
                        + "\nRequest Url: " + requestUrl;
                Log.d("Shortlink_Session_Test", entry);
                saveLogToFile(entry);
            }
        };
    }

    private synchronized void saveLogToFile(String logMessage) {
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
