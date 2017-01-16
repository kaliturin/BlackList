package com.kaliturin.blacklist;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.IdRes;
import android.support.annotation.IntegerRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String ACTION_SHOW_JOURNAL = "SHOW_JOURNAL";
    private static final String CURRENT_FRAGMENT = "CURRENT_FRAGMENT";

    private ContactsFragment contactsFragment = new ContactsFragment();
    private JournalFragment journalFragment = new JournalFragment();
    private SettingsFragment settingsFragment = new SettingsFragment();
    private FragmentSwitcher fragmentSwitcher = new FragmentSwitcher();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO apply permissions denials
        // permissions
        Permissions.checkAndRequestPermissions(this);

        // init settings defaults
        Settings.setDefaults(this);

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                journalFragment.dismissSnackBar();
                super.onDrawerOpened(drawerView);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // navigation
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // TODO: set checked at start the last selected item
        int itemId = R.id.nav_journal;

        String action = getIntent().getAction();
        if(action != null &&
            action.equals(ACTION_SHOW_JOURNAL)) {
            itemId = R.id.nav_journal;
        }
        navigationView.setCheckedItem(itemId);
        fragmentSwitcher.switchFragment(itemId);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (!journalFragment.dismissSnackBar() &&
                    !contactsFragment.dismissSnackBar()) {
                super.onBackPressed();
            }
        }
    }

    // Handle navigation view item click
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        fragmentSwitcher.switchFragment(item.getItemId());
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // check for result code from the child activity
        // (it could be a dialog-activity)
        if (resultCode == RESULT_OK) {
           fragmentSwitcher.updateFragment();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Switcher of the main fragment inside activity
    private class FragmentSwitcher {
        // Switches fragment by navigation menu item
        void switchFragment(@IdRes int menuItemId) {
            Bundle arguments = new Bundle();
            switch (menuItemId) {
                case R.id.nav_black_list:
                    arguments.putInt(ContactsFragment.CONTACT_TYPE,
                            DatabaseAccessHelper.Contact.TYPE_BLACK_LIST);
                    switchFragment(contactsFragment, arguments, R.string.black_list_title);
                    break;
                case R.id.nav_white_list:
                    arguments.putInt(ContactsFragment.CONTACT_TYPE,
                            DatabaseAccessHelper.Contact.TYPE_WHITE_LIST);
                    switchFragment(contactsFragment, arguments, R.string.white_list_title);
                    break;
                case R.id.nav_journal:
                    switchFragment(journalFragment, arguments, R.string.journal_title);
                    break;
                default:
                    switchFragment(settingsFragment, arguments, R.string.settings_title);
                    break;
            }
        }

        // Switches to passed fragment
        private void switchFragment(Fragment fragment, Bundle arguments, @StringRes int titleRes) {
            // update fragment's arguments
            Bundle bundle = fragment.getArguments();
            if(bundle != null) {
                bundle.clear();
                bundle.putAll(arguments);
            } else {
                fragment.setArguments(arguments);
            }

            // update or replace the current showed fragment
            Fragment current = getSupportFragmentManager().findFragmentByTag(CURRENT_FRAGMENT);
            if(current == fragment) {
                updateFragment();
            } else {
                fragment.setArguments(arguments);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.content_frame_layout, fragment, CURRENT_FRAGMENT);
                ft.commit();
            }

            // TODO: pass title to fragment through args
            // set toolbar title
            ActionBar toolbar = getSupportActionBar();
            if(toolbar != null) {
                toolbar.setTitle(getString(titleRes));
            }

            // update options menu
            invalidateOptionsMenu();
        }

        // Updates the current fragment with the new arguments
        private void updateFragment() {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(CURRENT_FRAGMENT);
            if(fragment != null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.detach(fragment);
                ft.attach(fragment);
                ft.commit();
            }
        }
    }
}
