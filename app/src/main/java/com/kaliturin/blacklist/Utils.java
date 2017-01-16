package com.kaliturin.blacklist;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.MenuItem;

/**
 * Some utils methods
 */

public class Utils {
    // Tints menu icon
    public static void tintMenuIcon(Context context, MenuItem item, @ColorRes int colorRes) {
        int color = ContextCompat.getColor(context, colorRes);
        Drawable drawable = DrawableCompat.wrap(item.getIcon());
        DrawableCompat.setTint(drawable, color);
        item.setIcon(drawable);
    }
}
