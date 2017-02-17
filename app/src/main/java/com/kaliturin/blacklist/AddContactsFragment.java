package com.kaliturin.blacklist;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
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

import com.kaliturin.blacklist.ContactsAccessHelper.ContactSourceType;
import com.kaliturin.blacklist.DatabaseAccessHelper.Contact;

import java.util.List;

/**
 * Fragment for representation the list of contacts to choose
 * which one is adding to the black/white list
 */
public class AddContactsFragment extends Fragment {
    // bundle arguments names
    public static final String CONTACT_TYPE = "CONTACT_TYPE";
    public static final String SOURCE_TYPE = "SOURCE_TYPE";

    private ContactsCursorAdapter cursorAdapter = null;
    private CustomSnackBar snackBar = null;
    private ContactSourceType sourceType = null;
    private int contactType = 0;

    public AddContactsFragment() {
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
        contactType = bundle.getInt(CONTACT_TYPE);
        sourceType = (ContactSourceType) bundle.getSerializable(SOURCE_TYPE);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_contacts, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // snack bar
        snackBar = new CustomSnackBar(view, R.id.snack_bar);
        // "Select all" button
        snackBar.setButton(R.id.button_left,
                getString(R.string.select_all),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setCheckedAllItems();
                    }
                });
        // "Add" button
        snackBar.setButton(R.id.button_center,
                getString(R.string.add),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        snackBar.dismiss();
                        // write checked contacts to the DB
                        writeCheckedContacts();
                    }
                });
        // "Cancel button" button
        snackBar.setButton(R.id.button_right,
                getString(R.string.cancel),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finishActivity(Activity.RESULT_CANCELED);
                    }
                });


        // cursor adapter
        cursorAdapter = new ContactsCursorAdapter(getContext());
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

        // add cursor listener to the list
        ListView listView = (ListView) view.findViewById(R.id.contacts_list);
        listView.setAdapter(cursorAdapter);

        // init and run the loader of contacts
        getLoaderManager().initLoader(0, null, newLoaderCallbacks(null));
    }

    @Override
    public void onDestroyView() {
        getLoaderManager().destroyLoader(0);
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
        MenuItem itemSearch = menu.findItem(R.id.action_search);
        Utils.tintMenuIcon(getContext(), itemSearch, R.color.colorAccent);
        itemSearch.setVisible(true);

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

        super.onCreateOptionsMenu(menu, inflater);
    }

//-------------------------------------------------------------------

    // Clears all items selection
    private void clearCheckedItems() {
        if(cursorAdapter != null) {
            cursorAdapter.setAllItemsChecked(false);
        }
    }

    // Sets all items selected
    private void setCheckedAllItems() {
        if(cursorAdapter != null) {
            cursorAdapter.setAllItemsChecked(true);
        }
    }

    // Closes snack bar
    public boolean dismissSnackBar() {
        clearCheckedItems();
        return snackBar != null && snackBar.dismiss();
    }

    // Reloads items
    private void reloadItems(String itemsFilter) {
        dismissSnackBar();
        getLoaderManager().restartLoader(0, null, newLoaderCallbacks(itemsFilter));
    }

    // Creates new contacts loader
    private ContactsLoaderCallbacks newLoaderCallbacks(String itemsFilter) {
        return new ContactsLoaderCallbacks(getContext(), sourceType, cursorAdapter, itemsFilter);
    }

//-------------------------------------------------------------------

    // Contact items loader
    private static class ContactsLoader extends CursorLoader {
        private ContactSourceType sourceType;
        private String itemsFilter;

        ContactsLoader(Context context,
                       ContactSourceType sourceType,
                       String itemsFilter) {
            super(context);
            this.sourceType = sourceType;
            this.itemsFilter = itemsFilter;
        }

        @Override
        public Cursor loadInBackground() {
            ContactsAccessHelper dao = ContactsAccessHelper.getInstance(getContext());
            return dao.getContacts(getContext(), sourceType, itemsFilter);
        }
    }

    // Contact items loader callbacks
    private static class ContactsLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        private Context context;
        private ContactSourceType sourceType;
        private ContactsCursorAdapter cursorAdapter;
        private String itemsFilter;

        ContactsLoaderCallbacks(Context context,
                                ContactSourceType sourceType,
                                ContactsCursorAdapter cursorAdapter,
                                String itemsFilter) {
            this.context = context;
            this.sourceType = sourceType;
            this.cursorAdapter = cursorAdapter;
            this.itemsFilter = itemsFilter;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new ContactsLoader(context, sourceType, itemsFilter);
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

//-------------------------------------------------------------------

    private void finishActivity(int result) {
        getActivity().setResult(result);
        getActivity().finish();
    }

    // Writes checked contacts to the database
    private void writeCheckedContacts() {
        // if permission is granted
        if(!Permissions.notifyIfNotGranted(getActivity(), Permissions.WRITE_EXTERNAL_STORAGE)) {
            // get list of contacts and write it to the DB
            List<Contact> contactList = cursorAdapter.extractCheckedContacts();
            ContactsWriter writer = new ContactsWriter(getContext(), contactType, contactList);
            writer.execute();
        }
    }

    // Async task - writes contacts to the DB
    private class ContactsWriter extends AsyncTask<Void, Integer, Void> {
        List<Contact> contactList;
        private int contactType;
        private ProgressDialog progressBar;

        ContactsWriter(Context context, int contactType, List<Contact> contactList) {
            this.contactType = contactType;
            this.contactList = contactList;

            progressBar = new ProgressDialog(context);
            progressBar.setTitle(getString(R.string.saving));
            progressBar.setCancelable(true);
            progressBar.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    ContactsWriter.this.cancel(true);
                }
            });
        }

        @Override
        protected Void doInBackground(Void... params) {
            DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
            if(db != null) {
                int count = 1;
                for (Contact contact : contactList) {
                    if (isCancelled()) break;
                    db.addContact(contactType, contact.name, contact.numbers);
                    publishProgress(100 / contactList.size() * count++);
                }
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            progressBar.dismiss();
            finishActivity(Activity.RESULT_OK);
        }

        @Override
        protected void onPostExecute(Void result) {
            progressBar.dismiss();
            finishActivity(Activity.RESULT_OK);
        }

        @Override
        protected void onPreExecute() {
            progressBar.setMessage("0%");
            progressBar.show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setProgress(values[0]);
            progressBar.setMessage("" + values[0] + "%");
        }
    }
}
