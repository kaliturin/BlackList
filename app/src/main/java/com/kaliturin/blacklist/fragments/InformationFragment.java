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


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.kaliturin.blacklist.R;
import com.kaliturin.blacklist.adapters.InformationArrayAdapter;

/**
 * Information fragment
 */
public class InformationFragment extends Fragment implements FragmentArguments {
    private static final String TAG = InformationFragment.class.getName();
    private ListView listView = null;
    private int listPosition = 0;

    public InformationFragment() {
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
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_information, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView = (ListView) view.findViewById(R.id.help_list);

        InformationArrayAdapter adapter = new InformationArrayAdapter(getContext());

        adapter.addTitle(R.string.About);
        String title = getString(R.string.app_name) + " (v" + getAppVersion() + ")";
        adapter.addText(title, getString(R.string.Info_about));

        adapter.addTitle(R.string.Attention);
        adapter.addText(R.string.Info_attention);

        adapter.addTitle(R.string.Messaging);
        adapter.addText(R.string.Info_messaging);

        adapter.addTitle(R.string.Black_list);
        adapter.addText(R.string.Info_black_list);

        adapter.addTitle(R.string.White_list);
        adapter.addText(R.string.Info_white_list);

        adapter.addTitle(R.string.Journal);
        adapter.addText(R.string.Info_journal);

        adapter.addTitle(R.string.Settings);
        adapter.addText(R.string.Info_settings);

        adapter.addTitle(R.string.Licence);
        adapter.addText(R.string.Info_licence);

        adapter.addTitle(R.string.Author);
        adapter.addText(R.string.Info_author);

        // add adapter to the ListView and scroll list to position
        listView.setAdapter(adapter);
        listView.post(new Runnable() {
            @Override
            public void run() {
                listView.setSelection(listPosition);
            }
        });
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

    @Nullable
    String getAppVersion() {
        Context context = getContext().getApplicationContext();
        if (context != null) {
            String packageName = context.getPackageName();
            try {
                PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
                return info.versionName;
            } catch (PackageManager.NameNotFoundException ex) {
                Log.w(TAG, ex);
            }
        }
        return null;
    }
}
