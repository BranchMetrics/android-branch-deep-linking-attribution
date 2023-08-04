package io.branch.referral;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.installreferrer.api.ReferrerDetails;

import io.branch.coroutines.InstallReferrersKt;
import io.branch.referral.interfaces.GoogleInstallReferrerEvents;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class StoreReferrerGooglePlayStore extends AppStoreReferrer{
    static boolean hasBeenUsed = false;
    static boolean erroredOut = false;

    static Long clickTimestamp = Long.MIN_VALUE;
    static Long installBeginTimestamp = Long.MIN_VALUE;
    static String rawReferrer = null;

    public static void fetch(final Context context, GoogleInstallReferrerEvents googleInstallReferrerEvents) {
        hasBeenUsed = true;

        InstallReferrersKt.getGooglePlayStoreReferrerDetails(context, new Continuation<ReferrerDetails>() {
            @NonNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NonNull Object o) {
                PrefHelper.Debug("getGooglePlayStoreReferrerDetails resumeWith " + o);
                if (o != null) {
                    try {
                        ReferrerDetails referrerDetails = (ReferrerDetails) o;
                        rawReferrer = referrerDetails.getInstallReferrer();
                        clickTimestamp = referrerDetails.getReferrerClickTimestampSeconds();
                        installBeginTimestamp = referrerDetails.getInstallBeginTimestampSeconds();

                        // TODO: We can get rid of InstantAppUtil.java with this one line
                        //  boolean isInstantApp = referrerDetails.getGooglePlayInstantParam();
                    }
                    catch (Exception e) {
                        PrefHelper.Debug(e.getMessage());
                        erroredOut = true;
                    }
                }

                if(googleInstallReferrerEvents != null){
                    googleInstallReferrerEvents.onGoogleInstallReferrerFetched();
                }
            }
        });
    }
}
