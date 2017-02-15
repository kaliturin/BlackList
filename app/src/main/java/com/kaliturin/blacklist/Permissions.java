package com.kaliturin.blacklist;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import java.util.HashMap;
import java.util.Map;

/**
 * Permissions check and request
 */
public class Permissions {
    public static final String RECEIVE_SMS = "android.permission.RECEIVE_SMS";
    public static final String CALL_PHONE = "android.permission.CALL_PHONE";
    public static final String READ_PHONE_STATE = "android.permission.READ_PHONE_STATE";
    public static final String WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    public static final String READ_CONTACTS = "android.permission.READ_CONTACTS";
    public static final String READ_SMS = "android.permission.READ_SMS";
    public static final String READ_CALL_LOG = "android.permission.READ_CALL_LOG";

    public static final Map<String, Boolean> permissionsResults = new HashMap<>();

    public static boolean checkPermission(Context context, String permission) {
        int permissionCheck = ActivityCompat.checkSelfPermission(context, permission);
        return (permissionCheck == PackageManager.PERMISSION_GRANTED);
    }

    public static void checkAndRequestPermissions(Activity context) {
        String[] PERMISSIONS = new String[]{
                RECEIVE_SMS,
                CALL_PHONE,
                READ_PHONE_STATE,
                WRITE_EXTERNAL_STORAGE,
                READ_CONTACTS,
                READ_SMS,
                READ_CALL_LOG
        };
        for (int i = 0; i < PERMISSIONS.length; i++) {
            if (!checkPermission(context, PERMISSIONS[i])) {
                // TODO fix
                ActivityCompat.requestPermissions(context, PERMISSIONS, i);
            }
        }
    }

    public static void onRequestPermissionsResult(int requestCode,
                                                  @NonNull String permissions[],
                                                  @NonNull int[] grantResults) {
        if (permissions.length != grantResults.length) return;
        for (int i = 0; i < permissions.length; i++) {
            permissionsResults.put(permissions[i],
                    grantResults[i] == PackageManager.PERMISSION_GRANTED);
        }
    }
}
