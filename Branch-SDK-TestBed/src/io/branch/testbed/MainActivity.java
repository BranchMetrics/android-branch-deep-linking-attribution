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
	TextView txtInstallCreditCount;
	TextView txtEventCount;
	TextView txtEventCreditCount;
	Button cmdRefreshCounts;
	Button cmdCreditInstall;
	Button cmdCreditBuy;
	Button cmdCommitBuy;
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
		txtInstallCreditCount = (TextView) findViewById(R.id.txtInstallCreditCount);
		txtEventCreditCount = (TextView) findViewById(R.id.txtEventCreditCount);
		cmdRefreshCounts = (Button) findViewById(R.id.cmdRefreshCounts);
		cmdCreditInstall = (Button) findViewById(R.id.cmdCreditInstall);
		cmdCreditBuy = (Button) findViewById(R.id.cmdCreditBuy);
		cmdCommitBuy = (Button) findViewById(R.id.cmdCommitBuyAction);
		cmdIdentifyUser = (Button) findViewById(R.id.cmdIdentifyUser);
		cmdLogoutUser = (Button) findViewById(R.id.cmdClearUser);
		cmdPrintInstallParams = (Button) findViewById(R.id.cmdPrintInstallParam);
		
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
				branch.loadPoints(new BranchReferralStateChangedListener() {
					@Override
					public void onStateChanged(boolean changed) {
						Log.i("BranchTestBed", "changed = " + changed);
						txtInstallCount.setText("install count = " + branch.getTotal("install"));
						txtEventCount.setText("buy event count = " + branch.getTotal("buy"));
						txtInstallCreditCount.setText("install credit count = " + branch.getCredit("install"));
						txtEventCreditCount.setText("buy event credit count = " + branch.getCredit("buy"));
					}
				});
			}
		});
		
		cmdCreditInstall.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				branch.creditUserForReferral("install", 1);
			}
		});
		
		cmdCreditBuy.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				branch.creditUserForReferral("buy", 1);
			}
		});
		
		cmdCommitBuy.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				branch.userCompletedAction("buy");
			}
		});
		
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		branch= Branch.getInstance(this.getApplicationContext(), "5680621892404085");
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
		});
	}



}
