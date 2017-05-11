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

package com.kaliturin.blacklist.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Flow layout allows to place views into layout in line and wraps to
 * the next line if there is no space in the current one
 **/
public class FlowLayout extends ViewGroup {
    private int lineHeight;

    public static class FlowLayoutParams extends ViewGroup.LayoutParams {
        final int hSpacing;
        final int vSpacing;

        FlowLayoutParams(int hSpacing, int vSpacing, ViewGroup.LayoutParams viewGroupLayout) {
            super(viewGroupLayout);
            this.hSpacing = hSpacing;
            this.vSpacing = vSpacing;
        }

        FlowLayoutParams(int hSpacing, int vSpacing) {
            super(0, 0);
            this.hSpacing = hSpacing;
            this.vSpacing = vSpacing;
        }
    }

    public FlowLayout(Context context) {
        super(context);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            throw new AssertionError("Measure mode isn't specified");
        }

        final int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int height = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();
        final int count = getChildCount();
        int lineHeight = 0;

        int xPos = getPaddingLeft();
        int yPos = getPaddingTop();

        int childHeightMeasureSpec;
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
        } else {
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final FlowLayoutParams lp = (FlowLayoutParams) child.getLayoutParams();
                child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), childHeightMeasureSpec);
                final int childWidth = child.getMeasuredWidth();
                lineHeight = Math.max(lineHeight, child.getMeasuredHeight() + lp.vSpacing);

                if (xPos + childWidth > width) {
                    xPos = getPaddingLeft();
                    yPos += lineHeight;
                }

                xPos += childWidth + lp.hSpacing;
            }
        }

        this.lineHeight = lineHeight;

        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            height = yPos + lineHeight;
        } else if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
            if (yPos + lineHeight < height) {
                height = yPos + lineHeight;
            }
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        // 1px spacing
        return new FlowLayoutParams(1, 1);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams params) {
        return new FlowLayoutParams(1, 1, params);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams params) {
        return (params instanceof FlowLayoutParams);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int count = getChildCount();
        final int width = r - l;
        int xPos = getPaddingLeft();
        int yPos = getPaddingTop();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final int childWidth = child.getMeasuredWidth();
                final int childHeight = child.getMeasuredHeight();
                final FlowLayoutParams lp = (FlowLayoutParams) child.getLayoutParams();
                if (xPos + childWidth > width) {
                    xPos = getPaddingLeft();
                    yPos += lineHeight;
                }
                child.layout(xPos, yPos, xPos + childWidth, yPos + childHeight);
                xPos += childWidth + lp.hSpacing;
            }
        }
    }
}