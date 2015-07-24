package io.branch.branchandroiddemo;

import android.app.Activity;
import android.widget.TextView;

import org.json.JSONException;

import io.branch.referral.Branch;

/**
 * Created by sojanpr on 7/21/15.
 * <p> Activity to demonstrate  the auto deep linking functionality.
 * This activity is defined in the manifest with Keys for auto deep linking.
 * See manifest file for auto deep link configurations.</p>
 */
public class AutoDeepLinkTestActivity extends Activity {

    @Override
    protected void onResume() {
        super.onResume();
        setContentView(R.layout.auto_deep_link_test);

        TextView launch_mode_txt = (TextView) findViewById(R.id.launch_mode_txt);
        if (Branch.isAutoDeepLinkLaunch(this)) {
            try {
                String autoDeeplinkedValue = Branch.getInstance().getLatestReferringParams().getString("auto_deeplink_key_1");
                launch_mode_txt.setText("Launched by Branch on auto deep linking!"
                        + "\n\n" + autoDeeplinkedValue);
                Branch.getInstance().getLatestReferringParams();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else {
            launch_mode_txt.setText("Launched by normal application flow");
        }

    }
}
