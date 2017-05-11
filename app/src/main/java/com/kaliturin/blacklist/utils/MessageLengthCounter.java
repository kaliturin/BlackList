/*
 * Copyright 2017 Anton Kaliturin
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

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

/**
 * Counts the length of the SMS-message being edited.
 * Shows the count in the TextView.
 */
public class MessageLengthCounter implements TextWatcher {
    private static final int SMS_LENGTH = 160;
    private static final int SMS_LENGTH2 = 153;
    private static final int SMS_LENGTH_UNICODE = 70;
    private static final int SMS_LENGTH2_UNICODE = 67;

    private TextView counterTextView;

    public MessageLengthCounter(TextView counterTextView) {
        this.counterTextView = counterTextView;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        update(s);
    }

    private void update(Editable messageText) {
        int messageLength = messageText.length();

        // is there unicode character in the message?
        boolean unicode = false;
        for (int i = 0; i < messageLength; i++) {
            char c = messageText.charAt(i);
            if (Character.UnicodeBlock.of(c) != Character.UnicodeBlock.BASIC_LATIN) {
                unicode = true;
                break;
            }
        }

        // get max length of sms part depending on encoding and full length
        int length1 = (unicode ? SMS_LENGTH_UNICODE : SMS_LENGTH);
        int length2 = (unicode ? SMS_LENGTH2_UNICODE : SMS_LENGTH2);
        int partMaxLength = (messageLength > length1 ? length2 : length1);
        // create current length status info
        int partsNumber = messageLength / partMaxLength + 1;
        int partLength = partMaxLength - messageLength % partMaxLength;
        // correct length info for second part
        if (partsNumber == 2 && partLength == partMaxLength) {
            partLength = length1 - (length1 - length2) * 2;
        }

        // show current length status info
        String counterText = "" + partLength + "/" + partsNumber;
        counterTextView.setText(counterText);
    }
}
