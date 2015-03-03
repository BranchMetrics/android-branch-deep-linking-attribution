package io.branch.branchandroiddemo.test;

import io.branch.referral.Branch;
import io.branch.referral.Branch.BranchLinkCreateListener;
import io.branch.referral.Branch.BranchReferralInitListener;
import io.branch.referral.Branch.BranchReferralStateChangedListener;
import io.branch.referral.BranchError;
import io.branch.referral.PrefHelper;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import android.test.InstrumentationTestCase;
import android.util.Log;

public class BranchSDKTests extends InstrumentationTestCase {
	
	CountDownLatch signal;
	Branch branch;
	String urlFB, urlTT;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		super.setUp();
		signal = new CountDownLatch(1);
		branch = Branch.getInstance(getInstrumentation().getContext());
		PrefHelper.getInstance(getInstrumentation().getContext()).disableSmartSession();
		initSession();
	}

	@After
	public void tearDown() throws Exception {
		branch.resetUserSession();
		super.tearDown();
	}
	
	private void initSession() throws InterruptedException {
		branch.initSession(new BranchReferralInitListener() {
			@Override
			public void onInitFinished(JSONObject referringParams, BranchError error) {
				Log.i("Branch SDK Test", "branch init complete!");
				try {
					Iterator<?> keys = referringParams.keys();
					while (keys.hasNext()) {
						String key = (String) keys.next();
						Log.i("BranchTestBed", key + ", " + referringParams.getString(key));
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void test1GetShortUrlSyncFailure() {
		String url = branch.getShortUrlSync();
		assertNull(url);
	}
	
	public void testGetShortURL() throws InterruptedException {
		
		branch.getShortUrl("facebook", null, null, null, new BranchLinkCreateListener() {
			@Override
			public void onLinkCreate(String url, BranchError error) {
				assertNull(error);
				assertTrue(url.startsWith("https://bnc.lt/l/"));
				urlFB = url;
				
				branch.getShortUrl("facebook", null, null, null, new BranchLinkCreateListener() {
					@Override
					public void onLinkCreate(String url, BranchError error) {
						assertNull(error);
						assertSame(url, urlFB);
						
						branch.getShortUrl("twitter", null, null, null, new BranchLinkCreateListener() {
							@Override
							public void onLinkCreate(String url, BranchError error) {
								assertNull(error);
								assertFalse(url.equals(urlFB));
								urlTT = url;
										
								signal.countDown();
							}
						});
					}
				});
			}
		});
		signal.await(1, TimeUnit.SECONDS);
		
		String url = branch.getShortUrlSync("facebook", null, null, null);
		assertNotNull(url);
		assertSame(url, urlFB);
		
		url = branch.getShortUrlSync("twitter", null, null, null);
		assertSame(url, urlTT);
	}
	
	public void testGetRewards() throws InterruptedException {
		branch.loadRewards(new BranchReferralStateChangedListener() {
			@Override
			public void onStateChanged(boolean changed, BranchError error) {
				assertNull(error);
				
				signal.countDown();
			}
		});
		signal.await(1, TimeUnit.SECONDS);
	}
	
	public void testReferralCode1Get() {
		branch.getReferralCode(prefix, amount, expiration, null, getCalculationType(), getLocation(), new BranchReferralInitListener() {
			@Override
			public void onInitFinished(JSONObject referralCode, BranchError error) {
				try {
					// Ugly! will add error code soon.
					if (!referralCode.has("error_message")) {
						txtReferralCode.setText(referralCode.getString("referral_code"));
					} else {
						txtReferralCode.setText(referralCode.getString("error_message"));
					}
				} catch (JSONException e) {
					txtReferralCode.setText("Error parsing JSON");
				}
			}
		});
	}
	
}
