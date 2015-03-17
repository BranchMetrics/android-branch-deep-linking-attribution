package io.branch.branchandroiddemo.test;

import io.branch.referral.Branch;
import io.branch.referral.Branch.BranchLinkCreateListener;
import io.branch.referral.Branch.BranchReferralInitListener;
import io.branch.referral.Branch.BranchReferralStateChangedListener;
import io.branch.referral.BranchError;
import io.branch.referral.BranchRemoteInterface;
import io.branch.referral.PrefHelper;
import io.branch.referral.PrefHelper.DebugNetworkCallback;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import android.test.InstrumentationTestCase;

public class BranchSDKTests extends InstrumentationTestCase {
	
	private Branch branch;
	private PrefHelper prefHelper;
	private static String urlFB;
	private static String referralCode;
	private static int referralCodeAmount;
	private static int referralCodeCalculationType;
	private static int referralCodeLocation;
	
	public static void setUpBeforeClass() throws Exception {
	}

	public static void tearDownAfterClass() throws Exception {
	}

	public void setUp() throws Exception {
		super.setUp();
		
		branch = Branch.getInstance(getInstrumentation().getContext());
		prefHelper = PrefHelper.getInstance(getInstrumentation().getContext());
		prefHelper.disableSmartSession();
	}

	public void tearDown() throws Exception {
		branch.resetUserSession();
		super.tearDown();
	}
	
	private void initSession() throws InterruptedException {
		final CountDownLatch signal = new CountDownLatch(1);
		branch.initSession(new BranchReferralInitListener() {
			@Override
			public void onInitFinished(JSONObject referringParams, BranchError error) {
				assertNull(error);
				signal.countDown();
			}
		});
		signal.await(1, TimeUnit.SECONDS);
	}
	
	public void test00GetShortUrlSyncFailure() {
		String url = branch.getShortUrlSync();
		assertNull(url);
	}
	
	public void test02GetShortURLAsync() throws InterruptedException, IOException {
		initSession();
		
		final CountDownLatch signal = new CountDownLatch(1);
		branch.getShortUrl("facebook", null, null, null, new BranchLinkCreateListener() {
			@Override
			public void onLinkCreate(String url, BranchError error) {
				assertNull(error);
				assertNotNull(url);
				assertTrue(url.startsWith("https://bnc.lt/l/"));
				urlFB = url;
				
				signal.countDown();
			}
		});
		signal.await(1, TimeUnit.SECONDS);
	}
	
	public void test04GetShortURLAsync1Cached() throws InterruptedException {
		initSession();
		
		final CountDownLatch signal = new CountDownLatch(1);
		branch.getShortUrl("facebook", null, null, null, new BranchLinkCreateListener() {
			@Override
			public void onLinkCreate(String url, BranchError error) {
				assertNull(error);
				assertNotNull(url);
				assertSame(url, urlFB);
				
				signal.countDown();
			}
		});
		signal.await(1, TimeUnit.SECONDS);
	}
	
	public void test04GetShortURLAsync2Uncached() throws InterruptedException {
		initSession();
		
		final CountDownLatch signal = new CountDownLatch(1);
		branch.getShortUrl("twitter", null, null, null, new BranchLinkCreateListener() {
			@Override
			public void onLinkCreate(String url, BranchError error) {
				assertNull(error);
				assertNotNull(url);
				assertFalse(url.equals(urlFB));
				
				signal.countDown();
			}
		});
		signal.await(1, TimeUnit.SECONDS);
	}
	
	public void test04GetShortURLSync() throws InterruptedException {
		initSession();
		
		String url = branch.getShortUrlSync("facebook", null, null, null);
		assertSame(url, urlFB);
		
		url = branch.getShortUrlSync("facebook", "feature1", null, null);
		assertFalse(url.equals(urlFB));
	}
	
	public void test01SetIdentity() throws InterruptedException {
		initSession();
		
		final CountDownLatch signal = new CountDownLatch(1);
		prefHelper.setIdentity(PrefHelper.NO_STRING_VALUE);
		branch.setIdentity("my_great_user", new BranchReferralInitListener() {
			@Override
			public void onInitFinished(JSONObject referringParams, BranchError error) {
				assertNull(error);
				assertNotNull(referringParams);
		        assertTrue(prefHelper.getIdentityID().equals("10406190693483182"));
		        
		        JSONObject installParams = branch.getFirstReferringParams();
		        try {
					assertTrue(installParams.getString("name").equals("test name"));
					assertTrue(installParams.getString("message").equals("hello there with short url"));
				} catch (JSONException e) {
				}

		        signal.countDown();
			}
		});
		signal.await(1, TimeUnit.SECONDS);
	}
	
	public void test03GetRewardsChanged() throws InterruptedException {
		initSession();
		
		final CountDownLatch signal = new CountDownLatch(1);
		prefHelper.setCreditCount("default", 9999999);
		
		branch.loadRewards(new BranchReferralStateChangedListener() {
			@Override
			public void onStateChanged(boolean changed, BranchError error) {
				assertNull(error);
				assertTrue(changed);
				
				signal.countDown();
			}
		});
		signal.await(1, TimeUnit.SECONDS);
	}
	
