package io.branch.search;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sojanpr on 3/2/17.
 * <p>
 * Builder for Branch local search query. Use this builder to customise your search for contents
 * with Branch search
 * </p>
 */
public class RecommendationBuilder implements Parcelable {
    private static final int DEF_MAX_APP_REC_COUNT = 5;
    private static final int DEF_MAX_CONTENT_REC_COUNT = 4;
    private int maxAppRecommendationCount;
    private int maxContentRecommendationCount;
    private final IRecommendationEvents callback;

    /**
     * <p>
     * Builder for getting content and app recommendations from Branch.
     * Branch will provide content app recommendation based on the configuration. The
     * contents and apps are ranked with recent interactions and other contextual parameters
     * </p>
     *
     * @param callback {@link io.branch.search.RecommendationBuilder.IRecommendationEvents} instance to listen to recommendation events
     */
    public RecommendationBuilder(IRecommendationEvents callback) {
        this.callback = callback;
        this.maxAppRecommendationCount = DEF_MAX_APP_REC_COUNT;
        this.maxContentRecommendationCount = DEF_MAX_CONTENT_REC_COUNT;
    }

    /**
     * (
     * Set the maximum number of recommended apps in the recommendations
     *
     * @param maxAppRecommendations Max num of apps in the recommended apps list
     * @return RecommendationBuilder instance for chaining
     */
    public RecommendationBuilder setMaxAppRecommendations(int maxAppRecommendations) {
        this.maxAppRecommendationCount = maxAppRecommendations;
        return this;
    }

    /**
     * (
     * Set the maximum number of recommended contents in the recommendations
     *
     * @param maxContentRecommendations Max num of contents in the recommended content list
     * @return RecommendationBuilder instance for chaining
     */
    public RecommendationBuilder setMaxContentRecommendations(int maxContentRecommendations) {
        this.maxContentRecommendationCount = maxContentRecommendations;
        return this;
    }

    /**
     * Gets the app and content recommendations based on the configurations specified
     * Recommendation results are returned back with {@link io.branch.search.RecommendationBuilder.IRecommendationEvents#onRecommendationsAvailable(BranchSearchResult)}
     * @return {@code true} if get recommendation task succeeded.
     */
    public boolean getRecommendations(){
        return BranchSearchServiceConnection.getInstance().getRecommendations(this);
    }


    public int getMaxAppRecommendationCount() {
        return maxAppRecommendationCount;
    }

    public int getMaxContentRecommendationCount() {
        return maxContentRecommendationCount;
    }

    public IRecommendationEvents getCallback() {
        return callback;
    }



    //----------- Parcelable implementation-------------//
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.maxAppRecommendationCount);
        dest.writeInt(this.maxContentRecommendationCount);
    }

    protected RecommendationBuilder(Parcel in) {
        this.maxAppRecommendationCount = in.readInt();
        this.maxContentRecommendationCount = in.readInt();
        callback = null;
    }

    public static final Parcelable.Creator<RecommendationBuilder> CREATOR = new Parcelable.Creator<RecommendationBuilder>() {
        @Override
        public RecommendationBuilder createFromParcel(Parcel source) {
            return new RecommendationBuilder(source);
        }

        @Override
        public RecommendationBuilder[] newArray(int size) {
            return new RecommendationBuilder[size];
        }
    };

    /**
     * Callback interface for listening recommendation events
     */
    public interface IRecommendationEvents {
        /**
         * Called when there is content or app recommendation available from Branch
         *
         * @param branchSearchResult {@link BranchSearchResult} with content and app recommendations
         */
        void onRecommendationsAvailable(BranchSearchResult branchSearchResult);
    }
}
