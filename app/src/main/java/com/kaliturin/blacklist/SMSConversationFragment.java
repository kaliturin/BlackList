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
        ListView listView = (ListView) view.findViewById(R.id.rows_list);
        listView.setAdapter(cursorAdapter);

        Bundle arguments = getArguments();
        if(arguments != null) {
            // get sms thread
            String smsThreadId = arguments.getString(SMS_THREAD_ID);
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
    private SMSConversationLoaderCallbacks newLoader(String smsThreadId) {
        return new SMSConversationLoaderCallbacks(getContext(), smsThreadId, cursorAdapter);
    }

    // SMS conversation loader
    private static class SMSConversationLoader extends CursorLoader {
        private String smsThreadId;

        SMSConversationLoader(Context context, String smsThreadId) {
            super(context);
            this.smsThreadId = smsThreadId;
        }

        @Override
        public Cursor loadInBackground() {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(getContext());
            return db.getSMSRecordsByThreadId(getContext(), smsThreadId, false, 0);
        }
    }

    // SMS conversation loader callbacks
    private static class SMSConversationLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        private Context context;
        private String smsThreadId;
        private SMSConversationCursorAdapter cursorAdapter;

        SMSConversationLoaderCallbacks(Context context, String smsThreadId,
                                        SMSConversationCursorAdapter cursorAdapter) {
            this.context = context;
            this.smsThreadId = smsThreadId;
            this.cursorAdapter = cursorAdapter;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new SMSConversationLoader(context, smsThreadId);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            cursorAdapter.changeCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            cursorAdapter.changeCursor(null);
        }
    }
}
