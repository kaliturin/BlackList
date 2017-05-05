package com.kaliturin.blacklist.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.kaliturin.blacklist.R;


/**
 * Progress dialog holder
 */

public class ProgressDialogHolder {
    private Dialog dialog = null;
    private TextView messageTextView = null;

    public void show(Context context, @StringRes int messageId) {
        show(context, 0, messageId);
    }

    public void show(Context context, @StringRes int titleId, @StringRes int messageId) {
        show(context, titleId, messageId, null);
    }

    public void show(Context context, DialogInterface.OnCancelListener listener) {
        show(context, 0, 0, listener);
    }

    public void show(Context context, @StringRes int titleId, @StringRes int messageId,
                     DialogInterface.OnCancelListener listener) {
        dismiss();
        DialogBuilder builder = new DialogBuilder(context);
        if (titleId > 0) {
            builder.setTitle(titleId);
        }
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.row_progress, null);
        messageTextView = (TextView) itemView.findViewById(R.id.text_progress);
        if (messageId > 0) {
            messageTextView.setText(messageId);
        }
        builder.addItem(itemView);
        builder.setOnCancelListener(listener);
        dialog = builder.show();
    }

    public void setMessage(String message) {
        if (messageTextView != null) {
            messageTextView.setText(message);
        }
    }

    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
            messageTextView = null;
        }
    }
}
