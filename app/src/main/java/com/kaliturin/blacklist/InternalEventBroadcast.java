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
     * Method is called if SMS has been written to the Inbox/Outbox/Sent box
     **/
    public void onSMSWasWritten(String phoneNumber) {
    }

    /**
     * Method is called if SMS has been deleted from the Inbox/Outbox/Sent box
     **/
    public void onSMSWasDeleted(String phoneNumber) {
    }

    /**
     * Method is called if SMS with thread id has been read from the Inbox/Outbox/Sent box
     **/
    public void onSMSThreadWasRead(int threadId) {
    }

    /**
     * Method is called if some record has been written to the Journal (Event log)
     **/
    public void onJournalWasWritten() {
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
     * Sends internal event of writing SMS to the Inbox/Outbox/Sent box, which causes
     * onSMSWasWritten invocation of registered receivers.
     **/
    public static void sendSMSWasWritten(Context context, @NonNull String phoneNumber) {
        Intent intent = new Intent(TAG);
        intent.putExtra(EVENT_TYPE, SMS_WAS_WRITTEN);
        intent.putExtra(CONTACT_NUMBER, phoneNumber);
        context.sendBroadcast(intent, null);
    }

    /**
     * Sends internal event of deleting the SMS from Inbox/Outbox/Sent box, which causes
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
