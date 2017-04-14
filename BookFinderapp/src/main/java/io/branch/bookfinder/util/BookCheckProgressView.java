package io.branch.bookfinder.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import io.branch.bookfinder.R;

/**
 * Created by sojanpr on 11/10/16.
 * <p>
 * Handy animation for progress  view
 * </p>
 */
public class BookCheckProgressView extends LinearLayout {
    Animation slide_down_;
    Animation slide_up_;


    public BookCheckProgressView(Context context) {
        super(context);
        init();
    }

    public BookCheckProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BookCheckProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        slide_down_ = AnimationUtils.loadAnimation(getContext(),
                R.anim.slide_down_anim);

        slide_up_ = AnimationUtils.loadAnimation(getContext(),
                R.anim.slide_up_anim);
        slide_up_.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                BookCheckProgressView.this.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }


    public void show() {
        this.clearAnimation();
        this.startAnimation(slide_up_);
    }

    public void hide() {
        this.clearAnimation();
        this.startAnimation(slide_down_);
        slide_down_.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                BookCheckProgressView.this.setVisibility(GONE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }
}
