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

/**
 * Sends SMS and processes the results of sending
 */

public class SMSSendHelper {
    private static final String SMS_PART = "SMS_PART";
    private static final String SMS_PART_ID = "SMS_PART_ID";

    // Sending SMS action results receiver
    abstract class ResultReceiver extends BroadcastReceiver {
        PendingIntent register(Context context, String intentName, String smsPart,
                               int smsPartId, int smsPartsCount) {
            // register receiver
            context.registerReceiver(this, new IntentFilter(intentName));
            // create pending intent
            Intent intent = new Intent(intentName);
            intent.putExtra(SMS_PART, smsPart);
            if(smsPartsCount > 1) {
                // add part id only for multipart messages
                intent.putExtra(SMS_PART_ID, smsPartId);
            }
            return PendingIntent.getBroadcast(context, 0, intent, 0);
        }
    }

    // Sends SMS
    public boolean sendSMS(final Context context, @NonNull String number, @NonNull String message) {
        if(number.isEmpty() || message.isEmpty()) {
            return false;
        }

        // divide message into parts
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> parts = smsManager.divideMessage(message);
        ArrayList<PendingIntent> sentIntents = new ArrayList<>(parts.size());
        ArrayList<PendingIntent> deliveryIntents = new ArrayList<>(parts.size());

        // create intents and result receivers for each part of message
        long time = System.currentTimeMillis();
        for(int i=0; i<parts.size(); i++) {
            String part = parts.get(i);

            // create on sent receiver
            ResultReceiver receiver = new ResultReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    onSMSSent(context, intent, getResultCode());
                    context.unregisterReceiver(this);
                }
            };
            // create unique intent name and register receiver
            String intentName = "SMS_SENT" + "_" + time + "_" + i;
            PendingIntent pendingIntent = receiver.register(context, intentName, part, i+1, parts.size());
            sentIntents.add(pendingIntent);

            // create on delivery receiver
            receiver = new ResultReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    onSMSDelivery(context, intent, getResultCode());
                    context.unregisterReceiver(this);
                }
            };
            // create unique intent name and register receiver
            intentName = "SMS_DELIVERED" + "_" + time + "_" + i;
            pendingIntent = receiver.register(context, intentName, part, i+1, parts.size());
            deliveryIntents.add(pendingIntent);
        }

        // send multipart message
        smsManager.sendMultipartTextMessage(number, null, parts, sentIntents, deliveryIntents);

        return true;
    }

    // Is calling on SMS sending results received
    public void onSMSSent(Context context, Intent intent, int result) {
        int stringId = 0;
        switch (result) {
            case Activity.RESULT_OK:
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
    public void onSMSDelivery(Context context, Intent intent, int result) {
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

    public void showNotification(Context context, Intent intent, @StringRes int stringId) {
        if(stringId == 0) {
            return;
        }
        // create message with SMS part id if it is defined
        String text = context.getString(stringId);
        int smsPartId = intent.getIntExtra(SMS_PART_ID, 0);
        if(smsPartId > 0) {
            text += " (ID=" + smsPartId + ")";
        }
        // TODO consider to user NotificationManager
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}
