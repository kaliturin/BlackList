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

package com.kaliturin.blacklist;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.kaliturin.blacklist.utils.SMSSendHelper;

/**
 * SMS message sending service
 */
public class SMSSendService extends IntentService {
    private static final String MESSAGE = "MESSAGE";
    private static final String ADDRESSES = "ADDRESSES";

    public SMSSendService() {
        super(SMSSendService.class.getName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }

        // get message body and addresses
        String message = intent.getStringExtra(MESSAGE);
        String[] addresses = intent.getStringArrayExtra(ADDRESSES);
        if (message == null || addresses == null) {
            return;
        }

        // send SMS message
        SMSSendHelper smsSendHelper = new SMSSendHelper();
        for (String address : addresses) {
            smsSendHelper.sendSMS(this, address, message);
        }
    }

    public static void run(Context context, String message, String[] addresses) {
        Intent intent = new Intent(context, SMSSendService.class);
        intent.putExtra(MESSAGE, message);
        intent.putExtra(ADDRESSES, addresses);
        context.startService(intent);
    }
}
