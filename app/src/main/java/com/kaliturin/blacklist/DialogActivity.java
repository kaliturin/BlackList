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
 * Activity for representation general dialog with arbitrary fragment inside
 */

public class DialogActivity extends AppCompatActivity {

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
        String title = (String) getIntent().getExtras().get(SharedData.FRAGMENT);
        setFragment(fragment, title);
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
    private void setFragment(Fragment fragment, String title) {
        if(fragment != null) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.content_frame_layout, fragment);
            fragmentTransaction.commit();
        }
        ActionBar toolbar = getSupportActionBar();
        if(toolbar != null) {
            toolbar.setTitle(title);
        }
    }

    // Opens dialog-activity with passed fragment inside
    public static void open(Activity parent, String title, Fragment fragment) {
        SharedData.put(SharedData.FRAGMENT, fragment);
        Intent intent = new Intent(parent, DialogActivity.class);
        intent.putExtra(SharedData.FRAGMENT, title);
        // start activity as a child of the current one and waiting for result code
        parent.startActivityForResult(intent, 0);
    }

    /**
     * Shared data holder
     */
    private static class SharedData {
        private static HashMap<String, Object> sharedObjects = new HashMap<>();
        static final String FRAGMENT = "FRAGMENT";
        static Object get(String name) {
            return sharedObjects.remove(name);
        }

        static Object put(String name, Object object) {
            return sharedObjects.put(name, object);
        }
    }
}
