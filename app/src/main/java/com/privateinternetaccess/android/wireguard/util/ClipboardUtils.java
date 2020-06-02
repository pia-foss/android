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

package com.privateinternetaccess.android.wireguard.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

/**
 * Standalone utilities for interacting with the system clipboard.
 */

public final class ClipboardUtils {
    private ClipboardUtils() {
        // Prevent instantiation
    }

    public static void copyTextView(final View view) {
        if (!(view instanceof TextView))
            return;
        final CharSequence text = ((TextView) view).getText();
        if (text == null || text.length() == 0)
            return;
        final Object service = view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (!(service instanceof ClipboardManager))
            return;
        final CharSequence description = view.getContentDescription();
        ((ClipboardManager) service).setPrimaryClip(ClipData.newPlainText(description, text));
        //Snackbar.make(view, description + " copied to clipboard", Snackbar.LENGTH_LONG).show();
    }
}
