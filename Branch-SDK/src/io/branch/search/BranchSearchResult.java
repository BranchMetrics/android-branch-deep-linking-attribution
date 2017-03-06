package io.branch.search;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;


/**
 * Created by sojanpr on 11/14/16.
 * <p>
 * Class  for representing the search results returned by Branch Search Service.
 * Search Result contains both {@code List<AppResult>} and  {@code List<ContentResult>}
 * </p>
 */
public class BranchSearchResult implements Parcelable {
    private final List<AppResult> appResults;
    private final List<ContentResult> contentResults;

    public BranchSearchResult(List<AppResult> appResults, List<ContentResult> contentResults) {
        this.appResults = appResults;
        this.contentResults = contentResults;
    }

    /**
     * Add an {@link AppResult} to the {@code List<AppResult>}
     *
     * @param appResult {@link AppResult} instance that matches the search query
     */
    public void addAppResult(AppResult appResult) {
        this.appResults.add(appResult);
    }

    /**
     * Add an {@link AppResult} to the {@code List<AppResult>} at given position in the list
     *
     * @param position  0 based index to put the {@link AppResult} in the list
     * @param appResult {@link AppResult} instance that matches the search query
     */
    public void addAppResult(int position, AppResult appResult) {
        this.appResults.add(position, appResult);
    }

    /**
     * Add an {@link ContentResult} to the {@code List<ContentResult>} at given position in the list
     *
     * @param contentResult {@link ContentResult} instance that matches the search query
     */
    public void addContentResult(ContentResult contentResult) {
        this.contentResults.add(contentResult);
    }

    /**
     * Add an {@link ContentResult} to the {@code List<ContentResult>} at given position in the list
     *
     * @param position      0 based index to put the {@link ContentResult} in the list
     * @param contentResult {@link ContentResult} instance that matches the search query
     */
    public void addContentResult(int position, ContentResult contentResult) {
        this.contentResults.add(contentResult);
    }

    /**
     * Get the {@code List<AppResult>} that matches the search query
     *
     * @return {@code List<AppResult>} with matching app results
     */
    public List<AppResult> getAppResults() {
        return appResults;
    }

    /**
     * Get the {@code List<ContentResult>} that matches the search query
     *
     * @return {@code List<ContentResult>} with matching content results
     */
    public List<ContentResult> getContentResults() {
        return contentResults;
    }


    //----------Parcelable implementation-----------------//

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(this.appResults);
        dest.writeTypedList(this.contentResults);
    }

    protected BranchSearchResult(Parcel in) {
        this.appResults = in.createTypedArrayList(AppResult.CREATOR);
        this.contentResults = in.createTypedArrayList(ContentResult.CREATOR);
    }

    public static final Parcelable.Creator<BranchSearchResult> CREATOR = new Parcelable.Creator<BranchSearchResult>() {
        @Override
        public BranchSearchResult createFromParcel(Parcel source) {
            return new BranchSearchResult(source);
        }

        @Override
        public BranchSearchResult[] newArray(int size) {
            return new BranchSearchResult[size];
        }
    };
}

