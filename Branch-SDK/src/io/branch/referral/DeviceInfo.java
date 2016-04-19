package io.branch.referral;

import android.util.DisplayMetrics;

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
     * Device os name
     */
    private final String osName_;
    /**
     * Device OS version
     */
    private final int osVersion_;

    private static DeviceInfo thisInstance_ = null;

    /**
     * Get the singleton instance for deviceInfo class
     *
     * @param sysObserver {@link SystemObserver} instance to get device info
     * @return {@link DeviceInfo} global instance
     */
    public static DeviceInfo getInstance(boolean isExternalDebug, SystemObserver sysObserver) {
        if (thisInstance_ == null) {
            thisInstance_ = new DeviceInfo(isExternalDebug, sysObserver);
        }
        return thisInstance_;
    }

    private DeviceInfo(boolean isExternalDebug, SystemObserver sysObserver) {
        hardwareID_ = sysObserver.getUniqueID(isExternalDebug);
        isHardwareIDReal_ = sysObserver.hasRealHardwareId();
        brandName_ = sysObserver.getPhoneBrand();
        modelName_ = sysObserver.getPhoneModel();

        DisplayMetrics dMetrics = sysObserver.getScreenDisplay();
        screenDensity_ = dMetrics.densityDpi;
        screenHeight_ = dMetrics.heightPixels;
        screenWidth_ = dMetrics.widthPixels;

        isWifiConnected_ = sysObserver.getWifiConnected();

        osName_ = sysObserver.getOS();
        osVersion_ = sysObserver.getOSVersion();

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

        } catch (JSONException ignore) {

        }
    }
}
