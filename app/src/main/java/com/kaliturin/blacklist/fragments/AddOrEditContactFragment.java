/*
 * Copyright (C) 2017 Anton Kaliturin <kaliturin@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.kaliturin.blacklist.fragments;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;

import com.kaliturin.blacklist.R;
import com.kaliturin.blacklist.utils.ButtonsBar;
import com.kaliturin.blacklist.utils.ContactsAccessHelper;
import com.kaliturin.blacklist.utils.ContactsAccessHelper.ContactSourceType;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.Contact;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.ContactCursorWrapper;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.ContactNumber;
import com.kaliturin.blacklist.utils.DialogBuilder;
import com.kaliturin.blacklist.utils.Permissions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Fragment for adding or editing contact
 */
public class AddOrEditContactFragment extends Fragment implements FragmentArguments {
    private int contactType = 0;
    private int contactId = -1;
    private LinearLayout numbersViewList;

    public AddOrEditContactFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments != null) {
            contactType = arguments.getInt(CONTACT_TYPE, 0);
            contactId = arguments.getInt(CONTACT_ID, -1);
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_or_edit_contact, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        numbersViewList = (LinearLayout) view.findViewById(R.id.numbers_list);

        // snack bar
        ButtonsBar snackBar = new ButtonsBar(view, R.id.three_buttons_bar);
        // "Cancel" button
        snackBar.setButton(R.id.button_left,
                getString(R.string.CANCEL),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finishActivity(Activity.RESULT_CANCELED);
                    }
                });
        // "Save" button
        snackBar.setButton(R.id.button_right,
                getString(R.string.SAVE),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!Permissions.notifyIfNotGranted(getContext(), Permissions.WRITE_EXTERNAL_STORAGE)) {
                            int result = (saveContact() ?
                                    Activity.RESULT_OK :
                                    Activity.RESULT_CANCELED);
                            finishActivity(result);
                        }
                    }
                });
        snackBar.show();

        // 'add new row' button click listener
        View button = view.findViewById(R.id.button_add_another);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddContactsMenuDialog();
            }
        });

        if (savedInstanceState == null) {
            if (contactId < 0) {
                // add the first empty row to the numbers list
                addRowToNumbersList("", ContactNumber.TYPE_EQUALS);
            } else {
                // get contact by id
                DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
                if (db != null) {
                    ContactCursorWrapper cursor = db.getContact(contactId);
                    if (cursor != null) {
                        // add contact numbers to the list
                        addRowsToNumbersList(cursor.getContact());
                        cursor.close();
                    } else {
                        finishActivity(Activity.RESULT_CANCELED);
                    }
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save numbers and types to the lists
        ArrayList<String> numbers = new ArrayList<>();
        ArrayList<Integer> types = new ArrayList<>();
        Set<Pair<String, Integer>> numbers2TypeSet = getNumber2TypePairs();
        for (Pair<String, Integer> pair : numbers2TypeSet) {
            numbers.add(pair.first);
            types.add(pair.second);
        }

        // save lists to state
        outState.putStringArrayList(CONTACT_NUMBERS, numbers);
        outState.putIntegerArrayList(CONTACT_NUMBER_TYPES, types);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // this is calling for receiving results from GetContactsFragment
        if (resultCode == Activity.RESULT_OK) {
            // add chosen resulting data to the list
            addRowsToNumbersList(data.getExtras());
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        // restore numbers list from saved state
        addRowsToNumbersList(savedInstanceState);
    }

//------------------------------------------------------------------------------------

    // Initializes the view with contact's data
    private void addRowsToNumbersList(Contact contact) {
        // contact name edit
        setName(contact.name);
        // add rows
        for (ContactNumber number : contact.numbers) {
            addRowToNumbersList(number.number, number.type);
        }
    }

    // Initializes the view with bundle data
    private void addRowsToNumbersList(Bundle data) {
        if (data == null) {
            return;
        }
        // get numbers and types from parameters
        ArrayList<String> numbers = data.getStringArrayList(CONTACT_NUMBERS);
        ArrayList<Integer> types = data.getIntegerArrayList(CONTACT_NUMBER_TYPES);
        if (numbers != null && types != null && numbers.size() == types.size()) {
            // get the set of unique pairs of numbers/types form the current view
            Set<Pair<String, Integer>> numbers2TypeSet = getNumber2TypePairs();
            for (int i = 0; i < numbers.size(); i++) {
                String number = numbers.get(i);
                int type = types.get(i);
                // add to View only rows with unique pair of number/type
                if (numbers2TypeSet.add(new Pair<>(number, type))) {
                    addRowToNumbersList(number, type);
                }
            }
        }
    }

    // Returns the set of unique pairs of numbers/types from the view
    private Set<Pair<String, Integer>> getNumber2TypePairs() {
        Set<Pair<String, Integer>> numbers2TypeSet = new HashSet<>();
        for (int i = 0; i < numbersViewList.getChildCount(); i++) {
            View row = numbersViewList.getChildAt(i);
            String number = getNumber(row);
            if (!number.isEmpty()) {
                int type = getNumberType(row);
                numbers2TypeSet.add(new Pair<>(number, type));
            }
        }
        return numbers2TypeSet;
    }

    // Saves contact data from the View to DB
    private boolean saveContact() {
        ContactsAccessHelper contactsAccessHelper =
                ContactsAccessHelper.getInstance(getContext());

        // get contact name
        String name = getName();

        // fill in the list of unique contact numbers
        List<ContactNumber> numbers = new LinkedList<>();
        Set<Pair<String, Integer>> numbers2TypeSet = getNumber2TypePairs();
        for (Pair<String, Integer> pair : numbers2TypeSet) {
            String number = contactsAccessHelper.normalizePhoneNumber(pair.first);
            if (number.isEmpty()) {
                continue;
            }
            int type = pair.second;
            numbers.add(new ContactNumber(0, number, type, 0));
        }

        // if there is not any number - take a name as a number
        if (numbers.isEmpty() && !name.isEmpty()) {
            numbers.add(new ContactNumber(0, name, 0));
        }

        // nothing to save
        if (numbers.isEmpty()) {
            return false;
        }

        if (name.isEmpty()) {
            // if name isn't defined
            if (numbers.size() == 1) {
                // if a single number - get it as a name
                name = numbers.get(0).number;
            } else {
                // get default name
                name = getContext().getString(R.string.Unnamed);
            }
        }

        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
        if (db != null) {
            if (contactId >= 0) {
                // delete the old contact
                db.deleteContact(contactId);
            }
            // save the new contact
            db.addContact(contactType, name, numbers);
        }

        return true;
    }

    // Adds row to the numbers list
    private boolean addRowToNumbersList(@NonNull String number, int type) {
        // create and add the new row
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View row = inflater.inflate(R.layout.row_contact_number, numbersViewList, false);
        numbersViewList.addView(row);

        // init row with number data
        setNumberType(row, type);
        setNumber(row, number);

        // init 'row remove' button
        ImageButton buttonRemove = (ImageButton) row.findViewById(R.id.button_remove);
        buttonRemove.setTag(row);
        buttonRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numbersViewList.removeView((View) v.getTag());
            }
        });

        // scroll list down
        moveScroll();

        return true;
    }

    private void moveScroll() {
        View view = getView();
        if (view != null) {
            final ScrollView scroll = (ScrollView) view.findViewById(R.id.scroll);
            scroll.post(new Runnable() {
                @Override
                public void run() {
                    scroll.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    }

    private String getName() {
        View view = getView();
        if (view != null) {
            EditText nameEdit = (EditText) view.findViewById(R.id.edit_name);
            return nameEdit.getText().toString().trim();
        }
        return "";
    }

    private void setName(String name) {
        View view = getView();
        if (view != null) {
            EditText nameEdit = (EditText) view.findViewById(R.id.edit_name);
            nameEdit.setText(name);
        }
    }

    private String getNumber(View row) {
        EditText numberEdit = (EditText) row.findViewById(R.id.edit_number);
        return numberEdit.getText().toString().trim();
    }

    private void setNumber(View row, String number) {
        final EditText numberEdit = (EditText) row.findViewById(R.id.edit_number);
        numberEdit.setText(number);
        if (number == null || number.isEmpty()) {
            numberEdit.postDelayed(new Runnable() {
                @Override
                public void run() {
                    numberEdit.requestFocus();
                }
            }, 100);
        }
    }

    private int getNumberType(View row) {
        Spinner numberTypeSpinner = (Spinner) row.findViewById(R.id.spinner_number_type);
        switch (numberTypeSpinner.getSelectedItemPosition()) {
            case 1:
                return ContactNumber.TYPE_CONTAINS;
            case 2:
                return ContactNumber.TYPE_STARTS;
            case 3:
                return ContactNumber.TYPE_ENDS;
        }

        return ContactNumber.TYPE_EQUALS;
    }

    private void setNumberType(View row, int type) {
        int position = 0;
        switch (type) {
            case ContactNumber.TYPE_CONTAINS:
                position = 1;
                break;
            case ContactNumber.TYPE_STARTS:
                position = 2;
                break;
            case ContactNumber.TYPE_ENDS:
                position = 3;
                break;
        }
        Spinner numberTypeSpinner = (Spinner) row.findViewById(R.id.spinner_number_type);
        numberTypeSpinner.setSelection(position);
    }

    private void finishActivity(int result) {
        getActivity().setResult(result);
        getActivity().finish();
    }

    // Shows menu dialog of contacts adding
    private void showAddContactsMenuDialog() {
        // create and show menu dialog for actions with the contact
        DialogBuilder dialog = new DialogBuilder(getContext());
        dialog.setTitle(R.string.Add_number).
                addItem(R.string.From_contacts_list, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showGetContactsFragment(ContactSourceType.FROM_CONTACTS);
                    }
                }).
                addItem(R.string.From_calls_list, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showGetContactsFragment(ContactSourceType.FROM_CALLS_LOG);
                    }
                }).
                addItem(R.string.From_SMS_list, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showGetContactsFragment(ContactSourceType.FROM_SMS_LIST);
                    }
                }).
                addItem(R.string.From_Black_list, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showGetContactsFragment(ContactSourceType.FROM_BLACK_LIST);
                    }
                }).
                addItem(R.string.From_White_list, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showGetContactsFragment(ContactSourceType.FROM_WHITE_LIST);
                    }
                }).
                addItem(R.string.Manually, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addRowToNumbersList("", ContactNumber.TYPE_EQUALS);
                    }
                }).
                show();
    }

    private void showGetContactsFragment(ContactSourceType sourceType) {
        GetContactsFragment.show(this, sourceType, true);
    }
}
