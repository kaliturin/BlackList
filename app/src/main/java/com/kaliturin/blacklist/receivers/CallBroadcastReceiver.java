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

package com.kaliturin.blacklist.receivers;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.kaliturin.blacklist.R;
import com.kaliturin.blacklist.services.BlockEventProcessService;
import com.kaliturin.blacklist.utils.ContactsAccessHelper;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.Contact;
import com.kaliturin.blacklist.utils.Permissions;
import com.kaliturin.blacklist.utils.Settings;

import java.lang.reflect.Method;
import java.util.List;

/**
 * BroadcastReceiver for calls catching
 */
public class CallBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = CallBroadcastReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (state != null && !state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            return;
        }

        //SilentModeSwitcher silentMode = new SilentModeSwitcher();
        //silentMode.enable(context);
        try {
            processCall(context, intent);
        } catch (Exception ignored) {
        }
        //silentMode.disable(context);
        //RingerModeService.start(context, silentMode.getRingerMode(), silentMode.getVibrateSettings());
    }

    private void processCall(Context context, Intent intent) {
        if (!Permissions.isGranted(context, Permissions.READ_PHONE_STATE) ||
                !Permissions.isGranted(context, Permissions.CALL_PHONE)) {
            return;
        }

        // get telephony service
        TelephonyManager telephony = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephony.getCallState() != TelephonyManager.CALL_STATE_RINGING) {
            return;
        }

        // get incoming call number
        String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

        // private number detected
        if (ContactsAccessHelper.isPrivatePhoneNumber(number)) {
            // if block private numbers
            if (Settings.getBooleanValue(context, Settings.BLOCK_PRIVATE_CALLS) ||
                    // or if block all calls
                    Settings.getBooleanValue(context, Settings.BLOCK_ALL_CALLS)) {
                String name = context.getString(R.string.Private_number);
                // break call and notify user
                breakCallAndNotify(context, number, name);
            }
            return;
        }

        // normalize number
        number = ContactsAccessHelper.normalizePhoneNumber(number);
        if (number.isEmpty()) {
            Log.w(TAG, "Received call address is empty");
            return;
        }

        // get contacts linked to the current number
        List<Contact> contacts = getContacts(context, number);
        if (contacts == null) {
            return;
        }

        // if contact is from the white list
        Contact contact = findContactByType(contacts, Contact.TYPE_WHITE_LIST);
        if (contact != null) {
            return;
        }

        // get name of contact
        String name = (contacts.size() > 0 ? contacts.get(0).name : null);

        // if block all calls (excluding the white list)
        if (Settings.getBooleanValue(context, Settings.BLOCK_ALL_CALLS)) {
            // break call and notify user
            breakCallAndNotify(context, number, name);
            return;
        }

        // if block calls from the black list
        if (Settings.getBooleanValue(context, Settings.BLOCK_CALLS_FROM_BLACK_LIST)) {
            contact = findContactByType(contacts, Contact.TYPE_BLACK_LIST);
            if (contact != null) {
                // break call and notify user
                breakCallAndNotify(context, number, contact.name);
                return;
            }
        }

        boolean abort = false;

        // if block numbers that are not in the contact list
        if (Settings.getBooleanValue(context, Settings.BLOCK_CALLS_NOT_FROM_CONTACTS) &&
                Permissions.isGranted(context, Permissions.READ_CONTACTS)) {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
            if (db.getContact(context, number) != null) {
                return;
            }
            // there is no contact - get number as name
            name = number;
            abort = true;
        }

        // if block numbers that are not in the sms content list
        if (Settings.getBooleanValue(context, Settings.BLOCK_CALLS_NOT_FROM_SMS_CONTENT) &&
                Permissions.isGranted(context, Permissions.READ_SMS)) {
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
            if (db.containsNumberInSMSContent(context, number)) {
                return;
            }
            abort = true;
        }

        if (abort) {
            // break call and notify user
            breakCallAndNotify(context, number, name);
        }
    }

    // Ends phone call
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void breakCall(Context context) {
        if (!Permissions.isGranted(context, Permissions.CALL_PHONE)) {
            return;
        }

        TelephonyManager telephony = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class c = Class.forName(telephony.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            ITelephony telephonyService = (ITelephony) m.invoke(telephony);
            telephonyService.endCall();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Finds contact by type
    private Contact findContactByType(List<Contact> contacts, int contactType) {
        for (Contact contact : contacts) {
            if (contact.type == contactType) {
                return contact;
            }
        }
        return null;
    }

    // Finds contacts by number
    @Nullable
    private List<Contact> getContacts(Context context, String number) {
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(context);
        return (db == null ? null : db.getContacts(number, false));
    }

    // Breaks the call and notifies the user
    private void breakCallAndNotify(Context context, String number, String name) {
        // end phone call
        breakCall(context);
        // process the event of blocking in the service
        BlockEventProcessService.start(context, number, name, null);
    }

    // Silent mode enabling/disabling
    private static class SilentModeSwitcher {
        private AudioManager audioManager;
        private int ringerMode = AudioManager.RINGER_MODE_NORMAL;
        @SuppressWarnings("deprecation")
        private int vibrateSettings = AudioManager.VIBRATE_SETTING_ON;

        public int getRingerMode() {
            return ringerMode;
        }

        public int getVibrateSettings() {
            return vibrateSettings;
        }

        public void setRingerMode(Context context, int ringerMode) {
            getAudioManager(context).setRingerMode(ringerMode);
        }

        @SuppressWarnings("deprecation")
        public void setVibrateSettings(Context context, int vibrateSettings) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                getAudioManager(context).setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, vibrateSettings);
            }
        }

        public void enable(Context context) {
            setSilentMode(context, true);
        }

        public void disable(Context context) {
            setSilentMode(context, false);
        }

        private AudioManager getAudioManager(Context context) {
            if (audioManager == null) {
                audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            }
            return audioManager;
        }

        // Switch ringer mode to silent and back depending in passed flag
        @SuppressWarnings("deprecation")
        private void setSilentMode(Context context, boolean enabled) {
            if (!Permissions.isGranted(context, Permissions.WRITE_SETTINGS) ||
                    !Permissions.isGranted(context, Permissions.MODIFY_AUDIO_SETTINGS)) {
                return;
            }

            AudioManager audioManager = getAudioManager(context);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                if (enabled) {
                    vibrateSettings = audioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
                    audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_OFF);
                } else {
                    audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, vibrateSettings);
                }
            }

            if (enabled) {
                ringerMode = audioManager.getRingerMode();
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            } else {
                audioManager.setRingerMode(ringerMode);
            }
        }
    }

    public static class RingerModeService extends IntentService {
        public static final String RINGER_MODE = "RINGER_MODE";
        public static final String VIBRATE_SETTINGS = "VIBRATE_SETTINGS";

        public RingerModeService() {
            super(RingerModeService.class.getName());
        }

        @Override
        protected void onHandleIntent(@Nullable Intent intent) {
            Context context = getBaseContext();
            if (!Permissions.isGranted(context, Permissions.WRITE_SETTINGS) ||
                    !Permissions.isGranted(context, Permissions.MODIFY_AUDIO_SETTINGS)) {
                return;
            }

            if (intent == null) {
                return;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }

            SilentModeSwitcher silentMode = new SilentModeSwitcher();
            int ringerMode = intent.getIntExtra(RINGER_MODE, -1);
            if(ringerMode >= 0) {
                silentMode.setRingerMode(context, ringerMode);
            }
            int vibrateSettings = intent.getIntExtra(VIBRATE_SETTINGS, -1);
            if(vibrateSettings >= 0) {
                silentMode.setVibrateSettings(context, vibrateSettings);
            }
        }

        public static void start(Context context, int ringerMode, int vibrateSettings) {
            Intent intent = new Intent(context, RingerModeService.class);
            intent.putExtra(RINGER_MODE, ringerMode);
            intent.putExtra(VIBRATE_SETTINGS, vibrateSettings);
            context.startService(intent);
        }
    }
}