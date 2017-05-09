/*
 * Copyright 2017 Anton Kaliturin
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

package com.kaliturin.blacklist.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.kaliturin.blacklist.R;
import com.kaliturin.blacklist.utils.CheckableLinearLayout;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.JournalRecord;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.JournalRecordCursorWrapper;
import com.kaliturin.blacklist.utils.IdentifiersContainer;
import com.kaliturin.blacklist.utils.Settings;
import com.kaliturin.blacklist.utils.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Journal data cursor adapter
 */
public class JournalCursorAdapter extends CursorAdapter {
    private final DateFormat timeFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
    private final SimpleDateFormat dateFormat = (SimpleDateFormat) SimpleDateFormat.getDateInstance(DateFormat.LONG);
    private final DateFormat yearLessDateFormat;
    private final Date datetime = new Date();
    private final Calendar calendar = Calendar.getInstance();
    private final SparseBooleanArray unfoldedTextItems = new SparseBooleanArray();
    private IdentifiersContainer checkedItems = new IdentifiersContainer(0);
    private View.OnClickListener outerOnClickListener = null;
    private View.OnLongClickListener outerOnLongClickListener = null;
    private final OnClickListener onClickListener = new OnClickListener();
    private final OnLongClickListener onLongClickListener = new OnLongClickListener();
    private boolean foldSMSText = false;
    private final int currentYear;

    public JournalCursorAdapter(Context context) {
        super(context, null, 0);
        foldSMSText = Settings.getBooleanValue(context, Settings.FOLD_SMS_TEXT_IN_JOURNAL);

        // creating year less date format
        String fullPattern = dateFormat.toPattern();
        // checking 'de' we omit problems with Spain locale
        String regex = fullPattern.contains("de") ? "[^Mm]*[Yy]+[^Mm]*" : "[^DdMm]*[Yy]+[^DdMm]*";
        String yearLessPattern = fullPattern.replaceAll(regex, "");
        yearLessDateFormat = new SimpleDateFormat(yearLessPattern);

        calendar.setTimeInMillis(System.currentTimeMillis());
        currentYear = calendar.get(Calendar.YEAR);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_journal, parent, false);

        // link view to holder
        view.setTag(new ViewHolder(context, view));

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

        // get previous record time
        long previousRecordTime = cursorWrapper.getTime(cursor.getPosition() - 1);
        // define date format of showing record
        calendar.setTimeInMillis(previousRecordTime);
        int lastRecordDay = calendar.get(Calendar.DAY_OF_YEAR);
        int lastRecordYear = calendar.get(Calendar.YEAR);
        calendar.setTimeInMillis(record.time);
        int currentRecordDay = calendar.get(Calendar.DAY_OF_YEAR);
        int currentRecordYear = calendar.get(Calendar.YEAR);
        DateFormat df = null;
        // if date of the previous record isn't the same as the current one - show date
        if (lastRecordDay != currentRecordDay || lastRecordYear != currentRecordYear) {
            // if current year - do not show it
            if (currentRecordYear == currentYear) {
                df = yearLessDateFormat;
            } else {
                df = dateFormat;
            }
        }

