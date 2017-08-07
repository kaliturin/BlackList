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

package com.kaliturin.blacklist.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.support.annotation.AttrRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;

import com.kaliturin.blacklist.R;
import com.kaliturin.blacklist.activities.MainActivity;

import static android.support.v4.app.NotificationCompat.PRIORITY_MAX;

/**
 * Status bar & ringtone/vibration notification
 */
public class Notifications {
    // Notification on call blocked
    public static void onCallBlocked(Context context, String address) {
        if (!Settings.getBooleanValue(context, Settings.BLOCKED_CALL_STATUS_NOTIFICATION)) {
            return;
        }
        String message = context.getString(R.string.call_is_blocked);
        int icon = R.drawable.ic_block;
        String action = MainActivity.ACTION_JOURNAL;
        Uri ringtone = getRingtoneUri(context,
                Settings.BLOCKED_CALL_SOUND_NOTIFICATION,
                Settings.BLOCKED_CALL_RINGTONE);
        boolean vibration = Settings.getBooleanValue(context,
                Settings.BLOCKED_CALL_VIBRATION_NOTIFICATION);
        notify(context, address, message, message, icon, action, ringtone, vibration);
    }

    // Notification on SMS blocked
    public static void onSmsBlocked(Context context, String address) {
        if (!Settings.getBooleanValue(context, Settings.BLOCKED_SMS_STATUS_NOTIFICATION)) {
            return;
        }
        String message = context.getString(R.string.message_is_blocked);
        int icon = R.drawable.ic_block;
        String action = MainActivity.ACTION_JOURNAL;
        Uri ringtone = getRingtoneUri(context,
                Settings.BLOCKED_SMS_SOUND_NOTIFICATION,
                Settings.BLOCKED_SMS_RINGTONE);
        boolean vibration = Settings.getBooleanValue(context,
                Settings.BLOCKED_SMS_VIBRATION_NOTIFICATION);
        notify(context, address, message, message, icon, action, ringtone, vibration);
    }

    // Notification on SMS received
    public static void onSmsReceived(Context context, String address, String smsBody) {
        String message = context.getString(R.string.message_is_received);
        int icon = R.drawable.ic_status_sms;
        String action = MainActivity.ACTION_SMS_CONVERSATIONS;
        Uri ringtone = getRingtoneUri(context,
                Settings.RECEIVED_SMS_SOUND_NOTIFICATION,
                Settings.RECEIVED_SMS_RINGTONE);
        boolean vibration = Settings.getBooleanValue(context,
                Settings.RECEIVED_SMS_VIBRATION_NOTIFICATION);
        notify(context, address, message, smsBody, icon, action, ringtone, vibration);
    }

    public static void onSmsDelivery(Context context, String address, String message) {
        if (!Settings.getBooleanValue(context, Settings.DELIVERY_SMS_NOTIFICATION)) {
            return;
        }
        int icon = R.drawable.ic_status_sms;
        String action = MainActivity.ACTION_SMS_CONVERSATIONS;
        Uri ringtone = getRingtoneUri(context,
                Settings.RECEIVED_SMS_SOUND_NOTIFICATION,
                Settings.RECEIVED_SMS_RINGTONE);
        boolean vibration = Settings.getBooleanValue(context,
                Settings.RECEIVED_SMS_VIBRATION_NOTIFICATION);
        notify(context, address, message, message, icon, action, ringtone, vibration);
    }

    private static void notify(Context context, String title, String message, String ticker,
                               @DrawableRes int icon, String action, Uri ringtone,
                               boolean vibration) {

        // turn off sound and vibration if phone is in silent mode
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        switch (am.getRingerMode()) {
            case AudioManager.RINGER_MODE_SILENT:
                ringtone = null;
                vibration = false;
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                ringtone = null;
                break;
            case AudioManager.RINGER_MODE_NORMAL:
                break;
        }

        // pending intent for activating app on click in status bar
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        // build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(title);
        builder.setTicker(ticker);
        builder.setContentText(message);
        builder.setSmallIcon(icon);
        builder.setColor(getColor(context, R.attr.colorAccent));
        builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
        builder.setPriority(PRIORITY_MAX);
        builder.setAutoCancel(true);
        if (ringtone != null) {
            builder.setSound(ringtone);
        }
        if (vibration) {
            builder.setVibrate(new long[]{0, 2000});
        }
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
    }

    // Returns notification ringtone uri (if settings do not allow it - returns null)
    private static Uri getRingtoneUri(Context context, String notificationProperty,
                                      String ringtoneProperty) {
        Uri ringtone = null;
        // if ringtone notification is allowed
        if (Settings.getBooleanValue(context, notificationProperty)) {
            // get ringtone uri
            String uriString = Settings.getStringValue(context, ringtoneProperty);
            if (uriString != null) {
                ringtone = Uri.parse(uriString);
            } else {
                // if there isn't uri in setting - get default ringtone
                ringtone = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
            }
        }
        return ringtone;
    }

    private static int getColor(Context context, @AttrRes int attrRes) {
        int styleRes = R.style.AppTheme_Dark;
        if (Settings.getBooleanValue(context, Settings.UI_THEME_DARK)) {
            styleRes = R.style.AppTheme_Light;
        }
        int colorRes = Utils.getResourceId(context, attrRes, styleRes);
        return ContextCompat.getColor(context, colorRes);
    }
}
