package io.branch.referral.validators;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.branch.referral.BranchUtil;
import io.branch.referral.Defines;

class BranchIntegrationModel {
    JSONObject deeplinkUriScheme;
    final String branchKeyTest;
    final String branchKeyLive;
    final List<String> applinkScheme;
    final String packageName;


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
        JSONObject obj = BranchUtil.getDeepLinkSchemes(context);
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