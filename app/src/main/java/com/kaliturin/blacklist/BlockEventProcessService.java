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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kaliturin.blacklist.utils.ContactsAccessHelper;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.Contact;
import com.kaliturin.blacklist.utils.Notifications;

/**
 * SMS/Call blocking events processing service
 */
public class BlockEventProcessService extends IntentService {
    private static final String NUMBER = "NUMBER";
    private static final String NAME = "NAME";
    private static final String BODY = "BODY";

    public BlockEventProcessService() {
        super(BlockEventProcessService.class.getName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            String number = intent.getStringExtra(NUMBER);
            if (number == null) {
                return;
            }
            String name = intent.getStringExtra(NAME);
            String body = intent.getStringExtra(BODY);
            processEvent(this, number, name, body);
        }
    }

    // Processes the event
    private void processEvent(Context context, @NonNull String number, String name, String body) {
        if (name == null) {
            // get name from the contacts
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
            Contact contact = db.getContact(context, number);
            name = (contact != null ? contact.name : number);
        }

        // write to the journal
        writeToJournal(context, number, name, body);

        // notify the user
        if (body == null) {
            Notifications.onCallBlocked(context, name);
        } else {
            Notifications.onSmsBlocked(context, name);
        }
    }

    // Writes record to the journal
    private void writeToJournal(Context context, String number, String name, String body) {
        if (number.equals(name)) {
            number = null;
        }
        long time = System.currentTimeMillis();
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(context);
        if (db != null && db.addJournalRecord(time, name, number, body) >= 0) {
            // send broadcast message
            InternalEventBroadcast.send(context, InternalEventBroadcast.JOURNAL_WAS_WRITTEN);
        }
    }

    // Starts the service
    public static void start(Context context, @NonNull String number, String name, String body) {
        Intent intent = new Intent(context, BlockEventProcessService.class);
        intent.putExtra(NUMBER, number);
        intent.putExtra(NAME, name);
        intent.putExtra(BODY, body);
        context.startService(intent);
    }
}
