package com.kaliturin.blacklist;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.kaliturin.blacklist.DatabaseAccessHelper.Contact;

/**
 * Popup dialog with menu of contact changing
 */
public class ContactMenuPopupDialog extends DialogFragment {
    public static final String CONTACT_ID = "CONTACT_ID";
    public static final String CONTACT_TYPE = "CONTACT_TYPE";
    public static final String CONTACT_NAME = "CONTACT_NAME";
    public static final int REQUEST_CODE = 0x10;

    public ContactMenuPopupDialog() {}

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // get arguments
        Bundle bundle = getArguments();
        final int contactId = bundle.getInt(CONTACT_ID, -1);
        final int contactType = bundle.getInt(CONTACT_TYPE, -1);
        final String contactName = bundle.getString(CONTACT_NAME);

        // menu items
        String[] items = new String[]{
                getString(R.string.delete),
                getString(R.string.edit)
        };

        // create title
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View titleView = inflater.inflate(R.layout.contact_menu_title, null);
        TextView textView = (TextView) titleView.findViewById(R.id.contact_name);
        textView.setText(contactName);

        // build menu dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCustomTitle(titleView).
                setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                        switch (which) {
                            case 0:
                                deleteContact(contactId);
                                break;
                            case 1:
                                editContact(contactId, contactType);
                                break;
                        }

                    }
                });

        return builder.create();
    }

    // Delete contact from DB
    private void deleteContact(int contactId) {
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
        db.deleteContact(contactId);
        notifyParent(Activity.RESULT_OK);
    }

    // Opens dialog activity for contact editing
    private void editContact(int contactId, int contactType) {
        AddOrEditContactFragment fragment = new AddOrEditContactFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(AddOrEditContactFragment.CONTACT_ID, contactId);
        arguments.putInt(AddOrEditContactFragment.CONTACT_TYPE, contactType);
        fragment.setArguments(arguments);
        fragment.setTargetFragment(getTargetFragment(), getTargetRequestCode());
        String title = getString(R.string.editing_contact);
        DialogActivity.show(getActivity(), fragment, title, 0);
    }

    // Notifies parent fragment/activity
    private void notifyParent(int result) {
        getTargetFragment().onActivityResult(getTargetRequestCode(),
                result, getActivity().getIntent());
    }

    /**
     * Creates and shows contact menu dialog
     */
    public static void show(Fragment parent, Contact contact) {
        // create menu dialog arguments
        Bundle arguments = new Bundle();
        arguments.putInt(CONTACT_ID, (int) contact.id);
        arguments.putInt(CONTACT_TYPE, contact.type);
        arguments.putString(CONTACT_NAME, contact.name);
        // show menu dialog
        ContactMenuPopupDialog dialog = new ContactMenuPopupDialog();
        dialog.setArguments(arguments);
        dialog.setTargetFragment(parent, REQUEST_CODE);
        dialog.show(parent.getFragmentManager(), ContactMenuPopupDialog.class.getName());
    }
}
