package io.branch.referral;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>Class that provides a chooser dialog with customised share options to share a link.
 * Class provides customised and easy way of sharing a deep link with other applications. </p>
 */
class ShareLinkManager {
    /* The custom chooser dialog for selecting an application to share the link. */
    AnimatedDialog shareDlg_;
    /* The message to be attached with the shared link */
    String shareMsg_;
    /* The subject to be attached with the sharing message */
    String shareSub_;
    /* Json object containing key-value pairs for the parameters to be linked. */
    JSONObject linkCreationParams_;
    /* Current Branch instance. */
    Branch branch_;
    /* Set of tags to be attached to the link. */
    Collection<String> tags_;
    /* Feature name associated with the link. */
    String feature_;
    /* Stage associated with the link. */
    String stage_;
    /* Callback instance for messaging sharing status. */
    Branch.BranchLinkShareListener callback_;
    /* List of apps available for sharing. */
    private List<ResolveInfo> appList_;
    /* Default URL to be shared in case there is an error creating deep link */
    private String defaultURL_;

    private Intent shareLinkIntent_;
    Context context_;
    /* Background color for the list view in enabled state. */
    private final int BG_COLOR_ENABLED = Color.argb(60, 17, 4, 56);
    /* Background color for the list view in disabled state. */
    private final int BG_COLOR_DISABLED = Color.argb(20, 17, 4, 56);
    private static int viewItemMinHeight = 100;

    /**
     * Creates an application selector and shares a link on user selecting the application.
     *
     * @param builder A {@link io.branch.referral.Branch.ShareLinkBuilder} instance to build share link.
     * @return Instance of the {@link Dialog} holding the share view. Null if sharing dialog is not created due to any error.
     */
    public Dialog shareLink(Branch.ShareLinkBuilder builder) {
        context_ = builder.getActivity();
        tags_ = builder.getTags();
        feature_ = builder.getFeature();
        stage_ = builder.getStage();
        linkCreationParams_ = builder.getLinkCreationParams();
        shareMsg_ = builder.getShareMsg();
        shareSub_ = builder.getShareSub();
        branch_ = builder.getBranch();
        callback_ = builder.getCallback();
        defaultURL_ = builder.getDefaultURL();
        shareLinkIntent_ = new Intent(Intent.ACTION_SEND);
        shareLinkIntent_.setType("text/plain");

        try {
            createShareDialog(builder.getPreferredOptions());
        } catch (Exception e) {
            e.printStackTrace();
            if (callback_ != null) {
                callback_.onLinkShareResponse(null, null, new BranchError("Trouble sharing link", BranchError.ERR_BRANCH_NO_SHARE_OPTION));
            } else {
                Log.i("BranchSDK", "Unable create share options. Couldn't find applications on device to share the link.");
            }
        }

        return shareDlg_;
    }

    /**
     * Dismiss the share dialog if showing. Should be called on activity stopping.
     */
    public void cancelShareLinkDialog() {
        if (shareDlg_ != null && shareDlg_.isShowing()) {
            callback_ = null;
            shareDlg_.cancel();
            shareDlg_ = null;
        }
    }


