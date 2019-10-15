package io.branch.referral.validators;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.branch.referral.BranchAsyncTask;
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
            obj = new getDeepLinkSchemeTasks().executeTask(context).get(2500, TimeUnit.MILLISECONDS);
            appSettingsAvailable = true;
        } catch (Throwable t) {
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

    // Reading deep linked schemes involves decompressing of apk and parsing manifest. This can lead to a ANR if reading file is slower
    // Use this only with a timeout
    private class getDeepLinkSchemeTasks extends BranchAsyncTask<Context, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(Context... contexts) {
            return BranchUtil.getDeepLinkSchemes(contexts[0]);
        }
    }
}