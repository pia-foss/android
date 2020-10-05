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

import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.listModel.NetworkItem;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.ui.drawer.TrustedWifiActivity;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TrustedWifiAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<ScanResult> wifiItems;
    private List<NetworkItem> networkItemList;

    private Context mContext;

    private RecyclerView mRecyclerView;

    public boolean isLoading = false;
    public boolean isAddingRule = false;

    public TrustedWifiAdapter(Context context, List<ScanResult> wifi, List<NetworkItem> networkItems) {
        wifiItems = wifi;
        networkItemList = networkItems;

        mContext = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(mContext).inflate(R.layout.list_wifi, parent, false);
            return new WifiHolder(v);
        }

        View v = LayoutInflater.from(mContext).inflate(R.layout.list_network_item, parent, false);
        return new NetworkItemHolder(v);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof WifiHolder) {
            WifiHolder wHolder = (WifiHolder) holder;
            String ssid = "";

            if (position < wifiItems.size()) {
                ssid = wifiItems.get(position).SSID;

                ScanResult finalItem = wifiItems.get(position);
                wHolder.itemView.setOnClickListener(view -> {
                    if (mContext instanceof TrustedWifiActivity) {
                        ((TrustedWifiActivity)mContext).addRuleForNetwork(finalItem);
                    }
                });
            }

            wHolder.tvWifiName.setText(ssid);
        }
        else if (holder instanceof NetworkItemHolder) {
            NetworkItemHolder nHolder = (NetworkItemHolder) holder;
            final NetworkItem item = networkItemList.get(position);

            applyStatus(item, nHolder);
            nHolder.tvTitle.setText(item.networkName);

            nHolder.ivOptionsButton.setOnClickListener(v -> {
                if (mContext instanceof TrustedWifiActivity) {
                    ((TrustedWifiActivity)mContext).updateNetworkRule(item);
                }
            });
        }
    }

    private void applyStatus(NetworkItem item, NetworkItemHolder holder) {
        switch (item.behavior) {
            case ALWAYS_CONNECT:
                holder.vColor.setBackgroundColor(mContext.getResources().getColor(R.color.rule_connect));
                holder.ivStatusIcon.setColorFilter(mContext.getResources().getColor(R.color.rule_connect));
                holder.tvStatus.setText(R.string.nmt_connect);
                break;
            case ALWAYS_DISCONNECT:
                holder.vColor.setBackgroundColor(mContext.getResources().getColor(R.color.rule_disconnect));
                holder.ivStatusIcon.setColorFilter(mContext.getResources().getColor(R.color.rule_disconnect));
                holder.tvStatus.setText(R.string.nmt_disconnect);
                break;
            case RETAIN_STATE:
                holder.vColor.setBackgroundColor(mContext.getResources().getColor(R.color.rule_retain));
                holder.ivStatusIcon.setColorFilter(mContext.getResources().getColor(R.color.rule_retain));
                holder.tvStatus.setText(R.string.nmt_retain);
                break;
        }

        switch(item.type) {
            case WIFI_OPEN:
                holder.ivStatusIcon.setImageResource(R.drawable.ic_open_wifi_connect);
                break;
            case WIFI_SECURE:
                holder.ivStatusIcon.setImageResource(R.drawable.ic_secure_wifi_connect);
                break;
            case MOBILE_DATA:
                holder.ivStatusIcon.setImageResource(R.drawable.ic_mobile_data_connect);
                break;
            case WIFI_CUSTOM:
                holder.ivStatusIcon.setImageResource(R.drawable.ic_custom_wifi_connect);
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isAddingRule) {
            return TYPE_HEADER;
        }

        return TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        if (isAddingRule) {
            return wifiItems.size();
        }
        else {
            return networkItemList.size();
        }
    }

//    private boolean hasRule(String ssid) {
//        List<String> networkRules = PiaPrefHandler.getNetworkRules(mContext);
//
//        for (String rule : networkRules) {
//            NetworkItem networkRule = NetworkItem.fromString(rule);
//
//            if (networkRule != null && networkRule.networkName.equals(ssid)) {
//                return true;
//            }
//        }
//
//        return false;
//    }

    class NetworkItemHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.list_network_color_view) View vColor;
        @BindView(R.id.list_network_status_icon) AppCompatImageView ivStatusIcon;
        @BindView(R.id.list_network_options_button) AppCompatImageView ivOptionsButton;
        @BindView(R.id.list_network_status_text) TextView tvStatus;
        @BindView(R.id.list_network_title_text) TextView tvTitle;

        public NetworkItemHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class WifiHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.list_wifi_add_remove) ImageView ivAddRemove;
        @BindView(R.id.list_wifi_name) TextView tvWifiName;

        public WifiHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

}
