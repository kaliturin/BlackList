package com.kaliturin.blacklist;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.MenuItem;

import com.kaliturin.blacklist.DatabaseAccessHelper.ContactNumber;

/**
 * Some utils methods
 */

public class Utils {
    /** Tints menu icon
     */
    public static void tintMenuIcon(Context context, MenuItem item, @ColorRes int colorRes) {
        int color = ContextCompat.getColor(context, colorRes);
        Drawable drawable = DrawableCompat.wrap(item.getIcon());
        DrawableCompat.setTint(drawable, color);
        item.setIcon(drawable);
    }

    /** Replaces found number's metadata with text information
     */
    public static String translateNumberMetadata(Context context, String number) {
        if(number != null) {
            if (number.startsWith(ContactNumber.STARTS_WITH)) {
                return context.getString(R.string.starts_with) + " " +
                        number.substring(ContactNumber.STARTS_WITH.length());
            }

            if (number.endsWith(ContactNumber.ENDS_WITH)) {
                return context.getString(R.string.ends_with) + " " +
                        number.substring(0, number.length() - ContactNumber.ENDS_WITH.length());
            }
        }

        return number;
    }
}
