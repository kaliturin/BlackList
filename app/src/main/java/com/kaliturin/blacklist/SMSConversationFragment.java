package com.kaliturin.blacklist;


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


/**
 * Fragment for showing one SMS conversation
 */
public class SMSConversationFragment extends Fragment implements FragmentArguments {
    private SMSConversationCursorAdapter cursorAdapter = null;
    private ListView listView = null;

    public SMSConversationFragment() {
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
        return inflater.inflate(R.layout.fragment_sms_conversation, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // notify user if permission isn't granted
        Permissions.notifyIfNotGranted(getActivity(), Permissions.READ_SMS);

        // cursor adapter
        cursorAdapter = new SMSConversationCursorAdapter(getContext());
        cursorAdapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View row) {

            }
        });

        // add cursor listener to the list
        listView = (ListView) view.findViewById(R.id.rows_list);
        listView.setAdapter(cursorAdapter);

        Bundle arguments = getArguments();
        if(arguments != null) {
            // get sms thread
            int threadId = arguments.getInt(THREAD_ID);
            int unreadCount = arguments.getInt(UNREAD_COUNT);
            // init and run the sms records loader
            getLoaderManager().initLoader(0, null, newLoader(threadId, unreadCount));
        }
    }

    @Override
    public void onDestroyView() {
        getLoaderManager().destroyLoader(0);
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
                // get showed first sms
                View row = listView.getChildAt(0);
                ContactsAccessHelper.SMSRecord sms = cursorAdapter.getSMSRecord(row);
                if(sms != null) {
                    // put arguments for SMS sending fragment
                    Bundle arguments = new Bundle();
                    arguments.putString(PERSON, sms.person);
                    arguments.putString(NUMBER, sms.number);
                    // open activity with fragment
                    CustomFragmentActivity.show(getContext(),
                            getString(R.string.new_message),
                            SendSMSFragment.class, arguments);
                }
                return true;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

//----------------------------------------------------

    // Creates SMS conversation loader
    private ConversationLoaderCallbacks newLoader(int threadId, int unreadCount) {
        return new ConversationLoaderCallbacks(getContext(),
                threadId, unreadCount, listView, cursorAdapter);
    }

    // SMS conversation loader
    private static class ConversationLoader extends CursorLoader {
        private int threadId;

        ConversationLoader(Context context, int threadId) {
            super(context);
            this.threadId = threadId;
        }

        @Override
        public Cursor loadInBackground() {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(getContext());
            // get SMS records by thread id
            return db.getSMSRecordsByThreadId(getContext(), threadId, false, 0);
        }
    }

    // SMS conversation loader callbacks
    private static class ConversationLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        private Context context;
        private int threadId;
        private int unreadCount;
        private ListView listView;
        private SMSConversationCursorAdapter cursorAdapter;

        ConversationLoaderCallbacks(Context context, int threadId, int unreadCount, ListView listView,
                                    SMSConversationCursorAdapter cursorAdapter) {
            this.context = context;
            this.threadId = threadId;
            this.unreadCount = unreadCount;
            this.listView = listView;
            this.cursorAdapter = cursorAdapter;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new ConversationLoader(context, threadId);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            // apply loaded data to cursor adapter
            cursorAdapter.changeCursor(cursor);

            // scroll list to bottom
            listView.post(new Runnable() {
                @Override
                public void run() {
                    listView.setSelection(cursorAdapter.getCount() - 1);
                    listView.setVisibility(View.VISIBLE);
                }
            });

            // is there unread sms in the thread
            if(unreadCount > 0) {
                // mark sms ot the thread are read
                new SMSReadMarker(context).execute(threadId);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            cursorAdapter.changeCursor(null);
        }
    }

    // Async task - marks SMS of the thread are read
    static class SMSReadMarker extends AsyncTask<Integer, Void, Void> {
        private Context context;

        SMSReadMarker(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(Integer... params) {
            int threadId = params[0];
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
            db.setSMSReadByThreadId(context, threadId);
            return null;
        }
    }
}
