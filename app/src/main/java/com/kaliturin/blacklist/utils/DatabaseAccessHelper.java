/*
 * Copyright (C) 2017 Anton Kaliturin <kaliturin@gmail.com>
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

package com.kaliturin.blacklist.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


/**
 * Database access helper
 */
public class DatabaseAccessHelper extends SQLiteOpenHelper {
    private static final String TAG = DatabaseAccessHelper.class.getName();
    public static final String DATABASE_NAME = "blacklist.db";
    private static final int DATABASE_VERSION = 1;
    private static volatile DatabaseAccessHelper sInstance = null;

    @Nullable
    public static DatabaseAccessHelper getInstance(Context context) {
        if (sInstance == null) {
            synchronized (DatabaseAccessHelper.class) {
                if (sInstance == null) {
                    if (Permissions.isGranted(context, Permissions.WRITE_EXTERNAL_STORAGE)) {
                        sInstance = new DatabaseAccessHelper(context.getApplicationContext());
                    }
                }
            }
        }
        return sInstance;
    }

    public static void invalidateCache() {
        if (sInstance != null) {
            synchronized (DatabaseAccessHelper.class) {
                if (sInstance != null) {
                    sInstance.close();
                    sInstance = null;
                }
            }
        }
    }

    private DatabaseAccessHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        // helper won't create the database file until we first open it
        SQLiteDatabase db = getWritableDatabase();
        // onConfigure isn't calling in android 2.3
        db.execSQL("PRAGMA foreign_keys=ON");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(JournalTable.Statement.CREATE);
        db.execSQL(ContactTable.Statement.CREATE);
        db.execSQL(ContactNumberTable.Statement.CREATE);
        db.execSQL(SettingsTable.Statement.CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        if (i != i1) {
            db.execSQL("DROP TABLE IF EXISTS " + SettingsTable.NAME);
            db.execSQL("DROP TABLE IF EXISTS " + ContactNumberTable.NAME);
            db.execSQL("DROP TABLE IF EXISTS " + ContactTable.NAME);
            db.execSQL("DROP TABLE IF EXISTS " + JournalTable.NAME);
            onCreate(db);
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.execSQL("PRAGMA foreign_keys=ON");
    }

//----------------------------------------------------------------

    // Checks whether file is SQLite file
    public static boolean isSQLiteFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists() || !file.canRead()) {
            return false;
        }

        FileReader reader = null;
        try {
            reader = new FileReader(file);
            char[] buffer = new char[16];
            if (reader.read(buffer, 0, 16) > 0) {
                String str = String.valueOf(buffer);
                return str.equals("SQLite format 3\u0000");
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        } finally {
            Utils.close(reader);
        }
        return false;
    }

    // Closes cursor if it is empty and returns false
    private boolean validate(Cursor cursor) {
        if (cursor == null || cursor.isClosed()) return false;
        if (cursor.getCount() == 0) {
            cursor.close();
            return false;
        }
        return true;
    }

    // Common statements
    private static class Common {
        /**
         * Creates 'IN part' of 'WHERE' clause.
         * If "all" is true - includes all items, except of specified in list.
         * Else includes all items specified in list.
         */
        @Nullable
        static String getInClause(String column, boolean all, List<String> items) {
            if (all) {
                if (items.isEmpty()) {
                    // include all items
                    return null;
                } else {
                    // include all items except of specified
                    String args = joinStrings(items, ", ");
                    return column + " NOT IN ( " + args + " ) ";
                }
            }
            // include all specified items
            String args = joinStrings(items, ", ");
            return column + " IN ( " + args + " ) ";
        }

        /**
         * Creates 'LIKE part' of 'WHERE' clause
         */

        @Nullable
        static String getLikeClause(String column, String filter) {
            return (filter == null ? null :
                    column + " LIKE '%" + filter + "%' ");
        }

        /**
         * Concatenates passed clauses with 'AND' operator
         */
        static String concatClauses(String[] clauses) {
            StringBuilder sb = new StringBuilder();
            for (String clause : clauses) {
                if (TextUtils.isEmpty(clause)) continue;
                if (sb.length() > 0) sb.append(" AND ");
                sb.append(clause);
            }
            return sb.toString();
        }

