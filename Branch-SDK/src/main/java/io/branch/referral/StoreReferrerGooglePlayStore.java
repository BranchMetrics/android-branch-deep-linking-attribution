package io.branch.referral;

import android.content.Context;
import android.os.RemoteException;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;

import java.util.Timer;
import java.util.TimerTask;

public class StoreReferrerGooglePlayStore extends AppStoreReferrer{
    private static IGoogleInstallReferrerEvents callback_ = null;
    static boolean hasBeenUsed = false;
    static boolean erroredOut = false;

    static Long clickTimestamp = Long.MIN_VALUE;
    static Long installBeginTimestamp = Long.MIN_VALUE;
    static String rawReferrer = null;

    public static void fetch(final Context context, IGoogleInstallReferrerEvents iGoogleInstallReferrerEvents) {
        callback_ = iGoogleInstallReferrerEvents;
        hasBeenUsed = true;

        final InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(context).build();

        referrerClient.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(final int responseCode) {
                PrefHelper.Debug("Google Play onInstallReferrerSetupFinished, responseCode = " + responseCode);

                switch (responseCode) {
                    case InstallReferrerClient.InstallReferrerResponse.OK:
                        try {
                            ReferrerDetails response = referrerClient.getInstallReferrer();

                            if (response != null) {
                                rawReferrer = response.getInstallReferrer();
                                clickTimestamp = response.getReferrerClickTimestampSeconds();
                                installBeginTimestamp = response.getInstallBeginTimestampSeconds();
                            }

                            referrerClient.endConnection();
                            onReferrerClientFinished(context, rawReferrer, clickTimestamp, installBeginTimestamp, referrerClient.getClass().getName());
                        }
                        catch (RemoteException ex) {
                            PrefHelper.Debug("onInstallReferrerSetupFinished() Remote Exception: " + ex.getMessage());
                            onReferrerClientError();
                        }
                        catch (Exception ex) {
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

        new Timer().schedule(new TimerTask() {
            @Override public void run() {
                PrefHelper.Debug("Google Store Referrer fetch lock released by timer");
                reportInstallReferrer();
            }
        }, 1500);
    }

    private static void onReferrerClientError() {
        erroredOut = true;
        reportInstallReferrer();
    }

    public static void reportInstallReferrer() {
        if (callback_ != null) {
            callback_.onGoogleInstallReferrerEventsFinished();
            callback_ = null;
        }
    }

    interface IGoogleInstallReferrerEvents {
        void onGoogleInstallReferrerEventsFinished();
    }

    protected static void onReferrerClientFinished(Context context, String rawReferrerString, long clickTS, long InstallBeginTS, String clientName) {
        PrefHelper.Debug(clientName + " onReferrerClientFinished() Referrer: " + rawReferrerString + " Click Timestamp: " + clickTS + " Install Timestamp: "  + InstallBeginTS);
        reportInstallReferrer();
    }
}
