package com.kaliturin.blacklist;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.kaliturin.blacklist.DatabaseAccessHelper.JournalRecord;
import com.kaliturin.blacklist.DatabaseAccessHelper.JournalRecordCursorWrapper;

/**
 * Journal data cursor adapter
 */
public class JournalCursorAdapter extends CursorAdapter {
    private final DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.LONG);
    private final DateFormat timeFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
    private Date datetime = new Date();
    private Calendar calendar1 = Calendar.getInstance();
    private Calendar calendar2 = Calendar.getInstance();
    private IdentifiersContainer checkedItems = new IdentifiersContainer(0);
    private View.OnClickListener outerOnClickListener = null;
    private View.OnLongClickListener outerOnLongClickListener = null;
    private RowOnClickListener rowOnClickListener = new RowOnClickListener();
    private RowOnLongClickListener rowOnLongClickListener = new RowOnLongClickListener();
    private long recordTime = 0;

    JournalCursorAdapter(Context context) {
        super(context, null, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_journal, parent, false);

        // link row-view to view holder
        view.setTag(new ViewHolder(view));

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // get cursor wrapper
        JournalRecordCursorWrapper cursorWrapper = (JournalRecordCursorWrapper) cursor;
        // FIXME consider to read record's id at first and whole record at second (for memory saving)
        // get journal item
        JournalRecord record = cursorWrapper.getJournalRecord();
        // get view holder from the row
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        boolean showDate = false;
        calendar1.setTimeInMillis(recordTime);
        calendar2.setTimeInMillis(record.time);
        // if date of previous record isn't the same as current one - show date in record view
        if(calendar1.get(Calendar.DAY_OF_YEAR) != calendar2.get(Calendar.DAY_OF_YEAR) ||
                calendar1.get(Calendar.YEAR) != calendar2.get(Calendar.YEAR)) {
            showDate = true;
        }

        // update the view holder with new record
        viewHolder.setModel(record, showDate);

        // save last time
        recordTime = record.time;
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

    void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        this.outerOnLongClickListener = onLongClickListener;
    }

    // Returns checked items container
    IdentifiersContainer getCheckedItems() {
        return checkedItems;
    }

    // Sets all items checked/unchecked
    void setAllItemsChecked(boolean checked) {
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

    // Row on long click listener
    private class RowOnLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View view) {
            return  (outerOnLongClickListener != null &&
                     outerOnLongClickListener.onLongClick(view));
        }
    }

    @Nullable
    JournalRecord getRecord(View view) {
        if(view != null) {
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            return viewHolder.record;
        }
        return null;
    }

    // View holder improves scroll performance
    private class ViewHolder {
        private JournalRecord record;
        private int itemId;
        private ImageView iconImageView;
        private TextView senderTextView;
        private TextView numberTextView;
        private TextView textTextView;
        private TextView dateTextView;
        private TextView timeTextView;
        private CheckBox checkBox;
        private View dateLayout;
        private CheckableLinearLayout contentLayout;

        ViewHolder(View rowView) {
            this((ImageView) rowView.findViewById(R.id.icon),
                    (TextView) rowView.findViewById(R.id.sender),
                    (TextView) rowView.findViewById(R.id.number),
                    (TextView) rowView.findViewById(R.id.text),
                    (TextView) rowView.findViewById(R.id.date),
                    (TextView) rowView.findViewById(R.id.time),
                    (CheckBox) rowView.findViewById(R.id.cb),
                    rowView.findViewById(R.id.date_layout),
                    (CheckableLinearLayout) rowView.findViewById(R.id.content_layout));
        }

        ViewHolder(ImageView iconImageView, TextView senderTextView,
                   TextView numberTextView, TextView textTextView, TextView dateTextView,
                   TextView timeTextView, CheckBox checkBox, View dateLayout,
                   CheckableLinearLayout contentLayout) {
            this.record = null;
            this.itemId = 0;
            this.iconImageView = iconImageView;
            this.senderTextView = senderTextView;
            this.numberTextView = numberTextView;
            this.textTextView = textTextView;
            this.dateTextView = dateTextView;
            this.timeTextView = timeTextView;
            this.checkBox = checkBox;
            this.dateLayout = dateLayout;
            this.contentLayout = contentLayout;
            contentLayout.setTag(this);

            // add on click listeners
            contentLayout.setOnClickListener(rowOnClickListener);
            contentLayout.setOnLongClickListener(rowOnLongClickListener);
        }

        private void setModel(JournalRecord record, boolean showDate) {
            this.record = record;
            itemId = (int) record.id;
            Date date = toDate(record.time);
            if(showDate) {
                dateTextView.setText(dateFormat.format(date));
                dateLayout.setVisibility(View.VISIBLE);
            } else {
                dateLayout.setVisibility(View.GONE);
            }
            timeTextView.setText(timeFormat.format(date));

            senderTextView.setText(record.caller);

            if(record.number != null &&
                    !record.caller.equals(record.number)) {
                numberTextView.setText(record.number);
                numberTextView.setVisibility(View.VISIBLE);
            } else {
                numberTextView.setText("");
                numberTextView.setVisibility(View.GONE);
            }

            if (record.text != null) {
                iconImageView.setImageResource(android.R.drawable.sym_action_email);
                textTextView.setText(record.text);
                textTextView.setVisibility(View.VISIBLE);
            } else {
                iconImageView.setImageResource(android.R.drawable.sym_action_call);
                textTextView.setText("");
                textTextView.setVisibility(View.GONE);
            }

            boolean checked = isChecked();
            checkBox.setChecked(checked);
            contentLayout.setChecked(checked);
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
            contentLayout.setChecked(checked);
        }

        private Date toDate(long time) {
            datetime.setTime(time);
            return datetime;
        }
    }
}
