package io.branch.uitestbed.test.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.GridLayout;

public class ContainerLayout extends GridLayout {
    private int numOfViewsAdded = 0;

    public ContainerLayout(Context context) {
        super(context);

    }

    public ContainerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ContainerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void addView(View view) {

        Log.d("Test_Grid", "----------------------------");
        Log.d("Test_Grid", "Row pos" +numOfViewsAdded / getColumnCount());
        Log.d("Test_Grid", "col pos" +numOfViewsAdded % getColumnCount());

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(numOfViewsAdded / getColumnCount(), 1),
                GridLayout.spec(numOfViewsAdded % getColumnCount(), 1));
        lp.setMargins(3,3,3,3);
        this.addView(view, lp);
        numOfViewsAdded++;

    }

}
