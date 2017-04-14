package io.branch.bookfinder.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import io.branch.referral.PrefHelper;

/**
 * Created by sojanpr on 8/11/16.
 */
public class BFPreference {
    private static final String SHARED_PREF_FILE = "book_finder_pref";
    private final Context context_;
    private SharedPreferences appSharedPrefs_;
    private SharedPreferences.Editor prefsEditor_;

    private static BFPreference thisInstance_;

    public static BFPreference getInstance(Context context) {
        if (thisInstance_ == null) {
            thisInstance_ = new BFPreference(context);
        }
        return thisInstance_;
    }

    private BFPreference(Context context) {
        context_ = context;
        appSharedPrefs_ = context.getSharedPreferences(SHARED_PREF_FILE,
                Context.MODE_PRIVATE);
        prefsEditor_ = this.appSharedPrefs_.edit();

    }

    /**
     * <p>Sets the value of the {@link String} key value supplied in preferences.</p>
     *
     * @param key   A {@link String} value containing the key to reference.
     * @param value A {@link String} value to set the preference record to.
     */
    private void setString(String key, String value) {
        prefsEditor_.putString(key, value);
        prefsEditor_.apply();
    }

    /**
     * <p>A basic method that returns a {@link String} value from a specified preferences Key.</p>
     *
     * @param key A {@link String} value containing the key to reference.
     * @return A {@link String} value of the specified key as stored in preferences.
     */
    private String getString(String key) {
        return appSharedPrefs_.getString(key, "");
    }

    
    //----------- Pref Values------------------------//

    private static final String LAST_SEARCH_KEY = "last_search_key";

    public void setLastSearchString(String lastSearch) {
        setString(LAST_SEARCH_KEY, lastSearch);
    }

    public String getLastSearchString() {
        String searchStr = getString(LAST_SEARCH_KEY);
        if (TextUtils.isEmpty(searchStr)) {
            searchStr = "Reading Benefits";
        }
        return searchStr;
    }
}
