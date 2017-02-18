package com.kaliturin.blacklist;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kaliturin.blacklist.ContactsAccessHelper.SMSRecordCursorWrapper;
import com.kaliturin.blacklist.ContactsAccessHelper.SMSRecord;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Cursor adapter for one SMS conversation
 */

public class SMSConversationCursorAdapter extends CursorAdapter {
    private final DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.SHORT);
    private Date datetime = new Date();
    private View.OnClickListener outerOnClickListener = null;
    private View.OnLongClickListener outerOnLongClickListener = null;
    private RowOnClickListener rowOnClickListener = new RowOnClickListener();
    private RowOnLongClickListener rowOnLongClickListener = new RowOnLongClickListener();

    SMSConversationCursorAdapter(Context context) {
        super(context, null, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.sms_conversation_row, parent, false);

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
        SMSRecord model = cursorWrapper.getSMSRecord();
        // get view holder from the row
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        // update the view holder with new model
        viewHolder.setModel(model);
    }

//------------------------------------------------------------------------

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

    private class ViewHolder {
        private SMSRecord model;
        private TextView bodyTextView;
        private TextView dateTextView;

        ViewHolder(View rowView) {
            this((TextView) rowView.findViewById(R.id.body),
                    (TextView) rowView.findViewById(R.id.date));
        }

        ViewHolder(TextView snippetTextView,
                   TextView dateTextView) {
            this.model = null;
            this.bodyTextView = snippetTextView;
            this.dateTextView = dateTextView;
        }

        void setModel(SMSRecord model) {
            this.model = model;
            String prefix = (model.type == SMSRecord.TYPE_INBOX ? "IN: " : "OUT: ");
            bodyTextView.setText(prefix + model.body);
            dateTextView.setText(dateFormat.format(toDate(model.date)));
        }

        private Date toDate(long time) {
            datetime.setTime(time);
            return datetime;
        }
    }
}
