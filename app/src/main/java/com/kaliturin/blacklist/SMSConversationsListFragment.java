package com.kaliturin.blacklist;


import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;


/**
 * Fragment for showing all SMS conversations
 */
public class SMSConversationsListFragment extends Fragment {
    private SMSConversationsListCursorAdapter cursorAdapter = null;
    private ListView listView = null;
    private int listViewPosition = 0;
    private ProgressDialog progressBar = null;

    public SMSConversationsListFragment() {
        // Required empty public constructor
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
        Permissions.notifyIfNotGranted(getActivity(), Permissions.READ_SMS);
        Permissions.notifyIfNotGranted(getActivity(), Permissions.READ_CONTACTS);
    }

    @Override
    public void onPause() {
        if(listView != null) {
            listViewPosition = listView.getFirstVisiblePosition();
            listView.setVisibility(View.INVISIBLE);
        }
        getLoaderManager().destroyLoader(0);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        progressBar = new ProgressDialog(getContext());
        progressBar.setTitle(getString(R.string.loading));
        progressBar.show();

        // cursor adapter
        cursorAdapter = new SMSConversationsListCursorAdapter(getContext());
        cursorAdapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View row) {
                // get the clicked conversation
                ContactsAccessHelper.SMSConversation sms = cursorAdapter.getSMSConversation(row);
                if(sms != null) {
                    // open activity with all the SMS of the conversation
                    Bundle arguments = new Bundle();
                    arguments.putInt(SMSConversationFragment.THREAD_ID, sms.threadId);
                    String title = (sms.person != null ? sms.person : sms.number);
                    CustomFragmentActivity.show(getContext(), title,
                            SMSConversationFragment.class, arguments);
                }
            }
        });

        // on row long click listener (receives clicked row)
        cursorAdapter.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View row) {
                final ContactsAccessHelper.SMSConversation smsConversation =
                        cursorAdapter.getSMSConversation(row);
                if(smsConversation != null) {
                    // create menu dialog
                    MenuDialogBuilder builder = new MenuDialogBuilder(getActivity());
                    builder.setDialogTitle(smsConversation.person);
                    // add menu item of sms deletion
                    builder.addMenuItem(getString(R.string.delete_thread), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ContactsAccessHelper db = ContactsAccessHelper.getInstance(getContext());
                            db.deleteSMSByThreadId(getContext(), smsConversation.threadId);
                        }
                    });
                    builder.show();
                }
                return true;
            }
        });

        View view = getView();
        if (view != null) {
            // add cursor listener to the list
            listView = (ListView) view.findViewById(R.id.rows_list);
            listView.setAdapter(cursorAdapter);
            // init and run the items loader
            getLoaderManager().initLoader(0, null, newLoader());
        }
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
                        SendSMSFragment.class, null);
                return true;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

//----------------------------------------------------------------------

    // Creates SMS conversations loader
    private ConversationsLoaderCallbacks newLoader() {
        return new ConversationsLoaderCallbacks(getContext(),
                progressBar, listView, listViewPosition, cursorAdapter);
    }

    // SMS conversations loader
    private static class ConversationsLoader extends CursorLoader {
        ConversationsLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(getContext());
            // get all SMS conversations
            return db.getSMSConversations(getContext());
        }
    }

    // SMS conversations loader callbacks
    private static class ConversationsLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        private Context context;
        private ListView listView;
        private ProgressDialog progressBar;
        private int listViewPosition;
        private SMSConversationsListCursorAdapter cursorAdapter;

        ConversationsLoaderCallbacks(Context context, ProgressDialog progressBar,
                                     ListView listView, int listViewPosition,
                                     SMSConversationsListCursorAdapter cursorAdapter) {
            this.context = context;
            this.progressBar = progressBar;
            this.listView = listView;
            this.listViewPosition = listViewPosition;
            this.cursorAdapter = cursorAdapter;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
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
                        listView.setSelection(listViewPosition);
                        listView.setVisibility(View.VISIBLE);
                    }
                });
            }

            if(cursor != null) {
                // mark all SMS are seen
                SMSSeenMarker marker = new SMSSeenMarker(context);
                marker.execute();
            }

            if(progressBar != null) {
                progressBar.dismiss();
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            cursorAdapter.changeCursor(null);
        }
    }

    // Async task - sets all SMS are seen
    private static class SMSSeenMarker extends AsyncTask<Void, Void, Void> {
        private Context context;
        SMSSeenMarker(Context context) {
            this.context = context;
        }
        @Override
        protected Void doInBackground(Void... params) {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
            db.setSMSSeen(context);
            return null;
        }
    }

}
