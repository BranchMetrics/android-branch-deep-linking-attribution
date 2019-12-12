package io.branch.referral;

import android.Manifest;
import android.app.UiModeManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import io.branch.referral.Defines.ModuleNameKeys;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static android.content.Context.UI_MODE_SERVICE;

/**
 * <p>Class that provides a series of methods providing access to commonly used, device-wide
 * attributes and parameters used by the Branch class, and made publicly available for use by
 * other classes.</p>
 */
abstract class SystemObserver {

    /**
     * Default value for when no value has been returned by a system information call, but where
     * null is not supported or desired.
     */
    static final String BLANK = "bnc_no_value";

    private static final String UUID_EMPTY = "00000000-0000-0000-0000-000000000000";
    private String GAIDString_ = null;
    private int LATVal_ = 0;

    /* Needed to avoid duplicating GAID initialization from App.onCreate and Activity.onStart */
    private String AIDInitializationSessionID_;

    /**
     * <p>Gets the {@link String} value of the {@link Secure#ANDROID_ID} setting in the device. This
     * immutable value is generated upon initial device setup, and re-used throughout the life of
     * the device.</p>
     * <p>If <i>true</i> is provided as a parameter, the method will return a different,
     * randomly-generated value each time that it is called. This allows you to simulate many different
     * user devices with different ANDROID_ID values with a single physical device or emulator.</p>
     *
     * @param debug A {@link Boolean} value indicating whether to run in <i>real</i> or <i>debug mode</i>.
     * @return <p>A {@link UniqueId} value representing the unique ANDROID_ID of the device, or a randomly-generated
     * debug value in place of a real identifier.</p>
     */
    static UniqueId getUniqueID(Context context, boolean debug) {
        return new UniqueId(context, debug);
    }

    /**
     * Get the package name for this application.
     * @param context Context.
     * @return {@link String} with value as package name. Empty String in case of error
     */
    static String getPackageName(Context context) {
        String packageName = "";
        if (context != null) {
            try {
                final PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                packageName = packageInfo.packageName;
            } catch (Exception e) {
                PrefHelper.LogException("Error obtaining PackageName", e);
            }
        }
        return packageName;
    }

    /**
     * Get the App Version Name of the current application that the SDK is integrated with.
     * @param context Context.
     * @return {@link String} value containing the full package name.  BLANK in case of error
     */
    static String getAppVersion(Context context) {
        String appVersion = "";
        if (context != null) {
            try {
                final PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                appVersion = packageInfo.versionName;
            } catch (Exception e) {
                PrefHelper.LogException("Error obtaining AppVersion", e);
            }
        }
        return (TextUtils.isEmpty(appVersion) ? BLANK : appVersion);
    }

    /**
     * Get the time at which the app was first installed, in milliseconds.
     * @param context Context.
     * @return the time at which the app was first installed.
     */
    static long getFirstInstallTime(Context context) {
        long firstTime = 0L;
        if (context != null) {
            try {
                final PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                firstTime = packageInfo.firstInstallTime;
            } catch (Exception e) {
                PrefHelper.LogException("Error obtaining FirstInstallTime", e);
            }
        }

        return firstTime;
    }

    /**
     * Determine if the package is installed.
     * @param context Context
     * @return true if the package is installed.
     */
    static boolean isPackageInstalled(Context context) {
        boolean isInstalled = false;
        if (context != null) {
            try {
                final PackageManager packageManager = context.getPackageManager();
                Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                if (intent == null) {
                    return false;
                }
                List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

                isInstalled = (list != null && list.size() > 0);
            } catch (Exception e) {
                PrefHelper.LogException("Error obtaining PackageInfo", e);
            }
        }

        return isInstalled;
    }

    /**
     * Get the time at which the app was last updated, in milliseconds.
     * @param context Context.
     * @return the time at which the app was last updated.
     */
    static long getLastUpdateTime(Context context) {
        long lastTime = 0L;
        if (context != null) {
            try {
                final PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                lastTime = packageInfo.lastUpdateTime;
            } catch (Exception e) {
                PrefHelper.LogException("Error obtaining LastUpdateTime", e);
            }
        }

        return lastTime;
    }

