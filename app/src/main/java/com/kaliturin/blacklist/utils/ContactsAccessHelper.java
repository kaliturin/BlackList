package com.kaliturin.blacklist.utils;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.kaliturin.blacklist.utils.DatabaseAccessHelper.Contact;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.ContactNumber;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.ContactSource;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Contacts/SMS/Calls list access helper
 */
public class ContactsAccessHelper {
    private static final String TAG = ContactsAccessHelper.class.getName();
    private static ContactsAccessHelper sInstance = null;
    private ContentResolver contentResolver = null;

    private ContactsAccessHelper(Context context) {
        contentResolver = context.getApplicationContext().getContentResolver();
    }

    public static synchronized ContactsAccessHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ContactsAccessHelper(context);
        }
        return sInstance;
    }

    private boolean validate(Cursor cursor) {
        if (cursor == null || cursor.isClosed()) return false;
        if (cursor.getCount() == 0) {
            cursor.close();
            return false;
        }
        return true;
    }

    // Types of the contact sources
    public enum ContactSourceType {
        FROM_CONTACTS,
        FROM_CALLS_LOG,
        FROM_SMS_LIST,
        FROM_BLACK_LIST,
        FROM_WHITE_LIST
    }

    @Nullable
    public static String getPermission(ContactSourceType sourceType) {
        switch (sourceType) {
            case FROM_CONTACTS:
                return Permissions.READ_CONTACTS;
            case FROM_CALLS_LOG:
                return Permissions.READ_CALL_LOG;
            case FROM_SMS_LIST:
                return Permissions.READ_SMS;
            case FROM_BLACK_LIST:
            case FROM_WHITE_LIST:
                return Permissions.WRITE_EXTERNAL_STORAGE;
        }
        return null;
    }

    // Returns contacts from specified source
    @Nullable
    public Cursor getContacts(Context context, ContactSourceType sourceType, @Nullable String filter) {
        // check permission
        final String permission = getPermission(sourceType);
        if (permission == null || !Permissions.isGranted(context, permission)) {
            return null;
        }
        // return contacts
        switch (sourceType) {
            case FROM_CONTACTS:
                return getContacts(filter);
            case FROM_CALLS_LOG:
                return getContactsFromCallsLog(filter);
            case FROM_SMS_LIST:
                return getContactsFromSMSList(filter);
            case FROM_BLACK_LIST: {
                DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(context);
                if (db != null) {
                    return db.getContacts(DatabaseAccessHelper.Contact.TYPE_BLACK_LIST, filter);
                }
            }
            case FROM_WHITE_LIST: {
                DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(context);
                if (db != null) {
                    return db.getContacts(DatabaseAccessHelper.Contact.TYPE_WHITE_LIST, filter);
                }
            }
        }
        return null;
    }

    // Selects contacts from contacts list
    @Nullable
    private ContactCursorWrapper getContacts(@Nullable String filter) {
        filter = (filter == null ? "%%" : "%" + filter + "%");
        Cursor cursor = contentResolver.query(
                Contacts.CONTENT_URI,
                new String[]{Contacts._ID, Contacts.DISPLAY_NAME},
                Contacts.IN_VISIBLE_GROUP + " != 0 AND " +
                        Contacts.HAS_PHONE_NUMBER + " != 0 AND " +
                        Contacts.DISPLAY_NAME + " IS NOT NULL AND " +
                        Contacts.DISPLAY_NAME + " LIKE ? ",
                new String[]{filter},
                Contacts.DISPLAY_NAME + " ASC");

        return (validate(cursor) ? new ContactCursorWrapper(cursor) : null);
    }

    // Selects contact from contacts list by id
    @Nullable
    private ContactCursorWrapper getContactCursor(long contactId) {
        Cursor cursor = contentResolver.query(
                Contacts.CONTENT_URI,
                new String[]{Contacts._ID, Contacts.DISPLAY_NAME},
                Contacts.DISPLAY_NAME + " IS NOT NULL AND " +
                        Contacts.IN_VISIBLE_GROUP + " != 0 AND " +
                        Contacts.HAS_PHONE_NUMBER + " != 0 AND " +
                        Contacts._ID + " = " + contactId,
                null,
                null);

        return (validate(cursor) ? new ContactCursorWrapper(cursor) : null);
    }

    @Nullable
    private Contact getContact(long contactId) {
        Contact contact = null;
        ContactCursorWrapper cursor = getContactCursor(contactId);
        if (cursor != null) {
            contact = cursor.getContact(false);
            cursor.close();
        }

        return contact;
    }

    // Selects contact from contacts list by phone number
    @Nullable
    private ContactCursorWrapper getContactCursor(String number) {
        Uri lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        Cursor cursor = contentResolver.query(lookupUri,
                new String[]{Contacts._ID, Contacts.DISPLAY_NAME},
                null,
                null,
                null);

        return (validate(cursor) ? new ContactCursorWrapper(cursor) : null);
    }

    @Nullable
    private Contact getContact(String number) {
        Contact contact = null;
        ContactCursorWrapper cursor = getContactCursor(number);
        if (cursor != null) {
            contact = cursor.getContact(false);
            cursor.close();
        }

        return contact;
    }

    @Nullable
    public Contact getContact(Context context, String number) {
        if (!Permissions.isGranted(context, Permissions.READ_CONTACTS)) {
            return null;
        }

        return getContact(number);
    }

    // Contact's cursor wrapper
    private class ContactCursorWrapper extends CursorWrapper implements ContactSource {
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

        Contact getContact(boolean withNumbers) {
            long id = getLong(ID);
            String name = getString(NAME);
            List<ContactNumber> numbers = new LinkedList<>();
            if (withNumbers) {
                ContactNumberCursorWrapper cursor = getContactNumbers(id);
                if (cursor != null) {
                    do {
                        // normalize the phone number (remove spaces and brackets)
                        String number = normalizePhoneNumber(cursor.getNumber());
                        // create and add contact number instance
                        ContactNumber contactNumber =
                                new ContactNumber(cursor.getPosition(), number, id);
                        numbers.add(contactNumber);
                    } while (cursor.moveToNext());
                    cursor.close();
                }
            }

            return new Contact(id, name, 0, numbers);
        }
    }

    // For the sake of performance we don't use comprehensive phone number pattern.
    // We just want to detect whether a phone number is digital but not symbolic.
    private Pattern digitalPhoneNumberPattern = Pattern.compile("[+]?[0-9-() ]+");
    // Is used for normalizing a phone number, removing from it brackets, dashes and spaces.
    private Pattern normalizePhoneNumberPattern = Pattern.compile("[-() ]");

    // If passed phone number is digital and not symbolic then normalizes
    // it, removing brackets, dashes and spaces.
    public String normalizePhoneNumber(String number) {
        if (digitalPhoneNumberPattern.matcher(number).matches()) {
            number = normalizePhoneNumberPattern.matcher(number).replaceAll("");
        }
        return number;
    }

    // Contact's number cursor wrapper
    private static class ContactNumberCursorWrapper extends CursorWrapper {
        private final int NUMBER;

        private ContactNumberCursorWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            NUMBER = cursor.getColumnIndex(Phone.NUMBER);
        }

        String getNumber() {
            return getString(NUMBER);
        }
    }

    // Selects all numbers of specified contact
    @Nullable
    private ContactNumberCursorWrapper getContactNumbers(long contactId) {
        Cursor cursor = contentResolver.query(
                Phone.CONTENT_URI,
                new String[]{Phone.NUMBER},
                Phone.NUMBER + " IS NOT NULL AND " +
                        Phone.CONTACT_ID + " = " + contactId,
                null,
                null);

        return (validate(cursor) ? new ContactNumberCursorWrapper(cursor) : null);
    }

