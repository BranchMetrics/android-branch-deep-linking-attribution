package io.branch.bookfinder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.ShareSheetStyle;

/**
 * Created by sojanpr on 8/9/16.
 */
public class BookDetailsActivity extends Activity {
    BranchUniversalObject book_ = null;
    public static final String BOOK_EXTRA_KEY = "book_extra_key";
    public static final String IS_DEEP_LINKED_LAUNCH = "deep_linked_launch";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bf_details);
        Intent intent = getIntent();

        if (intent != null) {
            book_ = intent.getParcelableExtra(BOOK_EXTRA_KEY);
            //book_.listOnSamsungSearch();
            book_.userCompletedAction(BranchEvent.VIEW);
        }

        if (book_ == null) {
            finish();
        } else {
            ((TextView) findViewById(R.id.book_title)).setText(book_.getTitle());
            final ImageView book_Img = (ImageView) findViewById(R.id.detail_img);
            Picasso.with(this)
                    .load(book_.getImageUrl())
                    .into(book_Img);

            ((TextView) findViewById(R.id.detail_desc_txt)).setText(book_.getDescription());
            if (book_.getMetadata().containsKey("authors")) {
                try {
                    JSONArray authArray = new JSONArray(book_.getMetadata().get("authors"));
                    ((TextView) findViewById(R.id.author_name_txt)).setText("Author " + authArray.getString(0));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (book_.getMetadata().containsKey("publisher")) {
                String publisherName = book_.getMetadata().get("publisher");
                ((TextView) findViewById(R.id.publisher_txt)).setText("publisher " + publisherName);
            }

            findViewById(R.id.share_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ShareSheetStyle shareSheetStyle = new ShareSheetStyle(BookDetailsActivity.this, "Have you read \"" + book_.getTitle() + "\"?", "hi, Here is a reading suggestion for you. I think you will love this book \n \"" + book_.getTitle() + "\"");
                    LinkProperties linkProperties = new LinkProperties();
                    book_.showShareSheet(BookDetailsActivity.this, linkProperties, shareSheetStyle, null);
                    book_.userCompletedAction(BranchEvent.SHARE_COMPLETED);
                }
            });
            try {
                ((RatingBar) findViewById(R.id.rating_bar)).setProgress(Integer.parseInt(book_.getMetadata().get("averageRating")));
            } catch (Exception ignore) {

            }
        }

        findViewById(R.id.preview_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPreView();
            }
        });
    }

    private void showPreView() {
        findViewById(R.id.preview_layout).setVisibility(View.VISIBLE);
        WebView previewWV = (WebView) findViewById(R.id.preview_web_view);
        previewWV.setWebViewClient(new WebViewClient());
        previewWV.loadUrl(book_.getMetadata().get("previewLink"));
        //book_.userCompletedAction(BranchEvent.ADD_TO_FAVORITE);
    }


    @Override
    public void onBackPressed() {
        if (findViewById(R.id.preview_layout).getVisibility() == View.VISIBLE) {
            findViewById(R.id.preview_layout).setVisibility(View.GONE);
        } else {
            if (getIntent().getBooleanExtra(IS_DEEP_LINKED_LAUNCH, false)) {
                Intent intent = new Intent(this, BookFinderHomeActivity.class);
                this.startActivity(intent);
            }
            super.onBackPressed();
        }
    }
}
