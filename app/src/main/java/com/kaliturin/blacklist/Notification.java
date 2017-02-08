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
public class Notification {
    public static void onCallNotification(Context context, String address) {
        showNotification(context,
                address + " " + context.getString(R.string.is_blocked),
                R.drawable.ic_status_call_blocked);
    }

    public static void onSmsNotification(Context context, String address) {
        showNotification(context,
                address + " " + context.getString(R.string.is_blocked),
                R.drawable.ic_status_sms_blocked);
    }

    private static void showNotification(Context context, String message, int icon) {
        Intent activityIntent = new Intent(context, MainActivity.class);
        //activityIntent.setAction(MainActivity.ACTION_SHOW_JOURNAL);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, activityIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(context.getString(R.string.app_name));
        builder.setContentText(message);
        builder.setSmallIcon(icon);
        builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), icon));
        //builder.setVibrate(new long[]{DEFAULT_VIBRATE});
        builder.setPriority(PRIORITY_MAX);
        builder.setAutoCancel(true);
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
    }
}
