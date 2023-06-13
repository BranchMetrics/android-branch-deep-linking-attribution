package io.branch.referral;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * <p> Class for building a share link dialog.This creates a chooser for selecting application for
 * sharing a link created with given parameters. </p>
 */
@SuppressWarnings({"UnusedReturnValue", "WeakerAccess"})
public class BranchShareSheetBuilder {

    private final Activity activity_;

    private String shareMsg_;
    private String shareSub_;
    private BranchLinkShareListener callback_;
    private IChannelProperties channelPropertiesCallback_;

    private ArrayList<SharingHelper.SHARE_WITH> preferredOptions_;
    private String defaultURL_;

    //Customise more and copy url option
    private Drawable moreOptionIcon_;
    private String moreOptionText_;
    private Drawable copyUrlIcon_;
    private String copyURlText_;
    private String urlCopiedMessage_;
    private int styleResourceID_;
    private boolean setFullWidthStyle_;
    private int dialogThemeResourceID_;
    private int dividerHeight_ = -1;
    private String sharingTitle_ = null;
    private View sharingTitleView_ = null;
    private int iconSize_ = 50;

    private BranchShortLinkBuilder shortLinkBuilder_;
    private List<String> includeInShareSheet = new ArrayList<>();
    private List<String> excludeFromShareSheet = new ArrayList<>();

    /**
     * <p>Creates options for sharing a link with other Applications. Creates a builder for sharing the link with
     * user selected clients</p>
     *
     * @param activity   The {@link Activity} to show the dialog for choosing sharing application.
     * @param parameters A {@link JSONObject} value containing the deep link params.
     */
    public BranchShareSheetBuilder(Activity activity, JSONObject parameters) {
        this.activity_ = activity;
        shortLinkBuilder_ = new BranchShortLinkBuilder(activity);
        try {
            Iterator<String> keys = parameters.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                shortLinkBuilder_.addParameters(key, parameters.get(key));
            }
        } catch (Exception ignore) {
        }
        shareMsg_ = "";
        callback_ = null;
        channelPropertiesCallback_ = null;
        preferredOptions_ = new ArrayList<>();
        defaultURL_ = null;

        moreOptionIcon_ = BranchUtil.getDrawable(activity.getApplicationContext(), android.R.drawable.ic_menu_more);
        moreOptionText_ = "More...";

        copyUrlIcon_ = BranchUtil.getDrawable(activity.getApplicationContext(), android.R.drawable.ic_menu_save);
        copyURlText_ = "Copy link";
        urlCopiedMessage_ = "Copied link to clipboard!";

