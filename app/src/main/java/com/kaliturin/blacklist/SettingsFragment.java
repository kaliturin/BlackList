package com.kaliturin.blacklist;


import android.app.Activity;
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
public class SettingsFragment extends Fragment {
    public static String TITLE = "TITLE";

    View.OnClickListener defaultSMSAppListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Activity activity = SettingsFragment.this.getActivity();
            DefaultSMSAppHelper.askForDefaultAppChange(activity, 0);
            Permissions.reset();
        }
    };

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle arguments = getArguments();
        if(arguments != null) {
            String title = arguments.getString(TITLE);
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(title);
            }
        }
    }

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Permissions.notifyIfNotGranted(getActivity(), Permissions.WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Create list adapter and fill it with data
        SettingsArrayAdapter adapter = new SettingsArrayAdapter(getContext());
        adapter.addModel(R.string.SMS);

        // default sms feature is available
        boolean isAvailable = DefaultSMSAppHelper.isAvailable();
        // current app is default sms app
        boolean isDefault = DefaultSMSAppHelper.isDefault(getContext());

        if(isAvailable) {
            // show sms default app switch
            adapter.addModel(R.string.set_as_default_sms_app, isDefault, defaultSMSAppListener);
        }

        if (!isAvailable || isDefault) {
            // show sms settings
            adapter.addModel(R.string.block_all_sms, Settings.BLOCK_ALL_SMS);
            adapter.addModel(R.string.block_sms, Settings.BLOCK_SMS_FROM_BLACK_LIST);
            adapter.addModel(R.string.block_hidden_sms, Settings.BLOCK_HIDDEN_SMS);
            adapter.addModel(R.string.block_sms_not_from_contacts, Settings.BLOCK_SMS_NOT_FROM_CONTACTS);
            adapter.addModel(R.string.block_sms_not_from_inbox, Settings.BLOCK_SMS_NOT_FROM_INBOX);
            adapter.addModel(R.string.show_sms_notifications, Settings.SHOW_SMS_NOTIFICATIONS);
            adapter.addModel(R.string.write_sms_journal, Settings.WRITE_SMS_JOURNAL);
        }

        // show calls settings
        adapter.addModel(R.string.CALLS);
        adapter.addModel(R.string.block_all_calls, Settings.BLOCK_ALL_CALLS);
        adapter.addModel(R.string.block_calls, Settings.BLOCK_CALLS_FROM_BLACK_LIST);
        adapter.addModel(R.string.block_hidden_calls, Settings.BLOCK_HIDDEN_CALLS);
        adapter.addModel(R.string.block_calls_not_from_contacts, Settings.BLOCK_CALLS_NOT_FROM_CONTACTS);
        adapter.addModel(R.string.block_calls_not_from_sms_inbox, Settings.BLOCK_CALLS_NOT_FROM_SMS_INBOX);
        adapter.addModel(R.string.show_calls_notifications, Settings.SHOW_CALLS_NOTIFICATIONS);
        adapter.addModel(R.string.write_calls_journal, Settings.WRITE_CALLS_JOURNAL);

        View view = getView();
        if(view != null) {
            ListView listView = (ListView) view.findViewById(R.id.settings_list);
            listView.setAdapter(adapter);
        }
    }
}

