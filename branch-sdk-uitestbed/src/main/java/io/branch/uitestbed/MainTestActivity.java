package io.branch.uitestbed;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridLayout;
import android.widget.TextView;

import io.branch.uitestbed.test.controls.ContainerLayout;
import io.branch.uitestbed.test.controls.LinkBuilderButton;
import io.branch.uitestbed.test.controls.ResultView;

public class MainTestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ContainerLayout containerLayout = findViewById(R.id.control_grid);
        ResultView resultView = findViewById(R.id.result_view);

        // Link creation test
        containerLayout.addView(new LinkBuilderButton(this, resultView));


    }

}
