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
import android.widget.TextView;

/**
 * Information list array adapter
 */

class InformationArrayAdapter extends ArrayAdapter<InformationArrayAdapter.Model> {
    private SparseArray<ViewHolder> rowsArray = new SparseArray<>();

    InformationArrayAdapter(Context context) {
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

    private void addModel(int type, @StringRes int titleId, @StringRes int commentId) {
        add(new Model(type, getString(titleId), getString(commentId)));
    }

    void addTitle(@StringRes int titleId) {
        addModel(Model.TITLE, titleId, 0);
    }

    void addButton(@StringRes int titleId, @StringRes int commentId) {
        add(new Model(Model.BUTTON, getString(titleId), getString(commentId)));
    }

    @Nullable
    private String getString(@StringRes int stringRes) {
        return (stringRes != 0 ? getContext().getString(stringRes) : null);
    }

    // Row item data
    class Model {
        private static final int TITLE = 1;
        private static final int BUTTON = 2;

        final int type;
        final String title;
        final String comment;

        Model(int type, String title, String comment) {
            this.type = type;
            this.title = title;
            this.comment = comment;
        }
    }

    // Row view holder
    class ViewHolder {
        final Model model;
        final View rowView;

        ViewHolder(View rowView, Model model, int position) {
            this.rowView = rowView;
            this.model = model;
            rowView.setTag(this);

            // title
            TextView titleView = (TextView) rowView.findViewById(R.id.text_title);
            if(titleView != null) {
                titleView.setText(model.title);
            }
            if(model.type == Model.TITLE) {
                if (position == 0) {
                    View borderView = rowView.findViewById(R.id.top_border);
                    if (borderView != null) {
                        borderView.setVisibility(View.GONE);
                    }
                }
            }

            // comment
            if(model.comment != null) {
                TextView commentView = (TextView) rowView.findViewById(R.id.text_comment);
                if (commentView != null) {
                    commentView.setText(model.comment);
                    commentView.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
