package io.branch.search;

import android.app.Application;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;

/**
 * Created by sojanpr on 2/27/17.
 * <p>
 *     Class for representing a local or recommended app content that matches a Branch search query.
 *     Class holds a reference to the {@link AppResult} pointing to the app which content belongs to
 * </p>
 */
public class ContentResult implements Parcelable {

    private AppResult appResult;
    private final  String title;
    private final String description;
    private final String imageUrl;
    private String contentDeepLinkUrl;
    private String category;
    private final HashMap<String,String> metadata;

    public ContentResult(String title, String description, String imageUrl) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        contentDeepLinkUrl = "";
        category = "";
        metadata = new HashMap<>();
        appResult = null;
    }

    public ContentResult setContentDeeplinkUrl(String deeplinkUrl) {
        this.contentDeepLinkUrl = deeplinkUrl;
        return this;
    }

    public ContentResult addMetadata(String key, String value) {
        this.metadata.put(key,value);
        return this;
    }
    public ContentResult setAppResult(AppResult appResult){
        this.appResult = appResult;
        return this;
    }

    /**
     * Get the app result associated with this content
     * @return {@link AppResult} for this content
     */
    public AppResult getAppResult() {
        return appResult;
    }

    /**
     * Category for this content
     * @return {@link String} content category
     */
    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get the image URL for the content
     * @return {@link String } image url
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Get the title for the content
     * @return {@link String} with title value
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get the metadata associated with this content
     * @return {@link HashMap<String,String>} with metadata
     */
    public HashMap<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.appResult, flags);
        dest.writeString(this.title);
        dest.writeString(this.description);
        dest.writeString(this.imageUrl);
        dest.writeString(this.contentDeepLinkUrl);
        dest.writeString(this.category);
        dest.writeSerializable(this.metadata);
    }

    protected ContentResult(Parcel in) {
        this.appResult = in.readParcelable(AppResult.class.getClassLoader());
        this.title = in.readString();
        this.description = in.readString();
        this.imageUrl = in.readString();
        this.contentDeepLinkUrl = in.readString();
        this.category = in.readString();
        this.metadata = (HashMap<String, String>) in.readSerializable();
    }

    public static final Parcelable.Creator<ContentResult> CREATOR = new Parcelable.Creator<ContentResult>() {
        @Override
        public ContentResult createFromParcel(Parcel source) {
            return new ContentResult(source);
        }

        @Override
        public ContentResult[] newArray(int size) {
            return new ContentResult[size];
        }
    };
}
