package io.branch.referral;

import android.Manifest;
import android.app.UiModeManager;
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

import static android.content.Context.UI_MODE_SERVICE;

/**
 * <p>Class that provides a series of methods providing access to commonly used, device-wide
 * attributes and parameters used by the Branch class, and made publicly available for use by
 * other classes.</p>
 */
class SystemObserver {

    /**
     * Default value for when no value has been returned by a system information call, but where
     * null is not supported or desired.
     */
    public static final String BLANK = "bnc_no_value";

    private static final int GAID_FETCH_TIME_OUT = 1500;
    static String GAIDString_ = null;
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
    SystemObserver(Context context) {
        context_ = context;
        isRealHardwareId = true;
    }

    /**
     * <p>Gets the {@link String} value of the {@link Secure#ANDROID_ID} setting in the device. This
     * immutable value is generated upon initial device setup, and re-used throughout the life of
     * the device.</p>
     * <p>If <i>true</i> is provided as a parameter, the method will return a different,
     * randomly-generated value each time that it is called. This allows you to simulate many different
     * user devices with different ANDROID_ID values with a single physical device or emulator.</p>
     *
     * @param debug A {@link Boolean} value indicating whether to run in <i>real</i> or <i>debug mode</i>.
     * @return <p>A {@link String} value representing the unique ANDROID_ID of the device, or a randomly-generated
     * debug value in place of a real identifier.</p>
     */
    String getUniqueID(boolean debug) {
        if (context_ != null) {
            String androidID = null;
            if (!debug && !Branch.isSimulatingInstalls_) {
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
    boolean hasRealHardwareId() {
        return isRealHardwareId;
    }

    /**
     * Get the package name for this application
     *
     * @return {@link String} with value as package name. Empty String in case of error
     */
    String getPackageName() {
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
     * <p>This method should be used for retrieving the URI scheme of the current application.</p>
     *
     * @return A {@link String} value containing the response from {@link SystemObserver#getURIScheme(String)}.
     */
    String getURIScheme() {
        return getURIScheme(context_.getPackageName());
    }
    
    /**
     * <p>Gets the URI scheme of the specified package from its AndroidManifest.xml file.</p>
     * <p>This method should be used for retrieving the URI scheme of the another application of
     * which the package name is known.</p>
     *
     * @param packageName A {@link String} containing the full package name of the app to check.
     * @return <p>A {@link String} containing the output of {@link ApkParser#decompressXML(byte[])}.</p>
     */
    private String getURIScheme(String packageName) {
        String scheme = BLANK;
        if (!BranchUtil.isLowOnMemory(context_)) {

            JarFile jf = null;
            InputStream is = null;
            byte[] xml;
            try {
                PackageManager pm = context_.getPackageManager();
                ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
                String sourceApk = ai.publicSourceDir;
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

        }
        return scheme;
    }


    /**
     * <p>Gets the package name of the current application that the SDK is integrated with.</p>
     *
     * @return <p>A {@link String} value containing the full package name of the application that the SDK is
     * currently integrated into.</p>
     */
    String getAppVersion() {
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
    String getPhoneBrand() {
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
    String getPhoneModel() {
        return android.os.Build.MODEL;
    }

    /**
     * Gets default ISO2 Country code
     *
     * @return A string representing the ISO2 Country code (eg US, IN)
     */
    String getISO2CountryCode() {
        if (Locale.getDefault() != null) {
            return Locale.getDefault().getCountry();
        } else {
            return "";
        }
    }

    /**
     * Gets default ISO2 language code
     *
     * @return A string representing the ISO2 language code (eg en, ml)
     */
    String getISO2LanguageCode() {
        if (Locale.getDefault() != null) {
            return Locale.getDefault().getLanguage();
        } else {
            return "";
        }
    }


    /**
     * <p>Hard-coded value, used by the Branch object to differentiate between iOS, Web and Android
     * SDK versions.</p>
     * <p>Not of practical use in your application.</p>
     *
     * @return A {@link String} value that indicates the broad OS type that is in use on the device.
     */
    String getOS() {
        return "Android";
    }

    /**
     * Returns the Android API version of the current device as an {@link Integer}.
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
    int getOSVersion() {
        return android.os.Build.VERSION.SDK_INT;
    }


    

    /**
     * <p>This method returns a {@link DisplayMetrics} object that contains the attributes of the
     * default display of the device that the SDK is running on. Use this when you need to know the
     * dimensions of the screen, density of pixels on the display or any other information that
     * relates to the device screen.</p>
     * <p>Especially useful when operating without an Activity context, e.g. from a background
     * service.</p>
     *
     * @return <p>A {@link DisplayMetrics} object representing the default display of the device.</p>
     * @see DisplayMetrics
     */
    DisplayMetrics getScreenDisplay() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Display display = ((WindowManager) context_.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        display.getMetrics(displayMetrics);
        return displayMetrics;
    }

    /**
     * <p>Use this method to query the system state to determine whether a WiFi connection is
     * available for use by applications installed on the device.</p>
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
    @SuppressWarnings("MissingPermission")
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
    private Object getAdInfoObject() {
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
     * <p>The Google Play services APIs expose the advertising tracking ID as UUID such as this:</p>
     * <pre>38400000-8cf0-11bd-b23e-10b96e40000d</pre>
     *
     * @return <p>A {@link String} value containing the client ad UUID as supplied by Google Play.</p>
     * @see <a href="https://developer.android.com/google/play-services/id.html">
     * Android Developers - Advertising ID</a>
     */
    private String getAdvertisingId(Object adInfoObj) {
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
    private int getLATValue(Object adInfoObj) {
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
    boolean prefetchGAdsParams(GAdsParamsFetchEvents callback) {
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

    /**
     * Get IP address from first non local net Interface
     */
    static String getLocalIPAddress() {
        String ipAddress = "";
        try {
            List<NetworkInterface> netInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface netInterface : netInterfaces) {
                List<InetAddress> addresses = Collections.list(netInterface.getInetAddresses());
                for (InetAddress address : addresses) {
                    if (!address.isLoopbackAddress()) {
                        String ip = address.getHostAddress();
                        boolean isIPv4 = ip.indexOf(':') < 0;
                        if (isIPv4) {
                            ipAddress = ip;
                            break;
                        }
                    }
                }
            }
        } catch (Throwable ignore) {
        }

        return ipAddress;
    }

    /**
     * Return the current running mode type. May be one of
     * {UI_MODE_TYPE_NORMAL Configuration.UI_MODE_TYPE_NORMAL},
     * {UI_MODE_TYPE_DESK Configuration.UI_MODE_TYPE_DESK},
     * {UI_MODE_TYPE_CAR Configuration.UI_MODE_TYPE_CAR},
     * {UI_MODE_TYPE_TELEVISION Configuration.UI_MODE_TYPE_TELEVISION},
     * {#UI_MODE_TYPE_APPLIANCE Configuration.UI_MODE_TYPE_APPLIANCE}, or
     * {#UI_MODE_TYPE_WATCH Configuration.UI_MODE_TYPE_WATCH}.
     */
    String getUIMode() {
        String mode;
        switch (((UiModeManager) context_.getSystemService(UI_MODE_SERVICE)).getCurrentModeType()) {
            case 0:
                mode = "UI_MODE_TYPE_UNDEFINED";
                break;
            case 1:
                mode = "UI_MODE_TYPE_NORMAL";
                break;
            case 2:
                mode = "UI_MODE_TYPE_DESK";
                break;
            case 3:
                mode = "UI_MODE_TYPE_CAR";
                break;
            case 4:
                mode = "UI_MODE_TYPE_TELEVISION";
                break;
            case 5:
                mode = "UI_MODE_TYPE_APPLIANCE";
                break;
            case 6:
                mode = "UI_MODE_TYPE_WATCH";
                break;
            default:
                mode = "UI_MODE_TYPE_UNDEFINED";
                break;
        }
        return mode;
    }
}
