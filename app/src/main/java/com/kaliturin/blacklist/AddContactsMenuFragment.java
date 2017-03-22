package com.kaliturin.blacklist;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kaliturin.blacklist.ContactsAccessHelper.ContactSourceType;


/**
 * Fragment for representation of the menu of contacts sources
 * for choosing where from to add a contact
 */
@Deprecated
public class AddContactsMenuFragment extends Fragment implements FragmentArguments {
    private int contactType = 0;

    // On menu items click listener
    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ContactSourceType sourceType = null;
            String title = "";
            String permission = "";
            switch (v.getId()) {
                case R.id.add_from_contacts:
                    title = getString(R.string.List_of_contacts);
                    sourceType = ContactSourceType.FROM_CONTACTS;
                    permission = Permissions.READ_CONTACTS;
                    break;
                case R.id.add_from_calls:
                    title = getString(R.string.List_of_calls);
                    sourceType = ContactSourceType.FROM_CALLS_LOG;
                    permission = Permissions.READ_CALL_LOG;
                    break;
                case R.id.add_from_sms:
                    title = getString(R.string.List_of_inbox_SMS);
                    sourceType = ContactSourceType.FROM_SMS_INBOX;
                    permission = Permissions.READ_SMS;
                    break;
                case R.id.add_manually:
                    title = getString(R.string.Adding_contact);
                    permission = Permissions.WRITE_EXTERNAL_STORAGE;
                    break;
            }

            // if permission is granted
            if(!Permissions.notifyIfNotGranted(getContext(), permission)) {
                // permission is granted
                Bundle arguments = new Bundle();
                Class<? extends Fragment> fragmentClass;
                if (sourceType != null) {
                    // create fragment of adding contacts from inbox/calls
                    arguments.putInt(CONTACT_TYPE, contactType);
                    arguments.putSerializable(SOURCE_TYPE, sourceType);
                    fragmentClass = AddContactsFragment.class;
                } else {
                    // create fragment of adding contacts manually
                    arguments.putInt(CONTACT_TYPE, contactType);
                    fragmentClass = AddOrEditContactFragment.class;
                }

                // open the dialog activity with the fragment of contact adding
                CustomFragmentActivity.show(getActivity(), title, fragmentClass, arguments, 0);
            }
        }
    };

    public AddContactsMenuFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if(arguments != null) {
            contactType = arguments.getInt(CONTACT_TYPE, 0);
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_contacts_menu, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // add on items click listener
        view.findViewById(R.id.add_from_contacts).setOnClickListener(onClickListener);
        view.findViewById(R.id.add_from_calls).setOnClickListener(onClickListener);
        view.findViewById(R.id.add_from_sms).setOnClickListener(onClickListener);
        view.findViewById(R.id.add_manually).setOnClickListener(onClickListener);
    }
}
