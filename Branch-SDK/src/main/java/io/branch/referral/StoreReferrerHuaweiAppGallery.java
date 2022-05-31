package io.branch.referral;

import android.content.Context;

import com.huawei.hms.ads.installreferrer.api.InstallReferrerClient;
import com.huawei.hms.ads.installreferrer.api.InstallReferrerStateListener;
import com.huawei.hms.ads.installreferrer.api.ReferrerDetails;

public class StoreReferrerHuaweiAppGallery {
    public static void fetch(final Context context) {
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
                                StoreReferrer.onReferrerClientFinished(context, rawReferrer, clickTimeStamp, installBeginTimeStamp, mReferrerClient.getClass().getName());
                            }
                            catch (Exception e) {
                                PrefHelper.Debug(e.getMessage());
                                StoreReferrer.onReferrerClientError();
                            }
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                        case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                        case InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED:
                        case InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR:
                            StoreReferrer.onReferrerClientError();
                            break;
                    }
                }

                @Override
                public void onInstallReferrerServiceDisconnected() {
                    PrefHelper.Debug("Huawei AppGallery onInstallReferrerServiceDisconnected");
                }
            });
        }
        catch (Exception exception) {
            PrefHelper.Debug(exception.getMessage());
            exception.printStackTrace();
        }
    }
}
