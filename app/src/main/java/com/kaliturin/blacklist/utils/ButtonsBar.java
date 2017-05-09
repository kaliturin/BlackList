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

import android.support.annotation.IdRes;
import android.view.View;
import android.widget.TextView;

/**
 * Customized snack bar
 */
public class ButtonsBar {
    private View view;

    public ButtonsBar(View parentView, @IdRes int layoutId) {
        view = parentView.findViewById(layoutId);
        dismiss();
    }

    // Sets button's parameters
    public boolean setButton(@IdRes int buttonId, String title, View.OnClickListener listener) {
        TextView button = (TextView) view.findViewById(buttonId);
        if (button != null) {
            button.setText(title);
            button.setOnClickListener(listener);
            button.setVisibility(View.VISIBLE);
            return true;
        }

        return false;
    }

    // Returns true if snack bar is shown
    public boolean isShown() {
        return view.getVisibility() == View.VISIBLE;
    }

    // Shows shack bar
    public void show() {
        if (!isShown()) {
            view.setVisibility(View.VISIBLE);
        }
    }

    // Hides shack bar
    public boolean dismiss() {
        if (isShown()) {
            view.setVisibility(View.GONE);
            return true;
        }
        return false;
    }
}
