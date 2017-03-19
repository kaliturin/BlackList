package com.kaliturin.blacklist;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

class SettingsArrayAdapter extends ArrayAdapter<SettingsArrayAdapter.Model> {
    private SparseArray<ViewHolder> rowsArray = new SparseArray<>();

    SettingsArrayAdapter(Context context) {
        super(context, 0);
    }

    @Override
    @NonNull
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // get created row by position
        View rowView;
        ViewHolder viewHolder = rowsArray.get(position);
        if(viewHolder != null) {
            rowView = viewHolder.rowView;
        } else {
            // get model by position
            Model model = getItem(position);
            // get row layout
            int layoutId = R.layout.row_settings;
            if(model != null) {
                if (model.type == Model.TITLE) {
                    layoutId = R.layout.row_title_settings;
                }
            }
            // create row
            LayoutInflater inflater = LayoutInflater.from(getContext());
            rowView = inflater.inflate(layoutId, parent, false);
            if(model != null) {
                viewHolder = new ViewHolder(rowView, model, position);
                rowsArray.put(position, viewHolder);
            }
        }

        return rowView;
    }

    void invalidateCache() {
        rowsArray.clear();
    }

    int getRowPosition(View rowView) {
        ViewHolder viewHolder = (ViewHolder) rowView.getTag();
        return  (viewHolder != null ? viewHolder.position : -1);
    }

    void setRowChecked(int position, boolean checked) {
        ViewHolder viewHolder = rowsArray.get(position);
        if(viewHolder != null) {
            viewHolder.setChecked(checked);
        }
    }

    boolean isRowChecked(int position) {
        ViewHolder viewHolder = rowsArray.get(position);
        return (viewHolder != null && viewHolder.isChecked());
    }

    private void addModel(int type, @StringRes int titleId, String property, View.OnClickListener listener) {
        boolean isChecked = false;
        if (property != null) {
            isChecked = Settings.getBooleanValue(getContext(), property);
        }
        String title = getContext().getString(titleId);
        add(new Model(type, title, property, isChecked, listener));
    }

    void addTitle(@StringRes int titleId) {
        addModel(Model.TITLE, titleId, null, null);
    }

    void addCheckbox(@StringRes int titleId, boolean isChecked, View.OnClickListener listener) {
        String title = getContext().getString(titleId);
        add(new Model(Model.CHECKBOX, title, null, isChecked, listener));
    }

    void addCheckbox(@StringRes int titleId, String property, View.OnClickListener listener) {
        addModel(Model.CHECKBOX, titleId, property, listener);
    }

    void addCheckbox(@StringRes int titleId, String property) {
        addCheckbox(titleId, property, null);
    }

    void addButton(@StringRes int titleId, View.OnClickListener listener) {
        String title = getContext().getString(titleId);
        add(new Model(Model.BUTTON, title, null, false, listener));
    }

    // Row item data
    class Model {
        private static final int TITLE = 1;
        private static final int CHECKBOX = 2;
        private static final int BUTTON = 3;

        final int type;
        final String title;
        final String property;
        final View.OnClickListener listener;
        private boolean checked;

        Model(int type, String title, String property,
                      boolean checked, View.OnClickListener listener) {
            this.type = type;
            this.title = title;
            this.property = property;
            this.checked = checked;
            this.listener = listener;
        }

        boolean isChecked() {
            return checked;
        }
        void setChecked(boolean checked) {
            this.checked = checked;
        }

    }

    // Row view holder
    class ViewHolder {
        final Model model;
        final View rowView;
        final CheckBox checkBox;
        final int position;

        ViewHolder(View rowView, Model model, final int position) {
            rowView.setTag(this);
            this.rowView = rowView;
            this.model = model;
            this.position = position;

            TextView titleView = (TextView) rowView.findViewById(R.id.text);
            titleView.setText(model.title);
            if(model.type == Model.CHECKBOX) {
                checkBox = (CheckBox) rowView.findViewById(R.id.cb);
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(model.isChecked());
            } else {
                checkBox = null;
            }

            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View rowView) {
                    ViewHolder viewHolder = (ViewHolder) rowView.getTag();
                    Model model = viewHolder.model;
                    if(model.listener != null) {
                        model.listener.onClick(rowView);
                    } else {
                        setChecked(!isChecked());
                    }
                }
            });
        }

        void setChecked(boolean checked) {
            if(model.type == Model.CHECKBOX) {
                checkBox.setChecked(checked);
                model.setChecked(checked);
                if(model.property != null) {
                    Settings.setBooleanValue(getContext(), model.property, checked);
                }
            }
        }

        boolean isChecked() {
            return model.isChecked();
        }
    }
}
