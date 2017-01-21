package com.kaliturin.blacklist;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.kaliturin.blacklist.DatabaseAccessHelper.Contact;
import com.kaliturin.blacklist.DatabaseAccessHelper.ContactSource;

/**
 * Contacts list access helper
 */
public class ContactsAccessHelper {
    private static final String TAG = "BlackList";
    private static ContactsAccessHelper sInstance = null;
    private ContentResolver contentResolver = null;

    private ContactsAccessHelper(Context context) {
        contentResolver = context.getContentResolver();
    }

    public static synchronized ContactsAccessHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ContactsAccessHelper(context);
        }
        return sInstance;
    }

    private boolean validate(Cursor cursor) {
        if(cursor == null || cursor.isClosed()) return false;
        if(cursor.getCount() == 0) {
            cursor.close();
            return false;
        }
        return true;
    }

    // Returns contacts from specified source
    public @Nullable Cursor getContacts(ContactSourceType sourceType, @Nullable String filter) {
        switch (sourceType) {
            case FROM_CALLS_LOG:
                return getContactsFromCallsLog(filter);
            case FROM_SMS_INBOX:
                return getContactsFromSMSInbox(filter);
        }
        return getContacts(filter);
    }

    // Selects contacts from contacts list
    public @Nullable ContactCursorWrapper getContacts(@Nullable String filter) {
        filter = (filter == null ? "%%" : "%" + filter + "%");
        Cursor cursor = contentResolver.query(
                Contacts.CONTENT_URI,
                new String[] {Contacts._ID, Contacts.DISPLAY_NAME},
                Contacts.IN_VISIBLE_GROUP + " != 0 AND " +
                Contacts.HAS_PHONE_NUMBER + " != 0 AND " +
                Contacts.DISPLAY_NAME + " IS NOT NULL AND " +
                Contacts.DISPLAY_NAME + " LIKE ? ",
                new String[]{filter},
                Contacts.DISPLAY_NAME + " ASC");

        return (validate(cursor) ? new ContactCursorWrapper(cursor) : null);
    }

    // Selects contact by id
    public @Nullable ContactCursorWrapper getContact(long contactId) {
        Cursor cursor = contentResolver.query(
                Contacts.CONTENT_URI,
                new String[] {Contacts._ID, Contacts.DISPLAY_NAME},
                Contacts.DISPLAY_NAME + " IS NOT NULL AND " +
                Contacts.IN_VISIBLE_GROUP + " != 0 AND " +
                Contacts.HAS_PHONE_NUMBER + " != 0 AND " +
                Contacts._ID + " = " + contactId,
                null,
                null);

        return (validate(cursor) ? new ContactCursorWrapper(cursor) : null);
    }

    // Contact's cursor wrapper
    public class ContactCursorWrapper extends CursorWrapper implements ContactSource {
        private final int ID;
        private final int NAME;

        private ContactCursorWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            ID = getColumnIndex(Contacts._ID);
            NAME = getColumnIndex(Contacts.DISPLAY_NAME);
        }

        @Override
        public Contact getContact() {
            return getContact(true);
        }

        public Contact getContact(boolean withNumbers) {
            long id = getLong(ID);
            String name = getString(NAME);
            List<String> numbers = new LinkedList<>();
            if(withNumbers) {
                ContactNumberCursorWrapper cursor = getContactNumbers(id);
                if(cursor != null) {
                    do {
                        numbers.add(cursor.getNumber());
                    } while (cursor.moveToNext());
                    cursor.close();
                }
            }

            return new Contact(id, name, 0, numbers);
        }
    }

    // Contact's number cursor wrapper
    private static class ContactNumberCursorWrapper extends CursorWrapper {
        private final int NORMALIZED_NUMBER;
        private final int NUMBER;

        private ContactNumberCursorWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            NORMALIZED_NUMBER = cursor.getColumnIndex(getNormalizedNumberColumnName());
            NUMBER = cursor.getColumnIndex(Phone.NUMBER);
        }

        String getNumber() {
            String number = getString(NORMALIZED_NUMBER);
            if(number == null) {
                number = getString(NUMBER);
            }
            return number;
        }

        private static String getNormalizedNumberColumnName() {
            final String name;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                name = Phone.NORMALIZED_NUMBER;
            } else {
                name = Phone.DATA4;
            }
            return name;
        }
    }

    // Selects all numbers of specified contact
    private @Nullable ContactNumberCursorWrapper getContactNumbers(long contactId) {
        String NORMALIZED_NUMBER = ContactNumberCursorWrapper.getNormalizedNumberColumnName();
        Cursor cursor = contentResolver.query(
                Phone.CONTENT_URI,
                new String[]{NORMALIZED_NUMBER, Phone.NUMBER},
                Phone.NUMBER + " IS NOT NULL AND " +
                Phone.CONTACT_ID + " = " + contactId,
                null,
                null);

        return (validate(cursor) ? new ContactNumberCursorWrapper(cursor) : null);
    }

    // Returns true if passed number contains in contacts
    public boolean containsNumberInContacts(@NonNull String number) {
        String NORMALIZED_NUMBER = ContactNumberCursorWrapper.getNormalizedNumberColumnName();
        Cursor cursor = contentResolver.query(
                Phone.CONTENT_URI,
                new String[]{NORMALIZED_NUMBER, Phone.NUMBER},
                NORMALIZED_NUMBER + " = ? OR " +
                Phone.NUMBER + " = ? ",
                new String[]{number, number},
                null);

        if(validate(cursor)) {
            cursor.close();
            return true;
        }

        return false;
    }

    // Returns true if passed number contains in SMS inbox
    public boolean containsNumberInSMSInbox(@NonNull String number) {
        final String ID = "_id";
        final String ADDRESS = "address";
        final String PERSON = "person";

        Cursor cursor = contentResolver.query(
                Uri.parse("content://sms/inbox"),
                new String[]{"DISTINCT " + ID, ADDRESS, PERSON},
                ADDRESS + " = ? ) GROUP BY (" + ADDRESS,
                new String[]{number},
                Calls.DATE + " DESC");

        if(validate(cursor)) {
            cursor.close();
            return true;
        }

        return false;
    }

    // Selects contacts from SMS inbox filtering by contact name or number
    public @Nullable ContactFromSMSCursorWrapper getContactsFromSMSInbox(@Nullable String filter) {
        filter = (filter == null ? "" : filter.toLowerCase());
        final String ID = "_id";
        final String ADDRESS = "address"; // number
        final String PERSON = "person"; // contact id

        // filter by address (number) if person (contact id) is null
        Cursor cursor = contentResolver.query(
                Uri.parse("content://sms/inbox"),
                new String[]{"DISTINCT " + ID, ADDRESS, PERSON},
                ADDRESS + " IS NOT NULL AND (" +
                PERSON + " IS NOT NULL OR " +
                ADDRESS + " LIKE ? )" +
                ") GROUP BY (" + ADDRESS,
                new String[]{"%" + filter + "%"},
                Calls.DATE + " DESC");

        // now we need to filter contacts by names and fill matrix cursor
        if(cursor != null &&
                cursor.moveToFirst()) {
            MatrixCursor matrixCursor = new MatrixCursor(new String[]{ID, ADDRESS, PERSON});
            final int _ID = cursor.getColumnIndex(ID);
            final int _ADDRESS = cursor.getColumnIndex(ADDRESS);
            final int _PERSON = cursor.getColumnIndex(PERSON);
            do {
                String id = cursor.getString(_ID);
                String address = cursor.getString(_ADDRESS);
                String person = address;
                if(cursor.isNull(_PERSON)) {
                    matrixCursor.addRow(new String[] {id, address, person});
                } else {
                    // get person name from contacts
                    long contactId = cursor.getLong(_PERSON);
                    ContactCursorWrapper contactCursor = getContact(contactId);
                    if(contactCursor != null) {
                        Contact contact = contactCursor.getContact(false);
                        contactCursor.close();
                        person = contact.name;
                    }
                    // filter contact
                    if(person.toLowerCase().contains(filter)) {
                        matrixCursor.addRow(new String[] {id, address, person});
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
            cursor = matrixCursor;
        }

        return (validate(cursor) ? new ContactFromSMSCursorWrapper(cursor) : null);
    }

    // Contact from SMS cursor wrapper
    public class ContactFromSMSCursorWrapper extends CursorWrapper implements ContactSource {
        private final int ID;
        private final int ADDRESS;
        private final int PERSON;

        private ContactFromSMSCursorWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            ID = getColumnIndex("_id");
            ADDRESS = getColumnIndex("address");
            PERSON = getColumnIndex("person");
        }

        @Override
        public Contact getContact() {
            long id = getLong(ID);
            String name = getString(PERSON);
            String number = getString(ADDRESS);
            List<String> numbers = new LinkedList<>();
            numbers.add(number);

            return new Contact(id, name, 0, numbers);
        }
    }

    // Selects contacts from calls log
    public @Nullable ContactFromCallsCursorWrapper getContactsFromCallsLog(@Nullable String filter) {
        filter = (filter == null ? "%%" : "%" + filter + "%");
        Cursor cursor = null;
        // This try/catch is required by IDE because we use Calls.CONTENT_URI
        try {
            // filter by name or by number
            cursor = contentResolver.query(
                    Calls.CONTENT_URI,
                    new String[] {Calls._ID, Calls.NUMBER, Calls.CACHED_NAME},
                    Calls.NUMBER + " IS NOT NULL AND (" +
                    Calls.CACHED_NAME + " IS NULL AND " +
                    Calls.NUMBER + " LIKE ? OR " +
                    Calls.CACHED_NAME + " LIKE ? )",
                    new String[]{filter, filter},
                    Calls.DATE + " DESC");
        } catch (SecurityException e) {
            Log.w(TAG, e);
        }

        if(cursor != null &&
                cursor.moveToFirst()) {
            // Because we cannot query distinct calls - we have queried all.
            // And now we must get rid of repeated calls with help of tree set and matrix cursor.
            MatrixCursor matrixCursor = new MatrixCursor(
                    new String[]{Calls._ID, Calls.NUMBER, Calls.CACHED_NAME});
            final int ID = cursor.getColumnIndex(Calls._ID);
            final int NUMBER = cursor.getColumnIndex(Calls.NUMBER);
            final int NAME = cursor.getColumnIndex(Calls.CACHED_NAME);
            Set<String> set = new TreeSet<>();
            do {
                String number = cursor.getString(NUMBER);
                String name = cursor.getString(NAME);
                String key = number + (name == null ? "" : name);
                if(set.add(key)) {
                    String id = cursor.getString(ID);
                    matrixCursor.addRow(new String[] {id, number, name});
                }
            } while (cursor.moveToNext());
            cursor.close();
            cursor = matrixCursor;
        }

        return (validate(cursor) ? new ContactFromCallsCursorWrapper(cursor) : null);
    }

    // Contact from calls cursor wrapper
    public class ContactFromCallsCursorWrapper extends  CursorWrapper implements ContactSource {
        private final int ID;
        private final int NUMBER;
        private final int NAME;

        private ContactFromCallsCursorWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            ID = cursor.getColumnIndex(Calls._ID);
            NUMBER = cursor.getColumnIndex(Calls.NUMBER);
            NAME = cursor.getColumnIndex(Calls.CACHED_NAME);
        }

        @Override
        public Contact getContact() {
            long id = getLong(ID);
            String number = getString(NUMBER);
            String name = getString(NAME);
            List<String> numbers = new LinkedList<>();
            numbers.add(number);
            if(name == null) {
                name = number;
            }

            return new Contact(id, name, 0, numbers);
        }
    }

    // Types of the contact sources
    public enum ContactSourceType {
        FROM_CONTACTS,
        FROM_CALLS_LOG,
        FROM_SMS_INBOX
    }

    static void debug(Cursor cursor) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String s = cursor.getString(i);
            String n = cursor.getColumnName(i);
            sb.append("[" + n + "]=" + s);
        }
        Log.d("BlackList", sb.toString());
    }
}
