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
import android.util.SparseIntArray;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Sends SMS and processes the results of sending
 */

public class SMSSendHelper {
    private static final String MESSAGE_PART = "MESSAGE_PART";
    private static final String MESSAGE_PART_ID = "MESSAGE_PART_ID";

    private Set<BroadcastReceiver> receivers = new HashSet<>();
    private PendingResult pendingResult = null;

    // Sends SMS
    public boolean sendSMS(Context context, @NonNull String phoneNumber, @NonNull String message) {
        if(phoneNumber.isEmpty() || message.isEmpty()) {
            return false;
        }

        // get application context
        context = context.getApplicationContext();

        // clean from the previous usage
        clean(context);

        // divide message into parts
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> parts = smsManager.divideMessage(message);
        ArrayList<PendingIntent> sentIntents = new ArrayList<>(parts.size());
        ArrayList<PendingIntent> deliveryIntents = new ArrayList<>(parts.size());

        // prepare operation pending result
        pendingResult = new PendingResult(phoneNumber, message,
                System.currentTimeMillis(), parts.size());

        // create intents and result receivers for each part of message
        for(int i=0; i<parts.size(); i++) {
            String part = parts.get(i);

            // create on sent SMS receiver
            final ResultReceiver sentReceiver = new ResultReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    receivers.remove(this);
                    context.unregisterReceiver(this);
                    onSMSPartSent(context, intent, getResultCode());
                }
            };

            // create unique intent name and register receiver
            String intentName = "SMS_SENT" + "_" + hashCode() + "_" + i;
            PendingIntent pendingIntent = sentReceiver.register(context, intentName, part, i+1);
            sentIntents.add(pendingIntent);

            // create on delivery SMS receiver
            ResultReceiver deliveryReceiver = new ResultReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    receivers.remove(this);
                    context.unregisterReceiver(this);
                    onSMSPartDelivery(context, intent, getResultCode());
                }
            };

            // create unique intent name and register receiver
            intentName = "SMS_DELIVERED" + "_" + hashCode() + "_" + i;
            pendingIntent = deliveryReceiver.register(context, intentName, part, i+1);
            deliveryIntents.add(pendingIntent);
        }

        // send multipart message
        smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, deliveryIntents);

        return true;
    }

    // Is calling on SMS part sending results received
    public void onSMSPartSent(Context context, Intent intent, int result) {
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

        // update pending result
        if(pendingResult != null) {
            pendingResult.onSMSPartSent(context, intent, result);
        }

        // notify user about sending
        showNotification(context, intent, stringId);
    }

    // Is calling on all SMS parts sending results received
    public void onSMSSent(Context context, PendingResult result) {
        // write the sent SMS to the Outbox
        writeSMSToOutbox(context, result);
    }

    /** Is calling on SMS part delivery result received **/
    public void onSMSPartDelivery(Context context, Intent intent, int result) {
        int stringId = 0;
        switch (result) {
            case Activity.RESULT_OK:
                stringId = R.string.SMS_is_delivered;
                break;
            case Activity.RESULT_CANCELED:
                stringId = R.string.SMS_is_not_delivered;
                break;
        }

        // update pending result
        if(pendingResult != null) {
            pendingResult.onSMSPartDelivery(context, intent, result);
        }

        // notify user about delivery
        showNotification(context, intent, stringId);
    }

    /** Is calling on all SMS delivery results received **/
    public void onSMSDelivery(Context context, PendingResult result) {
    }

    /** Cleans pending results **/
    public void clean(Context context) {
        context = context.getApplicationContext();
        for(BroadcastReceiver receiver : receivers) {
            context.unregisterReceiver(receiver);
        }
        receivers.clear();
        pendingResult = null;
    }

    /** Shows notification on SMS sent/delivery **/
    public void showNotification(Context context, Intent intent, @StringRes int stringId) {
        if(stringId == 0) {
            return;
        }
        // create message with SMS part id if it is defined
        String text = context.getString(stringId);
        if(pendingResult.phoneNumber != null) {
            text = pendingResult.phoneNumber + " : " + text;
        }
        int messagePartId = intent.getIntExtra(MESSAGE_PART_ID, 0);
        if(messagePartId > 0) {
            text += " [" + messagePartId + "]";
        }
        // TODO consider to user NotificationManager
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    // Writes the sent SMS to the Outbox
    private void writeSMSToOutbox(Context context, PendingResult result) {
        // is app isn't default - the SMS will be written by the system
        if(DefaultSMSAppHelper.isDefault(context)) {
            // write the sent SMS to the Outbox
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
            db.writeSMSMessageToOutbox(context, result.phoneNumber, result.message, result.timeSent);
        }

        // send internal event message
        InternalEventBroadcast.sendSMSWasWritten(context, result.phoneNumber);
    }

    /** SMS sending operation pending result **/
    public class PendingResult {
        final SparseIntArray sentResults = new SparseIntArray();
        final SparseIntArray deliveryResults = new SparseIntArray();
        final String phoneNumber;
        final String message;
        final long timeSent;
        final int messageParts;

        PendingResult(String phoneNumber, String message, long timeSent, int messageParts) {
            this.phoneNumber = phoneNumber;
            this.message = message;
            this.timeSent = timeSent;
            this.messageParts = messageParts;
        }

        void onSMSPartSent(Context context, Intent intent, int result) {
            // save sent result
            int messagePartId = intent.getIntExtra(MESSAGE_PART_ID, 0);
            if (messagePartId > 0) {
                sentResults.put(messagePartId, result);
            }
            // notify all results were received
            if (sentResults.size() == messageParts) {
                onSMSSent(context, this);
            }
        }

        void onSMSPartDelivery(Context context, Intent intent, int result) {
            // save delivery result
            int messagePartId = intent.getIntExtra(MESSAGE_PART_ID, 0);
            if(messagePartId > 0) {
                deliveryResults.put(messagePartId, result);
            }
            // notify if all result were received
            if(deliveryResults.size() == messageParts) {
                onSMSDelivery(context, this);
            }
        }
    }

    // Sending SMS action results receiver
    private abstract class ResultReceiver extends BroadcastReceiver {
        PendingIntent register(Context context, String intentName, String messagePart,
                               int messagePartId) {
            // register receiver
            context.registerReceiver(this, new IntentFilter(intentName));
            receivers.add(this);
            // create pending intent
            Intent intent = new Intent(intentName);
            intent.putExtra(MESSAGE_PART, messagePart);
            intent.putExtra(MESSAGE_PART_ID, messagePartId);
            return PendingIntent.getBroadcast(context, 0, intent, 0);
        }
    }
}
