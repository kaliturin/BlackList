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
import android.content.IntentFilter;
import android.support.annotation.NonNull;

/**
 * Broadcast receiver/sender is used for notifications about internal events
 */
public class InternalEventBroadcast extends BroadcastReceiver {
    public static final String TAG = InternalEventBroadcast.class.getName();
    public static final String JOURNAL_WAS_WRITTEN = "JOURNAL_WAS_WRITTEN";
    public static final String SMS_WAS_WRITTEN = "SMS_WAS_WRITTEN";
    public static final String SMS_WAS_DELETED = "SMS_WAS_DELETED";
    public static final String SMS_WAS_READ = "SMS_WAS_READ";

    private static final String EVENT_TYPE = "EVENT_TYPE";
    private static final String CONTACT_NUMBER = "CONTACT_NUMBER";
    private static final String THREAD_ID = "THREAD_ID";

    @Override
    public void onReceive(Context context, Intent intent) {
        // get action type
        String actionType = intent.getStringExtra(EVENT_TYPE);
        if (actionType == null) {
            return;
        }
        // invoke the callback correspondent to the received event
        switch (actionType) {
            case JOURNAL_WAS_WRITTEN:
                onJournalWasWritten();
                break;
            case SMS_WAS_WRITTEN: {
                String number = intent.getStringExtra(CONTACT_NUMBER);
                if (number != null) {
                    onSMSWasWritten(number);
                }
            }
            break;
            case SMS_WAS_DELETED: {
                String number = intent.getStringExtra(CONTACT_NUMBER);
                if (number != null) {
                    onSMSWasDeleted(number);
                }
            }
            break;
            case SMS_WAS_READ:
                int threadId = intent.getIntExtra(THREAD_ID, 0);
                onSMSThreadWasRead(threadId);
                break;
        }
    }

    public void register(Context context) {
        IntentFilter filter = new IntentFilter(TAG);
        context.registerReceiver(this, filter);
    }

    public void unregister(Context context) {
        context.unregisterReceiver(this);
    }

    /**
     * Method is called if some record was written to the Journal (Event log)
     **/
    public void onJournalWasWritten() {
    }

    /**
     * Method is called if SMS was written to the content provider
     **/
    public void onSMSWasWritten(String phoneNumber) {
    }

    /**
     * Method is called if SMS was deleted from the content provider
     **/
    public void onSMSWasDeleted(String phoneNumber) {
    }

    /**
     * Method is called if SMS with thread id was read from the content provider
     **/
    public void onSMSThreadWasRead(int threadId) {
    }

    /**
     * Sends internal event which will be received by corresponding
     * callback of registered receiver
     **/
    public static void send(Context context, String eventType) {
        Intent intent = new Intent(TAG);
        intent.putExtra(EVENT_TYPE, eventType);
        context.sendBroadcast(intent, null);
    }

    /**
     * Sends internal event of writing SMS to the content provider, which causes
     * onSMSWasWritten invocation of registered receivers.
     **/
    public static void sendSMSWasWritten(Context context, @NonNull String phoneNumber) {
        Intent intent = new Intent(TAG);
        intent.putExtra(EVENT_TYPE, SMS_WAS_WRITTEN);
        intent.putExtra(CONTACT_NUMBER, phoneNumber);
        context.sendBroadcast(intent, null);
    }

    /**
     * Sends internal event of deleting the SMS from content provider, which causes
     * onSMSWasDeleted invocation of registered receivers.
     **/
    public static void sendSMSWasDeleted(Context context, @NonNull String phoneNumber) {
        Intent intent = new Intent(TAG);
        intent.putExtra(EVENT_TYPE, SMS_WAS_DELETED);
        intent.putExtra(CONTACT_NUMBER, phoneNumber);
        context.sendBroadcast(intent, null);
    }

    /**
     * Sends internal event of reading the thread from the SMS Inbox, which causes
     * onSMSThreadWasRead invocation of registered receivers.
     **/
    public static void sendSMSThreadWasRead(Context context, int threadId) {
        Intent intent = new Intent(TAG);
        intent.putExtra(EVENT_TYPE, SMS_WAS_READ);
        intent.putExtra(THREAD_ID, threadId);
        context.sendBroadcast(intent, null);
    }
}
