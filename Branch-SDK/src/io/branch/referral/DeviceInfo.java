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
    /**
     * Immutable device hardware id
     */
    private final String hardwareID_;
    /**
     * Status for test vs real hardware ID
     */
    private final boolean isHardwareIDReal_;
    /*
     * Device manufacturer name
     */
    private final String brandName_;
    /**
     * Device model name
     */
    private final String modelName_;
    /**
     * Screen pixel density
     */
    private final int screenDensity_;
    /**
     * Height of the screen in pixels
     */
    private final int screenHeight_;
    /**
     * Width of the screen in pixels
     */
    private final int screenWidth_;
    /**
     * Status for connected to wifi or not
     */
    private final boolean isWifiConnected_;
    /**
     * Local IP address for the device
     */
    private final String localIpAddr_;
    /**
     * Device os name
     */
    private final String osName_;
    /**
     * Device OS version
     */
    private final int osVersion_;

    private final String packageName_;
    private final String appVersion_;
    private final String countryCode_;
    private final String languageCode_;

    private static DeviceInfo thisInstance_ = null;


    /**
     * Get the singleton instance for deviceInfo class
     *
     * @param sysObserver {@link SystemObserver} instance to get device info
     * @return {@link DeviceInfo} global instance
     */
    public static DeviceInfo getInstance(boolean isExternalDebug, SystemObserver sysObserver, boolean disableAndroidIDFetch) {
        if (thisInstance_ == null) {
            thisInstance_ = new DeviceInfo(isExternalDebug, sysObserver, disableAndroidIDFetch);
        }
        return thisInstance_;
    }

    /**
     * Get the singleton instance for this class
     *
     * @return {@link DeviceInfo} instance if already initialised or null
     */
    public static DeviceInfo getInstance() {
        return thisInstance_;
    }


    private DeviceInfo(boolean isExternalDebug, SystemObserver sysObserver, boolean disableAndroidIDFetch) {
        if (disableAndroidIDFetch) {
            hardwareID_ = sysObserver.getUniqueID(true);
        } else {
            hardwareID_ = sysObserver.getUniqueID(isExternalDebug);
        }
        isHardwareIDReal_ = sysObserver.hasRealHardwareId();
        brandName_ = sysObserver.getPhoneBrand();
        modelName_ = sysObserver.getPhoneModel();

        DisplayMetrics dMetrics = sysObserver.getScreenDisplay();
        screenDensity_ = dMetrics.densityDpi;
        screenHeight_ = dMetrics.heightPixels;
        screenWidth_ = dMetrics.widthPixels;

        isWifiConnected_ = sysObserver.getWifiConnected();
        localIpAddr_ = sysObserver.getLocalIPAddress();

        osName_ = sysObserver.getOS();
        osVersion_ = sysObserver.getOSVersion();

        packageName_ = sysObserver.getPackageName();
        appVersion_ = sysObserver.getAppVersion();
        countryCode_ = sysObserver.getISO2CountryCode();
        languageCode_ = sysObserver.getISO2LanguageCode();
    }

    /**
     * Update the given server request JSON with device params
     *
     * @param requestObj JSON object for Branch server request
     */
    public void updateRequestWithDeviceParams(JSONObject requestObj) {
        try {
            if (!hardwareID_.equals(SystemObserver.BLANK)) {
                requestObj.put(Defines.Jsonkey.HardwareID.getKey(), hardwareID_);
                requestObj.put(Defines.Jsonkey.IsHardwareIDReal.getKey(), isHardwareIDReal_);
            }
            if (!brandName_.equals(SystemObserver.BLANK)) {
                requestObj.put(Defines.Jsonkey.Brand.getKey(), brandName_);
            }
            if (!modelName_.equals(SystemObserver.BLANK)) {
                requestObj.put(Defines.Jsonkey.Model.getKey(), modelName_);
            }
            requestObj.put(Defines.Jsonkey.ScreenDpi.getKey(), screenDensity_);
            requestObj.put(Defines.Jsonkey.ScreenHeight.getKey(), screenHeight_);
            requestObj.put(Defines.Jsonkey.ScreenWidth.getKey(), screenWidth_);
            requestObj.put(Defines.Jsonkey.WiFi.getKey(), isWifiConnected_);


            if (!osName_.equals(SystemObserver.BLANK)) {
                requestObj.put(Defines.Jsonkey.OS.getKey(), osName_);
            }
            requestObj.put(Defines.Jsonkey.OSVersion.getKey(), osVersion_);
            if (!TextUtils.isEmpty(countryCode_)) {
                requestObj.put(Defines.Jsonkey.Country.getKey(), countryCode_);
            }
            if (!TextUtils.isEmpty(languageCode_)) {
                requestObj.put(Defines.Jsonkey.Language.getKey(), languageCode_);
            }
            if ((!TextUtils.isEmpty(localIpAddr_))) {
                requestObj.put(Defines.Jsonkey.LocalIP.getKey(), localIpAddr_);
            }

        } catch (JSONException ignore) {

        }
    }

    /**
     * Update the given server request JSON with user data. Used for V2 events
     *
     * @param requestObj JSON object for Branch server request
     */
    public void updateRequestWithUserData(Context context, PrefHelper prefHelper, JSONObject requestObj) {
        try {
            if (!hardwareID_.equals(SystemObserver.BLANK) && isHardwareIDReal_) {
                requestObj.put(Defines.Jsonkey.AndroidID.getKey(), hardwareID_);
            } else {
                requestObj.put(Defines.Jsonkey.UnidentifiedDevice.getKey(), true);
            }

            if (!brandName_.equals(SystemObserver.BLANK)) {
                requestObj.put(Defines.Jsonkey.Brand.getKey(), brandName_);
            }
            if (!modelName_.equals(SystemObserver.BLANK)) {
                requestObj.put(Defines.Jsonkey.Model.getKey(), modelName_);
            }
            requestObj.put(Defines.Jsonkey.ScreenDpi.getKey(), screenDensity_);
            requestObj.put(Defines.Jsonkey.ScreenHeight.getKey(), screenHeight_);
            requestObj.put(Defines.Jsonkey.ScreenWidth.getKey(), screenWidth_);


            if (!osName_.equals(SystemObserver.BLANK)) {
                requestObj.put(Defines.Jsonkey.OS.getKey(), osName_);
            }
            requestObj.put(Defines.Jsonkey.OSVersion.getKey(), osVersion_);
            if (!TextUtils.isEmpty(countryCode_)) {
                requestObj.put(Defines.Jsonkey.Country.getKey(), countryCode_);
            }
            if (!TextUtils.isEmpty(languageCode_)) {
                requestObj.put(Defines.Jsonkey.Language.getKey(), languageCode_);
            }
            if ((!TextUtils.isEmpty(localIpAddr_))) {
                requestObj.put(Defines.Jsonkey.LocalIP.getKey(), localIpAddr_);
            }
            if (prefHelper != null && !prefHelper.getDeviceFingerPrintID().equals(PrefHelper.NO_STRING_VALUE)) {
                requestObj.put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper.getDeviceFingerPrintID());
            }
            String devId = prefHelper.getIdentity();
            if (devId != null && !devId.equals(PrefHelper.NO_STRING_VALUE)) {
                requestObj.put(Defines.Jsonkey.DeveloperIdentity.getKey(), prefHelper.getIdentity());
            }
            requestObj.put(Defines.Jsonkey.AppVersion.getKey(), DeviceInfo.getInstance().getAppVersion());
            requestObj.put(Defines.Jsonkey.SDK.getKey(), "android");
            requestObj.put(Defines.Jsonkey.SdkVersion.getKey(), BuildConfig.VERSION_NAME);
            requestObj.put(Defines.Jsonkey.UserAgent.getKey(), getDefaultBrowserAgent(context));
        } catch (JSONException ignore) {
        }
    }

    /**
     * get the package name for the this application
     *
     * @return {@link String} with package name value
     */
    public String getPackageName() {
        return packageName_;
    }

    /**
     * Gets the version name for this application
     *
     * @return {@link String} with app version value
     */
    public String getAppVersion() {
        return appVersion_;
    }

    public boolean isHardwareIDReal() {
        return isHardwareIDReal_;
    }

    public String getHardwareID() {
        return hardwareID_.equals(SystemObserver.BLANK) ? null : hardwareID_;
    }

    public String getOsName() {
        return osName_;
    }

    // PRS : User agent is checked only from api-17
    private String getDefaultBrowserAgent(Context context) {
        String userAgent = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            userAgent = WebSettings.getDefaultUserAgent(context);
        }
        return userAgent;
    }

}