        static String joinStrings(List<String> list, String separator) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String item : list) {
                if (first)
                    first = false;
                else
                    sb.append(separator);

                sb.append(item);
            }
            return sb.toString();
        }
    }

    // Journal table scheme
    private static class JournalTable {
        static final String NAME = "journal";

        static class Column {
            static final String ID = "_id";
            static final String TIME = "time";
            static final String CALLER = "caller";
            static final String NUMBER = "number";
            static final String TEXT = "text";
        }

        static class Statement {
            static final String CREATE =
                    "CREATE TABLE " + JournalTable.NAME +
                            "(" +
                            Column.ID + " INTEGER PRIMARY KEY NOT NULL, " +
                            Column.TIME + " INTEGER NOT NULL, " +
                            Column.CALLER + " TEXT NOT NULL, " +
                            Column.NUMBER + " TEXT, " +
                            Column.TEXT + " TEXT " +
                            ")";

            static final String SELECT_FIRST_PART =
                    "SELECT " +
                            Column.ID + ", " +
                            Column.TIME +
                            " FROM " + JournalTable.NAME +
                            " ORDER BY " + Column.TIME +
                            " DESC";

            static final String SELECT_LAST_PART_BY_ID =
                    "SELECT " +
                            Column.CALLER + ", " +
                            Column.NUMBER + ", " +
                            Column.TEXT +
                            " FROM " + JournalTable.NAME +
                            " WHERE _id = ? ";

            static final String SELECT_FIRST_PART_BY_FILTER =
                    "SELECT * " +
                            " FROM " + JournalTable.NAME +
                            " WHERE " + Column.CALLER + " LIKE ? " +
                            " OR " + Column.TEXT + " LIKE ? " +
                            " ORDER BY " + Column.TIME +
                            " DESC";
        }
    }

    // Journal table record
    public static class JournalRecord {
        public final long id;
        public final long time;
        public final String caller;
        public final String number;
        public final String text;

        JournalRecord(long id, long time, @NonNull String caller,
                      String number, String text) {
            this.id = id;
            this.time = time;
            this.caller = caller;
            this.number = number;
            this.text = text;
        }
    }

    // Journal record cursor wrapper
    public class JournalRecordCursorWrapper extends CursorWrapper {
        private final int ID;
        private final int TIME;

        JournalRecordCursorWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            ID = cursor.getColumnIndex(JournalTable.Column.ID);
            TIME = cursor.getColumnIndex(JournalTable.Column.TIME);
        }

        public JournalRecord getJournalRecord() {
            long id = getLong(ID);
            long time = getLong(TIME);
            String[] parts = getJournalRecordPartsById(id);
            return new JournalRecord(id, time, parts[0], parts[1], parts[2]);
        }

        public long getTime(int position) {
            long time = 0;
            if (0 <= position && position < getCount()) {
                int curPosition = getPosition();
                if (moveToPosition(position)) {
                    time = getLong(TIME);
                    moveToPosition(curPosition);
                }
            }
            return time;
        }
    }

    // Selects journal record's parts by id
    private String[] getJournalRecordPartsById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(JournalTable.Statement.SELECT_LAST_PART_BY_ID,
                new String[]{String.valueOf(id)});

        String[] parts;
        if (validate(cursor)) {
            cursor.moveToFirst();
            final int CALLER = cursor.getColumnIndex(JournalTable.Column.CALLER);
            final int NUMBER = cursor.getColumnIndex(JournalTable.Column.NUMBER);
            final int TEXT = cursor.getColumnIndex(JournalTable.Column.TEXT);
            parts = new String[]{
                    cursor.getString(CALLER),
                    cursor.getString(NUMBER),
                    cursor.getString(TEXT)};
            cursor.close();
        } else {
            parts = new String[]{"?", "?", "?"};
        }

        return parts;
    }

    // Selects all journal records
    @Nullable
    private JournalRecordCursorWrapper getJournalRecords() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(JournalTable.Statement.SELECT_FIRST_PART, null);

        return (validate(cursor) ? new JournalRecordCursorWrapper(cursor) : null);
    }

    // Selects journal records filtered with passed filter
    @Nullable
    public JournalRecordCursorWrapper getJournalRecords(@Nullable String filter) {
        if (filter == null) {
            return getJournalRecords();
        }
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(JournalTable.Statement.SELECT_FIRST_PART_BY_FILTER,
                new String[]{"%" + filter + "%", "%" + filter + "%"});

        return (validate(cursor) ? new JournalRecordCursorWrapper(cursor) : null);
    }

    // Deletes all records specified in container and fit to filter
    public int deleteJournalRecords(IdentifiersContainer contactIds, @Nullable String filter) {
        if (contactIds.isEmpty()) return 0;

        boolean all = contactIds.isAll();
        List<String> ids = contactIds.getIdentifiers(new LinkedList<String>());

        // build 'WHERE' clause
        String clause = Common.concatClauses(new String[]{
                Common.getLikeClause(JournalTable.Column.CALLER, filter),
                Common.getInClause(JournalTable.Column.ID, all, ids)
        });

        // delete records
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(JournalTable.NAME, clause, null);
    }

    // Deletes record by specified id
    public boolean deleteJournalRecord(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return (db.delete(JournalTable.NAME, JournalTable.Column.ID + " = " + id, null) > 0);
    }

    // Writes journal record
    public long addJournalRecord(long time, @NonNull String caller,
                                 String number, String text) {
        if (number != null && number.equals(caller)) {
            number = null;
        }
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(JournalTable.Column.TIME, time);
        values.put(JournalTable.Column.CALLER, caller);
        values.put(JournalTable.Column.NUMBER, number);
        values.put(JournalTable.Column.TEXT, text);
        return db.insert(JournalTable.NAME, null, values);
    }

