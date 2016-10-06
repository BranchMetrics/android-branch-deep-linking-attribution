package io.branch.referral;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

/**
 * <p>Class that provides a series of methods providing access to commonly used, device-wide
 * attributes and parameters used by the Branch class, and made publically available for use by
 * other classes.</p>
 */
class SystemObserver {

    /**
     * Default value for when no value has been returned by a system information call, but where
     * null is not supported or desired.
     */
    public static final String BLANK = "bnc_no_value";

    private static final int STATE_FRESH_INSTALL = 0;
    private static final int STATE_UPDATE = 2;
    private static final int STATE_NO_CHANGE = 1;

    private static final int GAID_FETCH_TIME_OUT = 1500;
    String GAIDString_ = null;
    int LATVal_ = 0;


    private Context context_;

    /**
     * <p>Indicates whether or not a real device ID is in use, or if a debug value is in use.</p>
     */
    private boolean isRealHardwareId;

    /**
     * <p>Sole constructor method of the {@link SystemObserver} class. Instantiates the value of
     * <i>isRealHardware</i> {@link Boolean} value as <i>true</i>.</p>
     *
     * @param context Current application context
     */
    public SystemObserver(Context context) {
        context_ = context;
        isRealHardwareId = true;
    }

    /**
     * <p>Gets the {@link String} value of the {@link Secure#ANDROID_ID} setting in the device. This
     * immutable value is generated upon initial device setup, and re-used throughout the life of
     * the device.</p>
     * <p/>
     * <p>If <i>true</i> is provided as a parameter, the method will return a different,
     * randomly-generated value each time that it is called. This allows you to simulate many different
     * user devices with different ANDROID_ID values with a single physical device or emulator.</p>
     *
     * @param debug A {@link Boolean} value indicating whether to run in <i>real</i> or <i>debug mode</i>.
     * @return <p>A {@link String} value representing the unique ANDROID_ID of the device, or a randomly-generated
     * debug value in place of a real identifier.</p>
     */
    public String getUniqueID(boolean debug) {
        if (context_ != null) {
            String androidID = null;
            if (!debug) {
                androidID = Secure.getString(context_.getContentResolver(), Secure.ANDROID_ID);
            }
            if (androidID == null) {
                androidID = UUID.randomUUID().toString();
                isRealHardwareId = false;
            }
            return androidID;
        } else
            return BLANK;
    }

    /**
     * <p>Checks the value of the <i>isRealHardWareId</i> {@link Boolean} value within the current
     * instance of the class. If not set by the {@link SystemObserver#isRealHardwareId}
     * or will default to true upon class instantiation.</p>
     *
     * @return <p>A {@link Boolean} value indicating whether or not the current device has a hardware
     * identifier; {@link Secure#ANDROID_ID}</p>
     */
    public boolean hasRealHardwareId() {
        return isRealHardwareId;
    }

    /**
     * Get the package name for this application
     *
     * @return {@link String} with value as package name. Empty String in case of error
     */
    public String getPackageName() {
        String packageName = "";
        try {
            PackageInfo info = context_.getPackageManager().getPackageInfo(context_.getPackageName(), 0);
            packageName = info.packageName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return packageName;
    }

    /**
     * <p>Provides the package name of the current app, and passes it to the
     * {@link SystemObserver#getURIScheme(String)} method to enable the call without a {@link String}
     * parameter.</p>
     * <p/>
     * <p>This method should be used for retrieving the URI scheme of the current application.</p>
     *
     * @return A {@link String} value containing the response from {@link SystemObserver#getURIScheme(String)}.
     */
    public String getURIScheme() {
        return getURIScheme(context_.getPackageName());
    }

    /**
     * <p>Gets the URI scheme of the specified package from its AndroidManifest.xml file.</p>
     * <p/>
     * <p>This method should be used for retrieving the URI scheme of the another application of
     * which the package name is known.</p>
     *
     * @param packageName A {@link String} containing the full package name of the app to check.
     * @return <p>A {@link String} containing the output of {@link ApkParser#decompressXML(byte[])}.</p>
     */
    public String getURIScheme(String packageName) {
        String scheme = BLANK;
        if (!isLowOnMemory()) {
            PackageManager pm = context_.getPackageManager();
            try {
                ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
                String sourceApk = ai.publicSourceDir;
                JarFile jf = null;
                InputStream is = null;
                byte[] xml;
                try {
                    jf = new JarFile(sourceApk);
                    is = jf.getInputStream(jf.getEntry("AndroidManifest.xml"));
                    xml = new byte[is.available()];
                    //noinspection ResultOfMethodCallIgnored
                    is.read(xml);
                    scheme = new ApkParser().decompressXML(xml);
                } catch (Exception ignored) {
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
                    } catch (IOException ignored) {
                    }
                }
            } catch (NameNotFoundException ignored) {
            }
        }
        return scheme;
    }

