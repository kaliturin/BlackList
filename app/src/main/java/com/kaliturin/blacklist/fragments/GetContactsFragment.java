package com.kaliturin.blacklist.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.LongSparseArray;

import com.kaliturin.blacklist.R;
import com.kaliturin.blacklist.activities.CustomFragmentActivity;
import com.kaliturin.blacklist.utils.ContactsAccessHelper;
import com.kaliturin.blacklist.utils.ContactsAccessHelper.ContactSourceType;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.Contact;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.ContactNumber;
import com.kaliturin.blacklist.utils.Permissions;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for getting the list of chosen contacts
 */
public class GetContactsFragment extends AddContactsFragment {
    @Override
    protected void addContacts(List<Contact> contacts, LongSparseArray<ContactNumber> singleContactNumbers) {
        // prepare returning arguments - data of the chosen contacts
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> numbers = new ArrayList<>();
        ArrayList<Integer> types = new ArrayList<>();
        for (Contact contact : contacts) {
            ContactNumber contactNumber = singleContactNumbers.get(contact.id);
            if (contactNumber != null) {
                // add single number of the contact
                names.add(contact.name);
                numbers.add(contactNumber.number);
                types.add(contactNumber.type);
            } else {
                // all numbers of the contact
                for (ContactNumber _contactNumber : contact.numbers) {
                    names.add(contact.name);
                    numbers.add(_contactNumber.number);
                    types.add(_contactNumber.type);
                }
            }
        }

        // return arguments
        Intent intent = new Intent();
        intent.putStringArrayListExtra(CONTACT_NAMES, names);
        intent.putStringArrayListExtra(CONTACT_NUMBERS, numbers);
        intent.putIntegerArrayListExtra(CONTACT_NUMBER_TYPES, types);
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }

    // Shows custom activity with the fragment
    public static void show(Fragment parent, ContactSourceType sourceType, boolean singleNumberMode) {
        Context context = parent.getContext();

        // check permission
        final String permission = ContactsAccessHelper.getPermission(sourceType);
        if (permission == null || Permissions.notifyIfNotGranted(context, permission)) {
            return;
        }

        // create fragment's args
        Bundle arguments = new Bundle();
        arguments.putSerializable(SOURCE_TYPE, sourceType);
        arguments.putBoolean(SINGLE_NUMBER_MODE, singleNumberMode);

        // open the dialog activity with the fragment
        CustomFragmentActivity.show(context, parent,
                getTitleId(context, sourceType),
                GetContactsFragment.class, arguments, 0);
    }

    private static String getTitleId(Context context, ContactSourceType sourceType) {
        switch (sourceType) {
            case FROM_CONTACTS:
                return context.getString(R.string.List_of_contacts);
            case FROM_CALLS_LOG:
                return context.getString(R.string.List_of_calls);
            case FROM_SMS_LIST:
                return context.getString(R.string.List_of_SMS);
            case FROM_BLACK_LIST:
                return context.getString(R.string.Black_list);
            case FROM_WHITE_LIST:
                return context.getString(R.string.White_list);
        }
        return "";
    }

}