package io.branch.referral;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class Branch {	
	private static final int INTERVAL_RETRY = 3000;
	private static final int MAX_RETRIES = 5;

	private static Branch branchReferral_;
	private boolean isInit_;
	
	private BranchReferralInitListener initFinishedCallback_;
	private BranchReferralStateChangedListener stateChangedCallback_;
	private BranchLinkCreateListener linkCreateCallback_;
	
	private BranchRemoteInterface kRemoteInterface_;
	private PrefHelper prefHelper_;
		
	private Context context_;
	
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
		context_ = context;
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
		initFinishedCallback_ = callback;
		if (!isInit_) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					initSession();
				}
			}).start();
			isInit_ = true;
		} else {
			if (callback != null) callback.onInitFinished(getReferringParams());
		}
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
				int creditsToAdd = 0;
				int total = prefHelper_.getActionTotalCount(action);
				int prevCredits = prefHelper_.getActionCreditCount(action);
				
				if ((credit + prevCredits) > total) {
					creditsToAdd = total - prevCredits;
				} else {
					creditsToAdd = credit;
				}
				
				
				if (creditsToAdd > 0) {
					retryCount_ = 0;
					JSONObject post = new JSONObject();
					try {
						post.put("app_id", prefHelper_.getAppKey());
						post.put("app_install_id", prefHelper_.getAppInstallID());
						post.put("event", action);
						post.put("credit", creditsToAdd);
					} catch (JSONException ex) {
						ex.printStackTrace();
						return;
					}
					requestQueue_.add(new ServerRequest(BranchRemoteInterface.REQ_TAG_CREDIT_REFERRED, post));
					processNextQueueItem();
				}
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
					post.put("app_id", prefHelper_.getAppKey());
					post.put("app_install_id", prefHelper_.getAppInstallID());
					if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) post.put("link_click_id", prefHelper_.getLinkClickID());
					post.put("event", action);
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
		generateShortLink(null, null, callback);
	}
	
	public void getShortUrl(JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(null, params.toString(), callback);
	}
	
	public void getShortUrl(String tag, BranchLinkCreateListener callback) {
		generateShortLink(tag, null, callback);
	}
	
	public void getShortUrl(String tag, JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(tag, params.toString(), callback);
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
			return "init incomplete, did you call init yet?";
		}
	}
	
	private void generateShortLink(final String tag, final String params, BranchLinkCreateListener callback) {
		linkCreateCallback_ = callback;
		if (hasUser()) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					JSONObject linkPost = new JSONObject();
					try {
						linkPost.put("app_id", prefHelper_.getAppKey());
						linkPost.put("app_install_id", prefHelper_.getAppInstallID());
						if (tag != null)
							linkPost.put("tag", tag);
						if (params != null)
							linkPost.put("data", params);
					} catch (JSONException ex) {
						ex.printStackTrace();
					}
					requestQueue_.add(new ServerRequest(BranchRemoteInterface.REQ_TAG_GET_CUSTOM_URL, linkPost));
					processNextQueueItem();
				}
			}).start();
		}
	}
	
	private void processNextQueueItem() {
		try {
			serverSema_.acquire();
			if (networkCount_ == 0 && requestQueue_.size() > 0) {
				networkCount_ = 1;
				serverSema_.release();
				
				ServerRequest req = requestQueue_.get(0);
				
				if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL)) {
					kRemoteInterface_.registerInstall(PrefHelper.NO_STRING_VALUE);
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_OPEN)) {
					kRemoteInterface_.registerOpen();
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REFERRALS) && hasUser()) {
					kRemoteInterface_.getReferrals();
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_CREDIT_REFERRED) && hasUser()) {
					kRemoteInterface_.creditUserForReferrals(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_COMPLETE_ACTION) && hasUser()){
					kRemoteInterface_.userCompletedAction(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_CUSTOM_URL) && hasUser()) {
					kRemoteInterface_.createCustomUrl(req.getPost());
				} else if (!hasUser()) {
					Log.i("BranchSDK", "Branch Warning: User session has not been initialized");
				}
			} else {
				serverSema_.release();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void retryLastRequest() {
		retryCount_ = retryCount_ + 1;
		if (retryCount_ > MAX_RETRIES) {
			final ServerRequest req = requestQueue_.remove(0);
			Handler mainHandler = new Handler(context_.getMainLooper());
			mainHandler.post(new Runnable() {
				@Override
				public void run() {
					if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL) || req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_OPEN)) {
						if (initFinishedCallback_ != null) {
							JSONObject obj = new JSONObject();
							try {
								obj.put("error_message", "Trouble reaching server. Please try again in a few minutes");
							} catch(JSONException ex) {
								ex.printStackTrace();
							}
							initFinishedCallback_.onInitFinished(obj);
						}
					} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REFERRALS)) {
						if (stateChangedCallback_ != null) {
							stateChangedCallback_.onStateChanged(false);
						}
					} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_CUSTOM_URL)) {
						if (linkCreateCallback_ != null) {
							linkCreateCallback_.onLinkCreate("Trouble reaching server. Please try again in a few minutes");
						}
					}
				}
			});
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
		return !prefHelper_.getAppInstallID().equals(PrefHelper.NO_STRING_VALUE);
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
		final boolean finUpdateListener = updateListener;
		Handler mainHandler = new Handler(context_.getMainLooper());
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (stateChangedCallback_ != null) {
					stateChangedCallback_.onStateChanged(finUpdateListener);
				}
			}
		});
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
						requestQueue_.remove(0);
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL)) {
						prefHelper_.setAppInstallID(serverResponse.getString("app_install_id"));
						prefHelper_.setUserURL(serverResponse.getString("link"));
						if (serverResponse.has("link_click_id")) {
							prefHelper_.setLinkClickID(serverResponse.getString("link_click_id"));
						} else {
							prefHelper_.setLinkClickID(PrefHelper.NO_STRING_VALUE);
						}	
						if (serverResponse.has("data")) {
							prefHelper_.setSessionParams(serverResponse.getJSONObject("data").toString());
						} else {
							prefHelper_.setSessionParams(PrefHelper.NO_STRING_VALUE);
						}
						Handler mainHandler = new Handler(context_.getMainLooper());
						mainHandler.post(new Runnable() {
							@Override
							public void run() {
								if (initFinishedCallback_ != null) {
									initFinishedCallback_.onInitFinished(getReferringParams());
								}
							}
						});
						requestQueue_.remove(0);
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_REGISTER_OPEN)) {
						if (serverResponse.has("link_click_id")) {
							prefHelper_.setLinkClickID(serverResponse.getString("link_click_id"));
						} else {
							prefHelper_.setLinkClickID(PrefHelper.NO_STRING_VALUE);
						}
						if (serverResponse.has("data")) {
							prefHelper_.setSessionParams(serverResponse.getString("data"));
						} else {
							prefHelper_.setSessionParams(PrefHelper.NO_STRING_VALUE);
						}
						Handler mainHandler = new Handler(context_.getMainLooper());
						mainHandler.post(new Runnable() {
							@Override
							public void run() {
								if (initFinishedCallback_ != null) {
									initFinishedCallback_.onInitFinished(getReferringParams());
								}
							}
						});
						requestQueue_.remove(0);
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_CREDIT_REFERRED)) {
						ServerRequest req = requestQueue_.get(0);
						String action = req.getPost().getString("action");
						int credits = req.getPost().getInt("credit");
						prefHelper_.setActionCreditCount(action, prefHelper_.getActionCreditCount(action)+credits);
						prefHelper_.setActionBalanceCount(action, Math.max(0, prefHelper_.getActionTotalCount(action)-prefHelper_.getActionCreditCount(action)));
						requestQueue_.remove(0);
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_GET_CUSTOM_URL)) {
						final String url = serverResponse.getString("url");
						Handler mainHandler = new Handler(context_.getMainLooper());
						mainHandler.post(new Runnable() {
							@Override
							public void run() {
								if (linkCreateCallback_ != null) {
									linkCreateCallback_.onLinkCreate(url);
								}
							}
						});
						requestQueue_.remove(0);
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_COMPLETE_ACTION)) {
						requestQueue_.remove(0);
					}
					

					processNextQueueItem();
				} catch (JSONException ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	public interface BranchReferralInitListener {
		public void onInitFinished(JSONObject referringParams);
	}
	
	public interface BranchReferralStateChangedListener {
		public void onStateChanged(boolean changed);
	}
	
	public interface BranchLinkCreateListener {
		public void onLinkCreate(String url);
	}
}
