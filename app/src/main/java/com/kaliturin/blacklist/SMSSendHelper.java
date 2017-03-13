package com.kaliturin.blacklist;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.telephony.SmsManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Sends SMS and processes the results of sending
 */

class SMSSendHelper {
    private static final String TIME_SENT = "TIME_SENT";
    private static final String PHONE_NUMBER = "PHONE_NUMBER";
    private static final String MESSAGE = "MESSAGE";
    private static final String MESSAGE_PART_ID = "MESSAGE_PART_ID";
    private static Set<BroadcastReceiver> receivers = new HashSet<>();
    private static SMSSendHelper sInstance = null;

    private SMSSendHelper() {}

    public static synchronized SMSSendHelper getInstance() {
        if(sInstance == null) {
            sInstance = new SMSSendHelper();
        }
        return sInstance;
    }

    // TODO call it in MainActivity.onDestroyActivity
    // Cleans unregistered receivers
    synchronized void clean(Context context) {
        context = context.getApplicationContext();
        for(BroadcastReceiver receiver : receivers) {
            context.unregisterReceiver(receiver);
        }
        receivers.clear();
    }

    // Sending SMS action results receiver
    private abstract class ResultReceiver extends BroadcastReceiver {
        PendingIntent register(Context context, String intentName, long timeSent, String phoneNumber,
                               String message, int messagePartId, int messagePartsCount) {
            // register receiver
            receivers.add(this);
            context.registerReceiver(this, new IntentFilter(intentName));
            // create pending intent
            Intent intent = new Intent(intentName);
            intent.putExtra(TIME_SENT, timeSent);
            intent.putExtra(PHONE_NUMBER, phoneNumber);
            intent.putExtra(MESSAGE, message);
            if(messagePartsCount > 1) {
                // add part id only for multipart messages
                intent.putExtra(MESSAGE_PART_ID, messagePartId);
            }
            return PendingIntent.getBroadcast(context, 0, intent, 0);
        }
    }

    // Sends SMS
    synchronized boolean sendSMS(Context context, @NonNull String phoneNumber, @NonNull String smsMessage) {
        if(phoneNumber.isEmpty() || smsMessage.isEmpty()) {
            return false;
        }

        // get application context
        context = context.getApplicationContext();

        // divide message into parts
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> parts = smsManager.divideMessage(smsMessage);
        ArrayList<PendingIntent> sentIntents = new ArrayList<>(parts.size());
        ArrayList<PendingIntent> deliveryIntents = new ArrayList<>(parts.size());

        // create intents and result receivers for each part of message
        long timeSent = System.currentTimeMillis();
        for(int i=0; i<parts.size(); i++) {
            String part = parts.get(i);

            // create on sent SMS receiver
            ResultReceiver receiver = new ResultReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    onSMSSent(context, intent, getResultCode());
                    context.unregisterReceiver(this);
                    receivers.remove(this);
                }
            };
            // create unique intent name and register receiver
            String intentName = "SMS_SENT" + "_" + timeSent + "_" + i;
            PendingIntent pendingIntent = receiver.register(context,
                    intentName, timeSent, phoneNumber, part, i+1, parts.size());
            sentIntents.add(pendingIntent);

            // create on delivery SMS receiver
            receiver = new ResultReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    onSMSDelivery(context, intent, getResultCode());
                    context.unregisterReceiver(this);
                    receivers.remove(this);
                }
            };
            // create unique intent name and register receiver
            intentName = "SMS_DELIVERED" + "_" + timeSent + "_" + i;
            pendingIntent = receiver.register(context,
                    intentName, timeSent, phoneNumber, part, i+1, parts.size());
            deliveryIntents.add(pendingIntent);
        }

        // send multipart message
        smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, deliveryIntents);

        return true;
    }

    // Is calling on SMS sending results received
    void onSMSSent(Context context, Intent intent, int result) {
        int stringId = 0;
        switch (result) {
            case Activity.RESULT_OK:
                // write the sent SMS to the Outbox
                writeSMSToOutbox(context, intent);
                stringId = R.string.SMS_is_sent;
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                stringId = R.string.Generic_failure;
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                stringId = R.string.No_service;
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                stringId = R.string.Null_PDU;
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                stringId = R.string.Radio_off;
                break;
        }
        showNotification(context, intent, stringId);
    }

    // Is calling on SMS delivery results received
    void onSMSDelivery(Context context, Intent intent, int result) {
        // TODO test whether result code is enough
        int stringId = 0;
        switch (result) {
            case Activity.RESULT_OK:
                stringId = R.string.SMS_is_delivered;
                break;
            case Activity.RESULT_CANCELED:
                stringId = R.string.SMS_is_not_delivered;
                break;
        }
        showNotification(context, intent, stringId);
    }

    // Shows notification on SMS sent/delivery
    void showNotification(Context context, Intent intent, @StringRes int stringId) {
        if(stringId == 0) {
            return;
        }
        // create message with SMS part id if it is defined
        String text = context.getString(stringId);
        String phoneNumber = intent.getStringExtra(PHONE_NUMBER);
        if(phoneNumber != null) {
            text = phoneNumber + " : " + text;
        }
        int smsPartId = intent.getIntExtra(MESSAGE_PART_ID, 0);
        if(smsPartId > 0) {
            text += " [" + smsPartId + "]";
        }
        // TODO consider to user NotificationManager
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    // Writes SMS sent to the Outbox
    private synchronized void writeSMSToOutbox(Context context, Intent intent) {
        // TODO test writing multipart messages
        // get the sent SMS data
        long timeSent = intent.getLongExtra(TIME_SENT, 0);
        String phoneNumber = intent.getStringExtra(PHONE_NUMBER);
        String message = intent.getStringExtra(MESSAGE);
        if(timeSent != 0 && phoneNumber != null && message != null) {
            boolean written = false;
            // is app isn't default - the SMS will be written by the system
            if(!DefaultSMSAppHelper.isDefault(context)) {
                written = true;
            } else {
                // write the sent SMS to the Outbox
                ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
                if (db.writeSMSMessageToOutbox(context, timeSent, phoneNumber, message)) {
                    written = true;
                }
            }
            if(written) {
                // send internal event message
                InternalEventBroadcast.sendSMSWasWritten(context, phoneNumber);
            }
        }
    }
}
