package com.kaliturin.blacklist.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.kaliturin.blacklist.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Permissions check and request helper
 */
public class Permissions {
    private static final String TAG = Permissions.class.getName();
    private static final int REQUEST_CODE = 1020;
    private static final Map<String, Boolean> permissionsResults = new HashMap<>();

    // Permissions names
    public static final String WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    public static final String RECEIVE_SMS = "android.permission.RECEIVE_SMS";
    public static final String WRITE_SMS = "android.permission.WRITE_SMS";
    public static final String READ_SMS = "android.permission.READ_SMS";
    public static final String SEND_SMS = "android.permission.SEND_SMS";
    public static final String CALL_PHONE = "android.permission.CALL_PHONE";
    public static final String READ_PHONE_STATE = "android.permission.READ_PHONE_STATE";
    public static final String READ_CONTACTS = "android.permission.READ_CONTACTS";
    public static final String READ_CALL_LOG = "android.permission.READ_CALL_LOG";
    public static final String VIBRATE = "android.permission.VIBRATE";

    private static String[] PERMISSIONS = new String[]{
            WRITE_EXTERNAL_STORAGE,
            RECEIVE_SMS,
            WRITE_SMS,
            READ_SMS,
            SEND_SMS,
            CALL_PHONE,
            READ_PHONE_STATE,
            READ_CONTACTS,
            READ_CALL_LOG,
            VIBRATE
    };

    /**
     * Checks for permission
     **/
    public static synchronized boolean isGranted(@NonNull Context context, @NonNull String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        Boolean result = permissionsResults.get(permission);
        if (result == null) {
            int permissionCheck = ContextCompat.checkSelfPermission(context, permission);
            result = (permissionCheck == PackageManager.PERMISSION_GRANTED);
            permissionsResults.put(permission, result);
        }
        return result;
    }

    /**
     * Checks for permissions and notifies the user if they aren't granted
     **/
    public static synchronized void notifyIfNotGranted(@NonNull Context context) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String permission : PERMISSIONS) {
            if (!isGranted(context, permission)) {
                String info = getPermissionInfo(context, permission);
                sb.append(info);
                sb.append(";\n");
                count++;
            }
        }

        if (count > 0) {
            int duration;
            String message = "\"" + context.getString(R.string.app_name) + "\" ";
            if (count == 1) {
                duration = Toast.LENGTH_SHORT;
                message += context.getString(R.string.needs_permission) + ":\n" + sb.toString();
            } else {
                duration = Toast.LENGTH_LONG;
                message += context.getString(R.string.needs_permissions) + ":\n" + sb.toString();
            }
            Utils.showToast(context, message, duration);
        }
    }

    /**
     * Checks for permission and notifies if it isn't granted
     **/
    public static synchronized boolean notifyIfNotGranted(@NonNull Context context, @NonNull String permission) {
        if (!isGranted(context, permission)) {
            notify(context, permission);
            return true;
        }
        return false;
    }

    /**
     * Returns information string about permission
     **/
    @Nullable
    private static String getPermissionInfo(@NonNull Context context, @NonNull String permission) {
        context = context.getApplicationContext();
        PackageManager pm = context.getPackageManager();
        PermissionInfo info = null;
        try {
            info = pm.getPermissionInfo(permission, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException ex) {
            Log.w(TAG, ex);
        }

        if (info != null) {
            CharSequence label = info.loadLabel(pm);
            if (label == null) {
                label = info.nonLocalizedLabel;
            }
            return label.toString();
        }

        return null;
    }

    /**
     * Notifies the user if permission isn't granted
     **/
    private static void notify(@NonNull Context context, @NonNull String permission) {
        String info = getPermissionInfo(context, permission);
        if (info != null) {
            String message = "\"" + context.getString(R.string.app_name) + "\" " +
                    context.getString(R.string.needs_permission) + ": " + info;
            Utils.showToast(context, message, Toast.LENGTH_SHORT);
        }
    }

    /**
     * Checks for permissions and shows a dialog for permission granting
     **/
    public static void checkAndRequest(@NonNull Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(context, PERMISSIONS, REQUEST_CODE);
        }
    }

    /**
     * Resets permissions results cache
     **/
    public static synchronized void invalidateCache() {
        permissionsResults.clear();
    }

    /**
     * Saves the results of permission granting request
     **/
    public static synchronized  void onRequestPermissionsResult(int requestCode,
                                                  @NonNull String permissions[],
                                                  @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE &&
                permissions.length == grantResults.length) {
            for (int i = 0; i < permissions.length; i++) {
                boolean result = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
                permissionsResults.put(permissions[i], result);
            }
        }
    }
}
