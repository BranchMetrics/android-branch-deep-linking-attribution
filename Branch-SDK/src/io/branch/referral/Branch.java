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
	
	private BranchReferralInitListener initSessionFinishedCallback_;
	private BranchReferralInitListener initIdentityFinishedCallback_;
	private BranchReferralStateChangedListener stateChangedCallback_;
	private BranchLinkCreateListener linkCreateCallback_;
	
	private BranchRemoteInterface kRemoteInterface_;
	private PrefHelper prefHelper_;
	private SystemObserver systemObserver_;
	private Context context_;
	
	private Semaphore serverSema_;
	private ArrayList<ServerRequest> requestQueue_;
	private int networkCount_;
	private int retryCount_;
	
	private Branch(Context context) {
		prefHelper_ = PrefHelper.getInstance(context);
		kRemoteInterface_ = new BranchRemoteInterface(context);
		systemObserver_ = new SystemObserver(context);
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
	
	public static Branch getInstance() {
		if (branchReferral_ == null) {
			Log.i("BranchSDK", "Branch Warning: getInstance called before getInstance with key. Please init");
		}
		return branchReferral_;
	}
	
	public void resetUserSession() {
		isInit_ = false;
	}
	
	public void initUserSession(BranchReferralInitListener callback) {
		if (systemObserver_.getUpdateState() == 0 && !hasUser()) {
			prefHelper_.setIsReferrable();
		} else {
			prefHelper_.clearIsReferrable();
		}	
		initUserSessionInternal(callback);
	}
	
	public void initUserSession() {
		initUserSession(null);
	}
	
	public void initUserSession(boolean isReferrable) {
		initUserSession(null, isReferrable);
	}
	
	public void initUserSession(BranchReferralInitListener callback, boolean isReferrable) {
		if (isReferrable) {
			this.prefHelper_.setIsReferrable();
		} else {
			this.prefHelper_.clearIsReferrable();
		}
		initUserSessionInternal(callback);
	}
	
	private void initUserSessionInternal(BranchReferralInitListener callback) {
		initSessionFinishedCallback_ = callback;
		if (!isInit_) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					initSession();
				}
			}).start();
			isInit_ = true;
		} else if (!installOrOpenInQueue()) {
			if (callback != null) callback.onInitFinished(getReferringParams());
		}
	}
	
	public void closeSession() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				requestQueue_.add(new ServerRequest(BranchRemoteInterface.REQ_TAG_REGISTER_CLOSE, null));
				processNextQueueItem();
			}
		}).start();
	}
	
	public void identifyUser(String userId, BranchReferralInitListener callback) {
		initIdentityFinishedCallback_ = callback;
		identifyUser(userId);
	}
	
	public void identifyUser(final String userId) {
		if (!identifyInQueue()) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					JSONObject post = new JSONObject();
					try {
						post.put("app_id", prefHelper_.getAppKey());
						post.put("identity_id", prefHelper_.getIdentityID());
						post.put("identity", userId);
					} catch (JSONException ex) {
						ex.printStackTrace();
						return;
					}
					requestQueue_.add(new ServerRequest(BranchRemoteInterface.REQ_TAG_IDENTIFY, post));
					processNextQueueItem();
				}
			}).start();
		}
	}
	
	public void clearUser() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				JSONObject post = new JSONObject();
				try {
					post.put("app_id", prefHelper_.getAppKey());
					post.put("session_id", prefHelper_.getSessionID());
				} catch (JSONException ex) {
					ex.printStackTrace();
					return;
				}
				requestQueue_.add(new ServerRequest(BranchRemoteInterface.REQ_TAG_LOGOUT, post));
				processNextQueueItem();
			}
		}).start();
	}
	
	public void loadActionCounts() {
		loadActionCounts(null);
	}
	
	public void loadActionCounts(BranchReferralStateChangedListener callback) {
		stateChangedCallback_ = callback;
		new Thread(new Runnable() {
			@Override
			public void run() {
				requestQueue_.add(new ServerRequest(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_COUNTS, null));
				processNextQueueItem();
			}
		}).start();
	}
	
	public void loadRewards() {
		loadRewards(null);
	}
	
	public void loadRewards(BranchReferralStateChangedListener callback) {
		stateChangedCallback_ = callback;
		new Thread(new Runnable() {
			@Override
			public void run() {
				requestQueue_.add(new ServerRequest(BranchRemoteInterface.REQ_TAG_GET_REWARDS, null));
				processNextQueueItem();
			}
		}).start();
	}
	
	public int getCredits() {
		return prefHelper_.getCreditCount();
	}
	
	public int getCreditsForBucket(String bucket) {
		return prefHelper_.getCreditCount(bucket);
	}
	
	public int getTotalCountsForAction(String action) {
		return prefHelper_.getActionTotalCount(action);
	}
	
	public int getUniqueCountsForAction(String action) {
		return prefHelper_.getActionUniqueCount(action);
	}
	
	public void redeemRewards(int count) {
		redeemRewards("default", count);
	}
	
	public void redeemRewards(final String bucket, final int count) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				int creditsToRedeem = 0;
				int credits = prefHelper_.getCreditCount(bucket);
				
				if (count > credits) {
					creditsToRedeem = credits;
					Log.i("BranchSDK", "Branch Warning: You're trying to redeem more credits than are available. Have you updated loaded rewards");
				} else {
					creditsToRedeem = count;
				}
				
				if (creditsToRedeem > 0) {
					retryCount_ = 0;
					JSONObject post = new JSONObject();
					try {
						post.put("app_id", prefHelper_.getAppKey());
						post.put("identity_id", prefHelper_.getIdentityID());
						post.put("bucket", bucket);
						post.put("amount", creditsToRedeem);
					} catch (JSONException ex) {
						ex.printStackTrace();
						return;
					}
					requestQueue_.add(new ServerRequest(BranchRemoteInterface.REQ_TAG_REDEEM_REWARDS, post));
					processNextQueueItem();
				}
			}
		}).start();
	}
	
	public void userCompletedAction(final String action, final JSONObject metadata) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				retryCount_ = 0;
				JSONObject post = new JSONObject();
				try {
					post.put("app_id", prefHelper_.getAppKey());
					post.put("session_id", prefHelper_.getSessionID());
					post.put("event", action);
					if (metadata != null) post.put("metadata", metadata);
				} catch (JSONException ex) {
					ex.printStackTrace();
					return;
				}
				requestQueue_.add(new ServerRequest(BranchRemoteInterface.REQ_TAG_COMPLETE_ACTION, post));
				processNextQueueItem();
			}
		}).start();
	}
	
	public void userCompletedAction(final String action) {
		userCompletedAction(action, null);
	}
	
	public JSONObject getInstallReferringParams() {
		String storedParam = prefHelper_.getInstallParams();
		return convertParamsStringToDictionary(storedParam);
	}
	
	public JSONObject getReferringParams() {
		String storedParam = prefHelper_.getSessionParams();
		return convertParamsStringToDictionary(storedParam);
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
						linkPost.put("identity_id", prefHelper_.getIdentityID());
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
	
	private JSONObject convertParamsStringToDictionary(String paramString) {
		if (paramString.equals(PrefHelper.NO_STRING_VALUE)) {
			return new JSONObject();
		} else {
			try {
				return new JSONObject(paramString);
			} catch (JSONException e) {
				byte[] encodedArray = Base64.decode(paramString.getBytes(), Base64.NO_WRAP);
				try {
					return new JSONObject(new String(encodedArray));
				} catch (JSONException ex) {
					ex.printStackTrace();
					return new JSONObject();
				}
			}
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
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_COUNTS) && hasUser()) {
					kRemoteInterface_.getReferralCounts();
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REWARDS) && hasUser()) {
					kRemoteInterface_.getRewards();
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REDEEM_REWARDS) && hasUser()) {
					kRemoteInterface_.redeemRewards(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_COMPLETE_ACTION) && hasUser()){
					kRemoteInterface_.userCompletedAction(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_CUSTOM_URL) && hasUser()) {
					kRemoteInterface_.createCustomUrl(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_IDENTIFY) && hasUser()) {
					kRemoteInterface_.identifyUser(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_CLOSE) && hasUser()) {
					kRemoteInterface_.registerClose();
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_LOGOUT) && hasUser()) {
					kRemoteInterface_.logoutUser(req.getPost());
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
					if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL) || req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_OPEN) ) {
						if (initSessionFinishedCallback_ != null) {
							JSONObject obj = new JSONObject();
							try {
								obj.put("error_message", "Trouble reaching server. Please try again in a few minutes");
							} catch(JSONException ex) {
								ex.printStackTrace();
							}
							initSessionFinishedCallback_.onInitFinished(obj);
						}
					} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_COUNTS) || req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REWARDS)) {
						if (stateChangedCallback_ != null) {
							stateChangedCallback_.onStateChanged(false);
						}
					} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_CUSTOM_URL)) {
						if (linkCreateCallback_ != null) {
							linkCreateCallback_.onLinkCreate("Trouble reaching server. Please try again in a few minutes");
						}
					} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_IDENTIFY)) {
						if (initIdentityFinishedCallback_ != null) {
							JSONObject obj = new JSONObject();
							try {
								obj.put("error_message", "Trouble reaching server. Please try again in a few minutes");
							} catch(JSONException ex) {
								ex.printStackTrace();
							}
							initIdentityFinishedCallback_.onInitFinished(obj);
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
	
	private void updateAllRequestsInQueue() {
		try {
			for (int i = 0; i < requestQueue_.size(); i++) {
				ServerRequest req = requestQueue_.get(i);
				if (req.getPost() != null) {
					Iterator<?> keys = req.getPost().keys();
		    		while (keys.hasNext()) {
		    			String key = (String)keys.next();
		    			if (key.equals("app_id")) {
		    				req.getPost().put(key, prefHelper_.getAppKey());
		    			} else if (key.equals("session_id")) {
		    				req.getPost().put(key, prefHelper_.getSessionID());
		    			} else if (key.equals("identity_id")) {
		    				req.getPost().put(key, prefHelper_.getIdentityID());
		    			}
		    		}	
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}	
	}
	
	private boolean identifyInQueue() {
		for (int i = 0; i < requestQueue_.size(); i++) {
			ServerRequest req = requestQueue_.get(i);
			if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_IDENTIFY)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean installOrOpenInQueue() {
		for (int i = 0; i < requestQueue_.size(); i++) {
			ServerRequest req = requestQueue_.get(i);
			if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL) || req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_OPEN)) {
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
		return !prefHelper_.getIdentityID().equals(PrefHelper.NO_STRING_VALUE);
	}
	
	private void registerInstall() {
		if (!installOrOpenInQueue()) {
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
				int unique = counts.getInt("unique");
				
				if (total != prefHelper_.getActionTotalCount(key) || unique != prefHelper_.getActionUniqueCount(key)) {
					updateListener = true;
				}
				prefHelper_.setActionTotalCount(key, total);
				prefHelper_.setActionUniqueCount(key, unique);
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
	
	private void processRewardCounts(JSONObject obj) {
		boolean updateListener = false;
		Iterator<?> keys = obj.keys();
		while (keys.hasNext()) {
			String key = (String)keys.next();
			if (key.equals(BranchRemoteInterface.KEY_SERVER_CALL_STATUS_CODE) || key.equals(BranchRemoteInterface.KEY_SERVER_CALL_TAG))
				continue;
			
			try {
				int credits = obj.getInt(key);
				
				if (credits != prefHelper_.getCreditCount(key)) {
					updateListener = true;
				}
				prefHelper_.setCreditCount(key, credits);
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
					if (status >= 500) {
						retryLastRequest();
					} else if (status >= 400 && status < 500) {
						Log.i("BranchSDK", "Branch API Error: " + serverResponse.getString("message"));
						requestQueue_.remove(0);
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_COUNTS)) {
						processReferralCounts(serverResponse);
						requestQueue_.remove(0);
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_GET_REWARDS)) {
						processRewardCounts(serverResponse);
						requestQueue_.remove(0);
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL)) {
						prefHelper_.setDeviceFingerPrintID(serverResponse.getString("device_fingerprint_id"));
						prefHelper_.setIdentityID(serverResponse.getString("identity_id"));
						prefHelper_.setUserURL(serverResponse.getString("link"));
						prefHelper_.setSessionID(serverResponse.getString("session_id"));
						
						if (prefHelper_.getIsReferrable() == 1) {
							if (serverResponse.has("data")) {
								String params = serverResponse.getString("data");
								prefHelper_.setInstallParams(params);
							} else {
								prefHelper_.setInstallParams(PrefHelper.NO_STRING_VALUE);
							}
						}
						
						if (serverResponse.has("link_click_id")) {
							prefHelper_.setLinkClickID(serverResponse.getString("link_click_id"));
						} else {
							prefHelper_.setLinkClickID(PrefHelper.NO_STRING_VALUE);
						}	
						if (serverResponse.has("data")) {
							String params = serverResponse.getString("data");
							prefHelper_.setSessionParams(params);
						} else {
							prefHelper_.setSessionParams(PrefHelper.NO_STRING_VALUE);
						}
						
						updateAllRequestsInQueue();
						
						Handler mainHandler = new Handler(context_.getMainLooper());
						mainHandler.post(new Runnable() {
							@Override
							public void run() {
								if (initSessionFinishedCallback_ != null) {
									initSessionFinishedCallback_.onInitFinished(getReferringParams());
								}
							}
						});
						requestQueue_.remove(0);
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_REGISTER_OPEN)) {
						prefHelper_.setSessionID(serverResponse.getString("session_id"));
						
						if (serverResponse.has("link_click_id")) {
							prefHelper_.setLinkClickID(serverResponse.getString("link_click_id"));
						} else {
							prefHelper_.setLinkClickID(PrefHelper.NO_STRING_VALUE);
						}
						if (prefHelper_.getIsReferrable() == 1) {
							if (serverResponse.has("data")) {
								String params = serverResponse.getString("data");
								prefHelper_.setInstallParams(params);
							} 
						}
						if (serverResponse.has("data")) {
							String params = serverResponse.getString("data");
							prefHelper_.setSessionParams(params);
						} else {
							prefHelper_.setSessionParams(PrefHelper.NO_STRING_VALUE);
						}
						Handler mainHandler = new Handler(context_.getMainLooper());
						mainHandler.post(new Runnable() {
							@Override
							public void run() {
								if (initSessionFinishedCallback_ != null) {
									initSessionFinishedCallback_.onInitFinished(getReferringParams());
								}
							}
						});
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
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_LOGOUT)) {
						prefHelper_.setSessionID(serverResponse.getString("session_id"));
						prefHelper_.setIdentityID(serverResponse.getString("identity_id"));
						prefHelper_.setUserURL(serverResponse.getString("link"));
						
						prefHelper_.setInstallParams(PrefHelper.NO_STRING_VALUE);
						prefHelper_.setSessionParams(PrefHelper.NO_STRING_VALUE);
						prefHelper_.clearUserValues();
						
						requestQueue_.remove(0);
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_IDENTIFY)) {
						prefHelper_.setIdentityID(serverResponse.getString("identity_id"));
						prefHelper_.setUserURL(serverResponse.getString("link"));
						
						if (serverResponse.has("referring_data")) {
							String params = serverResponse.getString("referring_data");
							prefHelper_.setInstallParams(params);
						} 
						Handler mainHandler = new Handler(context_.getMainLooper());
						mainHandler.post(new Runnable() {
							@Override
							public void run() {
								if (initIdentityFinishedCallback_ != null) {
									initIdentityFinishedCallback_.onInitFinished(getInstallReferringParams());
								}
							}
						});
						requestQueue_.remove(0);
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_COMPLETE_ACTION) || requestTag.equals(BranchRemoteInterface.REQ_TAG_REGISTER_CLOSE) || requestTag.equals(BranchRemoteInterface.REQ_TAG_REDEEM_REWARDS)) {
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
