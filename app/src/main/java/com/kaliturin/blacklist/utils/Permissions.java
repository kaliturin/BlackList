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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Permissions check and request helper
 */
public class Permissions {
    private static final String TAG = Permissions.class.getName();
    private static final int REQUEST_CODE = (Permissions.class.hashCode() & 0xffff);
    private static final Map<String, Boolean> permissionsResults = new ConcurrentHashMap<>();

    // Permissions names
    public static final String WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    public static final String RECEIVE_SMS = "android.permission.RECEIVE_SMS";
    public static final String WRITE_SMS = "android.permission.WRITE_SMS";
    public static final String READ_SMS = "android.permission.READ_SMS";
    public static final String SEND_SMS = "android.permission.SEND_SMS";
    public static final String CALL_PHONE = "android.permission.CALL_PHONE";
    public static final String READ_PHONE_STATE = "android.permission.READ_PHONE_STATE";
    public static final String READ_CONTACTS = "android.permission.READ_CONTACTS";
    public static final String WRITE_CONTACTS = "android.permission.WRITE_CONTACTS";
    public static final String READ_CALL_LOG = "android.permission.READ_CALL_LOG";
    public static final String WRITE_CALL_LOG = "android.permission.WRITE_CALL_LOG";
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
            WRITE_CONTACTS,
            READ_CALL_LOG,
            WRITE_CALL_LOG,
            VIBRATE
    };

    /**
     * Checks for permission
     **/
    public static boolean isGranted(@NonNull Context context, @NonNull String permission) {
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
    public static void notifyIfNotGranted(@NonNull Context context) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String permission : PERMISSIONS) {
            if (!isGranted(context, permission)) {
                if (count > 0) {
                    sb.append("\n");
                }
                String info = getPermissionInfo(context, permission);
                sb.append(info);
                sb.append(";");
                count++;
            }
        }

        if (count > 0) {
            int duration;
            String message = context.getString(R.string.app_name) + " ";
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
    public static boolean notifyIfNotGranted(@NonNull Context context, @NonNull String permission) {
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
            String message = context.getString(R.string.app_name) + " " +
                    context.getString(R.string.needs_permission) + ":\n" + info + ";";
            Utils.showToast(context, message, Toast.LENGTH_SHORT);
        }
    }

    /**
     * Checks for permissions and shows a dialog for permission granting
     **/
    public static void checkAndRequest(@NonNull Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> permissions = new LinkedList<>();
            for (String permission : PERMISSIONS) {
                if (!isGranted(context, permission)) {
                    permissions.add(permission);
                }
            }
            if (permissions.size() > 0) {
                String[] array = permissions.toArray(new String[permissions.size()]);
                ActivityCompat.requestPermissions(context, array, REQUEST_CODE);
            }
        }
    }

    /**
     * Resets permissions results cache
     **/
    public static void invalidateCache() {
        permissionsResults.clear();
    }

    /**
     * Saves the results of permission granting request
     **/
    public static void onRequestPermissionsResult(int requestCode,
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
