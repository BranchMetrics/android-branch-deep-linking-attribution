package io.branch.referral;

import org.json.JSONArray;
import org.json.JSONObject;

public class ServerResponse {
	private int statusCode_;
	private String tag_;
	private Object post_;
	private BranchLinkData linkData_;
	
	/**
	 * <p>Constructor method for the {@link ServerResponse} class that allows for the instantiation 
	 * of a server response object as a direct result of a server call. Omits {@link BranchLinkData}
	 * , so is used where that value is null or unavailable.</p>
	 * 
	 * @param tag A {@link String} value of the <i>Tag</i> attribute of the current link.
	 * @param statusCode {@link Integer} value of the HTTP status code.
	 */
	public ServerResponse(String tag, int statusCode) {
		this(tag, statusCode, null);
	}
	
	/**
	 * <p>Main constructor method for the {@link ServerResponse} class that allows for the instantiation 
	 * of a server response object as a direct result of a server call.</p>
	 * 
	 * @param tag A {@link String} value of the <i>Tag</i> attribute of the current link.
	 * @param statusCode {@link Integer} value of the HTTP status code.
	 * @param linkData A {@link BranckLinkData} object containing data attributes associated with a Branch link.
	 */
	public ServerResponse(String tag, int statusCode, BranchLinkData linkData) {
		tag_ = tag;
		statusCode_ = statusCode;
		linkData_ = linkData;
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
	 * <p>Gets the {@link HttpStatus} code of the current response.</p>
	 * 
	 * @see {@link #getObject}
	 * @see {@link #getArray} 
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
	 * @see {@link #getObject}
	 * @see {@link #getArray}
	 * 
	 * @param post Generic {@link Object} instance containing post data associated with current 
	 * response.  
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
			return (JSONObject)post_;
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
			return (JSONArray)post_;
		}
		
		return null;
	}
	
	/**
	 * <p>Gets a {@link BranchLinkData} object containing the data associated with the current Branch 
	 * link.</p>
	 * 
	 * @return A {@link BranckLinkData} object containing data attributes associated with a Branch link.
	 */
	public BranchLinkData getLinkData() {
		return linkData_;
	}
}
