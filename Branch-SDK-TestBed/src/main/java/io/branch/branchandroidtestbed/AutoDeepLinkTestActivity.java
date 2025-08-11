package io.branch.branchandroidtestbed;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

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

        TextView launch_mode_txt = findViewById(R.id.launch_mode_txt);
        if (false) {
            launch_mode_txt.setText(R.string.launch_mode_branch);
            Branch.getInstance().getLatestReferringParams();
        } else {
            launch_mode_txt.setText(R.string.launch_mode_normal);
        }

        //You can also get linked params for the intent extra.
        if (getIntent().getExtras() != null && getIntent().getExtras().keySet() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Log.i("BranchTestBed:", "Deep Linked Param " +
                        key + " = " + getIntent().getExtras().getString(key));
            }
        }

    }
}
