package com.kaliturin.blacklist;


import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;


/**
 * Fragment for showing one SMS conversation
 */
public class SMSConversationFragment extends Fragment {
    public static String SMS_THREAD_ID = "SMS_THREAD_ID";
    private SMSConversationCursorAdapter cursorAdapter = null;
    private ListView listView = null;

    public SMSConversationFragment() {
        // Required empty public constructor
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
            int smsThreadId = arguments.getInt(SMS_THREAD_ID);
            // init and run the sms records loader
            getLoaderManager().initLoader(0, null, newLoader(smsThreadId));
        }
    }

    @Override
    public void onDestroyView() {
        getLoaderManager().destroyLoader(0);
        super.onDestroyView();
    }

//----------------------------------------------------

    // Creates SMS conversation loader
    private SMSConversationLoaderCallbacks newLoader(int smsThreadId) {
        return new SMSConversationLoaderCallbacks(getContext(), smsThreadId, listView, cursorAdapter);
    }

    // SMS conversation loader
    private static class SMSConversationLoader extends CursorLoader {
        private int smsThreadId;

        SMSConversationLoader(Context context, int smsThreadId) {
            super(context);
            this.smsThreadId = smsThreadId;
        }

        @Override
        public Cursor loadInBackground() {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(getContext());
            // get SMS records by thread id
            Cursor cursor = db.getSMSRecordsByThreadId(getContext(), smsThreadId, false, 0);
            if(cursor != null) {
                // set SMS of the thread were read
                db.setSMSReadByThreadId(getContext(), smsThreadId);
            }
            return cursor;
        }
    }

    // SMS conversation loader callbacks
    private static class SMSConversationLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        private Context context;
        private int smsThreadId;
        private ListView listView;
        private SMSConversationCursorAdapter cursorAdapter;

        SMSConversationLoaderCallbacks(Context context, int smsThreadId,
                                        ListView listView,
                                        SMSConversationCursorAdapter cursorAdapter) {
            this.context = context;
            this.smsThreadId = smsThreadId;
            this.listView = listView;
            this.cursorAdapter = cursorAdapter;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new SMSConversationLoader(context, smsThreadId);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            // apply loaded data to cursor adapter
            cursorAdapter.changeCursor(data);
            // scroll list to bottom
            listView.post(new Runnable() {
                @Override
                public void run() {
                    listView.setSelection(cursorAdapter.getCount() - 1);
                }
            });
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            cursorAdapter.changeCursor(null);
        }
    }
}
