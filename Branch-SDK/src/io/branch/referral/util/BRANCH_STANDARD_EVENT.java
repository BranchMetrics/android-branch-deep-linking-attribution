package io.branch.referral.util;

/**
 * Created by sojanpr on 7/5/17.
 * <p>
 * Standard Branch Events enumeration. These events are used with Branch standard events
 * </p>
 */
public enum BRANCH_STANDARD_EVENT {
    // Commerce events
    ADD_TO_CART("ADD_TO_CART"),
    ADD_TO_WISHLIST("ADD_TO_WISHLIST"),
    VIEW_CART("VIEW_CART"),
    INITIATE_PURCHASE("INITIATE_PURCHASE"),
    ADD_PAYMENT_INFO("ADD_PAYMENT_INFO"),
    PURCHASE("PURCHASE"),
    SPEND_CREDITS("SPEND_CREDITS"),

    // Content events
    SEARCH("SEARCH"),
    VIEW_CONTENT("VIEW_CONTENT"),
    VIEW_CONTENT_LIST("VIEW_CONTENT_LIST"),
    RATE("RATE"),
    SHARE_CONTENT("SHARE_CONTENT"),

    // User lifecycle events
    COMPLETE_REGISTRATION("COMPLETE_REGISTRATION"),
    COMPLETE_TUTORIAL("COMPLETE_TUTORIAL"),
    ACHIEVE_LEVEL("ACHIEVE_LEVEL"),
    UNLOCK_ACHIEVEMENTS("UNLOCK_ACHIEVEMENT");

    private final String name;

    BRANCH_STANDARD_EVENT(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}


