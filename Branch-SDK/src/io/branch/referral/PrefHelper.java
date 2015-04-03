package io.branch.referral;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * <p>A class that uses the helper pattern to provide regularly referenced static values and 
 * logging capabilities used in various other parts of the SDK, and that are related to globally set  
 * preference values.</p>
 */
public class PrefHelper {

	/**
	 * {@link Boolean} value that enables/disables Branch developer external debug mode.
	 */
	private static boolean BNC_Dev_Debug = false;
	
	/**
	 * {@link Boolean} value that enables/disables Branch general debug mode.
	 */
	private static boolean BNC_Debug = false;
	
	/**
	 * {@link Boolean} value that indicates whether debugger is in transitional connecting state.
	 */
	private static boolean BNC_Debug_Connecting = false;
	
	/**
	 * {@link Boolean} value that enables/disables remote debugging via the server.
	 */
	private static boolean BNC_Remote_Debug = false;
	
	/**
	 * {@link Boolean} value that TODO
	 */
	private static boolean BNC_App_Listing = true;

	/**
	 * TODO
	 */
	private static boolean BNC_Smart_Session = true;

	/**
	 * A {@link String} value used where no string value is available.
	 */
	public static final String NO_STRING_VALUE = "bnc_no_value";

	/**
	 * 
	 */
	private static final int INTERVAL_RETRY = 3000;
	
	/**
	 * Number of times to reattempt connection to the Branch server before giving up and throwing an 
	 * exception.
	 */
	private static final int MAX_RETRIES = 5;
	
	/**
	 * Time in milliseconds to wait before TODO
	 */
	private static final int TIMEOUT = 3000;

	private static final String SHARED_PREF_FILE = "branch_referral_shared_pref";

	private static final String KEY_APP_KEY = "bnc_app_key";
	private static final String KEY_APP_VERSION = "bnc_app_version";
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

	private static final String KEY_LAST_READ_SYSTEM = "bnc_system_read_date";

	/**
	 * {@link String} value used by {@link BranchRemoteInterface#connectToDebug()}.
	 */
	public static final String REQ_TAG_DEBUG_CONNECT = "t_debug_connect";
	
	/**
	 * {@link String} value used by {@link BranchRemoteInterface#sendLog(String log)}.
	 */
	public static final String REQ_TAG_DEBUG_LOG = "t_debug_log";
	
	/**
	 * {@link String} value used by {@link BranchRemoteInterface}.
	 * 
	 * @see BranchRemoteInterface
	 */
	public static final String REQ_TAG_DEBUG_SCREEN = "t_debug_screen";
	
	/**
	 * {@link String} value used by {@link BranchRemoteInterface#disconnectFromDebug()}.
	 * 
	 * @see BranchRemoteInterface
	 * @see BranchRemoteInterface#disconnectFromDebug()
	 */
	public static final String REQ_TAG_DEBUG_DISCONNECT = "t_debug_disconnect";
	
	/**
	 * The debug action is triggered by holding a multi-touch gesture. This {@link Integer} value 
	 * defines how many multi-touch points need to be held on the screen in order for the debug 
	 * action to be triggered.
	 */
	public static final int DEBUG_TRIGGER_NUM_FINGERS = 4;
	
	/**
	 * The debug action is triggered by holding a multi-touch gesture. This {@link Integer} value 
	 * defines how many milliseconds the gesture must be held in order for the debug action to be 
	 * triggered.
	 */
	public static final int DEBUG_TRIGGER_PRESS_TIME = 3000;

	/**
	 * Internal static variable of own type {@link PrefHelper}. This variable holds the single 
	 * instance used when the class is instantiated via the Singleton pattern.
	 */
	private static PrefHelper prefHelper_;
	
	/**
	 * A single variable that holds a reference to the application's {@link SharedPreferences} 
	 * object for use whenever {@link SharedPreferences} values are read or written via this helper 
	 * class.
	 */
	private SharedPreferences appSharedPrefs_;
	
	/**
	 * A single variable that holds a reference to an {@link Editor} object that is used by the 
	 * helper class whenever the preferences for the application are changed.
	 */
	private Editor prefsEditor_;

	/**
	 * Instance of {@link BranchRemoteInterface} enabling remote interaction via the server interface.
	 */
	private BranchRemoteInterface remoteInterface_;
	
	/**
	 * Reference of application {@link Context}, normally the base context of the application.
	 */
	private Context context_;

	/**
	 * Empty, but required constructor for the {@link PrefHelper} {@link SharedPreferences} helper 
	 * class.
	 */
	public PrefHelper() {
	}

