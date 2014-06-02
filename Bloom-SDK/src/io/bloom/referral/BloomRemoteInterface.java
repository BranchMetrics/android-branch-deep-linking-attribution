package io.bloom.referral;

import org.json.JSONObject;

import android.content.Context;

public class BloomRemoteInterface extends RemoteInterface {
	public static final String REQ_TAG_REGISTER_INSTALL = "t_register_install";
	public static final String REQ_TAG_REGISTER_OPEN = "t_register_open";
	public static final String REQ_TAG_COMPLETE_ACTION = "t_complete_action";
	public static final String REQ_TAG_GET_REFERRALS = "t_get_referral";
	public static final String REQ_TAG_CREDIT_REFERRED = "t_credit_referred";

	private PrefHelper prefHelper_;
	private NetworkCallback callback_;
	
	
	public BloomRemoteInterface() {}
	
	public BloomRemoteInterface(Context context) {
		prefHelper_ = PrefHelper.getInstance(context);
	}
	
	public void setNetworkCallbackListener(NetworkCallback callback) {
		callback_ = callback;
	}
	
	public void registerInstall() {
		String urlExtend = "v1/install";
		if (callback_ != null) {
			callback_.finished(make_restful_post(new JSONObject(), prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_REGISTER_INSTALL, prefHelper_.getAppKey()));
		}
	}
	
	public void registerOpen() {
		String urlExtend = "v1/open/" + prefHelper_.getUserID();
		if (callback_ != null) {
			callback_.finished(make_restful_post(new JSONObject(), prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_REGISTER_OPEN, prefHelper_.getAppKey()));
		}
	}
	
	public void userCompletedAction(JSONObject post) {
		String urlExtend = "v1/action/" + prefHelper_.getUserID();
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_COMPLETE_ACTION, prefHelper_.getAppKey()));
		}
	}
	
	public void creditUserForReferrals(JSONObject post) {
		String urlExtend = "v1/credit/" + prefHelper_.getUserID();
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_CREDIT_REFERRED, prefHelper_.getAppKey()));
		}
	}
	
	public void getReferrals() {
		String urlExtend = "v1/referrals/" + prefHelper_.getUserID();
		if (callback_ != null) {
			callback_.finished(make_restful_get(prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_GET_REFERRALS, prefHelper_.getAppKey()));
		}
	}
}
