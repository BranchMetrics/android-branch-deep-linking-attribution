package io.branch.referral.util;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Defines;

/**
 * Created by sojanpr on 7/5/17.
 * <p>
 * Builder class for tracking a standard event. This class builds a Branch standard event and
 * send to Branch. Please see {@link #track(Context)} method to execute tracking
 * </p>
 */
public class TrackStandardEventBuilder extends TrackEventBuilder<TrackStandardEventBuilder> {

    private BranchEventData eventData;
    private final List<BranchUniversalObject> buoList;

    public TrackStandardEventBuilder(BranchStandardEvents branchStandardEvent) {
        super(Defines.RequestPath.TrackStandardEvent.getPath(), branchStandardEvent.getName());
        eventData = null;
        buoList = new ArrayList<>();
    }

    public TrackStandardEventBuilder addEventData(BranchEventData data) {
        eventData = data;
        return this;
    }

    public TrackStandardEventBuilder addContentItems(BranchUniversalObject... contentItems) {
        Collections.addAll(buoList, contentItems);
        return this;
    }

    public TrackStandardEventBuilder addContentItems(List<BranchUniversalObject> contentItems) {
        buoList.addAll(contentItems);
        return this;
    }

    @Override
    protected JSONObject getRequestBody() {
        JSONObject reqBody = new JSONObject();

        try {
            reqBody.put(Defines.Jsonkey.Name.getKey(), eventName);
            if (customData.length() > 0) {
                reqBody.put(Defines.Jsonkey.CustomData.getKey(), customData);
            }
            if (eventData != null) {
                reqBody.put(Defines.Jsonkey.EventData.getKey(), eventData);
            }
            if (buoList.size() > 0) {
                JSONArray contentItemsArray = new JSONArray();
                reqBody.put(Defines.Jsonkey.ContentItems.getKey(), contentItemsArray);
                for (BranchUniversalObject buo : buoList) {
                    contentItemsArray.put(buo.convertToJson());
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return reqBody;
    }
}


