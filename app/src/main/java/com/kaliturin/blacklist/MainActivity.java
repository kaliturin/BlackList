package com.kaliturin.blacklist;

import android.content.Intent;
import android.net.Uri;
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
import android.view.KeyEvent;
import android.view.MenuItem;

import com.kaliturin.blacklist.DatabaseAccessHelper.Contact;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final String CURRENT_ITEM_ID = "CURRENT_ITEM_ID";
    private FragmentSwitcher fragmentSwitcher = new FragmentSwitcher();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO apply permissions denials
        // permissions
        Permissions.checkAndRequest(this);

        // init settings defaults
        Settings.initDefaults(this);

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // navigation menu
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // if there was a screen rotation
        if(savedInstanceState != null) {
            // set current navigation menu item
            int itemId = savedInstanceState.getInt(CURRENT_ITEM_ID);
            navigationView.setCheckedItem(itemId);
            fragmentSwitcher.setCurrentItemId(itemId);
        } else {
            // process SENDTO action
            String action = getIntent().getAction();
            if(action != null && action.equals("android.intent.action.SENDTO")) {
                // switch to SMS showing fragment
                navigationView.setCheckedItem(R.id.nav_sms);
                fragmentSwitcher.switchFragment(R.id.nav_sms);

                // get phone number where to send the SMS
                Uri uri = getIntent().getData();
                if (uri != null) {
                    String ssp = uri.getSchemeSpecificPart();
                    String number = ContactsAccessHelper.normalizeContactNumber(ssp);
                    // add it to the fragment's args
                    Bundle arguments = new Bundle();
                    arguments.putString("NUMBER", number);
                    // open SMS sending activity
                    CustomFragmentActivity.show(this, getString(R.string.sending_sms),
                            SendSMSFragment.class, arguments);
                }
            } else {
                // set default fragment as current
                navigationView.setCheckedItem(R.id.nav_journal);
                fragmentSwitcher.switchFragment(R.id.nav_journal);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_ITEM_ID, fragmentSwitcher.getCurrentItemId());
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
        // process permissions results
        Permissions.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // check granted permissions and notify about not granted
        Permissions.notifyIfNotGranted(this);
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

    // Switcher of fragments of activity
    private class FragmentSwitcher {
        private final String CURRENT_FRAGMENT = "CURRENT_FRAGMENT";
        private ContactsFragment blackListFragment = new ContactsFragment();
        private ContactsFragment whiteListFragment = new ContactsFragment();
        private JournalFragment journalFragment = new JournalFragment();
        private SettingsFragment settingsFragment = new SettingsFragment();
        private SMSConversationsFragment smsFragment = new SMSConversationsFragment();
        private int currentItemId;

        boolean onBackPressed() {
            return journalFragment.dismissSnackBar() ||
                    blackListFragment.dismissSnackBar() ||
                    whiteListFragment.dismissSnackBar();
        }

        int getCurrentItemId() {
            return currentItemId;
        }

        void setCurrentItemId(int itemId) {
            currentItemId = itemId;
        }

        // Switches fragment by navigation menu item
        void switchFragment(@IdRes int itemId) {
            switchFragment(itemId, new Bundle());
        }

        // Switches fragment by navigation menu item
        void switchFragment(@IdRes int itemId, Bundle arguments) {
            setCurrentItemId(itemId);
            switch (itemId) {
                case R.id.nav_journal:
                    arguments.putString(JournalFragment.TITLE, getString(R.string.journal_title));
                    switchFragment(journalFragment, arguments);
                    break;
                case R.id.nav_black_list:
                    arguments.putString(ContactsFragment.TITLE, getString(R.string.black_list_title));
                    arguments.putInt(ContactsFragment.CONTACT_TYPE, Contact.TYPE_BLACK_LIST);
                    switchFragment(blackListFragment, arguments);
                    break;
                case R.id.nav_white_list:
                    arguments.putString(ContactsFragment.TITLE, getString(R.string.white_list_title));
                    arguments.putInt(ContactsFragment.CONTACT_TYPE, Contact.TYPE_WHITE_LIST);
                    switchFragment(whiteListFragment, arguments);
                    break;
                case R.id.nav_sms:
                    arguments.putString(SMSConversationsFragment.TITLE, getString(R.string.sms_title));
                    switchFragment(smsFragment, arguments);
                    break;
                default:
                    arguments.putString(SettingsFragment.TITLE, getString(R.string.settings_title));
                    switchFragment(settingsFragment, arguments);
                    break;
            }
        }

        // Switches to passed fragment
        private void switchFragment(Fragment fragment, Bundle arguments) {
            // replace the current showed fragment
            Fragment current = getSupportFragmentManager().findFragmentByTag(CURRENT_FRAGMENT);
            if (current != fragment) {
                fragment.setArguments(arguments);
                getSupportFragmentManager().
                        beginTransaction().
                        replace(R.id.content_frame_layout, fragment, CURRENT_FRAGMENT).
                        commit();
            }
        }

        // Updates the current fragment with the new arguments
        private void updateFragment() {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(CURRENT_FRAGMENT);
            if (fragment != null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.detach(fragment).attach(fragment).commit();
            }
        }
    }



}
