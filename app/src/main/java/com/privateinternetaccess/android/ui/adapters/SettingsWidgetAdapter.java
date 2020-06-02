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
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.draweritems.SettingsItem;

import java.util.List;

public class SettingsWidgetAdapter extends RecyclerView.Adapter<SettingsWidgetAdapter.SettingsHolder> {

    private List<SettingsItem> itemList;
    private RecyclerView mRecyclerView;

    private Context mContext;

    public SettingsWidgetAdapter(Context context, List<SettingsItem> items) {
        mContext = context;
        itemList = items;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @Override
    public SettingsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.view_settings_item, parent, false);
        return new SettingsWidgetAdapter.SettingsHolder(v);
    }

    @Override
    public void onBindViewHolder(SettingsHolder holder, int position) {
        SettingsItem item = itemList.get(position);
        item.setupHolder(holder, mContext);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public class SettingsHolder extends RecyclerView.ViewHolder {
        public View view;
        public SwitchCompat sToggle;
        public AppCompatTextView tvTitle;
        public AppCompatTextView tvSummary;

        public SettingsHolder(final View itemView) {
            super(itemView);
            view = itemView;
            sToggle = itemView.findViewById(R.id.settings_toggle);
            tvTitle = itemView.findViewById(R.id.settings_title);
            tvSummary = itemView.findViewById(R.id.settings_summary);
        }
    }
}
