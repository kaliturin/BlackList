package com.kaliturin.blacklist.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

/**
 * {@link LinearLayout} with clickable behavior
 */

public class CheckableLinearLayout extends LinearLayout implements Checkable {
    private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};

    private boolean checked = false;

    public CheckableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean b) {
        if (b != checked) {
            checked = b;
            refreshDrawableState();
        }
    }

    public void toggle() {
        setChecked(!checked);
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }

        return drawableState;
    }
}