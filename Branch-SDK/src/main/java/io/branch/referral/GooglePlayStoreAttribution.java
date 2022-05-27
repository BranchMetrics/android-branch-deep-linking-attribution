package io.branch.referral;

import android.content.Context;
import android.os.RemoteException;
import android.text.TextUtils;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.miui.referrer.annotation.GetAppsReferrerResponse;
import com.miui.referrer.api.GetAppsReferrerClient;
import com.miui.referrer.api.GetAppsReferrerDetails;
import com.miui.referrer.api.GetAppsReferrerStateListener;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class to access Google Play Referrer Library to get ReferrerDetails object using the InstallReferrerClient.
 */
class StoreAttribution {

    /* Link identifier on installing app from play store. */
    private static String installID_ = PrefHelper.NO_STRING_VALUE;
    private static IInstallReferrerEvents callback_ = null;

    static boolean hasBeenUsed;
    // startConnection appears to throw errors synchronously, so IInstallReferrerEvents gets invoked, removes
    // INSTALL_REFERRER_FETCH_WAIT_LOCK form all requests on the queue but the install request has not
    // even been added to the queue yet. To mitigate this, we use the flag `erroredOut`
    static boolean erroredOut;

    void captureInstallReferrer(final Context context, final long maxWaitTime, IInstallReferrerEvents installReferrerFetch) {
        hasBeenUsed = true;
        callback_ = installReferrerFetch;

        try {
            if (classExists("com.huawei.hms.ads.installreferrer.api.InstallReferrerClient")) {
                tryHuaweiAppGalleryConnection(context);
            }

            if (classExists("com.miui.referrer.api.GetAppsReferrerClient")){
                tryXiaomiGetAppsConnection(context);
            }

            if(classExists("com.sec.android.app.samsungapps.installreferrer.api.InstallReferrerClient")){
                trySamsungGalaxyStoreConnection(context);
            }

            // This is imported by default as it is the most common case
            tryGooglePlayStoreConnection(context);

        }
        catch (Exception ex) {
            PrefHelper.Debug("ReferrerClientWrapper Exception: " + ex.getMessage());
        }

        // Google Play Referrer Library may sometimes not invoke the callback, so we need to release
        // the wait lock if it hasn't been released yet
        new Timer().schedule(new TimerTask() {
            @Override public void run() {
                reportInstallReferrer();
            }
        }, maxWaitTime);
    }

    private void trySamsungGalaxyStoreConnection(final Context context) {
        final com.sec.android.app.samsungapps.installreferrer.api.InstallReferrerClient referrerClient =
                com.sec.android.app.samsungapps.installreferrer.api.InstallReferrerClient.newBuilder(context).build();

        referrerClient.startConnection(new com.sec.android.app.samsungapps.installreferrer.api.InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                switch (responseCode) {
                    case com.sec.android.app.samsungapps.installreferrer.api.InstallReferrerClient.InstallReferrerResponse.OK:
                        // Connection established.
                        try {
                            com.sec.android.app.samsungapps.installreferrer.api.ReferrerDetails details = referrerClient.getInstallReferrer();

                            String rawReferrer = details.getInstallReferrer();
                            long clickTimeStamp = details.getReferrerClickTimestampSeconds();
                            long installBeginTimeStamp = details.getInstallBeginTimestampSeconds();

                            onReferrerClientFinished(context, rawReferrer, clickTimeStamp, installBeginTimeStamp);
                            referrerClient.endConnection();
                        }
                        catch (Exception e){
                            PrefHelper.Debug(e.getMessage());
                            onReferrerClientError();
                        }
                        break;
                    case com.sec.android.app.samsungapps.installreferrer.api.InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED: // API not available on the current Galaxy Store app.
                    case com.sec.android.app.samsungapps.installreferrer.api.InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:// Connection couldn't be established.
                    case com.sec.android.app.samsungapps.installreferrer.api.InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR:
                    case com.sec.android.app.samsungapps.installreferrer.api.InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED:
                        PrefHelper.Debug("responseCode: " + responseCode);
                        onReferrerClientError();
                        break;
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {
                PrefHelper.Debug("onInstallReferrerServiceDisconnected");
            }
        });
    }

