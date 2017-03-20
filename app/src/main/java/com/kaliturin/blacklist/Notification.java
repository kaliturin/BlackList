package com.kaliturin.blacklist;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v7.app.NotificationCompat;

import static android.support.v4.app.NotificationCompat.PRIORITY_MAX;

/**
 * Status bar notification
 */
class Notification {
    static void onCallBlocked(Context context, String address) {
        showNotification(context,
                address + " " + context.getString(R.string.call_is_blocked),
                R.drawable.ic_status_call_blocked, MainActivity.ACTION_JOURNAL, false);
    }

    static void onSmsBlocked(Context context, String address) {
        showNotification(context,
                address + " " + context.getString(R.string.message_is_blocked),
                R.drawable.ic_status_sms, MainActivity.ACTION_JOURNAL, false);
    }

    static void onSmsReceived(Context context, String address) {
        // TODO notify if user settings allowed
        //if(Settings.getBooleanValue(context, Settings.))
        showNotification(context, address + " " + context.getString(R.string.message_is_received),
                R.drawable.ic_status_sms, MainActivity.ACTION_SMS_CONVERSATIONS, false);
    }

    private static void showNotification(Context context, String message,
                                         int icon, String action, boolean vibrate) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(action);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(context.getString(R.string.app_name));
        builder.setContentText(message);
        builder.setSmallIcon(icon);
        builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), icon));
        // TODO: set vibration and ringtone
        if(vibrate) {
            builder.setVibrate(new long[]{1000, 1000});
        }
        builder.setPriority(PRIORITY_MAX);
        builder.setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
    }
}


/*

Ringtone ringtone = RingtoneManager.getRingtone(context, Settings.System.DEFAULT_RINGTONE_URI);

*/

/*

AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
switch (am.getRingerMode()) {
    case AudioManager.RINGER_MODE_SILENT:
        Log.i("MyApp","Silent mode");
        break;
    case AudioManager.RINGER_MODE_VIBRATE:
        Log.i("MyApp","Vibrate mode");
        break;
    case AudioManager.RINGER_MODE_NORMAL:
        Log.i("MyApp","Normal mode");
        break;
}

 */