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

package com.kaliturin.blacklist.fragments;


import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kaliturin.blacklist.receivers.InternalEventBroadcast;
import com.kaliturin.blacklist.R;
import com.kaliturin.blacklist.activities.CustomFragmentActivity;
import com.kaliturin.blacklist.adapters.SMSConversationsListCursorAdapter;
import com.kaliturin.blacklist.utils.ContactsAccessHelper;
import com.kaliturin.blacklist.utils.ContactsAccessHelper.SMSConversation;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.Contact;
import com.kaliturin.blacklist.utils.DefaultSMSAppHelper;
import com.kaliturin.blacklist.utils.DialogBuilder;
import com.kaliturin.blacklist.utils.Permissions;
import com.kaliturin.blacklist.utils.ProgressDialogHolder;
import com.kaliturin.blacklist.utils.Utils;


/**
 * Fragment for showing all SMS conversations
 */
public class SMSConversationsListFragment extends Fragment implements FragmentArguments {
    private InternalEventBroadcast internalEventBroadcast = null;
    private SMSConversationsListCursorAdapter cursorAdapter = null;
    private ListView listView = null;
    private int listPosition = 0;

    public SMSConversationsListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // set activity title
        Bundle arguments = getArguments();
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (arguments != null && actionBar != null) {
            actionBar.setTitle(arguments.getString(TITLE));
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            listPosition = savedInstanceState.getInt(LIST_POSITION, 0);
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sms_conversations_list, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // notify user if permission isn't granted
        Permissions.notifyIfNotGranted(getContext(), Permissions.READ_SMS);
        Permissions.notifyIfNotGranted(getContext(), Permissions.READ_CONTACTS);

        // cursor adapter
        cursorAdapter = new SMSConversationsListCursorAdapter(getContext());
        // on row click listener (receives clicked row)
        cursorAdapter.setOnClickListener(new OnRowClickListener());
        // on row long click listener (receives clicked row)
        cursorAdapter.setOnLongClickListener(new OnRowLongClickListener());

        // add cursor listener to the list
        listView = (ListView) view.findViewById(R.id.rows_list);
        listView.setAdapter(cursorAdapter);

        // on list empty comment
        TextView textEmptyView = (TextView) view.findViewById(R.id.text_empty);
        listView.setEmptyView(textEmptyView);

        // init internal broadcast event receiver
        internalEventBroadcast = new InternalEventBroadcast() {
            // SMS was written
            @Override
            public void onSMSWasWritten(String phoneNumber) {
                ContactsAccessHelper db = ContactsAccessHelper.getInstance(getContext());
                int threadId = db.getSMSThreadIdByNumber(getContext(), phoneNumber);
                if (threadId >= 0 &&
                        // refresh cached list view items
                        cursorAdapter.invalidateCache(threadId)) {
                    cursorAdapter.notifyDataSetChanged();
                } else {
                    // reload all list view items
                    loadListViewItems(false, false);
                }
            }

            // SMS was deleted
            @Override
            public void onSMSWasDeleted(String phoneNumber) {
                // reload all list view items
                loadListViewItems(false, false);
            }

            // SMS thread was read
            @Override
            public void onSMSThreadWasRead(int threadId) {
                // refresh cached list view items
                cursorAdapter.invalidateCache(threadId);
                cursorAdapter.notifyDataSetChanged();
            }
        };
        internalEventBroadcast.register(getContext());

        // load SMS conversations to the list
        loadListViewItems(listPosition, true, true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(LIST_POSITION, listView.getFirstVisiblePosition());
    }

    @Override
    public void onDestroyView() {
        getLoaderManager().destroyLoader(0);
        internalEventBroadcast.unregister(getContext());
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);

        MenuItem menuItem = menu.findItem(R.id.write_message);
        Utils.setMenuIconTint(getContext(), menuItem, R.attr.colorAccent);
        menuItem.setVisible(true);
        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // open SMS sending activity
                CustomFragmentActivity.show(getContext(),
                        getString(R.string.New_message),
                        SMSSendFragment.class, null);
                return true;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPause() {
        super.onPause();
        listPosition = listView.getFirstVisiblePosition();
    }

//----------------------------------------------------------------------

    // On row click listener
    private class OnRowClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View row) {
            // get the clicked conversation
            final SMSConversation sms = cursorAdapter.getSMSConversation(row);
            if (sms != null) {
                String person = (sms.person != null ? sms.person : sms.number);
                // open activity with all the SMS of the conversation
                Bundle arguments = new Bundle();
                arguments.putString(CONTACT_NAME, person);
                arguments.putString(CONTACT_NUMBER, sms.number);
                arguments.putInt(THREAD_ID, sms.threadId);
                arguments.putInt(UNREAD_COUNT, sms.unread);
                CustomFragmentActivity.show(getContext(), person,
                        SMSConversationFragment.class, arguments);
            }
        }
    }

    // On row long click listener
    private class OnRowLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View row) {
            final SMSConversation sms = cursorAdapter.getSMSConversation(row);
            if (sms == null) {
                return true;
            }

            final String person = (sms.person != null ? sms.person : sms.number);

            // create menu dialog
            DialogBuilder dialog = new DialogBuilder(getContext());
            dialog.setTitle(person);
            // add menu item of sms deletion
            dialog.addItem(R.string.Delete_thread, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (DefaultSMSAppHelper.isDefault(getContext())) {
                        // remove SMS thread
                        ContactsAccessHelper db = ContactsAccessHelper.getInstance(getContext());
                        if (db.deleteSMSMessagesByThreadId(getContext(), sms.threadId)) {
                            // reload list
                            loadListViewItems(false, true);
                        }
                    } else {
                        Toast.makeText(getContext(), R.string.Need_default_SMS_app,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });

            final DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
            if (db != null) {
                // 'move contact to black list'
                DatabaseAccessHelper.Contact contact = db.getContact(person, sms.number);
                if (contact == null || contact.type != Contact.TYPE_BLACK_LIST) {
                    dialog.addItem(R.string.Move_to_black_list, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            db.addContact(Contact.TYPE_BLACK_LIST, person, sms.number);
                        }
                    });
                }

                // 'move contact to white list'
                if (contact == null || contact.type != Contact.TYPE_WHITE_LIST) {
                    dialog.addItem(R.string.Move_to_white_list, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            db.addContact(Contact.TYPE_WHITE_LIST, person, sms.number);
                        }
                    });
                }
            }

            dialog.show();

            return true;
        }
    }

