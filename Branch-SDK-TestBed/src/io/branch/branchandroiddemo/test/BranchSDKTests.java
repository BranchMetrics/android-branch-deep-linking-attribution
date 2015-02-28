package io.branch.branchandroiddemo.test;

import io.branch.referral.Branch;
import io.branch.referral.Branch.BranchLinkCreateListener;
import io.branch.referral.Branch.BranchReferralInitListener;
import io.branch.referral.BranchError;

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
		initSession();
	}

	@After
	public void tearDown() throws Exception {
		branch.closeSession();
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
	
	public void testGetShortURLAsync()  throws InterruptedException {
		branch.getShortUrl("facebook", null, null, null, new BranchLinkCreateListener() {
			@Override
			public void onLinkCreate(String url, BranchError error) {
				assertNull(error);
				assertTrue(url.startsWith("https://bnc.lt/l/"));
				signal.countDown();
			}
		});
		signal.await(1, TimeUnit.SECONDS);
	}
	
	public void testGetShortUrlSync() {
		String url1 = branch.getShortUrlSync();
		assertNotNull(url1);
		
		String url2 = branch.getShortUrlSync();
		assertEquals(url1, url2);
		
		String url3 = branch.getContentUrlSync("twitter", null);
		assertFalse(url1.equals(url3));
	}
	
}
