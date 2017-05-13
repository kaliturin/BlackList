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

package com.kaliturin.blacklist.fragments;


import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kaliturin.blacklist.R;
import com.kaliturin.blacklist.activities.MainActivity;
import com.kaliturin.blacklist.adapters.SettingsArrayAdapter;
import com.kaliturin.blacklist.utils.DatabaseAccessHelper;
import com.kaliturin.blacklist.utils.DefaultSMSAppHelper;
import com.kaliturin.blacklist.utils.DialogBuilder;
import com.kaliturin.blacklist.utils.Permissions;
import com.kaliturin.blacklist.utils.Settings;
import com.kaliturin.blacklist.utils.Utils;

import java.io.File;


/**
 * Settings fragment
 */
public class SettingsFragment extends Fragment implements FragmentArguments {
    private static final int DEFAULT_SMS_APP = 1;
    private static final int BLOCKED_SMS = 2;
    private static final int RECEIVED_SMS = 3;
    private static final int BLOCKED_CALL = 4;
    private SettingsArrayAdapter adapter = null;
    private ListView listView = null;
    private int listPosition = 0;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // set activity title
        Bundle arguments = getArguments();
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (arguments != null && actionBar != null) {
            actionBar.setTitle(arguments.getString(TITLE));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            listPosition = savedInstanceState.getInt(LIST_POSITION, 0);
        } else {
            Bundle arguments = getArguments();
            if (arguments != null) {
                listPosition = arguments.getInt(LIST_POSITION, listPosition);
            }
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Permissions.notifyIfNotGranted(getContext(), Permissions.WRITE_EXTERNAL_STORAGE);

        listView = (ListView) view.findViewById(R.id.settings_list);
        loadListViewItems(listPosition);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(LIST_POSITION, listView.getFirstVisiblePosition());
    }

    @Override
    public void onPause() {
        super.onPause();
        listPosition = listView.getFirstVisiblePosition();
    }

