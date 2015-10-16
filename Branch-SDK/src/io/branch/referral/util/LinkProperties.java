package io.branch.referral.util;

import android.app.Activity;
import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;

import io.branch.referral.Branch;

/**
 * <p/>
 * Class for representing properties for a deep link.
 * Use this class to specify the properties of a deep link that you want to create.
 *
 * @see io.branch.referral.indexing.BranchUniversalObject#getShortUrl(Context, LinkProperties)
 * @see io.branch.referral.indexing.BranchUniversalObject#generateShortUrl(Context, LinkProperties, Branch.BranchLinkCreateListener)
 * @see io.branch.referral.indexing.BranchUniversalObject#showShareSheet(Activity, LinkProperties, ShareSheetStyle, Branch.BranchLinkShareListener)
 * </p>
 */
public class LinkProperties {
    private final ArrayList<String> tags_;
    private String feature_;
    private String alias_;
    private String stage_;
    private int matchDuration_;
    private final HashMap<String, String> controlParams_;
    private String channel_;

    /**
     * Create an instance of {@link LinkProperties}
     */
    public LinkProperties() {
        tags_ = new ArrayList<>();
        feature_ = "Share";
        controlParams_ = new HashMap<>();
        alias_ = null;
        stage_ = null;
        matchDuration_ = 0;
        channel_ = null;
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
     * <p>Adds a tag to the iterable collection of name associated with a deep link.</p>
     *
     * @param tag {@link String} tags associated with a deep link.
     * @return This Builder object to allow for chaining of calls to set methods.
     */
    public LinkProperties addTag(String tag) {
        this.tags_.add(tag);
        return this;
    }

    /**
     * <p>Adds the the given key value pair to the parameters associated with this link.</p>
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


    public ArrayList<String> getTags() {
        return tags_;
    }

    public HashMap<String, String> getControlParams() {
        return controlParams_;
    }

    public int getMatchDuration() {
        return matchDuration_;
    }

    public String getAlias() {
        return alias_;
    }

    public String getFeature() {
        return feature_;
    }

    public String getStage() {
        return stage_;
    }

    public String getChannel() {
        return channel_;
    }
}
