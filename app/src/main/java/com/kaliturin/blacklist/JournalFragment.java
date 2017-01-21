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
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Fragment for the journal (blocked calls/sms list) representation
 */
public class JournalFragment extends Fragment {
    private JournalCursorAdapter cursorAdapter = null;
    private SnackBarCustom snackBar = null;
    private String itemsFilter = null;

    public JournalFragment() {
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
        return inflater.inflate(R.layout.fragment_journal, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
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

        // add cursor listener to the journal list
        ListView listView = (ListView) view.findViewById(R.id.journal_list);
        listView.setAdapter(cursorAdapter);

        // init and run the journal records loader
        getLoaderManager().initLoader(0, null, newLoader(null, false));
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

    // Deletes all checked items
    private void deleteCheckedItems() {
        getLoaderManager().restartLoader(0, null, newLoader(itemsFilter, true));
    }

    // Reloads items
    private void reloadItems(String itemsFilter) {
        this.itemsFilter = itemsFilter;
        dismissSnackBar();
        getLoaderManager().restartLoader(0, null, newLoader(itemsFilter, false));
    }

    // Creates new journal items loader
    private JournalItemsLoaderCallbacks newLoader(String itemsFilter,
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
            DatabaseAccessHelper dao = DatabaseAccessHelper.getInstance(getContext());
            if (deletingItems != null) {
                dao.deleteJournalRecords(deletingItems, itemsFilter);
            }
            return dao.getJournalRecords(itemsFilter);
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

