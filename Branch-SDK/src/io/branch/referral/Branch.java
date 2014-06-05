package io.branch.referral;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.util.Log;

public class Branch {
	public static final String ACTION_INSTALL = "install";
	
	private static final int INTERVAL_RETRY = 3000;
	private static final int MAX_RETRIES = 10;

	private static Branch branchReferral_;
	private boolean isInit_;
	
	private BranchReferralInitListener initFinishedCallback_;
	private BranchReferralStateChangedListener stateChangedCallback_;
	private BranchLinkCreateListener linkCreateCallback_;
	
	private BranchRemoteInterface kRemoteInterface_;
	private PrefHelper prefHelper_;
		
	private Semaphore serverSema_;
	private ArrayList<ServerRequest> requestQueue_;
	private int networkCount_;
	private int retryCount_;
	
	private Branch(Context context) {
		prefHelper_ = PrefHelper.getInstance(context);
		kRemoteInterface_ = new BranchRemoteInterface(context);
		kRemoteInterface_.setNetworkCallbackListener(new ReferralNetworkCallback());
		requestQueue_ = new ArrayList<ServerRequest>();
		serverSema_ = new Semaphore(1);
		isInit_ = false;
		networkCount_ = 0;
	}
	
	public static Branch getInstance(Context context, String key) {
		if (branchReferral_ == null) {
			branchReferral_ = new Branch(context.getApplicationContext());
		}
		branchReferral_.prefHelper_.setAppKey(key);
		return branchReferral_;
	}
	
	public void initUserSession(BranchReferralInitListener callback) {
		if (!isInit_) {
			initSession();
			isInit_ = true;
		}
		initFinishedCallback_ = callback;
	}
	
	public void initUserSession() {
		initUserSession(null);
	}
	
	public void loadPoints() {
		loadPoints(null);
	}
	
	public void loadPoints(BranchReferralStateChangedListener callback) {
		stateChangedCallback_ = callback;
		new Thread(new Runnable() {
			@Override
			public void run() {
				requestQueue_.add(new ServerRequest(BranchRemoteInterface.REQ_TAG_GET_REFERRALS, null));
				processNextQueueItem();
			}
		}).start();
	}
	
