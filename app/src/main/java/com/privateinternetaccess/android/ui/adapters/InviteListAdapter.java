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

import android.app.Activity;
import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.privateinternetaccess.account.model.response.InvitesDetailsInformation.Invite;
import com.privateinternetaccess.android.R;

import java.util.List;

public class InviteListAdapter extends RecyclerView.Adapter<InviteListAdapter.InviteHolder> {

    private Context mContext;
    private List<Invite> mItems;

    public InviteListAdapter(List<Invite> items, Activity context) {
        this.mContext = context;
        this.mItems = items;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public InviteListAdapter.InviteHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.list_referral, parent, false);
        return new InviteListAdapter.InviteHolder(v);
    }

    @Override
    public void onBindViewHolder(InviteListAdapter.InviteHolder holder, final int position) {
        Invite invite = mItems.get(position);
        holder.tvEmail.setText(invite.getObfuscatedEmail());
        holder.tvRewarded.setText(invite.getRewarded() ? mContext.getString(R.string.refer_yes) : mContext.getString(R.string.refer_no));
        holder.tvAccepted.setText(invite.getAccepted() ? mContext.getString(R.string.refer_yes) : mContext.getString(R.string.refer_no));

        if (position == mItems.size() - 1)
            holder.lastDivider.setVisibility(View.VISIBLE);
        else
            holder.lastDivider.setVisibility(View.GONE);
    }

    class InviteHolder extends RecyclerView.ViewHolder {
        TextView tvEmail;
        TextView tvAccepted;
        TextView tvRewarded;

        View lastDivider;

        public InviteHolder(View itemView) {
            super(itemView);

            tvEmail = itemView.findViewById(R.id.list_invite_email);
            tvAccepted = itemView.findViewById(R.id.list_invite_signup);
            tvRewarded = itemView.findViewById(R.id.list_invite_rewarded);

            lastDivider = itemView.findViewById(R.id.list_invite_last_divider);
        }
    }
}
