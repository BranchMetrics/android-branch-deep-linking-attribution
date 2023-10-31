package io.branch.referral;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.SharingUtil;

public class NativeShareLinkManager {

    /* Current activity context.*/
    Context context_;
    Branch.BranchLinkShareListener linkShareListenerCallback_;
    Branch.IChannelProperties channelPropertiesCallback_;
    SharingUtil sharingUtility_ = new SharingUtil();
    
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    void shareLink(@NonNull Activity activity, String url, @NonNull LinkProperties linkProperties, @Nullable Branch.BranchLinkShareListener callback, @Nullable Branch.IChannelProperties channelProperties) {

        context_ = activity;
        linkShareListenerCallback_ = new LinkShareListenerWrapper(callback, linkProperties);
        channelPropertiesCallback_ = channelProperties;

        try {
            sharingUtility_.share(url,"test", activity);
            if (linkShareListenerCallback_ != null) {
                linkShareListenerCallback_.onShareLinkDialogLaunched();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (linkShareListenerCallback_ != null) {
                linkShareListenerCallback_.onLinkShareResponse(null, null, new BranchError("Trouble sharing link", BranchError.ERR_BRANCH_NO_SHARE_OPTION));
            } else {
                BranchLogger.v("Unable create share options. Couldn't find applications on device to share the link.");
            }
        }
    }

        /**
         * Class for intercepting share sheet events to report auto events on BUO
         */
        private class LinkShareListenerWrapper implements Branch.BranchLinkShareListener {
            private final Branch.BranchLinkShareListener originalCallback_;
            private final LinkProperties linkProperties_;

            LinkShareListenerWrapper(Branch.BranchLinkShareListener originalCallback, LinkProperties linkProperties) {
                originalCallback_ = originalCallback;
                linkProperties_ = linkProperties;
            }

            @Override
            public void onShareLinkDialogLaunched() {
                if (originalCallback_ != null) {
                    originalCallback_.onShareLinkDialogLaunched();
                }
            }

            @Override
            public void onShareLinkDialogDismissed() {
                if (originalCallback_ != null) {
                    originalCallback_.onShareLinkDialogDismissed();
                }
            }

            @Override
            public void onLinkShareResponse(String sharedLink, String sharedChannel, BranchError error) {
                BranchEvent shareEvent = new BranchEvent(BRANCH_STANDARD_EVENT.SHARE);
                if (error == null) {
                    shareEvent.addCustomDataProperty(Defines.Jsonkey.SharedLink.getKey(), sharedLink);
                    shareEvent.addCustomDataProperty(Defines.Jsonkey.SharedChannel.getKey(), sharedChannel);
            //Nd        shareEvent.addContentItems(BranchUniversalObject.this);
                } else {
                    shareEvent.addCustomDataProperty(Defines.Jsonkey.ShareError.getKey(), error.getMessage());
                }

                shareEvent.logEvent(Branch.getInstance().getApplicationContext());

                if (originalCallback_ != null) {
                    originalCallback_.onLinkShareResponse(sharedLink, sharedChannel, error);
                }
            }

            @Override
            public void onChannelSelected(String channelName) {
                if (originalCallback_ != null) {
                    originalCallback_.onChannelSelected(channelName);
                }
                if (originalCallback_ instanceof Branch.ExtendedBranchLinkShareListener) {
                   // if (((Branch.ExtendedBranchLinkShareListener) originalCallback_).onChannelSelected(channelName, BranchUniversalObject.this, linkProperties_)) {
                 //ND       shareSheetBuilder_.setShortLinkBuilderInternal(getLinkBuilder(shareSheetBuilder_.getShortLinkBuilder(), linkProperties_));
                   // }
                }
            }
        }

}
