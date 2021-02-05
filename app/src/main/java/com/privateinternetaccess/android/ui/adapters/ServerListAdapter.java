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
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.events.ServerClickedEvent;
import com.privateinternetaccess.android.model.listModel.ServerItem;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.PIAColorUtils;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.ui.tv.DashboardActivity;
import com.privateinternetaccess.android.ui.views.ConnectionSlider;
import com.privateinternetaccess.core.model.PIAServer;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.blinkt.openvpn.core.ConnectionStatus;

import static com.privateinternetaccess.regions.RegionsAPIKt.REGIONS_PING_TIMEOUT;

/**
 * Created by half47 on 2/23/16.
 */
public class ServerListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private Activity mContext;

    private List<ServerItem> mItems;
    private List<ServerItem> mFilteredItems;

    private int mSelectedItem = 0;
    private RecyclerView mRecyclerView;

    private Drawable heartDrawable;

    //TV specific variables to show connection state in cards.
    private int mConnectedItem = 0;
    private int mServerSelected = 0;

    private boolean isSearchMode = false;

    public ServerListAdapter(List<ServerItem> items, Activity context) {
        this.mContext = context;
        this.mItems = items;
        this.mFilteredItems = new ArrayList<>(mItems);

        this.heartDrawable = AppCompatResources.getDrawable(mContext, R.drawable.ic_heart).mutate();

        findSelectedServer();
    }

    @Override
    public int getItemViewType(int position) {
        if (PIAApplication.isAndroidTV(mContext) && position == 0 && !isSearchMode)
            return TYPE_HEADER;

        return TYPE_ITEM;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            return new ConnectionHolder(new ConnectionSlider(mContext));
        }

        View v = LayoutInflater.from(mContext).inflate(
                PIAApplication.isAndroidTV(mContext) ? R.layout.list_tv_server : R.layout.list_server,
                parent, false);

        return new ServerHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof  ServerHolder) {
            ServerHolder sHolder = (ServerHolder) holder;
            ServerItem item;
            if (PIAApplication.isAndroidTV(mContext)) {
                item = getServerItem(position);
            } else {
                item = mFilteredItems.get(position);
            }

            if (!PIAApplication.isAndroidTV(mContext)) {
                sHolder.image.setImageResource(item.getFlagId());
                sHolder.name.setText(item.getName());

                sHolder.vDivider.setVisibility(View.VISIBLE);
                sHolder.vLargeDivider.setVisibility(View.GONE);

                if (item.isSelected()) {
                    sHolder.selected.setVisibility(View.VISIBLE);
                } else {
                    sHolder.selected.setVisibility(View.INVISIBLE);
                }

                sHolder.view.setBackgroundResource(R.drawable.shape_standard_background);

                Long ping = null;
                if (item.getLatency() != null && !item.getLatency().isEmpty()) {
                    ping = Long.valueOf(item.getLatency());
                }

                int targetGeoVisibility = View.GONE;
                if (Prefs.with(mContext).get(PiaPrefHandler.GEO_SERVERS_ACTIVE, true)) {
                    targetGeoVisibility = item.isGeo() ? View.VISIBLE : View.GONE;
                    sHolder.geoServerImage.setImageDrawable(
                            AppCompatResources.getDrawable(mContext, R.drawable.ic_geo_unselected)
                    );
                    if (item.isSelected()) {
                        sHolder.geoServerImage.setImageDrawable(
                                AppCompatResources.getDrawable(mContext, R.drawable.ic_geo_selected)
                        );
                    }
                }
                sHolder.geoServerImage.setVisibility(targetGeoVisibility);


                if (!item.isAllowsPF() && PiaPrefHandler.isPortForwardingEnabled(mContext)) {
                    sHolder.portForwarding.setVisibility(View.VISIBLE);
                    sHolder.image.setColorFilter(ContextCompat.getColor(mContext, R.color.server_fade), android.graphics.PorterDuff.Mode.MULTIPLY);
                    sHolder.name.setTextColor(ContextCompat.getColor(mContext, R.color.text_fade));

                    if (ping != null && ping > 0L && ping < REGIONS_PING_TIMEOUT) {
                        if (ping < 200) {
                            sHolder.ping.setTextColor(ContextCompat.getColor(mContext, R.color.latency_green_faded));
                        } else if (ping < 500) {
                            sHolder.ping.setTextColor(ContextCompat.getColor(mContext, R.color.latency_yellow_faded));
                        } else {
                            sHolder.ping.setTextColor(ContextCompat.getColor(mContext, R.color.latency_red_faded));
                        }
                        sHolder.ping.setText(String.format(mContext.getString(R.string.ping_string), ping));
                        sHolder.ping.setVisibility(View.VISIBLE);
                    } else {
                        sHolder.ping.setText("");
                        sHolder.ping.setVisibility(View.GONE);
                    }
                } else {
                    sHolder.portForwarding.setVisibility(View.GONE);
                    sHolder.image.setColorFilter(null);
                    sHolder.portForwarding.setColorFilter(null);
                    sHolder.name.setTextColor(sHolder.originalColor);

                    if (ping != null && ping > 0L && ping < REGIONS_PING_TIMEOUT) {
                        if (ping < 200) {
                            sHolder.ping.setTextColor(ContextCompat.getColor(mContext, R.color.pia_gen_green));
                        } else if (ping < 500) {
                            sHolder.ping.setTextColor(ContextCompat.getColor(mContext, R.color.md_yellow_800));
                        } else {
                            sHolder.ping.setTextColor(ContextCompat.getColor(mContext, R.color.pia_gen_red));
                        }
                        sHolder.ping.setText(String.format(mContext.getString(R.string.ping_string), ping));
                        sHolder.ping.setVisibility(View.VISIBLE);
                    } else {
                        sHolder.ping.setText("");
                        sHolder.ping.setVisibility(View.GONE);
                    }
                }

                if (item.getDedicatedIp() != null) {
                    sHolder.lDip.setVisibility(View.VISIBLE);
                    sHolder.tvDip.setText(item.getDedicatedIp());

                    if (position == getDipCount()) {
                        sHolder.vDivider.setVisibility(View.GONE);
                        sHolder.vLargeDivider.setVisibility(View.VISIBLE);
                    }
                }
                else {
                    sHolder.lDip.setVisibility(View.GONE);
                }

                String itemName = item.getDedicatedIp() != null ? item.getDedicatedIp() : item.getName();
                sHolder.view.setTag(itemName + "," + item.getHash());
                sHolder.view.setOnClickListener(v -> {
                    String tag = (String) v.getTag();
                    String[] array = tag.split(",");
                    String serverName = array[0];
                    int hash = Integer.parseInt(array[1]);
                    DLog.d("ServerListAdapter", "Connecting to selection");
                    DLog.d("ServerListAdapter", "Tag: " + v.getTag());

                    EventBus.getDefault().post(new ServerClickedEvent(serverName, hash));
                });

                String favoriteName;
                if (!TextUtils.isEmpty(item.getDedicatedIp())) {
                    favoriteName = item.getDedicatedIp();
                }
                else {
                    favoriteName = item.getName();
                }

                sHolder.favoriteImage.setOnClickListener(view -> {
                    PiaPrefHandler.toggleFavorite(mContext, favoriteName);
                    notifyItemChanged(position);
                });

                if (PiaPrefHandler.isFavorite(mContext, favoriteName)) {
                    sHolder.favoriteImage.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_heart_selected));
                }
                else {
                    sHolder.favoriteImage.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_heart_mobile));
                }
            } else {
                sHolder.view.setActivated(item.isSelected());
                sHolder.connectionProgress.setVisibility(View.GONE);
                sHolder.connectedImage.setVisibility(View.GONE);
                sHolder.view.setClickable(true);

                if (item.isSelected()) {
                    if (PIAFactory.getInstance().getVPN(mContext).isVPNActive()) {
                        sHolder.connectedImage.setVisibility(View.VISIBLE);
                        sHolder.view.setClickable(false);
                    }
                }

                sHolder.favoriteImage.setImageDrawable(heartDrawable);
                sHolder.favoriteImage.setVisibility(PiaPrefHandler.isFavorite(
                        mContext, item.getName()) ? View.VISIBLE : View.GONE);

                sHolder.view.setOnClickListener(v -> trySelection());
            }

            int serverFadedColor = ContextCompat.getColor(mContext, R.color.server_fade);
            sHolder.image.setImageResource(item.getFlagId());
            sHolder.name.setText(item.getName());
            sHolder.image.setColorFilter(null);
            sHolder.geoServerImage.setColorFilter(null);
            sHolder.portForwarding.setColorFilter(null);
            sHolder.portForwarding.setVisibility(View.GONE);

            // region Server ping
            Long ping = null;
            if (item.getLatency() != null && !item.getLatency().isEmpty()) {
                ping = Long.valueOf(item.getLatency());
            }
            if (ping != null && ping > 0L && ping < REGIONS_PING_TIMEOUT) {
                if (ping < 200) {
                    sHolder.ping.setTextColor(ContextCompat.getColor(mContext, R.color.pia_gen_green));
                } else if (ping < 500) {
                    sHolder.ping.setTextColor(ContextCompat.getColor(mContext, R.color.md_yellow_800));
                } else {
                    sHolder.ping.setTextColor(ContextCompat.getColor(mContext, R.color.pia_gen_red));
                }
                sHolder.ping.setText(String.format(mContext.getString(R.string.ping_string), ping));
                sHolder.ping.setVisibility(View.VISIBLE);
            } else {
                sHolder.ping.setText("");
                sHolder.ping.setVisibility(View.GONE);
            }
            // endregion

            // region Server is geo located
            int targetGeoVisibility = View.GONE;
            if (Prefs.with(mContext).get(PiaPrefHandler.GEO_SERVERS_ACTIVE, true)) {
                targetGeoVisibility = item.isGeo() ? View.VISIBLE : View.GONE;
                sHolder.geoServerImage.setImageDrawable(
                        AppCompatResources.getDrawable(mContext, R.drawable.ic_geo_unselected)
                );
                if (item.isSelected()) {
                    sHolder.geoServerImage.setImageDrawable(
                            AppCompatResources.getDrawable(mContext, R.drawable.ic_geo_selected)
                    );
                }
            }
            sHolder.geoServerImage.setVisibility(targetGeoVisibility);
            // endregion

            // region Server not supporting port forwarding
            if (!item.isAllowsPF() && PiaPrefHandler.isPortForwardingEnabled(mContext)) {
                sHolder.geoServerImage.setColorFilter(serverFadedColor, PorterDuff.Mode.MULTIPLY);
                sHolder.image.setColorFilter(serverFadedColor, PorterDuff.Mode.MULTIPLY);
                sHolder.name.setTextColor(ContextCompat.getColor(mContext, R.color.text_fade));
                sHolder.portForwarding.setVisibility(View.VISIBLE);
            }
            // endregion

            // region Server offline
            if (item.isOffline()) {
                sHolder.view.setClickable(false);
                sHolder.image.setColorFilter(PIAColorUtils.INSTANCE.grayColorFilter());
                sHolder.name.setTextColor(ContextCompat.getColor(mContext, R.color.text_fade));
                sHolder.ping.setVisibility(View.GONE);
            }
            // endregion
        }
        else if (holder instanceof ConnectionHolder) {
            ((ConnectionHolder) holder).connectionButton.animateFocus(mSelectedItem == position);
            holder.itemView.setOnClickListener(v -> trySelection());
        }
    }

    @Override
    public int getItemCount() {
        int size = 0;
        if (mItems != null && !isSearchMode)
            size = mItems.size();
        else if (isSearchMode && mFilteredItems != null) {
            size = mFilteredItems.size();
        }

        if (PIAApplication.isAndroidTV(mContext) && !isSearchMode) {
            size += 1;
        }

        return size;
    }

    private boolean trySelection() {
        if (!isSearchMode) {
            if (mSelectedItem == 0) {
                IVPN vpn = PIAFactory.getInstance().getVPN(mContext);

                if(!vpn.isVPNActive()) {
                    vpn.start();
                } else {
                    vpn.stop();
                }

                return true;
            }
            else if (mSelectedItem == 1) {
                DashboardActivity dashboardActivity = (DashboardActivity) mContext;
                dashboardActivity.showSearchFragment();

                return true;
            }
        }

        if (mConnectedItem != 0 && mConnectedItem < getServerList().size()) {
            ServerItem previousItem = getServerItem(mConnectedItem);
            previousItem.setSelected(false);

            clearHolder(mRecyclerView.findViewHolderForAdapterPosition(mConnectedItem));
        }

        if (mServerSelected != 0 && mConnectedItem != mServerSelected) {
            clearHolder(mRecyclerView.findViewHolderForAdapterPosition(mConnectedItem));
        }

        ServerItem serverItem = getServerItem(mSelectedItem);
        serverItem.setSelected(true);
        mConnectedItem = mSelectedItem;
        mServerSelected = mSelectedItem;

        String itemName = serverItem.getDedicatedIp() != null ? serverItem.getDedicatedIp() : serverItem.getName();
        EventBus.getDefault().post(new ServerClickedEvent(itemName, serverItem.getHash()));

        return true;
    }

    private void clearHolder(final RecyclerView.ViewHolder holder) {
        if (holder != null && holder instanceof ServerHolder) {
            mRecyclerView.post(() -> {
                ServerHolder sHolder = (ServerHolder) holder;
                sHolder.view.setActivated(false);
                sHolder.connectedImage.setVisibility(View.GONE);
                sHolder.connectionProgress.setVisibility(View.GONE);
            });
        }
    }

    public void handleVpnState(final VpnStateEvent event) {
        mRecyclerView.post(() -> {
            PIAServerHandler handler = PIAServerHandler.getInstance(mContext);
            PIAServer selectedServer = handler.getSelectedRegion(mContext, true);

            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(
                    getPositionByRegion(selectedServer) + (isSearchMode ? 0 : 1));
            updateStatuses(selectedServer);

            if (holder != null && holder instanceof ServerHolder) {
                ServerHolder sHolder = (ServerHolder) holder;

                if (event != null) {
                    ConnectionStatus status = event.getLevel();

                    if (status == ConnectionStatus.LEVEL_CONNECTED) {
                        sHolder.connectionProgress.setVisibility(View.GONE);
                        sHolder.view.setActivated(true);
                        sHolder.view.setClickable(false);
                        sHolder.connectedImage.setVisibility(View.VISIBLE);
                    } else if (status == ConnectionStatus.LEVEL_NOTCONNECTED ||
                            status == ConnectionStatus.LEVEL_AUTH_FAILED || status == null) {
                        sHolder.connectionProgress.setVisibility(View.GONE);
                        sHolder.view.setClickable(true);
                        sHolder.connectedImage.setVisibility(View.GONE);
                    } else {
                        sHolder.connectionProgress.setVisibility(View.VISIBLE);
                        sHolder.view.setClickable(false);
                        sHolder.connectedImage.setVisibility(View.GONE);
                    }
                } else {
                    sHolder.connectionProgress.setVisibility(View.GONE);
                    sHolder.connectedImage.setVisibility(View.GONE);
                }
            }
        });
    }

    public void itemsUpdated(List<ServerItem> items) {
        mItems = items;
        mFilteredItems.clear();
        mFilteredItems.addAll(mItems);
        notifyDataSetChanged();
    }

    private void findSelectedServer() {
        for (int i = 0; i < mItems.size(); i++) {
            ServerItem item = mItems.get(i);

            if (item.isSelected()) {
                mServerSelected = i;
                return;
            }
        }
    }

    public int applySearch(String searchTerm) {
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

        return mFilteredItems.size();
    }

    private ServerItem getServerItem(int position) {
        if (isSearchMode) {
            return mFilteredItems.get(position);
        }
        else {
            return mItems.get(position - 1);
        }
    }

    private List<ServerItem> getServerList() {
        if (isSearchMode)
            return mFilteredItems;

        return mItems;
    }

    private int getPositionByRegion(PIAServer region) {
        List<ServerItem> serverItems = getServerList();

        for (int i = 0; i < serverItems.size(); i++) {
            if ((region == null && serverItems.get(i).getKey().equals(""))
                    || (region != null && serverItems.get(i).getKey().equals(region.getKey()))) {
                return i;
            }
        }

        return -1;
    }

    private int getDipCount() {
        return PiaPrefHandler.getDedicatedIps(mContext).size();
    }

    private void updateStatuses(PIAServer currentRegion) {
        List<ServerItem> serverItems = getServerList();

        for (ServerItem item : serverItems) {
            if (currentRegion == null) {
                if (!item.getKey().equals("")) {
                    item.setSelected(false);
                    clearHolder(mRecyclerView.findViewHolderForAdapterPosition(serverItems.indexOf(item)
                        + (isSearchMode ? 0 : 1)));
                }
            }
            else if (!item.getKey().equals(currentRegion.getKey())) {
                item.setSelected(false);
                clearHolder(mRecyclerView.findViewHolderForAdapterPosition(serverItems.indexOf(item)
                        + (isSearchMode ? 0 : 1)));
            }
        }
    }

    class ServerHolder extends RecyclerView.ViewHolder {
        View view;

        @BindView(R.id.list_server_flag) ImageView image;
        @BindView(R.id.list_server_name) TextView name;
        @Nullable
        @BindView(R.id.list_server_selected) View selected;
        @BindView(R.id.list_server_ping) TextView ping;
        @BindView(R.id.list_server_allows_port_forwarding) ImageView portForwarding;
        @Nullable
        @BindView(R.id.list_server_basic_divider) View vDivider;
        @Nullable
        @BindView(R.id.list_server_large_divider) View vLargeDivider;
        @Nullable
        @BindView(R.id.list_server_connection) ProgressBar connectionProgress;

        @BindView(R.id.list_server_geo) ImageView geoServerImage;
        @BindView(R.id.list_server_favorite) ImageView favoriteImage;
        @Nullable
        @BindView(R.id.list_server_connected_icon) ImageView connectedImage;

        @Nullable
        @BindView(R.id.list_server_dip) TextView tvDip;
        @Nullable
        @BindView(R.id.list_server_dip_layout) LinearLayout lDip;

        int originalColor;

        public ServerHolder(final View itemView) {
            super(itemView);
            view = itemView;

            ButterKnife.bind(this, itemView);

            originalColor = name.getCurrentTextColor();

            if (PIAApplication.isAndroidTV(mContext)) {
                itemView.setFocusable(true);
                itemView.setOnFocusChangeListener((view, b) -> {
                    if (b) {
                        mSelectedItem = getAdapterPosition();
                    }

                    mRecyclerView.post(() -> {
                        float scalingFactor = 1.0f;
                        int cardElevation = 0;

                        if (b) {
                            itemView.setSelected(true);
                            name.setTextColor(mContext.getResources().getColor(R.color.black));

                            scalingFactor = 1.1f;
                            cardElevation = 1;
                        } else {
                            itemView.setSelected(false);
                            name.setTextColor(mContext.getResources().getColor(R.color.white));
                        }

                        itemView.setScaleX(scalingFactor);
                        itemView.setScaleY(scalingFactor);
                        ViewCompat.setElevation(itemView, cardElevation);
                    });
                });
            }
        }
    }

    class ConnectionHolder extends RecyclerView.ViewHolder {
        ConnectionSlider connectionButton;

        public ConnectionHolder(ConnectionSlider itemView) {
            super(itemView);
            connectionButton = itemView;

            itemView.setFocusable(true);

            itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    mSelectedItem = 0;
                    connectionButton.animateFocus(b);
                }
            });
        }
    }

    public void setmSelectedItem(int mSelectedItem) {
        this.mSelectedItem = mSelectedItem;
    }
    public void setmConnectedItem(int connectedItem) {
        this.mConnectedItem = connectedItem + 1;
    }
    public void setSearchMode(boolean isSearching) {
        this.isSearchMode = isSearching;
    }
}