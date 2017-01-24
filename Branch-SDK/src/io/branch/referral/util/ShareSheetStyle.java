package io.branch.referral.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.SharingHelper;

/**
 * Class for defining the share sheet properties.
 * Defines the properties of share sheet. Use this class customise the share sheet style.
 *
 * @see BranchUniversalObject#showShareSheet(Activity, LinkProperties, ShareSheetStyle, Branch.BranchLinkShareListener)
 */
public class ShareSheetStyle {
    //Customise more and copy url option
    private Drawable moreOptionIcon_;
    private String moreOptionText_;

    private Drawable copyUrlIcon_;
    private String copyURlText_;
    private String urlCopiedMessage_;

    private final String messageTitle_;
    private final String messageBody_;

    private final ArrayList<SharingHelper.SHARE_WITH> preferredOptions_;
    private String defaultURL_;

    private int styleResourceID_ = -1;
    final Context context_;
    private boolean setFullWidthStyle_;
    private int dividerHeight_ = -1;

    private String sharingTitle_ = null;
    private View sharingTitleView_ = null;

    private List<String> includeInShareSheet = new ArrayList<>();
    private List<String> excludeFromShareSheet = new ArrayList<>();

    public ShareSheetStyle(@NonNull Context context, @NonNull String messageTitle, @NonNull String messageBody) {
        context_ = context;
        moreOptionIcon_ = null;
        moreOptionText_ = null;

        copyUrlIcon_ = null;
        copyURlText_ = null;
        urlCopiedMessage_ = null;

        preferredOptions_ = new ArrayList<>();
        defaultURL_ = null;

        messageTitle_ = messageTitle;
        messageBody_ = messageBody;
    }

    /**
     * <p> Set a default url to share in case there is any error creating the deep link </p>
     *
     * @param url A {@link String} with value of default url to be shared with the selected application in case deep link creation fails.
     * @return This object to allow method chaining
     */
    public ShareSheetStyle setDefaultURL(String url) {
        defaultURL_ = url;
        return this;
    }

