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
    // Content events
    SEARCH("SEARCH"),
    VIEW_ITEM("VIEW_ITEM"),
    VIEW_ITEMS("VIEW_ITEMS"),
    RATE("RATE"),
    SHARE("SHARE"),
    INITIATE_STREAM("INITIATE_STREAM"),
    COMPLETE_STREAM("COMPLETE_STREAM"),

    // User lifecycle events
    COMPLETE_REGISTRATION("COMPLETE_REGISTRATION"),
    COMPLETE_TUTORIAL("COMPLETE_TUTORIAL"),
    ACHIEVE_LEVEL("ACHIEVE_LEVEL"),
    UNLOCK_ACHIEVEMENT("UNLOCK_ACHIEVEMENT"),

    // V2 Events
    INVITE("INVITE"),
    LOGIN("LOGIN"),
    RESERVE("RESERVE"),
    SUBSCRIBE("SUBSCRIBE"),
    START_TRIAL("START_TRIAL"),
    CLICK_AD("CLICK_AD"),
    VIEW_AD("VIEW_AD");

    private final String name;

    BRANCH_STANDARD_EVENT(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}


