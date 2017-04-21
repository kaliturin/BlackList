package com.kaliturin.blacklist.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Settings name/value persistence container
 */

public class Settings {
    public static final String BLOCK_CALLS_FROM_BLACK_LIST = "BLOCK_CALLS_FROM_BLACK_LIST";
    public static final String BLOCK_ALL_CALLS = "BLOCK_ALL_CALLS";
    public static final String BLOCK_CALLS_NOT_FROM_CONTACTS = "BLOCK_CALLS_NOT_FROM_CONTACTS";
    public static final String BLOCK_CALLS_NOT_FROM_SMS_CONTENT = "BLOCK_CALLS_NOT_FROM_SMS_CONTENT";
    public static final String BLOCK_HIDDEN_CALLS = "BLOCK_HIDDEN_CALLS";
    public static final String BLOCKED_CALL_STATUS_NOTIFICATION = "BLOCKED_CALL_STATUS_NOTIFICATION";
    public static final String WRITE_CALLS_JOURNAL = "WRITE_CALLS_JOURNAL";
    public static final String BLOCK_SMS_FROM_BLACK_LIST = "BLOCK_SMS_FROM_BLACK_LIST";
    public static final String BLOCK_ALL_SMS = "BLOCK_ALL_SMS";
    public static final String BLOCK_SMS_NOT_FROM_CONTACTS = "BLOCK_SMS_NOT_FROM_CONTACTS";
    public static final String BLOCK_SMS_NOT_FROM_SMS_CONTENT = "BLOCK_SMS_NOT_FROM_SMS_CONTENT";
    public static final String BLOCK_HIDDEN_SMS = "BLOCK_HIDDEN_SMS";
    public static final String BLOCKED_SMS_STATUS_NOTIFICATION = "BLOCKED_SMS_STATUS_NOTIFICATION";
    public static final String WRITE_SMS_JOURNAL = "WRITE_SMS_JOURNAL";
    public static final String BLOCKED_SMS_SOUND_NOTIFICATION = "BLOCKED_SMS_SOUND_NOTIFICATION";
    public static final String RECEIVED_SMS_SOUND_NOTIFICATION = "RECEIVED_SMS_SOUND_NOTIFICATION";
    public static final String BLOCKED_SMS_VIBRATION_NOTIFICATION = "BLOCKED_SMS_VIBRATION_NOTIFICATION";
    public static final String RECEIVED_SMS_VIBRATION_NOTIFICATION = "RECEIVED_SMS_VIBRATION_NOTIFICATION";
    public static final String BLOCKED_SMS_RINGTONE = "BLOCKED_SMS_RINGTONE";
    public static final String RECEIVED_SMS_RINGTONE = "RECEIVED_SMS_RINGTONE";
    public static final String BLOCKED_CALL_SOUND_NOTIFICATION = "BLOCKED_CALL_SOUND_NOTIFICATION";
    public static final String BLOCKED_CALL_VIBRATION_NOTIFICATION = "BLOCKED_CALL_VIBRATION_NOTIFICATION";
    public static final String BLOCKED_CALL_RINGTONE = "BLOCKED_CALL_RINGTONE";
    public static final String DELIVERY_SMS_NOTIFICATION = "DELIVERY_SMS_NOTIFICATION";
    public static final String FOLD_SMS_TEXT_IN_JOURNAL = "FOLD_SMS_TEXT_IN_JOURNAL";
    public static final String UI_THEME_DARK = "UI_THEME_DARK";
    public static final String GO_TO_JOURNAL_AT_START = "GO_TO_JOURNAL_AT_START";
    public static final String DEFAULT_SMS_APP_NATIVE_PACKAGE = "DEFAULT_SMS_APP_NATIVE_PACKAGE";
    public static final String EXIT_ON_BACK_PRESSED = "EXIT_ON_BACK_PRESSED";

    private static final String TRUE = "TRUE";
    private static final String FALSE = "FALSE";

    private static Map<String, String> map = new HashMap<>();

    public static void invalidateCache() {
        map.clear();
    }

    public static boolean setStringValue(Context context, @NonNull String name, @NonNull String value) {
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(context);
        if (db != null) {
            if (db.setSettingsValue(name, value)) {
                map.put(name, value);
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static String getStringValue(Context context, @NonNull String name) {
        String value = map.get(name);
        if (value == null) {
            DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(context);
            if (db != null) {
                value = db.getSettingsValue(name);
                map.put(name, value);
            }
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

    public static void initDefaults(Context context) {
        Map<String, String> map = new HashMap<>();
        map.put(BLOCK_CALLS_FROM_BLACK_LIST, TRUE);
        map.put(BLOCK_ALL_CALLS, FALSE);
        map.put(BLOCK_CALLS_NOT_FROM_CONTACTS, FALSE);
        map.put(BLOCK_CALLS_NOT_FROM_SMS_CONTENT, FALSE);
        map.put(BLOCK_HIDDEN_CALLS, FALSE);
        map.put(WRITE_CALLS_JOURNAL, TRUE);
        map.put(BLOCKED_CALL_STATUS_NOTIFICATION, TRUE);
        map.put(BLOCKED_CALL_SOUND_NOTIFICATION, FALSE);
        map.put(BLOCKED_CALL_VIBRATION_NOTIFICATION, FALSE);
        map.put(BLOCK_SMS_FROM_BLACK_LIST, TRUE);
        map.put(BLOCK_ALL_SMS, FALSE);
        map.put(BLOCK_SMS_NOT_FROM_CONTACTS, FALSE);
        map.put(BLOCK_SMS_NOT_FROM_SMS_CONTENT, FALSE);
        map.put(BLOCK_HIDDEN_SMS, FALSE);
        map.put(BLOCKED_SMS_STATUS_NOTIFICATION, TRUE);
        map.put(WRITE_SMS_JOURNAL, TRUE);
        map.put(BLOCKED_SMS_SOUND_NOTIFICATION, FALSE);
        map.put(RECEIVED_SMS_SOUND_NOTIFICATION, TRUE);
        map.put(BLOCKED_SMS_SOUND_NOTIFICATION, FALSE);
        map.put(RECEIVED_SMS_VIBRATION_NOTIFICATION, TRUE);
        map.put(BLOCKED_SMS_VIBRATION_NOTIFICATION, FALSE);
        map.put(DELIVERY_SMS_NOTIFICATION, TRUE);
        map.put(FOLD_SMS_TEXT_IN_JOURNAL, TRUE);
        map.put(UI_THEME_DARK, FALSE);
        map.put(GO_TO_JOURNAL_AT_START, FALSE);
        map.put(EXIT_ON_BACK_PRESSED, TRUE);

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String setting = entry.getKey();
            if (getStringValue(context, setting) == null) {
                String value = entry.getValue();
                setStringValue(context, setting, value);
            }
        }
    }
}