    private void tryXiaomiGetAppsConnection(final Context context) {
        final com.miui.referrer.api.GetAppsReferrerClient referrerClient = GetAppsReferrerClient.Companion.newBuilder(context).build();
        referrerClient.startConnection(new GetAppsReferrerStateListener() {
            @Override
            public void onGetAppsReferrerSetupFinished(int responseCode) {
                PrefHelper.Debug("Xiaomi GetApps onGetAppsReferrerSetupFinished, responseCode = " + responseCode);

                switch (responseCode) {
                    case GetAppsReferrerResponse.OK:
                        try {
                            GetAppsReferrerDetails response = referrerClient.getInstallReferrer();

                            String rawReferrer = response.getInstallReferrer();
                            long clickTimeStamp = response.getReferrerClickTimestampSeconds();
                            long installBeginTimeStamp = response.getInstallBeginTimestampSeconds();

                            onReferrerClientFinished(context, rawReferrer, clickTimeStamp, installBeginTimeStamp);
                            referrerClient.endConnection();
                        }
                        catch (Exception e) {
                            PrefHelper.Debug(e.getMessage());
                            onReferrerClientError();
                        }
                        break;
                    case GetAppsReferrerResponse.SERVICE_UNAVAILABLE:
                    case GetAppsReferrerResponse.FEATURE_NOT_SUPPORTED:
                    case GetAppsReferrerResponse.DEVELOPER_ERROR:
                    case GetAppsReferrerResponse.PERMISSION_ERROR:
                        PrefHelper.Debug("responseCode: " + responseCode);
                        onReferrerClientError();
                        break;
                }
            }

            @Override
            public void onGetAppsServiceDisconnected() {
                PrefHelper.Debug("onGetAppsServiceDisconnected");
            }
        });
    }

    private void tryHuaweiAppGalleryConnection(final Context context) {
        final com.huawei.hms.ads.installreferrer.api.InstallReferrerClient mReferrerClient = com.huawei.hms.ads.installreferrer.api.InstallReferrerClient.newBuilder(context).build();
        mReferrerClient.startConnection(new com.huawei.hms.ads.installreferrer.api.InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                PrefHelper.Debug("Huawei AppGallery onInstallReferrerSetupFinished, responseCode = " + responseCode);

                switch (responseCode) {
                    case com.huawei.hms.ads.installreferrer.api.InstallReferrerClient.InstallReferrerResponse.OK:
                        try {
                            com.huawei.hms.ads.installreferrer.api.ReferrerDetails referrerDetails = mReferrerClient.getInstallReferrer();

                            String rawReferrer = referrerDetails.getInstallReferrer();
                            long clickTimeStamp = referrerDetails.getReferrerClickTimestampSeconds();
                            long installBeginTimeStamp = referrerDetails.getInstallBeginTimestampSeconds();

                            onReferrerClientFinished(context, rawReferrer, clickTimeStamp, installBeginTimeStamp);
                        }
                        catch (RemoteException | IOException e) {
                            PrefHelper.Debug( e.getMessage());
                            mReferrerClient.endConnection();
                            onReferrerClientError();
                        }
                        break;
                    case com.huawei.hms.ads.installreferrer.api.InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                    case com.huawei.hms.ads.installreferrer.api.InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                    case com.huawei.hms.ads.installreferrer.api.InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED:
                    case com.huawei.hms.ads.installreferrer.api.InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR:
                        PrefHelper.Debug("responseCode: " + responseCode);
                        onReferrerClientError();
                        break;
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {
                PrefHelper.Debug("onInstallReferrerServiceDisconnected");
            }
        });
    }

