package com.kaliturin.blacklist;

import android.support.annotation.IdRes;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Customized snack bar
 */
public class CustomSnackBar {
    private ViewGroup snackBarView;

    public CustomSnackBar(View parentView, @IdRes int snackBarId) {
        snackBarView = (ViewGroup) parentView.findViewById(snackBarId);
        // hide all children views
        for (int i = 0; i < snackBarView.getChildCount(); i++) {
            View view = snackBarView.getChildAt(i);
            view.setVisibility(View.INVISIBLE);
        }

        dismiss();
    }

    // Sets button's parameters
    public boolean setButton(@IdRes int buttonId, String title, View.OnClickListener listener) {
        TextView button = (TextView) snackBarView.findViewById(buttonId);
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
        return snackBarView.getVisibility() == View.VISIBLE;
    }

    // Shows shack bar
    public void show() {
        if (!isShown()) {
            snackBarView.setVisibility(View.VISIBLE);
        }
    }

    // Hides shack bar
    public boolean dismiss() {
        if (isShown()) {
            snackBarView.setVisibility(View.GONE);
            return true;
        }
        return false;
    }
}
