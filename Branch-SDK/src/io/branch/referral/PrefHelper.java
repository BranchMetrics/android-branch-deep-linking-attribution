package io.branch.referral;

import java.util.ArrayList;

import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class PrefHelper {
	private static boolean BNC_Dev_Debug = false;
	private static boolean BNC_Debug = false;
	private static boolean BNC_Debug_Connecting = false;
	private static boolean BNC_Remote_Debug = false;
	
	public static final String NO_STRING_VALUE = "bnc_no_value";
	
	private static final int INTERVAL_RETRY = 3000;
	private static final int MAX_RETRIES = 5;
	private static final int TIMEOUT = 3000;
	
	private static final String SHARED_PREF_FILE = "branch_referral_shared_pref";

	private static final String KEY_APP_KEY = "bnc_app_key";
	
	private static final String KEY_DEVICE_FINGERPRINT_ID = "bnc_device_fingerprint_id";
	private static final String KEY_SESSION_ID = "bnc_session_id";
	private static final String KEY_IDENTITY_ID = "bnc_identity_id";
	private static final String KEY_IDENTITY = "bnc_identity";
	private static final String KEY_LINK_CLICK_ID = "bnc_link_click_id";	
	private static final String KEY_LINK_CLICK_IDENTIFIER = "bnc_link_click_identifier";
	private static final String KEY_SESSION_PARAMS = "bnc_session_params";
	private static final String KEY_INSTALL_PARAMS = "bnc_install_params";
	private static final String KEY_USER_URL = "bnc_user_url";
	private static final String KEY_IS_REFERRABLE = "bnc_is_referrable";

	private static final String KEY_BUCKETS = "bnc_buckets";
	private static final String KEY_CREDIT_BASE = "bnc_credit_base_";
	
	private static final String KEY_ACTIONS = "bnc_actions";
	private static final String KEY_TOTAL_BASE = "bnc_total_base_";
	private static final String KEY_UNIQUE_BASE = "bnc_balance_base_";
	
	private static final String KEY_RETRY_COUNT = "bnc_retry_count";
	private static final String KEY_RETRY_INTERVAL = "bnc_retry_interval";
	private static final String KEY_TIMEOUT = "bnc_timeout";
	
	public static final String REQ_TAG_DEBUG_CONNECT = "t_debug_connect";
	public static final String REQ_TAG_DEBUG_LOG = "t_debug_log";
	public static final String REQ_TAG_DEBUG_SCREEN = "t_debug_screen";
	public static final String REQ_TAG_DEBUG_DISCONNECT = "t_debug_disconnect";
	public static final int DEBUG_TRIGGER_NUM_FINGERS = 4;
	public static final int DEBUG_TRIGGER_PRESS_TIME = 3000;

	private static PrefHelper prefHelper_;
	private SharedPreferences appSharedPrefs_;
	private Editor prefsEditor_;	
	
	private BranchRemoteInterface remoteInterface_;
	private Context context_;
	
	public PrefHelper() {}
	
	private PrefHelper(Context context) {
		this.appSharedPrefs_ = context.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE);
		this.prefsEditor_ = this.appSharedPrefs_.edit();
		this.context_ = context;
	}
	
	public static PrefHelper getInstance(Context context) {
		if (prefHelper_ == null) {
			prefHelper_ = new PrefHelper(context);
		}
		return prefHelper_;
	}
	
	public String getAPIBaseUrl() {
		return "https://api.branch.io/";
	}
	
	public void setTimeout(int timeout) {
		setInteger(KEY_TIMEOUT, timeout);
	}
	
	public int getTimeout() {
		return getInteger(KEY_TIMEOUT, TIMEOUT);
	}

	public void setRetryCount(int retry) {
		setInteger(KEY_RETRY_COUNT, retry);
	}
	
	public int getRetryCount() {
		return getInteger(KEY_RETRY_COUNT, MAX_RETRIES);
	}
	
	public void setRetryInterval(int retryInt) {
		setInteger(KEY_RETRY_INTERVAL, retryInt);
	}
	
	public int getRetryInterval() {
		return getInteger(KEY_RETRY_INTERVAL, INTERVAL_RETRY);
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
	
	public void setLinkClickIdentifier(String identifer) {
		setString(KEY_LINK_CLICK_IDENTIFIER, identifer);
	}
	
	public String getLinkClickIdentifier() {
		return getString(KEY_LINK_CLICK_IDENTIFIER);
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
		return getInteger(key, 0);
	}
	public int getInteger(String key, int defaultValue) {
		return prefHelper_.appSharedPrefs_.getInt(key, defaultValue);
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
	
	public void setExternDebug() {
		BNC_Dev_Debug = true;
	}
	
	public void setDebug() {
		BNC_Debug = true;
		BNC_Debug_Connecting = true;
		
		if (!BNC_Remote_Debug) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					if (remoteInterface_ == null) {
						remoteInterface_ = new BranchRemoteInterface(context_);
						remoteInterface_.setNetworkCallbackListener(new DebugNetworkCallback());
					}
					remoteInterface_.connectToDebug();
				}
			}).start();
		}
	}
	
	public void clearDebug() {
	    BNC_Debug = false;
	    BNC_Debug_Connecting = false;
	    
	    if (BNC_Remote_Debug) {
	        BNC_Remote_Debug = false;
	        
	        if (remoteInterface_ != null) {
		        new Thread(new Runnable() {
					@Override
					public void run() {
						remoteInterface_.disconnectFromDebug();
					}
				}).start();
	        }
	    }
	}

	public boolean isDebug() {
	    return BNC_Debug;
	}
	
	public void log(final String tag, final String message) {
	    if (BNC_Debug || BNC_Dev_Debug) {
	    	Log.i(tag, message);
	    	
	        if (BNC_Remote_Debug && remoteInterface_ != null) {
	        	new Thread(new Runnable() {
					@Override
					public void run() {
						remoteInterface_.sendLog(tag + "\t" + message);
					}
				}).start();
	        }
	    }
	}
	
	public static void Debug(String tag, String message) {
		if (prefHelper_ != null) {
			prefHelper_.log(tag, message);
		} else {
			if (BNC_Debug || BNC_Dev_Debug) {
				Log.i(tag, message);
			}
		}
	}
	
	public boolean keepDebugConnection() {
		if (BNC_Remote_Debug && remoteInterface_ != null) {
        	new Thread(new Runnable() {
				@Override
				public void run() {
					remoteInterface_.sendLog("");
				}
			}).start();
        	return true;
        }
		if (BNC_Debug_Connecting) {
			return true;
		} else {
			return false;	
		}
	}
	
	public class DebugNetworkCallback implements NetworkCallback {
		@Override
		public void finished(ServerResponse serverResponse) {
			if (serverResponse != null) {
				try {
					int status = serverResponse.getStatusCode();
					String requestTag = serverResponse.getTag();

					if (status == 465) {
						BNC_Remote_Debug = false;
			            Log.i("Branch Debug", "======= Server is not listening =======");
					} else if (status >= 400 && status < 500) {
						if (serverResponse.getObject() != null && serverResponse.getObject().has("error") && serverResponse.getObject().getJSONObject("error").has("message")) {
							Log.i("BranchSDK", "Branch API Error: " + serverResponse.getObject().getJSONObject("error").getString("message"));
						}
					} else if (status != 200) {
						if (status == RemoteInterface.NO_CONNECTIVITY_STATUS) {
							Log.i("BranchSDK", "Branch API Error: poor network connectivity. Please try again later.");
						} else {
							Log.i("BranchSDK", "Trouble reaching server. Please try again in a few minutes.");
						}
					} else if (requestTag.equals(REQ_TAG_DEBUG_CONNECT)) {
						BNC_Remote_Debug = true;
			            Log.i("Branch Debug", "======= Connected to Branch Remote Debugger =======");
					}
					
					BNC_Debug_Connecting = false;
				} catch (JSONException ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
}