	public void creditUserForReferral(final String action, final int credit) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				retryCount_ = 0;
				JSONObject post = new JSONObject();
				try {
					post.put("action", action);
					post.put("credit", credit);
				} catch (JSONException ex) {
					ex.printStackTrace();
					return;
				}
				requestQueue_.add(new ServerRequest(BranchRemoteInterface.REQ_TAG_CREDIT_REFERRED, post));
				processNextQueueItem();
			}
		}).start();
	}
	
	public void userCompletedAction(final String action) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				retryCount_ = 0;
				JSONObject post = new JSONObject();
				try {
					post.put("action", action);
				} catch (JSONException ex) {
					ex.printStackTrace();
					return;
				}
				requestQueue_.add(new ServerRequest(BranchRemoteInterface.REQ_TAG_COMPLETE_ACTION, post));
				processNextQueueItem();
			}
		}).start();
	}
	
	public int getTotal(String action) {
		return prefHelper_.getActionTotalCount(action);
	}
	
	public int getCredit(String action) {
		return prefHelper_.getActionCreditCount(action);
	}
	
	public int getBalance(String action) {
		return prefHelper_.getActionBalanceCount(action);
	}
	
	public JSONObject getReferringParams() {
		if (prefHelper_.getSessionParams().equals(PrefHelper.NO_STRING_VALUE)) {
			return new JSONObject();
		} else {
			try {
				return new JSONObject(prefHelper_.getSessionParams());
			} catch (JSONException e) {
				byte[] encodedArray = Base64.decode(prefHelper_.getSessionParams().getBytes(), Base64.NO_WRAP);
				try {
					return new JSONObject(new String(encodedArray));
				} catch (JSONException ex) {
					ex.printStackTrace();
					return new JSONObject();
				}
			}
		}
	}
	
	public String getLongURL() {
		return generateLongLink(null, null);
	}
	
	public String getLongURL(JSONObject params) {
		return generateLongLink(null, params.toString());
	}
	
	public String getLongURL(String tag) {
		return generateLongLink(tag, null);
	}
	
	public String getLongURL(String tag, JSONObject params) {
		return generateLongLink(tag, params.toString());
	}
	
	public void getShortUrl(BranchLinkCreateListener callback) {
		generateShortLink(null, null);
	}
	
	public void getShortUrl(JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(null, params.toString());
	}
	
	public void getShortUrl(String tag, BranchLinkCreateListener callback) {
		generateShortLink(tag, null);
	}
	
	public void getShortUrl(String tag, JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(tag, params.toString());
	}
	
	// PRIVATE FUNCTIONS

	private String generateLongLink(String tag, String params) {
		if (hasUser()) {
			String url = prefHelper_.getUserURL();
			if (tag != null) {
				url = url + "?t=" + tag;
				if (params != null) {
					byte[] encodedArray = Base64.encode(params.getBytes(), Base64.NO_WRAP);
					url = url + "&d=" + new String(encodedArray);
				}
			} else if (params != null) {
				byte[] encodedArray = Base64.encode(params.getBytes(), Base64.NO_WRAP);
				url = url + "?d=" + new String(encodedArray); 
			}
			return url;
		} else {
			return "init imcomplete, did you call init";
		}
	}
	
	private void generateShortLink(String tag, String params) {
		JSONObject linkPost = new JSONObject();
		try {
			linkPost.put("app_id", prefHelper_.getAppKey());
			linkPost.put("device_id", prefHelper_.getDeviceID());
			linkPost.put("user_id", prefHelper_.getUserID());
			if (tag != null)
				linkPost.put("tag", tag);
			if (params != null)
				linkPost.put("data", params);
		} catch (JSONException ex) {
			ex.printStackTrace();
		}
		kRemoteInterface_.createCustomUrl(linkPost);
	}
	
	private void processNextQueueItem() {
		try {
			serverSema_.acquire();
			if (networkCount_ == 0 && requestQueue_.size() > 0) {
				networkCount_ = 1;
				serverSema_.release();
				
				ServerRequest req = requestQueue_.get(0);
				
				if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL)) {
					Log.i("AppidemicSDK", "calling register install");
					kRemoteInterface_.registerInstall(PrefHelper.NO_STRING_VALUE);
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_OPEN)) {
					Log.i("AppidemicSDK", "calling register open");
					kRemoteInterface_.registerOpen();
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REFERRALS) && hasUser()) {
					Log.i("AppidemicSDK", "calling get referrals");
					kRemoteInterface_.getReferrals();
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_CREDIT_REFERRED) && hasUser()) {
					Log.i("AppidemicSDK", "calling credit referrals");
					kRemoteInterface_.creditUserForReferrals(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_COMPLETE_ACTION) && hasUser()){
					Log.i("AppidemicSDK", "calling completed action");
					kRemoteInterface_.userCompletedAction(req.getPost());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void retryLastRequest() {
		retryCount_ = retryCount_ + 1;
		if (retryCount_ > MAX_RETRIES) {
			requestQueue_.remove(0);
			retryCount_ = 0;
		} else {
			try {
				Thread.sleep(INTERVAL_RETRY);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean installInQueue() {
		for (int i = 0; i < requestQueue_.size(); i++) {
			ServerRequest req = requestQueue_.get(i);
			if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL)) {
				return true;
			}
		}
		return false;
	}
	
	private void moveInstallToFront() {
		for (int i = 0; i < requestQueue_.size(); i++) {
			ServerRequest req = requestQueue_.get(i);
			if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL)) {
				requestQueue_.remove(i);
				break;
			}
		}
		requestQueue_.add(0, new ServerRequest(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL, null));
	}
	
	private boolean hasUser() {
		return !prefHelper_.getUserID().equals(PrefHelper.NO_STRING_VALUE);
	}
	
	private void registerInstall() {
		if (!installInQueue()) {
			requestQueue_.add(0, new ServerRequest(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL, null));
		} else {
			moveInstallToFront();
		}
		processNextQueueItem();
	}
	
	private void registerOpen() {
		requestQueue_.add(0, new ServerRequest(BranchRemoteInterface.REQ_TAG_REGISTER_OPEN, null));
		processNextQueueItem();
	}
	
	private void initSession() {
		if (hasUser()) {
			registerOpen();
		} else {
			registerInstall();
		}
	} 
	
	private void processReferralCounts(JSONObject obj) {
		boolean updateListener = false;
		Iterator<?> keys = obj.keys();
		while (keys.hasNext()) {
			String key = (String)keys.next();
			if (key.equals(BranchRemoteInterface.KEY_SERVER_CALL_STATUS_CODE) || key.equals(BranchRemoteInterface.KEY_SERVER_CALL_TAG))
				continue;
			
			try {
				JSONObject counts = obj.getJSONObject(key);
				int total = counts.getInt("total");
				int credits = counts.getInt("credits");
				if (total != prefHelper_.getActionTotalCount(key) || credits != prefHelper_.getActionCreditCount(key)) {
					updateListener = true;
				}
				prefHelper_.setActionTotalCount(key, total);
				prefHelper_.setActionCreditCount(key, credits);
				prefHelper_.setActionBalanceCount(key, Math.max(0, total-credits));
			} catch (JSONException e) {
				e.printStackTrace();
			}			
		}
		if (updateListener) {
			if (stateChangedCallback_ != null) {
				stateChangedCallback_.onStateChanged();
			}
		}
	}
	
	public class ReferralNetworkCallback implements NetworkCallback {
		@Override
		public void finished(JSONObject serverResponse) {
			if (serverResponse != null) {
				try {
					int status = serverResponse.getInt(BranchRemoteInterface.KEY_SERVER_CALL_STATUS_CODE);
					String requestTag = serverResponse.getString(BranchRemoteInterface.KEY_SERVER_CALL_TAG);
					
					networkCount_ = 0;
					if (status != 200) {
						retryLastRequest();
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_GET_REFERRALS)) {
						processReferralCounts(serverResponse);
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL)) {
						String appInstallID = serverResponse.getString("app_install_id");
						prefHelper_.setUserID(serverResponse.getString("user_id"));
						prefHelper_.setDeviceID(serverResponse.getString("device_id"));
						prefHelper_.setAppInstallID(appInstallID);
						prefHelper_.setUserURL(prefHelper_.getShortURL() + "a/" + appInstallID);
						if (serverResponse.has("link_click_id")) {
							prefHelper_.setLinkClickID(serverResponse.getString("link_click_id"));
						} else {
							prefHelper_.setLinkClickID(PrefHelper.NO_STRING_VALUE);
						}	
						if (serverResponse.has("params")) {
							prefHelper_.setSessionParams(serverResponse.getJSONObject("params").toString());
						} else {
							prefHelper_.setSessionParams(PrefHelper.NO_STRING_VALUE);
						}
						if (initFinishedCallback_ != null) {
							initFinishedCallback_.onInitFinished();
						}
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_REGISTER_OPEN)) {
						if (serverResponse.has("link_click_id")) {
							prefHelper_.setLinkClickID(serverResponse.getString("link_click_id"));
						} else {
							prefHelper_.setLinkClickID(PrefHelper.NO_STRING_VALUE);
						}
						if (serverResponse.has("params")) {
							prefHelper_.setSessionParams(serverResponse.getString("params"));
						} else {
							prefHelper_.setSessionParams(PrefHelper.NO_STRING_VALUE);
						}
						if (initFinishedCallback_ != null) {
							initFinishedCallback_.onInitFinished();
						}
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_CREDIT_REFERRED)) {
						ServerRequest req = requestQueue_.get(0);
						String action = req.getPost().getString("action");
						int credits = req.getPost().getInt("credits");
						prefHelper_.setActionCreditCount(action, prefHelper_.getActionCreditCount(action)+credits);
						prefHelper_.setActionBalanceCount(action, Math.max(0, prefHelper_.getActionTotalCount(action)-prefHelper_.getActionCreditCount(action)));
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_GET_CUSTOM_URL)) {
						String url = serverResponse.getString("url");
						if (linkCreateCallback_ != null) {
							linkCreateCallback_.onLinkCreate(url);
						}
					}
					
					requestQueue_.remove(0);

					processNextQueueItem();
				} catch (JSONException ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	public interface BranchReferralInitListener {
		public void onInitFinished();
	}
	
	public interface BranchReferralStateChangedListener {
		public void onStateChanged();
	}
	
	public interface BranchLinkCreateListener {
		public void onLinkCreate(String url);
	}
}
