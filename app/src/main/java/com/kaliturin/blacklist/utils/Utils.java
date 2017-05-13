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
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RotateDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.AttrRes;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.kaliturin.blacklist.R;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Some utils methods
 */

public class Utils {
    private static final String TAG = Utils.class.getName();

    /**
     * Tints menu icon
     **/
    public static void setMenuIconTint(Context context, MenuItem item, @AttrRes int colorAttrRes) {
        Drawable drawable = DrawableCompat.wrap(item.getIcon());
        drawable.mutate();
        setDrawableTint(context, drawable, colorAttrRes);
    }

    /**
     * Sets the tint color of the drawable
     **/
    public static void setDrawableTint(Context context, Drawable drawable, @AttrRes int colorAttrRes) {
        int colorRes = getResourceId(context, colorAttrRes);
        int color = ContextCompat.getColor(context, colorRes);
        DrawableCompat.setTint(drawable, color);
    }

    /**
     * Sets the background color of the drawable
     **/
    public static void setDrawableColor(Context context, Drawable drawable, @AttrRes int colorAttrRes) {
        int colorRes = getResourceId(context, colorAttrRes);
        int color = ContextCompat.getColor(context, colorRes);
        if (drawable instanceof ShapeDrawable) {
            ((ShapeDrawable) drawable).getPaint().setColor(color);
        } else if (drawable instanceof GradientDrawable) {
            ((GradientDrawable) drawable).setColor(color);
        } else if (drawable instanceof ColorDrawable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                ((ColorDrawable) drawable).setColor(color);
            }
        } else if (drawable instanceof RotateDrawable) {
            setDrawableColor(context, ((RotateDrawable) drawable).getDrawable(), colorAttrRes);
        }
    }

    /**
     * Sets drawable for the view
     **/
    @SuppressWarnings("deprecation")
    public static void setDrawable(Context context, View view, @DrawableRes int drawableRes) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableRes);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackgroundDrawable(drawable);
        } else {
            view.setBackground(drawable);
        }
    }

    /**
     * Resolves attribute of passed theme returning referenced resource id
     **/
    public static int getResourceId(@AttrRes int attrRes, Resources.Theme theme) {
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attrRes, typedValue, true);
        return typedValue.resourceId;
    }

    /**
     * Resolves current theme's attribute returning referenced resource id
     **/
    public static int getResourceId(Context context, @AttrRes int attrRes) {
        return getResourceId(attrRes, context.getTheme());
    }

    /**
     * Resolves attribute of passed theme returning referenced resource id
     **/
    public static int getResourceId(Context context, @AttrRes int attrRes, @StyleRes int styleRes) {
        Resources.Theme theme = context.getResources().newTheme();
        theme.applyStyle(styleRes, true);
        return getResourceId(attrRes, theme);
    }

//----------------------------------------------------------------------------

    /**
     * Copies passed text to clipboard
     **/
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
                        android.content.ClipData.newPlainText("", text);
                clipboard.setPrimaryClip(clip);
            }
            return true;
        } catch (Exception e) {
            Log.w(TAG, e);
            return false;
        }
    }

    /**
     * Copies file from source to destination
     **/
    public static boolean copyFile(File src, File dst) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        } catch (IOException e) {
            Log.w(TAG, e);
            return false;
        } finally {
            close(in);
            close(out);
        }

        return true;
    }

    public static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }

    /**
     * Makes file path if it doesn't exist
     **/
    public static boolean makeFilePath(File file) {
        String parent = file.getParent();
        if (parent != null) {
            File dir = new File(parent);
            try {
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new SecurityException("File.mkdirs() returns false");
                }
            } catch (SecurityException e) {
                Log.w(TAG, e);
                return false;
            }
        }

        return true;
    }

    /**
     * Makes and shows threadsafe toast
     */
    public static void showToast(final Context context, final String message, final int duration) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context.getApplicationContext(), message, duration).show();
            }
        });
    }

    /**
     * Scales passed view with passed dimension on Tablets only
     */
    public static void scaleViewOnTablet(Context context, View view, @DimenRes int dimenRes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            boolean isTablet = context.getResources().getBoolean(R.bool.isTablet);
            if (isTablet) {
                TypedValue outValue = new TypedValue();
                context.getResources().getValue(dimenRes, outValue, true);
                float scale = outValue.getFloat();
                view.setScaleX(scale);
                view.setScaleY(scale);
            }
        }
    }

    /**
     * Returns year-less date format
     */
    @SuppressWarnings("SimpleDateFormat")
    public static DateFormat getYearLessDateFormat(DateFormat dateFormat) {
        if (dateFormat instanceof SimpleDateFormat) {
            // creating year less date format
            String fullPattern = ((SimpleDateFormat)dateFormat).toPattern();
            // checking 'de' we omit problems with Spain locale
            String regex = fullPattern.contains("de") ? "[^Mm]*[Yy]+[^Mm]*" : "[^DdMm]*[Yy]+[^DdMm]*";
            String yearLessPattern = fullPattern.replaceAll(regex, "");
            return new SimpleDateFormat(yearLessPattern);
        }
        return dateFormat;
    }
}
