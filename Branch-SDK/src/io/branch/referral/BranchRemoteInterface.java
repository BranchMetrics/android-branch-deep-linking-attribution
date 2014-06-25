package io.branch.referral;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.DisplayMetrics;

public class BranchRemoteInterface extends RemoteInterface {
	public static final String REQ_TAG_REGISTER_INSTALL = "t_register_install";
	public static final String REQ_TAG_REGISTER_OPEN = "t_register_open";
	public static final String REQ_TAG_REGISTER_CLOSE = "t_register_close";
	public static final String REQ_TAG_COMPLETE_ACTION = "t_complete_action";
	public static final String REQ_TAG_GET_REFERRALS = "t_get_referral";
	public static final String REQ_TAG_GET_CUSTOM_URL = "t_get_custom_url";
	public static final String REQ_TAG_IDENTIFY = "t_identify_user";
	public static final String REQ_TAG_LOGOUT = "t_logout";
	public static final String REQ_TAG_CREDIT_REFERRED = "t_credit_referred";

	private SystemObserver sysObserver_;
	private PrefHelper prefHelper_;
	private NetworkCallback callback_;
	
	
	public BranchRemoteInterface() {}
	
	public BranchRemoteInterface(Context context) {
		prefHelper_ = PrefHelper.getInstance(context);
		sysObserver_ = new SystemObserver(context);
	}
	
	public void setNetworkCallbackListener(NetworkCallback callback) {
		callback_ = callback;
	}
	
	public void registerInstall(String installID) {
		String urlExtend = "v1/install";
		if (callback_ != null) {
			JSONObject installPost = new JSONObject();
			try {
				installPost.put("app_id", prefHelper_.getAppKey());
				if (!installID.equals(PrefHelper.NO_STRING_VALUE))
					installPost.put("link_click_id", installID);
				if (!sysObserver_.getUniqueID().equals(SystemObserver.BLANK))
					installPost.put("hardware_id", sysObserver_.getUniqueID());
				if (!sysObserver_.getAppVersion().equals(SystemObserver.BLANK))
					installPost.put("app_version", sysObserver_.getAppVersion());
				if (!sysObserver_.getCarrier().equals(SystemObserver.BLANK))
					installPost.put("carrier", sysObserver_.getCarrier());
				installPost.put("bluetooth", sysObserver_.getBluetoothPresent());
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
				installPost.put("os_version", sysObserver_.getOSVersion());
				DisplayMetrics dMetrics = sysObserver_.getScreenDisplay();
				installPost.put("screen_dpi", dMetrics.densityDpi);
				installPost.put("screen_height", dMetrics.heightPixels);
				installPost.put("screen_width", dMetrics.widthPixels);
				installPost.put("wifi", sysObserver_.getWifiConnected());
				if (sysObserver_.getUpdateState() > 0) {
					installPost.put("update", sysObserver_.getUpdateState());
				}
			} catch (JSONException ex) {
				ex.printStackTrace();
			}
			callback_.finished(make_restful_post(installPost, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_REGISTER_INSTALL));
		}
	}
	
	public void registerOpen() {
		String urlExtend = "v1/open";
		if (callback_ != null) {
			JSONObject openPost = new JSONObject();
			try {
				openPost.put("app_id", prefHelper_.getAppKey());
				openPost.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
				openPost.put("identity_id", prefHelper_.getIdentityID());
				if (!sysObserver_.getAppVersion().equals(SystemObserver.BLANK))
					openPost.put("app_version", sysObserver_.getAppVersion());
				openPost.put("os_version", sysObserver_.getOSVersion());
			} catch (JSONException ex) {
				ex.printStackTrace();
			}
			callback_.finished(make_restful_post(openPost, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_REGISTER_OPEN));
		}
	}
	
	public void registerClose() {
		String urlExtend = "v1/close";
		if (callback_ != null) {
			JSONObject openPost = new JSONObject();
			try {
				openPost.put("app_id", prefHelper_.getAppKey());
				openPost.put("session_id", prefHelper_.getSessionID());
			} catch (JSONException ex) {
				ex.printStackTrace();
			}
			callback_.finished(make_restful_post(openPost, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_REGISTER_CLOSE));
		}
	}
	
	public void userCompletedAction(JSONObject post) {
		String urlExtend = "v1/event";
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_COMPLETE_ACTION));
		}
	}
	
	public void creditUserForReferrals(JSONObject post) {
		String urlExtend = "v1/credit";
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_CREDIT_REFERRED));
		}
	}
	
	public void getReferrals() {
		String urlExtend = "v1/referrals/" + prefHelper_.getIdentityID();
		if (callback_ != null) {
			callback_.finished(make_restful_get(prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_GET_REFERRALS));
		}
	}
	
	public void createCustomUrl(JSONObject post) {
		String urlExtend = "v1/url";
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_GET_CUSTOM_URL));
		}
	}
	
	public void identifyUser(JSONObject post) {
		String urlExtend = "v1/identify";
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_IDENTIFY));
		}
	}
	
	public void logoutUser(JSONObject post) {
		String urlExtend = "v1/logout";
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_LOGOUT));
		}
	}
}
