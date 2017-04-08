package com.kaliturin.blacklist;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
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

    // Triggers row checked status
    void triggerRowChecked(View view) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        if(viewHolder != null) {
            viewHolder.trigger();
        }
    }

    // Returns true if row is checked
    boolean isRowChecked(View view) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        return (viewHolder != null && viewHolder.isChecked());
    }

    // Sets row checked
    void setRowChecked(View view, boolean checked) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        if(viewHolder != null) {
            viewHolder.setChecked(checked);
        }
    }

    // Sets row checked by property name
    void setRowChecked(String property, boolean checked) {
        for(int i=0; i<rowsArray.size(); i++) {
            ViewHolder viewHolder = rowsArray.valueAt(i);
            if(viewHolder.model.property != null &&
                    viewHolder.model.property.equals(property)) {
                viewHolder.setChecked(checked);
            }
        }
    }

    // Returns property name from row's model
    @Nullable
    String getRowProperty(View view) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        return (viewHolder != null ? viewHolder.model.property : null);
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
            if(property != null) {
                Settings.setBooleanValue(getContext(), property, checked);
            }
        }
    }

    // Row view holder
    class ViewHolder {
        final Model model;
        final View rowView;
        final CheckBox checkBox;
        final int position;
        final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(model.listener != null) {
                    model.listener.onClick(rowView);
                } else {
                    trigger();
                }
            }
        };

        ViewHolder(View rowView, Model model, final int position) {
            this.rowView = rowView;
            this.model = model;
            this.position = position;

            rowView.setTag(this);

            // title
            TextView titleView = (TextView) rowView.findViewById(R.id.text);
            if(titleView != null) {
                titleView.setText(model.title);
            }

            // checkbox
            if(model.type == Model.CHECKBOX) {
                checkBox = (CheckBox) rowView.findViewById(R.id.cb);
                if(checkBox != null) {
                    checkBox.setVisibility(View.VISIBLE);
                    checkBox.setChecked(model.isChecked());
                    checkBox.setOnClickListener(listener);
                }
            } else {
                checkBox = null;
            }

            // button
            if(model.type == Model.BUTTON) {
                rowView.setOnClickListener(listener);
                // set row's background drawable
                int drawableRes = Utils.getResourceId(getContext(), R.attr.selector_control);
                Utils.setDrawable(getContext(), rowView, drawableRes);
                // set row's image
                ImageView imageView = (ImageView) rowView.findViewById(R.id.image);
                if(imageView != null) {
                    imageView.setVisibility(View.VISIBLE);
                }
            }
        }

        void setChecked(boolean checked) {
            if(checkBox != null) {
                checkBox.setChecked(checked);
                model.setChecked(checked);
            }
        }

        boolean isChecked() {
            return model.isChecked();
        }

        void trigger() {
            setChecked(!isChecked());
        }
    }
}
