package io.branch.referral.util;

import android.app.Activity;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchLinkCreateListener;
import io.branch.referral.BranchLinkShareListener;

/**
 * <p>
 * Class for representing any additional information that is specific to the link.
 * Use this class to specify the properties of a deep link such as channel, feature etc and any control params associated with the link.
 *
 * @see BranchUniversalObject#getShortUrl(Context, LinkProperties)
 * @see BranchUniversalObject#generateShortUrl(Context, LinkProperties, BranchLinkCreateListener)
 * @see BranchUniversalObject#showShareSheet(Activity, LinkProperties, ShareSheetStyle, BranchLinkShareListener)
 * </p>
 */
public class LinkProperties implements Parcelable {
    private final ArrayList<String> tags_;
    private String feature_;
    private String alias_;
    private String stage_;
    private int matchDuration_;
    private final HashMap<String, String> controlParams_;
    private String channel_;
    private String campaign_;

    /**
     * Create an instance of {@link LinkProperties}
     */
    public LinkProperties() {
        tags_ = new ArrayList<>();
        feature_ = "Share";
        controlParams_ = new HashMap<>();
        alias_ = "";
        stage_ = "";
        matchDuration_ = 0;
        channel_ = "";
        campaign_ = "";
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
    public LinkProperties setAlias(String alias) {
        this.alias_ = alias;
        return this;
    }

    /**
     * <p>Adds a tag to the iterable collection of name associated with a deep link.
     * </p>
     *
     * @param tag {@link String} tags associated with a deep link.
     * @return This Builder object to allow for chaining of calls to set methods.
     */
    public LinkProperties addTag(String tag) {
        this.tags_.add(tag);
        return this;
    }

    /**
     * <p>Adds any control params that control the behaviour of the link.
     * Control parameters include Custom redirect url ($android_url,$ios_url),
     * path for auto deep linking($android_deeplink_path,$deeplink_path) etc </p>
     *
     * @param key   A {@link String} with value of key for the parameter
     * @param value A {@link String} with value of value for the parameter
     * @return This Builder object to allow for chaining of calls to set methods.
     */
    public LinkProperties addControlParameter(String key, String value) {
        this.controlParams_.put(key, value);
        return this;
    }

    /**
     * <p> Set a name that identifies the feature that the link makes use of.</p>
     *
     * @param feature A {@link String} value identifying the feature that the link makes use of.
     *                Should not exceed 128 characters.
     * @return This Builder object to allow for chaining of calls to set methods.
     */
    public LinkProperties setFeature(String feature) {
        this.feature_ = feature;
        return this;
    }

    /**
     * <p> Sets the amount of time that Branch allows a click to remain outstanding.</p>
     *
     * @param duration A {@link Integer} value specifying the time that Branch allows a click to
     *                 remain outstanding and be eligible to be matched with a new app session.
     * @return This Builder object to allow for chaining of calls to set methods.
     */
    public LinkProperties setDuration(int duration) {
        this.matchDuration_ = duration;
        return this;
    }

    /**
     * <p>Set a name that identify the stage in an application or user flow process.</p>
     *
     * @param stage A {@link String} value identifying the stage in an application or user flow
     *              process. Should not exceed 128 characters.
     * @return This Builder object to allow for chaining of calls to set methods.
     */
    public LinkProperties setStage(String stage) {
        this.stage_ = stage;
        return this;
    }

    /**
     * <p> Sets the channel for this link. </p>
     *
     * @param channel A {@link String} denoting the channel that the link belongs to. Should not
     *                exceed 128 characters.
     * @return This Builder object to allow for chaining of calls to set methods.
     */
    public LinkProperties setChannel(String channel) {
        this.channel_ = channel;
        return this;
    }

    /**
     * <p> Sets the campaign for this link. </p>
     *
     * @param campaign A {@link String} denoting the campaign that the link belongs to. Should not
     *                 exceed 128 characters.
     * @return This Builder object to allow for chaining of calls to set methods.
     */
    public LinkProperties setCampaign(String campaign) {
        this.campaign_ = campaign;
        return this;
    }

    /**
     * Get the list of tags associated with this {@link LinkProperties}
     *
     * @return {@link ArrayList} with the tags associated with this {@link LinkProperties}
     */
    public ArrayList<String> getTags() {
        return tags_;
    }
    
    /**
     * Get all control params associated with this {@link LinkProperties}
     *
     * @return A {@link HashMap} with key value pairs for the control params associated with this {@link LinkProperties}
     */
    public HashMap<String, String> getControlParams() {
        return controlParams_;
    }

    /**
     * <p>
     * Get the amount of time that Branch allows a click to remain outstanding.
     * </p>
     *
     * @return A {@link Integer} value specifying the time that Branch allows a click to
     * remain outstanding and be eligible to be matched with a new app session.
     */
    public int getMatchDuration() {
        return matchDuration_;
    }

    /**
     * <p> Get the alias for this link. </p>
     *
     * @return A {@link String} with value alias 'alias' used to label the endpoint on the link.
     */
    public String getAlias() {
        return alias_;
    }

    /**
     * <p> Get a name that identifies the feature that the link makes use of.</p>
     *
     * @return A {@link String} value identifying the feature that the link makes use of.
     */
    public String getFeature() {
        return feature_;
    }

    /**
     * <p> Get a name that identify the stage in an application or user flow process.</p>
     *
     * @return A {@link String} value identifying the stage in an application or user flow
     * process
     */
    public String getStage() {
        return stage_;
    }

    /**
     * <p> Gets the channel for this link. </p>
     *
     * @return A {@link String} denoting the channel that the link belongs to
     */
    public String getChannel() {
        return channel_;
    }

    /**
     * <p> Gets the campaign for this link. </p>
     *
     * @return A {@link String} denoting the campaign that the link belongs to
     */
    public String getCampaign() {
        return campaign_;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public LinkProperties createFromParcel(Parcel in) {
            return new LinkProperties(in);
        }

        public LinkProperties[] newArray(int size) {
            return new LinkProperties[size];
        }
    };

    /**
     * Create a {@link LinkProperties} object based on the latest link click.
     *
     * @return A {@link LinkProperties} object based on the latest link click or a null if there is no link click registered for this session
     */
    public static LinkProperties getReferredLinkProperties() {
        LinkProperties linkProperties = null;
        Branch branchInstance = Branch.getInstance();
        if (branchInstance != null && branchInstance.getLatestReferringParams() != null) {
            JSONObject latestParam = branchInstance.getLatestReferringParams();

            try {
                if (latestParam.has("+clicked_branch_link") && latestParam.getBoolean("+clicked_branch_link")) {
                    linkProperties = new LinkProperties();
                    if (latestParam.has("~channel")) {
                        linkProperties.setChannel(latestParam.getString("~channel"));
                    }
                    if (latestParam.has("~feature")) {
                        linkProperties.setFeature(latestParam.getString("~feature"));
                    }
                    if (latestParam.has("~stage")) {
                        linkProperties.setStage(latestParam.getString("~stage"));
                    }
                    if (latestParam.has("~campaign")) {
                        linkProperties.setCampaign(latestParam.getString("~campaign"));
                    }
                    if (latestParam.has("~duration")) {
                        linkProperties.setDuration(latestParam.getInt("~duration"));
                    }
                    if (latestParam.has("$match_duration")) {
                        linkProperties.setDuration(latestParam.getInt("$match_duration"));
                    }
                    if (latestParam.has("~tags")) {
                        JSONArray tagsArray = latestParam.getJSONArray("~tags");
                        for (int i = 0; i < tagsArray.length(); i++) {
                            linkProperties.addTag(tagsArray.getString(i));
                        }
                    }

                    Iterator<String> keys = latestParam.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        if (key.startsWith("$")) {
                            linkProperties.addControlParameter(key, latestParam.getString(key));
                        }
                    }
                }
            } catch (Exception ignore) {
            }
        }
        return linkProperties;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(feature_);
        dest.writeString(alias_);
        dest.writeString(stage_);
        dest.writeString(channel_);
        dest.writeString(campaign_);
        dest.writeInt(matchDuration_);
        dest.writeSerializable(tags_);

        int controlParamSize = controlParams_.size();
        dest.writeInt(controlParamSize);
        for (HashMap.Entry<String, String> entry : controlParams_.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeString(entry.getValue());
        }

    }

    private LinkProperties(Parcel in) {
        this();
        feature_ = in.readString();
        alias_ = in.readString();
        stage_ = in.readString();
        channel_ = in.readString();
        campaign_ = in.readString();
        matchDuration_ = in.readInt();
        @SuppressWarnings("unchecked")
        ArrayList<String> tagsTemp = (ArrayList<String>) in.readSerializable();
        tags_.addAll(tagsTemp);

        int controlPramSize = in.readInt();
        for (int i = 0; i < controlPramSize; i++) {
            controlParams_.put(in.readString(), in.readString());
        }
    }
}
