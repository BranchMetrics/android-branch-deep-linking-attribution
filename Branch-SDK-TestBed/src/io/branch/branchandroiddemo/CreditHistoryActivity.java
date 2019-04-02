package io.branch.branchandroiddemo;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import io.branch.referral.Branch;
import io.branch.referral.Branch.BranchListResponseListener;
import io.branch.referral.BranchError;

public class CreditHistoryActivity extends Activity {

    private static SimpleDateFormat DateParseFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'", Locale.US);
    private static SimpleDateFormat DatePrintFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.US);
    private ListView listview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit_history);

        listview = findViewById(R.id.list);
    }

    @Override
    protected void onStart() {
        super.onStart();

        final CreditHistoryActivity self = this;
        Branch branch = Branch.getInstance();

        branch.getCreditHistory(new BranchListResponseListener() {

            public void onReceivingResponse(JSONArray history, BranchError error) {
                ArrayList<CreditTransaction> list = new ArrayList<>();
                if (error != null) {
                    Log.i("BranchTestBed", "branch load credit history failed. Caused by -" + error.getMessage());
                } else {
                    if (history.length() > 0) {
                        Log.i("BranchTestBed", history.toString());

                        try {
                            for (int i = 0; i < history.length(); i++) {
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
                                list.add(new CreditTransaction(bucket + " : " + amount,
                                        transaction.isNull("referrer") ? null : transaction.getString("referrer"),
                                        transaction.isNull("referree") ? null : transaction.getString("referree"),
                                        xactDate));
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
            }
        });

    }

    private class CreditHistoryArrayAdaptor extends BaseAdapter {

        private ArrayList<CreditTransaction> listData;

        private LayoutInflater layoutInflater;

        CreditHistoryArrayAdaptor(Context context, ArrayList<CreditTransaction> listData) {
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
                holder.transactionView = convertView.findViewById(R.id.transaction);
                holder.referrerView = convertView.findViewById(R.id.referrer);
                holder.dateView = convertView.findViewById(R.id.date);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.transactionView.setText((listData.get(position)).getTransaction());
            holder.referrerView.setText((listData.get(position)).getReferInfo());
            holder.dateView.setText((listData.get(position)).getDate());

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
        private String referree;
        private Date date;

        CreditTransaction(String bucket) {
            this(bucket, null, null, null);
        }

        CreditTransaction(String transaction, String referrer, String referree, Date date) {
            this.transaction = transaction;
            this.referrer = referrer;
            this.referree = referree;
            this.date = date;
        }

        String getTransaction() {
            return this.transaction;
        }

        String getReferInfo() {
            StringBuilder sb = new StringBuilder();
            if (this.referrer != null || this.referree != null) {
                boolean hasReferrer = false;
                sb.append("(");
                if (this.referrer != null) {
                    hasReferrer = true;
                    sb.append("referrer: ");
                    sb.append(this.referrer);
                }
                if (this.referree != null) {
                    if (hasReferrer) {
                        sb.append(" -> ");
                    }
                    sb.append("referree: ");
                    sb.append(this.referree);
                }
                sb.append(")");
            }
            return sb.toString();
        }

        String getDate() {
            return this.date != null ? DatePrintFormat.format(this.date) : "";
        }
    }

}
