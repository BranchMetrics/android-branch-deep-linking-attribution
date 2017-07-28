package io.branch.referral.util;

import android.text.TextUtils;

/**
 * Created by sojanpr on 7/7/17.
 * Enumerations for BranchContentSchema. This enumearates all content schemas supported by Branch
 */
public enum BranchContentSchema {
    COMMERCE_AUCTION,
    COMMERCE_BUSINESS,
    COMMERCE_OTHER,
    COMMERCE_PRODUCT,
    COMMERCE_RESTAURANT,
    COMMERCE_SERVICE,
    COMMERCE_TRAVEL_FLIGHT,
    COMMERCE_TRAVEL_HOTEL,
    COMMERCE_TRAVEL_OTHER,
    GAME_STATE,
    MEDIA_IMAGE,
    MEDIA_MIXED,
    MEDIA_MUSIC,
    MEDIA_OTHER,
    MEDIA_VIDEO,
    OTHER,
    TEXT_ARTICLE,
    TEXT_BLOG,
    TEXT_OTHER,
    TEXT_RECIPE,
    TEXT_REVIEW,
    TEXT_SEARCH_RESULTS,
    TEXT_STORY,
    TEXT_TECHNICAL_DOC;

    public static BranchContentSchema getValue(String name) {
        BranchContentSchema schema = null;
        if (!TextUtils.isEmpty(name)) {
            for (BranchContentSchema contentSchema : BranchContentSchema.values()) {
                if (contentSchema.name().equalsIgnoreCase(name)) {
                    schema = contentSchema;
                    break;
                }
            }
        }
        return schema;
    }
}
