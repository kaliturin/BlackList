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

package com.kaliturin.blacklist.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.kaliturin.blacklist.R;
import com.kaliturin.blacklist.services.BlockEventProcessService;
import com.kaliturin.blacklist.utils.Constants;
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

        // From https://developer.android.com/reference/android/telephony/TelephonyManager:
        // If the receiving app has Manifest.permission.READ_CALL_LOG and
        // Manifest.permission.READ_PHONE_STATE permission, it will receive the broadcast twice;
        // one with the EXTRA_INCOMING_NUMBER populated with the phone number, and another
        // with it blank. Due to the nature of broadcasts, you cannot assume the order in which
        // these broadcasts will arrive, however you are guaranteed to receive two in this case.
        // Apps which are interested in the EXTRA_INCOMING_NUMBER can ignore the broadcasts where
        // EXTRA_INCOMING_NUMBER is not present in the extras (e.g. where Intent#hasExtra(String)
        // returns false).
        if (!intent.hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)) {
            Log.d(TAG, "Event had no incoming_number metadata. Letting it keep ringing...");
            return;
        }

        // get incoming call number.
        String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
        Log.d(TAG, "Incoming number: " + number);

        // private number detected
        if (ContactsAccessHelper.isPrivatePhoneNumber(number)) {
            // if block private numbers
            if (Settings.getBooleanValue(context, Settings.BLOCK_PRIVATE_CALLS) ||
                    // or if block all calls
                    Settings.getBooleanValue(context, Settings.BLOCK_ALL_CALLS)) {
                String name = context.getString(R.string.Private_number);
                // break call and notify user
                breakCallAndNotify(context, number, name);
            }
            return;
        }

        // normalize number
        number = ContactsAccessHelper.normalizePhoneNumber(number);
        if (number.isEmpty()) {
            Log.w(TAG, "Received call address is empty");
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

        // get name of contact
        String name = (contacts.size() > 0 ? contacts.get(0).name : null);

        // if block all calls (excluding the white list)
        if (Settings.getBooleanValue(context, Settings.BLOCK_ALL_CALLS)) {
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
            // there is no contact - get number as name
            name = number;
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
            // break call and notify user
            breakCallAndNotify(context, number, name);
        }
    }

    private void breakCallNougatAndLower(Context context) {
        Log.d(TAG, "Trying to break call for Nougat and lower with TelephonyManager.");
        TelephonyManager telephony = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class c = Class.forName(telephony.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            ITelephony telephonyService = (ITelephony) m.invoke(telephony);
            telephonyService.endCall();
            Log.d(TAG, "Invoked 'endCall' on TelephonyService.");
        } catch (Exception e) {
            Log.e(TAG, "Could not end call. Check stdout for more info.");
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Constants.PIE_API_VERSION)
    private void breakCallPieAndHigher(Context context) {
        Log.d(TAG, "Trying to break call for Pie and higher with TelecomManager.");
        TelecomManager telecomManager = (TelecomManager)
                context.getSystemService(Context.TELECOM_SERVICE);
        try {
            telecomManager.getClass().getMethod("endCall").invoke(telecomManager);
            Log.d(TAG, "Invoked 'endCall' on TelecomManager.");
        } catch (Exception e) {
            Log.e(TAG, "Could not end call. Check stdout for more info.");
            e.printStackTrace();
        }
    }

    // Ends phone call
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void breakCall(Context context) {
        if (!Permissions.isGranted(context, Permissions.CALL_PHONE)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Constants.PIE_API_VERSION) {
            breakCallPieAndHigher(context);
        } else {
            breakCallNougatAndLower(context);
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

    // Finds contacts by number
    @Nullable
    private List<Contact> getContacts(Context context, String number) {
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(context);
        return (db == null ? null : db.getContacts(number, false));
    }

    // Breaks the call and notifies the user
    private void breakCallAndNotify(Context context, String number, String name) {
        // end phone call
        breakCall(context);
        // process the event of blocking in the service
        BlockEventProcessService.start(context, number, name, null);
    }
}
