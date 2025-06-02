package io.branch.referral;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import android.text.TextUtils;
import android.util.DisplayMetrics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.jar.JarFile;

/**
 * Class for Branch utility methods
 */
public class BranchUtil {

    /** For setting test mode using {@link Branch#enableTestMode()} */
    private static boolean isTestModeEnabled_ = false;

    private static Boolean testModeEnabledViaCompileTimeConfiguration = null;

    // Package Private
    static void shutDown() {
        isTestModeEnabled_ = false;
        testModeEnabledViaCompileTimeConfiguration = null;
    }

    /**
     * Get the value of "io.branch.sdk.TestMode" entry in application manifest or from String res.
     * This will also set the value of {@link BranchUtil#isTestModeEnabled()}
     *
     * @return value of "io.branch.sdk.TestMode" entry in application manifest or String res.
     * false if "io.branch.sdk.TestMode" is not added in the manifest or String res.
     */
    static boolean checkTestMode(Context context) {
        // setting isTestModeEnabled_ programmatically overrides both manifest and branch.json configurations.
        if (!isTestModeEnabled_) {

            if (testModeEnabledViaCompileTimeConfiguration == null) {

                BranchJsonConfig jsonConfig = BranchJsonConfig.getInstance(context);
                if (jsonConfig.isValid(BranchJsonConfig.BranchJsonKey.useTestInstance)) {
                    // branch.json overrides manifest configurations
                    Boolean r = jsonConfig.getUseTestInstance();
                    isTestModeEnabled_ = r != null ? r : false;
                } else {
                    // manifest configurations is the last resort
                    isTestModeEnabled_ = readTestMode(context);
                }

                testModeEnabledViaCompileTimeConfiguration = isTestModeEnabled_;
            }
        }
        return isTestModeEnabled_;
    }

