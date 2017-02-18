package com.kaliturin.blacklist;


import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
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

import java.util.List;

import com.kaliturin.blacklist.DatabaseAccessHelper.Contact;
import com.kaliturin.blacklist.DatabaseAccessHelper.JournalRecord;

/**
 * Fragment for the journal (blocked calls/sms list) representation
 */
public class JournalFragment extends Fragment {
    public static String TITLE = "TITLE";
    private JournalCursorAdapter cursorAdapter = null;
    private CustomSnackBar snackBar = null;
    private String itemsFilter = null;
    private SearchView searchView = null;
    private MenuItem itemSearch = null;

    public JournalFragment() {
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
        return inflater.inflate(R.layout.fragment_journal, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Permissions.notifyIfNotGranted(getActivity(), Permissions.WRITE_EXTERNAL_STORAGE);

        // snack bar
        snackBar = new CustomSnackBar(view, R.id.snack_bar);
        // "Select all" button
        snackBar.setButton(R.id.button_left,
                getString(R.string.select_all),
                new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAllItemsChecked();
            }
        });
        // "Delete" button
        snackBar.setButton(R.id.button_center,
                getString(R.string.delete),
                new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackBar.dismiss();
                deleteCheckedItems();
            }
        });
        // "Cancel button" button
        snackBar.setButton(R.id.button_right,
                getString(R.string.cancel),
                new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackBar.dismiss();
                clearCheckedItems();
            }
        });

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
        cursorAdapter.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View row) {
                // get contact from the clicked row
                final JournalRecord record = cursorAdapter.getRecord(row);
                if(record != null) {

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

                    // create and show menu dialog for actions with the contact
                    MenuDialogBuilder builder = new MenuDialogBuilder(getActivity());
                    builder.setDialogTitle(record.caller).
                            // add menu item of record deletion
                            addMenuItem(getString(R.string.delete_record), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    deleteItem(record.id);
                                    reloadItems(itemsFilter);
                                }
                            }).
                            // add menu item records searching
                            addMenuItem(getString(R.string.find_similar_records), new View.OnClickListener() {
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
                        builder.addMenuItem(getString(R.string.exclude_from_black), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                deleteContact(contactId);
                            }
                        });
                    } else {
                        // add menu item of adding the contact to the black list
                        builder.addMenuItem(getString(R.string.add_to_black), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                addContactToBlackList(record.caller, record.number);
                            }
                        });
                    }

                    // if contact is not found in the white list
                    if(whiteContact == null) {
                        // add menu item of adding contact to the white list
                        builder.addMenuItem(getString(R.string.move_to_white), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                moveContactToWhiteList(record.caller, record.number);
                            }
                        });
                    }

                    builder.show();
                }
                return true;
            }
        });

        // add cursor listener to the journal list
        ListView listView = (ListView) view.findViewById(R.id.journal_list);
        listView.setAdapter(cursorAdapter);

        // init and run the journal records loader
        getLoaderManager().initLoader(0, null, newLoaderCallbacks(null, false));
    }

    @Override
    public void onDestroyView() {
        getLoaderManager().destroyLoader(0);
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
        itemSearch = menu.findItem(R.id.action_search);
        Utils.tintMenuIcon(getContext(), itemSearch, R.color.colorAccent);
        itemSearch.setVisible(true);

        // get the view from search menu item
        searchView = (SearchView) MenuItemCompat.getActionView(itemSearch);
        searchView.setQueryHint(getString(R.string.action_search));
        // set on text change listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                reloadItems(newText);
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
                reloadItems(null);
                return true;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

//-------------------------------------------------------------------

    // Moves contact to the white list
    // TODO consider to run it from thread
    private void moveContactToWhiteList(String caller, String number) {
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
        if(db != null) {
            db.addContact(Contact.TYPE_WHITE_LIST, caller, number);
        }
    }

    // Adds contact to the black list
    // TODO consider to run it from thread
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

    // Deletes all checked items
    private void deleteCheckedItems() {
        getLoaderManager().restartLoader(0, null, newLoaderCallbacks(itemsFilter, true));
    }

    // Reloads items
    private void reloadItems(String itemsFilter) {
        this.itemsFilter = itemsFilter;
        dismissSnackBar();
        getLoaderManager().restartLoader(0, null, newLoaderCallbacks(itemsFilter, false));
    }

    // Creates new journal items loader
    private JournalItemsLoaderCallbacks newLoaderCallbacks(String itemsFilter,
                                                           boolean deleteCheckedItems) {
        return new JournalItemsLoaderCallbacks(getContext(),
                cursorAdapter, itemsFilter, deleteCheckedItems);
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
        private boolean deleteCheckedItems;

        JournalItemsLoaderCallbacks(Context context,
                                    JournalCursorAdapter cursorAdapter,
                                    @Nullable String itemsFilter,
                                    boolean deleteCheckedItems) {
            this.context = context;
            this.cursorAdapter = cursorAdapter;
            this.itemsFilter = itemsFilter;
            this.deleteCheckedItems = deleteCheckedItems;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            IdentifiersContainer deletingItems = null;
            if(deleteCheckedItems) {
                deletingItems = cursorAdapter.getCheckedItems().clone();
            }
            return new JournalItemsLoader(context, itemsFilter, deletingItems);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            cursorAdapter.changeCursor(data);
        }

        // TODO: check whether cursor is closing
        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            cursorAdapter.changeCursor(null);
        }
    }
}

