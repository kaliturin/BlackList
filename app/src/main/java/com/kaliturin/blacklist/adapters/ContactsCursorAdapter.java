package com.kaliturin.blacklist.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.kaliturin.blacklist.R;
import com.kaliturin.blacklist.utils.CheckableLinearLayout;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.Contact;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.ContactNumber;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.ContactSource;
import com.kaliturin.blacklist.utils.IdentifiersContainer;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Contacts' cursor adapter
 */
public class ContactsCursorAdapter extends CursorAdapter {
    private IdentifiersContainer checkedItems = new IdentifiersContainer(0);
    private View.OnClickListener outerOnClickListener = null;
    private View.OnLongClickListener outerOnLongClickListener = null;
    private RowOnClickListener rowOnClickListener = new RowOnClickListener();
    private RowOnLongClickListener rowOnLongClickListener = new RowOnLongClickListener();

    public ContactsCursorAdapter(Context context) {
        super(context, null, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        if (cursor == null) return null;

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
        if (cursor == null) return;

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
        return checkedItems.getSize() > 0;
    }

    // Returns list of contacts from checked items
    public List<Contact> extractCheckedContacts() {
        List<Contact> list = new LinkedList<>();
        Cursor cursor = getCursor();
        if (cursor != null) {
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
            if (outerOnClickListener != null) {
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

    public boolean isItemChecked(View row) {
        if (row != null) {
            ViewHolder viewHolder = (ViewHolder) row.getTag();
            return viewHolder.isChecked();
        }
        return false;
    }

    @Nullable
    public Contact getContact(View row) {
        if (row != null) {
            ViewHolder viewHolder = (ViewHolder) row.getTag();
            return viewHolder.contact;
        }
        return null;
    }

    // View holder improves scroll performance
    private class ViewHolder {
        private StringBuilder sb = new StringBuilder();

        private Contact contact;
        private int itemId;
        private CheckableLinearLayout rowView;
        private TextView nameTextView;
        private TextView numbersTextView;
        private CheckBox checkBox;

        ViewHolder(View row) {
            this((CheckableLinearLayout) row,
                    (TextView) row.findViewById(R.id.contact_name),
                    (TextView) row.findViewById(R.id.contact_numbers),
                    (CheckBox) row.findViewById(R.id.contact_cb));
        }

        ViewHolder(CheckableLinearLayout rowView, TextView nameTextView, TextView numbersTextView, CheckBox checkBox) {
            this.contact = null;
            this.itemId = 0;
            this.rowView = rowView;
            this.nameTextView = nameTextView;
            this.numbersTextView = numbersTextView;
            this.checkBox = checkBox;
        }

        private void setModel(Context context, Contact contact) {
            this.contact = contact;

            itemId = (int) contact.id;
            boolean oneNumberEquals = false;

            // show contact name
            String name = contact.name;
            if (contact.numbers.size() == 1) {
                ContactNumber number = contact.numbers.get(0);
                if (name.equals(number.number)) {
                    // there is just 1 number and it equals to the contact name
                    // add number type title before the contact name
                    name = getNumberTypeTitle(context, number.type) + name;
                    oneNumberEquals = true;
                }
            }
            nameTextView.setText(name);

            // show contact numbers
            sb.setLength(0);
            if (!oneNumberEquals) {
                String[] titles = getSortedNumberTitles(context, contact.numbers);
                String separator = (titles.length > 5 ? ", " : "\n");
                for (int i = 0; i < titles.length; i++) {
                    sb.append(titles[i]);
                    if (i < titles.length - 1) {
                        sb.append(separator);
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

    private String[] getSortedNumberTitles(Context context, List<ContactNumber> numbers) {
        Set<String> titles = new TreeSet<>();
        for (ContactNumber number : numbers) {
            titles.add(getNumberTypeTitle(context, number.type) + number.number);
        }
        return titles.toArray(new String[titles.size()]);
    }

    private String getNumberTypeTitle(Context context, int type) {
        switch (type) {
            case ContactNumber.TYPE_STARTS:
                return context.getString(R.string.Starts_with) + " ";
            case ContactNumber.TYPE_ENDS:
                return context.getString(R.string.Ends_with) + " ";
            case ContactNumber.TYPE_CONTAINS:
                return context.getString(R.string.Contains) + " ";
        }
        return "";
    }
}
