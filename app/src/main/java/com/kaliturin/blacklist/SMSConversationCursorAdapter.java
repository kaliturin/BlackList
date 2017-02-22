package com.kaliturin.blacklist;

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

import com.kaliturin.blacklist.ContactsAccessHelper.SMSRecord;
import com.kaliturin.blacklist.ContactsAccessHelper.SMSRecordCursorWrapper;

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
    private View.OnClickListener outerOnClickListener = null;
    private View.OnLongClickListener outerOnLongClickListener = null;
    private RowOnClickListener rowOnClickListener = new RowOnClickListener();
    private RowOnLongClickListener rowOnLongClickListener = new RowOnLongClickListener();
    private Padding paddingStart;
    private Padding paddingEnd;

    SMSConversationCursorAdapter(Context context) {
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

        // on click listeners for the row and checkbox (which is inside the row)
        view.setOnClickListener(rowOnClickListener);
        view.setOnLongClickListener(rowOnLongClickListener);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // get cursor wrapper
        SMSRecordCursorWrapper cursorWrapper = (SMSRecordCursorWrapper) cursor;
        // get model
        SMSRecord model = cursorWrapper.getSMSRecord(context);
        // get view holder from the row
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        // update the view holder with new model
        viewHolder.setModel(context, model);
    }

//------------------------------------------------------------------------

    @Nullable
    SMSRecord getSMSRecord(View row) {
        if(row != null) {
            ViewHolder holder = (ViewHolder) row.getTag();
            return holder.model;
        }
        return null;
    }

    void setOnClickListener(View.OnClickListener onClickListener) {
        this.outerOnClickListener = onClickListener;
    }

    void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        this.outerOnLongClickListener = onLongClickListener;
    }

    // Row on long click listener
    private class RowOnLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View view) {
            return  (outerOnLongClickListener != null &&
                    outerOnLongClickListener.onLongClick(view));
        }
    }

    // Row on click listener
    private class RowOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if(outerOnClickListener != null) {
                outerOnClickListener.onClick(view);
            }
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
            if(gravity == Gravity.START) {
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
        private SMSRecord model;
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
            this.model = null;
            this.rowView = rowView;
            this.contentView = contentView;
            this.bodyTextView = snippetTextView;
            this.dateTextView = dateTextView;
        }

        void setModel(Context context, SMSRecord model) {
            this.model = model;
            bodyTextView.setText(model.body);
            Date date = toDate(model.date);
            String text = timeFormat.format(date) + ", " + dateFormat.format(date);
            dateTextView.setText(text);

            // init alignments and color
            Padding padding;
            int gravity, color;
            if(model.type == SMSRecord.TYPE_INBOX) {
                padding = paddingStart;
                gravity = Gravity.START;
                color = R.color.colorIncomeSms;
            } else {
                padding = paddingEnd;
                gravity = Gravity.END;
                color = R.color.colorOutcomeSms;
            }

            // set alignments
            ((LinearLayout) rowView).setGravity(gravity);
            rowView.setPadding(padding.left, padding.top, padding.right, padding.bottom);

            // set background color
            Drawable drawable = contentView.getBackground().mutate();
            Utils.setDrawableColor(context, drawable, color);
        }

        private Date toDate(long time) {
            datetime.setTime(time);
            return datetime;
        }
    }
}
