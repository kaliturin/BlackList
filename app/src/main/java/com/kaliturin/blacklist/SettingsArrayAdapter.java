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
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Settings list array adapter
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
                    layoutId = R.layout.row_title;
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

    private void addModel(int type, @StringRes int titleId, @StringRes int commentId,
                          String property, View.OnClickListener listener) {
        boolean isChecked = (property != null && Settings.getBooleanValue(getContext(), property));
        add(new Model(type, getString(titleId), getString(commentId), property, isChecked, listener));
    }

    void addTitle(@StringRes int titleId) {
        addModel(Model.TITLE, titleId, 0, null, null);
    }

    void addCheckbox(@StringRes int titleId, @StringRes int commentId, boolean isChecked,
                     View.OnClickListener listener) {
        add(new Model(Model.CHECKBOX, getString(titleId), getString(commentId), null, isChecked, listener));
    }

    void addCheckbox(@StringRes int titleId, @StringRes int commentId, String property,
                     View.OnClickListener listener) {
        addModel(Model.CHECKBOX, titleId, commentId, property, listener);
    }

    void addCheckbox(@StringRes int titleId, @StringRes int commentId, String property) {
        addCheckbox(titleId, commentId, property, null);
    }

    void addButton(@StringRes int titleId, @StringRes int commentId, View.OnClickListener listener) {
        add(new Model(Model.BUTTON, getString(titleId), getString(commentId), null, false, listener));
    }

    @Nullable
    private String getString(@StringRes int stringRes) {
        return (stringRes != 0 ? getContext().getString(stringRes) : null);
    }

    // Row item data
    class Model {
        private static final int TITLE = 1;
        private static final int CHECKBOX = 2;
        private static final int BUTTON = 3;

        final int type;
        final String title;
        final String comment;
        final String property;
        final View.OnClickListener listener;
        private boolean checked;

        Model(int type, String title, String comment, String property,
                      boolean checked, View.OnClickListener listener) {
            this.type = type;
            this.title = title;
            this.comment = comment;
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

        ViewHolder(View rowView, Model model, int position) {
            this.rowView = rowView;
            this.model = model;

            rowView.setTag(this);

            // title
            TextView titleView = (TextView) rowView.findViewById(R.id.text_title);
            if(titleView != null) {
                titleView.setText(model.title);
            }

            // comment
            if(model.comment != null) {
                TextView commentView = (TextView) rowView.findViewById(R.id.text_comment);
                if (commentView != null) {
                    commentView.setText(model.comment);
                    commentView.setVisibility(View.VISIBLE);
                }
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

            // title
            if(model.type == Model.TITLE) {
                if(position == 0) {
                    View borderView = rowView.findViewById(R.id.top_border);
                    if(borderView != null) {
                        borderView.setVisibility(View.GONE);
                    }
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
