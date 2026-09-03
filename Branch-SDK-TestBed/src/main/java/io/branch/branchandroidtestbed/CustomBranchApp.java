package io.branch.branchandroidtestbed;

import static androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK;

import android.app.Application;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import io.branch.securesdk.BranchSecureSDK;
import io.branch.referral.Branch;
import io.branch.referral.BranchLogger;
import io.branch.referral.IBranchRequestTracingCallback;

public final class CustomBranchApp extends Application {
    /**
     * Forces the Secure SDK down its Play Integrity path instead of hardware Key Attestation.
     *
     * <p>Off by default. Flip to true, rebuild and reinstall to capture a Play Integrity token for
     * {@code tools/verify_play_integrity_self_managed.py} (EMT-4196). Hardware attestation succeeds
     * on any healthy API 24+ device, so the Play Integrity branch is otherwise unreachable here.</p>
     *
     * <p>Deliberately not wired to {@code io.branch.sdk.TestMode}: that switches the branch key and
     * carries other behaviour, and this is only about which attestation is produced.</p>
     */
    private static final boolean FORCE_PLAY_INTEGRITY = false;

    @Override
    public void onCreate() {
        super.onCreate();

        if (FORCE_PLAY_INTEGRITY) {
            forcePlayIntegrityFallback();
        }

        // Initialize the Branch Secure SDK (optional). setFraudDefenseProvider() below starts it
        // with the real branch key; calling initializeBranchSecureSDK() here too would only start
        // it early with a null key and the second call is ignored.
        BranchSecureSDK secureSDK = BranchSecureSDK.getInstance(this);

        Branch.enableLogging((message, tag) -> {
            Log.d("BranchTestbed", message);
            saveLogToFile(message);
        }, BranchLogger.BranchLogLevel.VERBOSE);
        Branch branch = Branch.getAutoInstance(this);

        // Set the device-trust provider
        branch.setFraudDefenseProvider(secureSDK);

        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                .setColorScheme(COLOR_SCHEME_DARK)
                .build();
        Branch.getInstance().setCustomTabsIntent(customTabsIntent);

        Branch.setCallbackForTracingRequests(new IBranchRequestTracingCallback() {
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
        });
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

    /**
     * Flips {@code AppAttestation.forcePlayIntegrityFallback} in the Secure SDK.
     *
     * <p>Reflection because the flag is Kotlin {@code internal}: it is public on the JVM but its
     * name carries the module and variant ({@code setForcePlayIntegrityFallback$securesdk_debug},
     * {@code ...$securesdk_release}), so it is matched by prefix rather than hardcoded. Keeping it
     * {@code internal} means no test switch leaks into the SDK's public API.</p>
     */
    private void forcePlayIntegrityFallback() {
        try {
            Class<?> clazz = Class.forName("io.branch.securesdk.AppAttestation");
            Object instance = clazz.getField("INSTANCE").get(null);
            for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                if (m.getName().startsWith("setForcePlayIntegrityFallback")) {
                    m.invoke(instance, true);
                    Log.w("BranchTestbed", "FORCE_PLAY_INTEGRITY on — hardware attestation disabled via "
                            + m.getName());
                    return;
                }
            }
            Log.e("BranchTestbed", "FORCE_PLAY_INTEGRITY set but no setter found on AppAttestation — "
                    + "the flag was probably renamed or removed.");
        } catch (Throwable t) {
            Log.e("BranchTestbed", "FORCE_PLAY_INTEGRITY failed: " + t);
        }
    }
}
