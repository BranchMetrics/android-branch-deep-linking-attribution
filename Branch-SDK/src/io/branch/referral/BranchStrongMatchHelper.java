package io.branch.referral;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Created by sojanpr on 9/2/16.
 * <p>
 * Class for handling strong matching using chrome custom tabs. Class will create  a custom
 * strong match url with device params and open the url in a chrome tab. Branch can use the shared
 * cookie jar for doing  a strong match.
 * </p>
 */
class BranchStrongMatchHelper {

    private static BranchStrongMatchHelper branchStrongMatchHelper_;
    Object mClient_ = null;
    private static final int STRONG_MATCH_CHECK_TIME_OUT = 500; // Time to wait for strong match check
    private static final long THIRTY_DAYS_EPOCH_MILLI_SEC = 30 * 24 * 60 * 60 * 1000L;

    private final Handler timeOutHandler_;


    private BranchStrongMatchHelper() {
        timeOutHandler_ = new Handler();
    }

    public static BranchStrongMatchHelper getInstance() {
        if (branchStrongMatchHelper_ == null) {
            branchStrongMatchHelper_ = new BranchStrongMatchHelper();
        }
        return branchStrongMatchHelper_;
    }


    public void checkForStrongMatch(Context context, String cookieMatchDomain, DeviceInfo deviceInfo, final PrefHelper prefHelper, SystemObserver systemObserver, final StrongMatchCheckEvents callback) {
        //Check if strong match checked in last 30 days
        if (System.currentTimeMillis() - prefHelper.getLastStrongMatchTime() < THIRTY_DAYS_EPOCH_MILLI_SEC) {
            updateStrongMatchCheckFinished(callback);
        } else {
            try {
                if (/*deviceInfo.isHardwareIDReal() &&*/ deviceInfo.getHardwareID() != null) {
                    final Uri strongMatchUri = buildStrongMatchUrl(cookieMatchDomain, deviceInfo, prefHelper, systemObserver);
                    if (strongMatchUri != null) {
                        timeOutHandler_.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                updateStrongMatchCheckFinished(callback);
                            }
                        }, STRONG_MATCH_CHECK_TIME_OUT);


                        final Class<?> CustomTabsClientClass = Class.forName("android.support.customtabs.CustomTabsClient");
                        final Class<?> CustomServiceTabConnectionClass = Class.forName("android.support.customtabs.CustomTabsServiceConnection");
                        final Class<?> CustomTabsCallbackClass = Class.forName("android.support.customtabs.CustomTabsCallback");
                        final Class<?> CustomTabsSessionClass = Class.forName("android.support.customtabs.CustomTabsSession");

                        Method bindCustomTabsServiceMethod = CustomTabsClientClass.getMethod("bindCustomTabsService", Context.class, String.class, CustomServiceTabConnectionClass);
                        final Method warmupMethod = CustomTabsClientClass.getMethod("warmup", long.class);
                        final Method newSessionMethod = CustomTabsClientClass.getMethod("newSession", CustomTabsCallbackClass);
                        final Method mayLaunchUrlMethod = CustomTabsSessionClass.getMethod("mayLaunchUrl", Uri.class, Bundle.class, List.class);

                        InvocationHandler CustomServiceTabConnectionHandler = new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                if (method.getName().equals("onCustomTabsServiceConnected") && args[1] != null) {
                                    mClient_ = CustomTabsClientClass.cast(args[1]);
                                    if (mClient_ != null) {
                                        warmupMethod.invoke(mClient_, 0);
                                        Object customTabsSessionObj = newSessionMethod.invoke(mClient_, null);
                                        if (customTabsSessionObj != null) {
                                            mayLaunchUrlMethod.invoke(customTabsSessionObj, strongMatchUri, null, null);
                                            prefHelper.saveLastStrongMatchTime(System.currentTimeMillis());
                                        }
                                    }
                                } else if (method.getName().equals("onServiceDisconnected")) {
                                    mClient_ = null;
                                    updateStrongMatchCheckFinished(callback);
                                }
                                return null;
                            }
                        };
                        Object customServiceTabConnListener = Proxy.newProxyInstance(CustomServiceTabConnectionClass.getClassLoader()
                                , new Class<?>[]{CustomServiceTabConnectionClass}
                                , CustomServiceTabConnectionHandler);
                        bindCustomTabsServiceMethod.invoke(CustomTabsClientClass, context, "com.android.chrome", customServiceTabConnListener);

                    } else {
                        updateStrongMatchCheckFinished(callback);
                    }
                } else {
                    updateStrongMatchCheckFinished(callback);
                    Log.d("BranchSDK", "Cannot use cookie-based matching while setDebug is enabled");
                }
            } catch (Throwable ignore) {
                updateStrongMatchCheckFinished(callback);
            }
        }
    }

    private void updateStrongMatchCheckFinished(StrongMatchCheckEvents callback) {
        if (callback != null) {
            callback.onStrongMatchCheckFinished();
            callback = null;
        }
    }

    private Uri buildStrongMatchUrl(String matchDomain, DeviceInfo deviceInfo, PrefHelper prefHelper, SystemObserver systemObserver) {
        Uri strongMatchUri = null;
        if (!TextUtils.isEmpty(matchDomain)) {
            String uriString = "https://" + matchDomain + "/_strong_match?os=" + deviceInfo.getOsName();
            // Add HW ID
            uriString += "&" + Defines.Jsonkey.HardwareID.getKey() + "=" + deviceInfo.getHardwareID();
            // Add GAID if available
            if (systemObserver.GAIDString_ != null) {
                uriString += "&" + Defines.Jsonkey.GoogleAdvertisingID.getKey() + "=" + systemObserver.GAIDString_;
            }
            // Add device finger print if available
            if (!prefHelper.getDeviceFingerPrintID().equals(PrefHelper.NO_STRING_VALUE)) {
                uriString += "&" + Defines.Jsonkey.DeviceFingerprintID.getKey() + "=" + prefHelper.getDeviceFingerPrintID();
            }
            //Add App version
            if (!deviceInfo.getAppVersion().equals(SystemObserver.BLANK)) {
                uriString += "&" + Defines.Jsonkey.AppVersion.getKey() + "=" + deviceInfo.getAppVersion();
            }
            //Add Branch key
            if (!prefHelper.getBranchKey().equals(PrefHelper.NO_STRING_VALUE)) {
                uriString += "&" + Defines.Jsonkey.BranchKey.getKey() + "=" + prefHelper.getBranchKey();
            }
            //Add SDK version
            uriString += "&sdk=android" + RemoteInterface.SDK_VERSION;

            strongMatchUri = Uri.parse(uriString);

        }
        return strongMatchUri;
    }

    interface StrongMatchCheckEvents {
        void onStrongMatchCheckFinished();
    }
}