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
    private final DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.SHORT);
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

        // add view holder to the row
        view.setTag(new ViewHolder(view));

        // on click listeners for the row and checkbox (which is inside the row)
        view.setOnClickListener(rowOnClickListener);
        view.setOnLongClickListener(rowOnLongClickListener);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // get cursor wrapper
        JournalRecordCursorWrapper cursorWrapper = (JournalRecordCursorWrapper) cursor;
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

        // update the view holder with new model
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
    JournalRecord getRecord(View row) {
        if(row != null) {
            ViewHolder viewHolder = (ViewHolder) row.getTag();
            return viewHolder.model;
        }
        return null;
    }

    // View holder improves scroll performance
    private class ViewHolder {
        private JournalRecord model;
        private int itemId;
        private CheckableLinearLayout rowView;
        private ImageView iconImageView;
        private TextView senderTextView;
        private TextView numberTextView;
        private TextView textTextView;
        private TextView dateTextView;
        private TextView timeTextView;
        private CheckBox checkBox;
        private View dateLayout;

        ViewHolder(View rowView) {
            this((CheckableLinearLayout) rowView,
                    (ImageView) rowView.findViewById(R.id.icon),
                    (TextView) rowView.findViewById(R.id.sender),
                    (TextView) rowView.findViewById(R.id.number),
                    (TextView) rowView.findViewById(R.id.text),
                    (TextView) rowView.findViewById(R.id.date),
                    (TextView) rowView.findViewById(R.id.time),
                    (CheckBox) rowView.findViewById(R.id.cb),
                    rowView.findViewById(R.id.date_layout));
        }

        ViewHolder(CheckableLinearLayout rowView, ImageView iconImageView, TextView senderTextView,
                   TextView numberTextView, TextView textTextView, TextView dateTextView,
                   TextView timeTextView, CheckBox checkBox, View dateLayout) {
            this.model = null;
            this.rowView = rowView;
            this.itemId = 0;
            this.iconImageView = iconImageView;
            this.senderTextView = senderTextView;
            this.numberTextView = numberTextView;
            this.textTextView = textTextView;
            this.dateTextView = dateTextView;
            this.timeTextView = timeTextView;
            this.checkBox = checkBox;
            this.dateLayout = dateLayout;
        }

        private void setModel(JournalRecord model, boolean showDate) {
            this.model = model;
            itemId = (int) model.id;
            Date date = toDate(model.time);
            if(showDate) {
                dateTextView.setText(dateFormat.format(date));
                dateLayout.setVisibility(View.VISIBLE);
            } else {
                dateLayout.setVisibility(View.GONE);
            }
            timeTextView.setText(timeFormat.format(date));

            senderTextView.setText(model.caller);

            if(model.number != null &&
                    !model.caller.equals(model.number)) {
                numberTextView.setText(model.number);
                numberTextView.setVisibility(View.VISIBLE);
            } else {
                numberTextView.setText("");
                numberTextView.setVisibility(View.GONE);
            }

            if (model.text != null) {
                iconImageView.setImageResource(android.R.drawable.sym_action_email);
                textTextView.setText(model.text);
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