    /**
     * <p>Checks the current device's {@link ActivityManager} system service and returns the value
     * of the lowMemory flag.</p>
     *
     * @return <p>A {@link Boolean} value representing the low memory flag of the current device.</p>
     * <p/>
     * <ul>
     * <li><i>true</i> - the free memory on the current device is below the system-defined threshold
     * that triggers the low memory flag.</li>
     * <li><i>false</i> - the device has plenty of free memory.</li>
     * </ul>
     */
    private boolean isLowOnMemory() {
        ActivityManager activityManager = (ActivityManager) context_.getSystemService(Context.ACTIVITY_SERVICE);
        MemoryInfo mi = new MemoryInfo();
        activityManager.getMemoryInfo(mi);
        return mi.lowMemory;
    }

    /**
     * <p>Gets a {@link JSONArray} object containing a list of the applications that are installed
     * on the current device.</p>
     * <p/>
     * <p>The method gets a handle on the {@link PackageManager} and calls the
     * {@link PackageManager#getInstalledApplications(int)} method to retrieve a {@link List} of
     * {@link ApplicationInfo} objects, each representing a single application that is installed on
     * the current device.</p>
     * <p/>
     * <p>For each of these items, the method gets the attributes shown below, and constructs a
     * {@link JSONArray} representation of the list, which can be consumed by a JSON parser on the
     * server.</p>
     * <p/>
     * <ul>
     * <li>loadLabel</li>
     * <li>packageName</li>
     * <li>publicSourceDir</li>
     * <li>sourceDir</li>
     * <li>publicSourceDir</li>
     * <li>uriScheme</li>
     * </ul>
     *
     * @return <p>A {@link JSONArray} containing information about all of the applications installed on the
     * current device.</p>
     */
    @SuppressLint("NewApi")
    public JSONArray getListOfApps() {
        JSONArray arr = new JSONArray();
        PackageManager pm = context_.getPackageManager();

        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        if (packages != null) {
            for (ApplicationInfo appInfo : packages) {
                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1) {
                    JSONObject packObj = new JSONObject();
                    try {
                        CharSequence labelCs = appInfo.loadLabel(pm);
                        String label = labelCs == null ? null : labelCs.toString();
                        if (label != null)
                            packObj.put("name", label);
                        String packName = appInfo.packageName;
                        if (packName != null) {
                            packObj.put(Defines.Jsonkey.AppIdentifier.getKey(), packName);
                            String uriScheme = getURIScheme(packName);
                            if (!uriScheme.equals(SystemObserver.BLANK))
                                packObj.put(Defines.Jsonkey.URIScheme.getKey(), uriScheme);
                        }
                        String pSourceDir = appInfo.publicSourceDir;
                        if (pSourceDir != null)
                            packObj.put("public_source_dir", pSourceDir);
                        String sourceDir = appInfo.sourceDir;
                        if (sourceDir != null)
                            packObj.put("source_dir", sourceDir);

                        PackageInfo packInfo = pm.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS);
                        if (packInfo != null) {
                            if (packInfo.versionCode >= 9) {
                                packObj.put("install_date", packInfo.firstInstallTime);
                                packObj.put("last_update_date", packInfo.lastUpdateTime);
                            }
                            packObj.put("version_code", packInfo.versionCode);
                            if (packInfo.versionName != null)
                                packObj.put("version_name", packInfo.versionName);
                        }
                        packObj.put(Defines.Jsonkey.OS.getKey(), this.getOS());

                        arr.put(packObj);
                    } catch (JSONException | NameNotFoundException ignore) {
                    }
                }
            }
        }
        return arr;
    }

    /**
     * <p>Gets the package name of the current application that the SDK is integrated with.</p>
     *
     * @return <p>A {@link String} value containing the full package name of the application that the SDK is
     * currently integrated into.</p>
     */
    public String getAppVersion() {
        try {
            PackageInfo packageInfo = context_.getPackageManager().getPackageInfo(context_.getPackageName(), 0);
            if (packageInfo.versionName != null)
                return packageInfo.versionName;
            else
                return BLANK;
        } catch (NameNotFoundException ignored) {
        }
        return BLANK;
    }

    /**
     * <p>Returns the hardware manufacturer of the current device, as defined by the manufacturer.
     * </p>
     *
     * @return A {@link String} value containing the hardware manufacturer of the current device.
     * @see <a href="http://developer.android.com/reference/android/os/Build.html#MANUFACTURER">
     * Build.MANUFACTURER</a>
     */
    public String getPhoneBrand() {
        return android.os.Build.MANUFACTURER;
    }

    /**
     * <p>Returns the hardware model of the current device, as defined by the manufacturer.</p>
     *
     * @return A {@link String} value containing the hardware model of the current device.
     * @see <a href="http://developer.android.com/reference/android/os/Build.html#MODEL">
     * Build.MODEL
     * </a>
     */
    public String getPhoneModel() {
        return android.os.Build.MODEL;
    }

    /**
     * Gets os locale string as it is represented in the http request header.
     *
     * @return A string representing the locale in format "ISO2Language-ISO2Country" (Eg. "ml-IN"). An empty string is returned on error
     */
    public String getLocaleString() {
        if (Locale.getDefault() != null) {
            return Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry();
        } else {
            return "";
        }
    }

    /**
     * <p>Hard-coded value, used by the Branch object to differentiate between iOS, Web and Android
     * SDK versions.</p>
     * <p/>
     * <p>Not of practical use in your application.</p>
     *
     * @return A {@link String} value that indicates the broad OS type that is in use on the device.
     */
    public String getOS() {
        return "Android";
    }

    /**
     * Returns the Android API version of the current device as an {@link Integer}.
     * <p/>
     * Common values:
     * <ul>
     * <li>22 - Android 5.1, Lollipop MR1</li>
     * <li>21 - Android 5.0, Lollipop</li>
     * <li>19 - Android 4.4, Kitkat</li>
     * <li>18 - Android 4.3, Jellybean</li>
     * <li>15 - Android 4.0.4, Ice Cream Sandwich MR1</li>
     * <li>13 - Android 3.2, Honeycomb MR2</li>
     * <li>10 - Android 2.3.4, Gingerbread MR1</li>
     * </ul>
     *
     * @return An {@link Integer} value representing the SDK/Platform Version of the OS of the
     * current device.
     * @see <a href="http://developer.android.com/guide/topics/manifest/uses-sdk-element.html#ApiLevels">
     * Android Developers - API Level and Platform Version</a>
     */
    public int getOSVersion() {
        return android.os.Build.VERSION.SDK_INT;
    }

    /**
     * <p>As the Build fingerprint is determined by the OS fingerprint, this will identify whether a
     * an emulator is being used where the developer of said emulator has followed convention and
     * used <b>generic</b> as the first segment of the virtual device fingerprint.</p>
     * <p/>
     * <p/>
     * Example of a <u>real device</u> (Google Nexus 5, Android 5.1):
     * <pre style="background:#fff;padding:10px;border:2px solid silver;">
     * <b>google</b>/hammerhead/hammerhead:5.1/LMY47D/1743759:user/release-keys</pre>
     * <p/>
     * <p/>
     * Example of an <u>emulator</u> (Genymotion Nexus 6 AVD, Android 5.0):
     * <pre style="background:#fff;padding:10px;border:2px solid silver;">
     * <b>generic</b>/vbox86p/vbox86p:5.0/LRX21M/buildbot12160004:userdebug/test-keys</pre>
     *
     * @return <p>A {@link Boolean} value indicating whether the device upon which the app is being run is
     * a simulated platform, i.e. an emulator. Or a real, hardware device.</p>
     * <p/>
     * <ul>
     * <li><i>true</i> - the app is running on an emulator</li>
     * <li><i>false</i> - the app is running on a physical device (or a badly configured AVD)</li>
     * </ul>
     */
    public boolean isSimulator() {
        return android.os.Build.FINGERPRINT.contains("generic");
    }

    /**
     * <p>This method returns an {@link Integer} value dependent on whether the application has been
     * updated since its installation on the device. If the application has just been installed and
     * launched immediately, this will always return 1.</p>
     * <p/>
     * <p>If however the application has already been installed for more than the duration of a
     * single update cycle, and has received one or more updates, the time in
     * {@link PackageInfo#firstInstallTime} will be different from that in
     * {@link PackageInfo#lastUpdateTime} so the return value will be 0; indicative of an update
     * having occurred whilst the app has been installed.</p>
     * <p/>
     * <p>This is useful to know when the manner of handling of deep-link data has changed betwen
     * application versions and where migration of SharedPrefs may be required. This method provides
     * a condition upon which a consistency check or migration validation operation can be carried
     * out.</p>
     * <p/>
     * <p>This will not work on Android SDK versions lower than 9, as the {@link PackageInfo#firstInstallTime}
     * and {@link PackageInfo#lastUpdateTime} values did not exist in older versions of the
     * {@link PackageInfo} class.</p>
     *
     * @param updatePrefs A {@link Boolean} value indicating whether or not current App version
     *                    number should be updated in preferences.
     * @return <p>A {@link Integer} value indicating the update state of the application package.</p>
     * <ul>
     * <li><i>1</i> - App not updated since install.</li>
     * <li><i>0</i> - App has been updated since initial install.</li>
     * </ul>
     */
    @SuppressLint("NewApi")
    public int getUpdateState(boolean updatePrefs) {
        PrefHelper pHelper = PrefHelper.getInstance(context_);
        String currAppVersion = getAppVersion();
        if (PrefHelper.NO_STRING_VALUE.equals(pHelper.getAppVersion())) {
            // if no app version is in storage, this must be the first time Branch is here
            if (updatePrefs) {
                pHelper.setAppVersion(currAppVersion);
            }
            if (android.os.Build.VERSION.SDK_INT >= 9) {
                // if we can access update/install time, use that to check if it's a fresh install or update
                try {
                    PackageInfo packageInfo = context_.getPackageManager().getPackageInfo(context_.getPackageName(), 0);
                    if (packageInfo.lastUpdateTime != packageInfo.firstInstallTime) {
                        return STATE_UPDATE;
                    }
                    return STATE_FRESH_INSTALL;
                } catch (NameNotFoundException ignored) {
                }
            }
            // otherwise, just register an install
            return STATE_FRESH_INSTALL;
        } else if (!pHelper.getAppVersion().equals(currAppVersion)) {
            // if the current app version doesn't match the stored, it's an update
            if (updatePrefs) {
                pHelper.setAppVersion(currAppVersion);
            }
            return STATE_UPDATE;
        }
        // otherwise it's an open
        return STATE_NO_CHANGE;
    }

    /**
     * <p>This method returns a {@link DisplayMetrics} object that contains the attributes of the
     * default display of the device that the SDK is running on. Use this when you need to know the
     * dimensions of the screen, density of pixels on the display or any other information that
     * relates to the device screen.</p>
     * <p/>
     * <p>Especially useful when operating without an Activity context, e.g. from a background
     * service.</p>
     *
     * @return <p>A {@link DisplayMetrics} object representing the default display of the device.</p>
     * @see DisplayMetrics
     */
    public DisplayMetrics getScreenDisplay() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Display display = ((WindowManager) context_.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        display.getMetrics(displayMetrics);
        return displayMetrics;
    }

    /**
     * <p>Use this method to query the system state to determine whether a WiFi connection is
     * available for use by applications installed on the device.</p>
     * <p/>
     * This applies only to WiFi connections, and does not indicate whether there is
     * a viable Internet connection available; if connected to an offline WiFi router for instance,
     * the boolean will still return <i>true</i>.
     *
     * @return <p>
     * A {@link boolean} value that indicates whether a WiFi connection exists and is open.
     * </p>
     * <ul>
     * <li><i>true</i> - A WiFi connection exists and is open.</li>
     * <li><i>false</i> - Not connected to WiFi.</li>
     * </ul>
     */
    public boolean getWifiConnected() {
        if (PackageManager.PERMISSION_GRANTED == context_.checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
            ConnectivityManager connManager = (ConnectivityManager) context_.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return ((wifiInfo != null) && wifiInfo.isConnected());
        }
        return false;
    }


    /**
     * Returns an instance of com.google.android.gms.ads.identifier.AdvertisingIdClient class  to be used
     * for getting GAId and LAT value
     *
     * @return {@link Object} instance of AdvertisingIdClient class
     */
    public Object getAdInfoObject() {
        Object adInfoObj = null;
        try {
            Class<?> AdvertisingIdClientClass = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
            Method getAdvertisingIdInfoMethod = AdvertisingIdClientClass.getMethod("getAdvertisingIdInfo", Context.class);
            adInfoObj = getAdvertisingIdInfoMethod.invoke(null, context_);
        } catch (Throwable ignore) {
        }
        return adInfoObj;
    }


    /**
     * <p>Google now requires that all apps use a standardised Advertising ID for all ad-based
     * actions within Android apps.</p>
     * <p/>
     * <p>The Google Play services APIs expose the advertising tracking ID as UUID such as this:</p>
     * <p/>
     * <pre>38400000-8cf0-11bd-b23e-10b96e40000d</pre>
     *
     * @return <p>A {@link String} value containing the client ad UUID as supplied by Google Play.</p>
     * @see <a href="https://developer.android.com/google/play-services/id.html">
     * Android Developers - Advertising ID</a>
     */
    public String getAdvertisingId(Object adInfoObj) {
        try {
            Method getIdMethod = adInfoObj.getClass().getMethod("getId");
            GAIDString_ = (String) getIdMethod.invoke(adInfoObj);
        } catch (Exception ignore) {
        }
        return GAIDString_;
    }

    /**
     * <p>Get the limit-ad-tracking status of the advertising identifier.</p>
     * <p>Check the Google Play services to for LAT enabled or disabled and return the LAT value as an integer.</p>
     *
     * @return <p> 0 if LAT is disabled else 1.</p>
     * @see <a href="https://developers.google.com/android/reference/com/google/android/gms/ads/identifier/AdvertisingIdClient.Info.html#isLimitAdTrackingEnabled()">
     * Android Developers - Limit Ad Tracking</a>
     */
    public int getLATValue(Object adInfoObj) {
        try {
            Method getLatMethod = adInfoObj.getClass().getMethod("isLimitAdTrackingEnabled");
            LATVal_ = (Boolean) getLatMethod.invoke(adInfoObj) ? 1 : 0;
        } catch (Exception ignore) {
        }
        return LATVal_;
    }

    /**
     * <p>
     * Method to prefetch the GAID and LAT values
     * </p>
     *
     * @param callback {@link GAdsParamsFetchEvents} instance to notify process completion
     * @return {@link Boolean} with true if GAID fetch process started.
     */
    public boolean prefetchGAdsParams(GAdsParamsFetchEvents callback) {
        boolean isPrefetchStarted = false;
        if (TextUtils.isEmpty(GAIDString_)) {
            isPrefetchStarted = true;
            new GAdsPrefetchTask(callback).executeTask();
        }
        return isPrefetchStarted;
    }

    /**
     * <p>
     * Async task to fetch GAID and LAT value
     * This task fetch the GAID and LAT in background. The Background task times out
     * After GAID_FETCH_TIME_OUT
     * </p>
     */
    private class GAdsPrefetchTask extends BranchAsyncTask<Void, Void, Void> {
        private final GAdsParamsFetchEvents callback_;

        public GAdsPrefetchTask(GAdsParamsFetchEvents callback) {
            callback_ = callback;
        }

        @Override
        protected Void doInBackground(Void... params) {

            final CountDownLatch latch = new CountDownLatch(1);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                    Object adInfoObj = getAdInfoObject();
                    getAdvertisingId(adInfoObj);
                    getLATValue(adInfoObj);
                    latch.countDown();
                }
            }).start();

            try {
                //Wait GAID_FETCH_TIME_OUT milli sec max to receive the GAID and LAT
                latch.await(GAID_FETCH_TIME_OUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (callback_ != null) {
                callback_.onGAdsFetchFinished();
            }
        }
    }

    interface GAdsParamsFetchEvents {
        void onGAdsFetchFinished();
    }
}
