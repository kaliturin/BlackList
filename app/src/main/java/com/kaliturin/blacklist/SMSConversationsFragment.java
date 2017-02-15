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
 * Fragment for showing SMS conversations
 */
public class SMSConversationsFragment extends Fragment {
    public static String TITLE = "TITLE";
    private SMSConversationsCursorAdapter cursorAdapter = null;

    public SMSConversationsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle bundle = getArguments();
        String title = bundle.getString(TITLE);
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sms_conversations, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // cursor adapter
        cursorAdapter = new SMSConversationsCursorAdapter(getContext());

        // add cursor listener to the list
        ListView listView = (ListView) view.findViewById(R.id.rows_list);
        listView.setAdapter(cursorAdapter);

        // init and run the contact items loader
        getLoaderManager().initLoader(0, null, newLoader());
    }

    @Override
    public void onDestroyView() {
        getLoaderManager().destroyLoader(0);
        super.onDestroyView();
    }

    // Creates new contacts loader
    private SMSConversationsLoaderCallbacks newLoader() {
        return new SMSConversationsLoaderCallbacks(getContext(), cursorAdapter);
    }

//----------------------------------------------------------------------

    // Items loader
    private static class SMSConversationsLoader extends CursorLoader {
        SMSConversationsLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(getContext());
            return db.getSMSConversations();
        }
    }

    // Contact items loader callbacks
    private static class SMSConversationsLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        private Context context;
        private SMSConversationsCursorAdapter cursorAdapter;

        SMSConversationsLoaderCallbacks(Context context,
                                        SMSConversationsCursorAdapter cursorAdapter) {
            this.context = context;
            this.cursorAdapter = cursorAdapter;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new SMSConversationsLoader(context);
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

//----------------------------------------------------

}
