package com.kaliturin.blacklist;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Builder of a dialog with a items list menu
 */
class MenuDialogBuilder {
    private Context context;
    private View view;
    private Dialog dialog;
    private LinearLayout listLayout;

    MenuDialogBuilder(@NonNull Context context) {
        this.context = context;
        this.listLayout = (LinearLayout) getView().findViewById(R.id.items_list);
    }

    // Sets the title of the dialog
    MenuDialogBuilder setTitle(@StringRes int titleId) {
        String title = context.getString(titleId);
        return setTitle(title);
    }

    // Sets the title of the dialog
    MenuDialogBuilder setTitle(String title) {
        TextView titleView = (TextView) getView().findViewById(R.id.dialog_title);
        titleView.setText(title);
        titleView.setVisibility(View.VISIBLE);
        return this;
    }

    // Adds the new item to the menu list with title and click listener
    MenuDialogBuilder addItem(@StringRes int titleId, final View.OnClickListener listener) {
        String title = context.getString(titleId);
        return addItem(title, true, listener);
    }

    // Adds the new item to the menu list with title and click listener
    MenuDialogBuilder addItem(String title, final View.OnClickListener listener) {
        return addItem(title, true, listener);
    }

    // Adds the new item to the menu list with title and click listener
    MenuDialogBuilder addItem(String title, final boolean dismissOnClick,
                              final View.OnClickListener listener) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.row_dialog_menu, null);
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

    MenuDialogBuilder setItemTag(Object tag) {
        int count = listLayout.getChildCount();
        if (count > 0) {
            View view = listLayout.getChildAt(count-1);
            view.setTag(tag);
        }
        return this;
    }

    // Returns the dialog's view
    View getView() {
        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(R.layout.dialog_menu, null);
        }

        return view;
    }

    // Returns the dialog
    Dialog getDialog() {
        if (dialog == null) {
            dialog = new AlertDialog.Builder(context).setView(getView()).create();
        }
        return dialog;
    }

    // Shows the dialog
    void show() {
        getDialog().show();
    }
}
