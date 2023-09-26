package io.branch.referral.validators;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
            // For one-off tasks
            ExecutorService executor = Executors.newSingleThreadExecutor();
            obj = executor.submit(() ->
                            BranchUtil.getDeepLinkSchemes(context)
                    ).get(2500, TimeUnit.MILLISECONDS);
            appSettingsAvailable = true;
        }
        catch (Exception e) {
            BranchLogger.d(e.getMessage());
        }
        if (obj != null) {
            deeplinkUriScheme = obj.optJSONObject(Defines.Jsonkey.URIScheme.getKey());
            JSONArray hostArray = obj.optJSONArray(Defines.Jsonkey.AppLinks.getKey());
            if (hostArray != null) {
                for (int i = 0; i < hostArray.length(); i++) {
                    applinkScheme.add(hostArray.optString(i));
                }
            }
        }
    }
}