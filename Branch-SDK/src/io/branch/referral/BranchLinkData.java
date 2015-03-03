package io.branch.referral;

import android.annotation.SuppressLint;
import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class BranchLinkData extends JSONObject {
	
	private Collection<String> tags;	
	private String alias;
	private int type;
	private String channel;
	private String feature;
	private String stage;
	private String params;
	private int duration;
	
	public BranchLinkData() {
		super();
	}
	
	public void putTags(Collection<String> tags) throws JSONException {
		if (tags != null) {
			this.tags = tags;
			
			JSONArray tagArray = new JSONArray();
			for (String tag : tags)
				tagArray.put(tag);
			this.put("tags", tagArray);
		}
	}

	public void putAlias(String alias) throws JSONException {
	    if (alias != null) {
	        this.alias = alias;
	        this.put("alias", alias);
	    }
	}

	public void putType(int type) throws JSONException {
	    if (type != 0) {
	        this.type = type;
	        this.put("type", type);
	    }
	}
	
	public void putDuration(int duration) throws JSONException {
	    if (duration > 0) {
	        this.duration = duration;
	        this.put("duration", duration);
	    }
	}

	public void putChannel(String channel) throws JSONException {
	    if (channel != null) {
	        this.channel = channel;
	        this.put("channel", channel);
	    }
	}

	public void putFeature(String feature) throws JSONException {
	    if (feature != null) {
	        this.feature = feature;
	        this.put("feature", feature);
	    }
	}

	public void putStage(String stage) throws JSONException {
	    if (stage != null) {
	        this.stage = stage;
	        this.put("stage", stage);
	    }
	}

	public void putParams(String params) throws JSONException {
	    this.params = params;
	    this.put("data", params);
	}

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
	
	@SuppressLint("DefaultLocale")
	@Override
    public int hashCode() {
		int result = 1;
	    int prime = 19;
	    
	    result = prime * result + this.type;
	    result = prime * result + ((alias == null) ? 0 : alias.toLowerCase().hashCode());
	    result = prime * result + ((channel == null) ? 0 : channel.toLowerCase().hashCode());
	    result = prime * result + ((feature == null) ? 0 : feature.toLowerCase().hashCode());
	    result = prime * result + ((stage == null) ? 0 : stage.toLowerCase().hashCode());
	    result = prime * result + ((params == null) ? 0 : params.toLowerCase().hashCode());
	    result = prime * result + this.duration;
	    
	    if (this.tags != null) {
	    	for (String tag : this.tags) {
	    		result = prime * result + tag.toLowerCase().hashCode();
	    	}
	    }
	    
	    return result;
    }

}
