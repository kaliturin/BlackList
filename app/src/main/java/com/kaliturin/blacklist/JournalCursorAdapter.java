package com.kaliturin.blacklist;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.kaliturin.blacklist.DatabaseAccessHelper.JournalRecord;
import com.kaliturin.blacklist.DatabaseAccessHelper.JournalRecordCursorWrapper;

/**
 * Journal data cursor adapter
 */
public class JournalCursorAdapter extends CursorAdapter {
    private final DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.SHORT);
    private final DateFormat timeFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
    private Date datetime = new Date();
    private IdentifiersContainer checkedItems = new IdentifiersContainer(0);
    private View.OnClickListener outerOnClickListener = null;
    private RowOnClickListener rowOnClickListener = new RowOnClickListener();

    JournalCursorAdapter(Context context) {
        super(context, null, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // TODO remove sample data from layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.journal_row, parent, false);

        // view holder for the row
        ViewHolder viewHolder = new ViewHolder(view);
        // add view holder to the row
        view.setTag(viewHolder);

        // on click listener for the row and checkbox (which is inside the row)
        view.setOnClickListener(rowOnClickListener);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // get cursor wrapper
        JournalRecordCursorWrapper cursorWrapper = (JournalRecordCursorWrapper) cursor;
        // get journal item
        JournalRecord item = cursorWrapper.getJournalRecord();
        // get view holder from the row
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        // update the view holder with new model
        viewHolder.setModel(context, item);
    }

    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
        // rebuild checked items container
        int size = (cursor != null ? cursor.getCount() : 0);
        checkedItems = new IdentifiersContainer(size);
    }

    void setOnClickListener(View.OnClickListener onClickListener) {
        this.outerOnClickListener = onClickListener;
    }

    // Returns checked items container
    IdentifiersContainer getCheckedItems() {
        return checkedItems;
    }

    // Sets all items checked/unchecked
    void setCheckedAllItems(boolean checked) {
        if(checkedItems.setAll(checked)) {
            notifyDataSetChanged();
        }
    }

    // Returns true if there are some checked items
    boolean hasCheckedItems() {
        return !checkedItems.isEmpty();
    }

    // Row on click listener
    private class RowOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            viewHolder.toggle();
            if(outerOnClickListener != null) {
                outerOnClickListener.onClick(view);
            }
        }
    }

    // View holder improves scroll performance
    private class ViewHolder {
        private int itemId;
        private CheckableLinearLayout rowView;
        private ImageView iconImageView;
        private TextView senderTextView;
        private TextView numberTextView;
        private TextView textTextView;
        private TextView dateTextView;
        private TextView timeTextView;
        private CheckBox checkBox;

        ViewHolder(View row) {
            this((CheckableLinearLayout) row,
                    (ImageView) row.findViewById(R.id.icon),
                    (TextView) row.findViewById(R.id.sender),
                    (TextView) row.findViewById(R.id.number),
                    (TextView) row.findViewById(R.id.text),
                    (TextView) row.findViewById(R.id.date),
                    (TextView) row.findViewById(R.id.time),
                    (CheckBox) row.findViewById(R.id.cb));
        }

        ViewHolder(CheckableLinearLayout rowView, ImageView iconImageView, TextView senderTextView,
                   TextView numberTextView, TextView textTextView, TextView dateTextView,
                   TextView timeTextView, CheckBox checkBox) {
            this.rowView = rowView;
            this.itemId = 0;
            this.iconImageView = iconImageView;
            this.senderTextView = senderTextView;
            this.numberTextView = numberTextView;
            this.textTextView = textTextView;
            this.dateTextView = dateTextView;
            this.timeTextView = timeTextView;
            this.checkBox = checkBox;
        }

        private void setModel(Context context, JournalRecord item) {
            itemId = (int) item.id;
            dateTextView.setText(dateFormat.format(toDate(item.time)));
            timeTextView.setText(timeFormat.format(toDate(item.time)));
            senderTextView.setText(Utils.translateNumberMetadata(context, item.caller));

            if(item.number != null &&
                    !item.caller.equals(item.number)) {
                numberTextView.setText(Utils.translateNumberMetadata(context, item.number));
                numberTextView.setVisibility(View.VISIBLE);
            } else {
                numberTextView.setText("");
                numberTextView.setVisibility(View.GONE);
            }

            if (item.text != null) {
                iconImageView.setImageResource(android.R.drawable.sym_action_email);
                textTextView.setText(item.text);
                textTextView.setVisibility(View.VISIBLE);
            } else {
                iconImageView.setImageResource(android.R.drawable.sym_action_call);
                textTextView.setText("");
                textTextView.setVisibility(View.GONE);
            }

            boolean checked = isChecked();
            checkBox.setChecked(checked);
            rowView.setChecked(checked);
        }

        private void toggle() {
            setChecked(!isChecked());
        }

        private boolean isChecked() {
            return checkedItems.contains(itemId);
        }

        private void setChecked(boolean checked) {
            checkedItems.set(itemId, checked);
            checkBox.setChecked(checked);
            rowView.setChecked(checked);
        }

        private Date toDate(long time) {
            datetime.setTime(time);
            return datetime;
        }
    }
}
