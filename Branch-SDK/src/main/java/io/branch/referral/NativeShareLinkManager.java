package io.branch.referral;

import static androidx.core.content.ContextCompat.startActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.LinkProperties;

public class NativeShareLinkManager {

    Branch.BranchLinkShareListener callback_;
    Branch.IChannelProperties channelPropertiesCallback_;
    private Intent shareLinkIntent_;

  /* Current activity context.*/
    Context context_;
    
    void shareLink(@NonNull Activity activity, String url, @NonNull LinkProperties linkProperties, @Nullable Branch.BranchLinkShareListener callback) {

        context_ = activity;
        callback_ = new LinkShareListenerWrapper(callback, linkProperties);
     // ND - TODO   channelPropertiesCallback_ = builder.getChannelPropertiesCallback();
        shareLinkIntent_ = new Intent(Intent.ACTION_SEND);
        shareLinkIntent_.setType("text/plain");
        shareLinkIntent_.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL");
        shareLinkIntent_.putExtra(Intent.EXTRA_TEXT, url);

        try {
            Intent shareIntent = Intent.createChooser(shareLinkIntent_, "Title - Nidhi");
            startActivity(activity, shareIntent, null);
        } catch (Exception e) {
            e.printStackTrace();
            if (callback_ != null) {
                callback_.onLinkShareResponse(null, null, new BranchError("Trouble sharing link", BranchError.ERR_BRANCH_NO_SHARE_OPTION));
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
