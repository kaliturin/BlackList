package com.kaliturin.blacklist;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

/**
 * Some utils methods
 */

public class Utils {
    static final String TAG = Utils.class.getName();

    /** Tints menu icon
     */
    public static void setMenuIconTint(Context context, MenuItem item, @ColorRes int colorRes) {
        Drawable drawable = DrawableCompat.wrap(item.getIcon());
        drawable.mutate();
        setDrawableTint(context, drawable, colorRes);
    }

    /** Sets the tint color of the drawable
     */
    public static void setDrawableTint(Context context, Drawable drawable, @ColorRes int colorRes) {
        int color = ContextCompat.getColor(context, colorRes);
        DrawableCompat.setTint(drawable, color);
    }

    /** Sets the background color of the drawable
     */
    public static void setDrawableColor(Context context, Drawable drawable, @ColorRes int colorRes) {
        int color = ContextCompat.getColor(context, colorRes);
        if (drawable instanceof ShapeDrawable) {
            ((ShapeDrawable)drawable).getPaint().setColor(color);
        } else
        if (drawable instanceof GradientDrawable) {
            ((GradientDrawable)drawable).setColor(color);
        } else
        if (drawable instanceof ColorDrawable) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                ((ColorDrawable) drawable).setColor(color);
            }
        }
    }

    /** Sets drawable for the view
     */
    @SuppressWarnings("deprecation")
    public static void setDrawable(Context context, View view, @DrawableRes int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackgroundDrawable(drawable);
        } else {
            view.setBackground(drawable);
        }
    }

    /** Copies passed text to clipboard
     */
    @SuppressWarnings("deprecation")
    public static boolean copyTextToClipboard(Context context, String text) {
        try {
            if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                android.text.ClipboardManager clipboard =
                        (android.text.ClipboardManager) context
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setText(text);
            } else {
                android.content.ClipboardManager clipboard =
                        (android.content.ClipboardManager)
                        context.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip =
                        android.content.ClipData.newPlainText(
                                context.getResources().getString(R.string.Message), text);
                clipboard.setPrimaryClip(clip);
            }
            return true;
        } catch (Exception e) {
            Log.w(TAG, e);
            return false;
        }
    }
}
