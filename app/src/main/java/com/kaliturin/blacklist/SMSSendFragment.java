package com.kaliturin.blacklist;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.util.LongSparseArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.kaliturin.blacklist.DatabaseAccessHelper.Contact;
import com.kaliturin.blacklist.DatabaseAccessHelper.ContactNumber;
import com.kaliturin.blacklist.ContactsAccessHelper.ContactSourceType;


/**
 * Fragment of SMS sending.
 */
public class SMSSendFragment extends Fragment implements FragmentArguments {
    private static final String CONTACT_NUMBERS = "CONTACT_NUMBERS";
    private static final String CONTACT_NAMES = "CONTACT_NAMES";
    private static final int SMS_LENGTH = 160;
    private static final int SMS_LENGTH2 = 153;
    private static final int SMS_LENGTH_UNICODE = 70;
    private static final int SMS_LENGTH2_UNICODE = 67;
    private ContactsContainer contactsContainer = new ContactsContainer();

    public SMSSendFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_send_sms, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // message body edit
        final EditText messageEdit = (EditText) view.findViewById(R.id.text_message);

        if(savedInstanceState != null) {
            // restore contacts list from saved state
            addRowsToContactsViewList(savedInstanceState);
        } else {
            // get contact from arguments
            Bundle arguments = getArguments();
            if(arguments != null) {
                String name = arguments.getString(CONTACT_NAME);
                if(name == null) {
                    name = "";
                }
                String number = arguments.getString(CONTACT_NUMBER);
                if(number != null) {
                    // add row to contacts list
                    addRowToContactsViewList(number, name);
                }
                String body = arguments.getString(SMS_MESSAGE_BODY);
                if(body != null) {
                    messageEdit.setText(body);
                }
            }
        }

        // phone number edit
        final EditText numberEdit = (EditText) view.findViewById(R.id.edit_number);

        // add contact from contacts list
        View addContactView = view.findViewById(R.id.button_add_contact);
        addContactView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String number = numberEdit.getText().toString().trim();
                if(!number.isEmpty()) {
                    // add number to contacts list
                    addRowToContactsViewList(number, "");
                    numberEdit.setText("");
                } else {
                    // open menu dialog
                    showAddContactsMenuDialog();
                }
            }
        });

        // message text changed listener
        final TextView lengthMessageText = (TextView) view.findViewById(R.id.text_message_length);
        messageEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                Editable editable = messageEdit.getText();
                int messageLength = editable.length();

                // is there unicode character in the message?
                boolean unicode = false;
                for(int i=0; i<messageLength; i++) {
                    char c = editable.charAt(i);
                    if (Character.UnicodeBlock.of(c) != Character.UnicodeBlock.BASIC_LATIN) {
                        unicode = true;
                        break;
                    }
                }

                // get max length of sms part depending on encoding and full length
                int length1 = (unicode ? SMS_LENGTH_UNICODE : SMS_LENGTH);
                int length2 = (unicode ? SMS_LENGTH2_UNICODE : SMS_LENGTH2);
                int partMaxLength = (messageLength > length1 ? length2 : length1);
                // create current length status info
                int partsNumber = messageLength/partMaxLength + 1;
                int partLength = partMaxLength - messageLength % partMaxLength;
                // correct length info for second part
                if(partsNumber == 2 && partLength == partMaxLength) {
                    partLength = length1 - (length1 - length2) * 2;
                }

                // show current length status info
                String text = "" + partLength + "/" + partsNumber;
                lengthMessageText.setText(text);
            }
        });

        // init send button
        Button sendButton = (Button) view.findViewById(R.id.button_send);
        sendButton.setTransformationMethod(null);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sendSMSMessage()) {
                    finishActivity(Activity.RESULT_OK);
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK) {
            // add chosen contacts to the list
            addRowsToContactsViewList(data.getExtras());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(CONTACT_NUMBERS, contactsContainer.getNumbers());
        outState.putStringArrayList(CONTACT_NAMES, contactsContainer.getNames());
    }

    private void finishActivity(int result) {
        getActivity().setResult(result);
        getActivity().finish();
    }

