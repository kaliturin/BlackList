package com.kaliturin.blacklist;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Telephony;

/**
 * Utilities class for check/set app as a default SMS app.
 * Is needed since API19, where only default SMS app can stop SMS from receiving.
 */
class DefaultSMSAppHelper {

    public static boolean isAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static void updateState(Context context) {
        boolean ready = isDefault(context);
        enableSMSReceiving(context, ready);
    }

    public static void enableSMSReceiving(Context context, boolean enable) {
        int state = (enable ?
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, SMSBroadcastReceiver.class);
        packageManager.setComponentEnabledSetting(
                componentName,
                state,
                PackageManager.DONT_KILL_APP);
    }

    @TargetApi(19)
    public static boolean isDefault(Context context) {
        if (!isAvailable()) return true;
        String myPackage = context.getPackageName();
        String smsPackage = Telephony.Sms.getDefaultSmsPackage(context);
        return (smsPackage != null && smsPackage.equals(myPackage));
    }

    @TargetApi(19)
    public static void askForDefaultAppChange(Activity activity, int requestCode) {
        if (!isAvailable()) return;
        String packageName;
        // current app package is already set as default
        if (isDefault(activity)) {
            // get native app package as default
            packageName = Settings.getStringValue(activity, Settings.DEFAULT_SMS_APP_NATIVE_PACKAGE);
        } else {
            // save native app package to the settings
            String nativePackage = Telephony.Sms.getDefaultSmsPackage(activity);
            Settings.setStringValue(activity, Settings.DEFAULT_SMS_APP_NATIVE_PACKAGE, nativePackage);
            // get current app package as default
            packageName = activity.getPackageName();
        }
        askForDefaultAppChange(activity, packageName, requestCode);
    }

    @TargetApi(19)
    private static void askForDefaultAppChange(Activity activity, String packageName, int requestCode) {
        if (!isAvailable()) return;
        Intent intent;
        if(packageName == null) {
            // TODO check on older versions
            intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
        } else {
            intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName);
        }
        activity.startActivityForResult(intent, requestCode);
    }
}
