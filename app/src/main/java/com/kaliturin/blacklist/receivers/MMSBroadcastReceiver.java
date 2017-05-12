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

        // isn't implemented yet...
    }

    public static String getAction() {
        return (DefaultSMSAppHelper.isAvailable() ? MMS_DELIVER : MMS_RECEIVED);
    }
}