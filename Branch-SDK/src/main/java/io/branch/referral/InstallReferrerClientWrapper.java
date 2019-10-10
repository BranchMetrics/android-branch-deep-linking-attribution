package io.branch.referral;

import android.content.Context;
import android.os.RemoteException;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;

/**
 * <p>
 * Class for getting the referrer info using the install referrer client. This is need `installreferrer` lib added to the application.
 * This will be working only with compatible version of Play store app. In case of an error this fallback to the install-referrer broadcast
 * </p>
 */
class InstallReferrerClientWrapper {
    private Object mReferrerClient; // Class may be unknown at the time on load if the `installreferrer` is missing
    private Context context_;

    interface InstallReferrerWrapperListener {
        void onReferrerClientFinished(Context context, String rawReferrerString, long clickTS, long InstallBeginTS);
        void onReferrerClientError();
        }

    InstallReferrerClientWrapper(Context context) {
        this.context_ = context;
    }

    boolean getReferrerUsingReferrerClient(final InstallReferrerWrapperListener installReferrerWrapperListener) {
        boolean isReferrerClientAvailable = false;
        try {
            InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(context_).build();
            mReferrerClient = referrerClient;
            referrerClient.startConnection(new InstallReferrerStateListener() {
                @Override
                public void onInstallReferrerSetupFinished(int responseCode) {
                    switch (responseCode) {
                        case InstallReferrerClient.InstallReferrerResponse.OK:
                            try {
                                if (mReferrerClient != null) {
                                    ReferrerDetails response = ((InstallReferrerClient) mReferrerClient).getInstallReferrer();
                                    String rawReferrer = null;
                                    long clickTimeStamp = 0L;
                                    long installBeginTimeStamp = 0L;
                                    if (response != null) {
                                        rawReferrer = response.getInstallReferrer();
                                        clickTimeStamp = response.getReferrerClickTimestampSeconds();
                                        installBeginTimeStamp = response.getInstallBeginTimestampSeconds();
                                    }
                                    installReferrerWrapperListener.onReferrerClientFinished(context_, rawReferrer, clickTimeStamp, installBeginTimeStamp);
                                }
                            } catch (RemoteException ex) {
                                PrefHelper.Debug("onInstallReferrerSetupFinished() Exception: " + ex.getMessage());
                                installReferrerWrapperListener.onReferrerClientError();
                            }
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                            // API not available on the current Play Store app
                            installReferrerWrapperListener.onReferrerClientError();
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                            // Connection could not be established
                            installReferrerWrapperListener.onReferrerClientError();
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR:
                            // General errors caused by incorrect usage
                            installReferrerWrapperListener.onReferrerClientError();
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED:
                            // Play Store service is not connected now - potentially transient state.
                            break;
                    }
                }

                @Override
                public void onInstallReferrerServiceDisconnected() {
                    installReferrerWrapperListener.onReferrerClientError();
                }
            });
            isReferrerClientAvailable = true;
        } catch (Throwable ex) {
            PrefHelper.Debug("ReferrerClientWrapper Exception: " + ex.getMessage());
        }
        return isReferrerClientAvailable;
    }
}

