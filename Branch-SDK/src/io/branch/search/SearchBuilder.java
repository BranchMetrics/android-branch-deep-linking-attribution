package io.branch.search;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sojanpr on 3/1/17.
 * <p>
 * Builder for Branch local search query. Use this builder to customise your search for contents
 * with Branch search
 * </p>
 */
public class SearchBuilder implements Parcelable {
    private static final int DEF_MAX_APP_RESULT = 3;
    private static final int DEF_MAX_CONTENT_RESULT = 5;
    private final String query;
    private final IBranchSearchEvent callback;
    private final List<String> packageNames;
    private int maxAppResults;
    private int maxContentResults;

    /**
     * Builder for creating a query for get search result from Branch.
     * @see  {@link #search()} method to execute a search
     * @param query {@link String} a keyword to search
     * @param callback {@link io.branch.search.SearchBuilder.IBranchSearchEvent} instance for listening the search results
     */
    public SearchBuilder(String query, IBranchSearchEvent callback) {
        this.query = query;
        this.callback = callback;
        packageNames = new ArrayList<>();
        maxAppResults = DEF_MAX_APP_RESULT;
        maxContentResults = DEF_MAX_CONTENT_RESULT;
    }

    /**
     * Set the maximum number of {@link AppResult} int the {@link BranchSearchResult}
     * @param maxAppResult maximum number of app result
     * @return SearchBuilder instance for  chaining
     */
    public SearchBuilder setMaxAppResults(int maxAppResult) {
        this.maxAppResults = maxAppResult;
        return this;
    }

    /**
     * Set the maximum number of {@link ContentResult} int the {@link BranchSearchResult}
     * @param maxContentResults maximum number of content result
     * @return SearchBuilder instance for  chaining
     */
    public SearchBuilder setMaxContentResults(int maxContentResults) {
        this.maxContentResults = maxContentResults;
        return this;
    }

    /**
     * Add the preferred  apps to search with. Setting this option will return only result associated with the specified app
     * @param packageName package name for the preferred app
     * @return SearchBuilder instance for  chaining
     */
    public SearchBuilder addPreferredApp(String packageName) {
        packageNames.add(packageName);
        return this;
    }

    /**
     * Execute a search with Branch with given query and configurations.
     * Results are called back with IBranchSearchEvent
     * @return SearchBuilder instance for  chaining
     */
    public boolean search() {
        return BranchSearchServiceConnection.getInstance().search(this);
    }


    public IBranchSearchEvent getCallback() {
        return callback;
    }

    public int getMaxAppResults() {
        return maxAppResults;
    }

    public int getMaxContentResults() {
        return maxContentResults;
    }

    public String getQuery() {
        return query;
    }

    public List<String> getPreferredApps() {
        return packageNames;
    }

    //----------------Parcellable implementation---------------//
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.query);
        dest.writeStringList(this.packageNames);
        dest.writeInt(this.maxAppResults);
        dest.writeInt(this.maxContentResults);
    }

    protected SearchBuilder(Parcel in) {
        this.query = in.readString();
        this.packageNames = in.createStringArrayList();
        this.maxAppResults = in.readInt();
        this.maxContentResults = in.readInt();
        callback = null;
    }

    public static final Parcelable.Creator<SearchBuilder> CREATOR = new Parcelable.Creator<SearchBuilder>() {
        @Override
        public SearchBuilder createFromParcel(Parcel source) {
            return new SearchBuilder(source);
        }

        @Override
        public SearchBuilder[] newArray(int size) {
            return new SearchBuilder[size];
        }
    };


    /**
     * Interface for listening to search results
     */
    public interface IBranchSearchEvent {
        /**
         * Called when there is a search result is available
         *
         * @param query        {@link String} query for search
         * @param searchResult {@link BranchSearchResult} with the result for the search
         */
        void onBranchSearchEvents(String query, BranchSearchResult searchResult);
    }
}