    private void tryGooglePlayStoreConnection(final Context context) {
        final InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(context).build();
        referrerClient.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                PrefHelper.Debug("Google Play onInstallReferrerSetupFinished, responseCode = " + responseCode);
                switch (responseCode) {
                    case InstallReferrerClient.InstallReferrerResponse.OK:
                        try {
                            ReferrerDetails response = referrerClient.getInstallReferrer();
                            String rawReferrer = null;
                            long clickTimeStamp = 0L;
                            long installBeginTimeStamp = 0L;
                            if (response != null) {
                                rawReferrer = response.getInstallReferrer();
                                clickTimeStamp = response.getReferrerClickTimestampSeconds();
                                installBeginTimeStamp = response.getInstallBeginTimestampSeconds();
                            }
                            onReferrerClientFinished(context, rawReferrer, clickTimeStamp, installBeginTimeStamp);
                        } catch (RemoteException ex) {
                            PrefHelper.Debug("onInstallReferrerSetupFinished() Remote Exception: " + ex.getMessage());
                            onReferrerClientError();
                        } catch (Exception ex) {
                            PrefHelper.Debug("onInstallReferrerSetupFinished() Exception: " + ex.getMessage());
                            onReferrerClientError();
                        }
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:// API not available on the current Play Store app
                    case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:// Connection could not be established
                    case InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR:// General errors caused by incorrect usage
                    case InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED:// Play Store service is not connected now - potentially transient state.
                        PrefHelper.Debug("responseCode: " + responseCode);
                        // Play Store service is not connected now - potentially transient state.
                        onReferrerClientError();
                        break;
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {
                // "This does not remove install referrer service connection itself - this binding
                // to the service will remain active, and you will receive a call to onInstallReferrerSetupFinished(int)
                // when install referrer service is next running and setup is complete."
                // https://developer.android.com/reference/com/android/installreferrer/api/InstallReferrerStateListener.html#oninstallreferrerservicedisconnected
                PrefHelper.Debug("onInstallReferrerServiceDisconnected()");
            }
        });
    }

    private static void onReferrerClientFinished(Context context, String rawReferrerString, long clickTS, long InstallBeginTS) {
        PrefHelper.Debug("onReferrerClientFinished()");
        processReferrerInfo(context, rawReferrerString, clickTS, InstallBeginTS);
        reportInstallReferrer();
    }

    private static void onReferrerClientError() {
        PrefHelper.Debug("onReferrerClientError()");
        erroredOut = true;
        reportInstallReferrer();
    }

    private static void processReferrerInfo(Context context, String rawReferrerString, long referrerClickTS, long installClickTS) {
        PrefHelper prefHelper = PrefHelper.getInstance(context);
        if (referrerClickTS > 0) {
            prefHelper.setLong(PrefHelper.KEY_REFERRER_CLICK_TS, referrerClickTS);
        }
        if (installClickTS > 0) {
            prefHelper.setLong(PrefHelper.KEY_INSTALL_BEGIN_TS, installClickTS);
        }

        if (rawReferrerString != null &&
                prefHelper.getGooglePlayReferrer().equals(PrefHelper.NO_STRING_VALUE)) {
            try {
                rawReferrerString = URLDecoder.decode(rawReferrerString, "UTF-8");
                HashMap<String, String> referrerMap = new HashMap<>();
                String[] referralParams = rawReferrerString.split("&");

                //Always set the raw referrer string:
                prefHelper.setGooglePlayReferrer(rawReferrerString);
                for (String referrerParam : referralParams) {
                    if (!TextUtils.isEmpty(referrerParam)) {
                        String splitter = "=";
                        if (!referrerParam.contains("=") && referrerParam.contains("-")) {
                            splitter = "-";
                        }
                        String[] keyValue = referrerParam.split(splitter);
                        if (keyValue.length > 1) { // To make sure that there is one key value pair in referrer
                            referrerMap.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
                        }
                    }
                }
                if (referrerMap.containsKey(Defines.Jsonkey.LinkClickID.getKey())) {
                    installID_ = referrerMap.get(Defines.Jsonkey.LinkClickID.getKey());
                    prefHelper.setLinkClickIdentifier(installID_);

                }
                // Check for full app conversion
                if (referrerMap.containsKey(Defines.Jsonkey.IsFullAppConv.getKey())
                        && referrerMap.containsKey(Defines.Jsonkey.ReferringLink.getKey())) {
                    prefHelper.setIsFullAppConversion(Boolean.parseBoolean(referrerMap.get(Defines.Jsonkey.IsFullAppConv.getKey())));
                    prefHelper.setAppLink(referrerMap.get(Defines.Jsonkey.ReferringLink.getKey()));
                }

                if (referrerMap.containsKey(Defines.Jsonkey.GoogleSearchInstallReferrer.getKey())) {
                    prefHelper.setGoogleSearchInstallIdentifier(referrerMap.get(Defines.Jsonkey.GoogleSearchInstallReferrer.getKey()));
                }

                if(referrerMap.containsValue(Defines.Jsonkey.PlayAutoInstalls.getKey())) {
                    BranchPreinstall.setBranchPreInstallGoogleReferrer(context, referrerMap);
                }
                if(referrerMap.containsKey(Defines.Jsonkey.ReferrerExtraGclidParam.getKey())){
                    prefHelper.setReferrerGclid(referrerMap.get(Defines.Jsonkey.ReferrerExtraGclidParam.getKey()));
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                PrefHelper.Debug("Illegal characters in url encoded string");
            }
        }
    }

    // historically been a public API but not advertised
    public static String getInstallationID() {
        return installID_;
    }

    private static void reportInstallReferrer() {
        if (callback_ != null) {
            callback_.onInstallReferrerEventsFinished();
            callback_ = null;
        }
    }

    interface IInstallReferrerEvents {
        void onInstallReferrerEventsFinished();
    }

    public boolean classExists(String className) {
        try  {
            Class.forName(className);
            return true;
        }  catch (ClassNotFoundException e) {
            PrefHelper.Debug("Could not find " + e + ". If expected, import the dependency into your app.");
            return false;
        }
    }
}
