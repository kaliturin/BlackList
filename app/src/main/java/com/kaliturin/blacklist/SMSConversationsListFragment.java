package com.kaliturin.blacklist;


import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import com.kaliturin.blacklist.ContactsAccessHelper.SMSConversation;


/**
 * Fragment for showing all SMS conversations
 */
public class SMSConversationsListFragment extends Fragment implements FragmentArguments {
    private static final String LIST_POSITION = "LIST_POSITION";

    private InternalEventBroadcast internalEventBroadcast = null;
    private SMSConversationsListCursorAdapter cursorAdapter = null;
    private ListView listView = null;

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

        // init internal broadcast event receiver
        internalEventBroadcast = new InternalEventBroadcast() {
            @Override
            public void onSMSInboxWrite(@NonNull String number) {
                // SMS Inbox was changed - reload list view items
                loadListViewItems();
            }

            @Override
            public void onSMSInboxRead(int threadId) {
                // SMS thread from the Inbox was read - refresh cached list view items
                cursorAdapter.invalidateCache(threadId);
                cursorAdapter.notifyDataSetChanged();
            }
        };
        internalEventBroadcast.register(getContext());

        // get saved list position
        int listPosition = 0;
        if(savedInstanceState != null) {
            listPosition = savedInstanceState.getInt(LIST_POSITION);
        }

        // load SMS conversations to the list
        loadListViewItems(listPosition);
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

        MenuItem writeSMS = menu.findItem(R.id.write_message);
        Utils.setMenuIconTint(getContext(), writeSMS, R.color.colorAccent);
        writeSMS.setVisible(true);

        writeSMS.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // open SMS sending activity
                CustomFragmentActivity.show(getContext(),
                        getString(R.string.new_message),
                        SMSSendFragment.class, null);
                return true;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

//----------------------------------------------------------------------

    // On row click listener
    class OnRowClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View row) {
            // get the clicked conversation
            final SMSConversation sms = cursorAdapter.getSMSConversation(row);
            if(sms != null) {
                // open activity with all the SMS of the conversation
                Bundle arguments = new Bundle();
                arguments.putInt(THREAD_ID, sms.threadId);
                arguments.putInt(UNREAD_COUNT, sms.unread);
                arguments.putString(CONTACT_NUMBER, sms.number);
                String title = (sms.person != null ? sms.person : sms.number);
                CustomFragmentActivity.show(getContext(), title,
                        SMSConversationFragment.class, arguments);
            }
        }
    }

    // On row long click listener
    class OnRowLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View row) {
            final SMSConversation sms = cursorAdapter.getSMSConversation(row);
            if(sms != null) {
                // create menu dialog
                MenuDialogBuilder dialog = new MenuDialogBuilder(getActivity());
                String title = (sms.person != null ? sms.person : sms.number);
                dialog.setTitle(title);
                // add menu item of sms deletion
                dialog.addItem(R.string.delete_thread, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ContactsAccessHelper db = ContactsAccessHelper.getInstance(getContext());
                        db.deleteSMSMessagesByThreadId(getContext(), sms.threadId);
                    }
                });
                dialog.show();
            }
            return true;
        }
    }

//----------------------------------------------------------------------

    // Loads SMS conversations to the list view
    private void loadListViewItems() {
        int listPosition = listView.getFirstVisiblePosition();
        loadListViewItems(listPosition);
    }

    // Loads SMS conversations to the list view
    private void loadListViewItems(int listPosition) {
        int loaderId = 0;
        ConversationsLoaderCallbacks callbacks =
                new ConversationsLoaderCallbacks(getContext(),
                        listView, listPosition, cursorAdapter);

        LoaderManager manager = getLoaderManager();
        if (manager.getLoader(loaderId) == null) {
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
        private Context context;
        private ListView listView;
        private int listPosition;
        private ProgressDialog progressBar;
        private SMSConversationsListCursorAdapter cursorAdapter;

        ConversationsLoaderCallbacks(Context context, ListView listView, int listPosition,
                                     SMSConversationsListCursorAdapter cursorAdapter) {
            this.context = context;
            this.listView = listView;
            this.listPosition = listPosition;
            this.cursorAdapter = cursorAdapter;
            this.progressBar = new ProgressDialog(context);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            progressBar.setMessage(context.getString(R.string.loading));
            progressBar.setCancelable(true);
            progressBar.show();
            return new ConversationsLoader(context);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            cursorAdapter.changeCursor(cursor);

            if(listView != null) {
                // scroll list to saved position
                listView.post(new Runnable() {
                    @Override
                    public void run() {
                        listView.setSelection(listPosition);
                        listView.setVisibility(View.VISIBLE);
                    }
                });
            }

            if(cursor != null) {
                // mark all SMS are seen
                new SMSSeenMarker(context).execute();
            }

            progressBar.dismiss();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            cursorAdapter.changeCursor(null);
            progressBar.dismiss();
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
