package com.kaliturin.blacklist;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

/**
 * Permissions check and request helper
 */
class Permissions {
    private static final String TAG = Permissions.class.getName();
    private static final int REQUEST_CODE = 1020;
    private static final Map<String, Boolean> permissionsResults = new HashMap<>();

    // Permissions that are using in the app
    static final String WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    static final String RECEIVE_SMS = "android.permission.RECEIVE_SMS";
    static final String WRITE_SMS = "android.permission.WRITE_SMS";
    static final String READ_SMS = "android.permission.READ_SMS";
    static final String CALL_PHONE = "android.permission.CALL_PHONE";
    static final String READ_PHONE_STATE = "android.permission.READ_PHONE_STATE";
    static final String READ_CONTACTS = "android.permission.READ_CONTACTS";
    static final String READ_CALL_LOG = "android.permission.READ_CALL_LOG";

    private static String[] PERMISSIONS = new String[]{
            WRITE_EXTERNAL_STORAGE,
            RECEIVE_SMS,
            WRITE_SMS,
            READ_SMS,
            CALL_PHONE,
            READ_PHONE_STATE,
            READ_CONTACTS,
            READ_CALL_LOG
    };

    // Checks for permission
    static boolean isGranted(@NonNull Context context, @NonNull String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        Boolean result = permissionsResults.get(permission);
        if(result == null) {
            int permissionCheck = ActivityCompat.checkSelfPermission(context, permission);
            result = (permissionCheck == PackageManager.PERMISSION_GRANTED);
            permissionsResults.put(permission, result);
        }
        return result;
    }

    // Checks for permissions and notifies if they aren't granted
    static void notifyIfNotGranted(@NonNull Activity activity) {
        for(String permission : PERMISSIONS) {
            notifyIfNotGranted(activity, permission);
        }
    }

    // Checks for permission and notifies if it isn't granted
    static boolean notifyIfNotGranted(@NonNull Activity activity, @NonNull String permission) {
        if(!isGranted(activity, permission)) {
            notify(activity, permission);
            return true;
        }
        return false;
    }

    // Notifies the user about permission isn't granted
    private static void notify(@NonNull final Activity activity, @NonNull String permission) {
        PackageManager pm = activity.getPackageManager();
        PermissionInfo info = null;
        try {
            info = pm.getPermissionInfo(permission, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException ex) {
            Log.w(TAG, ex);
        }

        if(info != null) {
            CharSequence label = info.loadLabel(pm);
            if(label == null) {
                label = info.nonLocalizedLabel;
            }
            final String message = "\"" + activity.getString(R.string.app_name) + "\" " +
                    activity.getString(R.string.needs_permission) + ": " + label;

            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Checks for permissions and shows a dialog for permission granting
    static void checkAndRequest(@NonNull Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(context, PERMISSIONS, REQUEST_CODE);
        }
    }

    // Resets permissions results cache
    static void invalidateCache() {
        permissionsResults.clear();
    }

    // Saves the results of permission granting request
    static void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CODE &&
                permissions.length == grantResults.length) {
            for (int i = 0; i < permissions.length; i++) {
                boolean result = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
                permissionsResults.put(permissions[i], result);
            }
        }
    }
}
