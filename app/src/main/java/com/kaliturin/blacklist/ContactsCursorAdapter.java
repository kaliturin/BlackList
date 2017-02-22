package com.kaliturin.blacklist;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import com.kaliturin.blacklist.DatabaseAccessHelper.ContactSource;
import com.kaliturin.blacklist.DatabaseAccessHelper.Contact;
import com.kaliturin.blacklist.DatabaseAccessHelper.ContactNumber;

import java.util.LinkedList;
import java.util.List;

/**
 * Contacts' cursor adapter
 */
class ContactsCursorAdapter extends CursorAdapter {
    private IdentifiersContainer checkedItems = new IdentifiersContainer(0);
    private View.OnClickListener outerOnClickListener = null;
    private View.OnLongClickListener outerOnLongClickListener = null;
    private RowOnClickListener rowOnClickListener = new RowOnClickListener();
    private RowOnLongClickListener rowOnLongClickListener = new RowOnLongClickListener();

    ContactsCursorAdapter(Context context) {
        super(context, null, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        if(cursor == null) return null;

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_contacts, parent, false);

        // view holder for the new row
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
        if(cursor == null) return;

        // get contact
        ContactSource contactSource = (ContactSource) cursor;
        Contact contact = contactSource.getContact();
        // get view holder from the row
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        // update the view holder with the new contact
        viewHolder.setModel(context, contact);
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
        return checkedItems.getSize() > 0;
    }

    // Returns list of contacts from checked items
    List<Contact> extractCheckedContacts() {
        List<Contact> list = new LinkedList<>();
        Cursor cursor = getCursor();
        if(cursor != null) {
            int position = cursor.getPosition();
            cursor.moveToFirst();
            do {
                Contact contact = ((ContactSource) cursor).getContact();
                if (checkedItems.contains((int) contact.id)) {
                    list.add(contact);
                }
            } while (cursor.moveToNext());
            cursor.moveToPosition(position);
        }
        return list;
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
            return (outerOnLongClickListener != null &&
                    outerOnLongClickListener.onLongClick(view));
        }
    }

    @Nullable
    Contact getContact(View row) {
        if(row != null) {
            ViewHolder viewHolder = (ViewHolder) row.getTag();
            return viewHolder.model;
        }
        return null;
    }

    // View holder improves scroll performance
    private class ViewHolder {
        private StringBuilder sb = new StringBuilder();

        private Contact model;
        private int itemId;
        private CheckableLinearLayout rowView;
        private TextView nameTextView;
        private TextView numbersTextView;
        private CheckBox checkBox;

        ViewHolder(View row) {
            this((CheckableLinearLayout)row,
                    (TextView) row.findViewById(R.id.contact_name),
                    (TextView) row.findViewById(R.id.contact_numbers),
                    (CheckBox) row.findViewById(R.id.contact_cb));
        }

        ViewHolder(CheckableLinearLayout rowView, TextView nameTextView, TextView numbersTextView, CheckBox checkBox) {
            this.model = null;
            this.itemId = 0;
            this.rowView = rowView;
            this.nameTextView = nameTextView;
            this.numbersTextView = numbersTextView;
            this.checkBox = checkBox;
        }

        private void setModel(Context context, Contact model) {
            this.model = model;

            itemId = (int) model.id;
            boolean oneNumberEquals = false;

            // show contact name
            String name = model.name;
            final int size = model.numbers.size();
            if (size == 1) {
                ContactNumber number = model.numbers.get(0);
                if (model.name.equals(number.number)) {
                    // there is just 1 number and it equals to the contact name
                    // add number type title before the contact name
                    name = getNumberTypeTitle(context, number.type) + " " + name;
                    oneNumberEquals = true;
                }
            }
            nameTextView.setText(name);

            // show contact numbers
            sb.setLength(0);
            if(!oneNumberEquals) {
                for (int i = 0; i < size; i++) {
                    ContactNumber number = model.numbers.get(i);
                    sb.append(getNumberTypeTitle(context, number.type));
                    sb.append(" ");
                    sb.append(number.number);
                    if (i < size - 1) {
                        sb.append("\n");
                    }
                }
            }
            numbersTextView.setText(sb.toString());
            if (numbersTextView.getText().length() == 0) {
                numbersTextView.setVisibility(View.GONE);
            } else {
                numbersTextView.setVisibility(View.VISIBLE);
            }

            // set selection
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
    }

    private String getNumberTypeTitle(Context context, int type) {
        switch (type) {
            case ContactNumber.TYPE_STARTS:
                return context.getString(R.string.starts_with);
            case ContactNumber.TYPE_ENDS:
                return context.getString(R.string.ends_with);
            case ContactNumber.TYPE_CONTAINS:
                return context.getString(R.string.contains);
        }
        return "";
    }
}
