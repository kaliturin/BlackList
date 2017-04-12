package com.kaliturin.blacklist;


import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import com.kaliturin.blacklist.DatabaseAccessHelper.Contact;
import com.kaliturin.blacklist.DatabaseAccessHelper.JournalRecord;

/**
 * Fragment for the journal (blocked calls/sms list) representation
 */
public class JournalFragment extends Fragment implements FragmentArguments {
    private static final String LIST_POSITION = "LIST_POSITION";
    private InternalEventBroadcast internalEventBroadcast = null;
    private JournalCursorAdapter cursorAdapter = null;
    private ListView listView = null;
    private ButtonsBar snackBar = null;
    private String itemsFilter = "";
    private SearchView searchView = null;
    private MenuItem itemSearch = null;
    private int listPosition = 0;

    public JournalFragment() {
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
        if(savedInstanceState != null) {
            listPosition = savedInstanceState.getInt(LIST_POSITION, 0);
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_journal, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Permissions.notifyIfNotGranted(getContext(), Permissions.WRITE_EXTERNAL_STORAGE);

        // snack bar
        snackBar = new ButtonsBar(view, R.id.three_buttons_bar);
        // "Cancel" button
        snackBar.setButton(R.id.button_left,
                getString(R.string.CANCEL),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        snackBar.dismiss();
                        clearCheckedItems();
                    }
                });
        // "Delete" button
        snackBar.setButton(R.id.button_center,
                getString(R.string.DELETE),
                new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackBar.dismiss();
                deleteCheckedItems();
            }
        });
        // "Select all" button
        snackBar.setButton(R.id.button_right,
                getString(R.string.SELECT_ALL),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setAllItemsChecked();
                    }
                });

        // init internal broadcast event receiver
        internalEventBroadcast = new InternalEventBroadcast() {
            @Override
            public void onJournalWasWritten() {
                clearSearchView();
                // reload list view items
                reloadItems("", true);
            }
        };
        internalEventBroadcast.register(getContext());

        // journal cursor adapter
        cursorAdapter = new JournalCursorAdapter(getContext());
        cursorAdapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cursorAdapter.hasCheckedItems()) {
                    snackBar.show();
                } else {
                    snackBar.dismiss();
                }
            }
        });

        // on row long click listener (receives clicked row)
        cursorAdapter.setOnLongClickListener(new OnLongClickListener());

        // add cursor listener to the journal list
        listView = (ListView) view.findViewById(R.id.journal_list);
        listView.setAdapter(cursorAdapter);

        // on list empty comment
        TextView textEmptyView = (TextView) view.findViewById(R.id.text_empty);
        listView.setEmptyView(textEmptyView);

        // load the list view
        loadListViewItems(itemsFilter, false, listPosition);
    }

    @Override
    public void onDestroyView() {
        getLoaderManager().destroyLoader(0);
        internalEventBroadcast.unregister(getContext());
        cursorAdapter.changeCursor(null);
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
        itemSearch = menu.findItem(R.id.action_search);
        Utils.setMenuIconTint(getContext(), itemSearch, R.attr.colorAccent);
        itemSearch.setVisible(true);

        // get the view from search menu item
        searchView = (SearchView) MenuItemCompat.getActionView(itemSearch);
        searchView.setQueryHint(getString(R.string.Search_action));
        // set on text change listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                reloadItems(newText, false);
                return true;
            }
        });

        // on search cancelling
        // SearchView.OnCloseListener is not calling so use other way...
        MenuItemCompat.setOnActionExpandListener(itemSearch,
                new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                reloadItems("", false);
                return true;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(LIST_POSITION, listView.getFirstVisiblePosition());
    }

    @Override
    public void onPause() {
        super.onPause();
        listPosition = listView.getFirstVisiblePosition();
    }

//-------------------------------------------------------------------

    // Moves contact to the white list
    // TODO consider to run it in thread
    private void moveContactToWhiteList(String caller, String number) {
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
        if(db != null) {
            db.addContact(Contact.TYPE_WHITE_LIST, caller, number);
        }
    }

    // Adds contact to the black list
    // TODO consider to run it in thread
    private void addContactToBlackList(String caller, String number) {
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
        if(db != null) {
            db.addContact(Contact.TYPE_BLACK_LIST, caller, number);
        }
    }

    // Deletes contact by id
    private void deleteContact(long id) {
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
        if(db != null) {
            db.deleteContact(id);
        }
    }

    // Shows SearchView with passed query
    private void searchItems(String query) {
        if(itemSearch != null && searchView != null) {
            MenuItemCompat.expandActionView(itemSearch);
            searchView.setQuery(query, true);
        }
    }

    // Deletes contact by id
    private void deleteItem(long recordId) {
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
        if(db != null) {
            db.deleteJournalRecord(recordId);
        }
    }

    // Clears all items selection
    private void clearCheckedItems() {
        if(cursorAdapter != null) {
            cursorAdapter.setAllItemsChecked(false);
        }
    }

    // Sets all items selected
    private void setAllItemsChecked() {
        if(cursorAdapter != null) {
            cursorAdapter.setAllItemsChecked(true);
        }
    }

    // Closes snack bar
    public boolean dismissSnackBar() {
        clearCheckedItems();
        return snackBar != null && snackBar.dismiss();
    }

    // Clears search view
    private void clearSearchView() {
        if(searchView != null && itemSearch != null) {
            if(searchView.getQuery().length() > 0) {
                searchView.setQuery("", false);
            }
            MenuItemCompat.collapseActionView(itemSearch);
        }
        itemsFilter = "";
    }

    // Deletes all checked items
    private void deleteCheckedItems() {
        int listPosition = listView.getFirstVisiblePosition();
        loadListViewItems(itemsFilter, true, listPosition);
    }

    // Reloads items
    private void reloadItems(@NonNull String itemsFilter, boolean force) {
        if(!force && this.itemsFilter.equals(itemsFilter)) {
            return;
        }
        this.itemsFilter = itemsFilter;
        dismissSnackBar();

        int listPosition = listView.getFirstVisiblePosition();
        loadListViewItems(itemsFilter, false, listPosition);
    }

    // Loads items to the list view
    private void loadListViewItems(String itemsFilter,  boolean deleteItems, int listPosition) {
        if(!isAdded()) {
            return;
        }
        int loaderId = 0;
        JournalItemsLoaderCallbacks callbacks =
                new JournalItemsLoaderCallbacks(getContext(), cursorAdapter,
                        itemsFilter, deleteItems, listView, listPosition);
        LoaderManager manager = getLoaderManager();
        if (manager.getLoader(loaderId) == null) {
            // init and run the items loader
            manager.initLoader(loaderId, null, callbacks);
        } else {
            // restart loader
            manager.restartLoader(loaderId, null, callbacks);
        }
    }

