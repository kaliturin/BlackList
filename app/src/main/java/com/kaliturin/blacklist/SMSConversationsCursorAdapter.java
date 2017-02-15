package com.kaliturin.blacklist;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kaliturin.blacklist.ContactsAccessHelper.SMSConversationWrapper;
import com.kaliturin.blacklist.ContactsAccessHelper.SMSConversation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Cursor adapter for SMS conversations
 */

public class SMSConversationsCursorAdapter extends CursorAdapter {

    SMSConversationsCursorAdapter(Context context) {
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
        //view.setOnClickListener(rowOnClickListener);
        //view.setOnLongClickListener(rowOnLongClickListener);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // get cursor wrapper
        SMSConversationWrapper cursorWrapper = (SMSConversationWrapper) cursor;
        // get model
        SMSConversation model = cursorWrapper.getConversation();
        // get view holder from the row
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        // update the view holder with new model
        viewHolder.setModel(model);
    }

    private final DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.SHORT);
    private Date datetime = new Date();

    private class ViewHolder {
        private SMSConversation model;
        private TextView addressTextView;
        private TextView snippetTextView;
        private TextView dateTextView;

        ViewHolder(View rowView) {
            this((TextView) rowView.findViewById(R.id.address),
                 (TextView) rowView.findViewById(R.id.snippet),
                    (TextView) rowView.findViewById(R.id.date));
        }

        ViewHolder(TextView addressTextView,
                   TextView snippetTextView,
                   TextView dateTextView) {
            this.model = null;
            this.addressTextView = addressTextView;
            this.snippetTextView = snippetTextView;
            this.dateTextView = dateTextView;
        }

        void setModel(SMSConversation model) {
            this.model = model;
            addressTextView.setText(model.address);
            snippetTextView.setText(model.snippet);
            dateTextView.setText(dateFormat.format(toDate(model.date)));
        }

        private Date toDate(long time) {
            datetime.setTime(time);
            return datetime;
        }
    }
}
