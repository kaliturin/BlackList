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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.kaliturin.blacklist.utils.ContactsAccessHelper;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.Contact;
import com.kaliturin.blacklist.utils.Permissions;
import com.kaliturin.blacklist.utils.Settings;

import java.lang.reflect.Method;
import java.util.List;

/**
 * BroadcastReceiver for calls catching
 */
public class CallBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = CallBroadcastReceiver.class.getName();

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
            Log.w(TAG, "Received call address is null");
            return;
        }
        number = number.trim();
        if (number.isEmpty()) {
            Log.w(TAG, "Received call address is empty");
            return;
        }

        // private number detected
        if (isPrivateNumber(number)) {
            // if block private numbers
            if (Settings.getBooleanValue(context, Settings.BLOCK_PRIVATE_CALLS)) {
                String name = context.getString(R.string.Private);
                // break call and notify user
                breakCallAndNotify(context, name, name);
            }
            return;
        }

        // get contacts linked to the current number
        List<Contact> contacts = getContacts(context, number);
        if (contacts == null) {
            return;
        }

        // if contact is from the white list
        Contact contact = findContactByType(contacts, Contact.TYPE_WHITE_LIST);
        if (contact != null) {
            return;
        }

        // if block all calls (excluding the white list)
        if (Settings.getBooleanValue(context, Settings.BLOCK_ALL_CALLS)) {
            // get name of contact
            String name = (contacts.size() > 0 ? contacts.get(0).name : null);
            // break call and notify user
            breakCallAndNotify(context, number, name);
            return;
        }

        // if block calls from the black list
        if (Settings.getBooleanValue(context, Settings.BLOCK_CALLS_FROM_BLACK_LIST)) {
            contact = findContactByType(contacts, Contact.TYPE_BLACK_LIST);
            if (contact != null) {
                // break call and notify user
                breakCallAndNotify(context, number, contact.name);
                return;
            }
        }

        boolean abort = false;

        // if block numbers that are not in the contact list
        if (Settings.getBooleanValue(context, Settings.BLOCK_CALLS_NOT_FROM_CONTACTS) &&
                Permissions.isGranted(context, Permissions.READ_CONTACTS)) {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
            if (db.getContact(context, number) != null) {
                return;
            }
            abort = true;
        }

        // if block numbers that are not in the sms content list
        if (Settings.getBooleanValue(context, Settings.BLOCK_CALLS_NOT_FROM_SMS_CONTENT) &&
                Permissions.isGranted(context, Permissions.READ_SMS)) {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
            if (db.containsNumberInSMSContent(context, number)) {
                return;
            }
            abort = true;
        }

        if (abort) {
            // get name of contact
            String name = (contacts.size() > 0 ? contacts.get(0).name : null);
            // break call and notify user
            breakCallAndNotify(context, number, name);
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

    // Finds contact by type
    private Contact findContactByType(List<Contact> contacts, int contactType) {
        for (Contact contact : contacts) {
            if (contact.type == contactType) {
                return contact;
            }
        }
        return null;
    }

    // Checks whether number is private
    private boolean isPrivateNumber(String number) {
        try {
            // private number detected
            if (number == null || Long.valueOf(number) < 0) {
                return true;
            }
        } catch (NumberFormatException ignored) {
        }
        return false;
    }

    // Finds contacts by number
    @Nullable
    private List<Contact> getContacts(Context context, String number) {
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(context);
        return (db == null ? null : db.getContacts(number, false));
    }

    // Breaks the call and notifies the user
    private void breakCallAndNotify(Context context, @NonNull String number, String name) {
        // end phone call
        breakCall(context);
        // process the event of blocking in the service
        BlockEventProcessService.start(context, number, name, null);
    }
}
