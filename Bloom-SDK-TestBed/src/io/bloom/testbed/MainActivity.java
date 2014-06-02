package io.bloom.testbed;

import io.bloom.referral.Bloom;
import io.bloom.referral.Bloom.BloomReferralStateChanged;
import io.bloom.testbed.R;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	EditText txtUrl;
	Button cmdRefreshUrl;
	TextView txtInstallCount;
	TextView txtInstallCreditCount;
	TextView txtEventCount;
	TextView txtEventCreditCount;
	Button cmdRefreshCounts;
	Button cmdCreditInstall;
	Button cmdCreditBuy;
	Button cmdCommitBuy;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		txtUrl = (EditText) findViewById(R.id.editReferralUrl);
		cmdRefreshUrl = (Button) findViewById(R.id.cmdRefreshURL);
		txtInstallCount = (TextView) findViewById(R.id.txtInstallCount);
		txtEventCount = (TextView) findViewById(R.id.txtEventCount);
		cmdRefreshCounts = (Button) findViewById(R.id.cmdRefreshCounts);
		cmdCreditInstall = (Button) findViewById(R.id.cmdCreditInstall);
		cmdCreditBuy = (Button) findViewById(R.id.cmdCreditBuy);
		cmdCommitBuy = (Button) findViewById(R.id.cmdCommitBuyAction);
		
		cmdRefreshUrl.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				txtUrl.setText(Bloom.getReferralURL());
			}
		});
		
		cmdRefreshCounts.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
			}
		});
		
		cmdCreditInstall.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
			}
		});
		
		cmdCreditBuy.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
			}
		});
		
		cmdCommitBuy.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
			}
		});
		
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Bloom.startSession(this.getApplicationContext(), "fake_key");
		Bloom.setStateChangedCallback(new BloomReferralStateChanged() {
			@Override
			public void onStateChanged() {
				Log.i("BloomTestBed", "Bloom test bed on update called");
			}
		});
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Bloom.stopSession();
	}


}
