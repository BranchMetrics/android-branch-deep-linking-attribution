package io.branch.referral;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class PrefHelper {
	public static final boolean LOG = false;
	
	public static final String NO_STRING_VALUE = "bnc_no_value";
	
	private static final String SHARED_PREF_FILE = "branch_referral_shared_pref";

	private static final String KEY_APP_KEY = "bnc_app_key";
	
	private static final String KEY_DEVICE_FINGERPRINT_ID = "bnc_device_fingerprint_id";
	private static final String KEY_SESSION_ID = "bnc_session_id";
	private static final String KEY_IDENTITY_ID = "bnc_identity_id";
	private static final String KEY_IDENTITY = "bnc_identity";
	private static final String KEY_LINK_CLICK_ID = "bnc_link_click_id";
	private static final String KEY_SESSION_PARAMS = "bnc_session_params";
	private static final String KEY_INSTALL_PARAMS = "bnc_install_params";
	private static final String KEY_USER_URL = "bnc_user_url";
	private static final String KEY_IS_REFERRABLE = "bnc_is_referrable";

	private static final String KEY_BUCKETS = "bnc_buckets";
	private static final String KEY_CREDIT_BASE = "bnc_credit_base_";
	
	private static final String KEY_ACTIONS = "bnc_actions";
	private static final String KEY_TOTAL_BASE = "bnc_total_base_";
	private static final String KEY_UNIQUE_BASE = "bnc_balance_base_";
	

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
	
	public void setIdentityID(String identity_id) {
		setString(KEY_IDENTITY_ID, identity_id);
	}
	
	public String getIdentityID() {
		return getString(KEY_IDENTITY_ID);
	}
	
	public void setIdentity(String identity) {
		setString(KEY_IDENTITY, identity);
	}
	
	public String getIdentity() {
		return getString(KEY_IDENTITY);
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
	
	public int getIsReferrable() {
		return getInteger(KEY_IS_REFERRABLE);
	}
	
	public void setIsReferrable() {
		setInteger(KEY_IS_REFERRABLE, 1);
	}
	
	public void clearIsReferrable() {
		setInteger(KEY_IS_REFERRABLE, 0);
	}
	
	public void clearUserValues() {
		ArrayList<String> buckets = getBuckets();
		for (String bucket : buckets) {
			setCreditCount(bucket, 0);
		}
		setBuckets(new ArrayList<String>());
		
		ArrayList<String> actions = getActions();
		for (String action : actions) {
			setActionTotalCount(action, 0);
			setActionUniqueCount(action, 0);
		}
		setActions(new ArrayList<String>());
	}
	
	// REWARD TRACKING CALLS
	
	private ArrayList<String> getBuckets() {
		String bucketList = getString(KEY_BUCKETS);
		if (bucketList.equals(NO_STRING_VALUE)) {
			return new ArrayList<String>();
		} else {
			return deserializeString(bucketList);
		}
	}
	
	private void setBuckets(ArrayList<String> buckets) {
		if (buckets.size() == 0) {
			setString(KEY_BUCKETS, NO_STRING_VALUE);
		} else {
			setString(KEY_BUCKETS, serializeArrayList(buckets));
		}
	}
	
	public void setCreditCount(int count) {
		setCreditCount("default", count);
	}
	
	public void setCreditCount(String bucket, int count) {
		ArrayList<String> buckets = getBuckets();
		if (!buckets.contains(bucket)) {
			buckets.add(bucket);
			setBuckets(buckets);
		}
		setInteger(KEY_CREDIT_BASE + bucket, count);
	}
	
	public int getCreditCount() {
		return getCreditCount("default");
	}
	
	public int getCreditCount(String bucket) {
		return getInteger(KEY_CREDIT_BASE + bucket);
	}
	
	// EVENT REFERRAL INSTALL CALLS
	
	private ArrayList<String> getActions() {
		String actionList = getString(KEY_ACTIONS);
		if (actionList.equals(NO_STRING_VALUE)) {
			return new ArrayList<String>();
		} else {
			return deserializeString(actionList);
		}
	}
	
	private void setActions(ArrayList<String> actions) {
		if (actions.size() == 0) {
			setString(KEY_ACTIONS, NO_STRING_VALUE);
		} else {
			setString(KEY_ACTIONS, serializeArrayList(actions));
		}
	}
	
	public void setActionTotalCount(String action, int count) {
		ArrayList<String> actions = getActions();
		if (!actions.contains(action)) {
			actions.add(action);
			setActions(actions);
		}
		setInteger(KEY_TOTAL_BASE + action, count);
	}
	
	public void setActionUniqueCount(String action, int count) {
		setInteger(KEY_UNIQUE_BASE + action, count);
	}
	
	public int getActionTotalCount(String action) {
		return getInteger(KEY_TOTAL_BASE + action);
	}
	
	public int getActionUniqueCount(String action) {
		return getInteger(KEY_UNIQUE_BASE + action);
	}
	
	// ALL GENERIC CALLS
	
	private String serializeArrayList(ArrayList<String> strings) {
		String retString = "";
		for (String value : strings) {
			retString = retString + value + ",";
		}
		retString = retString.substring(0, retString.length()-1);
		return retString;
	}
	
	private ArrayList<String> deserializeString(String list) {
		ArrayList<String> strings = new ArrayList<String>();
		String[] stringArr = list.split(",");
		for (int i = 0; i < stringArr.length; i++) {
			strings.add(stringArr[i]);
		}
		return strings;
	}
	
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
