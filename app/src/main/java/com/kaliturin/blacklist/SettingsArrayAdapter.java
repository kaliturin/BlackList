package com.kaliturin.blacklist;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.SparseArray;
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
    private SparseArray<View> rowsArray = new SparseArray<>();

    public SettingsArrayAdapter(Context context) {
        super(context, 0);
    }

    @Override
    @NonNull
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        // get created row by position
        View row = rowsArray.get(position);
        if(row == null) {
            // get model by position
            Model model = getItem(position);
            if (model != null) {
                // create row by model
                LayoutInflater inflater = LayoutInflater.from(getContext());
                if (model.property == null && model.listener == null) {
                    row = inflater.inflate(R.layout.row_title_settings, parent, false);
                } else {
                    row = inflater.inflate(R.layout.row_settings, parent, false);
                }
                setModel(row, model);
                rowsArray.put(position, row);
            }
        }

        return row;
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
