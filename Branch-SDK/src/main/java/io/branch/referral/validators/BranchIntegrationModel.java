package io.branch.referral.validators;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import io.branch.referral.BranchLogger;
import io.branch.referral.BranchUtil;
import io.branch.referral.Defines;

class BranchIntegrationModel {
    JSONObject deeplinkUriScheme;
    private final String branchKeyTest;
    private final String branchKeyLive;
    final List<String> applinkScheme;
    final String packageName;
    boolean appSettingsAvailable = false;
    
    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "BranchIntegration-Worker");
        t.setDaemon(true);
        return t;
    });


    public BranchIntegrationModel(Context context) {
        String liveKey = null, testKey = null;
        ApplicationInfo appInfo;
        applinkScheme = new ArrayList<>();
        packageName = context.getPackageName();

        try {
            appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                liveKey = appInfo.metaData.getString("io.branch.sdk.BranchKey");
                testKey = appInfo.metaData.getString("io.branch.sdk.BranchKey.test");
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        branchKeyLive = liveKey;
        branchKeyTest = testKey;
        updateDeepLinkSchemes(context);
    }

    private void updateDeepLinkSchemes(Context context) {
        JSONObject obj = null;
        try {
            // Avoid ANRs on reading and parsing manifest with a timeout
            BranchLogger.d("MODERNIZATION_TRACE: BranchIntegrationModel using CompletableFuture pattern");
            CompletableFuture<JSONObject> future = CompletableFuture.supplyAsync(
                () -> {
                    try {
                        BranchLogger.d("MODERNIZATION_TRACE: Executing deep link scheme extraction in CompletableFuture");
                        JSONObject result = BranchUtil.getDeepLinkSchemes(context);
                        BranchLogger.d("MODERNIZATION_TRACE: Deep link scheme extraction completed successfully");
                        return result;
                    } catch (Exception e) {
                        BranchLogger.w("MODERNIZATION_TRACE: Failed to extract deep link schemes: " + e.getMessage());
                        return new JSONObject();
                    }
                }, 
                executor
            );
            obj = future.get(2500, TimeUnit.MILLISECONDS);
            BranchLogger.d("MODERNIZATION_TRACE: BranchIntegrationModel CompletableFuture completed with timeout 2.5s");
            appSettingsAvailable = true;
        } catch (TimeoutException e) {
            BranchLogger.w("Deep link scheme extraction timed out after 2.5 seconds");
            appSettingsAvailable = false;
        } catch (ExecutionException e) {
            BranchLogger.w("Deep link scheme extraction failed: " + e.getCause().getMessage());
            appSettingsAvailable = false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            BranchLogger.w("Deep link scheme extraction was interrupted");
            appSettingsAvailable = false;
        } catch (Exception e) {
            BranchLogger.w("Unexpected error during deep link scheme extraction: " + e.getMessage());
            appSettingsAvailable = false;
        }
        if (obj != null) {
            try {
                deeplinkUriScheme = obj.optJSONObject(Defines.Jsonkey.URIScheme.getKey());
                JSONArray hostArray = obj.optJSONArray(Defines.Jsonkey.AppLinks.getKey());
                if (hostArray != null) {
                    for (int i = 0; i < hostArray.length(); i++) {
                        String host = hostArray.optString(i);
                        if (host != null && !host.isEmpty()) {
                            applinkScheme.add(host);
                        }
                    }
                }
            } catch (Exception e) {
                BranchLogger.w("Error parsing deep link schemes: " + e.getMessage());
            }
        }
    }

}