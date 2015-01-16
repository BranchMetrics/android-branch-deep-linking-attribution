package io.branch.testbed;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.Branch.BranchLinkCreateListener;
import io.branch.referral.Branch.BranchReferralInitListener;
import io.branch.referral.Branch.BranchReferralStateChangedListener;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
	Branch branch;
	
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
				branch.setIdentity("my_great_user", new BranchReferralInitListener() {
					@Override
					public void onInitFinished(JSONObject referringParams, BranchError error) {
						Log.i("BranchTestBed", "install params = " + referringParams.toString());
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
				JSONObject obj = new JSONObject();
				try {
					obj.put("name", "test name");
					obj.put("message", "hello there with short url");
				} catch (JSONException ex) {
					ex.printStackTrace();
				}
				ArrayList<String> tags = new ArrayList<String>();
				tags.add("tag1");
				tags.add("tag2");
				branch.getShortUrl(tags, "channel1", "feature1", "1", obj, new BranchLinkCreateListener() {
					@Override
					public void onLinkCreate(String url, BranchError error) {
						txtShortUrl.setText(url);
					}
				});
			}
		});
		
		cmdRefreshCounts.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				branch.loadActionCounts(new BranchReferralStateChangedListener() {
					@Override
					public void onStateChanged(boolean changed, BranchError error) {
						Log.i("BranchTestBed", "changed = " + changed);
						txtInstallCount.setText("install total = " + branch.getTotalCountsForAction("install") + ", unique = " + branch.getUniqueCountsForAction("install"));
						txtEventCount.setText("buy total = " + branch.getTotalCountsForAction("buy") + ", unique = " + branch.getUniqueCountsForAction("buy"));
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
						Log.i("BranchTestBed", "changed = " + changed);
						txtRewardBalance.setText("rewards = " + branch.getCredits());
					}
				});
			}
		});
		
		cmdRedeemFive.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				branch.redeemRewards(5);
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
				} catch(JSONException e) {
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
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		branch = Branch.getInstance(this.getApplicationContext(), "5680621892404085");
		branch.setDebug();
		branch.initSession(new BranchReferralInitListener() {
			@Override
			public void onInitFinished(JSONObject referringParams, BranchError error) {
				Log.i("BranchTestBed", "branch init complete!");
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
		}, this.getIntent().getData(), this);
	}


	@Override
	protected void onStop() {
		super.onStop();
		branch.closeSession();
	}

}