	/**
	 * Constructor with context passed from calling {@link Activity}.
	 * 
	 * @param context A reference to the {@link Context} that the application is operating within. 
	 * This is normally the base context of the application.
	 */
	private PrefHelper(Context context) {
		this.appSharedPrefs_ = context.getSharedPreferences(SHARED_PREF_FILE,
				Context.MODE_PRIVATE);
		this.prefsEditor_ = this.appSharedPrefs_.edit();
		this.context_ = context;
	}

	/**
	 * 
	 * 
	 * @param context
	 * @return
	 */
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
	
	public void setAppVersion(String version) {
		setString(KEY_APP_VERSION, version);
	}
	
	public String getAppVersion() {
		return getString(KEY_APP_VERSION);
	}

	@Deprecated
	public void setAppKey(String key) {
		setString(KEY_APP_KEY, key);
	}

	public String getAppKey() {
		String appKey = null;
		try {
			final ApplicationInfo ai = context_.getPackageManager()
					.getApplicationInfo(context_.getPackageName(),
							PackageManager.GET_META_DATA);
			if (ai.metaData != null) {
				appKey = ai.metaData.getString("io.branch.sdk.ApplicationId");
			}
		} catch (final PackageManager.NameNotFoundException e) {
		}

		if (appKey == null) {
			appKey = getString(KEY_APP_KEY);
		}

		return appKey;
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

	public void clearSystemReadStatus() {
		Calendar c = Calendar.getInstance();
		setLong(KEY_LAST_READ_SYSTEM, c.getTimeInMillis() / 1000);
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
		retString = retString.substring(0, retString.length() - 1);
		return retString;
	}

	private ArrayList<String> deserializeString(String list) {
		ArrayList<String> strings = new ArrayList<String>();
		String[] stringArr = list.split(",");
		Collections.addAll(strings, stringArr);
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

	public boolean getExternDebug() {
		return BNC_Dev_Debug;
	}

	public void disableExternAppListing() {
		BNC_App_Listing = false;
	}

	public boolean getExternAppListing() {
		return BNC_App_Listing;
	}

	public void disableSmartSession() {
		BNC_Smart_Session = false;
	}

	public boolean getSmartSession() {
		return BNC_Smart_Session;
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
						remoteInterface_
								.setNetworkCallbackListener(new DebugNetworkCallback());
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

	/**
	 * 
	 * @return 
	 */
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

		return BNC_Debug_Connecting;
	}

	/**
	 * Debug connection callback that implements {@link NetworkCallback} to react to server calls 
	 * to debug API end-points.
	 * 
	 * @see {@link NetworkCallback}
	 *
	 */
	public static class DebugNetworkCallback implements NetworkCallback {
		private int connectionStatus;

		/**
		 * 
		 * @return {@link Integer} value containing the HTTP Status code of the current connection.
		 * 
		 * <ul>
		 *     <li>200 - The request has succeeded.</li>
		 *     <li>400 - Request cannot be fulfilled due to bad syntax</li>
		 *     <li>465 - Server is not listening.</li>
		 *     <li>500 - The server encountered an unexpected condition which prevented it from fulfilling the request.</li>
		 * </ul>
		 * 
		 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html">HTTP/1.1 - Status Code Definitions</a>
		 */
		public int getConnectionStatus() {
			return connectionStatus;
		}

		/**
		 * Called when the server response is returned following a request to the debug API.
		 */
		@Override
		public void finished(ServerResponse serverResponse) {
			if (serverResponse != null) {
				try {
					connectionStatus = serverResponse.getStatusCode();
					String requestTag = serverResponse.getTag();

					if (connectionStatus == 465) {
						BNC_Remote_Debug = false;
						Log.i("Branch Debug",
								"======= Server is not listening =======");
					} else if (connectionStatus >= 400
							&& connectionStatus < 500) {
						if (serverResponse.getObject() != null
								&& serverResponse.getObject().has("error")
								&& serverResponse.getObject()
										.getJSONObject("error").has("message")) {
							Log.i("BranchSDK",
									"Branch API Error: "
											+ serverResponse.getObject()
													.getJSONObject("error")
													.getString("message"));
						}
					} else if (connectionStatus != 200) {
						if (connectionStatus == RemoteInterface.NO_CONNECTIVITY_STATUS) {
							Log.i("BranchSDK",
									"Branch API Error: poor network connectivity. Please try again later.");
						} else {
							Log.i("BranchSDK",
									"Trouble reaching server. Please try again in a few minutes.");
						}
					} else if (requestTag.equals(REQ_TAG_DEBUG_CONNECT)) {
						BNC_Remote_Debug = true;
						Log.i("Branch Debug",
								"======= Connected to Branch Remote Debugger =======");
					}

					BNC_Debug_Connecting = false;
				} catch (JSONException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

}
