package io.branch.referral.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;

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

    final Context context_;

    public ShareSheetStyle(Context context, String messageTitle, String messageBody) {
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
    public ShareSheetStyle setMoreOptionStyle(int drawableIconID, int stringLabelID) {
        moreOptionIcon_ = context_.getResources().getDrawable(drawableIconID);
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
    public ShareSheetStyle setCopyUrlStyle(int drawableIconID, int stringLabelID, int stringMessageID) {
        copyUrlIcon_ = context_.getResources().getDrawable(drawableIconID);
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

}