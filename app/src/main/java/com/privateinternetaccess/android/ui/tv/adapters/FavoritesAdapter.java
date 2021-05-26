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

package com.privateinternetaccess.android.ui.tv.adapters;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.listModel.ServerItem;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoritesHolder> {

    private Activity mContext;
    private List<ServerItem> mItems;
    private List<ServerItem> mFilteredItems;

    private int mSelectedItem = 0;
    private RecyclerView mRecyclerView;

    private Drawable heartDrawable;
    private Drawable darkHeartDrawable;

    public FavoritesAdapter(List<ServerItem> items, Activity context) {
        this.mContext = context;
        this.mItems = items;
        this.mFilteredItems = new ArrayList<>(mItems);

        heartDrawable = AppCompatResources.getDrawable(mContext, R.drawable.ic_heart).mutate();
        darkHeartDrawable = AppCompatResources.getDrawable(mContext, R.drawable.ic_heart).mutate();
        DrawableCompat.setTint(darkHeartDrawable, Color.BLACK);
    }

    @Override
    public FavoritesAdapter.FavoritesHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.list_favorites, parent, false);
        return new FavoritesHolder(v);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @Override
    public void onBindViewHolder(FavoritesHolder holder, int position) {
        final ServerItem item = mFilteredItems.get(position);
        holder.serverImage.setImageResource(item.getFlagId());
        holder.serverName.setText(item.getName());

        if (PiaPrefHandler.isFavorite(mContext, item.getName())) {
            holder.iconImage.setImageDrawable(heartDrawable);
        }
        else {
            holder.iconImage.setImageDrawable(darkHeartDrawable);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                trySelection();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mFilteredItems.size();
    }

    public void applySearch(String searchTerm) {
        mFilteredItems.clear();

        if(searchTerm.isEmpty()) {
            mFilteredItems.addAll(mItems);
        }
        else{
            searchTerm = searchTerm.toLowerCase();

            for(ServerItem item : mItems){
                if(item.getName().toLowerCase().contains(searchTerm)){
                    mFilteredItems.add(item);
                }
            }
        }

        notifyDataSetChanged();
    }

    private void handleClick(ServerItem item) {
        if (item.isDedicatedIP()) {
            PiaPrefHandler.toggleFavorite(mContext, item.getKey());
        } else {
            PiaPrefHandler.toggleFavorite(mContext, item.getName());
        }
    }

    private boolean trySelection() {
        ServerItem serverItem = mFilteredItems.get(mSelectedItem);
        handleClick(serverItem);

        notifyItemChanged(mSelectedItem);

        return true;
    }


    public class FavoritesHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.list_favorites_name) TextView serverName;
        @BindView(R.id.list_favorites_image) ImageView serverImage;
        @BindView(R.id.list_favorites_icon) ImageView iconImage;

        public FavoritesHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);

            itemView.setFocusable(true);
            itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, final boolean b) {
                    if (b) {
                        mSelectedItem = getAdapterPosition();
                    }

                    mRecyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (b) {
                                itemView.setSelected(true);
                                serverName.setTextColor(mContext.getResources().getColor(R.color.tv_grey_15));
                            }
                            else {
                                itemView.setSelected(false);
                                serverName.setTextColor(mContext.getResources().getColor(R.color.white));
                            }
                        }});
                }
            });
        }
    }
}