    /**
     * Create a custom chooser dialog with available share options.
     *
     * @param preferredOptions List of {@link io.branch.referral.SharingHelper.SHARE_WITH} options.
     */
    private void createShareDialog(List<SharingHelper.SHARE_WITH> preferredOptions) {
        final PackageManager packageManager = context_.getPackageManager();
        final List<ResolveInfo> preferredApps = new ArrayList<>();
        final List<ResolveInfo> matchingApps = packageManager.queryIntentActivities(shareLinkIntent_, PackageManager.MATCH_DEFAULT_ONLY);
        ArrayList<SharingHelper.SHARE_WITH> packagesFilterList = new ArrayList<>(preferredOptions);

        /* Get all apps available for sharing and the available preferred apps. */
        for (ResolveInfo resolveInfo : matchingApps) {
            SharingHelper.SHARE_WITH foundMatching = null;
            String packageName = resolveInfo.activityInfo.packageName;
            for (SharingHelper.SHARE_WITH PackageFilter : packagesFilterList) {
                if (resolveInfo.activityInfo != null && packageName.toLowerCase().contains(PackageFilter.toString().toLowerCase())) {
                    foundMatching = PackageFilter;
                    break;
                }
            }
            if (foundMatching != null) {
                preferredApps.add(resolveInfo);
                preferredOptions.remove(foundMatching);
            }
        }
        /* Create all app list with copy link item. */
        matchingApps.removeAll(preferredApps);
        matchingApps.addAll(0, preferredApps);

        matchingApps.add(new CopyLinkItem());
        preferredApps.add(new CopyLinkItem());

        if (preferredApps.size() > 1) {
            /* Add more and copy link option to preferred app.*/
            if (matchingApps.size() > preferredApps.size()) {
                preferredApps.add(new MoreShareItem());
            }
            appList_ = preferredApps;
        } else {
            appList_ = matchingApps;
        }

        /* Copy link option will be always there for sharing. */

        final ChooserArrayAdapter adapter = new ChooserArrayAdapter();
        final ListView shareOptionListView = new ListView(context_);
        shareOptionListView.setAdapter(adapter);
        shareOptionListView.setHorizontalFadingEdgeEnabled(false);
        shareOptionListView.setBackgroundColor(Color.WHITE);

        shareOptionListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                if (view.getTag() instanceof MoreShareItem) {
                    appList_ = matchingApps;
                    adapter.notifyDataSetChanged();
                } else {
                    if (callback_ != null) {
                        callback_.onChannelSelected(((ResolveInfo) view.getTag()).loadLabel(context_.getPackageManager()).toString());
                    }
                    invokeSharingClient((ResolveInfo) view.getTag());
                    adapter.selectedPos = pos;
                    adapter.notifyDataSetChanged();
                    if(shareDlg_ != null){
                        shareDlg_.cancel();
                    }
                }
            }
        });

        shareDlg_ = new AnimatedDialog(context_);
        shareDlg_.setContentView(shareOptionListView);
        shareDlg_.show();
        if(callback_!= null){
           callback_.onShareLinkDialogLaunched();
        }
    }


    /**
     * Invokes a sharing client with a link created by the given json objects.
     *
     * @param selectedResolveInfo The {@link ResolveInfo} corresponding to the selected sharing client.
     */
    @SuppressWarnings("deprecation")
	private void invokeSharingClient(final ResolveInfo selectedResolveInfo) {
        final String channelName = selectedResolveInfo.loadLabel(context_.getPackageManager()).toString();
        branch_.getShortUrl(tags_, channelName, feature_, stage_, linkCreationParams_, new Branch.BranchLinkCreateListener() {
            @Override
            public void onLinkCreate(String url, BranchError error) {
                if (error == null) {
                    shareWithClient(selectedResolveInfo, url, channelName);
                } else {
                    //If there is a default URL specified share it.
                    if (defaultURL_ != null && defaultURL_.length() > 0) {
                        shareWithClient(selectedResolveInfo, defaultURL_, channelName);
                    } else {
                        if (callback_ != null) {
                            callback_.onLinkShareResponse(url, channelName, error);
                        } else {
                            Log.i("BranchSDK", "Unable to share link " + error.getMessage());
                        }
                    }

                }
            }
        });
    }

    private void shareWithClient(ResolveInfo selectedResolveInfo, String url, String channelName) {
        if (selectedResolveInfo instanceof CopyLinkItem) {
            addLinkToClipBoard(url, shareMsg_);
        } else {
            if (callback_ != null) {
                callback_.onLinkShareResponse(url, channelName, null);
            } else {
                Log.i("BranchSDK", "Shared link with " + channelName);
            }
            shareLinkIntent_.setPackage(selectedResolveInfo.activityInfo.packageName);
            if(shareSub_ != null && shareSub_.trim().length() > 0) {
                shareLinkIntent_.putExtra(Intent.EXTRA_SUBJECT, shareSub_);
            }
            shareLinkIntent_.putExtra(Intent.EXTRA_TEXT, shareMsg_ + "\n" + url);
            context_.startActivity(shareLinkIntent_);
        }
    }

    /**
     * Adds a given link to the clip board.
     *
     * @param url   A {@link String} to add to the clip board
     * @param label A {@link String} label for the adding link
     */

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void addLinkToClipBoard(String url, String label) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context_.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(url);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context_.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(label, url);
            clipboard.setPrimaryClip(clip);
        }
        Toast.makeText(context_, "Copied link to clipboard!", Toast.LENGTH_SHORT).show();
    }

    /*
     * Adapter class for creating list of available share options
     */
    private class ChooserArrayAdapter extends BaseAdapter {
        public int selectedPos = -1;

        @Override
        public int getCount() {
            return appList_.size();
        }

        @Override
        public Object getItem(int position) {
            return appList_.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ShareItemView itemView;
            if (convertView == null) {
                itemView = new ShareItemView(context_);
            } else {
                itemView = (ShareItemView) convertView;
            }
            ResolveInfo resolveInfo = appList_.get(position);
            boolean setSelected = position == selectedPos;
            itemView.setLabel(resolveInfo.loadLabel(context_.getPackageManager()).toString(), resolveInfo.loadIcon(context_.getPackageManager()), setSelected);
            itemView.setTag(resolveInfo);
            itemView.setClickable(false);
            return itemView;
        }
        @Override
        public boolean isEnabled(int position) {
            return selectedPos < 0;
        }
    }

    /**
     * Class for sharing item view to be displayed in the list with Application icon and Name.
     */
    private class ShareItemView extends TextView {
        Context context_;
        final int padding = 5;
        final int leftMargin = 100;
        public ShareItemView(Context context) {
            super(context);
            context_ = context;
            this.setPadding(leftMargin, padding, padding, padding);
            this.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            this.setMinWidth(context_.getResources().getDisplayMetrics().widthPixels);
        }

        public void setLabel(String appName, Drawable appIcon, boolean isEnabled) {
            this.setText("\t" + appName);
            this.setTag(appName);
            if (appIcon == null) {
                this.setTextAppearance(context_, android.R.style.TextAppearance_Large);
                this.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            } else {
                this.setTextAppearance(context_, android.R.style.TextAppearance_Medium);
                this.setCompoundDrawablesWithIntrinsicBounds(appIcon, null, null, null);
                if(viewItemMinHeight < (appIcon.getIntrinsicHeight()+padding)){
                    viewItemMinHeight = (appIcon.getIntrinsicHeight()+padding);
                }
            }
            this.setMinHeight(viewItemMinHeight);
            this.setTextColor(context_.getResources().getColor(android.R.color.black));
            if(isEnabled){
                this.setBackgroundColor(BG_COLOR_ENABLED);
            }else{
                this.setBackgroundColor(BG_COLOR_DISABLED);
            }
        }
    }

    /**
     * Class for sharing item more
     */
    private class MoreShareItem extends ResolveInfo {
        @Override
        public CharSequence loadLabel(PackageManager pm) {
            return "More...";
        }

        @Override
        public Drawable loadIcon(PackageManager pm) {
            return context_.getResources().getDrawable(android.R.drawable.ic_menu_more);
        }
    }

    /**
     * Class for Sharing Item copy URl
     */
    private class CopyLinkItem extends ResolveInfo {
        @Override
        public CharSequence loadLabel(PackageManager pm) {
            return "Copy link";
        }

        @Override
        public Drawable loadIcon(PackageManager pm) {
            return context_.getResources().getDrawable(android.R.drawable.ic_menu_save);
        }

    }

}