//----------------------------------------------------------------------

    // Loads SMS conversations to the list view
    private void loadListViewItems(boolean markSeen, boolean showProgress) {
        int listPosition = listView.getFirstVisiblePosition();
        loadListViewItems(listPosition, markSeen, showProgress);
    }

    // Loads SMS conversations to the list view
    private void loadListViewItems(int listPosition, boolean markSeen, boolean showProgress) {
        if (!isAdded()) {
            return;
        }
        int loaderId = 0;
        ConversationsLoaderCallbacks callbacks =
                new ConversationsLoaderCallbacks(getContext(), listView,
                        listPosition, cursorAdapter, markSeen, showProgress);

        LoaderManager manager = getLoaderManager();
        Loader<?> loader = manager.getLoader(loaderId);
        if (loader == null) {
            // init and run the items loader
            manager.initLoader(loaderId, null, callbacks);
        } else {
            // restart loader
            manager.restartLoader(loaderId, null, callbacks);
        }
    }

    // SMS conversations loader
    private static class ConversationsLoader extends CursorLoader {
        ConversationsLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            // get all SMS conversations
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(getContext());
            return db.getSMSConversations(getContext());
        }
    }

    // SMS conversations loader callbacks
    private static class ConversationsLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        private ProgressDialogHolder progress = new ProgressDialogHolder();
        private SMSConversationsListCursorAdapter cursorAdapter;
        private Context context;
        private ListView listView;
        private int listPosition;
        private boolean markSeen;
        private boolean showProgress;

        ConversationsLoaderCallbacks(Context context, ListView listView, int listPosition,
                                     SMSConversationsListCursorAdapter cursorAdapter,
                                     boolean markSeen, boolean showProgress) {
            this.context = context;
            this.listView = listView;
            this.listPosition = listPosition;
            this.cursorAdapter = cursorAdapter;
            this.markSeen = markSeen;
            this.showProgress = showProgress;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (showProgress) {
                progress.show(context, R.string.Loading_);
            }
            return new ConversationsLoader(context);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            cursorAdapter.changeCursor(cursor);

            if (!cursorAdapter.isEmpty()) {
                // scroll list to saved position
                listView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (cursorAdapter.getCount() > 0) {
                            listView.setSelection(listPosition);
                            listView.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }

            if (markSeen) {
                // mark all SMS are seen
                new SMSSeenMarker(context).execute();
            }

            progress.dismiss();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            cursorAdapter.changeCursor(null);
            progress.dismiss();
        }
    }

//----------------------------------------------------------------------

    // Async task - marks all SMS are seen
    private static class SMSSeenMarker extends AsyncTask<Void, Void, Void> {
        private Context context;

        SMSSeenMarker(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
            db.setSMSMessagesSeen(context);
            return null;
        }
    }
}
