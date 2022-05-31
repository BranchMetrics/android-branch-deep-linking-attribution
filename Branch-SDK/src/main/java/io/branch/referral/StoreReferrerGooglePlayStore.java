package io.branch.referral;

import android.content.Context;
import android.os.RemoteException;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;

public class StoreReferrerGooglePlayStore {

    public static void fetch(final Context context) {
        final InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(context).build();

        referrerClient.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(final int responseCode) {
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

                            referrerClient.endConnection();
                            StoreAttribution.onReferrerClientFinished(context, rawReferrer, clickTimeStamp, installBeginTimeStamp, referrerClient.getClass().getName());
                        }
                        catch (RemoteException ex) {
                            PrefHelper.Debug("onInstallReferrerSetupFinished() Remote Exception: " + ex.getMessage());
                            StoreAttribution.onReferrerClientError();
                        }
                        catch (Exception ex) {
                            PrefHelper.Debug("onInstallReferrerSetupFinished() Exception: " + ex.getMessage());
                            StoreAttribution.onReferrerClientError();
                        }
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:// API not available on the current Play Store app
                    case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:// Connection could not be established
                    case InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR:// General errors caused by incorrect usage
                    case InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED:// Play Store service is not connected now - potentially transient state.
                        PrefHelper.Debug("responseCode: " + responseCode);
                        // Play Store service is not connected now - potentially transient state.
                        StoreAttribution.onReferrerClientError();
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
}
