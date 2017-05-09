/*
 * Copyright 2017 Anton Kaliturin
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

package com.kaliturin.blacklist.adapters;

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

import com.kaliturin.blacklist.R;

/**
 * Information list array adapter
 */

public class InformationArrayAdapter extends ArrayAdapter<InformationArrayAdapter.Model> {
    private SparseArray<ViewHolder> rowsArray = new SparseArray<>();

    public InformationArrayAdapter(Context context) {
        super(context, 0);
    }

    @Override
    @NonNull
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // get created row by position
        View rowView;
        ViewHolder viewHolder = rowsArray.get(position);
        if (viewHolder != null) {
            rowView = viewHolder.rowView;
        } else {
            // get model by position
            Model model = getItem(position);
            // get row layout
            int layoutId = R.layout.row_info;
            if (model != null) {
                if (model.type == Model.TITLE) {
                    layoutId = R.layout.row_title;
                }
            }
            // create row
            LayoutInflater inflater = LayoutInflater.from(getContext());
            rowView = inflater.inflate(layoutId, parent, false);
            if (model != null) {
                viewHolder = new ViewHolder(rowView, model, position);
                rowsArray.put(position, viewHolder);
            }
        }

        return rowView;
    }

    private void addModel(int type, @StringRes int titleId, @StringRes int commentId) {
        add(new Model(type, getString(titleId), getString(commentId)));
    }

    public void addTitle(@StringRes int titleId) {
        addModel(Model.TITLE, titleId, 0);
    }

    public void addText(@StringRes int commentId) {
        addText(null, getString(commentId));
    }

    public void addText(String title, String comment) {
        add(new Model(Model.TEXT, title, comment));
    }

    @Nullable
    private String getString(@StringRes int stringRes) {
        return (stringRes != 0 ? getContext().getString(stringRes) : null);
    }

    // Row item data
    class Model {
        private static final int TITLE = 1;
        private static final int TEXT = 2;

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
    private class ViewHolder {
        final Model model;
        final View rowView;

        ViewHolder(View rowView, Model model, int position) {
            this.rowView = rowView;
            this.model = model;
            rowView.setTag(this);

            // title
            TextView titleView = (TextView) rowView.findViewById(R.id.text_title);
            if (titleView != null) {
                if (model.title != null) {
                    titleView.setText(model.title);
                    titleView.setVisibility(View.VISIBLE);
                } else {
                    titleView.setVisibility(View.GONE);
                }
            }
            if (model.type == Model.TITLE) {
                if (position == 0) {
                    View borderView = rowView.findViewById(R.id.top_border);
                    if (borderView != null) {
                        borderView.setVisibility(View.GONE);
                    }
                }
            }

            // comment
            if (model.comment != null) {
                TextView commentView = (TextView) rowView.findViewById(R.id.text_comment);
                if (commentView != null) {
                    commentView.setText(model.comment);
                    commentView.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
