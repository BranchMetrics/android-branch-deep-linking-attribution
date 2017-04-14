package io.branch.bookfinder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;


public class SplashActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bf_splash);

//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                startHomeActivity();
//            }
//        }, 1500);1500
    }

    private void startDetailsActivity(BranchUniversalObject bookObj) {
        Intent intent = new Intent(this, BookDetailsActivity.class);
        intent.putExtra(BookDetailsActivity.BOOK_EXTRA_KEY, bookObj);
        intent.putExtra(BookDetailsActivity.IS_DEEP_LINKED_LAUNCH, true);
        this.startActivity(intent);
        this.finish();
    }

    private void startHomeActivity() {
        Intent intent = new Intent(this, BookFinderHomeActivity.class);
        this.startActivity(intent);
        this.finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Branch.getInstance().initSession(new Branch.BranchUniversalReferralInitListener() {
            @Override
            public void onInitFinished(BranchUniversalObject bookObj, LinkProperties linkProperties, BranchError branchError) {
                if (bookObj == null) {
                    startHomeActivity();
                } else {
                    startDetailsActivity(bookObj);
                }

            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }


}
