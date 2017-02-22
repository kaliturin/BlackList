package com.kaliturin.blacklist;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;

import com.kaliturin.blacklist.DatabaseAccessHelper.Contact;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String ACTION_JOURNAL = "ACTION_JOURNAL";
    public static final String ACTION_SMS_CONVERSATIONS = "ACTION_SMS_CONVERSATIONS";
    public static final String ACTION_SMS_SEND_TO = "android.intent.action.SENDTO";

    private static final String CURRENT_ITEM_ID = "CURRENT_ITEM_ID";
    private FragmentSwitcher fragmentSwitcher = new FragmentSwitcher();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        int itemId;
        // if there was a screen rotation
        if(savedInstanceState != null) {
            // current navigation menu item
            itemId = savedInstanceState.getInt(CURRENT_ITEM_ID);
        } else {
            // process actions
            String action = getIntent().getAction();
            if(action != null && action.equals(ACTION_SMS_SEND_TO)) {
                // switch to SMS conversations fragment
                itemId = R.id.nav_sms;
                // show sending SMS activity
                showSendSMSActivity();
            } else
            if(action != null && action.equals(ACTION_SMS_CONVERSATIONS)) {
                // switch to SMS conversations fragment
                itemId = R.id.nav_sms;
            } else {
                // set default fragment as current
                itemId = R.id.nav_journal;
            }
        }

        // switch to chosen fragment
        fragmentSwitcher.switchFragment(itemId);
        navigationView.setCheckedItem(itemId);
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

    // Switcher of activity's fragments
    private class FragmentSwitcher {
        private final String CURRENT_FRAGMENT = "CURRENT_FRAGMENT";
        private ContactsFragment blackListFragment = new ContactsFragment();
        private ContactsFragment whiteListFragment = new ContactsFragment();
        private JournalFragment journalFragment = new JournalFragment();
        private SettingsFragment settingsFragment = new SettingsFragment();
        private SMSConversationsListFragment smsFragment = new SMSConversationsListFragment();
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
                    switchFragment(journalFragment, R.string.journal_title, arguments);
                    break;
                case R.id.nav_black_list:
                    arguments.putInt(ContactsFragment.CONTACT_TYPE, Contact.TYPE_BLACK_LIST);
                    switchFragment(blackListFragment, R.string.black_list_title, arguments);
                    break;
                case R.id.nav_white_list:
                    arguments.putInt(ContactsFragment.CONTACT_TYPE, Contact.TYPE_WHITE_LIST);
                    switchFragment(whiteListFragment, R.string.white_list_title, arguments);
                    break;
                case R.id.nav_sms:
                    switchFragment(smsFragment, R.string.sms_title, arguments);
                    break;
                default:
                    switchFragment(settingsFragment, R.string.settings_title, arguments);
                    break;
            }
        }

        // Switches to passed fragment
        private void switchFragment(Fragment fragment, @StringRes int titleId, Bundle arguments) {
            // replace the current showed fragment
            Fragment current = getSupportFragmentManager().findFragmentByTag(CURRENT_FRAGMENT);
            if (current != fragment) {
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction().
                        replace(R.id.frame_layout, fragment, CURRENT_FRAGMENT).commit();
            }
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(getString(titleId));
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

    private void showSendSMSActivity() {
        Uri uri = getIntent().getData();
        if (uri != null) {
            // get phone number where to send the SMS
            String ssp = uri.getSchemeSpecificPart();
            String number = ContactsAccessHelper.normalizeContactNumber(ssp);

            // find person by phone number in contacts
            String person = null;
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(this);
            Contact contact = db.getContact(this, number);
            if (contact != null) {
                person = contact.name;
            }

            // get SMS thread id by phone number
            int threadId = db.getThreadIdByNumber(this, number);
            if(threadId >= 0) {
                // open thread's SMS conversation activity
                Bundle arguments = new Bundle();
                arguments.putInt(SMSConversationFragment.THREAD_ID, threadId);
                String title = (person != null ? person : number);
                CustomFragmentActivity.show(this, title, SMSConversationFragment.class, arguments);
            }

            // open SMS sending activity
            Bundle arguments = new Bundle();
            arguments.putString(SendSMSFragment.PERSON, person);
            arguments.putString(SendSMSFragment.NUMBER, number);
            String title = getString(R.string.new_message);
            CustomFragmentActivity.show(this, title, SendSMSFragment.class, arguments);
        }
    }
}
