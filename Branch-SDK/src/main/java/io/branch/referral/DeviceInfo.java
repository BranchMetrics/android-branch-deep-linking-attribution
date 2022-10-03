package io.branch.referral;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

import static android.content.Context.UI_MODE_SERVICE;
import static io.branch.referral.PrefHelper.NO_STRING_VALUE;

/**
 * <p>
 * Class for handling the device params with Branch server requests. responsible for capturing device info and updating
 * device info to Branch requests
 * </p>
 */
class DeviceInfo {
    private final SystemObserver systemObserver_;
    private final Context context_;

    /**
     * Get the singleton instance for this class
     *
     * @return {@link DeviceInfo} instance if already initialised or null
     */
    static DeviceInfo getInstance() {
        Branch b = Branch.getInstance();
        if (b == null) return null;
        return b.getDeviceInfo();
    }

    DeviceInfo(Context context) {
        context_ = context;
        systemObserver_ = new SystemObserverInstance();
    }

    /**
     * Update the given server request JSON with device params
     *
     * @param requestObj JSON object for Branch server request
     */
    void updateRequestWithV1Params(ServerRequest serverRequest, JSONObject requestObj) {
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

            String osName = SystemObserver.getOS(context_);
            if (!isNullOrEmptyOrBlank(osName)) {
                requestObj.put(Defines.Jsonkey.OS.getKey(), osName);
            }

            requestObj.put(Defines.Jsonkey.APILevel.getKey(), SystemObserver.getAPILevel());

            maybeAddTuneFields(serverRequest, requestObj);

            if (Branch.getPluginName() != null) {
                requestObj.put(Defines.Jsonkey.PluginName.getKey(), Branch.getPluginName());
                requestObj.put(Defines.Jsonkey.PluginVersion.getKey(), Branch.getPluginVersion());
            }

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
        } catch (JSONException ignore) { }
    }

    /**
     * Detects TV devices.
     *
     * @return a {@link Boolean} indicating whether the device is a television set.
     */
    boolean isTV() {
        UiModeManager uiModeManager = (UiModeManager) context_.getSystemService(UI_MODE_SERVICE);
        if (uiModeManager == null) {
            PrefHelper.Debug("uiModeManager is null, mark this as a non-TV device by default.");
            return false;
        }
        return uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    /**
     * Update the given server request JSON with user data. Used for V2 events
     *
     * @param userDataObj JSON object for Branch server request
     */
    void updateRequestWithV2Params(ServerRequest serverRequest, PrefHelper prefHelper, JSONObject userDataObj) {
        try {
            SystemObserver.UniqueId hardwareID = getHardwareID();
            if (!isNullOrEmptyOrBlank(hardwareID.getId())) {
                userDataObj.put(Defines.Jsonkey.AndroidID.getKey(), hardwareID.getId());
            }

            String brandName = SystemObserver.getPhoneBrand();
            if (!isNullOrEmptyOrBlank(brandName)) {
                userDataObj.put(Defines.Jsonkey.Brand.getKey(), brandName);
            }

            String modelName = SystemObserver.getPhoneModel();
            if (!isNullOrEmptyOrBlank(modelName)) {
                userDataObj.put(Defines.Jsonkey.Model.getKey(), modelName);
            }

            DisplayMetrics displayMetrics = SystemObserver.getScreenDisplay(context_);
            userDataObj.put(Defines.Jsonkey.ScreenDpi.getKey(), displayMetrics.densityDpi);
            userDataObj.put(Defines.Jsonkey.ScreenHeight.getKey(), displayMetrics.heightPixels);
            userDataObj.put(Defines.Jsonkey.ScreenWidth.getKey(), displayMetrics.widthPixels);
            userDataObj.put(Defines.Jsonkey.UIMode.getKey(), SystemObserver.getUIMode(context_));

            String osName = SystemObserver.getOS(context_);
            if (!isNullOrEmptyOrBlank(osName)) {
                userDataObj.put(Defines.Jsonkey.OS.getKey(), osName);
            }

            userDataObj.put(Defines.Jsonkey.APILevel.getKey(), SystemObserver.getAPILevel());

            maybeAddTuneFields(serverRequest, userDataObj);

            if (Branch.getPluginName() != null) {
                userDataObj.put(Defines.Jsonkey.PluginName.getKey(), Branch.getPluginName());
                userDataObj.put(Defines.Jsonkey.PluginVersion.getKey(), Branch.getPluginVersion());
            }

            String countryCode = SystemObserver.getISO2CountryCode();
            if (!TextUtils.isEmpty(countryCode)) {
                userDataObj.put(Defines.Jsonkey.Country.getKey(), countryCode);
            }

            String languageCode = SystemObserver.getISO2LanguageCode();
            if (!TextUtils.isEmpty(languageCode)) {
                userDataObj.put(Defines.Jsonkey.Language.getKey(), languageCode);
            }

            String localIpAddr = SystemObserver.getLocalIPAddress();
            if ((!TextUtils.isEmpty(localIpAddr))) {
                userDataObj.put(Defines.Jsonkey.LocalIP.getKey(), localIpAddr);
            }

            if (prefHelper != null) {
                if (!isNullOrEmptyOrBlank(prefHelper.getRandomizedDeviceToken())) {
                    userDataObj.put(Defines.Jsonkey.RandomizedDeviceToken.getKey(), prefHelper.getRandomizedDeviceToken());
                }
                String devId = prefHelper.getIdentity();
                if (!isNullOrEmptyOrBlank(devId)) {
                    userDataObj.put(Defines.Jsonkey.DeveloperIdentity.getKey(), devId);
                }

                String appStore = prefHelper.getAppStoreSource();
                if(!NO_STRING_VALUE.equals(appStore)) {
                    userDataObj.put(Defines.Jsonkey.App_Store.getKey(), appStore);
                }
            }

            userDataObj.put(Defines.Jsonkey.AppVersion.getKey(), getAppVersion());
            userDataObj.put(Defines.Jsonkey.SDK.getKey(), "android");
            userDataObj.put(Defines.Jsonkey.SdkVersion.getKey(), Branch.getSdkVersionNumber());
            userDataObj.put(Defines.Jsonkey.UserAgent.getKey(), getDefaultBrowserAgent(context_));

            if (serverRequest instanceof ServerRequestGetLATD) {
                userDataObj.put(Defines.Jsonkey.LATDAttributionWindow.getKey(),
                        ((ServerRequestGetLATD) serverRequest).getAttributionWindow());
            }

        } catch (JSONException ignore) { }
    }

    /**
     * Update the server request with params for all events
     * @param serverRequest
     * @param prefHelper
     * @param requestObj
     */
    void updateRequestWithParamsAllEvents(ServerRequest serverRequest, PrefHelper prefHelper, JSONObject requestObj){
        try {
            // For install events, referrer GCLID is already contained in `install_referrer_extras`
            // Otherwise, for all other v1 and v2 events, add referrer_gclid to top level
            if (!(serverRequest instanceof ServerRequestRegisterInstall)) {
                String gclid = prefHelper.getReferrerGclid();
                if (gclid != null && !gclid.equals(NO_STRING_VALUE)) {
                    requestObj.put(Defines.Jsonkey.ReferrerGclid.getKey(), gclid);
                }
            }
        }
        catch (JSONException ignore){
        }
    }

    private void maybeAddTuneFields(ServerRequest serverRequest, JSONObject requestObj) throws JSONException {
        if (serverRequest.isInitializationOrEventRequest()) {
            // fields for parity with Tune traffic
            requestObj.put(Defines.Jsonkey.CPUType.getKey(), SystemObserver.getCPUType());
            requestObj.put(Defines.Jsonkey.DeviceBuildId.getKey(), SystemObserver.getDeviceBuildId());
            requestObj.put(Defines.Jsonkey.Locale.getKey(), SystemObserver.getLocale());
            requestObj.put(Defines.Jsonkey.ConnectionType.getKey(), SystemObserver.getConnectionType(context_));
            requestObj.put(Defines.Jsonkey.DeviceCarrier.getKey(), SystemObserver.getCarrier(context_));
            requestObj.put(Defines.Jsonkey.OSVersionAndroid.getKey(), SystemObserver.getOSVersion());
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
     * @return the device Hardware ID.
     * Note that if either Debug is enabled or Fetch has been disabled, then return a "fake" ID.
     */
    public SystemObserver.UniqueId getHardwareID() {
        return getSystemObserver().getUniqueID(context_, Branch.isDeviceIDFetchDisabled());
    }

    public String getOsName() {
        return systemObserver_.getOS(context_);
    }


    /**
     * Returns the browser's user agent string
     * PRS : User agent is checked only from api-17
     * @param context
     * @return user agent string
     */
    String getDefaultBrowserAgent(final Context context) {
        if(!TextUtils.isEmpty(Branch._userAgentString)) {
            return Branch._userAgentString;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                PrefHelper.Debug("Unable to retrieve user agent string from WebView instance. Retrieving from WebSettings");
                Branch._userAgentString = WebSettings.getDefaultUserAgent(context);
            }
            catch (Exception exception) {
                PrefHelper.Debug(exception.getMessage());
                // A known Android issue. Webview packages are not accessible while any updates for chrome is in progress.
                // https://bugs.chromium.org/p/chromium/issues/detail?id=506369
            }
        }
        return Branch._userAgentString;
    }

    /**
     * Must be called from the main thread
     * Some devices appear to crash when accessing chromium through the Android framework statics
     * Suggested alternative is to use a webview instance
     * https://bugs.chromium.org/p/chromium/issues/detail?id=1279562
     * https://bugs.chromium.org/p/chromium/issues/detail?id=1271617
     **/
    String getUserAgentStringSync(final Context context){
        if(!TextUtils.isEmpty(Branch._userAgentString)) {
            return Branch._userAgentString;
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    PrefHelper.Debug("Running WebView initialization for user agent on thread " + Thread.currentThread());
                    WebView w = new WebView(context);
                    Branch._userAgentString = w.getSettings().getUserAgentString();
                    w.destroy();
                }
                catch (Exception e) {
                    PrefHelper.Debug(e.getMessage());
                }

            }
        });

        return Branch._userAgentString;
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
