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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;


/**
 * Fragment for showing all SMS conversations
 */
public class SMSAllConversationsFragment extends Fragment {
    public static String TITLE = "TITLE";
    private SMSAllConversationsCursorAdapter cursorAdapter = null;

    public SMSAllConversationsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle arguments = getArguments();
        if(arguments != null) {
            String title = arguments.getString(TITLE);
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(title);
            }
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
        return inflater.inflate(R.layout.fragment_sms_all_conversations, container, false);
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
        getLoaderManager().destroyLoader(0);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        // cursor adapter
        cursorAdapter = new SMSAllConversationsCursorAdapter(getContext());
        cursorAdapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View row) {
                // get the clicked conversation
                ContactsAccessHelper.SMSConversation smsConversation =
                        cursorAdapter.getSMSConversation(row);
                if(smsConversation != null) {
                    // open activity with sms of the conversation
                    Bundle arguments = new Bundle();
                    arguments.putInt(SMSConversationFragment.SMS_THREAD_ID, smsConversation.threadId);
                    CustomFragmentActivity.show(getContext(), smsConversation.address,
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
                    builder.setDialogTitle(smsConversation.address);
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
            ListView listView = (ListView) view.findViewById(R.id.rows_list);
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

        // item's 'add contact' on click listener
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
    private SMSAllConversationsLoaderCallbacks newLoader() {
        return new SMSAllConversationsLoaderCallbacks(getContext(), cursorAdapter);
    }

    // SMS conversations loader
    private static class SMSAllConversationsLoader extends CursorLoader {
        SMSAllConversationsLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(getContext());
            // get ass SMS conversations
            Cursor cursor = db.getSMSConversations(getContext());
            if(cursor != null) {
                // set all SMS were seen
                db.setSMSSeen(getContext());
            }
            return cursor;
        }
    }

    // SMS conversations loader callbacks
    private static class SMSAllConversationsLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        private Context context;
        private SMSAllConversationsCursorAdapter cursorAdapter;

        SMSAllConversationsLoaderCallbacks(Context context,
                                           SMSAllConversationsCursorAdapter cursorAdapter) {
            this.context = context;
            this.cursorAdapter = cursorAdapter;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new SMSAllConversationsLoader(context);
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
