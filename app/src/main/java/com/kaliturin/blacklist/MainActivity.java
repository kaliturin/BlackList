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
                R.string.Open_navigation_drawer, R.string.Close_navigation_drawer);
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
            // select drawer's menu item
            navigationView.setCheckedItem(itemId);
            fragmentSwitcher.setCurrentItemId(itemId);
        } else {
            // process actions
            if(isAction(ACTION_SMS_SEND_TO)) {
                // show SMS sending activity
                showSendSMSActivity();
                // switch to SMS conversations fragment in main activity
                itemId = R.id.nav_sms;
            } else
            if(isAction(ACTION_SMS_CONVERSATIONS)) {
                // switch to SMS conversations fragment in main activity
                itemId = R.id.nav_sms;
            } else {
                // switch to journal fragment in main activity
                itemId = R.id.nav_journal;
            }
            // select drawer's menu item
            navigationView.setCheckedItem(itemId);
            // switch to chosen fragment
            fragmentSwitcher.switchFragment(itemId);
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

//----------------------------------------------------------------------------

    boolean isAction(String name) {
        String action = getIntent().getAction();
        return (action != null && action.equals(name));
    }

    // Switcher of activity's fragments
    private class FragmentSwitcher implements FragmentArguments {
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
            setCurrentItemId(itemId);
            Bundle arguments = new Bundle();
            switch (itemId) {
                case R.id.nav_journal:
                    arguments.putString(TITLE, getString(R.string.Journal));
                    switchFragment(journalFragment, arguments);
                    break;
                case R.id.nav_black_list:
                    arguments.putString(TITLE, getString(R.string.Black_list));
                    arguments.putInt(CONTACT_TYPE, Contact.TYPE_BLACK_LIST);
                    switchFragment(blackListFragment, arguments);
                    break;
                case R.id.nav_white_list:
                    arguments.putString(TITLE, getString(R.string.White_list));
                    arguments.putInt(CONTACT_TYPE, Contact.TYPE_WHITE_LIST);
                    switchFragment(whiteListFragment, arguments);
                    break;
                case R.id.nav_sms:
                    arguments.putString(TITLE, getString(R.string.SMS_messages));
                    switchFragment(smsFragment, arguments);
                    break;
                default:
                    arguments.putString(TITLE, getString(R.string.Settings));
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
                getSupportFragmentManager().beginTransaction().
                        replace(R.id.frame_layout, fragment, CURRENT_FRAGMENT).commit();
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
        if (uri == null) {
            return;
        }
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
        int threadId = db.getSMSThreadIdByNumber(this, number);
        if(threadId >= 0) {
            // get the count of unread sms of the thread
            int unreadCount = db.getSMSMessagesUnreadCountByThreadId(this, threadId);

            // open thread's SMS conversation activity
            Bundle arguments = new Bundle();
            arguments.putInt(FragmentArguments.THREAD_ID, threadId);
            arguments.putInt(FragmentArguments.UNREAD_COUNT, unreadCount);
            arguments.putString(FragmentArguments.CONTACT_NUMBER, number);
            String title = (person != null ? person : number);
            CustomFragmentActivity.show(this, title, SMSConversationFragment.class, arguments);
        }

        // open SMS sending activity
        Bundle arguments = new Bundle();
        arguments.putString(FragmentArguments.CONTACT_NAME, person);
        arguments.putString(FragmentArguments.CONTACT_NUMBER, number);
        String title = getString(R.string.New_message);
        CustomFragmentActivity.show(this, title, SMSSendFragment.class, arguments);
    }
}
