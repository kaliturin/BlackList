package com.kaliturin.blacklist;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * Settings array adapter
 */

public class SettingsArrayAdapter extends ArrayAdapter<SettingsArrayAdapter.Model> {
    private RowOnClickListener rowOnClickListener = new RowOnClickListener();

    public SettingsArrayAdapter(Context context) {
        super(context, 0);
    }

    @Override
    @NonNull
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        // get model by position
        Model model = getItem(position);
        // create row by model
        if(model != null) {
            if (model.property == null) {
                convertView = inflater.inflate(R.layout.settings_title, parent, false);
            } else {
                convertView = inflater.inflate(R.layout.settings_row, parent, false);
            }
            setModel(convertView, model);
        }

        return convertView;
    }

    private void setModel(View row, Model model) {
        TextView title = (TextView) row.findViewById(R.id.text);
        title.setText(model.title);
        CheckBox cb = (CheckBox) row.findViewById(R.id.cb);
        if(cb != null) {
            cb.setChecked(model.isChecked);
        }
        row.setTag(model);
        row.setOnClickListener(rowOnClickListener);
    }

    class Model {
        private final String title;
        private final String property;
        private boolean isChecked;

        private Model(String title, String property, boolean isChecked) {
            this.title = title;
            this.property = property;
            this.isChecked = isChecked;
        }
    }

    public void addModel(String title, String property) {
        boolean isChecked = false;
        if(property != null) {
            isChecked = Settings.getBooleanValue(getContext(), property);
        }
        Model model = new Model(title, property, isChecked);
        add(model);
    }

    public void addModel(@StringRes int titleId, String property) {
        String title = getContext().getString(titleId);
        addModel(title, property);
    }

    public void addModel(@StringRes int titleId) {
        String title = getContext().getString(titleId);
        addModel(title, null);
    }

    private class RowOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View row) {
            CheckBox cb = (CheckBox) row.findViewById(R.id.cb);
            if(cb != null) {
                Model model = (Model) row.getTag();
                model.isChecked = !model.isChecked;
                cb.setChecked(model.isChecked);
                if(model.property != null) {
                    Settings.setBooleanValue(getContext(), model.property, model.isChecked);
                }
            }
        }
    }
}
