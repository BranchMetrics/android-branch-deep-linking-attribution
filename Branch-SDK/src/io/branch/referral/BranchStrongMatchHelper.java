package io.branch.referral;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.text.TextUtils;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sojanpr on 9/2/16.
 * <p>
 * Class for handling strong matching using chrome custom tabs. Class will create  a custom
 * strong match url with device params and open the url in a chrome tab. Branch can use the shared
 * cookie jar for doing  a strong match.
 * </p>
 */
public class BranchStrongMatchHelper {

    private static BranchStrongMatchHelper branchStrongMatchHelper_;
    CustomTabsClient mClient_ = null;
    private static final String APP_LINK_FORMAT = "^https://.+.app.link/";
    private static final String BNC_LINK_FORMAT = "^https://bnc.lt/";
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


    public void checkForStrongMatch(Context context, String sessionSharedLink, DeviceInfo deviceInfo, final PrefHelper prefHelper, final StrongMatchCheckEvents callback) {
        //Check if strong match checked in last 30 days
        if (System.currentTimeMillis() - prefHelper.getLastStrongMatchTime() < THIRTY_DAYS_EPOCH_MILLI_SEC) {
            updateStrongMatchCheckFinished(callback);
        } else {
            try {
                if (deviceInfo.isHardwareIDReal() && deviceInfo.getHardwareID() != null) {
                    final Uri strongMatchUri = buildStrongMatchUrl(sessionSharedLink, deviceInfo, prefHelper);
                    if (strongMatchUri != null) {
                        timeOutHandler_.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                updateStrongMatchCheckFinished(callback);
                            }
                        }, STRONG_MATCH_CHECK_TIME_OUT);
                        CustomTabsClient.bindCustomTabsService(context, "com.android.chrome", new CustomTabsServiceConnection() {
                            @Override
                            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                                mClient_ = client;
                                mClient_.warmup(0);
                                CustomTabsSession session = mClient_.newSession(null);
                                session.mayLaunchUrl(strongMatchUri, null, null);
                                prefHelper.saveLastStrongMatchTime(System.currentTimeMillis());
                            }

                            @Override
                            public void onServiceDisconnected(ComponentName name) {
                                mClient_ = null;
                                updateStrongMatchCheckFinished(callback);
                            }
                        });
                    } else {
                        updateStrongMatchCheckFinished(callback);
                    }
                } else {
                    updateStrongMatchCheckFinished(callback);
                    Log.d("BranchSDK", "Cannot use cookie-based matching while setDebug is enabled");
                }
            } catch (Exception ignore) {
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

    private Uri buildStrongMatchUrl(String sessionReferredLink, DeviceInfo deviceInfo, PrefHelper prefHelper) {
        Uri strongMatchUri = null;
        String hostString = "";
        if (!TextUtils.isEmpty(sessionReferredLink) && !TextUtils.isEmpty(Uri.parse(sessionReferredLink).getHost())) {
            Pattern pattern = Pattern.compile(BNC_LINK_FORMAT);
            Matcher matcher = pattern.matcher(sessionReferredLink);
            if (matcher.find()) {
                hostString = matcher.group(0);
            } else {
                pattern = Pattern.compile(APP_LINK_FORMAT);
                matcher = pattern.matcher(sessionReferredLink);
                String host = Uri.parse(sessionReferredLink).getHost();
                if (matcher.find()) {
                    hostString = matcher.group(0);
                }
            }

            if (!TextUtils.isEmpty(hostString)) {
                String uriString = "";
                uriString += hostString + "_strong_match?os=" + deviceInfo.getOsName();
                // Add HW ID
                uriString += "&" + Defines.Jsonkey.HardwareID.getKey() + "=" + deviceInfo.getHardwareID();
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
        }
        return strongMatchUri;
    }

    interface StrongMatchCheckEvents {
        void onStrongMatchCheckFinished();
    }
}