package io.branch.testbed;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.Branch.BranchLinkCreateListener;
import io.branch.referral.Branch.BranchReferralInitListener;
import io.branch.referral.Branch.BranchReferralStateChangedListener;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
	Branch branch;
	
	EditText txtUrl;
	EditText txtShortUrl;
	Button cmdRefreshUrl;
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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		txtUrl = (EditText) findViewById(R.id.editReferralUrl);
		txtShortUrl = (EditText) findViewById(R.id.editReferralShortUrl);
		cmdRefreshUrl = (Button) findViewById(R.id.cmdRefreshURL);
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
		
		cmdIdentifyUser.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				branch.identifyUser("my_great_user", new BranchReferralInitListener() {
					@Override
					public void onInitFinished(JSONObject referringParams) {
						Log.i("BranchTestBed", "install params = " + referringParams.toString());
					}
				});
			}
		});
		
		cmdLogoutUser.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				branch.clearUser();
			}
		});
		
		cmdPrintInstallParams.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				JSONObject obj = branch.getInstallReferringParams();
				Log.i("BranchTestBed", "install params = " + obj.toString());
			}
		});
		
		cmdRefreshUrl.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				JSONObject obj = new JSONObject();
				try {
					obj.put("name", "test name");
					obj.put("message", "hello there with long url");
				} catch (JSONException ex) {
					ex.printStackTrace();
				}
				String url = branch.getLongURL("test_tag", obj);
				txtUrl.setText(url);
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
				branch.getShortUrl("tag", obj, new BranchLinkCreateListener() {
					@Override
					public void onLinkCreate(String url) {
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
					public void onStateChanged(boolean changed) {
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
					public void onStateChanged(boolean changed) {
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
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		branch = Branch.getInstance(this.getApplicationContext(), "5680621892404085");
		branch.initUserSession(new BranchReferralInitListener() {
			@Override
			public void onInitFinished(JSONObject referringParams) {
				Log.i("BranchTestBed", "branch init complete!");
				JSONObject params = branch.getReferringParams();
				try {
					Iterator<?> keys = params.keys();
					while (keys.hasNext()) {
						String key = (String) keys.next();
						Log.i("BranchTestBed", key + ", " + referringParams.getString(key));
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}, this.getIntent().getData());
	}


	@Override
	protected void onStop() {
		super.onStop();
		branch.closeSession();
	}

}
