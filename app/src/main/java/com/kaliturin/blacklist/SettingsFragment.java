package com.kaliturin.blacklist;


import android.app.Activity;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;


/**
 * Settings fragment
 */
public class SettingsFragment extends Fragment implements FragmentArguments {
    private static final String VISIBLE_ROW_POSITION = "VISIBLE_ROW_POSITION";
    private static final int BLOCKED_SMS = 1;
    private static final int RECEIVED_SMS = 2;
    private static final int BLOCKED_CALL = 3;
    private SettingsArrayAdapter adapter = null;
    private int visibleRowPosition = 0;
    private ListView listView = null;

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
        if(savedInstanceState != null) {
            visibleRowPosition =  savedInstanceState.getInt(VISIBLE_ROW_POSITION, 0);
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Permissions.notifyIfNotGranted(getContext(), Permissions.WRITE_EXTERNAL_STORAGE);

        listView = (ListView) view.findViewById(R.id.settings_list);
    }

    // Array adapter is rebuilt here because we need to update rows after
    // we've got a result of the default SMS app changing dialog.
    @Override
    public void onResume() {
        super.onResume();

        // Create list adapter and fill it with data
        adapter = new SettingsArrayAdapter(getContext());

        // default sms feature is available
        boolean isSmsDefaultAvailable = DefaultSMSAppHelper.isAvailable();
        // current app is default sms app
        boolean isSmsDefault = DefaultSMSAppHelper.isDefault(getContext());

        if(isSmsDefaultAvailable) {
            // show sms default app switch
            adapter.addTitle(R.string.SMS_default_app);
            adapter.addCheckbox(R.string.set_as_default_sms_app, isSmsDefault,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Activity activity = SettingsFragment.this.getActivity();
                            DefaultSMSAppHelper.askForDefaultAppChange(activity, 0);
                            Permissions.invalidateCache();
                        }
                    });
        }

        if (!isSmsDefaultAvailable || isSmsDefault) {
            // sms blocking settings
            adapter.addTitle(R.string.SMS_blocking);
            adapter.addCheckbox(R.string.block_all_sms, Settings.BLOCK_ALL_SMS);
            adapter.addCheckbox(R.string.block_sms, Settings.BLOCK_SMS_FROM_BLACK_LIST);
            adapter.addCheckbox(R.string.block_hidden_sms, Settings.BLOCK_HIDDEN_SMS);
            adapter.addCheckbox(R.string.block_sms_not_from_contacts, Settings.BLOCK_SMS_NOT_FROM_CONTACTS);
            adapter.addCheckbox(R.string.block_sms_not_from_inbox, Settings.BLOCK_SMS_NOT_FROM_INBOX);
            adapter.addCheckbox(R.string.write_sms_journal, Settings.WRITE_SMS_JOURNAL);

            // sms notifications settings
            adapter.addTitle(R.string.SMS_notification);
            adapter.addCheckbox(R.string.Notify_with_statusbar_blocked_SMS, Settings.BLOCKED_SMS_STATUS_NOTIFICATION,
                    new DependentRowOnClickListener());
            adapter.addCheckbox(R.string.Notify_with_sound_blocked_SMS, Settings.BLOCKED_SMS_SOUND_NOTIFICATION,
                    new RingtonePickerOnClickListener(BLOCKED_SMS));
            adapter.addCheckbox(R.string.Notify_with_vibration_blocked_SMS, Settings.BLOCKED_SMS_VIBRATION_NOTIFICATION,
                    new DependentRowOnClickListener());
            adapter.addCheckbox(R.string.Notify_with_sound_received_SMS, Settings.RECEIVED_SMS_SOUND_NOTIFICATION,
                    new RingtonePickerOnClickListener(RECEIVED_SMS));
            adapter.addCheckbox(R.string.Notify_with_vibration_received_SMS, Settings.RECEIVED_SMS_VIBRATION_NOTIFICATION);
        }

        // calls blocking settings
        adapter.addTitle(R.string.Calls_blocking);
        adapter.addCheckbox(R.string.block_all_calls, Settings.BLOCK_ALL_CALLS);
        adapter.addCheckbox(R.string.block_calls, Settings.BLOCK_CALLS_FROM_BLACK_LIST);
        adapter.addCheckbox(R.string.block_hidden_calls, Settings.BLOCK_HIDDEN_CALLS);
        adapter.addCheckbox(R.string.block_calls_not_from_contacts, Settings.BLOCK_CALLS_NOT_FROM_CONTACTS);
        adapter.addCheckbox(R.string.block_calls_not_from_sms_inbox, Settings.BLOCK_CALLS_NOT_FROM_SMS_INBOX);
        adapter.addCheckbox(R.string.write_calls_journal, Settings.WRITE_CALLS_JOURNAL);

        // calls notifications settings
        adapter.addTitle(R.string.Calls_notification);
        adapter.addCheckbox(R.string.Notify_with_statusbar_blocked_call, Settings.BLOCKED_CALL_STATUS_NOTIFICATION,
                new DependentRowOnClickListener());
        adapter.addCheckbox(R.string.Notify_with_sound_blocked_call, Settings.BLOCKED_CALL_SOUND_NOTIFICATION,
                new RingtonePickerOnClickListener(BLOCKED_CALL));
        adapter.addCheckbox(R.string.Notify_with_vibration_blocked_call, Settings.BLOCKED_CALL_VIBRATION_NOTIFICATION,
                new DependentRowOnClickListener());

        listView.setAdapter(adapter);
        listView.setSelection(visibleRowPosition);
    }

    @Override
    public void onPause() {
        super.onPause();
        visibleRowPosition = listView.getFirstVisiblePosition();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save first showed row position
        outState.putInt(VISIBLE_ROW_POSITION, listView.getFirstVisiblePosition());
    }

    // Is used for getting result of ringtone picker dialog
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK) {
            // get ringtone url
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            // save url as settings property value
            setRingtoneUri(requestCode, uri);
        }
    }

    // Saves ringtone url as settings property value
    private void setRingtoneUri(int type, Uri uri) {
        String ringtoneProperty = null;
        String soundProperty = null;
        String statusProperty = null;

        switch (type) {
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
        }

        if(ringtoneProperty != null && soundProperty != null) {
            if(uri != null) {
                String uriString = uri.toString();
                Settings.setStringValue(getContext(), ringtoneProperty, uriString);
                Settings.setBooleanValue(getContext(), soundProperty, true);
                if(statusProperty != null) {
                    Settings.setBooleanValue(getContext(), statusProperty, true);
                }
            } else {
                Settings.setBooleanValue(getContext(), soundProperty, false);
            }
        }
    }

    // Returns ringtone url from settings property
    @Nullable
    private Uri getRingtoneUri(int type) {
        String uriString = null;
        switch (type) {
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
    class RingtonePickerOnClickListener implements View.OnClickListener {
        int requestCode;

        RingtonePickerOnClickListener(int requestCode) {
            this.requestCode = requestCode;
        }

        @Override
        public void onClick(View rowView) {
            // get the clicked row position
            if(!adapter.isRowChecked(rowView)) {
                // open ringtone picker dialog
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.Ringtone_picker));
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, getRingtoneUri(requestCode));
                startActivityForResult(intent, requestCode);
            } else {
                adapter.setRowChecked(rowView, false);
            }
        }
    }


    // On row click listener for updating dependent rows
    class DependentRowOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View rowView) {
            // trigger checked row
            adapter.triggerRowChecked(rowView);
            String property = adapter.getRowProperty(rowView);
            if(property == null) {
                return;
            }

            boolean checked = adapter.isRowChecked(rowView);
            if(!checked) {
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
}