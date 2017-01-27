package com.kaliturin.blacklist;

import android.app.Activity;
import android.app.Dialog;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Builder of dialog with menu items list
 */
public class MenuDialogBuilder {
    private Activity activity = null;
    private View view = null;
    private Dialog dialog = null;

    MenuDialogBuilder(@NonNull Activity activity) {
        this.activity = activity;
    }

    // Sets the title of the dialog
    public MenuDialogBuilder setDialogTitle(String title) {
        TextView titleView = (TextView) getView().findViewById(R.id.dialog_title);
        titleView.setText(title);
        return this;
    }

    // Adds the new item to the menu list with title and click listener
    public MenuDialogBuilder addMenuItem(String title, final View.OnClickListener listener) {
        return addMenuItem(title, true, listener);
    }

    // Adds the new item to the menu list with title and click listener
    public MenuDialogBuilder addMenuItem(String title, final boolean dismissOnClick,
                                         final View.OnClickListener listener) {
        LayoutInflater inflater = activity.getLayoutInflater();
        View itemView = inflater.inflate(R.layout.menu_dialog_row, null);
        LinearLayout listLayout = (LinearLayout) getView().findViewById(R.id.items_list);
        listLayout.addView(itemView);
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dismissOnClick) {
                    getDialog().dismiss();
                }
                listener.onClick(v);
            }
        });
        TextView titleView = (TextView) itemView.findViewById(R.id.item_title);
        titleView.setText(title);
        return this;
    }

    // Returns the dialog's view
    public View getView() {
        if (view == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            view = inflater.inflate(R.layout.menu_dialog, null);
        }

        return view;
    }

    // Returns the dialog
    public Dialog getDialog() {
        if (dialog == null) {
            dialog = new AlertDialog.Builder(activity).setView(getView()).create();
        }
        return dialog;
    }

    // Shows the dialog
    public void show() {
        getDialog().show();
    }
}
