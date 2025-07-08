package io.branch.referral;

import android.app.Activity;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.PrintWriter;
import java.io.StringWriter;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.SharingUtil;

public class NativeShareLinkManager {
    private static volatile NativeShareLinkManager INSTANCE = null;



    private NativeShareLinkManager() {
    }

    public static NativeShareLinkManager getInstance() {
        if (INSTANCE == null) {
            // synchronize the block to ensure only one thread can execute at a time
            synchronized (NativeShareLinkManager.class) {
                // check again if the instance is already created
                if (INSTANCE == null) {
                    INSTANCE = new NativeShareLinkManager();
                }
            }
        }
        return INSTANCE;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    void shareLink(@NonNull Activity activity, @NonNull BranchUniversalObject buo, @NonNull LinkProperties linkProperties, String title, String subject) {



        try {
            buo.generateShortUrl(activity, linkProperties, new Branch.BranchLinkCreateListener() {
                @Override
                public void onLinkCreate(String url, BranchError error) {
                    if (error == null) {
                        SharingUtil.share(url, title, subject, activity);
                    } else {

                        BranchLogger.v("Unable to share link " + error.getMessage());
                        if (error.getErrorCode() == BranchError.ERR_BRANCH_NO_CONNECTIVITY
                                || error.getErrorCode() == BranchError.ERR_BRANCH_TRACKING_DISABLED) {
                            SharingUtil.share(url, title, subject, activity);
                        }
                    }
                }
            });

        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            BranchLogger.e(errors.toString());
            BranchLogger.v("Unable to share link. " + e.getMessage());
        }
    }





}