//----------------------------------------------------------------

    // Contact number table scheme
    private static class ContactNumberTable {
        static final String NAME = "number";

        static class Column {
            static final String ID = "_id";
            static final String NUMBER = "number";
            static final String TYPE = "type";
            static final String CONTACT_ID = "contact_id";
        }

        static class Statement {
            static final String CREATE =
                    "CREATE TABLE " + ContactNumberTable.NAME +
                            "(" +
                            Column.ID + " INTEGER PRIMARY KEY NOT NULL, " +
                            Column.NUMBER + " TEXT NOT NULL, " +
                            Column.TYPE + " INTEGER NOT NULL, " +
                            Column.CONTACT_ID + " INTEGER NOT NULL, " +
                            "FOREIGN KEY(" + Column.CONTACT_ID + ") REFERENCES " +
                            ContactTable.NAME + "(" + ContactTable.Column.ID + ")" +
                            " ON DELETE CASCADE " +
                            ")";

            static final String SELECT_BY_CONTACT_ID =
                    "SELECT * " +
                            " FROM " + ContactNumberTable.NAME +
                            " WHERE " + Column.CONTACT_ID + " = ? " +
                            " ORDER BY " + Column.NUMBER +
                            " ASC";

            static final String SELECT_BY_TYPE_AND_NUMBER =
                    "SELECT * " +
                            " FROM " + ContactNumberTable.NAME +
                            " WHERE " + Column.TYPE + " = ? " +
                            " AND " + Column.NUMBER + " = ? ";

            static final String SELECT_BY_NUMBER =
                    "SELECT * " +
                            " FROM " + ContactNumberTable.NAME +
                            " WHERE (" +
                            Column.TYPE + " = " + ContactNumber.TYPE_EQUALS + " AND " +
                            " ? = " + Column.NUMBER + ") OR (" +
                            Column.TYPE + " = " + ContactNumber.TYPE_STARTS + " AND " +
                            " ? LIKE " + Column.NUMBER + "||'%') OR (" +
                            Column.TYPE + " = " + ContactNumber.TYPE_ENDS + " AND " +
                            " ? LIKE '%'||" + Column.NUMBER + ") OR (" +
                            Column.TYPE + " = " + ContactNumber.TYPE_CONTAINS + " AND " +
                            " ? LIKE '%'||" + Column.NUMBER + "||'%')";
        }
    }

    // ContactsNumber table item
    public static class ContactNumber {
        public static final int TYPE_EQUALS = 0;
        public static final int TYPE_CONTAINS = 1;
        public static final int TYPE_STARTS = 2;
        public static final int TYPE_ENDS = 3;

        public final long id;
        public final String number;
        public final int type;
        public final long contactId;

        public ContactNumber(long id, @NonNull String number, long contactId) {
            this(id, number, TYPE_EQUALS, contactId);
        }

        public ContactNumber(long id, @NonNull String number, int type, long contactId) {
            this.id = id;
            this.number = number;
            this.type = type;
            this.contactId = contactId;
        }
    }

    // ContactsNumber item cursor wrapper
    private class ContactNumberCursorWrapper extends CursorWrapper {
        private final int ID;
        private final int NUMBER;
        private final int TYPE;
        private final int CONTACT_ID;

        ContactNumberCursorWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            ID = cursor.getColumnIndex(ContactNumberTable.Column.ID);
            NUMBER = cursor.getColumnIndex(ContactNumberTable.Column.NUMBER);
            TYPE = cursor.getColumnIndex(ContactNumberTable.Column.TYPE);
            CONTACT_ID = cursor.getColumnIndex(ContactNumberTable.Column.CONTACT_ID);
        }

        ContactNumber getNumber() {
            long id = getLong(ID);
            String number = getString(NUMBER);
            int type = getInt(TYPE);
            long contactId = getLong(CONTACT_ID);
            return new ContactNumber(id, number, type, contactId);
        }
    }

    // Deletes contact number by id
    private int deleteContactNumber(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(ContactNumberTable.NAME,
                ContactNumberTable.Column.ID + " = " + id,
                null);
    }

    // Selects contact numbers by contact id
    @Nullable
    private ContactNumberCursorWrapper getContactNumbersByContactId(long contactId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                ContactNumberTable.Statement.SELECT_BY_CONTACT_ID,
                new String[]{String.valueOf(contactId)});

        return (validate(cursor) ? new ContactNumberCursorWrapper(cursor) : null);
    }

    // Searches contact numbers by number value
    @Nullable
    private ContactNumberCursorWrapper getContactNumbersByNumber(String number) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                ContactNumberTable.Statement.SELECT_BY_NUMBER,
                new String[]{number, number, number, number});

        return (validate(cursor) ? new ContactNumberCursorWrapper(cursor) : null);
    }

    // Searches contact numbers by type and value
    @Nullable
    private ContactNumberCursorWrapper getContactNumbersByTypeAndNumber(int numberType, String number) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                ContactNumberTable.Statement.SELECT_BY_TYPE_AND_NUMBER,
                new String[]{String.valueOf(numberType), number});

        return (validate(cursor) ? new ContactNumberCursorWrapper(cursor) : null);
    }

    // Searches contact numbers by number value
    private List<ContactNumber> getContactNumbers(String number) {
        List<ContactNumber> list = new LinkedList<>();
        ContactNumberCursorWrapper cursor = getContactNumbersByNumber(number);
        if (cursor != null) {
            do {
                list.add(cursor.getNumber());
            } while (cursor.moveToNext());
            cursor.close();
        }

        return list;
    }

    // Searches contact numbers by numbers types and values
    // This method is mainly needed for retrieving actual ContactNumber.id and/or ContactNumber.contactId
    private List<ContactNumber> getContactNumbers(List<ContactNumber> numbers) {
        List<ContactNumber> list = new LinkedList<>();
        for (ContactNumber number : numbers) {
            ContactNumberCursorWrapper cursor =
                    getContactNumbersByTypeAndNumber(number.type, number.number);
            if (cursor != null) {
                do {
                    list.add(cursor.getNumber());
                } while (cursor.moveToNext());
                cursor.close();
            }
        }

        return list;
    }
