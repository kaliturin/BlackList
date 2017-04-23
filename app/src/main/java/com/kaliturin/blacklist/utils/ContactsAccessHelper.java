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
import android.telephony.SmsMessage;
import android.util.Log;

import com.kaliturin.blacklist.utils.DatabaseAccessHelper.Contact;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.ContactNumber;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper.ContactSource;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Contacts list access helper
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
    public Contact getContact(Context context, String number) {
        if (!Permissions.isGranted(context, Permissions.READ_CONTACTS)) {
            return null;
        }

        Contact contact = null;
        ContactCursorWrapper cursor = getContactCursor(number);
        if (cursor != null) {
            contact = cursor.getContact(false);
            cursor.close();
        }

        return contact;
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
                        String number = normalizeContactNumber(cursor.getNumber());
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

    public static String normalizeContactNumber(String number) {
        return number.replaceAll("[-() ]", "");
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

    // Returns true if passed number contains in SMS content list
    public boolean containsNumberInSMSContent(Context context, @NonNull String number) {
        if (!Permissions.isGranted(context, Permissions.READ_SMS)) {
            return false;
        }

        final String ID = "_id";
        final String ADDRESS = "address";
        final String PERSON = "person";

        Cursor cursor = contentResolver.query(
                Uri.parse("content://sms"),
                new String[]{"DISTINCT " + ID, ADDRESS, PERSON},
                ADDRESS + " = ? ) GROUP BY (" + ADDRESS,
                new String[]{number},
                "date DESC");

        if (validate(cursor)) {
            cursor.close();
            return true;
        }

        return false;
    }

    // Selects contacts from SMS list filtering by contact name or number
    @Nullable
    private ContactFromSMSCursorWrapper getContactsFromSMSList(@Nullable String filter) {
        filter = (filter == null ? "" : filter.toLowerCase());
        final String ID = "_id";
        final String ADDRESS = "address"; // number
        final String PERSON = "person"; // contact id

        // filter by address (number) if person (contact id) is null
        Cursor cursor = contentResolver.query(
                Uri.parse("content://sms"),
                new String[]{"DISTINCT " + ID, ADDRESS, PERSON},
                ADDRESS + " IS NOT NULL AND (" +
                        PERSON + " IS NOT NULL OR " +
                        ADDRESS + " LIKE ? )" +
                        ") GROUP BY (" + ADDRESS,
                new String[]{"%" + filter + "%"},
                "date DESC");

        // now we need to filter contacts by names and fill matrix cursor
        if (validate(cursor)) {
            cursor.moveToFirst();
            MatrixCursor matrixCursor = new MatrixCursor(new String[]{ID, ADDRESS, PERSON});
            final int _ID = cursor.getColumnIndex(ID);
            final int _ADDRESS = cursor.getColumnIndex(ADDRESS);
            final int _PERSON = cursor.getColumnIndex(PERSON);
            do {
                String id = cursor.getString(_ID);
                String address = cursor.getString(_ADDRESS);
                String person = address;
                if (cursor.isNull(_PERSON)) {
                    matrixCursor.addRow(new String[]{id, address, person});
                } else {
                    // get person name from contacts
                    long contactId = cursor.getLong(_PERSON);
                    Contact contact = getContact(contactId);
                    if (contact != null) {
                        person = contact.name;
                    }
                    // filter contact
                    if (person.toLowerCase().contains(filter)) {
                        matrixCursor.addRow(new String[]{id, address, person});
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
            cursor = matrixCursor;
        }

        return (validate(cursor) ? new ContactFromSMSCursorWrapper(cursor) : null);
    }

    // Contact from SMS cursor wrapper
    private class ContactFromSMSCursorWrapper extends CursorWrapper implements ContactSource {
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
                if (set.add(key)) {
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
            if (name == null) {
                name = number;
            }

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
        private final int THREAD_ID;

        private SMSConversationWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            THREAD_ID = cursor.getColumnIndex("thread_id");
        }

        @Nullable
        public SMSConversation getConversation(Context context) {
            int threadId = getInt(THREAD_ID);
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
                Uri.parse("content://sms/conversations"),
                new String[]{"thread_id as _id", "thread_id"},
                null,
                null,
                "date DESC");

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
            SMSMessage sms = cursor.getSMSMessage(context);
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

        String orderClause = (desc ? " date DESC " : " date ASC ");
        String limitClause = (limit > 0 ? " LIMIT " + limit : "");
        Cursor cursor = contentResolver.query(
                Uri.parse("content://sms"),
                null,
                " thread_id = ? " +
                        // we don't support drafts yet
                        " AND address NOT NULL",
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

        String orderClause = (desc ? " date DESC " : " date ASC ");
        String limitClause = (limit > 0 ? " LIMIT " + limit : "");
        Cursor cursor = contentResolver.query(
                Uri.parse("content://sms"),
                new String[]{" _id "},
                " thread_id = ? " +
                        // we don't support drafts yet
                        " AND address NOT NULL",
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
                Uri.parse("content://sms/inbox"),
                new String[]{"COUNT(_id)"},
                " thread_id = ? AND " +
                        " read = ? ",
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
        values.put("read", 1);
        return contentResolver.update(
                Uri.parse("content://sms/inbox"),
                values,
                " thread_id = ? AND " +
                        " read = ? ",
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
                Uri.parse("content://sms"),
                " thread_id = ? ",
                new String[]{String.valueOf(threadId)});

        return (count > 0);
    }

    // Deletes SMS message by id
    public boolean deleteSMSMessageById(Context context, long id) {
        if (!Permissions.isGranted(context, Permissions.WRITE_SMS)) {
            return false;
        }

        int count = contentResolver.delete(
                Uri.parse("content://sms"),
                " _id = ? ",
                new String[]{String.valueOf(id)});

        return (count > 0);
    }

    // Marks all SMS messages are seen
    public boolean setSMSMessagesSeen(Context context) {
        if (!Permissions.isGranted(context, Permissions.WRITE_SMS)) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("seen", 1);
        return contentResolver.update(
                Uri.parse("content://sms/inbox"),
                values,
                " seen = ? ",
                new String[]{String.valueOf(0)}) > 0;
    }

    // Returns SMS thread id by phone number or -1 on error
    public int getSMSThreadIdByNumber(Context context, String number) {
        if (!Permissions.isGranted(context, Permissions.READ_SMS)) {
            return -1;
        }

        Cursor cursor = contentResolver.query(
                Uri.parse("content://sms"),
                new String[]{"thread_id"},
                " address = ? ",
                new String[]{number},
                "date DESC LIMIT 1 ");

        int threadId = -1;
        if (validate(cursor)) {
            cursor.moveToFirst();
            threadId = cursor.getInt(0);
            cursor.close();
        }

        return threadId;
    }

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
        private final int ID;
        private final int TYPE;
        private final int STATUS;
        private final int DATE;
        private final int DATE_SENT;
        private final int DELIVERY_DATE;
        private final int PERSON;
        private final int NUMBER;
        private final int BODY;

        private SMSMessageCursorWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            ID = cursor.getColumnIndex("_id");
            TYPE = cursor.getColumnIndex("type");
            STATUS = cursor.getColumnIndex("status");
            DATE = cursor.getColumnIndex("date");
            DATE_SENT = cursor.getColumnIndex("date_sent");
            DELIVERY_DATE = cursor.getColumnIndex("delivery_date");
            NUMBER = cursor.getColumnIndex("address");
            PERSON = cursor.getColumnIndex("person");
            BODY = cursor.getColumnIndex("body");
        }

        SMSMessage getSMSMessage(Context context) {
            long id = getLong(ID);
            int type = getInt(TYPE);
            int status = getInt(STATUS);
            long date = getLong(DATE);
            long date_sent = 0;
            if (DATE_SENT >= 0) {
                date_sent = getLong(DATE_SENT);
            } else {
                if (DELIVERY_DATE >= 0) {
                    date_sent = getLong(DELIVERY_DATE);
                }
            }
            String number = getString(NUMBER);
            String body = getString(BODY);

            String person = null;
            Contact contact;
            if (!isNull(PERSON)) {
                // if person is defined - get contact name
                long contactId = getLong(PERSON);
                contact = getContact(contactId);
            } else {
                contact = getContact(context, number);
            }
            if (contact != null) {
                person = contact.name;
            }

            return new SMSMessage(id, type, status, date, date_sent, person, number, body);
        }
    }

    // SMS message cursor wrapper
    public class SMSMessageCursorWrapper2 extends CursorWrapper {
        private final int ID;

        private SMSMessageCursorWrapper2(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            ID = cursor.getColumnIndex("_id");
        }

        @Nullable
        public SMSMessage getSMSMessage(Context context) {
            long id = getLong(ID);
            return getSMSMessagesById(context, id);
        }
    }

    @Nullable
    private SMSMessage getSMSMessagesById(Context context, long id) {
//        if (!Permissions.isGranted(context, Permissions.READ_SMS) ||
//                !Permissions.isGranted(context, Permissions.READ_CONTACTS)) {
//            return null;
//        }

        Cursor cursor = contentResolver.query(
                CONTENT_URI_SMS,
                null,
                " _id = " + id,
                null,
                null);

        SMSMessage message = null;
        if (validate(cursor)) {
            SMSMessageCursorWrapper cursorWrapper = new SMSMessageCursorWrapper(cursor);
            message = cursorWrapper.getSMSMessage(context);
            cursor.close();
        }

        return message;
    }

    // Writes SMS messages to the Inbox
    // Needed only since API19 - where only default SMS app can write to content resolver
    @TargetApi(19)
    public boolean writeSMSMessageToInbox(Context context, SmsMessage[] messages, long timeReceived) {
        if (!Permissions.isGranted(context, Permissions.WRITE_SMS)) {
            return false;
        }

        for (SmsMessage message : messages) {
            // get contact by SMS address
            Contact contact = getContact(context, message.getOriginatingAddress());

            // create writing values
            ContentValues values = new ContentValues();
            values.put(ADDRESS, message.getDisplayOriginatingAddress());
            values.put(BODY, message.getMessageBody());
            values.put(PERSON, (contact == null ? null : contact.id));
            values.put(DATE, timeReceived);
            values.put(DATE_SENT, message.getTimestampMillis());
            values.put(PROTOCOL, message.getProtocolIdentifier());
            values.put(REPLY_PATH_PRESENT, message.isReplyPathPresent());
            values.put(SERVICE_CENTER, message.getServiceCenterAddress());
            String subject = message.getPseudoSubject();
            subject = (subject != null && !subject.isEmpty() ? subject : null);
            values.put(SUBJECT, subject);
            values.put(READ, "0");
            values.put(SEEN, "0");

            // write SMS to Inbox
            contentResolver.insert(CONTENT_URI_SMS_INBOX, values);
        }

        return true;
    }


    // SMS data uris
    public static final Uri CONTENT_URI_SMS = Uri.parse("content://sms");
    public static final Uri CONTENT_URI_SMS_INBOX = Uri.parse("content://sms/inbox");
    public static final Uri CONTENT_URI_SMS_OUTBOX = Uri.parse("content://sms/outbox");

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
        Uri result = contentResolver.insert(CONTENT_URI_SMS, values);

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
                CONTENT_URI_SMS,
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