//--------------------------------------------

    // On row long click listener
    class OnLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View view) {
            // get contact from the clicked row
            final JournalRecord record = cursorAdapter.getRecord(view);
            if(record == null) return true;

            // find contacts in black and white lists by record's caller and number
            Contact blackContact = null, whiteContact = null;
            String number = (record.number == null ? record.caller : record.number);
            DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
            if(db != null) {
                List<Contact> contacts = db.getContacts(number, false);
                for(Contact contact : contacts) {
                    if(contact.name.equals(record.caller)) {
                        if (contact.type == Contact.TYPE_BLACK_LIST) {
                            blackContact = contact;
                        } else {
                            whiteContact = contact;
                        }
                    }
                }
            }

            // create menu dialog
            DialogBuilder dialog = new DialogBuilder(getActivity());
            dialog.setTitle(record.caller);
            if(record.text != null && !record.text.isEmpty()) {
                // add menu item of record copying
                dialog.addItem(R.string.Copy_text, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(Utils.copyTextToClipboard(getContext(), record.text)) {
                            Toast.makeText(getContext(), R.string.Copied_to_clipboard,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            // add menu item of record deletion
            dialog.addItem(R.string.Delete_record, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteItem(record.id);
                    reloadItems(itemsFilter, true);
                }
            });
            // add menu item records searching
            dialog.addItem(R.string.Find_similar_records, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // find all records by record's caller
                    searchItems(record.caller);
                }
            });

            // if contact is found in the black list
            if (blackContact != null) {
                // add menu item of excluding the contact from the black list
                final long contactId = blackContact.id;
                dialog.addItem(R.string.Exclude_from_black_list, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteContact(contactId);
                    }
                });
            } else {
                // add menu item of adding the contact to the black list
                dialog.addItem(R.string.Move_to_black_list, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addContactToBlackList(record.caller, record.number);
                    }
                });
            }

            // if contact is not found in the white list
            if(whiteContact == null) {
                // add menu item of adding contact to the white list
                dialog.addItem(R.string.Move_to_white_list, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        moveContactToWhiteList(record.caller, record.number);
                    }
                });
            }

            dialog.show();
            return true;
        }
    }

//--------------------------------------------

    // Journal items loader
    private static class JournalItemsLoader extends CursorLoader {
        private IdentifiersContainer deletingItems;
        private String itemsFilter;

        JournalItemsLoader(Context context,
                           @Nullable String itemsFilter,
                           @Nullable IdentifiersContainer deletingItems) {
            super(context);
            this.itemsFilter = itemsFilter;
            this.deletingItems = deletingItems;
        }

        @Override
        public Cursor loadInBackground() {
            DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
            if(db == null) {
                return null;
            }
            if(deletingItems != null) {
                db.deleteJournalRecords(deletingItems, itemsFilter);
            }
            return db.getJournalRecords(itemsFilter);
        }
    }

    // Journal items loader callbacks
    private static class JournalItemsLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        private Context context;
        private String itemsFilter;
        private JournalCursorAdapter cursorAdapter;
        private boolean deleteItems;
        private ListView listView;
        private int listPosition;

        JournalItemsLoaderCallbacks(Context context,
                                    JournalCursorAdapter cursorAdapter,
                                    @Nullable String itemsFilter,
                                    boolean deleteItems,
                                    ListView listView,
                                    int listPosition) {
            this.context = context;
            this.cursorAdapter = cursorAdapter;
            this.itemsFilter = itemsFilter;
            this.deleteItems = deleteItems;
            this.listView = listView;
            this.listPosition = listPosition;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            IdentifiersContainer deletingItems = null;
            if(deleteItems) {
                deletingItems = cursorAdapter.getCheckedItems().clone();
            }
            return new JournalItemsLoader(context, itemsFilter, deletingItems);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            cursorAdapter.changeCursor(data);

            if(listView != null) {
                // scroll list to saved position
                listView.post(new Runnable() {
                    @Override
                    public void run() {
                        listView.setSelection(listPosition);
                    }
                });
            }
        }

        // TODO: check whether cursor is closing
        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            cursorAdapter.changeCursor(null);
        }
    }
}

