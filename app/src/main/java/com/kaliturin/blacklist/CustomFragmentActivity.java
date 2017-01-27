package com.kaliturin.blacklist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import java.util.HashMap;

/**
 * Activity with arbitrary fragment inside
 */

public class CustomFragmentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_bar_main);

        // set toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // set fragment
        Fragment fragment = (Fragment) SharedData.get(SharedData.FRAGMENT);
        String title = (String) getIntent().getExtras().get(SharedData.TITLE);
        if(!setFragment(fragment, title)) {
            // TODO test
            finish();
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

    // Sets passed fragment to activity's layout
    private boolean setFragment(Fragment fragment, String title) {
        if(fragment == null) return false;

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame_layout, fragment);
        fragmentTransaction.commit();

        ActionBar toolbar = getSupportActionBar();
        if (toolbar != null) {
            toolbar.setTitle(title);
        }

        return true;
    }

    // Opens dialog-activity with passed fragment inside
    public static void show(Activity parent, Fragment fragment, String title) {
        show(parent, fragment, title, 0);
    }

    // Opens dialog-activity with passed fragment inside
    public static void show(Activity parent, Fragment fragment, String title, int requestCode) {
        SharedData.put(SharedData.FRAGMENT, fragment);
        Intent intent = new Intent(parent, CustomFragmentActivity.class);
        intent.putExtra(SharedData.TITLE, title);
        // start activity as a child of the current one and waiting for result code
        parent.startActivityForResult(intent, requestCode);
    }

    /**
     * Shared data holder
     */
    private static class SharedData {
        private static HashMap<String, Object> sharedObjects = new HashMap<>();
        static final String FRAGMENT = "FRAGMENT";
        static final String TITLE = "TITLE";
        static Object get(String name) {
            return sharedObjects.remove(name);
        }

        static Object put(String name, Object object) {
            return sharedObjects.put(name, object);
        }
    }
}
