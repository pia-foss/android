/*
 *  Copyright (c) 2020 Private Internet Access, Inc.
 *
 *  This file is part of the Private Internet Access Android Client.
 *
 *  The Private Internet Access Android Client is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  The Private Internet Access Android Client is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with the Private
 *  Internet Access Android Client.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.privateinternetaccess.android.pia.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DrawFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;


/**
 * Created by half47 on 4/13/17.
 */

public class AppUtilities {

    /**
     * Colors text in a string resid.
     *
     * Use <emph> and </emph> to surround the area to be colored.
     *
     * @param c
     * @param resid
     * @param colorId
     * @return
     */
    public static Spanned getColorText(Context c, @StringRes int resid, @ColorRes int colorId) {
        String text = c.getString(resid);
        text = text.replaceAll("<emph>", String.format("<font color=\"#%x\">", 0xffffff & ContextCompat.getColor(c, colorId)));
        text = text.replaceAll("</emph>", "</font>");
        return Html.fromHtml(text);
    }

    public static boolean isValidEmail(CharSequence target) {
        boolean isValid = false;
        if (!TextUtils.isEmpty(target)) {
            isValid = android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
        return isValid;
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        DrawFilter filter = new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG, 1);
        Canvas canvas = new Canvas(bitmap);
        canvas.setDrawFilter(filter);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }


    public static boolean keyboardShown(View rootView) {
        try {
            final int softKeyboardHeight = 100;
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            DisplayMetrics dm = rootView.getResources().getDisplayMetrics();
            int heightDiff = rootView.getBottom() - r.bottom;
            return heightDiff > softKeyboardHeight * dm.density;
        } catch (Exception e) {
            return false; //ensure that we're showing the button is something goes wrong.
        }
    }



}