	public void testGetRewardsUnchanged() throws InterruptedException {
		initSession();
		
		final CountDownLatch signal = new CountDownLatch(1);
		prefHelper.setCreditCount("default", prefHelper.getCreditCount("default"));
		
		branch.loadRewards(new BranchReferralStateChangedListener() {
			@Override
			public void onStateChanged(boolean changed, BranchError error) {
				assertNull(error);
				assertFalse(changed);
				
				signal.countDown();
			}
		});
		signal.await(1, TimeUnit.SECONDS);
	}
	
	public void testReferralCode1Get() throws InterruptedException {
		initSession();
		
		final CountDownLatch signalGet = new CountDownLatch(1);
		branch.getReferralCode(new BranchReferralInitListener() {
			@Override
			public void onInitFinished(JSONObject referralCodeJson, BranchError error) {
				assertNull(error);
				assertNotNull(referralCodeJson);
				try {
					referralCode = referralCodeJson.getString("referral_code");
					assertNotNull(referralCode);
					referralCodeAmount = referralCodeJson.getJSONObject("metadata").getInt("amount");
					referralCodeCalculationType = Integer.parseInt(referralCodeJson.getString("calculation_type"));
					referralCodeLocation = Integer.parseInt(referralCodeJson.getString("location"));
				} catch (JSONException ignore) {
				}
				
				signalGet.countDown();

			}
		});
		signalGet.await(1, TimeUnit.SECONDS);
	}
	
	public void testReferralCode2Validate() throws InterruptedException {
		initSession();
		
		final CountDownLatch signalValidate = new CountDownLatch(1);
		branch.validateReferralCode(referralCode, new BranchReferralInitListener() {
			@Override
			public void onInitFinished(JSONObject referralCodeJson, BranchError error) {
				assertNull(error);
				assertNotNull(referralCodeJson);
				try {
					assertTrue(referralCodeJson.getString("referral_code").equals(referralCode));
					assertEquals(referralCodeJson.getJSONObject("metadata").getInt("amount"), referralCodeAmount);
					assertEquals(Integer.parseInt(referralCodeJson.getString("calculation_type")), referralCodeCalculationType);
					assertEquals(Integer.parseInt(referralCodeJson.getString("location")), referralCodeLocation);
				} catch (JSONException ignore) {
				}
				
				signalValidate.countDown();

			}
		});
		signalValidate.await(1, TimeUnit.SECONDS);
	}
	
	public void testReferralCode3Apply() throws InterruptedException {
		initSession();
		
		final CountDownLatch signalApply = new CountDownLatch(1);
		branch.applyReferralCode(referralCode, new BranchReferralInitListener() {
			@Override
			public void onInitFinished(JSONObject referralCodeJson, BranchError error) {
				assertNull(error);
				assertNotNull(referralCodeJson);
				try {
					assertTrue(referralCodeJson.getString("referral_code").equals(referralCode));
					assertEquals(referralCodeJson.getJSONObject("metadata").getInt("amount"), referralCodeAmount);
					assertEquals(Integer.parseInt(referralCodeJson.getString("calculation_type")), referralCodeCalculationType);
					assertEquals(Integer.parseInt(referralCodeJson.getString("location")), referralCodeLocation);
				} catch (JSONException ignore) {
				}
				
				signalApply.countDown();
			}
		});
		signalApply.await(1, TimeUnit.SECONDS);
	}
	
	public void testYDebuggerConnect() {
		BranchRemoteInterface remoteInterface = new BranchRemoteInterface(getInstrumentation().getContext());
		DebugNetworkCallback callback = new DebugNetworkCallback();
		remoteInterface.setNetworkCallbackListener(callback);
		remoteInterface.connectToDebug();
		assertTrue(callback.getConnectionStatus() == 465 || callback.getConnectionStatus() == 200);
	}
	
	public void testZLoad() throws InterruptedException {
		initSession();
		
		final CountDownLatch signalFinal = new CountDownLatch(1);
		final CountDownLatch signal = new CountDownLatch(1);
		for (int i = 0; i < 100; i++) {
			final int idx = i;
			branch.getShortUrl(i + "", null, null, null, new BranchLinkCreateListener() {
				@Override
				public void onLinkCreate(String url, BranchError error) {
					assertNull(error);
					assertNotNull(url);
					if (idx == 99) {
						signal.countDown();
					}
				}
			});
			Thread.sleep(50);
		}
		signal.await(10, TimeUnit.SECONDS);
		
		branch.getShortUrl(null, "loadTest", null, null, new BranchLinkCreateListener() {
			@Override
			public void onLinkCreate(String url, BranchError error) {
				assertNull(error);
				assertNotNull(url);
				signalFinal.countDown();
			}
		});
		signalFinal.await(1, TimeUnit.SECONDS);
	}
}
