package com.kaliturin.blacklist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;

import java.util.List;

import com.kaliturin.blacklist.DatabaseAccessHelper.Contact;

/**
 * BroadcastReceiver for SMS catching
 */

public class SMSBroadcastReceiver extends BroadcastReceiver {
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        // if block sms not enabled
        if(!Settings.getBooleanValue(context, Settings.BLOCK_SMS)) {
            return;
        }

        // check action
        String action = intent.getAction();
        if(action == null || !action.equals(SMS_RECEIVED)) {
            return;
        }

        // get messages
        SmsMessage[] messages = getSMSMessages(intent);
        if(messages == null || messages.length == 0) {
            return;
        }

        // TODO: check whether can be different addresses in messages
        // get address number
        String number = messages[0].getDisplayOriginatingAddress();

        // private number detected
        if(isPrivateNumber(number)) {
            // if block private numbers
            if(Settings.getBooleanValue(context, Settings.BLOCK_HIDDEN_SMS)) {
                String name = context.getString(R.string.hidden);
                // abort broadcast and notify user
                abortSMSAndNotify(context, name, name, messages);
            }
            return;
        }

        // get contacts linked to the number
        List<Contact> contacts = getContacts(context, number);

        // if block all SMS
        if(Settings.getBooleanValue(context, Settings.BLOCK_ALL_SMS)) {
            String name = getContactName(contacts, number);
            // abort SMS and notify the user
            abortSMSAndNotify(context, name, number, messages);
            return;
        }

        // if contact is from the white list
        Contact contact = findContactByType(contacts, Contact.TYPE_WHITE_LIST);
        if(contact != null) {
            return;
        }

        // if contact is from the black list
        contact = findContactByType(contacts, Contact.TYPE_BLACK_LIST);
        if(contact != null) {
            // abort SMS and notify the user
            abortSMSAndNotify(context, contact.name, number, messages);
            return;
        }

        boolean abort = false;

        // if number is from the contacts
        if(Settings.getBooleanValue(context, Settings.BLOCK_SMS_NOT_FROM_CONTACTS)) {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
            if(db.containsNumberInContacts(number)) {
                return;
            }
            abort = true;
        }

        // if number is from the SMS inbox
        if(Settings.getBooleanValue(context, Settings.BLOCK_SMS_NOT_FROM_INBOX)) {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
            if(db.containsNumberInSMSInbox(number)) {
                return;
            }
            abort = true;
        }

        if(abort) {
            // abort SMS and notify the user
            abortSMSAndNotify(context, number, number, messages);
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

    // Extracts received SMS message from intent
    private SmsMessage[] getSMSMessages(Intent intent) {
        SmsMessage[] messages = null;
        Bundle bundle = intent.getExtras();
        if(bundle != null) {
            Object[] pdus = (Object[]) intent.getExtras().get("pdus");
            if (pdus != null) {
                messages = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        String format = bundle.getString("format");
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                    } else {
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    }
                }
            }
        }

        return messages;
    }

    private String getSMSMessageBody(Context context, SmsMessage[] messages) {
        StringBuilder smsBody = new StringBuilder();
        for (SmsMessage message : messages) {
            smsBody.append(message.getMessageBody());
        }
        String body = smsBody.toString();
        if(body.isEmpty()) {
            body = context.getString(R.string.empty_sms);
        }
        return body;
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

    private void notifyUser(Context context, String name) {
        // is show notifications
        if(Settings.getBooleanValue(context, Settings.SHOW_SMS_NOTIFICATIONS)) {
            Notification.onSmsNotification(context, name);
        }
    }

    // Writes record to the journal
    private void writeToJournal(Context context, String name, String number, SmsMessage[] messages) {
        if(number.equals(name)) {
            number = null;
        }
        String text = getSMSMessageBody(context, messages);
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(context);
        db.addJournalRecord(System.currentTimeMillis(), name, number, text);
    }

    private void abortSMSAndNotify(Context context, String name, String number, SmsMessage[] messages) {
        if(name == null || number == null) {
            return;
        }
        // prevent to place this SMS to incoming box
        abortBroadcast();
        // write record to the journal
        writeToJournal(context, name, number, messages);
        // notify the user
        notifyUser(context, name);
    }

}