    private static boolean readTestMode(Context context) {
        boolean result = isTestModeEnabled_;
        String testModeKey = "io.branch.sdk.TestMode";
        try {
            final ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData != null && ai.metaData.containsKey(testModeKey)) {
                result = ai.metaData.getBoolean(testModeKey, false);
            } else {
                Resources resources = context.getResources();
                result = Boolean.parseBoolean(resources.getString(resources.getIdentifier(testModeKey, "string", context.getPackageName())));
            }
        } catch (Exception e) { // Extending catch to trap any exception to handle a rare dead object scenario
        }
        return result;
    }

    public static String readBranchKey(Context context) {
        String branchKey = null;

        // branch.json overrides manifest or string resources configurations
        BranchJsonConfig jsonConfig = BranchJsonConfig.getInstance(context);
        if (jsonConfig.isValid()) branchKey = jsonConfig.getBranchKey();
        if (branchKey != null) {
            PrefHelper.getInstance(context).setBranchKeySource("branch_json");
            return branchKey;
        }

        String metaDataKey = isTestModeEnabled() ? "io.branch.sdk.BranchKey.test" : "io.branch.sdk.BranchKey";
        // manifest overrides string resources
        try {
            final ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData != null) {
                branchKey = ai.metaData.getString(metaDataKey);
                if (branchKey == null && isTestModeEnabled()) {
                    // If test mode is enabled, but the test key cannot be found, fall back to the live key.
                    branchKey = ai.metaData.getString("io.branch.sdk.BranchKey");
                }
                if (branchKey != null) {
                    PrefHelper.getInstance(context).setBranchKeySource("manifest");
                }
            }
        } catch (final PackageManager.NameNotFoundException e) {
            BranchLogger.d(e.getMessage());
        }
        if (branchKey != null) return branchKey;

        // check string resources as the last resort
        Resources resources = context.getResources();
        branchKey = resources.getString(resources.getIdentifier(metaDataKey, "string", context.getPackageName()));
        if (!branchKey.isEmpty()) {
            PrefHelper.getInstance(context).setBranchKeySource("strings");
        }

        return branchKey;
    }

    public static boolean getEnableLoggingConfig(Context context) {
        BranchJsonConfig jsonConfig = BranchJsonConfig.getInstance(context);
        boolean enableLogging = false;

        if(jsonConfig.isValid()){
            // Safely coerce nullable json result to boolean
            enableLogging = Boolean.TRUE.equals(jsonConfig.getEnableLogging());
        }

        return enableLogging;
    }

    public static boolean getDeferInitForPluginRuntimeConfig(Context context){
        BranchJsonConfig jsonConfig = BranchJsonConfig.getInstance(context);

        boolean deferInitForPluginRuntime = false;

        if(jsonConfig.isValid()){
            // Safely coerce nullable json result to boolean
            deferInitForPluginRuntime = Boolean.TRUE.equals(jsonConfig.getDeferInitForPluginRuntime());
        }

        return deferInitForPluginRuntime;
    }

    public static void setAPIBaseUrlFromConfig(Context context) {
        BranchJsonConfig jsonConfig = BranchJsonConfig.getInstance(context);
        String apiUrl = jsonConfig.getAPIUrl();
        if (!TextUtils.isEmpty(apiUrl)) {
            Branch.setAPIUrl(apiUrl);
        }
    }

    public static void setFbAppIdFromConfig(Context context) {
        BranchJsonConfig jsonConfig = BranchJsonConfig.getInstance(context);
        String fbAppId = jsonConfig.getFbAppId();
        if (!TextUtils.isEmpty(fbAppId)) {
            Branch.setFBAppID(fbAppId);
        }
    }

    public static void setCPPLevelFromConfig(Context context) {
        BranchJsonConfig jsonConfig = BranchJsonConfig.getInstance(context);
        String jsonString = jsonConfig.getConsumerProtectionAttributionLevel();

        // If there is no entry, do not change the setting or any default behavior.
        if(!TextUtils.isEmpty(jsonString)) {
            Defines.BranchAttributionLevel cppLevel = Defines.BranchAttributionLevel.valueOf(jsonString);
            Branch.getInstance().setConsumerProtectionAttributionLevel(cppLevel);
        }
    }

    /**
     * Get the value of "io.branch.sdk.TestMode" entry in application manifest or from String res.
     * This value can be overridden via. {@link Branch#enableTestMode()}
     *
     * @return value of "io.branch.sdk.TestMode" entry in application manifest or String res.
     * false if "io.branch.sdk.TestMode" is not added in the manifest or String res.
     */
    public static boolean isTestModeEnabled() {
        return isTestModeEnabled_;
    }

    static void setTestMode(boolean testMode) {
        isTestModeEnabled_ = testMode;
    }

    public static String decodeResourceId(Context context, int resourceId) {
        try {
            if (resourceId != -1) {
                return context.getResources().getString(resourceId);
            }
        }
        catch (Exception e) {
            BranchLogger.d(e.getMessage());
        }
        return null;
    }

    public static class JsonReader {
        private final JSONObject jsonObject;

        public JsonReader(JSONObject jsonObject) {
            JSONObject tempJsonObj = new JSONObject();
            try {
                tempJsonObj = new JSONObject(jsonObject.toString());
            } catch (JSONException e) {
                BranchLogger.d(e.getMessage());
            }
            this.jsonObject = tempJsonObj;
        }

        public JSONObject getJsonObject() {
            return jsonObject;
        }

        public int readOutInt(String key) {
            int val = jsonObject.optInt(key);
            jsonObject.remove(key);
            return val;
        }

        public Integer readOutInt(String key, Integer fallback) {
            Integer val = fallback;
            if (jsonObject.has(key)) {
                val = jsonObject.optInt(key);
                jsonObject.remove(key);
            }
            return val;
        }

        public String readOutString(String key) {
            String val = jsonObject.optString(key);
            jsonObject.remove(key);
            return val;
        }

        public String readOutString(String key, String fallback) {
            String val = jsonObject.optString(key, fallback);
            jsonObject.remove(key);
            return val;
        }

        public long readOutLong(String key) {
            long val = jsonObject.optLong(key);
            jsonObject.remove(key);
            return val;
        }

        public double readOutDouble(String key) {
            double val = jsonObject.optDouble(key);
            jsonObject.remove(key);
            return val;
        }

        public Double readOutDouble(String key, Double fallback) {
            Double val = fallback;
            if (jsonObject.has(key)) {
                val = jsonObject.optDouble(key);
                jsonObject.remove(key);
            }
            return val;
        }

        public boolean readOutBoolean(String key) {
            boolean val = jsonObject.optBoolean(key);
            jsonObject.remove(key);
            return val;
        }

        public JSONArray readOutJsonArray(String key) {
            JSONArray val = jsonObject.optJSONArray(key);
            jsonObject.remove(key);
            return val;
        }

        public Object readOut(String key) {
            Object val = jsonObject.opt(key);
            jsonObject.remove(key);
            return val;
        }

        public boolean has(String key) {
            return jsonObject.has(key);
        }

        public Iterator<String> keys() {
            return jsonObject.keys();
        }

    }

    public static Drawable getDrawable(@NonNull Context context, @DrawableRes int drawableID) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getResources().getDrawable(drawableID, context.getTheme());
        } else {
            //noinspection deprecation
            return context.getResources().getDrawable(drawableID);
        }
    }

    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }


    /**
     * <p>Checks the current device's {@link ActivityManager} system service and returns the value
     * of the lowMemory flag.</p>
     *
     * @return <p>A {@link Boolean} value representing the low memory flag of the current device.</p>
     * <ul>
     * <li><i>true</i> - the free memory on the current device is below the system-defined threshold
     * that triggers the low memory flag.</li>
     * <li><i>false</i> - the device has plenty of free memory.</li>
     * </ul>
     */
    public static boolean isLowOnMemory(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(mi);
        return mi.lowMemory;
    }

    public static JSONObject getDeepLinkSchemes(Context context) {
        JSONObject obj = null;
        if (!isLowOnMemory(context)) {
            JarFile jf = null;
            InputStream is = null;
            byte[] xml;
            try {
                jf = new JarFile(context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).publicSourceDir);
                is = jf.getInputStream(jf.getEntry("AndroidManifest.xml"));
                xml = new byte[is.available()];
                //noinspection ResultOfMethodCallIgnored
                is.read(xml);
                obj = new ApkParser().decompressXMLForValidator(xml, context);
            } catch (Exception e) {
            BranchLogger.d(e.getMessage());
        } finally {
                try {
                    if (is != null) {
                        is.close();
                        // noinspection unused
                        is = null;
                    }
                    if (jf != null) {
                        jf.close();
                    }
                } catch (IOException e) {
                    BranchLogger.d(e.getMessage());
                }
            }
        }
        return obj;
    }
}
