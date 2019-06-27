# Blacklist Call & SMS Blocker

(The project is not currently maintaining)

[<img src="https://f-droid.org/badge/get-it-on.png"
      alt="Get it on F-Droid"
      height="80">](https://f-droid.org/packages/com.kaliturin.blacklist/)

Overview
---
This is a free application that allows you to configure the blocking of unwanted calls and SMS
in convenient way. It does not contain ads, supports light and dark interface themes and is
supported by devices with Android 2.3 and up.

Features
---
- Managing the Blacklist - to block phone numbers
- Managing the Whitelist - to exclude phone numbers from blocking
- Reading the Contacts and SMS lists of your phone - to block phone numbers not included in these lists
- Blocking of private/anonymous numbers
- Different ways of comparing blocked numbers (equality, by fragment, etc.)
- Recording of the event log - the history of blocked calls and SMS (with saving the texts)
- Manage notifications about block events
- Support for light and dark themes of the interface
- Common functions of SMS-application - reading/writing/exchanging SMS-messages

Permissions granting
---
Because of the security policy in Android 4.4 and up to block the incoming SMS-messages
it is required to give to the application the permissions of "Default SMS-application" in the Settings.
If necessary, you can always opt-out and return to the previous "Default SMS-application".
In this case, the SMS blocking will be disabled. At the moment the application supports blocking
and exchanging SMS-messages only. Support for MMS-messages has not yet been implemented.

Because of the security policy in Android 6.0 and up, the application requires permissions:
1. "Access photos, media and files". Allows reading and writing files in the internal memory of
the device - for storing the block lists.
2. "Send and view SMS-messages". Allows to display and block incoming SMS-messages.
3. "Make and manage phone calls". Allows to block incoming calls.
4. "Access contacts". Allows to display the list of contacts.

Functionality details
---
1. Messaging.
Messages list displays SMS-chats, sorted by the time of the last message in them. From here you can
go to the reading, writing and sending SMS-messages.

2. Blacklist.
This is the list of contacts with numbers, calls and SMS from which you\'re planning to block.
Add a contact to the list by clicking the \"+\" toolbar button and from the resulting menu choose
one of the suggested ways to add a contact. If you manually add a blocked contact, you can choose
one of the rules for comparing numbers: \"equals\", \"contains\", \"starts with\", or \"ends with\".
To edit, delete, or move a contact to another list, make a long click on its line. If you want to
temporarily disable blocking contacts from the Blacklist, you can do this in the Settings.

3. Whitelist.
This is the list of contacts with numbers, calls and SMS from which will never be blocked. This
list is useful for additional setting of exceptions from blocking. It has a higher priority than
other lists when resolving conflicts between them. Managing the contacts in the Whitelist is
similar to the Blacklist.

4. Event list.
This is the list of records with info about the events of calls/SMS blocking. Disabling logging of
the events can be done in the Settings.

5. Settings.
In the Settings you can set the blocking of all calls and SMS from numbers that are not in the
list of contacts of your phone and (or) not included in the list of phone numbers with which you
exchanged SMS. These options can be used in conjunction with the Black and White lists, or
separately. You can adjust the level of visual and audio information about the event of blocking.
You can set the light or dark theme of the application interface. Here, you can also export and
import application data, for example, to transfer them from one device to another.

### License:

    Copyright (C) 2017 Anton Kaliturin <kaliturin@gmail.com>
    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.

[Privacy Policy](https://github.com/kaliturin/BlackList/wiki/Privacy-Policy)