//-------------------------------------------------------------

    // Sends SMS message
    boolean sendSMSMessage() {
        if(Permissions.notifyIfNotGranted(getContext(), Permissions.SEND_SMS)) {
            return false;
        }

        View view = getView();
        if(view == null) {
            return false;
        }

        // add phone number from EditText to the container
        EditText numberEdit = (EditText) view.findViewById(R.id.edit_number);
        String number_ = numberEdit.getText().toString().trim();
        contactsContainer.add(number_, "");
        if(contactsContainer.size() == 0) {
            Toast.makeText(getContext(), R.string.Address_is_not_defined, Toast.LENGTH_SHORT).show();
            return false;
        }

        // get SMS message text
        EditText messageEdit = (EditText) getView().findViewById(R.id.text_message);
        String message = messageEdit.getText().toString();
        if(message.isEmpty()) {
            Toast.makeText(getContext(), R.string.Message_text_is_not_defined, Toast.LENGTH_SHORT).show();
            return false;
        }

        // TODO do it in a thread
        // send SMS message to the all contacts in container
        SMSSendHelper smsSendHelper = new SMSSendHelper();
        List<String> numbers = contactsContainer.getNumbers();
        for(String number : numbers) {
            smsSendHelper.sendSMS(getContext(), number, message);
        }

        return true;
    }

    // Creates and adds rows to the contacts list
    private void addRowsToContactsViewList(Bundle data) {
        if(data == null) {
            return;
        }
        ArrayList<String> numbers = data.getStringArrayList(CONTACT_NUMBERS);
        ArrayList<String> names = data.getStringArrayList(CONTACT_NAMES);
        if(numbers != null && names != null && numbers.size() == names.size()) {
            for (int i=0; i<numbers.size(); i++) {
                String number = numbers.get(i);
                String name = names.get(i);
                addRowToContactsViewList(number, name);
            }
        }
    }

    // Creates and adds a row to the contacts list
    private boolean addRowToContactsViewList(@NonNull String number, @NonNull String name) {
        View view = getView();
        if(view == null) {
            return false;
        }

        // add contact to the container
        if(!contactsContainer.add(number, name)) {
            return false;
        }

        // create and add a new row
        final LinearLayout contactsViewList = (LinearLayout) view.findViewById(R.id.contacts_list);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View row = inflater.inflate(R.layout.row_sms_send_contact, contactsViewList, false);
        row.setTag(number);
        contactsViewList.addView(row);

        // init contact's info view
        String text = number;
        if(!name.isEmpty() && !name.equals(number)) {
            text = name + " (" + number + ")";
        }
        TextView textView = (TextView) row.findViewById(R.id.contact_number);
        textView.setText(text);

        // init button of removing
        ImageButton buttonRemove = (ImageButton) row.findViewById(R.id.button_remove);
        buttonRemove.setTag(row);
        buttonRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View row = (View) v.getTag();
                String number = (String) row.getTag();
                // remove contact from the container
                contactsContainer.remove(number);
                // remove row from the view-list
                contactsViewList.removeView(row);
            }
        });

        moveScroll(view);

        return true;
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

    // Shows menu dialog of contacts adding
    private void showAddContactsMenuDialog() {
        // create and show menu dialog for actions with the contact
        DialogBuilder dialog = new DialogBuilder(getContext());
        dialog.setTitle(R.string.Add_number).
                addItem(R.string.From_contacts_list, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAddContactsActivity(Permissions.READ_CONTACTS,
                                ContactSourceType.FROM_CONTACTS,
                                R.string.List_of_contacts);
                    }
                }).
                addItem(R.string.From_calls_list, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAddContactsActivity(Permissions.READ_CALL_LOG,
                                ContactsAccessHelper.ContactSourceType.FROM_CALLS_LOG,
                                R.string.List_of_calls);
                    }
                }).
                addItem(R.string.From_SMS_list, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAddContactsActivity(Permissions.READ_SMS,
                                ContactsAccessHelper.ContactSourceType.FROM_SMS_INBOX,
                                R.string.List_of_inbox_SMS);
                    }
                }).show();
    }

    // Shows activity of of contacts adding
    private void showAddContactsActivity(String permission,
                                         ContactSourceType sourceType, @StringRes int titleId) {
        // if permission isn't granted
        if (Permissions.notifyIfNotGranted(getContext(), permission)) {
            return;
        }

        // create fragment of adding contacts from inbox/calls
        Bundle arguments = new Bundle();
        arguments.putSerializable(SOURCE_TYPE, sourceType);
        arguments.putBoolean(SINGLE_NUMBER_MODE, true);

        // open the dialog activity with the fragment of contacts adding
        CustomFragmentActivity.show(getActivity(), this,
                getString(titleId), AddSmsContactsFragment.class, arguments, 0);
    }

    // Fragment for adding contacts for SMS sending
    public static class AddSmsContactsFragment extends AddContactsFragment {
        @Override
        protected void addContacts(List<Contact> contacts, LongSparseArray<String> contactIdToNumber) {
            // prepare returning arguments - chosen contacts
            ContactsContainer contactsContainer = new ContactsContainer();
            for(Contact contact : contacts) {
                String number = contactIdToNumber.get(contact.id);
                if(number != null) {
                    // add single number of the contact
                    contactsContainer.add(number, contact.name);
                } else {
                    // add all numbers of the contact
                    for(ContactNumber contactNumber : contact.numbers) {
                        contactsContainer.add(contactNumber.number, contact.name);
                    }
                }
            }

            // return arguments
            Intent intent = new Intent();
            intent.putStringArrayListExtra(CONTACT_NUMBERS, contactsContainer.getNumbers());
            intent.putStringArrayListExtra(CONTACT_NAMES, contactsContainer.getNames());
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        }
    }

    // Contacts container
    private static class ContactsContainer {
        private ArrayList<String> numbers = new ArrayList<>();
        private ArrayList<String> names = new ArrayList<>();

        int size() {
            return numbers.size();
        }

        boolean add(String number, String name) {
            if(!number.isEmpty() && !numbers.contains(number)) {
                numbers.add(number);
                names.add(name);
                return true;
            }
            return false;
        }

        void remove(String number) {
            int i = numbers.indexOf(number);
            if(i >=0 && i < names.size()) {
                numbers.remove(i);
                names.remove(i);
            }
        }

        ArrayList<String> getNumbers() {
            return numbers;
        }

        ArrayList<String> getNames() {
            return names;
        }
    }
}
