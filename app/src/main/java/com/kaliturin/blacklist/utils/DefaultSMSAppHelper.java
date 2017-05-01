package com.kaliturin.blacklist.utils;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Telephony;
import android.support.v4.app.Fragment;

import com.kaliturin.blacklist.SMSBroadcastReceiver;

/**
 * Utilities class for check/set app as a default SMS app.
 * Is needed since API19, where only default SMS app can stop SMS from receiving.
 */
public class DefaultSMSAppHelper {

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
    public static void askForDefaultAppChange(Fragment fragment, int requestCode) {
        if (!isAvailable()) return;
        Context context = fragment.getContext().getApplicationContext();
        String packageName;
        // current app package is already set as default
        if (isDefault(context)) {
            // get native app package as default
            packageName = Settings.getStringValue(context, Settings.DEFAULT_SMS_APP_NATIVE_PACKAGE);
        } else {
            // save native app package to the settings
            String nativePackage = Telephony.Sms.getDefaultSmsPackage(context);
            Settings.setStringValue(context, Settings.DEFAULT_SMS_APP_NATIVE_PACKAGE, nativePackage);
            // get current app package as default
            packageName = context.getPackageName();
        }
        askForDefaultAppChange(fragment, packageName, requestCode);
    }

    @TargetApi(19)
    private static void askForDefaultAppChange(Fragment fragment, String packageName, int requestCode) {
        if (!isAvailable()) return;
        Intent intent;
        if (packageName == null) {
            String action;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                action = android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS;
            } else {
                action = android.provider.Settings.ACTION_WIRELESS_SETTINGS;
            }
            intent = new Intent(action);
        } else {
            intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName);
        }
        fragment.startActivityForResult(intent, requestCode);
    }
}
