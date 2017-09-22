package io.branch.referral;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Class providing the structure of a HTTP response as recieved from the Branch API.
 *
 * Supports the following methods:
 * <ul>
 * <li>{@link ServerResponse#getTag()}</li>
 * <li>{@link ServerResponse#getStatusCode()}</li>
 * <li>{@link ServerResponse#setPost(Object)}</li>
 * <li>{@link ServerResponse#getObject()}</li>
 * <li>{@link ServerResponse#getArray()}</li>
 * </ul>
 */
public class ServerResponse {

    /**
     * Resultant HTTP status code of the corresponding {@link ServerRequest}.
     */
    private int statusCode_;

    /**
     * Tag associated with the response, as supplied by the {@link ServerRequest} object.
     */
    private String tag_;

    /**
     * Generic {@link Object} instance containing the data initially attached to a link. Must be
     * cast to a type before use to allow the appropriate "get" methods to be used.
     */
    private Object post_;


    /**
     * <p>Main constructor method for the {@link ServerResponse} class that allows for the instantiation
     * of a server response object as a direct result of a server call.</p>
     *
     * @param tag        A {@link String} value of the <i>Tag</i> attribute of the current link.
     * @param statusCode {@link Integer} value of the HTTP status code.
     */
    public ServerResponse(String tag, int statusCode) {
        tag_ = tag;
        statusCode_ = statusCode;
    }

    /**
     * <p>Gets the {@link String} value of the <i>Tag</i> attribute of the current link.</p>
     *
     * @return A {@link String} value of the <i>Tag</i> attribute of the current link.
     */
    public String getTag() {
        return tag_;
    }

    /**
     * <p>Gets the HttpStatus code of the current response.</p>
     *
     * @return {@link Integer} value of the HTTP status code.
     */
    public int getStatusCode() {
        return statusCode_;
    }

    /**
     * <p>Sets the post data attached to the current server request, as a generic {@link Object}
     * instance. This object can be type-cast by other methods within this class:</p>
     *
     * @param post Generic {@link Object} instance containing post data associated with current
     *             response.
     */
    public void setPost(Object post) {
        post_ = post;
    }

    /**
     * <p>Checks whether the post data associated with the current request is an instance of a
     * {@link JSONObject} object, and if so type-casts it to the corresponding class type.</p>
     *
     * @return A {@link JSONObject} containing the post data sent with a server request, or a null
     * value if the post data is not of the {@link JSONObject} type.
     */
    public JSONObject getObject() {
        if (post_ instanceof JSONObject) {
            return (JSONObject) post_;
        }

        return null;
    }

    /**
     * <p>Checks whether the post data associated with the current request is an instance of a
     * {@link JSONArray} object, and if so type-casts it to the corresponding class type.</p>
     *
     * @return A {@link JSONArray} containing the post data sent with a server request, or a null
     * value if the post data is not of the {@link JSONArray} type.
     */
    public JSONArray getArray() {
        if (post_ instanceof JSONArray) {
            return (JSONArray) post_;
        }

        return null;
    }


    /**
     * Get the reason for failure if there any
     *
     * @return A {@link String } value with failure reason
     */
    public String getFailReason() {
        String causeMsg = "";
        try {
            JSONObject postObj = getObject();
            if (postObj != null
                    && postObj.has("error")
                    && postObj.getJSONObject("error").has("message")) {
                causeMsg = postObj.getJSONObject("error").getString("message");
                if (causeMsg != null && causeMsg.trim().length() > 0) {
                    causeMsg = causeMsg + ".";
                }
            }
        } catch (Exception ignore) {
        }
        return causeMsg;
    }

}
