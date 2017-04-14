package io.branch.bookfinder.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.HashMap;


import io.branch.bookfinder.BookDetailsActivity;
import io.branch.bookfinder.IBookHandleEvents;

/**
 * <p>
 * Helper calss for getting paginated list of books
 * </p>
 */
public class BFListHelper implements IBookHandleEvents, BFSearchBox.IKeywordChangeListener, AbsListView.OnScrollListener {
    private final Activity activity_;
    private final BaseAdapter adapterInstance_;
    private int totalItems_ = 0;
    private static final int PAGE_SIZE = 10;
    private static final int PAGE_OVER_SCROLL_SIZE = 5;
    HashMap<Integer, BFBook> bookMap_ = new HashMap<>();
    private final View noResultView_;
    private final BookCheckProgressView progressView_;
    private String lastSearchString_;
    private boolean isRequestPending_;
    private int scrollState_ = SCROLL_STATE_IDLE;
    private final Handler updateReqHandler_;
    private int lastReqPosition_;

    public BFListHelper(Activity context, BaseAdapter adapter, View noResultView, BookCheckProgressView progressView) {
        activity_ = context;
        adapterInstance_ = adapter;
        this.noResultView_ = noResultView;
        this.progressView_ = progressView;
        updateReqHandler_ = new Handler();
    }

    public int getTotalItems() {
        return totalItems_;
    }

    public BFBook getBookForPosition(int position) {
        if (bookMap_.containsKey(position)) {
            return bookMap_.get(position);
        } else {
            updateReqHandler_.removeCallbacks(updateRunnable);
            if (!isRequestPending_ && scrollState_ != SCROLL_STATE_IDLE) {
                updateReqHandler_.postDelayed(updateRunnable, 700);
            }
            return null;
        }
    }

    Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            getMore(lastReqPosition_);
        }
    };

    public void getMore(int startIdx) {
        getMore(BFPreference.getInstance(activity_).getLastSearchString(), startIdx);

    }

    private void getMore(String searchString, int startIdx) {
        progressView_.show();
        isRequestPending_ = true;
        startIdx = startIdx - (startIdx % PAGE_SIZE);
        GoogleBookInterface.getInstance(activity_).searchBook(searchString, startIdx, PAGE_SIZE + PAGE_OVER_SCROLL_SIZE, this);

    }


    @Override
    public void onBookResponseReceived(BFBookResponse resp, int startIdx) {

        progressView_.hide();
        isRequestPending_ = false;
        if (totalItems_ == 0) {
            totalItems_ = resp.totalItems_;
        }

        ArrayList<BFBook> bookArrayList = resp.getBookList();
        int addIdx = startIdx;
        for (BFBook book : bookArrayList) {
            bookMap_.put(addIdx, book);
            addIdx++;
        }

        if (addIdx > startIdx && adapterInstance_ != null) {
            noResultView_.setVisibility(View.GONE);
            adapterInstance_.notifyDataSetChanged();
            if (!TextUtils.isEmpty(lastSearchString_)) {
                BFPreference.getInstance(activity_).setLastSearchString(lastSearchString_);
                lastSearchString_ = null;
            }
        } else {
            noResultView_.setVisibility(View.GONE);
        }


    }

    @Override
    public void onBookSelected(BFBook book) {
        Intent intent = new Intent(activity_, BookDetailsActivity.class);
        intent.putExtra(BookDetailsActivity.BOOK_EXTRA_KEY, book);
        activity_.startActivity(intent);
    }

    @Override
    public void onKeywordChanged(String keyword) {
        if (!TextUtils.isEmpty(keyword)) {
            totalItems_ = 0;
            bookMap_ = new HashMap<>();
            lastSearchString_ = keyword;
            getMore(keyword, 0);
        }

    }

    @Override
    public void onSearchBoxClosed() {
        if (noResultView_.getVisibility() == View.VISIBLE) {
            getMore(0);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        scrollState_ = scrollState;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        lastReqPosition_ = firstVisibleItem;
    }
}