//-------------------------------------------------------------------------------------

    // SMS data URIs
    private static final Uri URI_CONTENT_SMS = Uri.parse("content://sms");
    private static final Uri URI_CONTENT_SMS_INBOX = Uri.parse("content://sms/inbox");
    private static final Uri URI_CONTENT_SMS_CONVERSATIONS = Uri.parse("content://sms/conversations");

    // SMS data columns
    public static final String ID = "_id";
    public static final String ADDRESS = "address";
    public static final String BODY = "body";
    public static final String PERSON = "person";
    public static final String DATE = "date";
    public static final String DATE_SENT = "date_sent";
    public static final String PROTOCOL = "protocol";
    public static final String REPLY_PATH_PRESENT = "reply_path_present";
    public static final String SERVICE_CENTER = "service_center";
    public static final String SUBJECT = "subject";
    public static final String READ = "read";
    public static final String SEEN = "seen";
    public static final String TYPE = "type";
    public static final String STATUS = "status";
    public static final String DELIVERY_DATE = "delivery_date";
    public static final String THREAD_ID = "thread_id";

//-------------------------------------------------------------------------------------

    // Returns true if passed phone number contains in SMS content list
    public boolean containsNumberInSMSContent(Context context, @NonNull String number) {
        if (!Permissions.isGranted(context, Permissions.READ_SMS)) {
            return false;
        }

        // We cannot select passed phone number simply by using query because some stored numbers
        // may be not normalized. So we select all the unique numbers first, normalize them,
        // and then search for our number.
        Cursor cursor = contentResolver.query(
                URI_CONTENT_SMS,
                new String[]{"DISTINCT " + ADDRESS},
                ADDRESS + " IS NOT NULL) GROUP BY (" + ADDRESS,
                null,
                DATE + " DESC");

        if (validate(cursor)) {
            cursor.moveToFirst();
            final int _ADDRESS = cursor.getColumnIndex(ADDRESS);
            do {
                String address = cursor.getString(_ADDRESS);
                address = normalizePhoneNumber(address);
                if (address.equals(number)) {
                    cursor.close();
                    return true;
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        return false;
    }

    // Selects contacts from SMS list filtering by contact name or number
    @Nullable
    private ContactFromSMSCursorWrapper getContactsFromSMSList(@Nullable String filter) {
        filter = (filter == null ? "" : filter.toLowerCase());

        // filter by address (number) if person (contact id) is null
        Cursor cursor = contentResolver.query(
                URI_CONTENT_SMS,
                new String[]{"DISTINCT " + ID, ADDRESS, PERSON},
                ADDRESS + " IS NOT NULL " +
                        ") GROUP BY (" + ADDRESS,
                null,
                DATE + " DESC");

        // now we need to filter contacts by names and fill matrix cursor
        if (validate(cursor)) {
            cursor.moveToFirst();
            MatrixCursor matrixCursor = new MatrixCursor(new String[]{ID, ADDRESS, PERSON});
            final int _ID = cursor.getColumnIndex(ID);
            final int _ADDRESS = cursor.getColumnIndex(ADDRESS);
            final int _PERSON = cursor.getColumnIndex(PERSON);
            // set is used to filter repeated data
            Set<String> set = new HashSet<>();
            do {
                String id = cursor.getString(_ID);
                String address = cursor.getString(_ADDRESS);
                address = normalizePhoneNumber(address);
                if (!set.add(address)) {
                    continue;
                }
                String person = address;
                Contact contact = null;
                if (!cursor.isNull(_PERSON)) {
                    // get contact by id
                    long contactId = cursor.getLong(_PERSON);
                    contact = getContact(contactId);
                }
                // commented for the sake of performance
                //if (contact == null) {
                // find contact by address
                //contact = getContact(address);
                //}
                // get person name from contact
                if (contact != null) {
                    person = contact.name;
                }
                // filter contact
                if (person.toLowerCase().contains(filter)) {
                    matrixCursor.addRow(new String[]{id, address, person});
                }
            } while (cursor.moveToNext());
            cursor.close();
            cursor = matrixCursor;
        }

        return (validate(cursor) ? new ContactFromSMSCursorWrapper(cursor) : null);
    }

    // Contact from SMS cursor wrapper
    private class ContactFromSMSCursorWrapper extends CursorWrapper implements ContactSource {
        private final int _ID;
        private final int _ADDRESS;
        private final int _PERSON;

        private ContactFromSMSCursorWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            _ID = getColumnIndex(ID);
            _ADDRESS = getColumnIndex(ADDRESS);
            _PERSON = getColumnIndex(PERSON);
        }

        @Override
        public Contact getContact() {
            long id = getLong(_ID);
            String name = getString(_PERSON);
            String number = getString(_ADDRESS);
            List<ContactNumber> numbers = new LinkedList<>();
            numbers.add(new ContactNumber(0, number, id));

            return new Contact(id, name, 0, numbers);
        }
    }

//-------------------------------------------------------------------------------------

    // Selects contacts from calls log
    @Nullable
    private ContactFromCallsCursorWrapper getContactsFromCallsLog(@Nullable String filter) {
        filter = (filter == null ? "%%" : "%" + filter + "%");
        Cursor cursor = null;
        // This try/catch is required by IDE because we use Calls.CONTENT_URI
        try {
            // filter by name or by number
            cursor = contentResolver.query(
                    Calls.CONTENT_URI,
                    new String[]{Calls._ID, Calls.NUMBER, Calls.CACHED_NAME},
                    Calls.NUMBER + " IS NOT NULL AND (" +
                            Calls.CACHED_NAME + " IS NULL AND " +
                            Calls.NUMBER + " LIKE ? OR " +
                            Calls.CACHED_NAME + " LIKE ? )",
                    new String[]{filter, filter},
                    Calls.DATE + " DESC");
        } catch (SecurityException e) {
            Log.w(TAG, e);
        }

        if (validate(cursor)) {
            cursor.moveToFirst();
            // Because we cannot query distinct calls - we have queried all.
            // Then we getting rid of repeated data.
            MatrixCursor matrixCursor = new MatrixCursor(
                    new String[]{Calls._ID, Calls.NUMBER, Calls.CACHED_NAME});
            final int ID = cursor.getColumnIndex(Calls._ID);
            final int NUMBER = cursor.getColumnIndex(Calls.NUMBER);
            final int NAME = cursor.getColumnIndex(Calls.CACHED_NAME);
            Set<String> set = new HashSet<>();
            do {
                String number = cursor.getString(NUMBER);
                number = normalizePhoneNumber(number);
                String name = cursor.getString(NAME);
                if (name == null) {
                    name = number;
                }
                if (set.add(number + name)) {
                    String id = cursor.getString(ID);
                    matrixCursor.addRow(new String[]{id, number, name});
                }
            } while (cursor.moveToNext());
            cursor.close();
            cursor = matrixCursor;
        }

        return (validate(cursor) ? new ContactFromCallsCursorWrapper(cursor) : null);
    }

    // Contact from calls cursor wrapper
    private class ContactFromCallsCursorWrapper extends CursorWrapper implements ContactSource {
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
            List<ContactNumber> numbers = new LinkedList<>();
            numbers.add(new ContactNumber(0, number, id));

            return new Contact(id, name, 0, numbers);
        }
    }

//-------------------------------------------------------------------------------------

    // SMS conversation
    public class SMSConversation {
        public final int threadId;
        public final long date;
        public final String person;
        public final String number;
        public final String snippet;
        public final int unread;

        SMSConversation(int threadId, long date, String person,
                        String number, String snippet, int unread) {
            this.threadId = threadId;
            this.date = date;
            this.person = person;
            this.number = number;
            this.snippet = snippet;
            this.unread = unread;
        }
    }

    // SMS conversation cursor wrapper
    public class SMSConversationWrapper extends CursorWrapper {
        private final int _THREAD_ID;

        private SMSConversationWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            _THREAD_ID = cursor.getColumnIndex(THREAD_ID);
        }

        @Nullable
        public SMSConversation getConversation(Context context) {
            int threadId = getInt(_THREAD_ID);
            return getSMSConversationByThreadId(context, threadId);
        }
    }

    // Returns SMS conversation cursor wrapper
    @Nullable
    public SMSConversationWrapper getSMSConversations(Context context) {
        if (!Permissions.isGranted(context, Permissions.READ_SMS) ||
                !Permissions.isGranted(context, Permissions.READ_CONTACTS)) {
            return null;
        }

        // select available conversation's data
        Cursor cursor = contentResolver.query(
                URI_CONTENT_SMS_CONVERSATIONS,
                new String[]{THREAD_ID + " as " + ID, THREAD_ID},
                null,
                null,
                DATE + " DESC");

        return (validate(cursor) ? new SMSConversationWrapper(cursor) : null);
    }

    // Returns SMS conversation by thread id
    @Nullable
    private SMSConversation getSMSConversationByThreadId(Context context, int threadId) {
        if (!Permissions.isGranted(context, Permissions.READ_SMS) ||
                !Permissions.isGranted(context, Permissions.READ_CONTACTS)) {
            return null;
        }

        SMSConversation smsConversation = null;

        // get the count of unread SMS in the thread
        int unread = getSMSMessagesUnreadCountByThreadId(context, threadId);
        // get date and address from the last SMS of the thread
        SMSMessageCursorWrapper cursor = getSMSMessagesByThreadId(context, threadId, true, 1);
        if (cursor != null) {
            SMSMessage sms = cursor.getSMSMessage(true);
            smsConversation = new SMSConversation(threadId, sms.date,
                    sms.person, sms.number, sms.body, unread);
            cursor.close();
        }

        return smsConversation;
    }

    // Selects SMS messages by thread id
    @Nullable
    private SMSMessageCursorWrapper getSMSMessagesByThreadId(Context context, int threadId,
                                                             boolean desc, int limit) {
        if (!Permissions.isGranted(context, Permissions.READ_SMS) ||
                !Permissions.isGranted(context, Permissions.READ_CONTACTS)) {
            return null;
        }

        String orderClause = (desc ? DATE + " DESC " : DATE + " ASC ");
        String limitClause = (limit > 0 ? " LIMIT " + limit : "");
        Cursor cursor = contentResolver.query(
                URI_CONTENT_SMS,
                null,
                THREAD_ID + " = ? " +
                        // we don't support drafts yet
                        " AND " + ADDRESS + " NOT NULL ",
                new String[]{String.valueOf(threadId)},
                orderClause + limitClause);

        return (validate(cursor) ? new SMSMessageCursorWrapper(cursor) : null);
    }

    // Selects SMS messages by thread id.
    // Reads SMS in two steps: at first an index, at second all others data.
    // This approach is efficient for memory saving.
    @Nullable
    public SMSMessageCursorWrapper2 getSMSMessagesByThreadId2(Context context, int threadId,
                                                              boolean desc, int limit) {
        if (!Permissions.isGranted(context, Permissions.READ_SMS) ||
                !Permissions.isGranted(context, Permissions.READ_CONTACTS)) {
            return null;
        }

        String orderClause = (desc ? DATE + " DESC " : DATE + " ASC ");
        String limitClause = (limit > 0 ? " LIMIT " + limit : "");
        Cursor cursor = contentResolver.query(
                URI_CONTENT_SMS,
                new String[]{ID},
                THREAD_ID + " = ? " +
                        // we don't support drafts yet
                        " AND " + ADDRESS + " NOT NULL ",
                new String[]{String.valueOf(threadId)},
                orderClause + limitClause);

        return (validate(cursor) ? new SMSMessageCursorWrapper2(cursor) : null);
    }

    // Returns count of unread SMS messages by thread id
    public int getSMSMessagesUnreadCountByThreadId(Context context, int threadId) {
        if (!Permissions.isGranted(context, Permissions.READ_SMS)) {
            return 0;
        }

        Cursor cursor = contentResolver.query(
                URI_CONTENT_SMS_INBOX,
                new String[]{"COUNT(" + ID + ")"},
                THREAD_ID + " = ? AND " +
                        READ + " = ? ",
                new String[]{
                        String.valueOf(threadId),
                        String.valueOf(0)
                },
                null);

        int count = 0;
        if (validate(cursor)) {
            cursor.moveToFirst();
            count = cursor.getInt(0);
            cursor.close();
        }

        return count;
    }

    // Marks SMS messages are read by thread id
    public boolean setSMSMessagesReadByThreadId(Context context, int threadId) {
        if (!Permissions.isGranted(context, Permissions.WRITE_SMS)) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put(READ, 1);
        return contentResolver.update(
                URI_CONTENT_SMS_INBOX,
                values,
                THREAD_ID + " = ? AND " +
                        READ + " = ? ",
                new String[]{
                        String.valueOf(threadId),
                        String.valueOf(0)
                }) > 0;
    }

    // Deletes SMS messages by thread id
    public boolean deleteSMSMessagesByThreadId(Context context, int threadId) {
        if (!Permissions.isGranted(context, Permissions.WRITE_SMS)) {
            return false;
        }

        int count = contentResolver.delete(
                URI_CONTENT_SMS,
                THREAD_ID + " = ? ",
                new String[]{String.valueOf(threadId)});

        return (count > 0);
    }

    // Deletes SMS message by id
    public boolean deleteSMSMessageById(Context context, long id) {
        if (!Permissions.isGranted(context, Permissions.WRITE_SMS)) {
            return false;
        }

        int count = contentResolver.delete(
                URI_CONTENT_SMS,
                ID + " = ? ",
                new String[]{String.valueOf(id)});

        return (count > 0);
    }

    // Marks all SMS messages are seen
    public boolean setSMSMessagesSeen(Context context) {
        if (!Permissions.isGranted(context, Permissions.WRITE_SMS)) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put(SEEN, 1);
        return contentResolver.update(
                URI_CONTENT_SMS_INBOX,
                values,
                SEEN + " = ? ",
                new String[]{String.valueOf(0)}) > 0;
    }

    // Returns SMS thread id by phone number or -1 on error
    public int getSMSThreadIdByNumber(Context context, String number) {
        if (!Permissions.isGranted(context, Permissions.READ_SMS)) {
            return -1;
        }

        Cursor cursor = contentResolver.query(
                URI_CONTENT_SMS,
                new String[]{THREAD_ID},
                ADDRESS + " = ? ",
                new String[]{number},
                DATE + " DESC LIMIT 1 ");

        int threadId = -1;
        if (validate(cursor)) {
            cursor.moveToFirst();
            threadId = cursor.getInt(0);
            cursor.close();
        }

        return threadId;
    }

