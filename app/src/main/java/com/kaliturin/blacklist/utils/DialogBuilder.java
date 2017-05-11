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

package com.kaliturin.blacklist.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kaliturin.blacklist.R;

/**
 * Builder of a dialog with an items list
 */
public class DialogBuilder {
    private Context context;
    private View view;
    private Dialog dialog;
    private LinearLayout listLayout;
    private ButtonsBar buttonsBar;

    public DialogBuilder(@NonNull Context context) {
        this.context = context;
        this.listLayout = (LinearLayout) getView().findViewById(R.id.items_list);
    }

    /**
     * Sets the title of the dialog
     **/
    public DialogBuilder setTitle(@StringRes int titleId) {
        String title = context.getString(titleId);
        return setTitle(title);
    }

    /**
     * Sets the title of the dialog
     **/
    public DialogBuilder setTitle(String title) {
        TextView titleView = (TextView) getView().findViewById(R.id.dialog_title);
        titleView.setText(title);
        titleView.setVisibility(View.VISIBLE);
        View lineView = getView().findViewById(R.id.title_line);
        lineView.setVisibility(View.VISIBLE);
        return this;
    }

    /**
     * Sets the title of the dialog
     **/
    public DialogBuilder setTitle(String title, int maxLines) {
        TextView titleView = (TextView) getView().findViewById(R.id.dialog_title);
        titleView.setText(title);
        titleView.setMaxLines(maxLines);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        if (maxLines == 1) {
            titleView.setSingleLine(true);
        }
        titleView.setVisibility(View.VISIBLE);
        View lineView = getView().findViewById(R.id.title_line);
        lineView.setVisibility(View.VISIBLE);
        return this;
    }

    /**
     * Adds the new item to the list with title only
     **/
    public DialogBuilder addItem(@StringRes int titleId) {
        return addItem(-1, titleId, null, null);
    }

    /**
     * Adds the new item to the list with title and click listener
     **/
    public DialogBuilder addItem(@StringRes int titleId, View.OnClickListener listener) {
        return addItem(-1, titleId, null, listener);
    }

    /**
     * Adds the new item to the list with id, title, and click listener
     **/
    public DialogBuilder addItem(int id, @StringRes int titleId, View.OnClickListener listener) {
        return addItem(id, titleId, null, listener);
    }

    /**
     * Adds the new item to the list with id, title, tag, and click listener
     **/
    public DialogBuilder addItem(int id, @StringRes int titleId, Object tag, View.OnClickListener listener) {
        String title = context.getString(titleId);
        return addItem(id, title, tag, listener);
    }

    /**
     * Adds the new item to the list with title and click listener
     **/
    public DialogBuilder addItem(String title, final View.OnClickListener listener) {
        return addItem(-1, title, null, listener);
    }

    /**
     * Adds the new item to the list with id, title and click listener
     **/
    public DialogBuilder addItem(int id, String title, final View.OnClickListener listener) {
        return addItem(id, title, null, listener);
    }

    /**
     * Adds the new item to the list with id, title, tag and click listener
     **/
    public DialogBuilder addItem(int id, String title, Object tag, final View.OnClickListener listener) {
        // inflate row using default layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.row_item_dialog, null);
        itemView.setId(id);
        itemView.setTag(tag);

        // if there are some rows above
        if (listLayout.getChildCount() > 0) {
            // show top border
            View borderView = itemView.findViewById(R.id.item_top_border);
            if (borderView != null) {
                borderView.setVisibility(View.VISIBLE);
            }
        }

        if (listener != null) {
            ImageView imageView = (ImageView) itemView.findViewById(R.id.item_image);
            Utils.scaleViewOnTablet(context, imageView, R.dimen.iconScale);
            imageView.setVisibility(View.VISIBLE);
        }

        // wrap on click listener
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
                if (listener != null) {
                    listener.onClick(v);
                }
            }
        });

        TextView titleView = (TextView) itemView.findViewById(R.id.item_title);
        titleView.setText(title);

        return addItem(itemView);
    }

    /**
     * Adds the new item to the list
     **/
    public DialogBuilder addItem(View itemView) {
        listLayout.addView(itemView);
        return this;
    }

    /**
     * Adds the new edit to the list with id, text and hint
     **/
    public DialogBuilder addEdit(int id, String text, String hint) {
        // inflate row using default layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.row_edit_dialog, null);
        // if there are some rows above
        if (listLayout.getChildCount() > 0) {
            // show top border
            View borderView = itemView.findViewById(R.id.item_top_border);
            if (borderView != null) {
                borderView.setVisibility(View.VISIBLE);
            }
        }
        // setup edit
        EditText editText = (EditText) itemView.findViewById(R.id.edit_text);
        editText.setText(text);
        editText.setSelection(editText.getText().length());
        editText.setHint(hint);
        editText.setId(id);

        return addItem(itemView);
    }

    /**
     * Adds bottom-left button to the dialog
     **/
    public DialogBuilder addButtonLeft(String title,
                                       DialogInterface.OnClickListener listener) {
        return addButton(R.id.button_left, title, listener);
    }

    /**
     * Adds bottom-right button to the dialog
     **/
    public DialogBuilder addButtonRight(String title,
                                        DialogInterface.OnClickListener listener) {
        return addButton(R.id.button_right, title, listener);
    }

    /**
     * Adds bottom-center button to the dialog
     **/
    public DialogBuilder addButtonCenter(String title,
                                         DialogInterface.OnClickListener listener) {
        return addButton(R.id.button_center, title, listener);
    }

    private DialogBuilder addButton(@IdRes int buttonId, String title,
                                    final DialogInterface.OnClickListener listener) {
        View view = getView();
        if (buttonsBar == null) {
            buttonsBar = new ButtonsBar(view, R.id.three_buttons_bar);
            buttonsBar.show();
        }

        buttonsBar.setButton(buttonId, title, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onClick(getDialog(), 0);
                }
                getDialog().dismiss();
            }
        });

        return this;
    }

    // Returns the dialog's view
    private View getView() {
        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(R.layout.dialog_layout, null);
        }

        return view;
    }

    // Returns the dialog
    private Dialog getDialog() {
        if (dialog == null) {
            dialog = new AlertDialog.Builder(context).setView(getView()).create();
        }
        return dialog;
    }

    // Shows the dialog
    public Dialog show() {
        Dialog dialog = getDialog();
        dialog.show();
        return dialog;
    }

    public DialogBuilder setOnCancelListener(DialogInterface.OnCancelListener listener) {
        getDialog().setOnCancelListener(listener);
        return this;
    }
}
