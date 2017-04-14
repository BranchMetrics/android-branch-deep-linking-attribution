package io.branch.bookfinder;

import io.branch.bookfinder.util.BFBook;
import io.branch.bookfinder.util.BFBookResponse;

/**
 * Created by sojanpr on 8/9/16.
 */
public interface IBookHandleEvents {

    void onBookResponseReceived(BFBookResponse resp, int startIdx);

    void onBookSelected(BFBook book);
}
