package io.branch.referral;

import android.content.Context;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class to access Google Play Referrer Library to get ReferrerDetails object using the InstallReferrerClient.
 */
class StoreReferrer {

    /* Link identifier on installing app from play store. */
    private static String installID_ = PrefHelper.NO_STRING_VALUE;
    private static IInstallReferrerEvents callback_ = null;

    static boolean hasBeenUsed;
    // startConnection appears to throw errors synchronously, so IInstallReferrerEvents gets invoked, removes
    // INSTALL_REFERRER_FETCH_WAIT_LOCK from all requests on the queue but the install request has not
    // even been added to the queue yet. To mitigate this, we use the flag `erroredOut`
    static boolean erroredOut;

    void captureInstallReferrer(final Context context, final long maxWaitTime, IInstallReferrerEvents installReferrerFetch) {
        hasBeenUsed = true;
        callback_ = installReferrerFetch;

        try {
            //use the initial referrer to determine which OEM app store to use.
            String initialReferrer = PrefHelper.getInstance(context).getInitialReferrer();

            if (initialReferrer != null && initialReferrer.contains("com.huawei.appmarket")) {
                if (classExists("com.huawei.hms.ads.installreferrer.api.InstallReferrerClient")) {
                    StoreReferrerHuaweiAppGallery.fetch(context);
                }
            }

            else if (initialReferrer != null && initialReferrer.contains("com.xiaomi.mipicks")) {
                if (classExists("com.miui.referrer.api.GetAppsReferrerClient")) {
                    StoreReferrerXiaomiGetApps.fetch(context);
                }
            }

            else if (initialReferrer != null && initialReferrer.contains("com.sec.android.app.samsungapps")) {
                if (classExists("com.sec.android.app.samsungapps.installreferrer.api.InstallReferrerClient")) {
                    StoreReferrerSamsungGalaxyStore.fetch(context);
                }
            }
            // Default to play store as before
            else {
                StoreReferrerGooglePlayStore.fetch(context);
            }
        }
        catch (Exception ex) {
            PrefHelper.Debug("ReferrerClientWrapper Exception: " + ex.getMessage());
        }

        // TODO: This may release the lock before the callback finishes
        // Google Play Referrer Library may sometimes not invoke the callback, so we need to release
        // the wait lock if it hasn't been released yet
        new Timer().schedule(new TimerTask() {
            @Override public void run() {
                PrefHelper.Debug("Store Referrer fetch lock released by timer");
                reportInstallReferrer();
            }
        }, maxWaitTime);
    }

    protected static void onReferrerClientFinished(Context context, String rawReferrerString, long clickTS, long InstallBeginTS, String clientName) {
        PrefHelper.Debug(clientName + " onReferrerClientFinished()");
        processReferrerInfo(context, rawReferrerString, clickTS, InstallBeginTS);
        reportInstallReferrer();
    }

    protected static void onReferrerClientError() {
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
        if (rawReferrerString != null) {
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
        PrefHelper.Debug("reportInstallReferrer");
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
            PrefHelper.Debug("Could not find " + className + ". If expected, import the dependency into your app.");
            return false;
        }
    }
}