        if (Branch.getInstance().getDeviceInfo().isTV()) {
            // Google TV includes a default, stub email app, so the system will appear to have an
            // email app installed, even when there is none. (https://stackoverflow.com/a/10341104)
            excludeFromShareSheet("com.google.android.tv.frameworkpackagestubs");
        }
    }

    /**
     * *<p>Creates options for sharing a link with other Applications. Creates a builder for sharing the link with
     * user selected clients</p>
     *
     * @param activity         The {@link Activity} to show the dialog for choosing sharing application.
     * @param shortLinkBuilder An instance of {@link BranchShortLinkBuilder} to create link to be shared
     */
    public BranchShareSheetBuilder(Activity activity, BranchShortLinkBuilder shortLinkBuilder) {
        this(activity, new JSONObject());
        shortLinkBuilder_ = shortLinkBuilder;
    }

    /**
     * <p>Sets the message to be shared with the link.</p>
     *
     * @param message A {@link String} to be shared with the link
     * @return A {@link BranchShareSheetBuilder} instance.
     */
    public BranchShareSheetBuilder setMessage(String message) {
        this.shareMsg_ = message;
        return this;
    }

    /**
     * <p>Sets the subject of this message. This will be added to Email and SMS Application capable of handling subject in the message.</p>
     *
     * @param subject A {@link String} subject of this message.
     * @return A {@link BranchShareSheetBuilder} instance.
     */
    public BranchShareSheetBuilder setSubject(String subject) {
        this.shareSub_ = subject;
        return this;
    }

    /**
     * <p>Adds the given tag an iterable {@link Collection} of {@link String} tags associated with a deep
     * link.</p>
     *
     * @param tag A {@link String} to be added to the iterable {@link Collection} of {@link String} tags associated with a deep
     *            link.
     * @return A {@link BranchShareSheetBuilder} instance.
     */
    public BranchShareSheetBuilder addTag(String tag) {
        this.shortLinkBuilder_.addTag(tag);
        return this;
    }

    /**
     * <p>Adds the given tag an iterable {@link Collection} of {@link String} tags associated with a deep
     * link.</p>
     *
     * @param tags A {@link java.util.List} of tags to be added to the iterable {@link Collection} of {@link String} tags associated with a deep
     *             link.
     * @return A {@link BranchShareSheetBuilder} instance.
     */
    public BranchShareSheetBuilder addTags(ArrayList<String> tags) {
        this.shortLinkBuilder_.addTags(tags);
        return this;
    }

    /**
     * <p>Adds a feature that make use of the link.</p>
     *
     * @param feature A {@link String} value identifying the feature that the link makes use of.
     *                Should not exceed 128 characters.
     * @return A {@link BranchShareSheetBuilder} instance.
     */
    public BranchShareSheetBuilder setFeature(String feature) {
        this.shortLinkBuilder_.setFeature(feature);
        return this;
    }

    /**
     * <p>Adds a stage application or user flow associated with this link.</p>
     *
     * @param stage A {@link String} value identifying the stage in an application or user flow
     *              process. Should not exceed 128 characters.
     * @return A {@link BranchShareSheetBuilder} instance.
     */
    public BranchShareSheetBuilder setStage(String stage) {
        this.shortLinkBuilder_.setStage(stage);
        return this;
    }

    /**
     * <p>Adds a callback to get the sharing status.</p>
     *
     * @param callback A {@link BranchLinkShareListener} instance for getting sharing status.
     * @return A {@link BranchShareSheetBuilder} instance.
     */
    public BranchShareSheetBuilder setCallback(BranchLinkShareListener callback) {
        this.callback_ = callback;
        return this;
    }

    /**
     * @param channelPropertiesCallback A {@link IChannelProperties} instance for customizing sharing properties for channels.
     * @return A {@link BranchShareSheetBuilder} instance.
     */
    public BranchShareSheetBuilder setChannelProperties(IChannelProperties channelPropertiesCallback) {
        this.channelPropertiesCallback_ = channelPropertiesCallback;
        return this;
    }

    /**
     * <p>Adds application to the preferred list of applications which are shown on share dialog.
     * Only these options will be visible when the application selector dialog launches. Other options can be
     * accessed by clicking "More"</p>
     *
     * @param preferredOption A list of applications to be added as preferred options on the app chooser.
     *                        Preferred applications are defined in {@link io.branch.referral.SharingHelper.SHARE_WITH}.
     * @return A {@link BranchShareSheetBuilder} instance.
     */
    public BranchShareSheetBuilder addPreferredSharingOption(SharingHelper.SHARE_WITH preferredOption) {
        this.preferredOptions_.add(preferredOption);
        return this;
    }

    /**
     * <p>Adds application to the preferred list of applications which are shown on share dialog.
     * Only these options will be visible when the application selector dialog launches. Other options can be
     * accessed by clicking "More"</p>
     *
     * @param preferredOptions A list of applications to be added as preferred options on the app chooser.
     *                         Preferred applications are defined in {@link io.branch.referral.SharingHelper.SHARE_WITH}.
     * @return A {@link BranchShareSheetBuilder} instance.
     */
    public BranchShareSheetBuilder addPreferredSharingOptions(ArrayList<SharingHelper.SHARE_WITH> preferredOptions) {
        this.preferredOptions_.addAll(preferredOptions);
        return this;
    }

    /**
     * Add the given key value to the deep link parameters
     *
     * @param key   A {@link String} with value for the key for the deep link params
     * @param value A {@link String} with deep link parameters value
     * @return A {@link BranchShareSheetBuilder} instance.
     */
    public BranchShareSheetBuilder addParam(String key, String value) {
        try {
            this.shortLinkBuilder_.addParameters(key, value);
        } catch (Exception ignore) {
        }
        return this;
    }

    /**
     * <p> Set a default url to share in case there is any error creating the deep link </p>
     *
     * @param url A {@link String} with value of default url to be shared with the selected application in case deep link creation fails.
     * @return A {@link BranchShareSheetBuilder} instance.
     */
    public BranchShareSheetBuilder setDefaultURL(String url) {
        defaultURL_ = url;
        return this;
    }

    /**
     * <p> Set the icon and label for the option to expand the application list to see more options.
     * Default label is set to "More" </p>
     *
     * @param icon  Drawable to set as the icon for more option. Default icon is system menu_more icon.
     * @param label A {@link String} with value for the more option label. Default label is "More"
     * @return A {@link BranchShareSheetBuilder} instance.
     */
    public BranchShareSheetBuilder setMoreOptionStyle(Drawable icon, String label) {
        moreOptionIcon_ = icon;
        moreOptionText_ = label;
        return this;
    }

    /**
     * <p> Set the icon and label for the option to expand the application list to see more options.
     * Default label is set to "More" </p>
     *
     * @param drawableIconID Resource ID for the drawable to set as the icon for more option. Default icon is system menu_more icon.
     * @param stringLabelID  Resource ID for String label for the more option. Default label is "More"
     * @return A {@link BranchShareSheetBuilder} instance.
     */
    public BranchShareSheetBuilder setMoreOptionStyle(int drawableIconID, int stringLabelID) {
        moreOptionIcon_ = BranchUtil.getDrawable(activity_.getApplicationContext(), drawableIconID);
        moreOptionText_ = activity_.getResources().getString(stringLabelID);
        return this;
    }

    /**
     * <p> Set the icon, label and success message for copy url option. Default label is "Copy link".</p>
     *
     * @param icon    Drawable to set as the icon for copy url  option. Default icon is system menu_save icon
     * @param label   A {@link String} with value for the copy url option label. Default label is "Copy link"
     * @param message A {@link String} with value for a toast message displayed on copying a url.
     *                Default message is "Copied link to clipboard!"
     * @return A {@link BranchShareSheetBuilder} instance.
     */
    public BranchShareSheetBuilder setCopyUrlStyle(Drawable icon, String label, String message) {
        copyUrlIcon_ = icon;
        copyURlText_ = label;
        urlCopiedMessage_ = message;
        return this;
    }

    /**
     * <p> Set the icon, label and success message for copy url option. Default label is "Copy link".</p>
     *
     * @param drawableIconID  Resource ID for the drawable to set as the icon for copy url  option. Default icon is system menu_save icon
     * @param stringLabelID   Resource ID for the string label the copy url option. Default label is "Copy link"
     * @param stringMessageID Resource ID for the string message to show toast message displayed on copying a url
     * @return A {@link BranchShareSheetBuilder} instance.
     */
    public BranchShareSheetBuilder setCopyUrlStyle(int drawableIconID, int stringLabelID, int stringMessageID) {
        copyUrlIcon_ = BranchUtil.getDrawable(activity_.getApplicationContext(), drawableIconID);
        copyURlText_ = activity_.getResources().getString(stringLabelID);
        urlCopiedMessage_ = activity_.getResources().getString(stringMessageID);
        return this;

    }

    /**
     * <p> Sets the alias for this link. </p>
     *
     * @param alias Link 'alias' can be used to label the endpoint on the link.
     *              <p>
     *              For example:
     *              http://bnc.lt/AUSTIN28.
     *              Should not exceed 128 characters
     *              </p>
     * @return This Builder object to allow for chaining of calls to set methods.
     */
    public BranchShareSheetBuilder setAlias(String alias) {
        this.shortLinkBuilder_.setAlias(alias);
        return this;
    }

    /**
     * <p> Sets the amount of time that Branch allows a click to remain outstanding.</p>
     *
     * @param matchDuration A {@link Integer} value specifying the time that Branch allows a click to
     *                      remain outstanding and be eligible to be matched with a new app session.
     * @return This Builder object to allow for chaining of calls to set methods.
     */
    public BranchShareSheetBuilder setMatchDuration(int matchDuration) {
        this.shortLinkBuilder_.setDuration(matchDuration);
        return this;
    }

    /**
     * <p>
     * Sets the share dialog to full width mode. Full width mode will show a non modal sheet with entire screen width.
     * </p>
     *
     * @param setFullWidthStyle {@link Boolean} With value true if a full width style share sheet is desired.
     * @return This Builder object to allow for chaining of calls to set methods.
     */
    public BranchShareSheetBuilder setAsFullWidthStyle(boolean setFullWidthStyle) {
        this.setFullWidthStyle_ = setFullWidthStyle;
        return this;
    }

    /**
     * <p>
     * Sets the given resource id as the theme id for share sheet dialog view.
     * </p>
     *
     * @param styleResourceID the id of the theme to be applied to the share sheet dialog.
     * @return This Builder object to allow for chaining of calls to set methods.
     */
    public BranchShareSheetBuilder setDialogThemeResourceID(@StyleRes int styleResourceID) {
        this.dialogThemeResourceID_ = styleResourceID;
        return this;
    }

    /**
     * Set the height for the divider for the sharing channels in the list. Set this to zero to remove the dividers
     *
     * @param height The new height of the divider in pixels.
     * @return this Builder object to allow for chaining of calls to set methods.
     */
    public BranchShareSheetBuilder setDividerHeight(int height) {
        this.dividerHeight_ = height;
        return this;
    }

    /**
     * Set the title for the sharing dialog
     *
     * @param title {@link String} containing the value for the title text.
     * @return this Builder object to allow for chaining of calls to set methods.
     */
    public BranchShareSheetBuilder setSharingTitle(String title) {
        this.sharingTitle_ = title;
        return this;
    }

    /**
     * Set the title for the sharing dialog
     *
     * @param titleView {@link View} for setting the title.
     * @return this Builder object to allow for chaining of calls to set methods.
     */
    public BranchShareSheetBuilder setSharingTitle(View titleView) {
        this.sharingTitleView_ = titleView;
        return this;
    }

    /**
     * Set icon size for the sharing dialog
     *
     * @param iconSize {@link int} for setting the share sheet icon size.
     * @return this Builder object to allow for chaining of calls to set methods.
     */
    public BranchShareSheetBuilder setIconSize(int iconSize) {
        this.iconSize_ = iconSize;
        return this;
    }

    /**
     * Exclude items from the ShareSheet by package name String.
     *
     * @param packageName {@link String} package name to be excluded.
     * @return this Builder object to allow for chaining of calls to set methods.
     */
    public BranchShareSheetBuilder excludeFromShareSheet(@NonNull String packageName) {
        this.excludeFromShareSheet.add(packageName);
        return this;
    }

    /**
     * Exclude items from the ShareSheet by package name array.
     *
     * @param packageName {@link String[]} package name to be excluded.
     * @return this Builder object to allow for chaining of calls to set methods.
     */
    public BranchShareSheetBuilder excludeFromShareSheet(@NonNull String[] packageName) {
        this.excludeFromShareSheet.addAll(Arrays.asList(packageName));
        return this;
    }

    /**
     * Exclude items from the ShareSheet by package name List.
     *
     * @param packageNames {@link List} package name to be excluded.
     * @return this Builder object to allow for chaining of calls to set methods.
     */
    public BranchShareSheetBuilder excludeFromShareSheet(@NonNull List<String> packageNames) {
        this.excludeFromShareSheet.addAll(packageNames);
        return this;
    }

    /**
     * Include items from the ShareSheet by package name String. If only "com.Slack"
     * is included, then only preferred sharing options + Slack
     * will be displayed, for example.
     *
     * @param packageName {@link String} package name to be included.
     * @return this Builder object to allow for chaining of calls to set methods.
     */
    public BranchShareSheetBuilder includeInShareSheet(@NonNull String packageName) {
        this.includeInShareSheet.add(packageName);
        return this;
    }

    /**
     * Include items from the ShareSheet by package name Array. If only "com.Slack"
     * is included, then only preferred sharing options + Slack
     * will be displayed, for example.
     *
     * @param packageName {@link String[]} package name to be included.
     * @return this Builder object to allow for chaining of calls to set methods.
     */
    public BranchShareSheetBuilder includeInShareSheet(@NonNull String[] packageName) {
        this.includeInShareSheet.addAll(Arrays.asList(packageName));
        return this;
    }

    /**
     * Include items from the ShareSheet by package name List. If only "com.Slack"
     * is included, then only preferred sharing options + Slack
     * will be displayed, for example.
     *
     * @param packageNames {@link List} package name to be included.
     * @return this Builder object to allow for chaining of calls to set methods.
     */
    public BranchShareSheetBuilder includeInShareSheet(@NonNull List<String> packageNames) {
        this.includeInShareSheet.addAll(packageNames);
        return this;
    }

    /**
     * <p> Set the given style to the List View showing the share sheet</p>
     *
     * @param resourceID A Styleable resource to be applied to the share sheet list view
     */
    public void setStyleResourceID(@StyleRes int resourceID) {
        styleResourceID_ = resourceID;
    }

    public void setShortLinkBuilderInternal(BranchShortLinkBuilder shortLinkBuilder) {
        this.shortLinkBuilder_ = shortLinkBuilder;
    }

    /**
     * <p>Creates an application selector dialog and share a link with user selected sharing option.
     * The link is created with the parameters provided to the builder. </p>
     */
    public void shareLink() {
        Branch.getInstance().shareLink(this);
    }

    public Activity getActivity() {
        return activity_;
    }

    public ArrayList<SharingHelper.SHARE_WITH> getPreferredOptions() {
        return preferredOptions_;
    }

    List<String> getExcludedFromShareSheet() {
        return excludeFromShareSheet;
    }

    List<String> getIncludedInShareSheet() {
        return includeInShareSheet;
    }

    @Deprecated public Branch getBranch() {
        return Branch.getInstance();
    }

    public String getShareMsg() {
        return shareMsg_;
    }

    public String getShareSub() {
        return shareSub_;
    }

    public BranchLinkShareListener getCallback() {
        return callback_;
    }

    public IChannelProperties getChannelPropertiesCallback() {
        return channelPropertiesCallback_;
    }

    public String getDefaultURL() {
        return defaultURL_;
    }

    public Drawable getMoreOptionIcon() {
        return moreOptionIcon_;
    }

    public String getMoreOptionText() {
        return moreOptionText_;
    }

    public Drawable getCopyUrlIcon() {
        return copyUrlIcon_;
    }

    public String getCopyURlText() {
        return copyURlText_;
    }

    public String getUrlCopiedMessage() {
        return urlCopiedMessage_;
    }

    public BranchShortLinkBuilder getShortLinkBuilder() {
        return shortLinkBuilder_;
    }

    public boolean getIsFullWidthStyle() {
        return setFullWidthStyle_;
    }

    public int getDialogThemeResourceID() {
        return dialogThemeResourceID_;
    }

    public int getDividerHeight() {
        return dividerHeight_;
    }

    public String getSharingTitle() {
        return sharingTitle_;
    }

    public View getSharingTitleView() {
        return sharingTitleView_;
    }

    public int getStyleResourceID() {
        return styleResourceID_;
    }

    public int getIconSize() {
        return iconSize_;
    }
}