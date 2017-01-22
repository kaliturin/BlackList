package com.kaliturin.blacklist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.kaliturin.blacklist.DatabaseAccessHelper.Contact;

/**
 * Contacts fragment (black/white list)
 */

public class ContactsFragment extends Fragment {
    // bundle argument name
    public static final String CONTACT_TYPE = "CONTACT_TYPE";

    private ContactsCursorAdapter cursorAdapter = null;
    private SnackBarCustom snackBar = null;
    private int contactType = 0;
    private String itemsFilter = null;

    public ContactsFragment() {
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
        Bundle bundle = getArguments();
        contactType = bundle.getInt(CONTACT_TYPE, 0);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // snack bar
        snackBar = new SnackBarCustom(view, R.id.snack_bar);
        // "Select all" button
        snackBar.setButton(R.id.button_left,
                getString(R.string.select_all),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setCheckedAllItems();
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

        // cursor adapter
        cursorAdapter = new ContactsCursorAdapter(getContext());

        // on row click listener
        cursorAdapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View row) {
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
                // get contact from the row was clicked
                Contact contact = cursorAdapter.getContact(row);
                if(contact != null) {
                    // create and show contact menu dialog
                    ContactMenuPopupDialog.show(ContactsFragment.this, contact);
                }

                return true;
            }
        });

        // add cursor listener to the list
        ListView listView = (ListView) view.findViewById(R.id.contacts_list);
        listView.setAdapter(cursorAdapter);

        // init and run the contact items loader
        getLoaderManager().initLoader(0, null, newLoader(null, false));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if result from contact menu dialog
        if(requestCode == ContactMenuPopupDialog.REQUEST_CODE) {
            if(resultCode == Activity.RESULT_OK) {
                reloadItems(itemsFilter);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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

        // tune menu options
        MenuItem itemSearch = menu.findItem(R.id.action_search);
        Utils.tintMenuIcon(getContext(), itemSearch, R.color.colorAccent);
        itemSearch.setVisible(true);
        MenuItem itemAdd = menu.findItem(R.id.action_add);
        Utils.tintMenuIcon(getContext(), itemAdd, R.color.colorAccent);
        itemAdd.setVisible(true);

        // get the view from search menu item
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(itemSearch);
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

        // item's 'add contact' on click listener
        itemAdd.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // add contacts menu fragment
                AddContactsMenuFragment addContactsMenuFragment = new AddContactsMenuFragment();
                // set current type of contacts (black/white list)
                Bundle arguments = new Bundle();
                arguments.putInt(AddContactsMenuFragment.CONTACT_TYPE, contactType);
                addContactsMenuFragment.setArguments(arguments);
                // open the dialog activity with the fragment
                String title = getString(R.string.add_contact);
                DialogActivity.show(getActivity(), addContactsMenuFragment, title, 0);
                return true;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

//----------------------------------------------------

    // Clears all items selection
    private void clearCheckedItems() {
        if(cursorAdapter != null) {
            cursorAdapter.setCheckedAllItems(false);
        }
    }

    // Sets all items selected
    private void setCheckedAllItems() {
        if(cursorAdapter != null) {
            cursorAdapter.setCheckedAllItems(true);
        }
    }

    // Closes snack bar
    public boolean dismissSnackBar() {
        clearCheckedItems();
        return snackBar != null && snackBar.dismiss();
    }

    // Deletes checked items
    private void deleteCheckedItems() {
        getLoaderManager().restartLoader(0, null, newLoader(itemsFilter, true));
    }

    // Reloads items
    private void reloadItems(String itemsFilter) {
        this.itemsFilter = itemsFilter;
        dismissSnackBar();
        getLoaderManager().restartLoader(0, null, newLoader(itemsFilter, false));
    }

    // Creates new contacts loader
    private ContactsLoaderCallbacks newLoader(String itemsFilter, boolean deleteCheckedItems) {
        return new ContactsLoaderCallbacks(getContext(), contactType,
                cursorAdapter, itemsFilter, deleteCheckedItems);
    }

//----------------------------------------------------

    // Contact items loader
    private static class ContactsLoader extends CursorLoader {
        private IdentifiersContainer deletingItems;
        private int contactType;
        private String itemsFilter;

        ContactsLoader(Context context,
                       int contactType,
                       String itemsFilter,
                       @Nullable IdentifiersContainer deletingItems) {
            super(context);
            this.contactType = contactType;
            this.itemsFilter = itemsFilter;
            this.deletingItems = deletingItems;
        }

        @Override
        public Cursor loadInBackground() {
            DatabaseAccessHelper dao = DatabaseAccessHelper.getInstance(getContext());
            if (deletingItems != null) {
                dao.deleteContacts(contactType, deletingItems, itemsFilter);
            }
            return dao.getContacts(contactType, itemsFilter);
        }
    }

    // Contact items loader callbacks
    private static class ContactsLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        private Context context;
        private int contactType;
        private String itemsFilter;
        private ContactsCursorAdapter cursorAdapter;
        private boolean deleteCheckedItems;

        ContactsLoaderCallbacks(Context context,
                                int contactType,
                                ContactsCursorAdapter cursorAdapter,
                                String itemsFilter,
                                boolean deleteCheckedItems) {
            this.context = context;
            this.itemsFilter = itemsFilter;
            this.contactType = contactType;
            this.cursorAdapter = cursorAdapter;
            this.deleteCheckedItems = deleteCheckedItems;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            IdentifiersContainer deletingItems = null;
            if(deleteCheckedItems) {
                deletingItems = cursorAdapter.getCheckedItems().clone();
            }
            return new ContactsLoader(context, contactType, itemsFilter, deletingItems);
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
