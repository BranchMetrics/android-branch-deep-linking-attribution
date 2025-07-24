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

    Branch.BranchNativeLinkShareListener nativeLinkShareListenerCallback_;

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
    void shareLink(@NonNull Activity activity, @NonNull BranchUniversalObject buo, @NonNull LinkProperties linkProperties, @Nullable Branch.BranchNativeLinkShareListener callback, String title, String subject) {

        nativeLinkShareListenerCallback_ = new NativeLinkShareListenerWrapper(callback, linkProperties, buo);

        try {
            buo.generateShortUrl(activity, linkProperties, new Branch.BranchLinkCreateListener() {
                @Override
                public void onLinkCreate(String url, BranchError error) {
                    if (error == null) {
                        SharingUtil.share(url, title, subject, activity);
                    } else {

                        if (callback != null) {
                            callback.onLinkShareResponse(url, error);
                        } else {
                            BranchLogger.v("Unable to share link " + error.getMessage());
                        }
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
            if (nativeLinkShareListenerCallback_ != null) {
                nativeLinkShareListenerCallback_.onLinkShareResponse(null, new BranchError("Trouble sharing link", BranchError.ERR_BRANCH_NO_SHARE_OPTION));
            } else {
                BranchLogger.v("Unable to share link. " + e.getMessage());
            }
        }
    }

    public Branch.BranchNativeLinkShareListener getLinkShareListenerCallback() {
        return nativeLinkShareListenerCallback_;
    }

    /**
     * Class for intercepting share sheet events to report auto events on BUO
     */
    private class NativeLinkShareListenerWrapper implements Branch.BranchNativeLinkShareListener {
        private final Branch.BranchNativeLinkShareListener branchNativeLinkShareListener_;
        private final BranchUniversalObject buo_;
        private String channelSelected_;

        NativeLinkShareListenerWrapper(Branch.BranchNativeLinkShareListener branchNativeLinkShareListener, LinkProperties linkProperties, BranchUniversalObject buo) {
            branchNativeLinkShareListener_ = branchNativeLinkShareListener;
            buo_ = buo;
            channelSelected_ = "";
        }

        @Override
        public void onLinkShareResponse(String sharedLink, BranchError error) {
            BranchEvent shareEvent = new BranchEvent(BRANCH_STANDARD_EVENT.SHARE);
            if (error == null) {
                shareEvent.addCustomDataProperty(Defines.Jsonkey.SharedLink.getKey(), sharedLink);
                shareEvent.addCustomDataProperty(Defines.Jsonkey.SharedChannel.getKey(), channelSelected_);
                shareEvent.addContentItems(buo_);
            } else {
                shareEvent.addCustomDataProperty(Defines.Jsonkey.ShareError.getKey(), error.getMessage());
            }

            shareEvent.logEvent(Branch.init().getApplicationContext());

            if (branchNativeLinkShareListener_ != null) {
                branchNativeLinkShareListener_.onLinkShareResponse(sharedLink, error);
            }
        }

        @Override
        public void onChannelSelected(String channelName) {
            channelSelected_ = channelName;
            if (branchNativeLinkShareListener_ != null) {
                branchNativeLinkShareListener_.onChannelSelected(channelName);
            }
        }
    }

}