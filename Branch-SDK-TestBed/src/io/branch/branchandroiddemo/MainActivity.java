package io.branch.branchandroiddemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Iterator;

import io.branch.referral.Branch;
import io.branch.referral.Branch.BranchReferralInitListener;
import io.branch.referral.Branch.BranchReferralStateChangedListener;
import io.branch.referral.BranchError;
import io.branch.referral.SharingHelper;
import io.branch.referral.indexing.BranchUniversalObject;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.ShareSheetStyle;

public class MainActivity extends Activity {
    Branch branch;

    public enum SESSION_MANAGEMENT_MODE {
        AUTO,    /* Branch SDK Manages the session for you. For this mode minimum API level should
                 be 14 or above. Make sure to instantiate {@link BranchApp} class to use this mode. */

        MANUAL  /* You are responsible for managing the session. Need to call initialiseSession() and
                closeSession() on activity onStart() and onStop() respectively. */
    }

    /* Current mode for the Session Management */
    public static SESSION_MANAGEMENT_MODE sessionMode = SESSION_MANAGEMENT_MODE.AUTO;

    EditText txtShortUrl;
    Button cmdRefreshShortUrl;
    TextView txtInstallCount;
    TextView txtRewardBalance;
    TextView txtEventCount;
    Button cmdRefreshCounts;
    Button cmdRedeemFive;
    Button cmdRefreshReward;
    Button cmdCommitBuy;
    Button cmdCommitBuyMetadata;
    Button cmdIdentifyUser;
    Button cmdLogoutUser;
    Button cmdPrintInstallParams;
    Button cmdGetCreditHistory;
    Button cmdReferralCode;

    BranchUniversalObject branchUniversalObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtShortUrl = (EditText) findViewById(R.id.editReferralShortUrl);
        cmdRefreshShortUrl = (Button) findViewById(R.id.cmdRefreshShortURL);
        txtInstallCount = (TextView) findViewById(R.id.txtInstallCount);
        txtEventCount = (TextView) findViewById(R.id.txtEventCount);
        txtRewardBalance = (TextView) findViewById(R.id.txtRewardBalance);
        cmdRefreshCounts = (Button) findViewById(R.id.cmdRefreshCounts);
        cmdRedeemFive = (Button) findViewById(R.id.cmdRedeemFive);
        cmdRefreshReward = (Button) findViewById(R.id.cmdRefreshReward);
        cmdCommitBuy = (Button) findViewById(R.id.cmdCommitBuyAction);
        cmdIdentifyUser = (Button) findViewById(R.id.cmdIdentifyUser);
        cmdLogoutUser = (Button) findViewById(R.id.cmdClearUser);
        cmdPrintInstallParams = (Button) findViewById(R.id.cmdPrintInstallParam);
        cmdCommitBuyMetadata = (Button) findViewById(R.id.cmdCommitBuyMetadataAction);
        cmdGetCreditHistory = (Button) findViewById(R.id.cmdGetCreditHistory);
        cmdReferralCode = (Button) findViewById(R.id.cmdReferralCode);

