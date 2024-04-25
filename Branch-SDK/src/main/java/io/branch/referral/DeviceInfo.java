package io.branch.referral;

import static android.content.Context.UI_MODE_SERVICE;
import static io.branch.referral.PrefHelper.NO_STRING_VALUE;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.coroutines.DeviceSignalsKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

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

            String anonID = SystemObserver.getAnonID(context_);
            if (!isNullOrEmptyOrBlank(anonID)) {
                requestObj.put(Defines.Jsonkey.AnonID.getKey(), anonID);
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

            if (serverRequest.isInitializationOrEventRequest()) {
                requestObj.put(Defines.Jsonkey.CPUType.getKey(), SystemObserver.getCPUType());
                requestObj.put(Defines.Jsonkey.DeviceBuildId.getKey(), SystemObserver.getDeviceBuildId());
                requestObj.put(Defines.Jsonkey.Locale.getKey(), SystemObserver.getLocale());
                requestObj.put(Defines.Jsonkey.ConnectionType.getKey(), SystemObserver.getConnectionType(context_));
                requestObj.put(Defines.Jsonkey.DeviceCarrier.getKey(), SystemObserver.getCarrier(context_));
                requestObj.put(Defines.Jsonkey.OSVersionAndroid.getKey(), SystemObserver.getOSVersion());
            }
        } catch (JSONException e) {
            BranchLogger.w("Caught JSONException" + e.getMessage());
        }
    }

    /**
     * Detects TV devices.
     *
     * @return a {@link Boolean} indicating whether the device is a television set.
     */
    boolean isTV() {
        UiModeManager uiModeManager = (UiModeManager) context_.getSystemService(UI_MODE_SERVICE);
        if (uiModeManager == null) {
            BranchLogger.v("uiModeManager is null, mark this as a non-TV device by default.");
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

            String anonID = SystemObserver.getAnonID(context_);
            if (!isNullOrEmptyOrBlank(anonID)) {
                userDataObj.put(Defines.Jsonkey.AnonID.getKey(), anonID);
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

            setPostUserAgent(userDataObj);

            if (serverRequest instanceof ServerRequestGetLATD) {
                userDataObj.put(Defines.Jsonkey.LATDAttributionWindow.getKey(),
                        ((ServerRequestGetLATD) serverRequest).getAttributionWindow());
            }

            if (serverRequest.isInitializationOrEventRequest()) {
                userDataObj.put(Defines.Jsonkey.CPUType.getKey(), SystemObserver.getCPUType());
                userDataObj.put(Defines.Jsonkey.DeviceBuildId.getKey(), SystemObserver.getDeviceBuildId());
                userDataObj.put(Defines.Jsonkey.Locale.getKey(), SystemObserver.getLocale());
                userDataObj.put(Defines.Jsonkey.ConnectionType.getKey(), SystemObserver.getConnectionType(context_));
                userDataObj.put(Defines.Jsonkey.DeviceCarrier.getKey(), SystemObserver.getCarrier(context_));
                userDataObj.put(Defines.Jsonkey.OSVersionAndroid.getKey(), SystemObserver.getOSVersion());
            }

            AttributionReportingManager attributionManager = new AttributionReportingManager();
            if (attributionManager.isMeasurementApiEnabled()) {
                userDataObj.put(Defines.Jsonkey.Privacy_Sandbox.getKey(), true);
            }

        } catch (JSONException e) {
            BranchLogger.w("Caught JSONException" + e.getMessage());
        }
    }

    /**
     * Method to append the user agent string to the POST request body's user_data object
     * If the user agent string is empty, either because it was not obtained asynchronously
     * or on time, query it synchronously.
     * @param userDataObj
     */
    private void setPostUserAgent(final JSONObject userDataObj) {
        BranchLogger.v("setPostUserAgent " + Thread.currentThread().getName());
        try {
            if (!TextUtils.isEmpty(Branch._userAgentString)) {
                BranchLogger.v("userAgent was cached: " + Branch._userAgentString);

                userDataObj.put(Defines.Jsonkey.UserAgent.getKey(), Branch._userAgentString);

                Branch.getInstance().requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.USER_AGENT_STRING_LOCK);
                Branch.getInstance().requestQueue_.processNextQueueItem("setPostUserAgent");
            }
            else if (Branch.userAgentSync) {
                // If user agent sync is false, then the async coroutine is executed instead but may not have finished yet.
                BranchLogger.v("Start invoking getUserAgentSync from thread " + Thread.currentThread().getName());
                DeviceSignalsKt.getUserAgentSync(context_, new Continuation<String>() {
                    @NonNull
                    @Override
                    public CoroutineContext getContext() {
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    @Override
                    public void resumeWith(@NonNull Object o) {
                        if (o != null) {
                            Branch._userAgentString = (String) o;
                            BranchLogger.v("onUserAgentStringFetchFinished getUserAgentSync resumeWith releasing lock");

                            try {
                                userDataObj.put(Defines.Jsonkey.UserAgent.getKey(), Branch._userAgentString);
                            }
                            catch (JSONException e) {
                                BranchLogger.w("Caught JSONException " + e.getMessage());
                            }
                        }

                        Branch.getInstance().requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.USER_AGENT_STRING_LOCK);
                        Branch.getInstance().requestQueue_.processNextQueueItem("onUserAgentStringFetchFinished");
                    }
                });
            }
            // In cases where v2 events objects are enqueued before an init, this will execute first.
            else {
                DeviceSignalsKt.getUserAgentAsync(context_, new Continuation<String>() {
                    @NonNull
                    @Override
                    public CoroutineContext getContext() {
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    @Override
                    public void resumeWith(@NonNull Object o) {
                        if (o != null) {
                            Branch._userAgentString = (String) o;
                            BranchLogger.v("onUserAgentStringFetchFinished getUserAgentAsync resumeWith releasing lock");

                            try {
                                userDataObj.put(Defines.Jsonkey.UserAgent.getKey(), Branch._userAgentString);
                            }
                            catch (JSONException e) {
                                BranchLogger.w("Caught JSONException " + e.getMessage());
                            }
                        }

                        Branch.getInstance().requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.USER_AGENT_STRING_LOCK);
                        Branch.getInstance().requestQueue_.processNextQueueItem("getUserAgentAsync resumeWith");
                    }
                });
            }
        }
        catch (Exception exception){
            BranchLogger.w("Caught exception trying to set userAgent " + exception.getMessage());
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