//----------------------------------------------------------------

    // Table of contacts (black/white lists)
    private static class ContactTable {
        static final String NAME = "contact";

        static class Column {
            static final String ID = "_id";
            static final String NAME = "name";
            static final String TYPE = "type"; // black/white type
        }

        static class Statement {
            static final String CREATE =
                    "CREATE TABLE " + ContactTable.NAME +
                            "(" +
                            Column.ID + " INTEGER PRIMARY KEY NOT NULL, " +
                            Column.NAME + " TEXT NOT NULL, " +
                            Column.TYPE + " INTEGER NOT NULL DEFAULT 0 " +
                            ")";

            static final String SELECT_BY_TYPE =
                    "SELECT * " +
                            " FROM " + ContactTable.NAME +
                            " WHERE " + Column.TYPE + " = ? " +
                            " ORDER BY " + Column.NAME +
                            " ASC";

            static final String SELECT_BY_NAME =
                    "SELECT * " +
                            " FROM " + ContactTable.NAME +
                            " WHERE " + Column.NAME + " = ? ";

            static final String SELECT_BY_TYPE_AND_NAME =
                    "SELECT * " +
                            " FROM " + ContactTable.NAME +
                            " WHERE " + Column.TYPE + " = ? " +
                            " AND " + Column.NAME + " = ? ";

            static final String SELECT_BY_ID =
                    "SELECT * " +
                            " FROM " + ContactTable.NAME +
                            " WHERE " + Column.ID + " = ? ";

            static final String SELECT_BY_FILTER =
                    "SELECT * " +
                            " FROM " + ContactTable.NAME +
                            " WHERE " + Column.TYPE + " = ? " +
                            " AND " + Column.NAME + " LIKE ? " +
                            " ORDER BY " + Column.NAME +
                            " ASC";
        }
    }

    // The contact
    public static class Contact {
        public static final int TYPE_BLACK_LIST = 1;
        public static final int TYPE_WHITE_LIST = 2;

        public final long id;
        public final String name;
        public final int type;
        public final List<ContactNumber> numbers;

        Contact(long id, @NonNull String name, int type, @NonNull List<ContactNumber> numbers) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.numbers = numbers;
        }
    }

    // Source of the contact
    public interface ContactSource {
        Contact getContact();
    }

    // Contact cursor wrapper
    public class ContactCursorWrapper extends CursorWrapper implements ContactSource {
        private final int ID;
        private final int NAME;
        private final int TYPE;

        ContactCursorWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            ID = cursor.getColumnIndex(ContactTable.Column.ID);
            NAME = getColumnIndex(ContactTable.Column.NAME);
            TYPE = getColumnIndex(ContactTable.Column.TYPE);
        }

        @Override
        public Contact getContact() {
            return getContact(true);
        }

        Contact getContact(boolean withNumbers) {
            long id = getLong(ID);
            String name = getString(NAME);
            int type = getInt(TYPE);

            List<ContactNumber> numbers = new LinkedList<>();
            if (withNumbers) {
                ContactNumberCursorWrapper cursor = getContactNumbersByContactId(id);
                if (cursor != null) {
                    do {
                        numbers.add(cursor.getNumber());
                    } while (cursor.moveToNext());
                    cursor.close();
                }
            }

            return new Contact(id, name, type, numbers);
        }
    }

    // Searches all contacts by type
    @Nullable
    private ContactCursorWrapper getContacts(int contactType) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                ContactTable.Statement.SELECT_BY_TYPE,
                new String[]{String.valueOf(contactType)});

        return (validate(cursor) ? new ContactCursorWrapper(cursor) : null);
    }

    // Searches all contacts by type filtering by passed filter
    @Nullable
    public ContactCursorWrapper getContacts(int contactType, @Nullable String filter) {
        if (filter == null) {
            return getContacts(contactType);
        }
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                ContactTable.Statement.SELECT_BY_FILTER,
                new String[]{String.valueOf(contactType), "%" + filter + "%"});

        return (validate(cursor) ? new ContactCursorWrapper(cursor) : null);
    }

    // Searches contact by name
    @Nullable
    public ContactCursorWrapper getContact(String contactName) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                ContactTable.Statement.SELECT_BY_NAME,
                new String[]{contactName});

        return (validate(cursor) ? new ContactCursorWrapper(cursor) : null);
    }

    // Searches contact by type and name
    @Nullable
    private ContactCursorWrapper getContact(int contactType, String contactName) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                ContactTable.Statement.SELECT_BY_TYPE_AND_NAME,
                new String[]{String.valueOf(contactType), contactName});

        return (validate(cursor) ? new ContactCursorWrapper(cursor) : null);
    }

    // Searches contact by id
    @Nullable
    public ContactCursorWrapper getContact(long contactId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                ContactTable.Statement.SELECT_BY_ID,
                new String[]{String.valueOf(contactId)});

        return (validate(cursor) ? new ContactCursorWrapper(cursor) : null);
    }

    // Adds a new contact and returns contact id or -1 on error
    private long addContact(int contactType, @NonNull String contactName) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ContactTable.Column.NAME, contactName);
        values.put(ContactTable.Column.TYPE, contactType);
        return db.insert(ContactTable.NAME, null, values);
    }

    // Adds a contact with numbers and returns contact id or -1 on error.
    // If adding numbers already belong to some contacts - removes them at first.
    public long addContact(int contactType, @NonNull String contactName, @NonNull List<ContactNumber> numbers) {
        if (numbers.size() == 0) return -1;

        long contactId = -1;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // delete existing numbers from contacts
            deleteContactNumbers(numbers);

            // try to find existing contact with the same name and type
            ContactCursorWrapper cursor = getContact(contactType, contactName);
            if (cursor != null) {
                Contact contact = cursor.getContact(false);
                contactId = contact.id;
                cursor.close();
            }

            // contact was not found
            if (contactId < 0) {
                // add a new one
                contactId = addContact(contactType, contactName);
            }

            // add numbers to the contact
            if (contactId >= 0) {
                for (ContactNumber number : numbers) {
                    ContentValues values = new ContentValues();
                    values.put(ContactNumberTable.Column.NUMBER, number.number);
                    values.put(ContactNumberTable.Column.TYPE, number.type);
                    values.put(ContactNumberTable.Column.CONTACT_ID, contactId);
                    if (db.insert(ContactNumberTable.NAME, null, values) < 0) {
                        return -1;
                    }
                }
                db.setTransactionSuccessful();
            }
        } finally {
            db.endTransaction();
        }

        return contactId;
    }

    // Deletes contact numbers if they exist.
    // After that deletes parent contacts if they are empty.
    private void deleteContactNumbers(List<ContactNumber> numbers) {
        // get full initialized contact numbers by type and value
        List<ContactNumber> contactNumbers = getContactNumbers(numbers);

        // delete found numbers and collect ids of parent contacts
        Set<Long> contactIds = new TreeSet<>();
        for (ContactNumber number : contactNumbers) {
            if (deleteContactNumber(number.id) > 0) {
                contactIds.add(number.contactId);
            }
        }

        // check contacts consistency
        for (Long contactId : contactIds) {
            // if contact does not have any number
            ContactNumberCursorWrapper cursor = getContactNumbersByContactId(contactId);
            if (cursor == null) {
                // delete it
                deleteContact(contactId);
            } else {
                cursor.close();
            }
        }
    }

    // Adds contact with single number
    public long addContact(int contactType, @NonNull String contactName, @NonNull ContactNumber contactNumber) {
        List<ContactNumber> numbers = new LinkedList<>();
        numbers.add(contactNumber);
        return addContact(contactType, contactName, numbers);
    }

    // Adds contact with single number with default type
    public long addContact(int contactType, @NonNull String contactName, @Nullable String number) {
        if (number == null) {
            number = contactName;
        }
        List<ContactNumber> numbers = new LinkedList<>();
        numbers.add(new ContactNumber(0, number, 0));
        return addContact(contactType, contactName, numbers);
    }

    // Deletes all contacts which are specified in container with specified type
    public int deleteContacts(int contactType, IdentifiersContainer contactIds, @Nullable String filter) {
        if (contactIds.isEmpty()) return 0;

        boolean all = contactIds.isAll();
        List<String> ids = contactIds.getIdentifiers(new LinkedList<String>());

        // build 'WHERE' clause
        String clause = Common.concatClauses(new String[]{
                ContactTable.Column.TYPE + " = " + contactType,
                Common.getLikeClause(ContactTable.Column.NAME, filter),
                Common.getInClause(ContactTable.Column.ID, all, ids)
        });

        // delete contacts
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(ContactTable.NAME, clause, null);
    }

    // Deletes contact by id
    public int deleteContact(long contactId) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(ContactTable.NAME,
                ContactTable.Column.ID + " = " + contactId,
                null);
    }

    // Searches contacts by contact numbers (retrieving them by ContactNumber.contactId)
    private List<Contact> getContacts(List<ContactNumber> numbers, boolean withNumbers) {
        List<Contact> contacts = new LinkedList<>();
        for (ContactNumber contactNumber : numbers) {
            ContactCursorWrapper cursor = getContact(contactNumber.contactId);
            if (cursor != null) {
                contacts.add(cursor.getContact(withNumbers));
                cursor.close();
            }
        }
        return contacts;
    }

    // Searches contacts by contact number
    public List<Contact> getContacts(String number, boolean withNumbers) {
        List<ContactNumber> numbers = getContactNumbers(number);
        return getContacts(numbers, withNumbers);
    }

    // Searches contact by name and number
    @Nullable
    public Contact getContact(String contactName, String number) {
        List<Contact> contacts = getContacts(number, false);
        for (Contact contact : contacts) {
            if (contact.name.equals(contactName)) {
                return contact;
            }
        }
        return null;
    }

    // Moves the contact to the opposite type list
    public long moveContact(Contact contact) {
        int type = reverseContactType(contact.type);
        return addContact(type, contact.name, contact.numbers);
    }

    // Reverses passed contact type
    private int reverseContactType(int type) {
        return (type == Contact.TYPE_BLACK_LIST ?
                Contact.TYPE_WHITE_LIST :
                Contact.TYPE_BLACK_LIST);
    }