        cmdIdentifyUser.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                branch.setIdentity("test_user_10", new BranchReferralInitListener() {
                    @Override
                    public void onInitFinished(JSONObject referringParams, BranchError error) {
                        if (error != null) {
                            Log.i("BranchTestBed", "branch set Identity failed. Caused by -" + error.getMessage());
                        } else {
                            Log.i("BranchTestBed", "install params = " + referringParams.toString());
                        }
                    }
                });
            }
        });

        cmdLogoutUser.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                branch.logout();

                txtRewardBalance.setText("rewards = ");
                txtInstallCount.setText("install count =");
                txtEventCount.setText("buy count =");
            }
        });

        cmdPrintInstallParams.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject obj = branch.getFirstReferringParams();
                Log.i("BranchTestBed", "install params = " + obj.toString());
            }
        });

        cmdRefreshShortUrl.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {

                LinkProperties linkProperties = new LinkProperties()
                        .addTag("myTag1")
                        .addTag("myTag2")
                        .setAlias("myContentName")
                        .setChannel("myChannel2")
                        .setFeature("Myfeature2")
                        .setStage("10")
                        .addControlParameter("Name", "MyUserName1")
                        .addControlParameter("Message", "My Custom message")
                        .setDuration(100);

                txtShortUrl.setText(branchUniversalObject.getShortUrl(MainActivity.this, linkProperties));
            }


        });

        cmdRefreshCounts.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                branch.loadActionCounts(new BranchReferralStateChangedListener() {
                    @Override
                    public void onStateChanged(boolean changed, BranchError error) {
                        if (error != null) {
                            Log.i("BranchTestBed", "branch load action count failed. Caused by -" + error.getMessage());
                        } else {
                            Log.i("BranchTestBed", "changed = " + changed);
                            txtInstallCount.setText("install total = " + branch.getTotalCountsForAction("install") + ", unique = " + branch.getUniqueCountsForAction("install"));
                            txtEventCount.setText("buy total = " + branch.getTotalCountsForAction("buy") + ", unique = " + branch.getUniqueCountsForAction("buy"));
                        }
                    }
                });
            }
        });

        cmdRefreshReward.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                branch.loadRewards(new BranchReferralStateChangedListener() {
                    @Override
                    public void onStateChanged(boolean changed, BranchError error) {
                        if (error != null) {
                            Log.i("BranchTestBed", "branch load rewards failed. Caused by -" + error.getMessage());
                        } else {
                            Log.i("BranchTestBed", "changed = " + changed);
                            txtRewardBalance.setText("rewards = " + branch.getCredits());
                        }
                    }
                });
            }
        });

        cmdRedeemFive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                branch.redeemRewards(5, new BranchReferralStateChangedListener() {
                    @Override
                    public void onStateChanged(boolean changed, BranchError error) {
                        if (error != null) {
                            Log.i("BranchTestBed", "branch redeem rewards failed. Caused by -" + error.getMessage());
                        } else {
                            if (changed) {
                                Log.i("BranchTestBed", "redeemed rewards = " + changed);
                                txtRewardBalance.setText("rewards = " + branch.getCredits());
                            } else {
                                Log.i("BranchTestBed", "redeem rewards error : " + error);
                            }
                        }
                    }
                });
            }
        });

        cmdCommitBuy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                branch.userCompletedAction("buy");
            }
        });

        cmdCommitBuyMetadata.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                JSONObject params = new JSONObject();
                try {
                    params.put("name", "Alex");
                    params.put("boolean", true);
                    params.put("int", 1);
                    params.put("double", 0.13415512301);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                branch.userCompletedAction("buy", params);
            }

        });

        cmdGetCreditHistory.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("BranchTestBed", "Getting credit history...");
                Intent i = new Intent(getApplicationContext(), CreditHistoryActivity.class);
                startActivity(i);
            }
        });

        cmdReferralCode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("BranchTestBed", "Navigating to Referral Code...");
                Intent i = new Intent(getApplicationContext(), ReferralCodeActivity.class);
                startActivity(i);
            }
        });

        findViewById(R.id.share_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject obj = new JSONObject();
                LinkProperties linkProperties = new LinkProperties()
                        .addTag("myShareTag1")
                        .addTag("myShareTag2")
                        .setAlias("mySahreContentName")
                        .setChannel("myShareChannel2")
                        .setFeature("mySharefeature2")
                        .setStage("10")
                        .addControlParameter("Name", "MyUserName1")
                        .addControlParameter("Message", "My Custom message")
                        .setDuration(100);

                ShareSheetStyle shareSheetStyle = new ShareSheetStyle(MainActivity.this, "My Sharing Message Title", "My Sharing message body")
                        .setCopyUrlStyle(getResources().getDrawable(android.R.drawable.ic_menu_send),"Save this URl","Link added to clipboard")
                        .setMoreOptionStyle(getResources().getDrawable(android.R.drawable.ic_menu_search), "Show more")
                        .addPreferredSharingOption(SharingHelper.SHARE_WITH.FACEBOOK)
                        .addPreferredSharingOption(SharingHelper.SHARE_WITH.EMAIL)
                        .addPreferredSharingOption(SharingHelper.SHARE_WITH.MESSAGE)
                        .addPreferredSharingOption(SharingHelper.SHARE_WITH.TWITTER);

                branchUniversalObject.showShareSheet(MainActivity.this, linkProperties, shareSheetStyle, new Branch.BranchLinkShareListener() {
                    @Override
                    public void onShareLinkDialogLaunched() {
                    }
                    @Override
                    public void onShareLinkDialogDismissed() {
                    }
                    @Override
                    public void onLinkShareResponse(String sharedLink, String sharedChannel, BranchError error) {
                    }
                    @Override
                    public void onChannelSelected(String channelName) {
                    }
                });
            }
        });

        branchUniversalObject = new BranchUniversalObject("canonical/identifier/", "My Content Title")
                .setContentDescription("My Content Description ")
                .setContentImageUrl("https://contents/mycontent.png")
                .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PRIVATE)
                .setContentType("application/vnd.businessobjects")
                .setContentExpiration(new Date(2015, 11, 12))
                .addKeyWord("My Key1")
                .addKeyWord("My Key2")

                .addContentMetaData("name", "test name")
                .addContentMetaData("auto_deeplink_key_1", "This is an auto deep linked value")
                .addContentMetaData("message", "hello there with short url");


    }

    @Override
    protected void onStart() {
        super.onStart();

        if (sessionMode != SESSION_MANAGEMENT_MODE.AUTO) {
            branch = Branch.getInstance(this);
        } else {
            branch = Branch.getInstance();
        }
        branch.setDebug();
        //branch.disableTouchDebugging();

        branch.initSession(new BranchReferralInitListener() {
            @Override
            public void onInitFinished(JSONObject referringParams,
                                       BranchError error) {
                if (error != null) {
                    Log.i("BranchTestBed", "branch init failed. Caused by -" + error.getMessage());
                } else {
                    Log.i("BranchTestBed", "branch init complete!");
                    try {
                        Iterator<?> keys = referringParams.keys();
                        while (keys.hasNext()) {
                            String key = (String) keys.next();
                            String value = referringParams.getString(key);
                            Log.i("BranchTestBed", key + ", " + value);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, this.getIntent().getData(), this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        branchUniversalObject.markAsViewed();
    }

    @Override
    public void onNewIntent(Intent intent) {
        this.setIntent(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (sessionMode != SESSION_MANAGEMENT_MODE.AUTO) {
            branch.closeSession();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Checking if the previous activity is launched on branch Auto deep link.
        if(requestCode == getResources().getInteger(R.integer.AutoDeeplinkRequestCode)){
            //Decide here where  to navigate  when an auto deep linked activity finishes.
            //For e.g. Go to HomeActivity or a  SignUp Activity.
            Intent i = new Intent(getApplicationContext(), CreditHistoryActivity.class);
            startActivity(i);

        }
    }
}
