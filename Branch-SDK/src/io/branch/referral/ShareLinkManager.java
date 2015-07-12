package io.branch.referral;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
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
    /* The custom chooser dialog foe selecting an application to share the link. */
    Dialog shareDlg_;
    /* The message to be attached with the shared link */
    String shareMsg_;
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

    private final String OPTIONS_MORE = "More...";
    private final String OPTIONS_COPY_LINK = "Copy link";
    private Intent shareLinkIntent_;
    Context context_;

    public void shareLink(Context context,
                          Collection<String> tags,
                          String feature,
                          String stage,
                          JSONObject parameters,
                          String message,
                          Branch branch,
                          List<SharingHelper.SHARE_WITH> preferredOptions,
                          Branch.BranchLinkShareListener callback) {
        context_ = context;
        tags_ = tags;
        feature_ = feature;
        stage_ = stage;
        linkCreationParams_ = parameters;
        shareMsg_ = message;
        branch_ = branch;
        callback_ = callback;
        shareLinkIntent_ = new Intent(Intent.ACTION_SEND);
        shareLinkIntent_.setType("text/plain");
        try {
            createShareDialog(preferredOptions);
        } catch (Exception e) {
            e.printStackTrace();
            if (callback_ != null) {
                callback_.onLinkShareResponse(null, null, new BranchError("Trouble sharing link", BranchError.ERR_BRANCH_NO_SHARE_OPTION));
            } else {
                Log.i("BranchSDK", "Unable create share options. Couldn't find applications on device to share the link.");
            }
        }
    }

    /**
     * Dismiss the share dialog if showing. Should be called on activity stopping.
     */
    public void cancelShareLink() {
        if (shareDlg_ != null && shareDlg_.isShowing()) {
            shareDlg_.dismiss();
        }
    }

    /**
     * Create a custom chooser dialog with available share options.
     *
     * @param preferredOptions List of {@link io.branch.referral.SharingHelper.SHARE_WITH} options.
     */
    private void createShareDialog(List<SharingHelper.SHARE_WITH> preferredOptions) {
        final PackageManager packageManager = context_.getPackageManager();
        final List<ResolveInfo> preferredApps = new ArrayList<ResolveInfo>();
        final List<ResolveInfo> matchingApps = packageManager.queryIntentActivities(shareLinkIntent_, PackageManager.MATCH_DEFAULT_ONLY);
        ArrayList<SharingHelper.SHARE_WITH> packagesFilterList = new ArrayList<SharingHelper.SHARE_WITH>(preferredOptions);

        /* Get alla apps available for sharing and the available preferred apps. */
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

        if (preferredApps.size() > 0) {
            /* Add more and copy link option to preferred app*/
            preferredApps.add(new MoreShareItem());
            preferredApps.add(new CopyLinkItem());
            appList_ = preferredApps;
        } else {
            appList_ = matchingApps;
        }
        /* Copy link option will be always there for sharing. */

        final BaseAdapter adapter = new ChooserArrayAdapter();
        final ListView shareOptionListView = new ListView(context_);
        shareOptionListView.setAdapter(adapter);
        shareOptionListView.setHorizontalFadingEdgeEnabled(false);

        shareOptionListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                if (view.getTag() instanceof MoreShareItem) {
                    appList_ = matchingApps;
                    adapter.notifyDataSetChanged();
                } else {
                    invokeSharingClient((ResolveInfo) view.getTag());
                }
            }
        });
        shareDlg_ = new Dialog(context_);
        shareDlg_.requestWindowFeature(Window.FEATURE_NO_TITLE);
        shareDlg_.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        Window window = shareDlg_.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.BOTTOM;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(shareDlg_.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        shareDlg_.setContentView(shareOptionListView);
        shareDlg_.show();
        shareDlg_.getWindow().setAttributes(lp);
    }

    /**
     * Invokes a sharing client with a link created by the given json objects.
     *
     * @param selectedResolveInfo The {@link ResolveInfo} corresponding to the selected sharing client.
     */
    void invokeSharingClient(final ResolveInfo selectedResolveInfo) {
        final String channelName = selectedResolveInfo.loadLabel(context_.getPackageManager()).toString();
        branch_.getShortUrl(tags_, channelName, feature_, stage_, linkCreationParams_, new Branch.BranchLinkCreateListener() {
            @Override
            public void onLinkCreate(String url, BranchError error) {
                if (error != null) {
                    if (callback_ != null) {
                        callback_.onLinkShareResponse(url, channelName, error);
                    } else {
                        Log.i("BranchSDK", "Unable to share link " + error.getMessage());
                    }
                    shareDlg_.dismiss();
                } else {
                    if (selectedResolveInfo instanceof CopyLinkItem) {
                        addLinkToClipBoard(url, shareMsg_);
                    } else {
                        shareLinkIntent_.setPackage(selectedResolveInfo.activityInfo.packageName);
                        shareLinkIntent_.putExtra(Intent.EXTRA_TEXT, shareMsg_ + "\n" + url);
                        context_.startActivity(shareLinkIntent_);
                        if (callback_ != null) {
                            callback_.onLinkShareResponse(url, channelName, null);
                        } else {
                            Log.i("BranchSDK", "Shared link with " + channelName);
                        }
                    }
                    shareDlg_.dismiss();
                }
            }
        });
    }

    /**
     * Adds a given link to the clip board.
     *
     * @param url   A {@link String} to add to the clip board
     * @param label A {@link String} label for the adding link
     */
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
        Toast.makeText(context_, "Link copied", Toast.LENGTH_SHORT).show();
    }

    /*
     * Adapter class for creating list of available share options
     */
    private class ChooserArrayAdapter extends BaseAdapter {
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
            itemView.setLabel(resolveInfo.loadLabel(context_.getPackageManager()).toString(), resolveInfo.loadIcon(context_.getPackageManager()));
            itemView.setTag(resolveInfo);
            return itemView;
        }
    }

    /**
     * Class for sharing item view to be displayed in the list with Application icon and Name.
     */
    private class ShareItemView extends TextView {
        Context context_;

        public ShareItemView(Context context) {
            super(context);
            context_ = context;
            this.setPadding(100, 5, 5, 5);
            this.setBackgroundColor(Color.argb(60, 17, 04, 56));
            this.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            this.setMinHeight(100);

        }

        public void setLabel(String appName, Drawable appIcon) {
            this.setText("\t" + appName);
            this.setTag(appName);
            if (appIcon == null) {
                this.setTextAppearance(context_, android.R.style.TextAppearance_Large);
                this.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            } else {
                this.setTextAppearance(context_, android.R.style.TextAppearance_Medium);
                this.setCompoundDrawablesWithIntrinsicBounds(appIcon, null, null, null);
            }
            this.setTextColor(context_.getResources().getColor(android.R.color.black));
        }
    }

    /**
     * Class for sharing item more
     */
    private class MoreShareItem extends ResolveInfo {
        @Override
        public CharSequence loadLabel(PackageManager pm) {
            return OPTIONS_MORE;
        }

        @Override
        public Drawable loadIcon(PackageManager pm) {
            return null;
        }
    }

    /**
     * Class for Sharing Item copy URl
     */
    private class CopyLinkItem extends ResolveInfo {
        @Override
        public CharSequence loadLabel(PackageManager pm) {
            return OPTIONS_COPY_LINK;
        }

        @Override
        public Drawable loadIcon(PackageManager pm) {
            return null;
        }

    }


}
