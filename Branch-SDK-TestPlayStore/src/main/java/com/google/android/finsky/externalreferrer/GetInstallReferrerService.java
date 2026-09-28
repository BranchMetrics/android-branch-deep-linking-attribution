package com.google.android.finsky.externalreferrer;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import com.android.vending.ConfigureReferrer;

/**
 * Test Play Store referrer service (installreferrer 2.2 protocol) that answers after DELAY_MS.
 * InstallReferrerSlowPlayStoreTests expects these defaults; ConfigureReferrer overrides them.
 */
public class GetInstallReferrerService extends Service {
    private static final String TAG = "TestPlayStore";
    private static final String DESCRIPTOR = "com.google.android.finsky.externalreferrer.IGetInstallReferrerService";
    private static final int TRANSACTION_GET_INSTALL_REFERRER = 1;
    static final long DELAY_MS = 10_000;
    static final String REFERRER = "utm_source=test_play_store&utm_medium=test";

    private final Binder binder = new Binder() {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code != TRANSACTION_GET_INSTALL_REFERRER) {
                return super.onTransact(code, data, reply, flags);
            }
            data.enforceInterface(DESCRIPTOR);
            Bundle request = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
            String caller = request == null ? null : request.getString("package_name");
            SharedPreferences config = getSharedPreferences(ConfigureReferrer.PREFS, MODE_PRIVATE);
            long delayMs = config.getLong(ConfigureReferrer.DELAY_MS, DELAY_MS);
            Log.i(TAG, "getInstallReferrer from " + caller + ", answering in " + delayMs + " ms");
            SystemClock.sleep(delayMs);

            Bundle result = new Bundle();
            result.putString("install_referrer", config.getString(ConfigureReferrer.REFERRER, REFERRER));
            result.putLong("referrer_click_timestamp_seconds", 1790000000L);
            result.putLong("install_begin_timestamp_seconds", 1790000060L);
            result.putBoolean("google_play_instant", false);
            result.putLong("referrer_click_timestamp_server_seconds", 1790000001L);
            result.putLong("install_begin_timestamp_server_seconds", 1790000061L);
            result.putString("install_version", "test");

            reply.writeNoException();
            reply.writeInt(1);
            result.writeToParcel(reply, 0);
            Log.i(TAG, "getInstallReferrer answered " + caller);
            return true;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
