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
	String referralCode;

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
	
	public void testReferralCode() throws InterruptedException {
		// Get
		branch.getReferralCode("test", 7, new BranchReferralInitListener() {
			@Override
			public void onInitFinished(JSONObject referralCodeJson, BranchError error) {
				assertNull(error);
				try {
					referralCode = referralCodeJson.getString("referral_code");
					assertTrue(referralCode.startsWith("test"));
					assertEquals(referralCodeJson.getJSONObject("metadata").getInt("amount"), 7);
					assertEquals(Integer.parseInt(referralCodeJson.getString("calculation_type")), Branch.REFERRAL_CODE_AWARD_UNLIMITED);
					assertEquals(Integer.parseInt(referralCodeJson.getString("location")), Branch.REFERRAL_CODE_LOCATION_REFERRING_USER);
				} catch (JSONException ignore) {
				}
				
				signal.countDown();

			}
		});
		signal.await(1, TimeUnit.SECONDS);
		
		// Validate
		final CountDownLatch signalValidate = new CountDownLatch(1);
		branch.validateReferralCode(referralCode, new BranchReferralInitListener() {
			@Override
			public void onInitFinished(JSONObject referralCodeJson, BranchError error) {
				assertNull(error);
				try {
					assertTrue(referralCodeJson.getString("referral_code").equals(referralCode));
					assertEquals(referralCodeJson.getJSONObject("metadata").getInt("amount"), 7);
					assertEquals(Integer.parseInt(referralCodeJson.getString("calculation_type")), Branch.REFERRAL_CODE_AWARD_UNLIMITED);
					assertEquals(Integer.parseInt(referralCodeJson.getString("location")), Branch.REFERRAL_CODE_LOCATION_REFERRING_USER);
				} catch (JSONException ignore) {
				}
				
				signalValidate.countDown();

			}
		});
		signalValidate.await(1, TimeUnit.SECONDS);
		
		// Apply
		final CountDownLatch signalApply = new CountDownLatch(1);
		branch.applyReferralCode(referralCode, new BranchReferralInitListener() {
			@Override
			public void onInitFinished(JSONObject referralCodeJson, BranchError error) {
				assertNull(error);
				try {
					assertTrue(referralCodeJson.getString("referral_code").equals(referralCode));
					assertEquals(referralCodeJson.getJSONObject("metadata").getInt("amount"), 7);
					assertEquals(Integer.parseInt(referralCodeJson.getString("calculation_type")), Branch.REFERRAL_CODE_AWARD_UNLIMITED);
					assertEquals(Integer.parseInt(referralCodeJson.getString("location")), Branch.REFERRAL_CODE_LOCATION_REFERRING_USER);
				} catch (JSONException ignore) {
				}
				
				signalApply.countDown();

			}
		});
		signalApply.await(1, TimeUnit.SECONDS);

	}
	
}
