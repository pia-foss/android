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

package com.privateinternetaccess.android.ui.adapters;

import android.content.Context;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.ui.WidgetManager;
import com.privateinternetaccess.android.utils.drag.ItemTouchHelperAdapter;
import com.privateinternetaccess.android.utils.drag.ItemTouchHelperViewHolder;
import com.privateinternetaccess.android.utils.drag.OnStartDragListener;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class WidgetsAdapter extends RecyclerView.Adapter<WidgetsAdapter.WidgetViewHolder>
        implements ItemTouchHelperAdapter {

    private Context mContext;
    private List<WidgetManager.WidgetItem> widgets;

    public boolean isReordering = false;
    public WidgetManager widgetManager;

    private final OnStartDragListener mDragStartListener;

    public WidgetsAdapter(Context context, List<WidgetManager.WidgetItem> list, OnStartDragListener dragStartListener) {
        mDragStartListener = dragStartListener;
        mContext = context;
        widgets = list;
    }

    @Override
    public WidgetViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_widget, parent, false);
        return new WidgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(WidgetViewHolder viewHolder, int position) {
        final WidgetManager.WidgetItem item = widgets.get(position);
        final WidgetViewHolder holder = viewHolder;

        setVisibleIcon(holder.ivVisible, item.isVisible);

        holder.ivDrag.setVisibility(isReordering ? View.VISIBLE : View.GONE);
        holder.ivVisible.setVisibility(isReordering ? View.VISIBLE : View.GONE);
        holder.lContent.addView(WidgetManager.getView(mContext, item.widgetType));

        holder.ivVisible.setOnClickListener(view -> {
            item.isVisible = !item.isVisible;
            setVisibleIcon(holder.ivVisible, item.isVisible);

            widgetManager.saveWidgets(widgets);
        });

        holder.ivDrag.setOnTouchListener((v, event) -> {
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                mDragStartListener.onStartDrag(holder);
            }
            return false;
        });
    }

    @Override
    public void onItemDismiss(int position) {
        widgets.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(widgets, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);

        widgetManager.saveWidgets(widgets);

        return true;
    }


    private void setVisibleIcon(ImageView view, boolean isVisible) {
        if (isVisible) {
            view.setImageResource(R.drawable.ic_visible);
        }
        else {
            view.setImageResource(R.drawable.ic_not_visible);
        }
    }

    @Override
    public int getItemCount() {
        return widgets.size();
    }

    class WidgetViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {
        @BindView(R.id.list_widget_visible) ImageView ivVisible;
        @BindView(R.id.list_widget_drag) ImageView ivDrag;
        @BindView(R.id.list_widget_body) FrameLayout lContent;

        public WidgetViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        public void onItemSelected() {
        }

        @Override
        public void onItemClear() {
        }
    }
}
