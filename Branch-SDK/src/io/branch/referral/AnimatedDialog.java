package io.branch.referral;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;

/**
 * <p>Class for creating a Dialog which open and closes with an animation to the content view </p>
 */
class AnimatedDialog extends Dialog {
    private static boolean isClosing_ = false;
    Context context_;
    public AnimatedDialog(Context context) {
        super(context);
        init(context);
    }

    public AnimatedDialog(Context context, int theme) {
        super(context, theme);
        init(context);
    }

    public AnimatedDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        init(context);
    }

    //--------------------- Public  methods -------------//

    /**
     * <p> Opens the dialog with an animation to the content View.</p>
     */
    @Override
    public void show() {
        slideOpen();
    }

    /**
     * <p> Cancels the dialog with an animation to the content View.</p>
     */
    @Override
    public void cancel() {
        slideClose();
    }

    @Override
    public void setContentView(int layoutResID) {
        setDialogWindowAttributes();
        super.setContentView(layoutResID);
    }


    //------------------Private methods------------------//
    private void init(Context context) {
        context_ = context;
        setDialogWindowAttributes();
        // Listen for the backpress in order to dismiss the dialog with animation
        setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    slideClose();
                }
                return true;
            }
        });
    }

    /**
     * Set the window attributes for the invite dialog.
     */
    public void setDialogWindowAttributes() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        lp.gravity = Gravity.BOTTOM;
        lp.dimAmount = 0.8f;
        getWindow().setAttributes(lp);
        getWindow().setWindowAnimations(android.R.anim.slide_in_left);
        setCanceledOnTouchOutside(true);
    }

    /**
     * </p> Opens the dialog with a translation animation to the content view </p>
     */
    private void slideOpen() {
        TranslateAnimation slideUp = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0f);
        slideUp.setDuration(500);
        slideUp.setInterpolator(new AccelerateInterpolator());
        ((ViewGroup) getWindow().getDecorView()).getChildAt(0).startAnimation(slideUp);
        super.show();
    }

    /**
     * </p> Closes the dialog with a translation animation to the content view </p>
     */
    private void slideClose() {
        if (!isClosing_) {
            isClosing_ = true;
            TranslateAnimation slideDown = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1f);
            slideDown.setDuration(500);
            slideDown.setInterpolator(new DecelerateInterpolator());

            ((ViewGroup) getWindow().getDecorView()).getChildAt(0).startAnimation(slideDown);
            slideDown.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    dismiss();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }
    }

}
