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
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.listModel.PIALogItem;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by hfrede on 3/23/18.
 */

public class VpnLogAdapter extends RecyclerView.Adapter<VpnLogAdapter.LogItemHolder> {

    Context context;
    LinkedList<PIALogItem> logs;

    public VpnLogAdapter(Context context, List<PIALogItem> logs) {
        this.context = context;
        this.logs = new LinkedList<>(logs);
    }

    @Override
    public LogItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_log_item, parent, false);
        return new LogItemHolder(view);
    }

    @Override
    public void onBindViewHolder(LogItemHolder holder, int position) {
        PIALogItem item = logs.get(position);
        holder.text.setText(item.message);
        holder.datetime.setText(item.timeString);
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    public class LogItemHolder extends RecyclerView.ViewHolder{

        private TextView text;
        private TextView datetime;

        public LogItemHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.textview);
            datetime = itemView.findViewById(R.id.datetime);
        }
    }

    public void addLog(PIALogItem log){
        logs.push(log);
        notifyItemInserted(0);
        if(logs.size() > 1000){
            logs.pop();
            notifyItemRemoved(1001);
        }
    }
}