package io.branch.testbed;

import io.branch.referral.Branch;
import io.branch.referral.Branch.BranchListResponseListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class CreditHistoryActivity extends Activity {
	
	private static SimpleDateFormat DateParseFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'");
	private static SimpleDateFormat DatePrintFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	private Branch branch;
	private ListView listview;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_credit_history);
		
		listview = (ListView) findViewById(R.id.list);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		final CreditHistoryActivity self = this;
		branch = Branch.getInstance(this.getApplicationContext());
		branch.initSession();
		branch.getCreditHistory(new BranchListResponseListener() {
			@SuppressLint("NewApi") public void onReceivingResponse(JSONArray history) {
				ArrayList<CreditTransaction> list = new ArrayList<CreditTransaction>();
				
				if (history.length() > 0) {
					Log.i("BranchTestBed", history.toString());
					
					try {
						for(int i = 0; i < history.length(); i++) {
							JSONObject transaction = history.getJSONObject(i);
							JSONObject xact = transaction.getJSONObject("transaction");
							String bucket = xact.getString("bucket");
							int amount = xact.getInt("amount");
							String date = xact.getString("date");
							Date xactDate = null;
							try {
								xactDate = DateParseFormat.parse(date);
							} catch (ParseException e) {
								e.printStackTrace();
							}
							list.add(new CreditTransaction(bucket + " : " + amount, transaction.isNull("referrer") ? null : transaction.getString("referrer"), xactDate));
						}
						
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else {
					list.add(new CreditTransaction("None found"));
				}
				
				CreditHistoryArrayAdaptor adapter = new CreditHistoryArrayAdaptor(self, list);
				listview.setAdapter(adapter);
			}
		});
	}

	@Override
	protected void onStop() {
		super.onStop();
		branch.closeSession();
	}
	
	private class CreditHistoryArrayAdaptor extends BaseAdapter {

		private ArrayList<CreditTransaction> listData;

		private LayoutInflater layoutInflater;

		public CreditHistoryArrayAdaptor(Context context, ArrayList<CreditTransaction> listData) {
			this.listData = listData;
			layoutInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return listData.size();
		}

		@Override
		public Object getItem(int position) {
			return listData.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = layoutInflater.inflate(R.layout.activity_credit_transaction, parent, false);
				holder = new ViewHolder();
				holder.transactionView = (TextView) convertView.findViewById(R.id.transaction);
				holder.referrerView = (TextView) convertView.findViewById(R.id.referrer);
				holder.dateView = (TextView) convertView.findViewById(R.id.date);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.transactionView.setText(((CreditTransaction)listData.get(position)).getTransaction());
			holder.referrerView.setText(((CreditTransaction)listData.get(position)).getReferrer());
			holder.dateView.setText(((CreditTransaction)listData.get(position)).getDate());

			return convertView;
		}

		private class ViewHolder {
			TextView transactionView;
			TextView referrerView;
			TextView dateView;
		}

	}
	
	private class CreditTransaction {
		private String transaction;
		private String referrer;
		private Date date;
		
		public CreditTransaction(String bucket) {
			this(bucket, null, null);
		}
		
		public CreditTransaction(String transaction, String referrer, Date date) {
			this.transaction = transaction;
			this.referrer = referrer;
			this.date = date;
		}
		
		public String getTransaction() {
			return this.transaction; 
		}
		
		public String getReferrer() {
			return this.referrer != null ? "(" + this.referrer + ")" : ""; 
		}
		
		public String getDate() {
			return this.date != null ? DatePrintFormat.format(this.date) : "";
		}
	}

}