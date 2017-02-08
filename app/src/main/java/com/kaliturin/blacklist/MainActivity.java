package com.kaliturin.blacklist;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.kaliturin.blacklist.DatabaseAccessHelper.Contact;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

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
                fragmentSwitcher.onDrawerOpened();
                super.onDrawerOpened(drawerView);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // TODO if screen rotated - select current menu item
        // navigation menu
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // if there is not a screen rotation
        if(savedInstanceState == null) {
            navigationView.setCheckedItem(R.id.nav_journal);
            fragmentSwitcher.switchFragment(R.id.nav_journal);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if(!fragmentSwitcher.onBackPressed()) {
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

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            if(drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                drawer.openDrawer(GravityCompat.START);
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    // Switcher of the main fragment inside activity
    private class FragmentSwitcher {
        private final String FRAGMENT = "FRAGMENT";
        private ContactsFragment blackListFragment = new ContactsFragment();
        private ContactsFragment whiteListFragment = new ContactsFragment();
        private JournalFragment journalFragment = new JournalFragment();
        private SettingsFragment settingsFragment = new SettingsFragment();

        void onDrawerOpened() {
            // TODO
            onBackPressed();
        }

        boolean onBackPressed() {
            // TODO consider use interface for this
            return journalFragment.dismissSnackBar() ||
                    blackListFragment.dismissSnackBar() ||
                    whiteListFragment.dismissSnackBar();
        }

        // Switches fragment by navigation menu item
        void switchFragment(@IdRes int menuItemId) {
            Bundle args = new Bundle();
            switch (menuItemId) {
                case R.id.nav_journal:
                    args.putString(JournalFragment.TITLE, getString(R.string.journal_title));
                    switchFragment(journalFragment, args);
                    break;
                case R.id.nav_black_list:
                    args.putString(JournalFragment.TITLE, getString(R.string.black_list_title));
                    args.putInt(ContactsFragment.CONTACT_TYPE, Contact.TYPE_BLACK_LIST);
                    switchFragment(blackListFragment, args);
                    break;
                case R.id.nav_white_list:
                    args.putString(JournalFragment.TITLE, getString(R.string.white_list_title));
                    args.putInt(ContactsFragment.CONTACT_TYPE, Contact.TYPE_WHITE_LIST);
                    switchFragment(whiteListFragment, args);
                    break;
                default:
                    args.putString(JournalFragment.TITLE, getString(R.string.settings_title));
                    switchFragment(settingsFragment, args);
                    break;
            }
        }

        // Switches to passed fragment
        private void switchFragment(Fragment fragment, Bundle args) {
            // replace the current showed fragment
            Fragment current = getSupportFragmentManager().findFragmentByTag(FRAGMENT);
            if (current != fragment) {
                fragment.setArguments(args);
                getSupportFragmentManager().
                        beginTransaction().
                        replace(R.id.content_frame_layout, fragment, FRAGMENT).
                        commit();
            }
        }

        // Updates the current fragment with the new arguments
        private void updateFragment() {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT);
            if (fragment != null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.detach(fragment).attach(fragment).commit();
            }
        }
    }
}
