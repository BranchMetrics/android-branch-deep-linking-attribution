package io.branch.referral;

import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.DisplayMetrics;

public class BranchRemoteInterface extends RemoteInterface {
	
	/**
	 * A static Integer that is used to identify a tag that indicates a 
	 * <b>register/install</b> action.
	 */
	public static final String REQ_TAG_REGISTER_INSTALL = "t_register_install";
	
	/**
	 * A static Integer that is used to identify a tag that indicates a 
	 * <b>register/app opened</b> action.
	 */
	public static final String REQ_TAG_REGISTER_OPEN = "t_register_open";
	
	/**
	 * A static Integer that is used to identify a tag that indicates a 
	 * <b>register/app closed</b> action.
	 */
	public static final String REQ_TAG_REGISTER_CLOSE = "t_register_close";
	
	/**
	 * A static Integer that is used to identify a tag that indicates a 
	 * <b>complete</b> action.
	 */
	public static final String REQ_TAG_COMPLETE_ACTION = "t_complete_action";
	
	/**
	 * A static Integer that is used to identify a tag that indicates a 
	 * <b>register/install</b> action.
	 */
	public static final String REQ_TAG_GET_REFERRAL_COUNTS = "t_get_referral_counts";
	
	/**
	 * A static Integer that is used to identify a tag that indicates a 
	 * <b>get rewards</b> action.
	 */
	public static final String REQ_TAG_GET_REWARDS = "t_get_rewards";
	
	/**
	 * A static Integer that is used to identify a tag that indicates a 
	 * <b>redeem rewards</b> action.
	 */
	public static final String REQ_TAG_REDEEM_REWARDS = "t_redeem_rewards";
	
	/**
	 * A static Integer that is used to identify a tag that indicates a 
	 * <b>get rewards history</b> action.
	 */
	public static final String REQ_TAG_GET_REWARD_HISTORY = "t_get_reward_history";
	
	/**
	 * A static Integer that is used to identify a tag that indicates a 
	 * <b>get custom URL</b> action.
	 */
	public static final String REQ_TAG_GET_CUSTOM_URL = "t_get_custom_url";
	
	/**
	 * A static Integer that is used to identify a tag that indicates a 
	 * <b>identify user</b> action.
	 */
	public static final String REQ_TAG_IDENTIFY = "t_identify_user";
	
	/**
	 * A static Integer that is used to identify a tag that indicates a 
	 * <b>logout</b> action.
	 */
	public static final String REQ_TAG_LOGOUT = "t_logout";
	
	/**
	 * A static Integer that is used to identify a tag that indicates a 
	 * <b>get referral code</b> action.
	 */
	public static final String REQ_TAG_GET_REFERRAL_CODE = "t_get_referral_code";
	
	/**
	 * <p>A static Integer that is used to identify a tag that indicates a 
	 * <b>validate referral code</b> action.</p>
	 */
	public static final String REQ_TAG_VALIDATE_REFERRAL_CODE = "t_validate_referral_code";
	
	/**
	 * <p>A static Integer that is used to identify a tag that indicates a 
	 * <b>apply referral code</b> action.</p>
	 */
	public static final String REQ_TAG_APPLY_REFERRAL_CODE = "t_apply_referral_code";
	
	/**
	 * <p>A static Integer that is used to identify a tag that indicates a 
	 * <b>send app list to server</b> action.</p>
	 */
	public static final String REQ_TAG_SEND_APP_LIST = "t_send_app_list";

	/**
	 * <p>A {@link SystemObserver} object that is used throughout the class to report on the current 
	 * system (in the case of this SDK, an Android Device - phone, phablet, tablet, wearable?) 
	 * state and changeable attributes.</p> 
	 * 
	 * @see SystemObserver
	 */
	private SystemObserver sysObserver_;

	/**
	 * <p>A class-level {@link NetworkCallback} instance.</p>
	 * 
	 * @see NetworkCallback
	 */
	private NetworkCallback callback_;
	
	/**
	 * <p>Required, but empty constructor method.</p>
	 * 
	 * <p>Use {@link #BranchRemoteInterface(Context)} instead, as it instantiates the class 
	 * {@link PrefHelper} and {@link SystemObserver} handles for the class.</p>
	 */
	public BranchRemoteInterface() {}
	
	/**
	 * <p>The main constructor of the BranchRemoteInterface class.</p>
	 * 
	 * @param context		A {@link Context} from which this call was made.
	 */
	public BranchRemoteInterface(Context context) {
		super(context);
		sysObserver_ = new SystemObserver(context);
	}
	
