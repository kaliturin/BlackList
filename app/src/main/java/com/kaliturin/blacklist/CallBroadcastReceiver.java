/*
 * Copyright 2017 Anton Kaliturin
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
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.ITelephony;
import com.kaliturin.blacklist.utils.ContactsAccessHelper;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.Contact;
import com.kaliturin.blacklist.utils.Notifications;
import com.kaliturin.blacklist.utils.Permissions;
import com.kaliturin.blacklist.utils.Settings;

import java.lang.reflect.Method;
import java.util.List;

/**
 * BroadcastReceiver for calls catching
 */

public class CallBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!Permissions.isGranted(context, Permissions.READ_PHONE_STATE) ||
                !Permissions.isGranted(context, Permissions.CALL_PHONE)) {
            return;
        }

        // get telephony service
        TelephonyManager telephony = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephony.getCallState() != TelephonyManager.CALL_STATE_RINGING) {
            return;
        }

        // get incoming call number
        String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
        if (number == null) {
            return;
        }
        number = number.trim();
        if (number.isEmpty()) {
            return;
        }

        // private number detected
        if (isPrivateNumber(number)) {
            // if block private numbers
            if (Settings.getBooleanValue(context, Settings.BLOCK_HIDDEN_CALLS)) {
                String name = context.getString(R.string.Private);
                // break call and notify
                breakCallAndNotify(context, name, name);
            }
            return;
        }

        // get contacts linked to the current number
        List<Contact> contacts = getContacts(context, number);
        if (contacts == null) {
            return;
        }

        // is contact is int the white list
        Contact contact = findContactByType(contacts, Contact.TYPE_WHITE_LIST);
        if (contact != null) {
            // do not break a call
            return;
        }

        // if block all calls (excluding the white list)
        if (Settings.getBooleanValue(context, Settings.BLOCK_ALL_CALLS)) {
            String name = getContactName(contacts, number);
            // break call and notify
            breakCallAndNotify(context, name, name);
            return;
        }

        // if block calls from the black list
        if (Settings.getBooleanValue(context, Settings.BLOCK_CALLS_FROM_BLACK_LIST)) {
            // if  contact is from the black list...
            contact = findContactByType(contacts, Contact.TYPE_BLACK_LIST);
            if (contact != null) {
                // break call and notify
                breakCallAndNotify(context, contact.name, number);
                return;
            }
        }

        boolean abort = false;

        // if number is from the contacts
        if (Settings.getBooleanValue(context, Settings.BLOCK_CALLS_NOT_FROM_CONTACTS) &&
                Permissions.isGranted(context, Permissions.READ_CONTACTS)) {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
            if (db.getContact(context, number) != null) {
                return;
            }
            abort = true;
        }

        // if number is from the SMS content list
        if (Settings.getBooleanValue(context, Settings.BLOCK_CALLS_NOT_FROM_SMS_CONTENT) &&
                Permissions.isGranted(context, Permissions.READ_SMS)) {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
            if (db.containsNumberInSMSContent(context, number)) {
                return;
            }
            abort = true;
        }

        if (abort) {
            // break call and notify
            breakCallAndNotify(context, number, number);
        }
    }

    // Ends phone call
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void breakCall(Context context) {
        if (!Permissions.isGranted(context, Permissions.CALL_PHONE)) {
            return;
        }

        TelephonyManager telephony = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class c = Class.forName(telephony.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            ITelephony telephonyService = (ITelephony) m.invoke(telephony);
            telephonyService.endCall();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Contact findContactByType(List<Contact> contacts, int contactType) {
        for (Contact contact : contacts) {
            if (contact.type == contactType) {
                return contact;
            }
        }
        return null;
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

    // Writes record to the journal
    private void writeToJournal(Context context, String name, String number) {
        if (number.equals(name)) {
            number = null;
        }
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(context);
        if (db != null) {
            // write to the journal
            if (db.addJournalRecord(System.currentTimeMillis(), name, number, null) >= 0) {
                // send broadcast message
                InternalEventBroadcast.send(context, InternalEventBroadcast.JOURNAL_WAS_WRITTEN);
            }
        }
    }

    // Checks whether number is private
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

    private void breakCallAndNotify(Context context, String name, String number) {
        if (name == null || number == null) {
            return;
        }
        // end phone call
        breakCall(context);
        // write record to the journal
        writeToJournal(context, name, number);
        // notify the user
        Notifications.onCallBlocked(context, name);
    }
}