    /**
     * <p>Returns the hardware manufacturer of the current device, as defined by the manufacturer.
     * </p>
     *
     * @return A {@link String} value containing the hardware manufacturer of the current device.
     * @see <a href="http://developer.android.com/reference/android/os/Build.html#MANUFACTURER">
     * Build.MANUFACTURER</a>
     */
    static String getPhoneBrand() {
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
    static String getPhoneModel() {
        return android.os.Build.MODEL;
    }

    /**
     * Gets default ISO2 Country code
     *
     * @return A string representing the ISO2 Country code (eg US, IN)
     */
    static String getISO2CountryCode() {
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
    static String getISO2LanguageCode() {
        if (Locale.getDefault() != null) {
            return Locale.getDefault().getLanguage();
        } else {
            return "";
        }
    }

    /**
    * Helper function to determine of the device is running Fire OS
    */
    private static boolean isFireOSDevice() {
        return getPhoneBrand().equalsIgnoreCase("amazon");
    }

    /**
     * <p>Hard-coded value, used by the Branch object to differentiate between iOS, Web and Android
     * SDK versions.</p>
     * <p>Not of practical use in your application.</p>
     *
     * @return A {@link String} value that indicates the broad OS type that is in use on the device.
     */
    static String getOS(Context context) {
        if (isFireOSDevice()) {
            if (context == null) {
                return getPhoneModel().contains("AFT") ? "AMAZON_FIRE_TV" : "AMAZON_FIRE";
            } else if (context.getPackageManager().hasSystemFeature("amazon.hardware.fire_tv")) {
                return "AMAZON_FIRE_TV";
            }
            return "AMAZON_FIRE";
        }
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
    static int getAPILevel() {
        return android.os.Build.VERSION.SDK_INT;
    }

    static String getOSVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * Returns the CPU type of the device.
     *
     * @return A {@link String} value representing the CPU type.</a>
     */
    static String getCPUType() {
        return System.getProperty("os.arch");
    }

    /**
     * Returns the device build ID.
     *
     * @return A {@link String} value representing the device build ID.</a>
     */
    static String getDeviceBuildId() {
        return Build.DISPLAY;
    }

    /**
     * Returns the device locale in the format "en_US".
     *
     * @return A {@link String} value representing the device locale.</a>
     */
    static String getLocale() {
        return Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry();
    }

    /**
     * Returns the device connection type, wifi or mobile.
     *
     * @return A {@link String} value representing the device connection type.</a>
     */
    @SuppressWarnings("MissingPermission")
    static String getConnectionType(Context context) {
        if (context != null && PackageManager.PERMISSION_GRANTED ==
                context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connManager != null) {
                NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        return "wifi";
                    } else {
                        return "mobile";
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the device carrier.
     *
     * @return A {@link String} value representing the device carrier.</a>
     */
    static String getCarrier(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) return null;
        String carrier = tm.getNetworkOperatorName();
        return TextUtils.isEmpty(carrier) ? null : carrier;
    }

    /**
     * <p>This method returns a {@link DisplayMetrics} object that contains the attributes of the
     * default display of the device that the SDK is running on. Use this when you need to know the
     * dimensions of the screen, density of pixels on the display or any other information that
     * relates to the device screen.</p>
     * <p>Especially useful when operating without an Activity context, e.g. from a background
     * service.</p>
     *
     * @param context Context.
     * @return <p>A {@link DisplayMetrics} object representing the default display of the device.</p>
     * @see DisplayMetrics
     */
    static DisplayMetrics getScreenDisplay(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        if (context != null) {
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (windowManager != null) {
                Display display = windowManager.getDefaultDisplay();
                display.getMetrics(displayMetrics);
            }
        }
        return displayMetrics;
    }

    /**
     * <p>Use this method to query the system state to determine whether a WiFi connection is
     * available for use by applications installed on the device.</p>
     * This applies only to WiFi connections, and does not indicate whether there is
     * a viable Internet connection available; if connected to an offline WiFi router for instance,
     * the boolean will still return <i>true</i>.
     *
     * @param context Context.
     * @return <p>
     * A {@link boolean} value that indicates whether a WiFi connection exists and is open.
     * </p>
     * <ul>
     * <li><i>true</i> - A WiFi connection exists and is open.</li>
     * <li><i>false</i> - Not connected to WiFi.</li>
     * </ul>
     */
    @SuppressWarnings("MissingPermission")
    static boolean getWifiConnected(Context context) {
        if (context != null && PackageManager.PERMISSION_GRANTED == context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifiInfo = null;
            if (connManager != null) {
                wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            }
            return ((wifiInfo != null) && wifiInfo.isConnected());
        }
        return false;
    }

    /**
     * Method to prefetch the GAID and LAT values.
     *
     * @param context Context.
     * @param callback {@link AdsParamsFetchEvents} instance to notify process completion
     * @return {@link Boolean} with true if GAID fetch process started.
     */
    boolean prefetchAdsParams(Context context, AdsParamsFetchEvents callback) {
        AIDInitializationSessionID_ = PrefHelper.getInstance(context).getSessionID();
        boolean isPrefetchStarted = false;
        if (isFireOSDevice()) {
            if (context == null) return isPrefetchStarted;
            try {
                ContentResolver cr = context.getContentResolver();
                setLAT(Secure.getInt(cr, "limit_ad_tracking"));
                setGAID(Secure.getString(cr, "advertising_id"));
                // Don't save advertising id if it's empty/all zeroes/lat=true
                if (TextUtils.isEmpty(GAIDString_) || GAIDString_.equals(UUID_EMPTY) || LATVal_ == 1) {
                    setGAID(null);
                }
                if (callback != null) {
                    callback.onAdsParamsFetchFinished();
                }
            } catch (Settings.SettingNotFoundException ignored) {}
        } else {
            isPrefetchStarted = true;
            new GAdsPrefetchTask(context, callback).executeTask();
        }
        return isPrefetchStarted;
    }

    interface AdsParamsFetchEvents {
        void onAdsParamsFetchFinished();
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
    static String getUIMode(Context context) {
        String mode = "UI_MODE_TYPE_UNDEFINED";
        UiModeManager modeManager = null;

        try {
            if (context != null) {
                modeManager = (UiModeManager) context.getSystemService(UI_MODE_SERVICE);
            }

            if (modeManager != null) {
                switch (modeManager.getCurrentModeType()) {
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

                    case 0:
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            // Have seen reports of "DeadSystemException" from UiModeManager.
        }
        return mode;
    }

    /**
     * Unique Hardware Id.
     * This wraps both the fetching of the ANDROID_ID with knowledge if it is a "fake" id used
     * for debugging or simulating installs.
     */
    static class UniqueId {
        private String uniqueId;
        private boolean isRealId;

        UniqueId(Context context, boolean isDebug) {
            this.isRealId = !isDebug;
            this.uniqueId = BLANK;

            String androidID = null;
            if (context != null) {
                if (!isDebug && !Branch.isSimulatingInstalls()) {
                    androidID = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
                }
            }

            if (androidID == null) {
                androidID = UUID.randomUUID().toString();
                isRealId = false;
            }
            uniqueId = androidID;
        }

        String getId() {
            return this.uniqueId;
        }

        boolean isReal() {
            return this.isRealId;
        }

        @Override
        public boolean equals(Object other) {
            // self check
            if (this == other)
                return true;

            // null check
            if (other == null)
                return false;

            // type check and cast
            if (getClass() != other.getClass())
                return false;

            UniqueId uidOther = (UniqueId) other;

            // field comparison
            return this.uniqueId.equals(uidOther.uniqueId)
                    && this.isRealId == uidOther.isRealId;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1 + (isRealId ? 1 : 0);

            return (prime * result + ((uniqueId == null) ? 0 : uniqueId.hashCode()));
        }
    }

    String getAID() {
        return GAIDString_;
    }

    int getLATVal() {
        return LATVal_;
    }

    void setGAID(String gaid) {
        GAIDString_ = gaid;
    }

    void setLAT(int lat) {
        LATVal_ = lat;
    }

    /**
     * Get IP address from the Module injected key-value pairs
     */
    static String getImei(Context context_) {
        String imei = PrefHelper.getInstance(context_)
                .getSecondaryRequestMetaData(ModuleNameKeys.imei.getKey());
        if (!TextUtils.isEmpty(imei)) {
            return imei;
        }
        return null;
    }

    String getAIDInitializationSessionID() {
        return AIDInitializationSessionID_;
    }
}
