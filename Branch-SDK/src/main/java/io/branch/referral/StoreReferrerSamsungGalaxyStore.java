package io.branch.referral;

import android.content.Context;

import androidx.annotation.NonNull;

import com.samsung.android.sdk.sinstallreferrer.api.ReferrerDetails;

import io.branch.coroutines.InstallReferrersKt;
import io.branch.referral.interfaces.SamsungInstallReferrerEvents;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class StoreReferrerSamsungGalaxyStore extends AppStoreReferrer{
    static boolean hasBeenUsed = false;
    static boolean erroredOut = false;

    static Long clickTimestamp = Long.MIN_VALUE;
    static Long installBeginTimestamp = Long.MIN_VALUE;
    static String rawReferrer = null;

    public static void fetch(final Context context, SamsungInstallReferrerEvents samsungInstallReferrerEvents) {
        hasBeenUsed = true;

        InstallReferrersKt.getSamsungGalaxyStoreReferrerDetails(context, new Continuation<ReferrerDetails>() {
            @NonNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NonNull Object o) {
                PrefHelper.Debug("getSamsungGalaxyStoreReferrerDetails resumeWith " + o);
                if(o != null) {
                    try {
                        ReferrerDetails referrerDetails = (ReferrerDetails) o;
                        rawReferrer = referrerDetails.getInstallReferrer();
                        clickTimestamp = referrerDetails.getReferrerClickTimestampSeconds();
                        installBeginTimestamp = referrerDetails.getInstallBeginTimestampSeconds();
                    }
                    catch (Exception e) {
                        PrefHelper.Debug(e.getMessage());
                        erroredOut = true;
                    }
                }

                if(samsungInstallReferrerEvents != null){
                    samsungInstallReferrerEvents.onSamsungInstallReferrerFetched();
                }
            }
        });
    }
}
