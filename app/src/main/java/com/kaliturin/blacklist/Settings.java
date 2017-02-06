package com.kaliturin.blacklist;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Settings name/value container
 */

public class Settings {
    public static final String BLOCK_CALLS = "BLOCK_CALLS";
    public static final String BLOCK_ALL_CALLS = "BLOCK_ALL_CALLS";
    public static final String BLOCK_CALLS_NOT_FROM_CONTACTS = "BLOCK_CALLS_NOT_FROM_CONTACTS";
    public static final String BLOCK_CALLS_NOT_FROM_SMS_INBOX = "BLOCK_CALLS_NOT_FROM_SMS_INBOX";
    public static final String BLOCK_HIDDEN_CALLS = "BLOCK_HIDDEN_CALLS";
    public static final String SHOW_CALLS_NOTIFICATIONS = "SHOW_CALLS_NOTIFICATIONS";
    public static final String WRITE_CALLS_JOURNAL  = "WRITE_CALLS_JOURNAL";

    public static final String BLOCK_SMS = "BLOCK_SMS";
    public static final String BLOCK_ALL_SMS = "BLOCK_ALL_SMS";
    public static final String BLOCK_SMS_NOT_FROM_CONTACTS = "BLOCK_SMS_NOT_FROM_CONTACTS";
    public static final String BLOCK_SMS_NOT_FROM_INBOX = "BLOCK_SMS_NOT_FROM_INBOX";
    public static final String BLOCK_HIDDEN_SMS = "BLOCK_HIDDEN_SMS";
    public static final String SHOW_SMS_NOTIFICATIONS = "SHOW_SMS_NOTIFICATIONS";
    public static final String WRITE_SMS_JOURNAL  = "WRITE_SMS_JOURNAL";

    public static final String DEFAULT_SMS_APP_NATIVE_PACKAGE = "DEFAULT_SMS_APP_NATIVE_PACKAGE";

    public static final String TRUE = "TRUE";
    public static final String FALSE = "FALSE";

    private static Map<String, String> map = new HashMap<>();

    public static boolean setStringValue(Context context, @NonNull String name, @NonNull String value) {
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(context);
        if(db.setSettingsValue(name, value)) {
            map.put(name, value);
            return true;
        }
        return false;
    }

    public static @Nullable String getStringValue(Context context, @NonNull String name) {
        String value = map.get(name);
        if(value == null) {
            DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(context);
            value = db.getSettingsValue(name);
            map.put(name, value);
        }
        return value;
    }

    public static boolean setBooleanValue(Context context, @NonNull String name, boolean value) {
        String v = (value ? TRUE : FALSE);
        return setStringValue(context, name, v);
    }

    public static boolean getBooleanValue(Context context, @NonNull String name) {
        String value = getStringValue(context, name);
        return (value != null && value.equals(TRUE));
    }

    public static void setDefaults(Context context) {
        Map<String, String> map = new HashMap<>();
        map.put(BLOCK_CALLS, TRUE);
        map.put(BLOCK_ALL_CALLS, FALSE);
        map.put(BLOCK_CALLS_NOT_FROM_CONTACTS, FALSE);
        map.put(BLOCK_CALLS_NOT_FROM_SMS_INBOX, FALSE);
        map.put(BLOCK_HIDDEN_CALLS, FALSE);
        map.put(SHOW_CALLS_NOTIFICATIONS, TRUE);
        map.put(WRITE_CALLS_JOURNAL, TRUE);

        map.put(BLOCK_SMS, TRUE);
        map.put(BLOCK_ALL_SMS, FALSE);
        map.put(BLOCK_SMS_NOT_FROM_CONTACTS, FALSE);
        map.put(BLOCK_SMS_NOT_FROM_INBOX, FALSE);
        map.put(BLOCK_HIDDEN_SMS, FALSE);
        map.put(SHOW_SMS_NOTIFICATIONS, TRUE);
        map.put(WRITE_SMS_JOURNAL, TRUE);

        for(Map.Entry<String, String> entry : map.entrySet()) {
            if(getStringValue(context, entry.getKey()) == null) {
                setStringValue(context, entry.getKey(), entry.getValue());
            }
        }
    }
}
