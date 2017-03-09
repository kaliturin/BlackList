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
    public static final String JOURNAL_WRITE = "JOURNAL_WRITE";
    public static final String SMS_INBOX_WRITE = "SMS_INBOX_WRITE";
    public static final String SMS_INBOX_READ = "SMS_INBOX_READ";

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
            case JOURNAL_WRITE:
                onJournalWrite();
                break;
            case SMS_INBOX_WRITE:
                String number = intent.getStringExtra(CONTACT_NUMBER);
                if(number != null) {
                    onSMSInboxWrite(number);
                }
                break;
            case SMS_INBOX_READ:
                int threadId = intent.getIntExtra(THREAD_ID, 0);
                onSMSInboxRead(threadId);
                break;
        }
    }

    public void register(Context context) {
        IntentFilter filter = new IntentFilter(InternalEventBroadcast.class.getName());
        context.registerReceiver(this, filter);
    }

    public void unregister(Context context) {
        context.unregisterReceiver(this);
    }

    /** Method is called if SMS has been written to the Inbox **/
    public void onSMSInboxWrite(@NonNull String number) {}

    /** Method is called if SMS with thread id have been read from the Inbox **/
    public void onSMSInboxRead(int threadId) {}

    /** Method is called if some record has been written to the Journal **/
    public void onJournalWrite() {}

    /** Sends internal event which will be received by corresponding
     * callback of registered receiver **/
    public static void send(Context context, String eventType) {
        Intent intent = new Intent(InternalEventBroadcast.class.getName());
        intent.putExtra(EVENT_TYPE, eventType);
        context.sendBroadcast(intent, null);
    }

    /** Sends internal event of writing to the SMS Inbox, which causes
     *  onSMSInboxWrite invocation of registered receivers. **/
    public static void sendSMSInboxWrite(Context context, @NonNull String number) {
        Intent intent = new Intent(InternalEventBroadcast.class.getName());
        intent.putExtra(EVENT_TYPE, SMS_INBOX_WRITE);
        intent.putExtra(CONTACT_NUMBER, number);
        context.sendBroadcast(intent, null);
    }

    /** Sends internal event of reading the thread from the SMS Inbox, which causes
     *  onSMSInboxRead invocation of registered receivers. **/
    public static void sendSMSInboxRead(Context context, int threadId) {
        Intent intent = new Intent(InternalEventBroadcast.class.getName());
        intent.putExtra(EVENT_TYPE, SMS_INBOX_READ);
        intent.putExtra(THREAD_ID, threadId);
        context.sendBroadcast(intent, null);
    }
}
