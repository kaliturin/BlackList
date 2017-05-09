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
import android.support.v4.util.LongSparseArray;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kaliturin.blacklist.R;
import com.kaliturin.blacklist.utils.ContactsAccessHelper.SMSConversation;
import com.kaliturin.blacklist.utils.ContactsAccessHelper.SMSConversationWrapper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Cursor adapter for all SMS conversations
 */
public class SMSConversationsListCursorAdapter extends CursorAdapter {
    private final DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);
    private Date datetime = new Date();
    private View.OnClickListener outerOnClickListener = null;
    private View.OnLongClickListener outerOnLongClickListener = null;
    private RowOnClickListener rowOnClickListener = new RowOnClickListener();
    private RowOnLongClickListener rowOnLongClickListener = new RowOnLongClickListener();
    private LongSparseArray<SMSConversation> smsConversationCache = new LongSparseArray<>();

    public SMSConversationsListCursorAdapter(Context context) {
        super(context, null, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_sms_conversations_list, parent, false);

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
        // try to get a model from the cache
        long itemId = getItemId(cursor.getPosition());
        SMSConversation model = smsConversationCache.get(itemId);
        if (model == null) {
            // get cursor wrapper
            SMSConversationWrapper cursorWrapper = (SMSConversationWrapper) cursor;
            // get model
            model = cursorWrapper.getConversation(context);
            // put it to the cache
            smsConversationCache.put(itemId, model);
        }
        // get view holder from the row
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        // update the view holder with new model
        viewHolder.setModel(model);
    }

    @Override
    public void changeCursor(Cursor cursor) {
        invalidateCache();
        super.changeCursor(cursor);
    }

//---------------------------------------------------------------------------------

    // Clears the cache
    private void invalidateCache() {
        smsConversationCache.clear();
    }

    // Removes particular item from the cache (if exists)
    public boolean invalidateCache(int itemId) {
        if (smsConversationCache.get(itemId) == null) {
            return false;
        }
        smsConversationCache.remove(itemId);
        return true;
    }

    // Returns sms conversation by passed row
    @Nullable
    public SMSConversation getSMSConversation(View row) {
        if (row != null) {
            ViewHolder holder = (ViewHolder) row.getTag();
            return holder.model;
        }
        return null;
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.outerOnClickListener = onClickListener;
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

    // Row on click listener
    private class RowOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (outerOnClickListener != null) {
                outerOnClickListener.onClick(view);
            }
        }
    }

    // Holder of the view data
    private class ViewHolder {
        private SMSConversation model;
        private View rowView;
        private TextView addressTextView;
        private TextView snippetTextView;
        private TextView dateTextView;
        private TextView unreadTextView;

        ViewHolder(View rowView) {
            this(rowView,
                    (TextView) rowView.findViewById(R.id.address),
                    (TextView) rowView.findViewById(R.id.snippet),
                    (TextView) rowView.findViewById(R.id.date),
                    (TextView) rowView.findViewById(R.id.unread_sms));
        }

        ViewHolder(View rowView,
                   TextView addressTextView,
                   TextView snippetTextView,
                   TextView dateTextView,
                   TextView unreadTextView) {
            this.model = null;
            this.rowView = rowView;
            this.addressTextView = addressTextView;
            this.snippetTextView = snippetTextView;
            this.dateTextView = dateTextView;
            this.unreadTextView = unreadTextView;
        }

        void setModel(@Nullable SMSConversation model) {
            this.model = model;
            if (model == null) {
                rowView.setVisibility(View.GONE);
                return;
            }
            rowView.setVisibility(View.VISIBLE);
            String address;
            if (model.person != null) {
                address = model.person + " (" + model.number + ")";
            } else {
                address = model.number;
            }
            addressTextView.setText(address);
            snippetTextView.setText(model.snippet);
            dateTextView.setText(dateFormat.format(toDate(model.date)));

            if (model.unread > 0) {
                unreadTextView.setText(String.valueOf(model.unread));
                unreadTextView.setVisibility(View.VISIBLE);
            } else {
                unreadTextView.setVisibility(View.GONE);
            }
        }

        private Date toDate(long time) {
            datetime.setTime(time);
            return datetime;
        }
    }
}
