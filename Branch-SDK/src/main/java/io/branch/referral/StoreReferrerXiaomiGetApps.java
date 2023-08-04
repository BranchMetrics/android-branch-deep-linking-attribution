package io.branch.referral;

import android.content.Context;

import androidx.annotation.NonNull;

import com.miui.referrer.api.GetAppsReferrerDetails;

import io.branch.coroutines.InstallReferrersKt;
import io.branch.referral.interfaces.XiaomiInstallReferrerEvents;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class StoreReferrerXiaomiGetApps extends AppStoreReferrer{
    static boolean hasBeenUsed = false;
    static boolean erroredOut = false;

    static Long clickTimestamp = Long.MIN_VALUE;
    static Long installBeginTimestamp = Long.MIN_VALUE;
    static String rawReferrer = null;

    public static void fetch(final Context context, XiaomiInstallReferrerEvents xiaomiInstallReferrerEvents) {
        hasBeenUsed = true;

        InstallReferrersKt.getXiaomiGetAppsReferrerDetails(context, new Continuation<GetAppsReferrerDetails>() {
            @NonNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NonNull Object o) {
                PrefHelper.Debug("getXiaomiGetAppsReferrerDetails resumeWith " + o);
                if(o != null) {
                    try {
                        GetAppsReferrerDetails referrerDetails = (GetAppsReferrerDetails) o;
                        rawReferrer = referrerDetails.getInstallReferrer();
                        clickTimestamp = referrerDetails.getReferrerClickTimestampSeconds();
                        installBeginTimestamp = referrerDetails.getInstallBeginTimestampSeconds();
                    }
                    catch (Exception e) {
                        PrefHelper.Debug(e.getMessage());
                        erroredOut = true;
                    }
                }

                if(xiaomiInstallReferrerEvents != null){
                    xiaomiInstallReferrerEvents.onXiaomiInstallReferrerFetched();
                }
            }
        });
    }
}
