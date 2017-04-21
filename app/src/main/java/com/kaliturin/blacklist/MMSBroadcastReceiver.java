package com.kaliturin.blacklist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.kaliturin.blacklist.utils.DefaultSMSAppHelper;

/**
 * MMSBroadcastReceiver stub
 */

public class MMSBroadcastReceiver extends BroadcastReceiver {
    private static final String MMS_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED";
    private static final String MMS_DELIVER = "android.provider.Telephony.WAP_PUSH_DELIVER";
    private static final String MMS_TYPE = "application/vnd.wap.mms-message";

    @Override
    public void onReceive(Context context, Intent intent) {
        // check action
        String action = intent.getAction();
        String type = intent.getType();
        if (action == null || type == null ||
                !action.equals(getAction()) ||
                !type.equals(MMS_TYPE)) {
            return;
        }

        // if not default sms app
        if (!DefaultSMSAppHelper.isDefault(context)) {
            return;
        }

        // FIXME implement
    }

    public static String getAction() {
        return (DefaultSMSAppHelper.isAvailable() ? MMS_DELIVER : MMS_RECEIVED);
    }
}