        // update the view holder with the new record
        viewHolder.setModel(record, df);
    }

    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
        // rebuild checked items container
        int size = (cursor != null ? cursor.getCount() : 0);
        checkedItems = new IdentifiersContainer(size);
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.outerOnClickListener = onClickListener;
    }

    public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        this.outerOnLongClickListener = onLongClickListener;
    }

    // Returns checked items container
    public IdentifiersContainer getCheckedItems() {
        return checkedItems;
    }

    // Sets all items checked/unchecked
    public void setAllItemsChecked(boolean checked) {
        if (checkedItems.setAll(checked)) {
            notifyDataSetChanged();
        }
    }

    // Returns true if there are some checked items
    public boolean hasCheckedItems() {
        return !checkedItems.isEmpty();
    }

    // Row on click listener
    private class OnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            viewHolder.toggle();
            if (outerOnClickListener != null) {
                outerOnClickListener.onClick(view);
            }
        }
    }

    // Row on long click listener
    private class OnLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View view) {
            return (outerOnLongClickListener != null &&
                    outerOnLongClickListener.onLongClick(view));
        }
    }

    // Returns record linked to the passed view if it is available
    public
    @Nullable
    JournalRecord getRecord(View view) {
        ViewHolder viewHolder = null;
        if (view != null) {
            viewHolder = (ViewHolder) view.getTag();
        }
        return (viewHolder == null ? null : viewHolder.record);
    }

    // View holder improves scroll performance
    private class ViewHolder {
        private JournalRecord record;
        private int itemId;
        private ImageView iconImageView;
        private TextView senderTextView;
        private TextView textTextView;
        private TextView dateTextView;
        private TextView timeTextView;
        private CheckBox checkBox;
        private View dateLayout;
        private CheckableLinearLayout contentLayout;

        ViewHolder(Context context, View rowView) {
            this(context, (ImageView) rowView.findViewById(R.id.icon),
                    (TextView) rowView.findViewById(R.id.sender),
                    (TextView) rowView.findViewById(R.id.text),
                    (TextView) rowView.findViewById(R.id.date),
                    (TextView) rowView.findViewById(R.id.time),
                    (CheckBox) rowView.findViewById(R.id.cb),
                    rowView.findViewById(R.id.date_layout),
                    (CheckableLinearLayout) rowView.findViewById(R.id.content_layout));
        }

        ViewHolder(Context context, ImageView iconImageView, TextView senderTextView,
                   TextView textTextView, TextView dateTextView,
                   TextView timeTextView, CheckBox checkBox, View dateLayout,
                   CheckableLinearLayout contentLayout) {
            this.record = null;
            this.itemId = 0;
            this.iconImageView = iconImageView;
            this.senderTextView = senderTextView;
            this.textTextView = textTextView;
            this.dateTextView = dateTextView;
            this.timeTextView = timeTextView;
            this.checkBox = checkBox;
            this.dateLayout = dateLayout;
            this.contentLayout = contentLayout;

            Utils.scaleViewOnTablet(context, checkBox, R.dimen.iconScale);
            Utils.scaleViewOnTablet(context, iconImageView, R.dimen.iconScale);

            contentLayout.setTag(this);
            textTextView.setTag(this);

            // add on click listeners
            contentLayout.setOnClickListener(onClickListener);
            contentLayout.setOnLongClickListener(onLongClickListener);
            if (foldSMSText) {
                textTextView.setOnLongClickListener(onLongClickListener);
                textTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setTextUnfolded(!isTextUnfolded());
                    }
                });
            }
        }

        private void setModel(JournalRecord record, DateFormat dateFormat) {
            this.record = record;
            itemId = (int) record.id;
            Date date = toDate(record.time);
            if (dateFormat != null) {
                dateTextView.setText(dateFormat.format(date));
                dateLayout.setVisibility(View.VISIBLE);
            } else {
                dateLayout.setVisibility(View.GONE);
            }
            timeTextView.setText(timeFormat.format(date));

            String sender = record.caller;
            if (record.number != null &&
                    !record.caller.equals(record.number)) {
                sender += " (" + record.number + ")";
            }
            senderTextView.setText(sender);

            if (record.text != null) {
                iconImageView.setImageResource(android.R.drawable.sym_action_email);
                textTextView.setText(record.text);
                textTextView.setVisibility(View.VISIBLE);
                if (foldSMSText) {
                    setTextUnfolded(isTextUnfolded());
                }
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

        private boolean isTextUnfolded() {
            return unfoldedTextItems.get(itemId);
        }

        private void setTextUnfolded(boolean unfolded) {
            if (unfolded) {
                textTextView.setSingleLine(false);
                textTextView.setMaxLines(Integer.MAX_VALUE);
                textTextView.setEllipsize(null);
                unfoldedTextItems.append(itemId, true);

            } else {
                textTextView.setSingleLine(true);
                textTextView.setMaxLines(1);
                textTextView.setEllipsize(TextUtils.TruncateAt.END);
                unfoldedTextItems.delete(itemId);
            }
        }

        private Date toDate(long time) {
            datetime.setTime(time);
            return datetime;
        }
    }
}
