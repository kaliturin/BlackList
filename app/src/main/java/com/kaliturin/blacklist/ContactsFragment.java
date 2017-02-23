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

import com.kaliturin.blacklist.DatabaseAccessHelper.Contact;

/**
 * Contacts fragment (black/white list)
 */

public class ContactsFragment extends Fragment implements FragmentArguments {
    private ContactsCursorAdapter cursorAdapter = null;
    private CustomSnackBar snackBar = null;
    private int contactType = 0;
    private String itemsFilter = null;

    public ContactsFragment() {
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
        Bundle arguments = getArguments();
        if(arguments != null) {
            contactType = arguments.getInt(CONTACT_TYPE, 0);
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
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
                // get contact from the clicked row
                final Contact contact = cursorAdapter.getContact(row);
                if(contact != null) {
                    // create and show menu dialog for actions with the contact
                    MenuDialogBuilder builder = new MenuDialogBuilder(getActivity());
                    builder.setDialogTitle(contact.name).
                            // add menu item of contact deletion
                            addMenuItem(getString(R.string.remove_contact), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    deleteContact(contact.id);
                                    reloadItems(itemsFilter);
                                }
                            }).
                            // add menu item of contact editing
                            addMenuItem(getString(R.string.edit_contact), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // edit contact
                                    editContact(contact.id);
                                }
                            });
                    // add menu item of contact moving to opposite list
                    String itemTitle = (contact.type == Contact.TYPE_WHITE_LIST ?
                                        getString(R.string.move_to_black) :
                                        getString(R.string.move_to_white));
                    builder.addMenuItem(itemTitle, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    moveContactToOppositeList(contact);
                                    reloadItems(itemsFilter);
                                }
                            }).show();
                }
                return true;
            }
        });

        // add cursor listener to the list
        ListView listView = (ListView) view.findViewById(R.id.contacts_list);
        listView.setAdapter(cursorAdapter);

        // init and run the contact items loader
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

        // tune menu options
        MenuItem itemSearch = menu.findItem(R.id.action_search);
        Utils.setMenuIconTint(getContext(), itemSearch, R.color.colorAccent);
        itemSearch.setVisible(true);
        MenuItem itemAdd = menu.findItem(R.id.action_add);
        Utils.setMenuIconTint(getContext(), itemAdd, R.color.colorAccent);
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
                // set current type of contacts (black/white list)
                Bundle arguments = new Bundle();
                arguments.putInt(CONTACT_TYPE, contactType);
                // open the dialog activity with the contacts menu fragment
                String title = getString(R.string.add_contact);
                CustomFragmentActivity.show(getActivity(), title,
                        AddContactsMenuFragment.class, arguments, 0);

                return true;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

//----------------------------------------------------

    // Deletes contact by id
    private void deleteContact(long id) {
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
        if(db != null) {
            db.deleteContact(id);
        }
    }

    // Move contact to opposite type list
    private void moveContactToOppositeList(Contact contact) {
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
        if(db != null) {
            db.moveContact(contact);
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

    // Deletes checked items
    private void deleteCheckedItems() {
        getLoaderManager().restartLoader(0, null, newLoaderCallbacks(itemsFilter, true));
    }

    // Reloads items
    private void reloadItems(String itemsFilter) {
        this.itemsFilter = itemsFilter;
        dismissSnackBar();
        getLoaderManager().restartLoader(0, null, newLoaderCallbacks(itemsFilter, false));
    }

    // Creates new contacts loader
    private ContactsLoaderCallbacks newLoaderCallbacks(String itemsFilter, boolean deleteCheckedItems) {
        return new ContactsLoaderCallbacks(getContext(), contactType,
                cursorAdapter, itemsFilter, deleteCheckedItems);
    }

    // Opens fragment for contact editing
    private void editContact(long id) {
        Bundle arguments = new Bundle();
        arguments.putInt(CONTACT_ID, (int)id);
        arguments.putInt(CONTACT_TYPE, contactType);
        CustomFragmentActivity.show(getActivity(), getString(R.string.editing_contact),
                AddOrEditContactFragment.class, arguments, 0);
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
            DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
            if(db == null) {
                return null;
            }
            if (deletingItems != null) {
                db.deleteContacts(contactType, deletingItems, itemsFilter);
            }
            return db.getContacts(contactType, itemsFilter);
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