	/**
	 * <p>Sets a callback listener to handle network events received during this app session.</p>
	 * 
	 * @param callback		A {@link NetworkCallback} object instance that will be triggered for 
	 * 						each network event that occurs during this app session.
	 */
	public void setNetworkCallbackListener(NetworkCallback callback) {
		callback_ = callback;
	}
	
	/**
	 * <p>Register an install event with the Branch API.</p>
	 * 
	 * @param installID		A {@link String} value containing the identifier used to denote this 
	 * 						install instance; if none exists, uses the link ID used to open the app.
	 * 
	 * @param debug			A {@link Boolean} value that determines whether or not to open the 
	 * 						connection in debug mode or not. If <i>true</i>, the operations carried 
	 * 						out for this session will not be counted in analytics.
	 */
	public void registerInstall(String installID, boolean debug) {
		String urlExtend = "v1/install";
		if (callback_ != null) {
			JSONObject installPost = new JSONObject();
			try {
				if (!installID.equals(PrefHelper.NO_STRING_VALUE))
					installPost.put("link_click_id", installID);
				String uniqId = sysObserver_.getUniqueID(prefHelper_.getExternDebug());
				if (!uniqId.equals(SystemObserver.BLANK)) {
					installPost.put("hardware_id", uniqId);
					installPost.put("is_hardware_id_real", sysObserver_.hasRealHardwareId());
				}
				if (!sysObserver_.getAppVersion().equals(SystemObserver.BLANK))
					installPost.put("app_version", sysObserver_.getAppVersion());
				if (!sysObserver_.getCarrier().equals(SystemObserver.BLANK))
					installPost.put("carrier", sysObserver_.getCarrier());
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
					installPost.put("bluetooth", sysObserver_.getBluetoothPresent());
				}
				if (!sysObserver_.getBluetoothVersion().equals(SystemObserver.BLANK))
					installPost.put("bluetooth_version", sysObserver_.getBluetoothVersion());
				installPost.put("has_nfc", sysObserver_.getNFCPresent());
				installPost.put("has_telephone", sysObserver_.getTelephonePresent());
				if (!sysObserver_.getPhoneBrand().equals(SystemObserver.BLANK))
					installPost.put("brand", sysObserver_.getPhoneBrand());
				if (!sysObserver_.getPhoneModel().equals(SystemObserver.BLANK))
					installPost.put("model", sysObserver_.getPhoneModel());
				if (!sysObserver_.getOS().equals(SystemObserver.BLANK))
					installPost.put("os", sysObserver_.getOS());
				String uriScheme = sysObserver_.getURIScheme();
				if (!uriScheme.equals(SystemObserver.BLANK)) 
					installPost.put("uri_scheme", uriScheme);
				installPost.put("os_version", sysObserver_.getOSVersion());
				DisplayMetrics dMetrics = sysObserver_.getScreenDisplay();
				installPost.put("screen_dpi", dMetrics.densityDpi);
				installPost.put("screen_height", dMetrics.heightPixels);
				installPost.put("screen_width", dMetrics.widthPixels);
				installPost.put("wifi", sysObserver_.getWifiConnected());
				installPost.put("is_referrable", prefHelper_.getIsReferrable());
				installPost.put("update", sysObserver_.getUpdateState(true));
				if (!prefHelper_.getLinkClickIdentifier().equals(PrefHelper.NO_STRING_VALUE)) {
					installPost.put("link_identifier", prefHelper_.getLinkClickIdentifier());
				}
				String advertisingId = sysObserver_.getAdvertisingId();
				if (advertisingId != null) {
					installPost.put("google_advertising_id", advertisingId);
				}
				installPost.put("debug", debug);
			} catch (JSONException ex) {
				ex.printStackTrace();
			}
			callback_.finished(make_restful_post(installPost, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_REGISTER_INSTALL, prefHelper_.getTimeout()));
		}
	}
	
	/**
	 * <p>Register an open application event with the Branch API.</p>
	 * 
	 * @param debug 	A {@link Boolean} value that determines whether or not to open the connection 
	 * 					in debug mode or not. If <i>true</i>, the operations carried out for this 
	 * 					session will not be counted in analytics.
	 */
	public void registerOpen(boolean debug) {
		String urlExtend = "v1/open";
		if (callback_ != null) {
			JSONObject openPost = new JSONObject();
			try {
				openPost.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
				openPost.put("identity_id", prefHelper_.getIdentityID());
				openPost.put("is_referrable", prefHelper_.getIsReferrable());
				if (!sysObserver_.getAppVersion().equals(SystemObserver.BLANK))
					openPost.put("app_version", sysObserver_.getAppVersion());
				openPost.put("os_version", sysObserver_.getOSVersion());
				openPost.put("update", sysObserver_.getUpdateState(true));
				String uriScheme = sysObserver_.getURIScheme();
				if (!uriScheme.equals(SystemObserver.BLANK)) 
					openPost.put("uri_scheme", uriScheme);
				if (!sysObserver_.getOS().equals(SystemObserver.BLANK))
					openPost.put("os", sysObserver_.getOS());
				if (!prefHelper_.getLinkClickIdentifier().equals(PrefHelper.NO_STRING_VALUE)) {
					openPost.put("link_identifier", prefHelper_.getLinkClickIdentifier());
				}
				String advertisingId = sysObserver_.getAdvertisingId();
				if (advertisingId != null) {
					openPost.put("google_advertising_id", advertisingId);
				}
				openPost.put("debug", debug);
			} catch (JSONException ex) {
				ex.printStackTrace();
			}
			callback_.finished(make_restful_post(openPost, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_REGISTER_OPEN, prefHelper_.getTimeout()));
		}
	}
	
	/**
	 * <p>Calls the command to close the connection on the Branch API.</p>
	 */
	public void registerClose() {
		String urlExtend = "v1/close";
		if (callback_ != null) {
			JSONObject closePost = new JSONObject();
			try {
				closePost.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
				closePost.put("identity_id", prefHelper_.getIdentityID());
				closePost.put("session_id", prefHelper_.getSessionID());
				if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
					closePost.put("link_click_id", prefHelper_.getLinkClickID());
				}
			} catch (JSONException ex) {
				ex.printStackTrace();
			}
			callback_.finished(make_restful_post(closePost, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_REGISTER_CLOSE, prefHelper_.getTimeout()));
		}
	}
	
	/**
	 * <p>Registers a list of apps currently installed on the device.</p>
	 * 
	 * @param post		A {@link JSONObject} containing post data key-value-pairs.
	 */
	public void registerListOfApps(JSONObject post) {
		String urlExtend = "v1/applist";
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_SEND_APP_LIST, prefHelper_.getTimeout()));
		}
	}
	
	/**
	 * <p>A void call to indicate that the user has performed a specific action and for that to be 
	 * reported to the Branch API, with additional app-defined meta data to go along with that action.</p>
	 * 
	 * @param post		A {@link JSONObject} containing post data key-value-pairs.
	 */
	public void userCompletedAction(JSONObject post) {
		String urlExtend = "v1/event";
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_COMPLETE_ACTION, prefHelper_.getTimeout()));
		}
	}
	
	/**
	 * <p>Get the rewards for the App ID specified in the post {@link JSONObject}.</p>
	 * 
	 * @param post		A {@link JSONObject} containing post data key-value-pairs.
	 */
	public void redeemRewards(JSONObject post) {
		String urlExtend = "v1/redeem";
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_REDEEM_REWARDS, prefHelper_.getTimeout()));
		}
	}
	
	/**
	 * <p>Get the rewards for the App ID specified in the current {@link PrefHelper} object.</p>
	 */
	public void getRewards() {
		String urlExtend = "v1/credits/" + prefHelper_.getIdentityID();
		if (callback_ != null) {
			callback_.finished(make_restful_get(prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_GET_REWARDS, prefHelper_.getTimeout()));
		}
	}
	
	/**
	 * <p>Get the number of referrals for the App ID specified in the current {@link PrefHelper} 
	 * object.</p>
	 */
	public void getReferralCounts() {
		String urlExtend = "v1/referrals/" + prefHelper_.getIdentityID();
		if (callback_ != null) {
			callback_.finished(make_restful_get(prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_GET_REFERRAL_COUNTS, prefHelper_.getTimeout()));
		}
	}
	
	/**
	 * <p>Gets the credit history of the bucket specified in the post request.</p>
	 * 
	 * @param post	- A {@link JSONObject} containing post data key-value-pairs.
	 */
	public void getCreditHistory(JSONObject post) {
        String urlExtend = "v1/credithistory";
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_GET_REWARD_HISTORY, prefHelper_.getTimeout()));
		}
	}
	
	/**
	 * <p>Create custom URL, for use elsewhere within the app, without returning the value of the 
	 * created link itself.</p>
	 * 
	 * @param post		A {@link JSONObject} containing post data key-value-pairs.
	 */
	public void createCustomUrl(JSONObject post) {
		String urlExtend = "v1/url";
		if (callback_ != null) {
			BranchLinkData linkData = null;
			if (post instanceof BranchLinkData) {
				linkData = (BranchLinkData)post;
			}
			
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_GET_CUSTOM_URL, prefHelper_.getTimeout(), linkData));
		}
	}
	
	/**
	 * <p>Create custom URL, and return the server response for use elsewhere within the app.</p>
	 * 
	 * @param post		A {@link JSONObject} containing post data key-value-pairs.
	 * 
	 * @return			A {@link ServerResponse} object containing the Branch API response to the 
	 * 					request.
	 */
	public ServerResponse createCustomUrlSync(JSONObject post) {
		String urlExtend = "v1/url";
		BranchLinkData linkData = null;
		if (post instanceof BranchLinkData) {
			linkData = (BranchLinkData)post;
		}
		
		return make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_GET_CUSTOM_URL, prefHelper_.getTimeout(), linkData);
	}
	
	/**
	 * <p>Identify the user logged in to the current session.</p>
	 * 
	 * @param post		A {@link JSONObject} containing post data key-value-pairs.
	 */
	public void identifyUser(JSONObject post) {
		String urlExtend = "v1/profile";
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_IDENTIFY, prefHelper_.getTimeout()));
		}
	}
	
	/**
	 * <p>Logs out the user from the current session.<p>
	 * 
	 * @param post		A {@link JSONObject} containing post data key-value-pairs.
	 */
	public void logoutUser(JSONObject post) {
		String urlExtend = "v1/logout";
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_LOGOUT, prefHelper_.getTimeout()));
		}
	}
	
	/**
	 * <p>Get the applied referral code, and store in currently instantiated {@link PrefHelper} object 
	 * if one exists.</p>
	 * 
	 * @param post		A {@link JSONObject} containing post data key-value-pairs.
	 */
	public void getReferralCode(JSONObject post) {
		String urlExtend = "v1/referralcode";
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_GET_REFERRAL_CODE, prefHelper_.getTimeout()));
		}
	}
	
	/**
	 * <p>Submits a referral code to be validated by the Branch API.</p>
	 * 
	 * @param post		A {@link JSONObject} containing post data key-value-pairs, including one 
	 * 					called "referral_code", that is required for a successful code validation 
	 * 					attempt.
	 */
	public void validateReferralCode(JSONObject post) {
		String urlExtend;
		try {
			urlExtend = "v1/referralcode/" + post.getString("referral_code");
			if (callback_ != null) {
				callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_VALIDATE_REFERRAL_CODE, prefHelper_.getTimeout()));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * <p>Submits a referral code to be applied to the current Branch session.</p>
	 * 
	 * @param post		A {@link JSONObject} containing post data key-value-pairs, including one 
	 * 					called "referral_code", that is required for a successful code validation 
	 * 					attempt.
	 */
	public void applyReferralCode(JSONObject post) {
		String urlExtend;
		try {
			urlExtend = "v1/applycode/" + post.getString("referral_code");
			if (callback_ != null) {
				callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_APPLY_REFERRAL_CODE, prefHelper_.getTimeout()));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * <p>Connect to server debug endpoint.</p>
	 */
	public void connectToDebug() {
		try {
			String urlExtend = "v1/debug/connect";
			JSONObject post = new JSONObject();
			post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
			if (sysObserver_.getBluetoothPresent()) {
				post.put("device_name", BluetoothAdapter.getDefaultAdapter().getName());
			} else {
				post.put("device_name", sysObserver_.getPhoneModel());
			}
			post.put("os", sysObserver_.getOS());
		    post.put("os_version", sysObserver_.getOSVersion());
		    post.put("model", sysObserver_.getPhoneModel());
		    post.put("is_simulator", sysObserver_.isSimulator());
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, PrefHelper.REQ_TAG_DEBUG_CONNECT, prefHelper_.getTimeout(), false));
		} catch (JSONException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * <p>Disconnect from the server debug interface.</p>
	 */
	public void disconnectFromDebug() {
		try {
			String urlExtend = "v1/debug/disconnect";
			JSONObject post = new JSONObject();
			post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, PrefHelper.REQ_TAG_DEBUG_DISCONNECT, prefHelper_.getTimeout(), false));
		} catch (JSONException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * <p>Log messages to the server's debug interface.</p>
	 * 
	 * @param log		A {@link String} variable containing information to log.
	 */
	public void sendLog(String log) {
		try {
			String urlExtend = "v1/debug/log";
			JSONObject post = new JSONObject();
			post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
			post.put("log", log);
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, PrefHelper.REQ_TAG_DEBUG_LOG, prefHelper_.getTimeout(), false));
		} catch (JSONException ex) {
			ex.printStackTrace();
		}
	}
}
