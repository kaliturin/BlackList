package com.kaliturin.blacklist;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
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
import android.view.View;

import com.kaliturin.blacklist.DatabaseAccessHelper.Contact;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String ACTION_JOURNAL = "ACTION_JOURNAL";
    public static final String ACTION_SMS_CONVERSATIONS = "ACTION_SMS_CONVERSATIONS";
    public static final String ACTION_SETTINGS = "ACTION_SETTINGS";
    public static final String ACTION_SMS_SEND_TO = "android.intent.action.SENDTO";

    private static final String CURRENT_ITEM_ID = "CURRENT_ITEM_ID";
    private FragmentSwitcher fragmentSwitcher = new FragmentSwitcher();
    private NavigationView navigationView;
    private DrawerLayout drawer;
    private int selectedMenuItemId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyCurrentTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // permissions
        Permissions.checkAndRequest(this);

        // init settings defaults
        Settings.initDefaults(this);

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Show custom toolbar shadow on pre LOLLIPOP devices
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            View view = findViewById(R.id.toolbar_shadow);
            if(view != null) {
                view.setVisibility(View.VISIBLE);
            }
        }

        // drawer
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.Open_navigation_drawer, R.string.Close_navigation_drawer);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // navigation menu
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        int itemId;
        // if there was a screen rotation
        if(savedInstanceState != null) {
            // current navigation menu item
            itemId = savedInstanceState.getInt(CURRENT_ITEM_ID);
        } else {
            // choose the fragment in main activity
            if(isAction(ACTION_SMS_SEND_TO)) {
                // show SMS sending activity
                showSendSMSActivity();
                // switch to SMS conversations fragment
                itemId = R.id.nav_sms;
            } else
            if(isAction(ACTION_SMS_CONVERSATIONS)) {
                // switch to SMS conversations fragment
                itemId = R.id.nav_sms;
            } else
            if(isAction(ACTION_SETTINGS)) {
                // switch to settings fragment
                itemId = R.id.nav_settings;
            } else {
                // switch to journal fragment
                itemId = R.id.nav_journal;
            }
        }
        // select navigation menu item
        selectNavigationMenuItem(itemId);
        // switch to chosen fragment
        fragmentSwitcher.switchFragment(itemId);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_ITEM_ID, selectedMenuItemId);
    }

    @Override
    public void onBackPressed() {
        if(drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if(!fragmentSwitcher.onBackPressed()) {
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        // exit item was clicked
        if(itemId == R.id.nav_exit) {
            finish();
            return true;
        }

        // switch to the new fragment
        fragmentSwitcher.switchFragment(itemId);
        drawer.closeDrawer(GravityCompat.START);

        // Normally we don't need to select navigation items manually. But in API 10
        // (and maybe some another) there is bug of menu item selection/deselection.
        // To resolve this problem we deselect the old selected item and select the
        // new one manually. And it's why we return false in the current method.
        // This way of deselection of the item was found as the most appropriate.
        // Because of some side effects of all others tried.
        selectNavigationMenuItem(itemId);

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // check for result code from the child activity
        // (it could be a dialog-activity)
        if(resultCode == RESULT_OK) {
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
        if(keyCode == KeyEvent.KEYCODE_MENU) {
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

    private void selectNavigationMenuItem(int itemId) {
        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.activity_main_drawer);
        navigationView.getMenu().findItem(itemId).setChecked(true);
        // save selected item
        selectedMenuItemId = itemId;
    }

    // Applies the current UI theme depending on settings
    private void applyCurrentTheme() {
        if(Settings.getBooleanValue(this, Settings.UI_THEME_DARK)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme_Light);
        }
    }

    // Returns true if intent action equals to passed string
    private boolean isAction(String name) {
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
        private InformationFragment informationFragment = new InformationFragment();
        private SMSConversationsListFragment smsFragment = new SMSConversationsListFragment();

        boolean onBackPressed() {
            return journalFragment.dismissSnackBar() ||
                    blackListFragment.dismissSnackBar() ||
                    whiteListFragment.dismissSnackBar();
        }

        // Switches fragment by navigation menu item
        void switchFragment(@IdRes int itemId) {
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
                    arguments.putString(TITLE, getString(R.string.Messaging));
                    switchFragment(smsFragment, arguments);
                    break;
                case R.id.nav_settings:
                    arguments.putString(TITLE, getString(R.string.Settings));
                    switchFragment(settingsFragment, arguments);
                    break;
                default:
                    arguments.putString(TITLE, getString(R.string.Information));
                    switchFragment(informationFragment, arguments);
                    break;
            }
        }

        // Switches to passed fragment
        private void switchFragment(Fragment fragment, Bundle arguments) {
            // replace the current showed fragment
            Fragment current = getSupportFragmentManager().findFragmentByTag(CURRENT_FRAGMENT);
            if(current != fragment) {
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction().
                        replace(R.id.frame_layout, fragment, CURRENT_FRAGMENT).commit();
            }
        }

        // Updates the current fragment with the new arguments
        private void updateFragment() {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(CURRENT_FRAGMENT);
            if(fragment != null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.detach(fragment).attach(fragment).commit();
            }
        }
    }

    private void showSendSMSActivity() {
        Uri uri = getIntent().getData();
        if(uri == null) {
            return;
        }
        // get phone number where to send the SMS
        String ssp = uri.getSchemeSpecificPart();
        String number = ContactsAccessHelper.normalizeContactNumber(ssp);

        // find person by phone number in contacts
        String person = null;
        ContactsAccessHelper db = ContactsAccessHelper.getInstance(this);
        Contact contact = db.getContact(this, number);
        if(contact != null) {
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