    /**
     * <p> Set the icon and label for the option to expand the application list to see more options.
     * Default label is set to "More" </p>
     *
     * @param icon  Drawable to set as the icon for more option. Default icon is system menu_more icon.
     * @param label A {@link String} with value for the more option label. Default label is "More"
     * @return This object to allow method chaining
     */
    public ShareSheetStyle setMoreOptionStyle(Drawable icon, String label) {
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
     * @return This object to allow method chaining
     */
    public ShareSheetStyle setMoreOptionStyle(@DrawableRes int drawableIconID, @StringRes int stringLabelID) {
        moreOptionIcon_ = getDrawable(context_, drawableIconID);
        moreOptionText_ = context_.getResources().getString(stringLabelID);

        return this;
    }

    /**
     * <p> Set the icon, label and success message for copy url option. Default label is "Copy link".</p>
     *
     * @param icon    Drawable to set as the icon for copy url  option. Default icon is system menu_save icon
     * @param label   A {@link String} with value for the copy url option label. Default label is "Copy link"
     * @param message A {@link String} with value for a toast message displayed on copying a url.
     *                Default message is "Copied link to clipboard!"
     * @return This object to allow method chaining
     */
    public ShareSheetStyle setCopyUrlStyle(Drawable icon, String label, String message) {
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
     * @return A {@link ShareSheetStyle} instance.
     */
    public ShareSheetStyle setCopyUrlStyle(@DrawableRes int drawableIconID, @StringRes int stringLabelID, @StringRes int stringMessageID) {
        copyUrlIcon_ = getDrawable(context_, drawableIconID);
        copyURlText_ = context_.getResources().getString(stringLabelID);
        urlCopiedMessage_ = context_.getResources().getString(stringMessageID);
        return this;
    }

    /**
     * <p>Adds application to the preferred list of applications which are shown on share dialog.
     * Only these options will be visible when the application selector dialog launches. Other options can be
     * accessed by clicking "More"</p>
     *
     * @param preferredOption A list of applications to be added as preferred options on the app chooser.
     *                        Preferred applications are defined in {@link io.branch.referral.SharingHelper.SHARE_WITH}.
     * @return This object to allow method chaining
     */
    public ShareSheetStyle addPreferredSharingOption(SharingHelper.SHARE_WITH preferredOption) {
        this.preferredOptions_.add(preferredOption);
        return this;
    }

    /**
     * <p> Set the given style to the List View showing the share sheet</p>
     *
     * @param styleResourceID A Styleable resource to be applied to the share sheet list view
     */
    public ShareSheetStyle setStyleResourceID(@StyleRes int styleResourceID) {
        styleResourceID_ = styleResourceID;
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
    public ShareSheetStyle setAsFullWidthStyle(boolean setFullWidthStyle) {
        this.setFullWidthStyle_ = setFullWidthStyle;
        return this;
    }

    /**
     * Set the height for the divider for the sharing channels in the list. Set this to zero to remove the dividers
     *
     * @param height The new height of the divider in pixels.
     * @return his Builder object to allow for chaining of calls to set methods.
     */
    public ShareSheetStyle setDividerHeight(int height) {
        this.dividerHeight_ = height;
        return this;
    }

    /**
     * Set the title for the sharing dialog
     *
     * @param title {@link String} containing the value for the title text.
     * @return this Builder object to allow for chaining of calls to set methods.
     */
    public ShareSheetStyle setSharingTitle(String title) {
        this.sharingTitle_ = title;
        return this;
    }

    /**
     * Set the title for the sharing dialog
     *
     * @param titleView {@link View} for setting the title.
     * @return this Builder object to allow for chaining of calls to set methods.
     */
    public ShareSheetStyle setSharingTitle(View titleView) {
        this.sharingTitleView_ = titleView;
        return this;
    }

    public ShareSheetStyle excludeFromShareSheet(@NonNull String packageName) {
        this.excludeFromShareSheet.add(packageName);
        return this;
    }

    public ShareSheetStyle excludeFromShareSheet(@NonNull String[] packageName) {
        excludeFromShareSheet.addAll(Arrays.asList(packageName));
        return this;
    }

    public ShareSheetStyle excludeFromShareSheet(@NonNull List<String> packageNames) {
        this.excludeFromShareSheet.addAll(packageNames);
        return this;
    }

    public ShareSheetStyle includeInShareSheet(@NonNull String packageName) {
        this.includeInShareSheet.add(packageName);
        return this;
    }

    public ShareSheetStyle includeInShareSheet(@NonNull String[] packageName) {
        includeInShareSheet.addAll(Arrays.asList(packageName));
        return this;
    }

    public ShareSheetStyle includeInShareSheet(@NonNull List<String> packageNames) {
        this.includeInShareSheet.addAll(packageNames);
        return this;
    }

    public List<String> getExcludedFromShareSheet() {
        return excludeFromShareSheet;
    }

    public List<String> getIncludedInShareSheet() {
        return includeInShareSheet;
    }

    public ArrayList<SharingHelper.SHARE_WITH> getPreferredOptions() {
        return preferredOptions_;
    }

    public Drawable getCopyUrlIcon() {
        return copyUrlIcon_;
    }

    public Drawable getMoreOptionIcon() {
        return moreOptionIcon_;
    }

    public String getMessageBody() {
        return messageBody_;
    }

    public String getMessageTitle() {
        return messageTitle_;
    }

    public String getCopyURlText() {
        return copyURlText_;
    }

    public String getDefaultURL() {
        return defaultURL_;
    }

    public String getMoreOptionText() {
        return moreOptionText_;
    }

    public String getUrlCopiedMessage() {
        return urlCopiedMessage_;
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

    public boolean getIsFullWidthStyle() {
        return setFullWidthStyle_;
    }

    private Drawable getDrawable(@NonNull Context context, @DrawableRes int drawableID) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getResources().getDrawable(drawableID, context.getTheme());
        } else {
            //noinspection deprecation
            return context.getResources().getDrawable(drawableID);
        }
    }

    public int getStyleResourceID() {
        return styleResourceID_;
    }
}