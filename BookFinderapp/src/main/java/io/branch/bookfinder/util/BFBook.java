package io.branch.bookfinder.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.PrivateKey;

import io.branch.indexing.BranchUniversalObject;

/**
 * Created by sojanpr on 8/9/16.
 */
public class BFBook extends BranchUniversalObject {

    public BFBook(JSONObject bookJson) {
        if (bookJson != null) {
            try {
                if (bookJson.has("id")) {
                    this.setCanonicalIdentifier(bookJson.getString("id"));
                }
                if (bookJson.has("selfLink")) {
                    this.setCanonicalUrl(bookJson.getString("selfLink"));
                }

                if (bookJson.has("volumeInfo")) {
                    JSONObject volumeInfo = bookJson.getJSONObject("volumeInfo");
                    if (volumeInfo.has("title")) {
                        this.setTitle(volumeInfo.getString("title"));
                        this.addKeyWord(volumeInfo.getString("title"));
                    }
                    if (volumeInfo.has("subtitle")) {
                        this.addContentMetadata("subtitle", volumeInfo.getString("subtitle"));
                        this.addKeyWord(volumeInfo.getString("subtitle"));
                    }
                    if (volumeInfo.has("authors")) {
                        this.addContentMetadata("authors", volumeInfo.getJSONArray("authors").toString());

                    }
                    if (volumeInfo.has("publisher")) {
                        this.addContentMetadata("publisher", volumeInfo.getString("publisher").toString());
                        this.addKeyWord(volumeInfo.getString("publisher"));
                    }
                    if (volumeInfo.has("description")) {
                        String desc = volumeInfo.getString("description");
                        if (desc != null && desc.length() > 200) {
                            this.setContentDescription(volumeInfo.getString("description").substring(0, 200));
                        } else {
                            this.setContentDescription(volumeInfo.getString("description"));
                        }
                    }
                    if (volumeInfo.has("imageLinks")) {
                        JSONObject imageLinks = volumeInfo.getJSONObject("imageLinks");
                        if (imageLinks.has("thumbnail")) {
                            this.setContentImageUrl(imageLinks.getString("thumbnail"));
                        }
                        if (imageLinks.has("smallThumbnail")) {
                            this.addContentMetadata("smallThumbnail", imageLinks.getString("smallThumbnail"));
                        }
                    }

                    if (volumeInfo.has("previewLink")) {
                        this.addContentMetadata("previewLink", volumeInfo.getString("previewLink"));
                    }
                    if (volumeInfo.has("averageRating")) {
                        this.addContentMetadata("averageRating", volumeInfo.getString("averageRating"));
                    }
                    if (volumeInfo.has("categories")) {
                        this.addContentMetadata("categories", volumeInfo.getJSONArray("categories").toString());
                    }
                    if (volumeInfo.has("averageRating")) {
                        this.addContentMetadata("averageRating", Integer.toString(volumeInfo.getInt("averageRating")));
                    }

                }


            } catch (JSONException ignore) {

            }
        }
    }

}
