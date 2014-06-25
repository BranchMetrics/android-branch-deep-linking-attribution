package io.branch.referral;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class PrefHelper {
	public static final boolean LOG = true;
	
	public static final String NO_STRING_VALUE = "bnc_no_value";
	
	private static final String SHARED_PREF_FILE = "branch_referral_shared_pref";

	private static final String KEY_APP_KEY = "bnc_app_key";
	
	private static final String KEY_DEVICE_FINGERPRINT_ID = "bnc_device_fingerprint_id";
	private static final String KEY_SESSION_ID = "bnc_session_id";
	private static final String KEY_IDENTITY_ID = "bnc_identity_id";
	private static final String KEY_LINK_CLICK_ID = "bnc_link_click_id";
	private static final String KEY_SESSION_PARAMS = "bnc_session_params";
	private static final String KEY_INSTALL_PARAMS = "bnc_install_params";
	private static final String KEY_USER_URL = "bnc_user_url";
	
	private static final String KEY_CREDIT_BASE = "bnc_credit_base_";
	private static final String KEY_TOTAL_BASE = "bnc_total_base_";
	private static final String KEY_BALANCE_BASE = "bnc_balance_base_";
	

	private static PrefHelper prefHelper_;
	private SharedPreferences appSharedPrefs_;
	private Editor prefsEditor_;	
	
	public PrefHelper() {}
	
	private PrefHelper(Context context) {
		this.appSharedPrefs_ = context.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE);
		this.prefsEditor_ = this.appSharedPrefs_.edit();
		
	}
	
	public static PrefHelper getInstance(Context context) {
		if (prefHelper_ == null) {
			prefHelper_ = new PrefHelper(context);
		}
		return prefHelper_;
	}
	
	public String getAPIBaseUrl() {
		return "http://api.branchmetrics.io/";
	}

	public void setAppKey(String key) {
		setString(KEY_APP_KEY, key);
	}
	
	public String getAppKey() {
		return getString(KEY_APP_KEY);
	}
	
	public void setDeviceFingerPrintID(String device_fingerprint_id) {
		setString(KEY_DEVICE_FINGERPRINT_ID, device_fingerprint_id);
	}
	
	public String getDeviceFingerPrintID() {
		return getString(KEY_DEVICE_FINGERPRINT_ID);
	}
	
	public void setSessionID(String session_id) {
		setString(KEY_SESSION_ID, session_id);
	}
	
	public String getSessionID() {
		return getString(KEY_SESSION_ID);
	}
	
	public void setIdentityID(String device_fingerprint_id) {
		setString(KEY_IDENTITY_ID, device_fingerprint_id);
	}
	
	public String getIdentityID() {
		return getString(KEY_IDENTITY_ID);
	}
	
	public void setLinkClickID(String link_click_id) {
		setString(KEY_LINK_CLICK_ID, link_click_id);
	}
	
	public String getLinkClickID() {
		return getString(KEY_LINK_CLICK_ID);
	}
	
	public String getSessionParams() {
		return getString(KEY_SESSION_PARAMS);
	}
	
	public void setSessionParams(String params) {
		setString(KEY_SESSION_PARAMS, params);
	}
	
	public String getInstallParams() {
		return getString(KEY_INSTALL_PARAMS);
	}
	
	public void setInstallParams(String params) {
		setString(KEY_INSTALL_PARAMS, params);
	}
	
	public void setUserURL(String user_url) {
		setString(KEY_USER_URL, user_url);
	}
	
	public String getUserURL() {
		return getString(KEY_USER_URL);
	}
	
	// EVENT REFERRAL INSTALL CALLS
	
	public void setActionTotalCount(String action, int count) {
		setInteger(KEY_TOTAL_BASE + action, count);
	}
	
	public void setActionBalanceCount(String action, int count) {
		setInteger(KEY_BALANCE_BASE + action, count);
	}
	
	public void setActionCreditCount(String action, int count) {
		setInteger(KEY_CREDIT_BASE + action, count);
	}
	
	public int getActionTotalCount(String action) {
		return getInteger(KEY_TOTAL_BASE + action);
	}
	
	public int getActionBalanceCount(String action) {
		return getInteger(KEY_BALANCE_BASE + action);
	}
	
	public int getActionCreditCount(String action) {
		return getInteger(KEY_CREDIT_BASE + action);
	}
	
	// ALL GENERIC CALLS
	
	public int getInteger(String key) {
		return prefHelper_.appSharedPrefs_.getInt(key, 0);
	}
	public long getLong(String key) {
		return prefHelper_.appSharedPrefs_.getLong(key, 0);
	}
	public float getFloat(String key) {
		return prefHelper_.appSharedPrefs_.getFloat(key, 0);
	}
	public String getString(String key) {
		return prefHelper_.appSharedPrefs_.getString(key, NO_STRING_VALUE);
	}
	public boolean getBool(String key) {
		return prefHelper_.appSharedPrefs_.getBoolean(key, false);
	}
	
	public void setInteger(String key, int value) {
		prefHelper_.prefsEditor_.putInt(key, value);
		prefHelper_.prefsEditor_.commit();
	}
	public void setLong(String key, long value) {
		prefHelper_.prefsEditor_.putLong(key, value);
		prefHelper_.prefsEditor_.commit();
	}
	public void setFloat(String key, float value) {
		prefHelper_.prefsEditor_.putFloat(key, value);
		prefHelper_.prefsEditor_.commit();
	}
	public void setString(String key, String value) {
		prefHelper_.prefsEditor_.putString(key, value);
		prefHelper_.prefsEditor_.commit();
	}
	public void setBool(String key, Boolean value) {
		prefHelper_.prefsEditor_.putBoolean(key, value);
		prefHelper_.prefsEditor_.commit();
	}
	
	
}
