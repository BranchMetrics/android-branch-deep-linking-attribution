package io.branch.branchandroiddemo;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import io.branch.referral.Branch;
import io.branch.referral.PrefHelper;

public class SettingsActivity extends Activity {
    Branch branch;
    EditText txtDeepKey;
    EditText txtDeepValue;
    EditText txtEditBranchKey;
    PrefHelper prefHelper;
    TextView txtDebugKeyValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefHelper = PrefHelper.getInstance(this);

        txtEditBranchKey = (EditText) findViewById(R.id.editBranchKey);
        txtDeepKey = (EditText) findViewById(R.id.editKey);
        txtDeepValue = (EditText) findViewById(R.id.editValue);
        txtDebugKeyValues = (TextView) findViewById(R.id.txtDebugKeyValues);

        if (!TextUtils.isEmpty(prefHelper.getBranchKey())) {
            txtEditBranchKey.setText(prefHelper.getBranchKey());
        }

        txtDeepValue.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    setDeepLinkDebugData();
                    handled = true;
                }
                return handled;
            }
        });

        findViewById(R.id.cmdSetBranchKey).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(txtEditBranchKey.getText().toString().trim())) {
                    prefHelper.setBranchKey(txtEditBranchKey.getText().toString().trim());
                    Toast.makeText(getApplicationContext(), "Saved Key", Toast.LENGTH_LONG).show();
                }
            }
        });

        findViewById(R.id.cmdSetDebugValue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDeepLinkDebugData();
            }
        });

        findViewById(R.id.cmdClearSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefHelper.setBranchKey(prefHelper.getBranchKey());
                txtEditBranchKey.setText(prefHelper.getBranchKey());
                Branch.getInstance().setDeepLinkDebugMode(null);
                txtDebugKeyValues.setText("");
                Toast.makeText(getApplicationContext(), "Cleared Settings", Toast.LENGTH_LONG).show();
            }
        });

        fillDebugParams();
    }

    private void setDeepLinkDebugData() {
        if (TextUtils.isEmpty(txtDeepKey.getText().toString().trim()) || TextUtils.isEmpty(txtDeepValue.getText().toString().trim())) {
            Toast.makeText(getApplicationContext(), "Add debug Key/Value failed -- empty string(s)", Toast.LENGTH_LONG).show();
            return;
        }

        JSONException jsonException = null;
        try {
            JSONObject debugObj = new JSONObject();
            debugObj.put(txtDeepKey.getText().toString().trim(), txtDeepValue.getText().toString().trim());

            //should we make appendDebugParams (Branch.java) public?
            JSONObject currentDebugParams = Branch.getInstance().getDeeplinkDebugParams();
            if (currentDebugParams != null) {
                currentDebugParams.put(txtDeepKey.getText().toString().trim(), txtDeepValue.getText().toString().trim());
                Branch.getInstance().setDeepLinkDebugMode(currentDebugParams);
            } else {
                Branch.getInstance().setDeepLinkDebugMode(debugObj);
            }

        } catch (JSONException ignore) {
            jsonException = ignore;
            Toast.makeText(getApplicationContext(), "Add debug Key/Value failed", Toast.LENGTH_LONG).show();
        } finally {
            if (jsonException == null) {
                String addText = txtDeepKey.getText().toString().trim() + " : " + txtDeepValue.getText().toString().trim() + "\n";
                txtDebugKeyValues.append(addText);
                txtDeepKey.setText("");
                txtDeepValue.setText("");
                txtDeepKey.requestFocus();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        branch = Branch.getInstance();
    }

    private void fillDebugParams() {
        JSONObject debugKeyValues = Branch.getInstance().getDeeplinkDebugParams();
        if (debugKeyValues != null) {
            for (Iterator<String> iter = debugKeyValues.keys(); iter.hasNext(); ) {
                String key = iter.next();
                try {
                    String value = (String) debugKeyValues.get(key);
                    if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                        String addText = key + " : " + value + "\n";
                        txtDebugKeyValues.append(addText);
                    }
                } catch (JSONException e) {

                }
            }
        }
    }
}
