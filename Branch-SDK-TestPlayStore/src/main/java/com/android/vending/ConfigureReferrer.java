package com.android.vending;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

/**
 * Overrides the test Play Store's referrer and delay until reset:
 * adb shell am broadcast --include-stopped-packages -n com.android.vending/.ConfigureReferrer
 *     --es referrer 'link_click_id=123' --el delay_ms 0   (or --ez reset true)
 */
public class ConfigureReferrer extends BroadcastReceiver {
    public static final String PREFS = "test_play_store";
    public static final String REFERRER = "referrer";
    public static final String DELAY_MS = "delay_ms";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras() == null ? new Bundle() : intent.getExtras();
        SharedPreferences.Editor config = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        if (extras.getBoolean("reset")) {
            config.clear();
        }
        if (extras.containsKey(REFERRER)) {
            config.putString(REFERRER, extras.getString(REFERRER));
        }
        Object delayMs = extras.get(DELAY_MS);
        if (delayMs instanceof Number) {
            config.putLong(DELAY_MS, ((Number) delayMs).longValue());
        }
        config.commit();
        Log.i("TestPlayStore", "configured " + extras);
        setResultCode(Activity.RESULT_OK);
    }
}
