package io.branch.bookfinder.util;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import io.branch.bookfinder.IBookHandleEvents;
import io.branch.bookfinder.R;

/**
 * Created by sojanpr on 8/9/16.
 */
public class BFBookAdapter extends BaseAdapter {

    private final BFListHelper listHelper_;
    final Context context_;
    final IBookHandleEvents callBack_;
    Display display_;
    DisplayMetrics dispMetrics_;

    public BFBookAdapter(Activity context, View noResultView, BookCheckProgressView progressView) {
        context_ = context;
        listHelper_ = new BFListHelper(context, this, noResultView, progressView);
        listHelper_.getMore(0);
        callBack_ = listHelper_;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        display_ = wm.getDefaultDisplay();
        dispMetrics_ = context.getResources().getDisplayMetrics();
    }

    public BFSearchBox.IKeywordChangeListener getKeyWordEventListener() {
        return listHelper_;
    }

    public AbsListView.OnScrollListener getScrollStateListener() {
        return listHelper_;
    }


    @Override
    public int getCount() {
        return listHelper_.getTotalItems();
    }

    @Override
    public Object getItem(int position) {
        return listHelper_.getBookForPosition(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            itemView = inflater.inflate(R.layout.book_list_item, parent, false);
            float itemWidth = (display_.getWidth() / 2) - (5 * dispMetrics_.density);
            itemView.setLayoutParams(new AbsListView.LayoutParams((int) itemWidth, (int) (itemWidth * 4) / 3));
        } else {
            itemView = convertView;
        }

        BFBook book = (BFBook) getItem(position);
        if (book != null) {
            ((TextView) itemView.findViewById(R.id.title_txt)).setText(book.getTitle());
            if (!TextUtils.isEmpty(book.getDescription())) {
                ((TextView) itemView.findViewById(R.id.desc_txt)).setText(book.getDescription());
            } else {
                itemView.findViewById(R.id.desc_txt).setVisibility(View.INVISIBLE);
            }
            ImageView thumbView = (ImageView) itemView.findViewById(R.id.book_img);
            Picasso.with(context_)
                    .load(book.getImageUrl())
                    .into(thumbView);

            itemView.setTag(position);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (callBack_ != null) {
                        callBack_.onBookSelected((BFBook) getItem((Integer) v.getTag()));
                    }
                }
            });
        } else {
            ((TextView) itemView.findViewById(R.id.title_txt)).setText("Loading..");
            ((TextView) itemView.findViewById(R.id.desc_txt)).setText("Loading.. ");
            ((ImageView) itemView.findViewById(R.id.book_img)).setImageResource(R.drawable.book_icon);
        }

        return itemView;
    }


}
