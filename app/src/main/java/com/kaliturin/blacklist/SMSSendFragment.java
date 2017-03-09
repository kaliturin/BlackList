package com.kaliturin.blacklist;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

import com.kaliturin.blacklist.DatabaseAccessHelper.Contact;
import com.kaliturin.blacklist.DatabaseAccessHelper.ContactNumber;
import com.kaliturin.blacklist.ContactsAccessHelper.ContactSourceType;


/**
 * Fragment of SMS sending.
 */
public class SMSSendFragment extends Fragment implements FragmentArguments {
    static final String CONTACTS_LIST = "CONTACTS_LIST";
    static final String SEPARATOR = ">|<";
    static final int SMS_LENGTH = 160;
    static final int SMS_LENGTH_UNICODE = 70;

    private ArrayList<String> contactsStringList = new ArrayList<>();

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
            // restore numbers list
            List<String> list = savedInstanceState.getStringArrayList(CONTACTS_LIST);
            if(list != null) {
                for (String contact : list) {
                    addRowToContactsViewList(contact);
                }
            }
        } else {
            // get contact from arguments
            Bundle arguments = getArguments();
            if(arguments != null) {
                String name = arguments.getString(CONTACT_NAME);
                String number = arguments.getString(CONTACT_NUMBER);
                if(number != null) {
                    String contact = number;
                    if(name != null) {
                        contact += SEPARATOR + name;
                    }
                    addRowToContactsViewList(contact);
                }
                String body = arguments.getString(SMS_MESSAGE_BODY);
                if(body != null) {
                    messageEdit.setText(body);
                }
            }
        }

        // phone number edit
        final EditText numberEdit = (EditText) view.findViewById(R.id.edit_number);
        View addButton = view.findViewById(R.id.button_add_number);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String number = numberEdit.getText().toString().trim();
                if(!number.isEmpty()) {
                    addRowToContactsViewList(number);
                    numberEdit.setText("");
                } else {
                    showAddContactsMenuDialog();
                }
            }
        });

        // add contact from contacts list
        View addContactView = view.findViewById(R.id.button_add_contact);
        addContactView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddContactsMenuDialog();
            }
        });

        // message text changed listener
        final TextView lengthMessageText = (TextView) view.findViewById(R.id.text_message_length);
        messageEdit.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                // detect unicode characters and get max length of sms
                Editable editable = messageEdit.getText();
                int length = editable.length();
                int maxLength = SMS_LENGTH;
                for(int i=0; i<length; i++) {
                    char c = editable.charAt(i);
                    if (Character.UnicodeBlock.of(c) != Character.UnicodeBlock.BASIC_LATIN) {
                        maxLength = SMS_LENGTH_UNICODE;
                    }
                }

                // create message body length info
                int m = maxLength - length%maxLength;
                int n = length/maxLength + 1;
                String text = "" + m + "/" + n;
                lengthMessageText.setText(text);
            }
        });

        // init send button
        Button sendButton = (Button) view.findViewById(R.id.button_send);
        sendButton.setTransformationMethod(null);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSMSMessage();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK) {
            return;
        }

        // get resulting contacts array
        ArrayList<String> contacts = data.getStringArrayListExtra(CONTACTS_LIST);
        if(contacts == null) {
            return;
        }

        // get contacts and add rows to list
        for (String contact : contacts) {
            addRowToContactsViewList(contact);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(CONTACTS_LIST, contactsStringList);
    }

//-------------------------------------------------------------

    // Sends SMS message
    boolean sendSMSMessage() {
        if(!Permissions.isGranted(getContext(), Permissions.SEND_SMS)) {
            return false;
        }

        View view = getView();
        if(view == null) {
            return false;
        }

        EditText messageEdit = (EditText) getView().findViewById(R.id.text_message);
        String text = messageEdit.getText().toString();
        if(text.isEmpty()) {
            return false;
        }



        return false;
    }

    // Creates and adds a row to the contacts list from the passing string
    private boolean addRowToContactsViewList(String contact) {
        View view = getView();
        if(view == null) {
            return false;
        }

        // if contacts is already added
        if(contactsStringList.contains(contact)) {
            return true;
        }

        // get contact's number and name
        String number = contact;
        String name = "";
        int index = contact.indexOf(SEPARATOR);
        if(index >= 0 ) {
            number = contact.substring(0, index);
            name = contact.substring(index+SEPARATOR.length());
        }

        if(number.isEmpty()) {
            return false;
        }

        // create and add a new row
        final LinearLayout contactsViewList = (LinearLayout) view.findViewById(R.id.contacts_list);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View row = inflater.inflate(R.layout.row_sms_send_contact, contactsViewList, false);
        row.setTag(contact);
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
                String contact = (String) row.getTag();
                contactsStringList.remove(contact);
                contactsViewList.removeView(row);
            }
        });

        contactsStringList.add(contact);
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
        MenuDialogBuilder dialog = new MenuDialogBuilder(getActivity());
        dialog.setTitle(R.string.add_number).
                addItem(R.string.from_contacts_list, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAddContactsActivity(Permissions.READ_CONTACTS,
                                ContactSourceType.FROM_CONTACTS,
                                R.string.contacts_list);
                    }
                }).
                addItem(R.string.from_calls_list, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAddContactsActivity(Permissions.READ_CALL_LOG,
                                ContactsAccessHelper.ContactSourceType.FROM_CALLS_LOG,
                                R.string.calls_list);
                    }
                }).
                addItem(R.string.from_sms_list, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAddContactsActivity(Permissions.READ_SMS,
                                ContactsAccessHelper.ContactSourceType.FROM_SMS_INBOX,
                                R.string.sms_inbox_list);
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

        // open the dialog activity with the fragment of contact adding
        CustomFragmentActivity.show(getActivity(), this,
                getString(titleId), AddSmsContactsFragment.class, arguments, 0);
    }

    // Fragment for adding contacts for SMS sending
    public static class AddSmsContactsFragment extends AddContactsFragment {
        @Override
        protected void addContacts(List<Contact> contacts, LongSparseArray<String> contactIdToNumber) {
            ArrayList<String> list = new ArrayList<>();
            for(Contact contact : contacts) {
                String number = contactIdToNumber.get(contact.id);
                if(number != null) {
                    // add single number of the contact
                    list.add(number + SEPARATOR + contact.name);
                } else {
                    // add all numbers of contact
                    for(ContactNumber contactNumber : contact.numbers) {
                        list.add(contactNumber.number + SEPARATOR + contact.name);
                    }
                }
            }

            // return arguments
            Intent intent = new Intent();
            intent.putStringArrayListExtra(CONTACTS_LIST, list);
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        }
    }

    private void finishActivity(int result) {
        getActivity().setResult(result);
        getActivity().finish();
    }

    class TextWatcherAdapter implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}
