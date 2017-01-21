package com.kaliturin.blacklist;


import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.util.LinkedList;
import java.util.List;

import com.kaliturin.blacklist.DatabaseAccessHelper.ContactNumber;


/**
 * Fragment for manual adding of contact to the black/white list
 */
public class AddContactManualFragment extends Fragment {
    // bundle argument name
    public static final String CONTACT_TYPE = "CONTACT_TYPE";
    private int contactType = 0;

    public AddContactManualFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        contactType = bundle.getInt(CONTACT_TYPE);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_contact_manual, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // snack bar
        SnackBarCustom snackBar = new SnackBarCustom(view, R.id.snack_bar);
        // "Add" button
        snackBar.setButton(R.id.button_left,
                getString(R.string.add),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        saveContact();
                        finishActivity(Activity.RESULT_OK);
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

        snackBar.show();

        // add the first row to the phones list
        phonesListAddRow();

        View addAnother = view.findViewById(R.id.view_add_another);
        addAnother.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // add another row by button click
                phonesListAddRow();
            }
        });
    }

    // Adds row to the phones list
    private void phonesListAddRow() {
        View parent = getView();
        if(parent != null) {
            LinearLayout phonesListLayout = (LinearLayout) parent.findViewById(R.id.layout_phones_list);
            LayoutInflater inflater = getLayoutInflater(null);
            View row = inflater.inflate(R.layout.phone_row, phonesListLayout, false);
            phonesListLayout.addView(row);
        }
    }

    // Saves contacts with phone number(s)
    private void saveContact() {
        View parent = getView();
        if(parent == null) return;

        // get list of contact phones
        List<ContactNumber> numbers = new LinkedList<>();
        LinearLayout phonesListLayout = (LinearLayout) parent.findViewById(R.id.layout_phones_list);
        for(int i=0; i<phonesListLayout.getChildCount(); i++) {
            View row = phonesListLayout.getChildAt(i);
            String number = getNumber(row);
            if(!number.isEmpty()) {
                int type = getNumberType(row);
                numbers.add(new ContactNumber(i, number, type, 0));
            }
        }

        // nothing to save
        if(numbers.isEmpty()) return;

        // get contact name
        String name = getName(parent);
        if(name.isEmpty()) {
            // if name isn't defined
            if(numbers.size() == 1) {
                // if a single number - get it as a name
                name = numbers.get(0).number;
            } else {
                // get default name
                name = getContext().getString(R.string.unnamed);
            }
        }

        // save contact
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
        db.addContact(name, contactType, numbers);
    }

    private String getName(View parent) {
        EditText nameEdit = (EditText) parent.findViewById(R.id.edit_name);
        return nameEdit.getText().toString().trim();
    }

    private String getNumber(View row) {
        EditText phoneEditText = (EditText) row.findViewById(R.id.edit_text_phone);
        return phoneEditText.getText().toString().trim();
    }

    private int getNumberType(View row) {
        Spinner metadataSpinner = (Spinner) row.findViewById(R.id.spinner_metadata);
        switch (metadataSpinner.getSelectedItemPosition()) {
            case 1:
                return ContactNumber.TYPE_STARTS;
            case 2:
                return ContactNumber.TYPE_ENDS;
        }

        return ContactNumber.TYPE_EQUALS;
    }

    private void finishActivity(int result) {
        Activity activity = getActivity();
        if(activity != null) {
            activity.setResult(result);
            activity.finish();
        }
    }
}
