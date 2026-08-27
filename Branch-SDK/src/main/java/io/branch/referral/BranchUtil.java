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

    /** For setting test mode using {@link BranchConfiguration.Builder#setTestMode} */
    private static boolean isTestModeEnabled_ = false;

    private static Boolean testModeEnabledViaCompileTimeConfiguration = null;

    private static final String SOURCE_BRANCH_JSON = "branch_json";

    // Package Private
    static void shutDown() {
        isTestModeEnabled_ = false;
        testModeEnabledViaCompileTimeConfiguration = null;
    }

    /**
     * Resolves test mode from branch.json. A value set programmatically — via
     * {@link BranchConfiguration.Builder#setTestMode} or {@link BranchUtil#setTestMode} — takes
     * precedence and short-circuits this lookup.
     *
     * @return whether test mode is enabled. False when branch.json does not set it.
     */
    static boolean checkTestMode(Context context) {
        if (!isTestModeEnabled_) {

            if (testModeEnabledViaCompileTimeConfiguration == null) {

                BranchJsonConfig jsonConfig = BranchJsonConfig.getInstance(context);
                if (jsonConfig.isValid(BranchJsonConfig.BranchJsonKey.useTestInstance)) {
                    Boolean r = jsonConfig.getUseTestInstance();
                    isTestModeEnabled_ = r != null ? r : false;
                }

                testModeEnabledViaCompileTimeConfiguration = isTestModeEnabled_;
            }
        }
        return isTestModeEnabled_;
    }

    /**
     * Resolves the branch key from branch.json. The manifest and string-resource entry points were
     * removed in 6.0 — pass the key to {@link BranchConfiguration.Builder} instead.
     */
    public static String readBranchKey(Context context) {
        String branchKey = readBranchKeyFromJson(context);
        if (branchKey != null) {
            setBranchKeyAndSource(context, branchKey, SOURCE_BRANCH_JSON);
        } else {
            BranchLogger.v("Branch key not present in branch.json");
        }
        return branchKey;
    }

    private static String readBranchKeyFromJson(Context context) {
        BranchJsonConfig jsonConfig = BranchJsonConfig.getInstance(context);
        String branchKey = jsonConfig.isValid() ? jsonConfig.getBranchKey() : null;
        if (branchKey != null) {
            BranchLogger.v("Found branch key in branch.json: " + (branchKey.length() > 10 ? branchKey.substring(0, 10) + "..." : branchKey));
        } else {
            BranchLogger.v("branch.json configuration not valid or branch key not present");
        }
        return branchKey;
    }





    private static void setBranchKeyAndSource(Context context, String branchKey, String source) {
        PrefHelper prefHelper = PrefHelper.getInstance(context);
        prefHelper.setBranchKey(branchKey);
        prefHelper.setBranchKeySource(source);
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
            PrefHelper.setAPIUrl(apiUrl);
        }
    }

    public static void setFbAppIdFromConfig(Context context) {
        BranchJsonConfig jsonConfig = BranchJsonConfig.getInstance(context);
        String fbAppId = jsonConfig.getFbAppId();
        if (!TextUtils.isEmpty(fbAppId)) {
            PrefHelper.setFbAppId(fbAppId);
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
     * @return whether test mode is enabled. Set via
     * {@link BranchConfiguration.Builder#setTestMode}, or resolved from branch.json.
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
