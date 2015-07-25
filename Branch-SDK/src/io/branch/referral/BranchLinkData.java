package io.branch.referral;

import android.annotation.SuppressLint;
import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>
 * The {@link BranchLinkData} object is based on {@link JSONObject} and expands
 * the class to enable collections of tags.
 * </p>
 * 
 * <p>
 * Tags are returned from the server, and form the data dictionary that each
 * deep link shared via the Branch platform contains.
 * </p>
 *
 */
class BranchLinkData extends JSONObject {

    /**
     * <p>An iterable {@link Collection} of {@link String} tags associated with a deep
     * link.</p>
     *
     * @see {@link #putTags} - to set this value.
     */
    private Collection<String> tags;

    /**
     * <p>A {@link String} that can be used to label the endpoint on the link.<p>
     *
     * @see {@link #putAlias} - to set this value.
     */
    private String alias;

    /**
     * <p>
     * An {@link int} that can be used for scenarios where you want the link to
     * only deep link the first time.
     * </p>
     *
     * <b>Values:</b>
     *
     * <ul>
     * 		<li>{@link null}</li>
     * 		<li>{@link Branch#LINK_TYPE_UNLIMITED_USE}</li>
     * 		<li>{@link Branch#LINK_TYPE_ONE_TIME_USE}</li>
     * </ul>
     *
     * @see {@link #putType} - to set this value.
     */
    private int type;

    /**
     * <p>A {@link String} denoting the channel that the link belongs to. Should not exceed 128
     * characters.</p>
     *
     * <b>Example values:</b>
     *
     * <ul>
     * 		<li><i>null</i></li>
     * 		<li>"facebook"</li>
     * 		<li>"twitter"</li>
     * 		<li>"text_message"</li>
     * </ul>
     *
     * @see {@link #putChannel} - to set this value.
     */
    private String channel;

    /**
     * <p>A {@link String} value identifying the feature that the link makes use of.
     * Should not exceed 128 characters.</p>
     *
     * <b>Example values:</b>
     *
     * <ul>
     * 		<li><i>null</i></li>
     * 		<li>Branch.FEATURE_TAG_SHARE</li>
     *  	<li>Branch.FEATURE_TAG_REFERRAL</li>
     *  	<li>"unlock"</li>
     * </ul>
     *
     * @see {@link #putChannel} - to set this value.
     */
    private String feature;

    /**
     * <p>A {@link String} value identifying the stage in an application or user flow process.
     * Should not exceed 128 characters</p>
     *
     * <b>Example values:</b>
     *
     * <ul>
     * 		<li>null</li>
     * 		<li>"level_6"</li>
     * 		<li>"past_customer"</li>
     * </ul>
     *
     * @see {@link #putStage} - to set this value.
     */
    private String stage;

    /**
     * <p>A {@link String} value containing params are the deep linked params associated with the
     * link that the user clicked before showing up.</p>
     *
     * @see {@link #putParams} - to set this value.
     */
    private String params;

    /**
     * <p>An {@link int} the time that Branch allows a click to remain outstanding and be eligible
     * to be matched with a new app session.</p>
     *
     * @see {@link #putDuration} to set this variable.
     */
    private int duration;

    /**
     * <p>BranchLinkData constructor requires no parameters, and is identical to
     * that of its superclass {@link JSONObject}.</p>
     */
    public BranchLinkData() {
        super();
    }

    /**
     * <b>Use this method to add tags to the data dictionary that will go along
     * with the deep link that is being created.</b>
     *
     * @param tags				A {@link Collection} of {@link String} objects, each element
     *            				of which contains a tag and value to be added to the data
     *            				dictionary of a link.
     *
     * @throws JSONException	Each item in the tags needs to be of
     *             				valid JSON format. If it is not, a JSONException will be
     *             				thrown.
     */
    public void putTags(Collection<String> tags) throws JSONException {
        if (tags != null) {
            this.tags = tags;

            JSONArray tagArray = new JSONArray();
            for (String tag : tags)
                tagArray.put(tag);
            this.put("tags", tagArray);
        }
    }

    /**
     * <b>Adds an alias to the link.</b>
     *
     * @param alias				A {@link String} value containing the desired alias name to add.
     *
     * @throws JSONException	The parameter value must be in valid JSON format, or a
     * 							{@link JSONException} will be thrown.
     */
    public void putAlias(String alias) throws JSONException {
        if (alias != null) {
            this.alias = alias;
            this.put("alias", alias);
        }
    }

    /**
     * <b>Adds a type to the link.</b>
     *
     * @param type		An {@link Integer} value of the type specified. Valid values are:
     *
     *            		<ul>
     *            			<li>{@link Branch#LINK_TYPE_UNLIMITED_USE}</li>
     *            			<li>{@link Branch#LINK_TYPE_ONE_TIME_USE}</li>
     *            		</ul>
     *
     * @throws JSONException
     *             The parameter value must be in valid JSON format, or a
     *             {@link JSONException} will be thrown.
     */
    public void putType(int type) throws JSONException {
        if (type != 0) {
            this.type = type;
            this.put(Defines.Jsonkey.Type.getKey(), type);
        }
    }

