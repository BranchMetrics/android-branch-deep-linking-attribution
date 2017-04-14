package io.branch.bookfinder.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by sojanpr on 8/9/16.
 */
public class BFBookResponse {

    private final int responseStatus_;
    public int totalItems_;
    ArrayList<BFBook> bookList_ = new ArrayList<>();

    public BFBookResponse(int statusCode) {
        responseStatus_ = statusCode;
    }

    public void setBookResponse(JSONObject bookResponse) {
        try {
            if (bookResponse.has("totalItems")) {
                totalItems_ = bookResponse.getInt("totalItems");
            }
            bookList_ = new ArrayList<>();
            if (bookResponse != null) {

                if (bookResponse.has("items")) {
                    JSONArray booksJsonArray = bookResponse.getJSONArray("items");
                    for (int i = 0; i < booksJsonArray.length(); i++) {
                        bookList_.add(new BFBook(booksJsonArray.getJSONObject(i)));
                    }
                }
            }
        } catch (JSONException ignore) {

        }
    }

    public ArrayList<BFBook> getBookList() {
        return bookList_;
    }
}
