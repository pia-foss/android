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

package com.privateinternetaccess.android.ui.tv.views;

import android.graphics.Rect;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.privateinternetaccess.android.pia.utils.DLog;

public class ServerSelectionItemDecoration extends RecyclerView.ItemDecoration {

    private int spanCount;
    private int spacing;

    private int decorationOffset;

    public boolean disable = false;

    public ServerSelectionItemDecoration(int spanCount, int spacing, int offset) {
        this.spanCount = spanCount;
        this.spacing = spacing;
        this.decorationOffset = offset;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (parent.getChildAdapterPosition(view) < decorationOffset || disable) {
            return;
        }

        int position = parent.getChildAdapterPosition(view) - decorationOffset;
        int column = position % spanCount;

        outRect.left = column * spacing / spanCount;
        outRect.right = spacing - (column + 1) * spacing / spanCount;

        if (position >= spanCount) {
            outRect.top = spacing;
        }
    }
}
