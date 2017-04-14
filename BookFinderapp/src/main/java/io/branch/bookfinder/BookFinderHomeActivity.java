package io.branch.bookfinder;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.TextAppearanceSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.TextView;

import io.branch.bookfinder.util.BFBookAdapter;
import io.branch.bookfinder.util.BFSearchBox;
import io.branch.bookfinder.util.BookCheckProgressView;

/**
 * <p>Activity for showing list of books </p>
 */
public class BookFinderHomeActivity extends Activity {


    BFSearchBox bfSearchBox_;
    BFBookAdapter bookAdapter_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bf_home);
        bookAdapter_ = new BFBookAdapter(this, findViewById(R.id.no_result_layout), (BookCheckProgressView) findViewById(R.id.progress_layout));

        GridView gridView = ((GridView) findViewById(R.id.book_grid_view));
        gridView.setAdapter(bookAdapter_);
        gridView.setOnScrollListener(bookAdapter_.getScrollStateListener());

        bfSearchBox_ = (BFSearchBox) findViewById(R.id.search_txt);
        bfSearchBox_.setSearchButton(findViewById(R.id.search_btn));
        bfSearchBox_.setKeywordChangeListener(bookAdapter_.getKeyWordEventListener());
    }

    @Override
    public void onBackPressed() {
        if (bfSearchBox_.getVisibility() == View.VISIBLE) {
            bfSearchBox_.setText("");
            bfSearchBox_.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }


}
