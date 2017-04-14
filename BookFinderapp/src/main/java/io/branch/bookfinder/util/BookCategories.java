package io.branch.bookfinder.util;

/**
 * Created by sojanpr on 9/16/16.
 */
public class BookCategories {

    private static final String[] categories = new String[]{
            "Recent", "Adventure", "Classics", "Cooking", "Drama", "Fiction", "Romance", "Satire", "Travel"
    };

    public String[] getCategories() {
        return categories;
    }
}
