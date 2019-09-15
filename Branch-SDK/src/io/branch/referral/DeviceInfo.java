package io.branch.referral;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.webkit.WebSettings;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>
 * Class for handling the device params with Branch server requests. responsible for capturing device info and updating
 * device info to Branch requests
 * </p>
 */
class DeviceInfo {
    private final SystemObserver systemObserver_;
    private final Context context_;

    private static DeviceInfo thisInstance_ = null;

    /**
     * Initialize the singleton instance for deviceInfo class
     *
     * @return {@link DeviceInfo} global instance
     */
    static DeviceInfo initialize(Context context) {
        if (thisInstance_ == null) {
            thisInstance_ = new DeviceInfo(context);
        }
        return thisInstance_;
    }

    /**
     * Get the singleton instance for this class
     *
     * @return {@link DeviceInfo} instance if already initialised or null
     */
    static DeviceInfo getInstance() {
        return thisInstance_;
    }

    // Package Private
    static void shutDown() {
        thisInstance_ = null;
    }

    private DeviceInfo(Context context) {
        context_ = context;
        systemObserver_ = new SystemObserverInstance();
    }

    /**
     * Update the given server request JSON with device params
     *
     * @param requestObj JSON object for Branch server request
     */
    void updateRequestWithV1Params(JSONObject requestObj) {
        try {
            SystemObserver.UniqueId hardwareID = getHardwareID();
            if (!isNullOrEmptyOrBlank(hardwareID.getId())) {
                requestObj.put(Defines.Jsonkey.HardwareID.getKey(), hardwareID.getId());
                requestObj.put(Defines.Jsonkey.IsHardwareIDReal.getKey(), hardwareID.isReal());
            }

            String brandName = SystemObserver.getPhoneBrand();
            if (!isNullOrEmptyOrBlank(brandName)) {
                requestObj.put(Defines.Jsonkey.Brand.getKey(), brandName);
            }

            String modelName = SystemObserver.getPhoneModel();
            if (!isNullOrEmptyOrBlank(modelName)) {
                requestObj.put(Defines.Jsonkey.Model.getKey(), modelName);
            }

            DisplayMetrics displayMetrics = SystemObserver.getScreenDisplay(context_);
            requestObj.put(Defines.Jsonkey.ScreenDpi.getKey(), displayMetrics.densityDpi);
            requestObj.put(Defines.Jsonkey.ScreenHeight.getKey(), displayMetrics.heightPixels);
            requestObj.put(Defines.Jsonkey.ScreenWidth.getKey(), displayMetrics.widthPixels);

            requestObj.put(Defines.Jsonkey.WiFi.getKey(), SystemObserver.getWifiConnected(context_));
            requestObj.put(Defines.Jsonkey.UIMode.getKey(), SystemObserver.getUIMode(context_));

            String osName = SystemObserver.getOS();
            if (!isNullOrEmptyOrBlank(osName)) {
                requestObj.put(Defines.Jsonkey.OS.getKey(), osName);
            }

            requestObj.put(Defines.Jsonkey.OSVersion.getKey(), SystemObserver.getOSVersion());

            String countryCode = SystemObserver.getISO2CountryCode();
            if (!TextUtils.isEmpty(countryCode)) {
                requestObj.put(Defines.Jsonkey.Country.getKey(), countryCode);
            }

            String languageCode = SystemObserver.getISO2LanguageCode();
            if (!TextUtils.isEmpty(languageCode)) {
                requestObj.put(Defines.Jsonkey.Language.getKey(), languageCode);
            }

            String localIpAddr = SystemObserver.getLocalIPAddress();
            if ((!TextUtils.isEmpty(localIpAddr))) {
                requestObj.put(Defines.Jsonkey.LocalIP.getKey(), localIpAddr);
            }

        } catch (JSONException ignore) {

        }
    }

