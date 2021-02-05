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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.privateinternetaccess.account.model.response.DedicatedIPInformationResponse;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.ui.DialogFactory;
import com.privateinternetaccess.android.ui.drawer.DedicatedIPActivity;
import com.privateinternetaccess.android.ui.features.WebviewActivity;
import com.privateinternetaccess.android.ui.loginpurchasing.LoginPurchaseActivity;
import com.privateinternetaccess.core.model.PIAServer;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DedicatedIPAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private List<PIAServer> ipList;

    private PIAServerHandler serverHandler;

    public DedicatedIPAdapter(Context context, List<PIAServer> list) {
        this.mContext = context;
        this.ipList = list;

        serverHandler = PIAServerHandler.getInstance(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_dedicated_ip, parent, false);
        return new DedicatedIpHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final PIAServer dip = ipList.get(position);
        DedicatedIpHolder ipHolder = (DedicatedIpHolder)holder;

        ipHolder.tvCountry.setText(dip.getName());
        ipHolder.tvIp.setText(dip.getDedicatedIp());

        int flagId = serverHandler.getFlagResource(dip.getIso());
        ipHolder.ivFlag.setImageResource(flagId);

        ipHolder.ivClose.setOnClickListener(view -> {
            DialogFactory factory = new DialogFactory(mContext);
            final Dialog dialog = factory.buildDialog();
            factory.setHeader(mContext.getString(R.string.dip_remove_header));
            factory.setMessage(mContext.getString(R.string.dip_remove_body, dip.getName(), dip.getDedicatedIp()));

            factory.setPositiveButton(mContext.getString(R.string.ok), view1 -> {
                if (mContext instanceof DedicatedIPActivity) {
                    ((DedicatedIPActivity)mContext).removeDip(dip);
                }
                dialog.dismiss();
            });

            factory.setNegativeButton(mContext.getString(R.string.cancel), view12 -> dialog.dismiss());

            dialog.show();
        });
    }

    @Override
    public int getItemCount() {
        return ipList.size();
    }

    public class DedicatedIpHolder extends RecyclerView.ViewHolder{

        @BindView(R.id.list_server_name) TextView tvCountry;
        @BindView(R.id.list_server_ping) TextView tvIp;

        @BindView(R.id.list_server_flag) ImageView ivFlag;

        @BindView(R.id.list_server_close_button) AppCompatImageView ivClose;

        public DedicatedIpHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            tvCountry.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
    }

}
