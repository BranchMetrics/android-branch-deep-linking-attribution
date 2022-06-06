package io.branch.referral;

import android.content.Context;

import com.huawei.hms.ads.installreferrer.api.InstallReferrerClient;
import com.huawei.hms.ads.installreferrer.api.InstallReferrerStateListener;
import com.huawei.hms.ads.installreferrer.api.ReferrerDetails;

import java.util.Timer;
import java.util.TimerTask;

public class StoreReferrerHuaweiAppGallery extends AppStoreReferrer{
    private static IHuaweiInstallReferrerEvents callback_ = null;
    static boolean hasBeenUsed = false;
    static boolean erroredOut = false;

    public static void fetch(final Context context, IHuaweiInstallReferrerEvents iHuaweiInstallReferrerEvents) {
        callback_ = iHuaweiInstallReferrerEvents;
        hasBeenUsed = true;

        try {
            final InstallReferrerClient mReferrerClient = InstallReferrerClient.newBuilder(context).build();

            mReferrerClient.startConnection(new InstallReferrerStateListener() {
                @Override
                public void onInstallReferrerSetupFinished(int responseCode) {
                    PrefHelper.Debug("Huawei AppGallery onInstallReferrerSetupFinished, responseCode = " + responseCode);

                    switch (responseCode) {
                        case InstallReferrerClient.InstallReferrerResponse.OK:
                            try {
                                ReferrerDetails referrerDetails = mReferrerClient.getInstallReferrer();

                                String rawReferrer = referrerDetails.getInstallReferrer();
                                long clickTimeStamp = referrerDetails.getReferrerClickTimestampSeconds();
                                long installBeginTimeStamp = referrerDetails.getInstallBeginTimestampSeconds();

                                mReferrerClient.endConnection();
                                onReferrerClientFinished(context, rawReferrer, clickTimeStamp, installBeginTimeStamp, mReferrerClient.getClass().getName());
                            }
                            catch (Exception e) {
                                PrefHelper.Debug(e.getMessage());
                                onReferrerClientError();
                            }
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                        case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                        case InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED:
                        case InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR:
                            onReferrerClientError();
                            break;
                    }
                }

                @Override
                public void onInstallReferrerServiceDisconnected() {
                    PrefHelper.Debug("Huawei AppGallery onInstallReferrerServiceDisconnected");
                }
            });

            new Timer().schedule(new TimerTask() {
                @Override public void run() {
                    PrefHelper.Debug("Huawei Store Referrer fetch lock released by timer");
                    reportInstallReferrer();
                }
            }, 1500);
        }
        catch (Exception exception) {
            PrefHelper.Debug(exception.getMessage());
            exception.printStackTrace();
        }
    }

    private static void onReferrerClientError() {
        erroredOut = true;
        reportInstallReferrer();
    }

    interface IHuaweiInstallReferrerEvents {
        void onHuaweiInstallReferrerEventsFinished();
    }

    public static void reportInstallReferrer() {
        if (callback_ != null) {
            callback_.onHuaweiInstallReferrerEventsFinished();
            callback_ = null;
        }
    }

    protected static void onReferrerClientFinished(Context context, String rawReferrerString, long clickTS, long InstallBeginTS, String clientName) {
        PrefHelper.Debug(clientName + " onReferrerClientFinished()");
        processReferrerInfo(context, rawReferrerString, clickTS, InstallBeginTS);
        reportInstallReferrer();
    }
}
