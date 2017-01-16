package com.kaliturin.blacklist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;
import java.util.List;

import com.android.internal.telephony.ITelephony;

import com.kaliturin.blacklist.DatabaseAccessHelper.Contact;

/**
 * BroadcastReceiver for calls catching
 */

public class CallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        // if block calls not enabled
        if(!Settings.getBooleanValue(context, Settings.BLOCK_CALLS)) {
            return;
        }

        // get telephony service
        TelephonyManager telephony = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        if(telephony.getCallState() != TelephonyManager.CALL_STATE_RINGING) {
            return;
        }

        // get incoming call number
        String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

        // private number detected
        if(isPrivateNumber(number)) {
            // if block private numbers
            if(Settings.getBooleanValue(context, Settings.BLOCK_HIDDEN_CALLS)) {
                String name = context.getString(R.string.hidden);
                // break call and notify
                breakCallAndNotify(context, name, name);
            }
            return;
        }

        // get contacts linked to the current number
        List<Contact> contacts = getContacts(context, number);

        // if block all calls
        if(Settings.getBooleanValue(context, Settings.BLOCK_ALL_CALLS)) {
            String name = getContactName(contacts, number);
            // break call and notify
            breakCallAndNotify(context, name, name);
            return;
        }

        // is contact is int the white list
        Contact contact = findContactByType(contacts, Contact.TYPE_WHITE_LIST);
        if(contact != null) {
            // do not break a call
            return;
        }

        // if  contact is from the black list...
        contact = findContactByType(contacts, Contact.TYPE_BLACK_LIST);
        if(contact != null) {
            // break call and notify
            breakCallAndNotify(context, contact.name, number);
            return;
        }

        boolean abort = false;

        // if number is from the contacts
        if(Settings.getBooleanValue(context, Settings.BLOCK_CALLS_NOT_FROM_CONTACTS)) {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
            if(db.containsNumberInContacts(number)) {
                return;
            }
            abort = true;
        }

        // if number is from the SMS inbox
        if(Settings.getBooleanValue(context, Settings.BLOCK_CALLS_NOT_FROM_SMS_INBOX)) {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
            if(db.containsNumberInSMSInbox(number)) {
                return;
            }
            abort = true;
        }

        if(abort) {
            // break call and notify
            breakCallAndNotify(context, number, number);
        }
    }

    // Ends phone call
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void breakCall(Context context) {
        TelephonyManager telephony = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class c = Class.forName(telephony.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            ITelephony telephonyService = (ITelephony) m.invoke(telephony);
            telephonyService.endCall();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Contact findContactByType(List<Contact> contacts, int contactType) {
        for(Contact contact : contacts) {
            if(contact.type == contactType) {
                return contact;
            }
        }

        return null;
    }

    private String getContactName(List<Contact> contacts, String number) {
        String name;
        if(contacts.size() > 0) {
            Contact contact = contacts.get(0);
            name = contact.name;
        } else {
            name = number;
        }
        return name;
    }

    // Notifies the user
    private void notifyUser(Context context, String name) {
        // is show notifications
        if(Settings.getBooleanValue(context, Settings.SHOW_CALLS_NOTIFICATIONS)) {
            Notification.onCallNotification(context, name);
        }
    }

    // Writes record to the journal
    private void writeToJournal(Context context, String name, String number) {
        if(number.equals(name)) {
            number = null;
        }
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(context);
        db.addJournalRecord(System.currentTimeMillis(), name, number, null);
    }

    // Checks whether number is private
    private boolean isPrivateNumber(String number) {
        try {
            // private number detected
            if(number == null ||
                    Long.valueOf(number) < 0) {
                return true;
            }
        } catch (NumberFormatException ignored) {
        }
        return false;
    }

    private List<Contact> getContacts(Context context, String number) {
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(context);
        return db.getContacts(number);
    }

    private void breakCallAndNotify(Context context, String name, String number) {
        if(name == null || number == null) {
            return;
        }
        // end phone call
        breakCall(context);
        // write record to the journal
        writeToJournal(context, name, number);
        // notify the user
        notifyUser(context, name);
    }
}
