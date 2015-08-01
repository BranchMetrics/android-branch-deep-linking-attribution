package io.branch.referral.builders;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.branch.referral.Branch;

/**
 * <p>
 * Abstract for creating builder for getting a short url with Branch. This builder provide an easy and flexible way to configure and create
 * a short url Synchronously or asynchronously.
 * </p>
 */
public class BranchShortUrlBuilder {

    /* Deep linked params associated with the link that will be passed into a new app session when clicked */
    protected JSONObject params_;
    /* Name of the channel that the link belongs to. */
    protected String channel_;
    /* Name that identifies the feature that the link makes use of. */
    protected String feature_;
    /* Name that identify the stage in an application or user flow process. */
    protected String stage_;
    /* Link 'alias' can be used to label the endpoint on the link. */
    protected String alias_;
    /* Number of times the link should perform deep link */
    protected int type_ = Branch.LINK_TYPE_UNLIMITED_USE;
    /* The amount of time that Branch allows a click to remain outstanding. */
    protected int duration_ = 0;
    /* An iterable collection of name associated with a deep link. */
    protected ArrayList<String> tags_;
    /* Branch Instance */
    Branch branchReferral_;

    /**
     * <p>
     * Creates an instance of {@link BranchShortUrlBuilder} to create short links synchronously.
     * {@see getShortUrl() }
     * </p>
     */
    public BranchShortUrlBuilder() {
        branchReferral_ = Branch.getInstance();
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
     * @return A {@link BranchShortUrlBuilder} instance.
     */
    public BranchShortUrlBuilder setAlias(String alias) {
        this.alias_ = alias;
        return this;
    }

    /**
     * <p> Sets the channel for this link. </p>
     *
     * @param channel A {@link String} denoting the channel that the link belongs to. Should not
     *                exceed 128 characters.
     * @return A {@link BranchShortUrlBuilder} instance.
     */
    public BranchShortUrlBuilder setChannel(String channel) {
        this.channel_ = channel;
        return this;
    }

    /**
     * <p> Sets the amount of time that Branch allows a click to remain outstanding.</p>
     *
     * @param duration A {@link Integer} value specifying the time that Branch allows a click to
     *                 remain outstanding and be eligible to be matched with a new app session.
     * @return A {@link BranchShortUrlBuilder} instance.
     */
    public BranchShortUrlBuilder setDuration(int duration) {
        this.duration_ = duration;
        return this;
    }

    /**
     * <p> Set a name that identifies the feature that the link makes use of.</p>
     *
     * @param feature A {@link String} value identifying the feature that the link makes use of.
     *                Should not exceed 128 characters.
     * @return A {@link BranchShortUrlBuilder} instance.
     */
    public BranchShortUrlBuilder setFeature(String feature) {
        this.feature_ = feature;
        return this;
    }

    /**
     * <p> Set the parameters associated with the link.</p>
     *
     * @param parameters A {@link JSONObject} value containing the deep linked params associated with
     *                   the link that will be passed into a new app session when clicked.
     *                   {@see addParameters} if you want to set parameters as individual key value.
     * @return A {@link BranchShortUrlBuilder} instance.
     */
    public BranchShortUrlBuilder setParameters(JSONObject parameters) {
        this.params_ = parameters;
        return this;
    }

    /**
     * <p>Set a name that identify the stage in an application or user flow process.</p>
     *
     * @param stage A {@link String} value identifying the stage in an application or user flow
     *              process. Should not exceed 128 characters.
     * @return A {@link BranchShortUrlBuilder} instance.
     */
    public BranchShortUrlBuilder setStage(String stage) {
        this.stage_ = stage;
        return this;
    }

    /**
     * <p>Adds a tag to the iterable collection of name associated with a deep link.</p>
     *
     * @param tag {@link String} tags associated with a deep link.
     * @return A {@link BranchShortUrlBuilder} instance.
     */
    public BranchShortUrlBuilder addTag(String tag) {
        if (this.tags_ == null) {
            tags_ = new ArrayList<String>();
        }
        this.tags_.add(tag);
        return this;
    }

    /**
     * <p>Set the number of times the link should perform deep link.</p>
     *
     * @param type {@link int} that can be used for scenarios where you want the link to
     *             only deep link the first time.
     * @returnA {@link BranchShortUrlBuilder} instance.
     */
    public BranchShortUrlBuilder setType(int type) {
        this.type_ = type;
        return this;
    }

    /**
     * <p>Adds the the given key value pair to the parameters associated with this link.</p>
     *
     * @param key   A {@link String} with value of key for the parameter
     * @param value A {@link String} with value of value for the parameter
     * @returnA {@link BranchShortUrlBuilder} instance.
     */
    public BranchShortUrlBuilder addParameters(String key, String value) {
        try {
            if (this.params_ == null) {
                this.params_ = new JSONObject();
            }
            this.params_.put(key, value);
        } catch (JSONException ignore) {

        }
        return this;
    }

    ///------------------------- link Build methods---------------------------///

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers, via a synchronous
     * call; with a duration specified within which an app session should be matched to the link.</p>
     *
     * @return A {@link String} containing the resulting short URL. Null is returned in case of an error or if Branch is not initialised.
     */
    public String getShortUrl() {
        String shortUrl = null;
        if (branchReferral_ != null) {
            shortUrl = branchReferral_.generateShortLink(alias_, type_, duration_, tags_, channel_, feature_, stage_, stringifyParams(params_), null, false);
        }
        return shortUrl;
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers, via a asynchronous
     * call; The {@link Branch.BranchLinkCreateListener} is called back with the url when the url is generated.</p>
     *
     * @param callback A {@link Branch.BranchLinkCreateListener} callback instance that will trigger
     */

    public void generateShortLink(Branch.BranchLinkCreateListener callback) {
        String shortUrl = null;
        if (branchReferral_ != null) {
            branchReferral_.generateShortLink(alias_, type_, duration_, tags_, channel_, feature_, stage_, stringifyParams(params_), callback, true);
        }
    }

    //------------------------- Utility methods -----------------------//

    protected static String stringifyParams(JSONObject params) {
        if (params == null) {
            params = new JSONObject();
        }
        try {
            params.put("source", "android");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return params.toString();
    }
}
