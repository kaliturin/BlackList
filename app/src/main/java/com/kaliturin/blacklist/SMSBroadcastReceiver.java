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

package com.kaliturin.blacklist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.telephony.SmsMessage;
import android.util.Log;

import com.kaliturin.blacklist.utils.ContactsAccessHelper;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.Contact;
import com.kaliturin.blacklist.utils.DefaultSMSAppHelper;
import com.kaliturin.blacklist.utils.Notifications;
import com.kaliturin.blacklist.utils.Permissions;
import com.kaliturin.blacklist.utils.Settings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BroadcastReceiver for SMS catching
 */

public class SMSBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = SMSBroadcastReceiver.class.getName();
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final String SMS_DELIVER = "android.provider.Telephony.SMS_DELIVER";

    @Override
    public void onReceive(Context context, Intent intent) {
        long timeReceive = System.currentTimeMillis();

        // check action
        String action = intent.getAction();
        if (action == null ||
                !action.equals(SMS_DELIVER) &&
                        !action.equals(SMS_RECEIVED)) {
            return;
        }

        // is current app the "default SMS app"
        boolean isDefaultSmsApp = DefaultSMSAppHelper.isDefault(context);

        // if "default SMS app" feature is available and app is default
        if (DefaultSMSAppHelper.isAvailable() &&
                isDefaultSmsApp && action.equals(SMS_RECEIVED)) {
            // ignore SMS_RECEIVED action - wait only for SMS_DELIVER
            return;
        }

        // get message data
        Map<String, String> data = extractMessageData(context, intent, timeReceive);
        if (data == null) {
            return;
        }

        // if isn't "default SMS app" or if message isn't blocked
        if (!isDefaultSmsApp || !processMessageData(context, data)) {
            // process message in service
            SMSProcessService.run(context, data);
        }
    }

    // Extracts message data
    @Nullable
    private Map<String, String> extractMessageData(Context context,
                                                   Intent intent, long timeReceive) {
        // get messages
        SmsMessage[] messages = getSMSMessages(intent);
        if (messages == null || messages.length == 0) {
            Log.w(TAG, "Received message is null or empty");
            return null;
        }

        // Assume that all messages in array received at ones have the same data except of bodies.
        // So get just the first message to get the rest data.
        SmsMessage message = messages[0];
        String address = message.getOriginatingAddress();
        if (address == null) {
            Log.w(TAG, "Received message address is null");
            return null;
        }
        address = address.trim();
        if (address.isEmpty()) {
            Log.w(TAG, "Received message address is empty");
            return null;
        }

        Map<String, String> data = new HashMap<>();
        // save concatenated bodies of messages
        data.put(ContactsAccessHelper.BODY, getSMSMessageBody(context, messages));
        data.put(ContactsAccessHelper.ADDRESS, address);
        data.put(ContactsAccessHelper.DATE, String.valueOf(timeReceive));
        data.put(ContactsAccessHelper.DATE_SENT, String.valueOf(message.getTimestampMillis()));
        data.put(ContactsAccessHelper.PROTOCOL, String.valueOf(message.getProtocolIdentifier()));
        data.put(ContactsAccessHelper.REPLY_PATH_PRESENT, String.valueOf(message.isReplyPathPresent()));
        data.put(ContactsAccessHelper.SERVICE_CENTER, message.getServiceCenterAddress());
        String subject = message.getPseudoSubject();
        subject = (subject != null && !subject.isEmpty() ? subject : null);
        data.put(ContactsAccessHelper.SUBJECT, subject);

        return data;
    }

    // Processes message; returns true if message was blocked, false else
    private boolean processMessageData(Context context, Map<String, String> data) {
        String address = data.get(ContactsAccessHelper.ADDRESS);
        String body = data.get(ContactsAccessHelper.BODY);

        // private number detected
        if (isPrivateNumber(address)) {
            // if block private numbers
            if (Settings.getBooleanValue(context, Settings.BLOCK_HIDDEN_SMS)) {
                String name = context.getString(R.string.Private);
                // abort broadcast and notify user
                abortSMSAndNotify(context, name, name, body);
                return true;
            }
            return false;
        }

        // get contacts linked to the number
        List<Contact> contacts = getContacts(context, address);

        // if contact is from the white list
        Contact contact = findContactByType(contacts, Contact.TYPE_WHITE_LIST);
        if (contact != null) {
            return false;
        }

        // if block all SMS (excluding the white list)
        if (Settings.getBooleanValue(context, Settings.BLOCK_ALL_SMS)) {
            String name = getContactName(contacts, address);
            // abort SMS and notify the user
            abortSMSAndNotify(context, name, address, body);
            return true;
        }

        // if contact is from the black list
        if (Settings.getBooleanValue(context, Settings.BLOCK_SMS_FROM_BLACK_LIST)) {
            contact = findContactByType(contacts, Contact.TYPE_BLACK_LIST);
            if (contact != null) {
                // abort SMS and notify the user
                abortSMSAndNotify(context, contact.name, address, body);
                return true;
            }
        }

        boolean abort = false;

        // if number is from the contacts
        if (Settings.getBooleanValue(context, Settings.BLOCK_SMS_NOT_FROM_CONTACTS) &&
                Permissions.isGranted(context, Permissions.READ_CONTACTS)) {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
            if (db.getContact(context, address) != null) {
                return false;
            }
            abort = true;
        }

        // if number is from the SMS content
        if (Settings.getBooleanValue(context, Settings.BLOCK_SMS_NOT_FROM_SMS_CONTENT) &&
                Permissions.isGranted(context, Permissions.READ_SMS)) {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
            if (db.containsNumberInSMSContent(context, address)) {
                return false;
            }
            abort = true;
        }

        if (abort) {
            // abort SMS and notify the user
            abortSMSAndNotify(context, address, address, body);
        }

        return abort;
    }

    private Contact findContactByType(List<Contact> contacts, int contactType) {
        for (Contact contact : contacts) {
            if (contact.type == contactType) {
                return contact;
            }
        }

        return null;
    }

    // Extracts received SMS message from intent
    @SuppressWarnings("deprecation")
    private SmsMessage[] getSMSMessages(Intent intent) {
        SmsMessage[] messages = null;
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus != null) {
                messages = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    } else {
                        String format = bundle.getString("format");
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                    }
                }
            }
        }

        return messages;
    }

    private String getSMSMessageBody(Context context, SmsMessage[] messages) {
        StringBuilder smsBody = new StringBuilder();
        for (SmsMessage message : messages) {
            String text = message.getMessageBody();
            if (text != null) {
                smsBody.append(text);
            }
        }
        String body = smsBody.toString();
        if (body.isEmpty()) {
            body = context.getString(R.string.No_text);
        }
        return body;
    }

    private String getContactName(List<Contact> contacts, String number) {
        String name;
        if (contacts.size() > 0) {
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
            if (number == null ||
                    Long.valueOf(number) < 0) {
                return true;
            }
        } catch (NumberFormatException ignored) {
        }
        return false;
    }

    @Nullable
    private List<Contact> getContacts(Context context, String number) {
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(context);
        return (db == null ? null : db.getContacts(number, false));
    }

    // Writes record to the journal
    private void writeToJournal(Context context, String name, String number, String body) {
        if (number.equals(name)) {
            number = null;
        }
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(context);
        if (db != null) {
            // write to the journal
            if (db.addJournalRecord(System.currentTimeMillis(), name, number, body) >= 0) {
                // send broadcast message
                InternalEventBroadcast.send(context, InternalEventBroadcast.JOURNAL_WAS_WRITTEN);
            }
        }
    }

    // Aborts broadcast (if available) and notifies user
    private void abortSMSAndNotify(Context context, String name, String number, String body) {
        if (name == null || number == null) {
            return;
        }
        // prevent to place this SMS to incoming box
        abortBroadcast();
        // write record to the journal
        writeToJournal(context, name, number, body);
        // notify user
        Notifications.onSmsBlocked(context, name);
    }
}
