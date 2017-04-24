package com.kaliturin.blacklist.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.widget.CursorAdapter;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kaliturin.blacklist.R;
import com.kaliturin.blacklist.utils.ContactsAccessHelper;
import com.kaliturin.blacklist.utils.ContactsAccessHelper.SMSMessage;
import com.kaliturin.blacklist.utils.ContactsAccessHelper.SMSMessageCursorWrapper2;
import com.kaliturin.blacklist.utils.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Cursor adapter for one SMS conversation
 */

public class SMSConversationCursorAdapter extends CursorAdapter {
    private final DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);
    private final DateFormat timeFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
    private Date datetime = new Date();
    private View.OnLongClickListener outerOnLongClickListener = null;
    private RowOnLongClickListener rowOnLongClickListener = new RowOnLongClickListener();
    private Padding paddingStart;
    private Padding paddingEnd;

    public SMSConversationCursorAdapter(Context context) {
        super(context, null, 0);
        paddingStart = new Padding(context, Gravity.START, 5, 50);
        paddingEnd = new Padding(context, Gravity.END, 5, 50);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_sms_conversation, parent, false);

        // view holder for the row
        ViewHolder viewHolder = new ViewHolder(view);
        // add view holder to the row
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // get cursor wrapper
        SMSMessageCursorWrapper2 cursorWrapper = (SMSMessageCursorWrapper2) cursor;
        // get message
        SMSMessage model = cursorWrapper.getSMSMessage(false);
        // get view holder from the row
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        // update the view holder with new message
        viewHolder.setModel(context, model);
    }

//------------------------------------------------------------------------

    // Extracts SMS message data from the passed view if contains
    @Nullable
    public SMSMessage getSMSMessage(View view) {
        ViewHolder holder = null;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        }
        return (holder == null ? null : holder.message);
    }

    public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        this.outerOnLongClickListener = onLongClickListener;
    }

    // Row on long click listener
    private class RowOnLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View view) {
            return (outerOnLongClickListener != null &&
                    outerOnLongClickListener.onLongClick(view));
        }
    }

//------------------------------------------------------------------------

    // Padding calculator
    private class Padding {
        final int left;
        final int right;
        final int top;
        final int bottom;

        Padding(Context context, int gravity, int min, int max) {
            if (gravity == Gravity.START) {
                left = dpToPx(context, min);
                right = dpToPx(context, max);
            } else {
                left = dpToPx(context, max);
                right = dpToPx(context, min);
            }
            top = 0;
            bottom = 0;
        }

        private int dpToPx(Context context, int dp) {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        }
    }

    // Holder of the view data
    private class ViewHolder {
        private SMSMessage message;
        private View rowView;
        private TextView bodyTextView;
        private TextView dateTextView;
        private View contentView;

        ViewHolder(View rowView) {
            this(rowView,
                    rowView.findViewById(R.id.content_shape),
                    (TextView) rowView.findViewById(R.id.body),
                    (TextView) rowView.findViewById(R.id.date));
        }

        ViewHolder(View rowView,
                   View contentView,
                   TextView snippetTextView,
                   TextView dateTextView) {
            this.message = null;
            this.rowView = rowView;
            this.contentView = contentView;
            this.bodyTextView = snippetTextView;
            this.dateTextView = dateTextView;

            // add click listener to message area
            contentView.setTag(this);
            contentView.setOnLongClickListener(rowOnLongClickListener);
            bodyTextView.setTag(this);
            bodyTextView.setOnLongClickListener(rowOnLongClickListener);
        }

        void setModel(Context context, SMSMessage message) {
            this.message = message;
            bodyTextView.setText(message.body);

            String text;
            switch (message.type) {
                case ContactsAccessHelper.MESSAGE_TYPE_OUTBOX:
                    text = context.getString(R.string.Sending_);
                    break;
                case ContactsAccessHelper.MESSAGE_TYPE_FAILED:
                    text = context.getString(R.string.Failed);
                    break;
                default:
                    Date date = toDate(message.date);
                    text = timeFormat.format(date) + ", " + dateFormat.format(date);
                    break;
            }
            dateTextView.setText(text);

            // init alignments and color
            Padding padding;
            int gravity;
            int color;
            if (message.type == ContactsAccessHelper.MESSAGE_TYPE_INBOX) {
                padding = paddingStart;
                gravity = Gravity.START;
                color = R.attr.colorIncomeSms;
            } else {
                padding = paddingEnd;
                gravity = Gravity.END;
                color = R.attr.colorOutcomeSms;
            }

            // set alignments
            ((LinearLayout) rowView).setGravity(gravity);
            rowView.setPadding(padding.left, padding.top, padding.right, padding.bottom);

            Drawable drawable = contentView.getBackground().mutate();
            Utils.setDrawableColor(context, drawable, color);
        }

        private Date toDate(long time) {
            datetime.setTime(time);
            return datetime;
        }
    }
}
