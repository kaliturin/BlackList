/*
 * Copyright (C) 2017 Anton Kaliturin <kaliturin@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.kaliturin.blacklist.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.kaliturin.blacklist.R;
import com.kaliturin.blacklist.utils.Settings;


/**
 * Activity with arbitrary fragment inside
 */
public class CustomFragmentActivity extends AppCompatActivity {
    private static final String TAG = CustomFragmentActivity.class.getName();
    private static final String ACTIVITY_TITLE = "ACTIVITY_TITLE";
    private static final String FRAGMENT_ARGUMENTS = "FRAGMENT_ARGUMENTS";
    private static final String FRAGMENT_CLASS = "FRAGMENT_CLASS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyCurrentTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_bar_main);

        // get toolbar's title
        String title = getIntent().getStringExtra(ACTIVITY_TITLE);
        if (title != null) {
            // setup toolbar
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setTitle(title);
            }

            // show custom toolbar shadow on pre LOLLIPOP devices
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                View view = findViewById(R.id.toolbar_shadow);
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                }
            }
        }

        // there is not just a screen rotation
        if (savedInstanceState == null) {
            // create fragment
            Fragment fragment;
            String fragmentClass = getIntent().getStringExtra(FRAGMENT_CLASS);
            try {
                Class<?> clazz = Class.forName(fragmentClass);
                fragment = (Fragment) clazz.newInstance();
            } catch (Exception ex) {
                Log.w(TAG, ex);
                finish();
                return;
            }
            // add arguments
            Bundle arguments = getIntent().getBundleExtra(FRAGMENT_ARGUMENTS);
            fragment.setArguments(arguments);
            // put fragment int activity
            getSupportFragmentManager().
                    beginTransaction().
                    replace(R.id.frame_layout, fragment).
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

    // Applies the current UI theme depending on settings
    private void applyCurrentTheme() {
        if (Settings.getBooleanValue(this, Settings.UI_THEME_LIGHT)) {
            setTheme(R.style.AppTheme_Light);
        } else {
            setTheme(R.style.AppTheme_Dark);
        }
    }

    // Opens activity with fragment and waiting for result
    public static void show(Activity context, String activityTitle,
                            Class<? extends Fragment> fragmentClass,
                            Bundle fragmentArguments, int requestCode) {
        Intent intent = getIntent(context, activityTitle, fragmentClass, fragmentArguments);
        context.startActivityForResult(intent, requestCode);
    }

    // Opens activity with fragment and waiting for result
    public static void show(Context context, Fragment parent, String activityTitle,
                            Class<? extends Fragment> fragmentClass,
                            Bundle fragmentArguments, int requestCode) {
        Intent intent = getIntent(context, activityTitle, fragmentClass, fragmentArguments);
        parent.startActivityForResult(intent, requestCode);
    }

    // Opens activity with fragment
    public static void show(Context context, String activityTitle,
                            Class<? extends Fragment> fragmentClass,
                            Bundle fragmentArguments) {
        Intent intent = getIntent(context, activityTitle, fragmentClass, fragmentArguments);
        context.startActivity(intent);
    }

    private static Intent getIntent(Context context, String activityTitle,
                                    Class<? extends Fragment> fragmentClass,
                                    Bundle fragmentArguments) {
        Intent intent = new Intent(context, CustomFragmentActivity.class);
        intent.putExtra(ACTIVITY_TITLE, activityTitle);
        intent.putExtra(FRAGMENT_CLASS, fragmentClass.getName());
        intent.putExtra(FRAGMENT_ARGUMENTS, fragmentArguments);
        return intent;
    }
}
