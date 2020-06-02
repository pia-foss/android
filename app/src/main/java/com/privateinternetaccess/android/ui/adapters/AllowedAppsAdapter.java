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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.events.TVAppBarExpandEvent;
import com.privateinternetaccess.android.model.interfaces.IAllowedApps;
import com.privateinternetaccess.android.pia.handlers.ThemeHandler;

import org.greenrobot.eventbus.EventBus;

import java.util.Vector;

/**
 * Created by half47 on 2/5/16.
 */
public class AllowedAppsAdapter extends RecyclerView.Adapter<AllowedAppsAdapter.AppViewHolder> {

    private Vector<ApplicationInfo> mPackages;
    private Vector<ApplicationInfo> mPackagesCopy; // for filtering

    private final LayoutInflater mInflater;

    private PackageManager mPm;

    private IAllowedApps iApps;

    private boolean selectApp;
    private Integer selectedAppPosition;

    private int mSelectedItem = 0;
    private RecyclerView mRecyclerView;

    public AllowedAppsAdapter(Context c, IAllowedApps listen, boolean selectApp) {
        this.mInflater = LayoutInflater.from(c);
        this.mPm = c.getPackageManager();
        this.iApps = listen;
        this.selectApp = selectApp;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @Override
    public AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.list_per_app, parent, false);
        return new AppViewHolder(view);
    }

    @SuppressLint({"RecyclerView"})
    @Override
    public void onBindViewHolder(AppViewHolder holder, int position) {

        holder.mInfo = mPackages.get(position);
        ApplicationInfo mInfo = mPackages.get(position);

        CharSequence appName = mInfo.loadLabel(mPm);

        if (TextUtils.isEmpty(appName))
            appName = mInfo.packageName;
        holder.appName.setText(appName);

        holder.appIcon.setImageDrawable(mInfo.loadIcon(mPm));

        //decide on visuals depending on what state we are in.
        if(!selectApp) {
            boolean contained = iApps.contains(mInfo.packageName);
            int containedResId = contained ? R.drawable.ic_locket_open : R.drawable.ic_locket_closed;
            holder.checkBox.setImageResource(containedResId);
        } else {
            if(iApps.isSelectedApp(mInfo.packageName)){
                holder.itemView.setBackgroundResource(R.drawable.shape_rect_pia_green);
                holder.checkBox.setImageResource(R.drawable.ic_serverselected);
                if(selectedAppPosition == null)
                    selectedAppPosition = position;
            } else {
                holder.itemView.setBackgroundResource(R.drawable.shape_standard_background);
                holder.checkBox.setImageDrawable(null);
            }
            holder.checkBox.setTag(position);
            holder.itemView.setBackgroundResource(iApps.isSelectedApp(holder.mInfo.packageName) ? R.drawable.shape_rect_pia_green : R.drawable.shape_standard_background);
        }

        // Alert the user of problems
        boolean isProblem = iApps.isProblem(mInfo.packageName);
        if(isProblem){
            holder.appName.setTextColor(ContextCompat.getColor(holder.appName.getContext(), R.color.pia_gen_red));
        } else {
            int colorRes = ContextCompat.getColor(holder.appName.getContext(), R.color.pia_text_dark_87_percent);
            if(ThemeHandler.getPrefTheme(holder.appName.getContext()) == ThemeHandler.Theme.NIGHT)
                colorRes = ContextCompat.getColor(holder.appName.getContext(), R.color.pia_text_light_white_87_percent);
            holder.appName.setTextColor(colorRes);
        }
    }

    @Override
    public int getItemCount() {
        if (mPackages != null)
            return mPackages.size();
        else
            return 0;
    }

    @Override
    public long getItemId(int position) {
        return mPackages.get(position).packageName.hashCode();
    }

    class AppViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public ApplicationInfo mInfo;
        public TextView appName;
        public ImageView appIcon;
        public ImageView checkBox;

        public AppViewHolder(View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.list_allowed_app_name);
            appIcon = itemView.findViewById(R.id.list_allowed_app_icon);
            checkBox = itemView.findViewById(R.id.list_allowed_app_selected);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(!selectApp)
                toggle();
            else
                selected();
        }

        private void selected() {
            iApps.appSelected(mInfo.packageName);
            itemView.setBackgroundResource(R.drawable.shape_rect_pia_green);
            checkBox.setImageResource(R.drawable.ic_serverselected);
            if(selectedAppPosition != null){
                notifyItemChanged(selectedAppPosition);
            }
            selectedAppPosition = (Integer) checkBox.getTag();
        }

        public void toggle(){
            if(!iApps.isSelectedApp(mInfo.packageName)) {
                boolean checked = iApps.contains(mInfo.packageName);
                checked = !checked;
                checkBox.setImageResource(!checked ? R.drawable.ic_locket_closed : R.drawable.ic_locket_open);
            }
            iApps.toggleApp(mInfo.packageName, appName.getText() + "");
        }
    }

    public void setmPackages(Vector<ApplicationInfo> mPackages) {
        this.mPackages = mPackages;
        mPackagesCopy = new Vector<>(mPackages);
        notifyDataSetChanged();
    }

    public void filter(String s) {
        if (TextUtils.isEmpty(s)) {
            mPackages = mPackagesCopy;
        } else {
            Vector<ApplicationInfo> results = new Vector<>();
            for (ApplicationInfo info : mPackagesCopy) {
                CharSequence appName = info.loadLabel(mPm);
                if (TextUtils.isEmpty(appName))
                    appName = info.packageName;
                String appNameCompare = appName + "";
                if (appNameCompare.toLowerCase().contains(s.toLowerCase()))
                    results.add(info);
            }
            mPackages = results;
        }
        notifyDataSetChanged();
    }

    public boolean tryMoveSelection(int direction) {
        int nextSelectItem = mSelectedItem + direction;
        // If still within valid bounds, move the selection, notify to redraw, and scroll
        if (nextSelectItem >= 0 && nextSelectItem < getItemCount()) {
            mSelectedItem = nextSelectItem;
            EventBus.getDefault().post(new TVAppBarExpandEvent(mSelectedItem < 2));
            mRecyclerView.smoothScrollToPosition(mSelectedItem);
            return true;
        }
        return false;
    }
}