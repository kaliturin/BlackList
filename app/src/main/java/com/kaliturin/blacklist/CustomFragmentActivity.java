package com.kaliturin.blacklist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import java.util.HashMap;

/**
 * Activity with arbitrary fragment inside
 */
public class CustomFragmentActivity extends AppCompatActivity {
    private static HashMap<String, Object> arguments = new HashMap<>();
    private static final String TITLE = "TITLE";
    private static final String FRAGMENT = "FRAGMENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_bar_main);

        // get toolbar's title
        String title = (String) getIntent().getExtras().get(TITLE);
        if(title == null) {
            finish();
            return;
        }

        // setup toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(title);

        // there is not just a screen rotation
        if(savedInstanceState == null) {
            // set fragment
            Fragment fragment = (Fragment) arguments.remove(FRAGMENT);
            if (fragment == null) {
                finish();
                return;
            }
            getSupportFragmentManager().
                    beginTransaction().
                    replace(R.id.content_frame_layout, fragment).
                    commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // check for result code from the child activity
        // (which could be started from a child fragment of the current activity)
        if (resultCode == RESULT_OK) {
            // a child activity has done an action - close the current activity
            setResult(resultCode);
            finish();
        }
    }

    // Opens dialog-activity with passed fragment inside
    public static void show(Activity parent, Fragment fragment, String title) {
        show(parent, fragment, title, 0);
    }

    // Opens dialog-activity with passed fragment inside
    public static void show(Activity parent, Fragment fragment, String title, int requestCode) {
        arguments.put(FRAGMENT, fragment);
        Intent intent = new Intent(parent, CustomFragmentActivity.class);
        intent.putExtra(TITLE, title);
        // start activity as a child of the current one and waiting for result code
        parent.startActivityForResult(intent, requestCode);
    }
}
