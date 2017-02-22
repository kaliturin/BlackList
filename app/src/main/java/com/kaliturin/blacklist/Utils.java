package com.kaliturin.blacklist;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.MenuItem;

import com.kaliturin.blacklist.DatabaseAccessHelper.ContactNumber;

/**
 * Some utils methods
 */

class Utils {
    /** Tints menu icon
     */
    static void setMenuIconTint(Context context, MenuItem item, @ColorRes int colorRes) {
        Drawable drawable = DrawableCompat.wrap(item.getIcon());
        drawable.mutate();
        setDrawableTint(context, drawable, colorRes);
    }

    /** Sets the tint color of the drawable
     */
    static void setDrawableTint(Context context, Drawable drawable, @ColorRes int colorRes) {
        int color = ContextCompat.getColor(context, colorRes);
        DrawableCompat.setTint(drawable, color);
    }

    /** Sets the background color of the drawable
     */
    static void setDrawableColor(Context context, Drawable drawable, @ColorRes int colorRes) {
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

//    /** Returns app name
//     */
//    static String getApplicationName(Context context) {
//        ApplicationInfo info = context.getApplicationInfo();
//        int id = info.labelRes;
//        return id == 0 ? info.nonLocalizedLabel.toString() : context.getString(id);
//    }
}
