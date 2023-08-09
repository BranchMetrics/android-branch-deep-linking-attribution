package io.branch.referral;

import android.content.Context;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

/**
 * Abstract class for Store Referrers
 */
public class AppStoreReferrer {

    /* Link identifier on installing app from play store. */
    private static String installID_ = PrefHelper.NO_STRING_VALUE;

    public static void processReferrerInfo(Context context, String rawReferrerString, long referrerClickTS, long installClickTS, String store) {
        PrefHelper prefHelper = PrefHelper.getInstance(context);
        if(!TextUtils.isEmpty(store)){
            prefHelper.setAppStoreSource(store);
        }
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
                prefHelper.setAppStoreReferrer(rawReferrerString);
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
}