    // Is used for getting result of ringtone picker dialog & default sms app dialog
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            // default sms app dialog result
            case DEFAULT_SMS_APP:
                if (resultCode == Activity.RESULT_OK) {
                    Permissions.invalidateCache();
                }
                // reload list
                listPosition = listView.getFirstVisiblePosition();
                loadListViewItems(listPosition);
                break;
            // ringtone picker dialog results
            default:
                // get ringtone url
                Uri uri = null;
                if (resultCode == Activity.RESULT_OK && data != null) {
                    uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                }
                // save url as settings property value
                setRingtoneUri(requestCode, uri);
                break;
        }
    }

    // Loads settings list view
    private void loadListViewItems(final int listPosition) {
        // Create list adapter and fill it with data
        adapter = new SettingsArrayAdapter(getContext());

        // default sms feature is available
        boolean isDefaultSmsAppAvailable = DefaultSMSAppHelper.isAvailable();
        // current app is default sms app
        boolean isDefaultSmsApp = DefaultSMSAppHelper.isDefault(getContext());

        if (isDefaultSmsAppAvailable) {
            // show sms default app switch
            adapter.addTitle(R.string.SMS_default_app);
            adapter.addCheckbox(R.string.Default_SMS_app, R.string.Set_as_default_SMS_app,
                    isDefaultSmsApp, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            DefaultSMSAppHelper.askForDefaultAppChange(
                                    SettingsFragment.this, DEFAULT_SMS_APP);
                        }
                    });
        }

        if (isDefaultSmsApp) {
            // sms blocking settings
            adapter.addTitle(R.string.SMS_blocking);
            adapter.addCheckbox(R.string.All_SMS, R.string.Block_all_SMS, Settings.BLOCK_ALL_SMS);
            adapter.addCheckbox(R.string.Black_list, R.string.Block_SMS_from_black_list,
                    Settings.BLOCK_SMS_FROM_BLACK_LIST);
            adapter.addCheckbox(R.string.Contacts_list, R.string.Block_SMS_not_from_contacts,
                    Settings.BLOCK_SMS_NOT_FROM_CONTACTS);
            adapter.addCheckbox(R.string.SMS_list, R.string.Block_SMS_not_from_SMS_list,
                    Settings.BLOCK_SMS_NOT_FROM_SMS_CONTENT);
            adapter.addCheckbox(R.string.Private_numbers, R.string.Block_SMS_from_private,
                    Settings.BLOCK_PRIVATE_SMS);
            adapter.addCheckbox(R.string.Journal, R.string.Write_SMS_to_journal,
                    Settings.WRITE_SMS_JOURNAL);

            // sms notifications settings
            adapter.addTitle(R.string.SMS_blocking_notification);
            adapter.addCheckbox(R.string.Status_bar, R.string.Notify_in_status_bar_blocked_SMS,
                    Settings.BLOCKED_SMS_STATUS_NOTIFICATION, new DependentRowOnClickListener());
            adapter.addCheckbox(R.string.Sound, R.string.Notify_with_sound_blocked_SMS,
                    Settings.BLOCKED_SMS_SOUND_NOTIFICATION, new RingtonePickerOnClickListener(BLOCKED_SMS));
            adapter.addCheckbox(R.string.Vibration, R.string.Notify_with_vibration_blocked_SMS,
                    Settings.BLOCKED_SMS_VIBRATION_NOTIFICATION, new DependentRowOnClickListener());
        }

        // sms receiving/sending
        adapter.addTitle(R.string.SMS_receiving_notification);
        adapter.addCheckbox(R.string.Sound, R.string.Notify_with_sound_received_SMS,
                Settings.RECEIVED_SMS_SOUND_NOTIFICATION, new RingtonePickerOnClickListener(RECEIVED_SMS));
        adapter.addCheckbox(R.string.Vibration, R.string.Notify_with_vibration_received_SMS,
                Settings.RECEIVED_SMS_VIBRATION_NOTIFICATION);
        adapter.addCheckbox(R.string.SMS_delivery, R.string.Notify_on_SMS_delivery,
                Settings.DELIVERY_SMS_NOTIFICATION);

        // calls blocking settings
        adapter.addTitle(R.string.Calls_blocking);
        adapter.addCheckbox(R.string.All_calls, R.string.Block_all_calls, Settings.BLOCK_ALL_CALLS);

        adapter.addCheckbox(R.string.Black_list, R.string.Block_calls_from_black_list,
                Settings.BLOCK_CALLS_FROM_BLACK_LIST);
        adapter.addCheckbox(R.string.Contacts_list, R.string.Block_calls_not_from_contacts,
                Settings.BLOCK_CALLS_NOT_FROM_CONTACTS);
        adapter.addCheckbox(R.string.SMS_list, R.string.Block_calls_not_from_SMS_list,
                Settings.BLOCK_CALLS_NOT_FROM_SMS_CONTENT);
        adapter.addCheckbox(R.string.Private_numbers, R.string.Block_calls_from_private,
                Settings.BLOCK_PRIVATE_CALLS);
        adapter.addCheckbox(R.string.Journal, R.string.Write_calls_to_journal,
                Settings.WRITE_CALLS_JOURNAL);
        adapter.addCheckbox(R.string.Call_log, R.string.Remove_from_call_log,
                Settings.REMOVE_FROM_CALL_LOG);

        // calls notifications settings
        adapter.addTitle(R.string.Calls_blocking_notification);
        adapter.addCheckbox(R.string.Status_bar, R.string.Notify_in_status_bar_blocked_call,
                Settings.BLOCKED_CALL_STATUS_NOTIFICATION, new DependentRowOnClickListener());
        adapter.addCheckbox(R.string.Sound, R.string.Notify_with_sound_blocked_call,
                Settings.BLOCKED_CALL_SOUND_NOTIFICATION, new RingtonePickerOnClickListener(BLOCKED_CALL));
        adapter.addCheckbox(R.string.Vibration, R.string.Notify_with_vibration_blocked_call,
                Settings.BLOCKED_CALL_VIBRATION_NOTIFICATION, new DependentRowOnClickListener());

        // app interface
        adapter.addTitle(R.string.App_interface);
        adapter.addCheckbox(R.string.Text_folding, R.string.Journal_SMS_text_folding,
                Settings.FOLD_SMS_TEXT_IN_JOURNAL);
        adapter.addCheckbox(R.string.Journal, R.string.Go_to_Journal_at_start,
                Settings.GO_TO_JOURNAL_AT_START);
        adapter.addCheckbox(R.string.Back_button, R.string.Exit_on_back_pressed,
                Settings.DONT_EXIT_ON_BACK_PRESSED);
        adapter.addCheckbox(R.string.UI_theme_light, 0, Settings.UI_THEME_LIGHT, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                restartApp();
            }
        });

        // app data export/import
        adapter.addTitle(R.string.App_data);
        // export DB file
        adapter.addButton(R.string.Export_data, R.string.Write_data_into_external,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // check permissions
                        if (Permissions.notifyIfNotGranted(getContext(), Permissions.WRITE_EXTERNAL_STORAGE)) {
                            return;
                        }
                        // open the dialog for getting the exporting DB file path
                        showFilePathDialog(R.string.Export_data, new TextView.OnEditorActionListener() {
                            @Override
                            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                                // export data file
                                exportDataFile(textView.getText().toString());
                                return true;
                            }
                        });
                    }
                });
        // import DB file
        adapter.addButton(R.string.Import_data, R.string.Load_data_from_external,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // check permissions
                        if (Permissions.notifyIfNotGranted(getContext(), Permissions.WRITE_EXTERNAL_STORAGE)) {
                            return;
                        }
                        // open the dialog for getting the importing DB file path
                        showFilePathDialog(R.string.Import_data, new TextView.OnEditorActionListener() {
                            @Override
                            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                                // import data file
                                if (importDataFile(textView.getText().toString())) {
                                    // import complete - restart
                                    Settings.invalidateCache();
                                    restartApp();
                                }
                                return true;
                            }
                        });
                    }
                });

        // add adapter to the ListView and scroll list to position
        listView.setAdapter(adapter);
        listView.post(new Runnable() {
            @Override
            public void run() {
                listView.setSelection(listPosition);
            }
        });
    }

    // Saves ringtone url as settings property value
    private void setRingtoneUri(int requestCode, @Nullable Uri uri) {
        String ringtoneProperty, soundProperty, statusProperty = null;
        switch (requestCode) {
            case BLOCKED_CALL:
                ringtoneProperty = Settings.BLOCKED_CALL_RINGTONE;
                soundProperty = Settings.BLOCKED_CALL_SOUND_NOTIFICATION;
                statusProperty = Settings.BLOCKED_CALL_STATUS_NOTIFICATION;
                break;
            case BLOCKED_SMS:
                ringtoneProperty = Settings.BLOCKED_SMS_RINGTONE;
                soundProperty = Settings.BLOCKED_SMS_SOUND_NOTIFICATION;
                statusProperty = Settings.BLOCKED_SMS_STATUS_NOTIFICATION;
                break;
            case RECEIVED_SMS:
                ringtoneProperty = Settings.RECEIVED_SMS_RINGTONE;
                soundProperty = Settings.RECEIVED_SMS_SOUND_NOTIFICATION;
                break;
            default:
                return;
        }

        if (uri != null) {
            Settings.setStringValue(getContext(), ringtoneProperty, uri.toString());
            adapter.setRowChecked(soundProperty, true);
            if (statusProperty != null) {
                // if we enable ringtone we must enable status bar notification
                adapter.setRowChecked(statusProperty, true);
            }
        } else {
            adapter.setRowChecked(soundProperty, false);
        }
    }

    // Returns ringtone url from settings property
    @Nullable
    private Uri getRingtoneUri(int requestCode) {
        String uriString = null;
        switch (requestCode) {
            case BLOCKED_CALL:
                uriString = Settings.getStringValue(getContext(), Settings.BLOCKED_CALL_RINGTONE);
                break;
            case BLOCKED_SMS:
                uriString = Settings.getStringValue(getContext(), Settings.BLOCKED_SMS_RINGTONE);
                break;
            case RECEIVED_SMS:
                uriString = Settings.getStringValue(getContext(), Settings.RECEIVED_SMS_RINGTONE);
                break;
        }

        return (uriString != null ? Uri.parse(uriString) : null);
    }

    // On row click listener for opening ringtone picker
    private class RingtonePickerOnClickListener implements View.OnClickListener {
        int requestCode;

        RingtonePickerOnClickListener(int requestCode) {
            this.requestCode = requestCode;
        }

        @Override
        public void onClick(View view) {
            if (isAdded()) {
                if (adapter.isRowChecked(view)) {
                    // open ringtone picker dialog
                    Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.Ringtone_picker));
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, getRingtoneUri(requestCode));
                    startActivityForResult(intent, requestCode);
                }
            } else {
                adapter.setRowChecked(view, false);
            }
        }
    }

    // On row click listener for updating dependent rows
    private class DependentRowOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            String property = adapter.getRowProperty(view);
            if (property == null) {
                return;
            }

            boolean checked = adapter.isRowChecked(view);
            if (!checked) {
                // if row was unchecked - reset dependent rows
                switch (property) {
                    case Settings.BLOCKED_SMS_STATUS_NOTIFICATION:
                        adapter.setRowChecked(Settings.BLOCKED_SMS_SOUND_NOTIFICATION, false);
                        adapter.setRowChecked(Settings.BLOCKED_SMS_VIBRATION_NOTIFICATION, false);
                        break;
                    case Settings.BLOCKED_CALL_STATUS_NOTIFICATION:
                        adapter.setRowChecked(Settings.BLOCKED_CALL_SOUND_NOTIFICATION, false);
                        adapter.setRowChecked(Settings.BLOCKED_CALL_VIBRATION_NOTIFICATION, false);
                        break;
                }
            } else {
                switch (property) {
                    case Settings.BLOCKED_SMS_SOUND_NOTIFICATION:
                    case Settings.BLOCKED_SMS_VIBRATION_NOTIFICATION:
                        adapter.setRowChecked(Settings.BLOCKED_SMS_STATUS_NOTIFICATION, true);
                        break;
                    case Settings.BLOCKED_CALL_SOUND_NOTIFICATION:
                    case Settings.BLOCKED_CALL_VIBRATION_NOTIFICATION:
                        adapter.setRowChecked(Settings.BLOCKED_CALL_STATUS_NOTIFICATION, true);
                        break;
                }
            }
        }
    }

    // Shows the dialog of database file path definition
    private void showFilePathDialog(@StringRes int titleId, final TextView.OnEditorActionListener listener) {
        if (!isAdded()) return;
        String filePath = Environment.getExternalStorageDirectory().getPath() +
                "/Download/" + DatabaseAccessHelper.DATABASE_NAME;

        @IdRes final int editId = 1;
        // create dialog
        DialogBuilder dialog = new DialogBuilder(getContext());
        dialog.setTitle(titleId);
        dialog.addEdit(editId, filePath, getString(R.string.File_path));
        dialog.addButtonLeft(getString(R.string.CANCEL), null);
        dialog.addButtonRight(getString(R.string.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Window window = ((Dialog) dialog).getWindow();
                if (window != null) {
                    TextView textView = (TextView) window.findViewById(editId);
                    if (textView != null) {
                        listener.onEditorAction(textView, 0, null);
                    }
                }
            }
        });
        dialog.show();
    }

    // Exports data file to the passed path
    private boolean exportDataFile(String dstFilePath) {
        if (!Permissions.isGranted(getContext(), Permissions.WRITE_EXTERNAL_STORAGE)) {
            return false;
        }

        // get source file
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
        if (db == null) {
            return false;
        }
        File srcFile = new File(db.getReadableDatabase().getPath());

        // check destination file
        File dstFile = new File(dstFilePath);
        if (dstFile.getParent() == null) {
            toast(R.string.Error_invalid_file_path);
            return false;
        }
        // create destination file path
        if (!Utils.makeFilePath(dstFile)) {
            toast(R.string.Error_on_file_path_creating);
            return false;
        }
        // copy source file to destination
        if (!Utils.copyFile(srcFile, dstFile)) {
            toast(R.string.Error_on_file_writing);
            return false;
        }

        toast(R.string.Export_complete);

        return true;
    }

    // Imports data file from the passed path
    private boolean importDataFile(String srcFilePath) {
        if (!Permissions.isGranted(getContext(), Permissions.WRITE_EXTERNAL_STORAGE)) {
            return false;
        }

        // check source file
        if (!DatabaseAccessHelper.isSQLiteFile(srcFilePath)) {
            toast(R.string.Error_file_is_not_valid);
            return false;
        }
        // get source file
        File srcFile = new File(srcFilePath);

        // get destination file
        DatabaseAccessHelper db = DatabaseAccessHelper.getInstance(getContext());
        if (db == null) {
            return false;
        }
        File dstFile = new File(db.getReadableDatabase().getPath());
        // clear cache
        DatabaseAccessHelper.invalidateCache();
        // remove the old file
        if (dstFile.exists() && !dstFile.delete()) {
            toast(R.string.Error_on_old_data_deletion);
            return false;
        }
        // copy source file to destination
        if (!Utils.copyFile(srcFile, dstFile)) {
            toast(R.string.Error_on_file_writing);
            return false;
        }

        toast(R.string.Import_complete);

        return true;
    }

    // Shows toast
    private void toast(@StringRes int messageId) {
        Toast.makeText(getContext(), messageId, Toast.LENGTH_SHORT).show();
    }

    // Restarts the current app and opens settings fragment
    private void restartApp() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(MainActivity.ACTION_SETTINGS);
        intent.putExtra(LIST_POSITION, listView.getFirstVisiblePosition());
        startActivity(intent);
        getActivity().finish();
    }
}