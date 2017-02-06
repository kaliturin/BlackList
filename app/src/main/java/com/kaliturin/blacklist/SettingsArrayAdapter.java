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
        if (model != null) {
            if (model.property == null && model.listener == null) {
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
        if (cb != null) {
            cb.setChecked(model.isChecked);
        }
        row.setTag(model);
        row.setOnClickListener(rowOnClickListener);
    }

    public class Model {
        private final String title;
        private final String property;
        private boolean isChecked;
        private View.OnClickListener listener;

        private Model(String title, String property,
                      View.OnClickListener listener, boolean isChecked) {
            this.title = title;
            this.property = property;
            this.isChecked = isChecked;
            this.listener = listener;
        }
    }

    public void addModel(String title, String property, View.OnClickListener listener) {
        boolean isChecked = false;
        if (property != null) {
            isChecked = Settings.getBooleanValue(getContext(), property);
        }
        Model model = new Model(title, property, listener, isChecked);
        add(model);
    }

    public void addModel(@StringRes int titleId, boolean isChecked, View.OnClickListener listener) {
        String title = getContext().getString(titleId);
        Model model = new Model(title, null, listener, isChecked);
        add(model);
    }

    public void addModel(@StringRes int titleId, String property) {
        String title = getContext().getString(titleId);
        addModel(title, property, null);
    }

    public void addModel(@StringRes int titleId) {
        String title = getContext().getString(titleId);
        addModel(title, null, null);
    }

    private class RowOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View row) {
            Model model = (Model) row.getTag();
            CheckBox cb = (CheckBox) row.findViewById(R.id.cb);
            if (model.listener != null) {
                model.listener.onClick(row);
            } else {
                if (cb != null) {
                    model.isChecked = !model.isChecked;
                    cb.setChecked(model.isChecked);
                    if (model.property != null) {
                        Settings.setBooleanValue(getContext(), model.property, model.isChecked);
                    }
                }
            }
        }
    }
}