//----------------------------------------------------------------

    // Table of settings
    private static class SettingsTable {
        static final String NAME = "settings";

        static class Column {
            static final String ID = "_id";
            static final String NAME = "name";
            static final String VALUE = "value";
        }

        static class Statement {
            static final String CREATE =
                    "CREATE TABLE " + SettingsTable.NAME +
                            "(" +
                            Column.ID + " INTEGER PRIMARY KEY NOT NULL, " +
                            Column.NAME + " TEXT NOT NULL, " +
                            Column.VALUE + " TEXT " +
                            ")";

            static final String SELECT_BY_NAME =
                    "SELECT * " +
                            " FROM " + SettingsTable.NAME +
                            " WHERE " + Column.NAME + " = ? ";
        }
    }

    // Settings item
    private class SettingsItem {
        final long id;
        final String name;
        final String value;

        SettingsItem(long id, String name, String value) {
            this.id = id;
            this.name = name;
            this.value = value;
        }
    }

    // SettingsItem cursor wrapper
    private class SettingsItemCursorWrapper extends CursorWrapper {
        private final int ID;
        private final int NAME;
        private final int VALUE;

        SettingsItemCursorWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            ID = cursor.getColumnIndex(SettingsTable.Column.ID);
            NAME = cursor.getColumnIndex(SettingsTable.Column.NAME);
            VALUE = cursor.getColumnIndex(SettingsTable.Column.VALUE);
        }

        SettingsItem getSettings() {
            long id = getLong(ID);
            String name = getString(NAME);
            String value = getString(VALUE);
            return new SettingsItem(id, name, value);
        }
    }

    // Selects settings by name
    @Nullable
    private SettingsItemCursorWrapper getSettings(@NonNull String name) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                SettingsTable.Statement.SELECT_BY_NAME,
                new String[]{name});

        return (validate(cursor) ? new SettingsItemCursorWrapper(cursor) : null);
    }

    // Selects value of settings by name
    @Nullable
    public String getSettingsValue(@NonNull String name) {
        SettingsItemCursorWrapper cursor = getSettings(name);
        if (cursor != null) {
            SettingsItem item = cursor.getSettings();
            cursor.close();
            return item.value;
        }
        return null;
    }

    // Sets value of settings with specified name
    public boolean setSettingsValue(@NonNull String name, @NonNull String value) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SettingsTable.Column.VALUE, value);
        // try to update value
        int n = db.update(SettingsTable.NAME,
                values,
                SettingsTable.Column.NAME + " = ? ",
                new String[]{name});
        if (n == 0) {
            // try to add name/value
            values.put(SettingsTable.Column.NAME, name);
            return db.insert(SettingsTable.NAME, null, values) >= 0;
        }

        return true;
    }
}
