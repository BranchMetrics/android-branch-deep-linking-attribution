package io.branch.referral.util;

/**
 * Created by sojanpr on 7/7/17.
 * Enumerations for BranchContentSchema. This enumearates all content schemas supported by Branch
 */
public enum BranchContentSchema {
    COMMERCE_PRODUCT("COMMERCE_PRODUCT"),
    TEXT_ARTICLE("TEXT_ARTICLE"),
    TEXT_BLOG("TEXT_BLOG"),
    MEDIA_IMAGE("MEDIA_IMAGE"),
    MEDIA_MUSIC("MEDIA_MUSIC"),
    MEDIA_VIDEO("MEDIA_VIDEO"),
    COMMERCE_AUCTION("COMMERCE_AUCTION"),
    COMMERCE_BUSINESS("COMMERCE_BUSINESS"),
    COMMERCE_RESTAURANT("COMMERCE_RESTAURANT"),
    COMMERCE_SERVICE("COMMERCE_SERVICE"),
    COMMERCE_TRAVEL_HOTEL("COMMERCE_TRAVEL_HOTEL"),
    COMMERCE_TRAVEL_FLIGHT("COMMERCE_TRAVEL_FLIGHT"),
    COMMERCE_TRAVEL_OTHER("COMMERCE_TRAVEL_OTHER"),
    TEXT_RECIPE("TEXT_RECIPE"),
    OTHER_GAME_STATE("OTHER_GAME_STATE"),
    OTHER_ACTION("OTHER_ACTION"),
    MEDIA_MIXED("MEDIA_MIXED"),
    TEXT_STORY("TEXT_STORY"),
    TEXT_TECHNICAL_DOC("TEXT_TECHNICAL_DOC"),
    TEXT_REVIEW("TEXT_REVIEW"),
    TEXT_SEARCH_RESULTS("TEXT_SEARCH_RESULTS"),
    COMMERCE_OTHER("COMMERCE_OTHER"),
    TEXT_OTHER("TEXT_OTHER"),
    MEDIA_OTHER("MEDIA_OTHER"),
    OTHER("OTHER");
    private String schemaName;

    BranchContentSchema(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getName() {
        return schemaName;
    }

    public static BranchContentSchema getByName(String name) {
        BranchContentSchema contentSchemaResult = COMMERCE_PRODUCT;
        for (BranchContentSchema contentSchema : BranchContentSchema.values()) {
            if (contentSchema.schemaName.equalsIgnoreCase(name)) {
                contentSchemaResult = contentSchema;
                break;
            }
        }
        return contentSchemaResult;
    }
}