//--------------------------------------------------------------------------------

    // SMS message
    public class SMSMessage {
        public final long id;
        public final int type;
        public final int status;
        public final long date;
        public final long deliveryDate;
        public final String person;
        public final String number;
        public final String body;

        SMSMessage(long id, int type, int status, long date, long deliveryDate,
                   String person, String number, String body) {
            this.id = id;
            this.type = type;
            this.status = status;
            this.date = date;
            this.deliveryDate = deliveryDate;
            this.person = person;
            this.number = number;
            this.body = body;
        }
    }

    // SMS message cursor wrapper
    private class SMSMessageCursorWrapper extends CursorWrapper {
        private final int _ID;
        private final int _TYPE;
        private final int _STATUS;
        private final int _DATE;
        private final int _DATE_SENT;
        private final int _DELIVERY_DATE;
        private final int _PERSON;
        private final int _NUMBER;
        private final int _BODY;

        private SMSMessageCursorWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            _ID = cursor.getColumnIndex(ID);
            _TYPE = cursor.getColumnIndex(TYPE);
            _STATUS = cursor.getColumnIndex(STATUS);
            _DATE = cursor.getColumnIndex(DATE);
            _DATE_SENT = cursor.getColumnIndex(DATE_SENT);
            _DELIVERY_DATE = cursor.getColumnIndex(DELIVERY_DATE);
            _NUMBER = cursor.getColumnIndex(ADDRESS);
            _PERSON = cursor.getColumnIndex(PERSON);
            _BODY = cursor.getColumnIndex(BODY);
        }

        SMSMessage getSMSMessage(boolean withContact) {
            long id = getLong(_ID);
            int type = getInt(_TYPE);
            int status = getInt(_STATUS);
            long date = getLong(_DATE);
            long date_sent = 0;
            if (_DATE_SENT >= 0) {
                date_sent = getLong(_DATE_SENT);
            } else {
                if (_DELIVERY_DATE >= 0) {
                    date_sent = getLong(_DELIVERY_DATE);
                }
            }
            String number = getString(_NUMBER);
            number = normalizePhoneNumber(number);
            String body = getString(_BODY);
            String person = null;
            if (withContact) {
                Contact contact = null;
                // if person is defined
                if (!isNull(_PERSON)) {
                    // get contact
                    long contactId = getLong(_PERSON);
                    contact = getContact(contactId);
                }
                if (contact == null) {
                    // find contact by number
                    contact = getContact(number);
                }
                if (contact != null) {
                    person = contact.name;
                }
            }

            return new SMSMessage(id, type, status, date, date_sent, person, number, body);
        }
    }

    // SMS message cursor wrapper
    public class SMSMessageCursorWrapper2 extends CursorWrapper {
        private final int _ID;

        private SMSMessageCursorWrapper2(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            _ID = cursor.getColumnIndex(ID);
        }

        @Nullable
        public SMSMessage getSMSMessage(boolean withContact) {
            long id = getLong(_ID);
            return getSMSMessagesById(id, withContact);
        }
    }

    @Nullable
    private SMSMessage getSMSMessagesById(long id, boolean withContact) {
//        if (!Permissions.isGranted(context, Permissions.READ_SMS) ||
//                !Permissions.isGranted(context, Permissions.READ_CONTACTS)) {
//            return null;
//        }

        Cursor cursor = contentResolver.query(
                URI_CONTENT_SMS,
                null,
                ID + " = " + id,
                null,
                null);

        SMSMessage message = null;
        if (validate(cursor)) {
            SMSMessageCursorWrapper cursorWrapper = new SMSMessageCursorWrapper(cursor);
            message = cursorWrapper.getSMSMessage(withContact);
            cursor.close();
        }

        return message;
    }

    // Writes SMS message to the Inbox. This method is needed only since API19 -
    // where only default SMS app can write to the content resolver.
    @TargetApi(19)
    public boolean writeSMSMessageToInbox(Context context, Contact contact, Map<String, String> data) {
        if (!Permissions.isGranted(context, Permissions.WRITE_SMS)) {
            return false;
        }

        // create writing values
        ContentValues values = new ContentValues();
        values.put(ADDRESS, data.get(ADDRESS));
        values.put(BODY, data.get(BODY));
        values.put(PERSON, (contact == null ? null : contact.id));
        values.put(DATE, data.get(DATE));
        values.put(DATE_SENT, data.get(DATE_SENT));
        values.put(PROTOCOL, data.get(PROTOCOL));
        values.put(REPLY_PATH_PRESENT, data.get(REPLY_PATH_PRESENT));
        values.put(SERVICE_CENTER, data.get(SERVICE_CENTER));
        String subject = data.get(SUBJECT);
        subject = (subject != null && !subject.isEmpty() ? subject : null);
        values.put(SUBJECT, subject);
        values.put(READ, "0");
        values.put(SEEN, "0");

        // write message to the Inbox
        contentResolver.insert(URI_CONTENT_SMS_INBOX, values);

        return true;
    }