    /**
     * <p>[Optional] You can set the duration manually. This is the time that
     * Branch allows a click to remain outstanding and be eligible to be matched
     * with a new app session.</p>
     *
     * @param duration			An {@link Integer} value in seconds.
     *
     * @throws JSONException	The parameter value must be in valid JSON format, or a
     *             				{@link JSONException} will be thrown.
     */
    public void putDuration(int duration) throws JSONException {
        if (duration > 0) {
            this.duration = duration;
            this.put("duration", duration);
        }
    }

    /**
     * <b>[Optional] The channel in which the link will be shared. eg: "facebook",
     * "text_message".</b>
     *
     * @param channel			A {@link String} value containing the channel which the link
     *            				belongs to. (max 128 characters).
     *
     * @throws JSONException	The parameter value must be in valid JSON format, or a
     *             				{@link JSONException} will be thrown.
     */
    public void putChannel(String channel) throws JSONException {
        if (channel != null) {
            this.channel = channel;
            this.put("channel", channel);
        }
    }

    /**
     * <b>[Optional] The feature in which the link will be used. eg: "invite",
     * "referral", "share", "gift", etc.</b>
     *
     * @param feature			A {@link String} specifying the feature. (max 128 characters).
     *
     * @throws JSONException	The parameter value must be in valid JSON format, or a
     *             				{@link JSONException} will be thrown.
     */
    public void putFeature(String feature) throws JSONException {
        if (feature != null) {
            this.feature = feature;
            this.put("feature", feature);
        }
    }

    /**
     * <b>A string value that represents the stage of the user in the app. eg:
     * "level1", "logged_in", etc.</b>
     *
     * @param stage				A {@link String} value specifying the stage.
     *
     * @throws JSONException	The parameter value must be in valid JSON format, or a
     * 							{@link JSONException} will be thrown.
     */
    public void putStage(String stage) throws JSONException {
        if (stage != null) {
            this.stage = stage;
            this.put("stage", stage);
        }
    }

    /**
     * <p>Any other params to be added; you can define your own.</p>
     *
     * @param params			A {@link String} containing other params in JSON format.
     *
     * @throws JSONException
     *             The parameter value must be in valid JSON format, or a
     *             {@link JSONException} will be thrown.
     */
    public void putParams(String params) throws JSONException {
        this.params = params;
        this.put(Defines.Jsonkey.Data.getKey(), params);
    }

    /**
     * <p>Compares a BranchLinkData object by instance
     * ("is the object the exact same one in memory") and by associated
     * attributes ("is this object identically configured?")</p>
     *
     * @param obj		A {@link BranchLinkData} object to be compared to the one that
     * 					this method belongs to.
     *
     * @return 			Returns true if identical, false if different.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BranchLinkData other = (BranchLinkData) obj;
        if (alias == null) {
            if (other.alias != null)
                return false;
        } else if (!alias.equals(other.alias))
            return false;
        if (channel == null) {
            if (other.channel != null)
                return false;
        } else if (!channel.equals(other.channel))
            return false;
        if (feature == null) {
            if (other.feature != null)
                return false;
        } else if (!feature.equals(other.feature))
            return false;
        if (params == null) {
            if (other.params != null)
                return false;
        } else if (!params.equals(other.params))
            return false;
        if (stage == null) {
            if (other.stage != null)
                return false;
        } else if (!stage.equals(other.stage))
            return false;
        if (type != other.type)
            return false;
        if (duration != other.duration)
            return false;

        if (tags == null) {
            if (other.tags != null)
                return false;
        } else if (!tags.toString().equals(other.tags.toString()))
            return false;

        return true;
    }

    /**
     * <p>
     * Calculates the hash for this object as currently configured and returns
     * the resultant {@link Integer}.
     * </p>
     *
     * <p>
     * The hash value is a combination of the hashCode of:
     * </p>
     *
     * <ul>
     * 		<li>alias</li>
     * 		<li>channel</li>
     * 		<li>feature</li>
     * 		<li>stage</li>
     * 		<li>params</li>
     * </ul>
     *
     * <p>
     * Changing any of these attributes will change the hashCode, making
     * comparison straightforward.
     * </p>
     *
     */
    @SuppressLint("DefaultLocale")
    @Override
    public int hashCode() {
        int result = 1;
        int prime = 19;

        result = prime * result + this.type;
        result = prime * result
                + ((alias == null) ? 0 : alias.toLowerCase().hashCode());
        result = prime * result
                + ((channel == null) ? 0 : channel.toLowerCase().hashCode());
        result = prime * result
                + ((feature == null) ? 0 : feature.toLowerCase().hashCode());
        result = prime * result
                + ((stage == null) ? 0 : stage.toLowerCase().hashCode());
        result = prime * result
                + ((params == null) ? 0 : params.toLowerCase().hashCode());
        result = prime * result + this.duration;

        if (this.tags != null) {
            for (String tag : this.tags) {
                result = prime * result + tag.toLowerCase().hashCode();
            }
        }

        return result;
    }

}