    /**
     * Update the given server request JSON with user data. Used for V2 events
     *
     * @param requestObj JSON object for Branch server request
     */
    void updateRequestWithV2Params(Context context, PrefHelper prefHelper, JSONObject requestObj) {
        try {
            SystemObserver.UniqueId hardwareID = getHardwareID();
            if (!isNullOrEmptyOrBlank(hardwareID.getId()) && hardwareID.isReal()) {
                requestObj.put(Defines.Jsonkey.AndroidID.getKey(), hardwareID.getId());
            } else {
                requestObj.put(Defines.Jsonkey.UnidentifiedDevice.getKey(), true);
            }

            String brandName = SystemObserver.getPhoneBrand();
            if (!isNullOrEmptyOrBlank(brandName)) {
                requestObj.put(Defines.Jsonkey.Brand.getKey(), brandName);
            }

            String modelName = SystemObserver.getPhoneModel();
            if (!isNullOrEmptyOrBlank(modelName)) {
                requestObj.put(Defines.Jsonkey.Model.getKey(), modelName);
            }

            DisplayMetrics displayMetrics = SystemObserver.getScreenDisplay(context_);
            requestObj.put(Defines.Jsonkey.ScreenDpi.getKey(), displayMetrics.densityDpi);
            requestObj.put(Defines.Jsonkey.ScreenHeight.getKey(), displayMetrics.heightPixels);
            requestObj.put(Defines.Jsonkey.ScreenWidth.getKey(), displayMetrics.widthPixels);

            String osName = SystemObserver.getOS();
            if (!isNullOrEmptyOrBlank(osName)) {
                requestObj.put(Defines.Jsonkey.OS.getKey(), osName);
            }

            requestObj.put(Defines.Jsonkey.OSVersion.getKey(), SystemObserver.getOSVersion());

            String countryCode = SystemObserver.getISO2CountryCode();
            if (!TextUtils.isEmpty(countryCode)) {
                requestObj.put(Defines.Jsonkey.Country.getKey(), countryCode);
            }

            String languageCode = SystemObserver.getISO2LanguageCode();
            if (!TextUtils.isEmpty(languageCode)) {
                requestObj.put(Defines.Jsonkey.Language.getKey(), languageCode);
            }

            String localIpAddr = SystemObserver.getLocalIPAddress();
            if ((!TextUtils.isEmpty(localIpAddr))) {
                requestObj.put(Defines.Jsonkey.LocalIP.getKey(), localIpAddr);
            }

            if (prefHelper != null) {
                if (!isNullOrEmptyOrBlank(prefHelper.getDeviceFingerPrintID())) {
                    requestObj.put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper.getDeviceFingerPrintID());
                }
                String devId = prefHelper.getIdentity();
                if (!isNullOrEmptyOrBlank(devId)) {
                    requestObj.put(Defines.Jsonkey.DeveloperIdentity.getKey(), devId);
                }
            }

            requestObj.put(Defines.Jsonkey.AppVersion.getKey(), getAppVersion());
            requestObj.put(Defines.Jsonkey.SDK.getKey(), "android");
            requestObj.put(Defines.Jsonkey.SdkVersion.getKey(), BuildConfig.VERSION_NAME);
            requestObj.put(Defines.Jsonkey.UserAgent.getKey(), getDefaultBrowserAgent(context));
        } catch (JSONException ignore) {
        }
    }

    /**
     * ONLY used for CPID/LATD endpoints
     * Update the given server request JSON with device params
     *
     * @param requestObj JSON object for Branch server request
     */
    void updateRequestWithV2CPIDParams(Context context, PrefHelper prefHelper, JSONObject requestObj) {
        try {

            requestObj.put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper.getDeviceFingerPrintID());
            //requestObj.put(Jsonkey.DeviceFingerprintID.getKey(), "694966626071736477");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * get the package name for the this application
     *
     * @return {@link String} with package name value
     */
    public String getPackageName() {
        return SystemObserver.getPackageName(context_);
    }

    /**
     * Gets the version name for this application
     *
     * @return {@link String} with app version value
     */
    public String getAppVersion() {
        return SystemObserver.getAppVersion(context_);
    }

    /**
     * @return the time at which the app was first installed, in milliseconds.
     */
    public long getFirstInstallTime() {
        return SystemObserver.getFirstInstallTime(context_);
    }

    /**
     * @return the time at which the app was last updated, in milliseconds.
     */
    public long getLastUpdateTime() {
        return SystemObserver.getLastUpdateTime(context_);
    }

    /**
     * Determine if the package is installed, vs. if this is an "Instant" app.
     * @return true if the package is installed.
     */
    public boolean isPackageInstalled() {
        return SystemObserver.isPackageInstalled(context_);
    }

    /**
     * @return true if a Debug HardwareId is needed.
     * Note that if either Debug is enabled or Fetch has been disabled, then requests for a Hardware Id will return a "fake" ID.
     */
    static public boolean isDebugHardwareIdNeeded() {
        boolean isDebugHardwareIdNeeded = Branch.isDeviceIDFetchDisabled() || BranchUtil.isDebugEnabled();
        return isDebugHardwareIdNeeded;
    }

    /**
     * @return the device Hardware ID.
     * Note that if either Debug is enabled or Fetch has been disabled, then return a "fake" ID.
     */
    public SystemObserver.UniqueId getHardwareID() {
        return getSystemObserver().getUniqueID(context_, isDebugHardwareIdNeeded());
    }

    public String getOsName() {
        return systemObserver_.getOS();
    }

    // PRS : User agent is checked only from api-17
    private String getDefaultBrowserAgent(Context context) {
        String userAgent = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                userAgent = WebSettings.getDefaultUserAgent(context);
            } catch (Exception ignore) {
                // A known Android issue. Webview packages are not accessible while any updates for chrome is in progress.
                // https://bugs.chromium.org/p/chromium/issues/detail?id=506369
            }
        }
        return userAgent;
    }

    /**
     * Concrete SystemObserver implementation
     */
    private class SystemObserverInstance extends SystemObserver {
        public SystemObserverInstance() {
            super();
        }
    }

    /**
     * @return the current SystemObserver instance
     */
    SystemObserver getSystemObserver() {
        return systemObserver_;
    }

    public static boolean isNullOrEmptyOrBlank(String str) {
        return TextUtils.isEmpty(str) || str.equals(SystemObserver.BLANK);
    }


}
