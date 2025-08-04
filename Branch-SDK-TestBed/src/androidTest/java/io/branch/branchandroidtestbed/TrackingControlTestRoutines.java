package io.branch.branchandroidtestbed;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;

/**
 * Created by sojanpr on 3/20/18.
 * <p>
 * Class for testing the overall Tracking control behaviour.
 * This is not unit test. This test need to be run on a device or simuluator.
 * To run this tests call  {@link #testLinkCreation(boolean)} )} from {@link Branch.BranchReferralInitListener} callback.
 * </p>
 */

public class TrackingControlTestRoutines {
    private static TrackingControlTestRoutines instance;
    private final BranchUniversalObject buo;
    private static final String TEST_TITLE = "test_title";
    private static final String TEST_DESC = "test_description";
    private static final String LINK_HOST = "bnctestbed.test-app.link";
    private static final int BRANCH_INIT_WAIT_TIME = 2000; // Wait for 3 sec  for branhc to init
    private static final int TEST_FAILED = -1;
    private static int loadTestCounter;
    private static final int MAX_LOAD_CNT = 25;
    private static final String TAG = "Branch:TrackingCtrlTest";
    private final Context context;
    
    
    private static TrackingControlTestRoutines getInstance(Context context) {
        if (instance == null) {
            instance = new TrackingControlTestRoutines(context);
        }
        return instance;
    }
    
    private TrackingControlTestRoutines(Context context) {
        this.context = context;
        buo = new BranchUniversalObject().setTitle(TEST_TITLE).setContentDescription(TEST_DESC);
    }
    
    public static void  runTrackingControlTest(Context context) {
        getInstance(context).runTrackingControlTest(0);
    }
    
    private void runTrackingControlTest(int stateCnt) {
        if (stateCnt == 0) {
            Log.d(TAG, "1. Link creation test <Tracking disabled>");
            disableTracking();
            if (!testLinkCreation(true)) {
                Log.d(TAG, "Link creation test failed when Tracking is disabled");
                return;
            }
            stateCnt++;
        }
        if (stateCnt == 1) {
            enableTrackingAndProceed(stateCnt + 1);
            return;
        }
        if (stateCnt == 2) {
            Log.d(TAG, "2. Link creation test <Tracking enabled>");
            if (!testLinkCreation(false)) {
                Log.d(TAG, "Link creation test failed when Tracking is enabled");
                return;
            }
            stateCnt++;
        }
        if (stateCnt == 3) {
            Log.d(TAG, "3. Branch event test <Tracking disabled>");
            disableTracking();
            testBranchEvent(stateCnt + 1);
        }
        if (stateCnt == 4) {
            Log.d(TAG, "4. Branch event test <Tracking enabled>");
            enableTrackingAndProceed(stateCnt + 1);
        }
        if (stateCnt == 5) {
            testBranchEvent(stateCnt + 1);
        }
        if (stateCnt == 6) {
            if (loadTestCounter == 0) {
                Log.d(TAG, "5. Branch event load test");
            }
            if (loadTestCounter < MAX_LOAD_CNT) {
                if (loadTestCounter % 5 == 0) {
                    if (!Branch.init().isTrackingDisabled()) {
                        Log.d(TAG, "-- Disabling tracking ");
                        disableTracking();
                    }
                } else {
                    if (Branch.init().isTrackingDisabled()) {
                        Log.d(TAG, "-- Enabling  tracking ");
                        enableTrackingAndProceed(6);
                    }
                }
                
                if (Branch.init().isTrackingDisabled()) {
                    Log.d(TAG, "-- test " + loadTestCounter + " <Tracking disabled>");
                    loadTestCounter++;
                    testBranchEvent(6);
                } else {
                    Log.d(TAG, "-- test " + loadTestCounter + " <Tracking Enabled>");
                    loadTestCounter++;
                    testBranchEvent(6);
                }
                
            } else {
                stateCnt++;
            }
        }
        
        if (stateCnt == 7) {
            Log.d(TAG, "All test passed");
        }
        if (stateCnt == TEST_FAILED) {
            Log.d(TAG, "All test failed");
            loadTestCounter = MAX_LOAD_CNT + 1;
        }
    }
    
    
    private boolean testLinkCreation(boolean disableTracking) {
        String syncLink = buo.setCanonicalIdentifier(UUID.randomUUID().toString()).getShortUrl(context, new LinkProperties(), true);
        Log.d(TAG, "-- Link created= " + syncLink);
        if (disableTracking) {
            return checkIsLongLink(syncLink);
        } else {
            return checkIsShortLink(syncLink);
        }
    }
    
    private void testBranchEvent(final int stateCnt) {
        Branch.init().setIdentity(UUID.randomUUID().toString(), new Branch.BranchReferralInitListener() {
            @Override
            public void onInitFinished(JSONObject referringParams, BranchError error) {
                boolean passed;
                
                if (Branch.init().isTrackingDisabled()) {
                    passed = error != null && error.getErrorCode() == BranchError.ERR_BRANCH_TRACKING_DISABLED;
                } else {
                    passed = (error == null || error.getErrorCode() != BranchError.ERR_BRANCH_TRACKING_DISABLED);
                }
                if (passed) {
                    Log.d(TAG, "-- Passed ");
                    runTrackingControlTest(stateCnt);
                } else {
                    Log.d(TAG, "-- failed ");
                    if (error != null) {
                        Log.d(TAG, " Error : " + error.getMessage());
                    }
                    runTrackingControlTest(TEST_FAILED);
                }
            }
        });
    }
    
    
    private void disableTracking() {
        Branch.init().disableTracking(true);
    }
    
    private void enableTrackingAndProceed(final int stateCnt) {
        Branch.init().disableTracking(false);
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runTrackingControlTest(stateCnt);
            }
        }, BRANCH_INIT_WAIT_TIME);
    }
    
    private static boolean checkIsShortLink(String link) {
        boolean isShortLink = true;
        try {
            URL url = new URL(link);
            if (!TextUtils.isEmpty(url.getQuery()) || !LINK_HOST.equalsIgnoreCase(url.getHost())) {
                isShortLink = false;
            }
        } catch (MalformedURLException ex) {
            isShortLink = false;
        }
        return isShortLink;
    }
    
    private static boolean checkIsLongLink(String link) {
        boolean isLongLink;
        try {
            URL url = new URL(link);
            isLongLink = !TextUtils.isEmpty(url.getQuery()) && url.getQuery().contains("data=");
        } catch (MalformedURLException ex) {
            isLongLink = false;
        }
        return isLongLink;
    }
    
    
    private void waitForBranchInitAndExecuteNext(final int testCnt) {
        Branch.sessionBuilder(null).withCallback(new Branch.BranchUniversalReferralInitListener() {
            @Override
            public void onInitFinished(BranchUniversalObject branchUniversalObject, LinkProperties linkProperties, BranchError error) {
                if (error != null) {
                    runTrackingControlTest(testCnt);
                }
            }
        }).init();
    }
    
}
