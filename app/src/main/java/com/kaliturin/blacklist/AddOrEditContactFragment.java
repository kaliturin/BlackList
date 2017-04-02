package com.kaliturin.blacklist;


import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;

import com.kaliturin.blacklist.DatabaseAccessHelper.Contact;
import com.kaliturin.blacklist.DatabaseAccessHelper.ContactNumber;
import com.kaliturin.blacklist.DatabaseAccessHelper.ContactCursorWrapper;

import java.util.LinkedList;
import java.util.List;

/**
 * Fragment for adding or editing contact
 */
public class AddOrEditContactFragment extends Fragment implements FragmentArguments {
    private int contactType = 0;
    private int contactId = -1;

    public AddOrEditContactFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if(arguments != null) {
            contactType = arguments.getInt(CONTACT_TYPE, 0);
            contactId = arguments.getInt(CONTACT_ID, -1);
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_or_edit_contact, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
                        if(!Permissions.notifyIfNotGranted(getContext(), Permissions.WRITE_EXTERNAL_STORAGE)) {
                            int result = (saveContact() ?
                                    Activity.RESULT_OK :
                                    Activity.RESULT_CANCELED);
                            finishActivity(result);
                        }
                    }
                });
        snackBar.show();

        // 'add new row' button click listener
        View addAnother = view.findViewById(R.id.view_add_another);
        addAnother.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // add another row by button click
                numberListAddRow("", ContactNumber.TYPE_EQUALS);
            }
        });

        if(contactId < 0) {
            // add the first row to the numbers list
            numberListAddRow("", ContactNumber.TYPE_EQUALS);
        } else {
            // get contact by id
            DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
            if(db != null) {
                ContactCursorWrapper cursor = db.getContact(contactId);
                if (cursor != null) {
                    initView(view, cursor.getContact());
                    cursor.close();
                } else {
                    finishActivity(Activity.RESULT_CANCELED);
                }
            }
        }
    }

    // Initializes the view with contact's data
    private void initView(View view, Contact contact) {
        // contact name edit
        setName(view, contact.name);

        // add numbers rows
        for(ContactNumber number : contact.numbers) {
            numberListAddRow(number.number, number.type);
        }
    }

    // Saves contact data from view to DB
    private boolean saveContact() {
        View parent = getView();
        if(parent == null) return false;

        // get contact name
        String name = getName(parent);

        // get list of contact phones
        List<ContactNumber> numbers = new LinkedList<>();
        LinearLayout numberListLayout = (LinearLayout) parent.findViewById(R.id.layout_number_list);
        for(int i=0; i<numberListLayout.getChildCount(); i++) {
            View row = numberListLayout.getChildAt(i);
            String number = getNumber(row);
            if(!number.isEmpty()) {
                int type = getNumberType(row);
                numbers.add(new ContactNumber(i, number, type, 0));
            }
        }

        // if there is not any number - take name as number
        if(numbers.isEmpty() && !name.isEmpty()) {
            numbers.add(new ContactNumber(0, name, 0));
        }

        // nothing to save
        if(numbers.isEmpty()) {
            return false;
        }

        if(name.isEmpty()) {
            // if name isn't defined
            if(numbers.size() == 1) {
                // if a single number - get it as a name
                name = numbers.get(0).number;
            } else {
                // get default name
                name = getContext().getString(R.string.Unnamed);
            }
        }

        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
        if(db != null) {
            if (contactId >= 0) {
                // delete the old contact
                db.deleteContact(contactId);
            }
            // save the new contact
            db.addContact(contactType, name, numbers);
        }

        return true;
    }

    // Adds row to the phones list
    private void numberListAddRow(String number, int type) {
        View parent = getView();
        if(parent == null) {
            return;
        }
        final LinearLayout numberRowsListLayout =
                (LinearLayout) parent.findViewById(R.id.layout_number_list);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // create new row
        View row = inflater.inflate(R.layout.row_contact_number, numberRowsListLayout, false);
        // save row
        numberRowsListLayout.addView(row);
        // init row with number data
        setNumberType(row, type);
        setNumber(row, number);
        // init 'row remove' button
        ImageButton buttonRemove = (ImageButton) row.findViewById(R.id.button_remove);
        buttonRemove.setTag(row);
        buttonRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numberRowsListLayout.removeView((View)v.getTag());
            }
        });
        // scroll list down
        moveScroll(parent);
        setFocus(row);
    }

    private void moveScroll(View parent) {
        final ScrollView scroll = (ScrollView) parent.findViewById(R.id.scroll);
        scroll.post(new Runnable() {
            @Override
            public void run() {
                scroll.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void setFocus(final View row) {
        row.post(new Runnable() {
            @Override
            public void run() {
                EditText numberEdit = (EditText) row.findViewById(R.id.edit_number);
                numberEdit.requestFocus();
            }
        });
    }

    private String getName(View parent) {
        EditText nameEdit = (EditText) parent.findViewById(R.id.edit_name);
        return nameEdit.getText().toString().trim();
    }

    private void setName(View parent, String name) {
        EditText nameEdit = (EditText) parent.findViewById(R.id.edit_name);
        nameEdit.setText(name);
    }

    private String getNumber(View row) {
        EditText numberEdit = (EditText) row.findViewById(R.id.edit_number);
        return numberEdit.getText().toString().trim();
    }

    private void setNumber(View row, String number) {
        EditText numberEdit = (EditText) row.findViewById(R.id.edit_number);
        numberEdit.setText(number);
    }

    private int getNumberType(View row) {
        Spinner numberTypeSpinner = (Spinner) row.findViewById(R.id.spinner_number_type);
        switch (numberTypeSpinner.getSelectedItemPosition()) {
            case 1:
                return ContactNumber.TYPE_STARTS;
            case 2:
                return ContactNumber.TYPE_ENDS;
            case 3:
                return ContactNumber.TYPE_CONTAINS;
        }

        return ContactNumber.TYPE_EQUALS;
    }

    private void setNumberType(View row, int type) {
        int position = 0;
        switch (type) {
            case ContactNumber.TYPE_STARTS:
                position = 1;
                break;
            case ContactNumber.TYPE_ENDS:
                position = 2;
                break;
            case ContactNumber.TYPE_CONTAINS:
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
}
