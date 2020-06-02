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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.privateinternetaccess.android.R;

/**
 * Created by half47 on 4/25/17.
 */

public class SettingsAdapter extends ArrayAdapter<String> {

    private Context mContext;
    private String[] options;
    private String[] displayNames;

    private String selected;

    private int selectedIndex = 0;
    private int previewIndex = -1;

    private String warningMessage;
    private boolean hasWarning = false;

    public SettingsAdapter(@NonNull Context context) {
        super(context, R.layout.list_settings);
        this.mContext = context;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    @Override
    public int getCount() {
        return options.length + (hasWarning ? 1 : 0);
    }

    @Override
    public String getItem(int position) {
        return options[position];
    }

    @Nullable
    public String getDisplayName(int position) {
        if (displayNames != null && displayNames.length > position) {
            return displayNames[position];
        }

        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null)
            convertView = View.inflate(mContext, R.layout.list_settings, null);
        SettingsHolder holder = (SettingsHolder) convertView.getTag();
        if(holder == null)
            holder = new SettingsHolder(convertView);

        String option = getItem(position);

        if (position >= options.length) {
            holder.name.setText(warningMessage);
            holder.selected.setVisibility(View.GONE);
        }
        else {
            String displayName = getDisplayName(position);

            holder.selected.setChecked(false);

            holder.name.setText(displayName != null ? displayName : option);
            holder.selected.setVisibility(View.VISIBLE);
            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SettingsHolder holder = (SettingsHolder) v.getTag();
                    if(holder != null){
                        selected = options[holder.position];
                        selectedIndex = holder.position;
                        notifyDataSetChanged();
                    }
                }
            });
        }

        if (selectedIndex != -1) {
            if (selectedIndex == position) {
                holder.selected.setChecked(true);
            }
        }
        else {
            holder.selected.setChecked(selected.equals(option));
        }

        if (previewIndex == position) {
            holder.preview.setVisibility(View.VISIBLE);
        }
        else {
            holder.preview.setVisibility(View.GONE);
        }

        holder.position = position;
        holder.view.setTag(holder);

        return convertView;
    }

    static class SettingsHolder extends RecyclerView.ViewHolder {

        View view;
        TextView name;
        RadioButton selected;
        TextView preview;
        int position;

        public SettingsHolder(View itemView) {
            super(itemView);
            view = itemView;
            name = itemView.findViewById(R.id.list_settings_text);
            selected = itemView.findViewById(R.id.list_settings_radio);
            preview = itemView.findViewById(R.id.list_settings_preview);
        }
    }

    public void setDisplayNames(String[] names) { this.displayNames = names; }

    public void setOptions(String[] options) {
        this.options = options;
    }

    public String getSelected() {
        return selected;
    }

    public int getSelectedIndex() { return selectedIndex; }

    public void setSelected(String selected) {
        this.selected = selected;

        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(selected)) {
                selectedIndex = i;
                break;
            }
        }
    }

    public void setLastItemSelected() {
        this.selectedIndex = options.length - 1;
    }

    public void setPreviewIndex(int index) { this.previewIndex = index; }

    public void setWarning(String warning) {
        this.warningMessage = warning;
        this.hasWarning = this.warningMessage != null && this.warningMessage.length() > 0;
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
    }
}