//--------------------------------------------------------------------------------

    // Message statuses
    public static final int MESSAGE_STATUS_NONE = -1;
    public static final int MESSAGE_STATUS_COMPLETE = 0;
    public static final int MESSAGE_STATUS_PENDING = 32;
    public static final int MESSAGE_STATUS_FAILED = 64;

    // Message types
    public static final int MESSAGE_TYPE_INBOX = 1;
    public static final int MESSAGE_TYPE_SENT = 2;
    public static final int MESSAGE_TYPE_OUTBOX = 4;
    public static final int MESSAGE_TYPE_FAILED = 5;

//--------------------------------------------------------------------------------

    // Writes SMS message to the Outbox
    public long writeSMSMessageToOutbox(Context context, String number, String message) {
        if (!Permissions.isGranted(context, Permissions.WRITE_SMS)) {
            return -1;
        }

        // get contact by SMS address
        Contact contact = getContact(context, number);

        // write SMS
        ContentValues values = new ContentValues();
        values.put(ADDRESS, number);
        values.put(BODY, message);
        values.put(TYPE, MESSAGE_TYPE_OUTBOX);
        values.put(STATUS, MESSAGE_STATUS_NONE);
        values.put(PERSON, (contact == null ? null : contact.id));
        Uri result = contentResolver.insert(URI_CONTENT_SMS, values);

        // get id of the written SMS
        long id = -1;
        if (result != null) {
            try {
                id = Long.valueOf(result.getLastPathSegment());
            } catch (NumberFormatException ex) {
                Log.w(TAG, ex);
            }
        }

        return id;
    }

    // Updates SMS message on sent
    public boolean updateSMSMessageOnSent(Context context, long messageId, boolean delivery, boolean failed) {
        int type, status;
        if (failed) {
            type = MESSAGE_TYPE_FAILED;
            status = MESSAGE_STATUS_NONE;
        } else {
            type = MESSAGE_TYPE_SENT;
            if (delivery) {
                status = MESSAGE_STATUS_PENDING;
            } else {
                status = MESSAGE_STATUS_NONE;
            }
        }
        return updateSMSMessage(context, messageId, type, status, 0);
    }

    // Updates SMS message on delivery
    public boolean updateSMSMessageOnDelivery(Context context, long messageId, boolean failed) {
        int type, status;
        if (failed) {
            type = MESSAGE_TYPE_FAILED;
            status = MESSAGE_STATUS_FAILED;
        } else {
            type = MESSAGE_TYPE_SENT;
            status = MESSAGE_STATUS_COMPLETE;
        }
        return updateSMSMessage(context, messageId, type, status, System.currentTimeMillis());
    }

    // Updates SMS message
    private boolean updateSMSMessage(Context context, long messageId, int type, int status, long deliveryDate) {
        if (!Permissions.isGranted(context, Permissions.WRITE_SMS)) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put(TYPE, type);
        values.put(STATUS, status);
        if (deliveryDate > 0) {
            values.put(DELIVERY_DATE, deliveryDate);
            values.put(DATE_SENT, deliveryDate);
        }

        return contentResolver.update(
                URI_CONTENT_SMS,
                values,
                ID + " = ? ",
                new String[]{String.valueOf(messageId)}) > 0;
    }

//---------------------------------------------------------------------

    private static void debug(Cursor cursor) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String s = cursor.getString(i);
            String n = cursor.getColumnName(i);
            sb.append("[").append(n).append("]=").append(s);
        }
        Log.d(TAG, sb.toString());
    }

    /*
    SMS table row sample:
    [_id]=6
    [thread_id]=5
    [address]=123
    [person]=null
    [date]=1485692853433
    [date_sent]=1485692853000
    [protocol]=0
    [read]=0
    [status]=-1
    [type]=1
    [reply_path_present]=0
    [subject]=null
    [body]=Don't forget the marshmallows!
    [service_center]=null
    [locked]=0
    [error_code]=0
    [seen]=0
     */
}
