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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.kaliturin.blacklist.R;
import com.kaliturin.blacklist.receivers.InternalEventBroadcast;
import com.kaliturin.blacklist.utils.ContactsAccessHelper;
import com.kaliturin.blacklist.utils.Notifications;
import com.kaliturin.blacklist.utils.Utils;

import static com.kaliturin.blacklist.utils.SMSSendHelper.DELIVERY;
import static com.kaliturin.blacklist.utils.SMSSendHelper.MESSAGE_ID;
import static com.kaliturin.blacklist.utils.SMSSendHelper.MESSAGE_PARTS;
import static com.kaliturin.blacklist.utils.SMSSendHelper.MESSAGE_PART_ID;
import static com.kaliturin.blacklist.utils.SMSSendHelper.PHONE_NUMBER;

/**
 * Receives the results of SMS sending/delivery
 */

public class SMSSendResultBroadcastReceiver extends BroadcastReceiver {
    public static final String SMS_SENT = "com.kaliturin.blacklist.SMS_SENT";
    public static final String SMS_DELIVERY = "com.kaliturin.blacklist.SMS_DELIVERY";

    @Override
    public void onReceive(Context context, Intent intent) {
        // check action
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case SMS_SENT:
                    onSMSSent(context, intent, getResultCode());
                    break;
                case SMS_DELIVERY:
                    onSMSDelivery(context, intent, getResultCode());
                    break;
            }
        }
    }

    // Is calling on SMS sending result received
    private void onSMSSent(Context context, Intent intent, int result) {
        boolean failed = true;
        int stringId = R.string.Unknown_error;
        switch (result) {
            case Activity.RESULT_OK:
                stringId = R.string.SMS_is_sent;
                failed = false;
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

        // get SMS parameters
        String phoneNumber = intent.getStringExtra(PHONE_NUMBER);
        if (phoneNumber != null) {
            int messagePartId = intent.getIntExtra(MESSAGE_PART_ID, 0);
            int messageParts = intent.getIntExtra(MESSAGE_PARTS, 0);
            long messageId = intent.getLongExtra(MESSAGE_ID, 0);
            boolean delivery = intent.getBooleanExtra(DELIVERY, false);

            // if the last part of the message was sent
            if (messageParts > 0 && messagePartId == messageParts) {
                // notify the user about the message sending
                String message = phoneNumber + "\n" + context.getString(stringId);
                Utils.showToast(context, message, Toast.LENGTH_SHORT);

                if (messageId >= 0) {
                    // update written message as sent
                    ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
                    db.updateSMSMessageOnSent(context, messageId, delivery, failed);
                }

                // send internal event message
                InternalEventBroadcast.sendSMSWasWritten(context, phoneNumber);
            }
        }
    }

    // Is calling on SMS delivery result received
    private void onSMSDelivery(Context context, Intent intent, int result) {
        boolean failed = true;
        int stringId = R.string.Unknown_error;
        switch (result) {
            case Activity.RESULT_OK:
                stringId = R.string.SMS_is_delivered;
                failed = false;
                break;
            case Activity.RESULT_CANCELED:
                stringId = R.string.SMS_is_not_delivered;
                break;
        }

        // get SMS parameters
        String phoneNumber = intent.getStringExtra(PHONE_NUMBER);
        if (phoneNumber != null) {
            int messagePartId = intent.getIntExtra(MESSAGE_PART_ID, 0);
            int messageParts = intent.getIntExtra(MESSAGE_PARTS, 0);
            long messageId = intent.getLongExtra(MESSAGE_ID, 0);

            // if the last part of the message was delivered
            if (messageParts > 0 && messagePartId == messageParts) {
                // notify the user about the message delivery
                String message = context.getString(stringId);
                Notifications.onSmsDelivery(context, phoneNumber, message);

                if (messageId >= 0) {
                    // update written message as delivered
                    ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
                    db.updateSMSMessageOnDelivery(context, messageId, failed);
                }

                // send internal event message
                InternalEventBroadcast.sendSMSWasWritten(context, phoneNumber);
            }
        }
    }
}
