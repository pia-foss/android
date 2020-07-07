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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.ui.drawer.TrustedWifiActivity;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TrustedWifiAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<String> wifiItems;
    private List<String> trustedItems;

    private Context mContext;

    private RecyclerView mRecyclerView;

    public boolean isLoading = false;

    public TrustedWifiAdapter(Context context, List<String> wifi, List<String> trusted) {
        wifiItems = wifi;
        trustedItems = trusted;

        mContext = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(mContext).inflate(R.layout.list_header, parent, false);
            return new HeaderHolder(v);
        }

        View v = LayoutInflater.from(mContext).inflate(R.layout.list_wifi, parent, false);
        return new WifiHolder(v);
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
            final String item;

            if (position < wifiItems.size() + 1) {
                item = wifiItems.get(position - 1);

                wHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PiaPrefHandler.addTrustedNetwork(mContext, item);

                        if (mContext instanceof TrustedWifiActivity) {
                            ((TrustedWifiActivity)mContext).setupLists();
                        }
                    }
                });

                wHolder.ivAddRemove.setImageResource(R.drawable.ic_plus);
            }
            else {
                item = trustedItems.get(position - wifiItems.size() - 2);

                wHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PiaPrefHandler.removeTrustedNetwork(mContext, item);

                        if (mContext instanceof TrustedWifiActivity) {
                            ((TrustedWifiActivity)mContext).setupLists();
                        }
                    }
                });

                wHolder.ivAddRemove.setImageResource(R.drawable.ic_minus);
            }

            wHolder.tvWifiName.setText(item);
        }
        else if (holder instanceof HeaderHolder) {
            HeaderHolder hHolder = (HeaderHolder) holder;

            hHolder.pbLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);

            if (position == 0) {
                hHolder.tvHeader.setText(R.string.available_wifi_header);

                if (!isLoading) {
                    hHolder.tvDescription.setText(R.string.trusted_wifi_not_available);
                    hHolder.tvDescription.setVisibility(wifiItems.size() == 0 ? View.VISIBLE : View.GONE);
                }
            }
            else {
                hHolder.tvHeader.setText(R.string.trusted_wifi_plural);

                if (!isLoading) {
                    hHolder.tvDescription.setText(R.string.trusted_wifi_no_trusted);
                    hHolder.tvDescription.setVisibility(trustedItems.size() == 0 ? View.VISIBLE : View.GONE);
                }
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 || position == wifiItems.size() + 1) {
            return TYPE_HEADER;
        }

        return TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return wifiItems.size() + trustedItems.size() + 2;
    }

    class WifiHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.list_wifi_add_remove) ImageView ivAddRemove;
        @BindView(R.id.list_wifi_name) TextView tvWifiName;

        public WifiHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class HeaderHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.list_header_text) TextView tvHeader;
        @BindView(R.id.list_header_description) TextView tvDescription;
        @BindView(R.id.list_header_progress) ProgressBar pbLoading;

        public HeaderHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
