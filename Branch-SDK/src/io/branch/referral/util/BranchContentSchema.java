package io.branch.referral.util;

/**
 * Created by sojanpr on 7/7/17.
 * Enumerations for BranchContentSchema. This enumearates all content schemas supported by Branch
 */
public enum BranchContentSchema {
    COMMERCE_PRODUCT("COMMERCE_PRODUCT");
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
