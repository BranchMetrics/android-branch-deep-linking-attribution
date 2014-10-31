package io.branch.testbed;

import io.branch.referral.Branch;
import io.branch.referral.Branch.BranchListResponseListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class CreditHistoryActivity extends Activity {
	Branch branch;
	TableLayout table_layout;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_credit_history);
		
		table_layout = (TableLayout)findViewById(R.id.credit_history_table);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		table_layout.removeAllViews();
		
		final CreditHistoryActivity self = this;
		branch = Branch.getInstance(this.getApplicationContext());
		branch.initUserSession();
		branch.getCreditHistory(new BranchListResponseListener() {
			@SuppressLint("NewApi") public void onReceivingResponse(JSONArray history) {
				if (history.length() > 0) {
					Log.i("BranchTestBed", history.toString());
					try {
						for(int i = 0; i < history.length(); i++) {
							TableRow row = new TableRow(self);
							TextView tv = new TextView(self);
							JSONObject transaction = history.getJSONObject(i);
							JSONObject xact = transaction.getJSONObject("transaction");
							String bucket = xact.getString("bucket");
							int amount = xact.getInt("amount");
							String date = xact.getString("date");
							
							row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
							tv.setPadding(25, 15, 25, 10);
							tv.setText(bucket + " : " + amount + (transaction.isNull("referrer") ? "" : transaction.getString("referrer")) + "\n" + date);

							row.addView(tv);
							table_layout.addView(row);
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else {
					TableRow row = new TableRow(self);
					TextView tv = new TextView(self);
					row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
					tv.setPadding(25, 15, 25, 10);
					tv.setText("None found");
					row.addView(tv);
					table_layout.addView(row);
				}
			}
		});
	}


	@Override
	protected void onStop() {
		super.onStop();
		branch.closeSession();
	}

}
