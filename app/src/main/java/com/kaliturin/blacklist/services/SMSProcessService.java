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

package com.kaliturin.blacklist.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.kaliturin.blacklist.receivers.InternalEventBroadcast;
import com.kaliturin.blacklist.utils.ContactsAccessHelper;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper;
import com.kaliturin.blacklist.utils.DefaultSMSAppHelper;
import com.kaliturin.blacklist.utils.Notifications;

import java.util.HashMap;
import java.util.Map;

/**
 * Service processes received SMS data
 */
public class SMSProcessService extends IntentService {
    private static final String TAG = SMSProcessService.class.getName();
    private static final String KEYS = "KEYS";
    private static final String VALUES = "VALUES";
    private static final String PRIVATE_NUMBER = "-2";

    public SMSProcessService() {
        super(SMSProcessService.class.getName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try {
            Map<String, String> data = extractMessageData(intent);
            processMessageData(this, data);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, e);
        }
    }

    private void processMessageData(Context context, Map<String, String> data) {
        String number = data.get(ContactsAccessHelper.ADDRESS);

        boolean isPrivate = ContactsAccessHelper.isPrivatePhoneNumber(number);
        if (isPrivate) {
            // TODO: consider to use real number
            number = PRIVATE_NUMBER;
            data.put(ContactsAccessHelper.ADDRESS, number);
        }

        // if before API 19
        if (!DefaultSMSAppHelper.isAvailable() ||
                // or if not "default SMS app"
                !DefaultSMSAppHelper.isDefault(context)) {
            // SMS will be written by default app
            try {
                // wait for writing to complete
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            // FIXME: showing private numbers isn't valid
            // inform internal receivers
            InternalEventBroadcast.sendSMSWasWritten(context, number);
            return;
        }

        ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
        DatabaseAccessHelper.Contact contact = null;
        if (!isPrivate) {
            // get contact by number
            contact = db.getContact(context, number);
        }
        // write message to the inbox
        if (db.writeSMSMessageToInbox(context, contact, data)) {
            // send broadcast event
            InternalEventBroadcast.sendSMSWasWritten(context, number);
            // get name for notification
            String name = data.get(ContactsAccessHelper.NAME);
            if (name == null) {
                name = (contact == null ? number : contact.name);
            }
            // notify user
            String body = data.get(ContactsAccessHelper.BODY);
            Notifications.onSmsReceived(context, name, body);
        }
    }

    // Extracts message's data from intent.
    // Throws exception on data intent is illegal.
    private Map<String, String> extractMessageData(@Nullable Intent intent) throws IllegalArgumentException {
        Map<String, String> data = new HashMap<>();
        if (intent != null) {
            String[] keys = intent.getStringArrayExtra(KEYS);
            String[] values = intent.getStringArrayExtra(VALUES);
            if (keys != null && values != null && keys.length == values.length) {
                for (int i = 0; i < keys.length; i++) {
                    data.put(keys[i], values[i]);
                }
            }
        }

        if (data.isEmpty()) {
            String intentString = (intent == null ? "null" : intent.toString());
            throw new IllegalArgumentException("Message intent data is illegal: " + intentString);
        }

        return data;
    }

    // Starts the service
    public static void start(Context context, Map<String, String> data) {
        Intent intent = new Intent(context, SMSProcessService.class);
        intent.putExtra(KEYS, data.keySet().toArray(new String[data.size()]));
        intent.putExtra(VALUES, data.values().toArray(new String[data.size()]));
        context.startService(intent);
    }
}
