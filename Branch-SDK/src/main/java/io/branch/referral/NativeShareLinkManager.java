package io.branch.referral;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.SharingUtil;

public class NativeShareLinkManager {

    /* Current activity context.*/
    Context context_;
    Branch.BranchNativeLinkShareListener linkShareListenerCallback_;
    //SharingUtil sharingUtility_ = new SharingUtil();

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
    void shareLink(@NonNull Activity activity, @NonNull BranchUniversalObject buo, @NonNull LinkProperties linkProperties, @Nullable Branch.BranchNativeLinkShareListener callback) {

        context_ = activity;
        linkShareListenerCallback_ = new LinkShareListenerWrapper(callback, linkProperties,buo);

        try {
            buo.generateShortUrl(activity, linkProperties, new Branch.BranchLinkCreateListener() {
                @Override
                public void onLinkCreate(String url, BranchError error) {
                    if (error == null) {
                        SharingUtil.share(url,"", activity);
                    } else {

                        if (callback != null) {
                            callback.onLinkShareError(url, error);
                        } else {
                            BranchLogger.v("Unable to share link " + error.getMessage());
                        }
                        if (error.getErrorCode() == BranchError.ERR_BRANCH_NO_CONNECTIVITY
                                || error.getErrorCode() == BranchError.ERR_BRANCH_TRACKING_DISABLED) {
                            SharingUtil.share(url, "", activity);
                        }
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            if (linkShareListenerCallback_ != null) {
                linkShareListenerCallback_.onLinkShareError(null, new BranchError("Trouble sharing link", BranchError.ERR_BRANCH_NO_SHARE_OPTION));
            } else {
                BranchLogger.v("Unable create share options. Couldn't find applications on device to share the link.");
            }
        }
    }

    /**
     * Class for intercepting share sheet events to report auto events on BUO
     */
    private class LinkShareListenerWrapper implements Branch.BranchNativeLinkShareListener {
        private final Branch.BranchNativeLinkShareListener originalCallback_;
        private final LinkProperties linkProperties_;
        private final BranchUniversalObject buo_;

        LinkShareListenerWrapper(Branch.BranchNativeLinkShareListener originalCallback, LinkProperties linkProperties, BranchUniversalObject buo) {
            originalCallback_ = originalCallback;
            linkProperties_ = linkProperties;
            buo_ = buo;
        }

        @Override
        public void onLinkShareError(String sharedLink, BranchError error) {
            BranchEvent shareEvent = new BranchEvent(BRANCH_STANDARD_EVENT.SHARE);
            if (error == null) {
                shareEvent.addCustomDataProperty(Defines.Jsonkey.SharedLink.getKey(), sharedLink);
                shareEvent.addContentItems(buo_);
            } else {
                shareEvent.addCustomDataProperty(Defines.Jsonkey.ShareError.getKey(), error.getMessage());
            }

            shareEvent.logEvent(Branch.getInstance().getApplicationContext());

            if (originalCallback_ != null) {
                originalCallback_.onLinkShareError(sharedLink, error);
            }
        }

        @Override
        public void onChannelSelected(String channelName) {
            if (originalCallback_ != null) {
                originalCallback_.onChannelSelected(channelName);
            }
            if (originalCallback_ instanceof Branch.ExtendedBranchNativeLinkShareListener) {
                 if (((Branch.ExtendedBranchNativeLinkShareListener) originalCallback_).onChannelSelected(channelName, buo_, linkProperties_)) {
                   //    shareSheetBuilder_.setShortLinkBuilderInternal(getLinkBuilder(shareSheetBuilder_.getShortLinkBuilder(), linkProperties_));
                 }
            }
        }
    }

}
