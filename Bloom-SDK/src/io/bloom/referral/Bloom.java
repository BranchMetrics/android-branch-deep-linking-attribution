package io.bloom.referral;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class Bloom {
	public static final String ACTION_INSTALL = "install";
	
	private static final int MAX_STOPPED_TIMER = 60000;
	private static final int INTERVAL_SESSION_CHECK = 5000;
	private static final int INTERVAL_SYNC = 10000;
	private static final int INTERVAL_RETRY = 3000;
	private static final int MAX_RETRIES = 10;

	private static Bloom appidemicReferral_;
	
	private BloomReferralStateChanged stateChangedCallback_;
	
	private BloomRemoteInterface kRemoteInterface_;
	private PrefHelper prefHelper_;
	
	private Thread workerThread_;
	private boolean requestStopRunSync_;
	private int stopRequestTimer_;
	private boolean runSync_;
	private Timer sessionTimer_;
	private boolean sessionActive_;
	
	private Semaphore serverSema_;
	private ArrayList<ServerRequest> requestQueue_;
	private int networkCount_;
	private int retryCount_;
	
	private Bloom(Context context) {
		prefHelper_ = PrefHelper.getInstance(context);
		kRemoteInterface_ = new BloomRemoteInterface(context);
		kRemoteInterface_.setNetworkCallbackListener(new ReferralNetworkCallback());
		requestQueue_ = new ArrayList<ServerRequest>();
		serverSema_ = new Semaphore(1);
		networkCount_ = 0;
		runSync_ = true;
		sessionActive_ = false;
	}
	
	public static void startSession(Context context, String key) {
		if (appidemicReferral_ == null) {
			appidemicReferral_ = new Bloom(context);
		}
		appidemicReferral_.initSession();
		appidemicReferral_.runSync_ = true;
		appidemicReferral_.requestStopRunSync_ = false;
		appidemicReferral_.prefHelper_.setAppKey(key);
	}
	
	public static void stopSession() {
		if (appidemicReferral_ != null) {
			appidemicReferral_.requestStopRunSync_ = false;
			appidemicReferral_.stopRequestTimer_ = 0;
		}
	}
	
	public static void setStateChangedCallback(BloomReferralStateChanged callback) {
		if (appidemicReferral_ != null)
			appidemicReferral_.stateChangedCallback_ = callback;
	}
	
	public static void forceSyncPoints() {
		if (appidemicReferral_ != null) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					appidemicReferral_.requestQueue_.add(new ServerRequest(BloomRemoteInterface.REQ_TAG_GET_REFERRALS, null));
					appidemicReferral_.processNextQueueItem();
				}
			}).start();
		}
	}
	
	public static void creditUserForReferral(final String action, final int credit) {
		if (appidemicReferral_ != null) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					appidemicReferral_.retryCount_ = 0;
					JSONObject post = new JSONObject();
					try {
						post.put("action", action);
						post.put("credit", credit);
					} catch (JSONException ex) {
						ex.printStackTrace();
						return;
					}
					appidemicReferral_.requestQueue_.add(new ServerRequest(BloomRemoteInterface.REQ_TAG_CREDIT_REFERRED, post));
					appidemicReferral_.processNextQueueItem();
				}
			}).start();
		}
	}
	
	public static void userCompletedAction(final String action) {
		if (appidemicReferral_ != null) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					appidemicReferral_.retryCount_ = 0;
					JSONObject post = new JSONObject();
					try {
						post.put("action", action);
					} catch (JSONException ex) {
						ex.printStackTrace();
						return;
					}
					appidemicReferral_.requestQueue_.add(new ServerRequest(BloomRemoteInterface.REQ_TAG_COMPLETE_ACTION, post));
					appidemicReferral_.processNextQueueItem();
				}
			}).start();
		}
	}
	
	public static String getReferralURL() {
		if (appidemicReferral_.hasUser()) {
			return appidemicReferral_.prefHelper_.getUserURL();
		} else {
			return "still creating, try again shortly";
		}
	}
	
	public static String getReferralURLForTextMessage() {
		return getReferralURLWithTag("txt");
	}
	public static String getReferralURLForEmail() {
		return getReferralURLWithTag("em");
	}
	public static String getReferralURLForFacebook() {
		return getReferralURLWithTag("fb");
	}
	public static String getReferralURLForTwitter() {
		return getReferralURLWithTag("tw");
	}
	public static String getReferralURLWithTag(String tag) {
		if (appidemicReferral_.hasUser()) {
			return appidemicReferral_.prefHelper_.getUserURL() + "?t=" + tag;
		} else {
			return "still creating, try again shortly";
		}
	}
	
	private void processNextQueueItem() {
		try {
			serverSema_.acquire();
			if (networkCount_ == 0 && requestQueue_.size() > 0) {
				networkCount_ = 1;
				serverSema_.release();
				
				ServerRequest req = requestQueue_.get(0);
				
				if (req.getTag().equals(BloomRemoteInterface.REQ_TAG_REGISTER_INSTALL)) {
					Log.i("AppidemicSDK", "calling register install");
					kRemoteInterface_.registerInstall();
				} else if (req.getTag().equals(BloomRemoteInterface.REQ_TAG_REGISTER_OPEN)) {
					Log.i("AppidemicSDK", "calling register open");
					kRemoteInterface_.registerOpen();
				} else if (req.getTag().equals(BloomRemoteInterface.REQ_TAG_GET_REFERRALS) && hasUser()) {
					Log.i("AppidemicSDK", "calling get referrals");
					kRemoteInterface_.getReferrals();
				} else if (req.getTag().equals(BloomRemoteInterface.REQ_TAG_CREDIT_REFERRED) && hasUser()) {
					Log.i("AppidemicSDK", "calling credit referrals");
					appidemicReferral_.kRemoteInterface_.creditUserForReferrals(req.getPost());
				} else if (req.getTag().equals(BloomRemoteInterface.REQ_TAG_COMPLETE_ACTION) && hasUser()){
					Log.i("AppidemicSDK", "calling completed action");
					appidemicReferral_.kRemoteInterface_.userCompletedAction(req.getPost());
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
			if (req.getTag().equals(BloomRemoteInterface.REQ_TAG_REGISTER_INSTALL)) {
				return true;
			}
		}
		return false;
	}
	
	private void moveInstallToFront() {
		for (int i = 0; i < requestQueue_.size(); i++) {
			ServerRequest req = requestQueue_.get(i);
			if (req.getTag().equals(BloomRemoteInterface.REQ_TAG_REGISTER_INSTALL)) {
				requestQueue_.remove(i);
				break;
			}
		}
		requestQueue_.add(0, new ServerRequest(BloomRemoteInterface.REQ_TAG_REGISTER_INSTALL, null));
	}
	
	private boolean hasUser() {
		return !prefHelper_.getUserID().equals(PrefHelper.NO_STRING_VALUE);
	}
	
	private void registerInstall() {
		if (!installInQueue()) {
			requestQueue_.add(0, new ServerRequest(BloomRemoteInterface.REQ_TAG_REGISTER_INSTALL, null));
		} else {
			moveInstallToFront();
		}
		processNextQueueItem();
	}
	
	private void registerOpen() {
		requestQueue_.add(0, new ServerRequest(BloomRemoteInterface.REQ_TAG_REGISTER_OPEN, null));
		processNextQueueItem();
	}
	
	private void initSession() {
		// check if session active
		// if not active, register install or open appropriately
		if (!sessionActive_) {
			if (hasUser()) {
				registerOpen();
			} else {
				registerInstall();
			}
			sessionTimer_ = new Timer();
			sessionTimer_.schedule(new TimerTask() {

				@Override
				public void run() {
					if (requestStopRunSync_) {
						stopRequestTimer_ = stopRequestTimer_ + 1;
					} else {
						stopRequestTimer_ = 0;
					}
					
					if (stopRequestTimer_ > MAX_STOPPED_TIMER) {
						runSync_ = false;
						sessionTimer_.cancel();
						sessionTimer_ = null;
						sessionActive_ = false;
						workerThread_ = null;
					}
				}
				
			}, 0, INTERVAL_SESSION_CHECK);
			sessionActive_ = true;
		} 
		
		if (workerThread_ == null) {
			workerThread_ = new Thread(new Runnable() {
				@Override
				public void run() {
					while (runSync_) {
						appidemicReferral_.requestQueue_.add(new ServerRequest(BloomRemoteInterface.REQ_TAG_GET_REFERRALS, null));
						appidemicReferral_.processNextQueueItem();
						
						try {
							Thread.sleep(INTERVAL_SYNC);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			});
		}
	}
	
	private void processReferralCounts(JSONObject obj) {
		boolean updateListener = false;
		Iterator<?> keys = obj.keys();
		while (keys.hasNext()) {
			String key = (String)keys.next();
			if (key.equals(BloomRemoteInterface.KEY_SERVER_CALL_STATUS_CODE) || key.equals(BloomRemoteInterface.KEY_SERVER_CALL_TAG))
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
					int status = serverResponse.getInt(BloomRemoteInterface.KEY_SERVER_CALL_STATUS_CODE);
					String requestTag = serverResponse.getString(BloomRemoteInterface.KEY_SERVER_CALL_TAG);
					
					networkCount_ = 0;
					if (status != 200) {
						retryLastRequest();
					} else if (requestTag.equals(BloomRemoteInterface.REQ_TAG_GET_REFERRALS)) {
						processReferralCounts(serverResponse);
					} else if (requestTag.equals(BloomRemoteInterface.REQ_TAG_REGISTER_INSTALL)) {
						String userID = serverResponse.getString("user_id");
						prefHelper_.setUserID(userID);
						prefHelper_.setUserURL(prefHelper_.getShortURL() + userID);
					} else if (requestTag.equals(BloomRemoteInterface.REQ_TAG_REGISTER_OPEN)) {
						if (serverResponse.has("link_click_id")) {
							prefHelper_.setLinkClickID(serverResponse.getString("link_click_id"));
						} else {
							prefHelper_.setLinkClickID(PrefHelper.NO_STRING_VALUE);
						}	
					} else if (requestTag.equals(BloomRemoteInterface.REQ_TAG_CREDIT_REFERRED)) {
						ServerRequest req = requestQueue_.get(0);
						String action = req.getPost().getString("action");
						int credits = req.getPost().getInt("credits");
						prefHelper_.setActionCreditCount(action, prefHelper_.getActionCreditCount(action)+credits);
						prefHelper_.setActionBalanceCount(action, Math.max(0, prefHelper_.getActionTotalCount(action)-prefHelper_.getActionCreditCount(action)));
					}
					
					requestQueue_.remove(0);

					processNextQueueItem();
				} catch (JSONException ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	public interface BloomReferralStateChanged {
		public void onStateChanged();
	}
}
