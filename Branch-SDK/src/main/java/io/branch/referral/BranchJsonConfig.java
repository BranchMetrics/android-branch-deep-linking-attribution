package io.branch.referral;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by jdee on 6/7/17.
 */

public class BranchJsonConfig {
    private JSONObject mConfiguration = null;
    public static final String TAG = "BranchJsonConfig";

    private static final String fileName = "branch.json";

    public enum BranchJsonKey {
        branchKey,
        testKey,
        liveKey,
        useTestInstance,
        enableLogging,
        deferInitForPluginRuntime,
        apiUrl,
        fbAppId,
        cppLevel
    }

    /*

        place file called "branch.json" -> in assets folder, see available key-values below,
        - if "branchKey" is present, it will override the useTestInstance/testKey/liveKey config
        - if "branchKey" is missing, all three, useTestInstance/testKey/liveKey, must be present.

       {
            "branchKey":"key_live_hcnegAumkH7Kv18M8AOHhfgiohpXq5tB",
            "testKey":"key_test_hdcBLUy1xZ1JD0tKg7qrLcgirFmPPVJc",
            "liveKey":"key_live_hcnegAumkH7Kv18M8AOHhfgiohpXq5tB",
            "useTestInstance": true,
            "enableLogging": true,
            "deferInitForPluginRuntime": true,
            "apiUrl": "https://api2.branch.io",
            "fbAppId": "xyz123456789",
            "consumerProtectionAttributionLevel: "Reduced"
       }
    */

    private BranchJsonConfig(Context context) {
        try {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(context.getAssets().open(fileName)));
            } catch (FileNotFoundException e) {
                return;
            }

            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            mConfiguration = new JSONObject(builder.toString());
        }
        catch (IOException e) {
            Log.e(TAG, "Error loading branch.json: " + e.getMessage());
        }
        catch (JSONException e) {
            Log.e(TAG, "Error parsing branch.json: " + e.getMessage());
        }
    }

    private static BranchJsonConfig instance;
    public static BranchJsonConfig getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new BranchJsonConfig(context);
        }
        return instance;
    }

    public boolean isValid() {
        return mConfiguration != null;
    }
    public boolean isValid(BranchJsonKey key) {
        return mConfiguration != null && mConfiguration.has(key.toString());
    }

    @Nullable
    public Object get(BranchJsonKey key) {
        if (!isValid(key)) return null;

        try {
            return mConfiguration.get(key.toString());
        } catch (JSONException exception) {
            Log.e(TAG, "Error parsing branch.json: " + exception.getMessage());
            return null;
        }
    }

    @Nullable
    public Boolean getEnableLogging() {
        if (!isValid(BranchJsonKey.enableLogging)) {
            return null;
        }

        try {
            return mConfiguration.getBoolean(BranchJsonKey.enableLogging.toString());
        }
        catch (JSONException exception) {
            Log.e(TAG, "Error parsing branch.json: " + exception.getMessage());
            return false;
        }
    }

    @Nullable
    public Boolean getDeferInitForPluginRuntime(){
        if (!isValid(BranchJsonKey.deferInitForPluginRuntime)) {
            return null;
        }

        try {
            return mConfiguration.getBoolean(BranchJsonKey.deferInitForPluginRuntime.toString());
        }
        catch (JSONException exception) {
            Log.e(TAG, "Error parsing branch.json: " + exception.getMessage());
            return false;
        }
    }

    @Nullable
    public String getBranchKey() {
        if (isValid(BranchJsonKey.branchKey) ||
                (isValid(BranchJsonKey.liveKey) && isValid(BranchJsonKey.testKey) && isValid(BranchJsonKey.useTestInstance))) {

            try {
                if (isValid(BranchJsonKey.branchKey)) {
                    return mConfiguration.getString(BranchJsonKey.branchKey.toString());
                } else {
                    return getUseTestInstance() ? getTestKey() : getLiveKey();
                }
            } catch (JSONException exception) {
                Log.e(TAG, "Error parsing branch.json: " + exception.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    @Nullable
    private String getLiveKey() {
        if (!isValid(BranchJsonKey.liveKey)) return null;

        try {
            return mConfiguration.getString(BranchJsonKey.liveKey.toString());
        } catch (JSONException exception) {
            Log.e(TAG, "Error parsing branch.json: " + exception.getMessage());
            return null;
        }
    }

    @Nullable
    private String getTestKey() {
        if (mConfiguration == null) return null;

        try {
            if (!mConfiguration.has(BranchJsonKey.testKey.toString())) return null;
            return mConfiguration.getString(BranchJsonKey.testKey.toString());
        }
        catch (JSONException exception) {
            Log.e(TAG, "Error parsing branch.json: " + exception.getMessage());
            return null;
        }
    }

    public @Nullable Boolean getUseTestInstance() {
        if (!isValid(BranchJsonKey.useTestInstance)) return null;

        try {
            return mConfiguration.getBoolean(BranchJsonKey.useTestInstance.toString());
        } catch (JSONException exception) {
            Log.e(TAG, "Error parsing branch.json: " + exception.getMessage());
            return false;
        }
    }

    @Nullable
    public String getAPIUrl() {
        if (mConfiguration == null) return null;

        try {
            if (!mConfiguration.has(BranchJsonKey.apiUrl.toString())) return null;
            return mConfiguration.getString(BranchJsonKey.apiUrl.toString());
        }
        catch (JSONException exception) {
            Log.e(TAG, "Error parsing branch.json: " + exception.getMessage());
            return null;
        }
    }

    @Nullable
    public String getFbAppId() {
        if (mConfiguration == null) {
            return null;
        }

        try {
            if (!mConfiguration.has(BranchJsonKey.fbAppId.toString())) {
                return null;
            }
            
            return mConfiguration.getString(BranchJsonKey.fbAppId.toString());
        }
        catch (JSONException exception) {
            Log.e(TAG, "Error parsing branch.json: " + exception.getMessage());
            return null;
        }
    }

    @Nullable
    public String getConsumerProtectionAttributionLevel() {
        if (mConfiguration == null) {
            return null;
        }

        try {
            if (!mConfiguration.has(BranchJsonKey.cppLevel.toString())) {
                return null;
            }

            return mConfiguration.getString(BranchJsonKey.cppLevel.toString());
        }
        catch (JSONException exception) {
            Log.e(TAG, "Error parsing branch.json: " + exception.getMessage());
            return null;
        }
    }
}
