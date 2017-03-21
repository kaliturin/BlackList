package com.kaliturin.blacklist;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.v7.app.NotificationCompat;

import static android.support.v4.app.NotificationCompat.PRIORITY_MAX;

/**
 * Status bar & ringtone/vibration notification
 */
class Notifications {
    static void onCallBlocked(Context context, String address) {
        if(!Settings.getBooleanValue(context, Settings.BLOCKED_CALL_STATUS_NOTIFICATION)) {
            return;
        }
        String message = address + " " + context.getString(R.string.call_is_blocked);
        int icon = R.drawable.ic_block;
        String action = MainActivity.ACTION_JOURNAL;
        Uri ringtone = getRingtoneUri(context,
                Settings.BLOCKED_CALL_SOUND_NOTIFICATION,
                Settings.BLOCKED_CALL_RINGTONE);
        boolean vibration = Settings.getBooleanValue(context,
                Settings.BLOCKED_CALL_VIBRATION_NOTIFICATION);
        notify(context, message, icon, action, ringtone, vibration);
    }

    static void onSmsBlocked(Context context, String address) {
        if(!Settings.getBooleanValue(context, Settings.BLOCKED_SMS_STATUS_NOTIFICATION)) {
            return;
        }
        String message = address + " " + context.getString(R.string.message_is_blocked);
        int icon = R.drawable.ic_block;
        String action = MainActivity.ACTION_JOURNAL;
        Uri ringtone = getRingtoneUri(context,
                Settings.BLOCKED_SMS_SOUND_NOTIFICATION,
                Settings.BLOCKED_SMS_RINGTONE);
        boolean vibration = Settings.getBooleanValue(context,
                Settings.BLOCKED_SMS_VIBRATION_NOTIFICATION);
        notify(context, message, icon, action, ringtone, vibration);
    }

    static void onSmsReceived(Context context, String address) {
        String message = address + " " + context.getString(R.string.message_is_received);
        int icon = R.drawable.ic_status_sms;
        String action = MainActivity.ACTION_SMS_CONVERSATIONS;
        Uri ringtone = getRingtoneUri(context,
                Settings.RECEIVED_SMS_SOUND_NOTIFICATION,
                Settings.RECEIVED_SMS_RINGTONE);
        boolean vibration = Settings.getBooleanValue(context,
                Settings.RECEIVED_SMS_VIBRATION_NOTIFICATION);
        notify(context, message, icon, action, ringtone, vibration);
    }

    static void onSmsDelivery(Context context, String message) {
        if(!Settings.getBooleanValue(context, Settings.DELIVERY_SMS_NOTIFICATION)) {
            return;
        }
        int icon = R.drawable.ic_status_sms;
        String action = MainActivity.ACTION_SMS_CONVERSATIONS;
        Uri ringtone = getRingtoneUri(context,
                Settings.RECEIVED_SMS_SOUND_NOTIFICATION,
                Settings.RECEIVED_SMS_RINGTONE);
        boolean vibration = Settings.getBooleanValue(context,
                Settings.RECEIVED_SMS_VIBRATION_NOTIFICATION);
        notify(context, message, icon, action, ringtone, vibration);
    }

    private static void notify(Context context, String message, @DrawableRes int icon,
                                         String action, Uri ringtone, boolean vibration) {

        // turn off sound and vibration if phone is in silent mode
        AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
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
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        // build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(context.getString(R.string.app_name));
        builder.setTicker(message);
        builder.setContentText(message);
        builder.setSmallIcon(icon);
        builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), icon));
        builder.setPriority(PRIORITY_MAX);
        builder.setAutoCancel(true);
        if(ringtone != null) {
            builder.setSound(ringtone);
        }
        if(vibration) {
            builder.setVibrate(new long[]{1000, 1000});
        }
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
    }

    private static Uri getRingtoneUri(Context context, String notificationProperty,
                                      String ringtoneProperty) {
        Uri ringtone = null;
        // if ringtone notification is allowed
        if(Settings.getBooleanValue(context, notificationProperty)) {
            // get ringtone uri
            String uriString = Settings.getStringValue(context, ringtoneProperty);
            if(uriString != null) {
                ringtone = Uri.parse(uriString);
            } else {
                // if there isn't uri in setting - get default ringtone
                ringtone = android.provider.Settings.System.DEFAULT_RINGTONE_URI;
            }
        }
        return ringtone;
    }
}
