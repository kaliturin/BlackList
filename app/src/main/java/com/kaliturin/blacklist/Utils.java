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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Some utils methods
 */

public class Utils {
    static final String TAG = Utils.class.getName();

    /** Tints menu icon **/
    public static void setMenuIconTint(Context context, MenuItem item, @ColorRes int colorRes) {
        Drawable drawable = DrawableCompat.wrap(item.getIcon());
        drawable.mutate();
        setDrawableTint(context, drawable, colorRes);
    }

    /** Sets the tint color of the drawable **/
    public static void setDrawableTint(Context context, Drawable drawable, @ColorRes int colorRes) {
        int color = ContextCompat.getColor(context, colorRes);
        DrawableCompat.setTint(drawable, color);
    }

    /** Sets the background color of the drawable **/
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

    /** Sets drawable for the view **/
    @SuppressWarnings("deprecation")
    public static void setDrawable(Context context, View view, @DrawableRes int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackgroundDrawable(drawable);
        } else {
            view.setBackground(drawable);
        }
    }

    /** Copies passed text to clipboard **/
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

    /** Copies file from source to destination **/
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
            if(closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }

    /** Makes file path if it doesn't exist **/
    public static boolean makeFilePath(File file) {
        String parent = file.getParent();
        if(parent != null) {
            File dir = new File(parent);
            try {
                if(!dir.exists() && !dir.mkdirs()) {
                    throw new SecurityException("File.mkdirs() returns false");
                }
            } catch (SecurityException e) {
                Log.w(TAG, e);
                return false;
            }
        }

        return true;
    }
}
