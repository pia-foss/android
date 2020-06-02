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

package com.privateinternetaccess.android.ui.views;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.events.ServerClickedEvent;
import com.privateinternetaccess.android.model.listModel.ServerItem;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.model.PIAServer;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.ui.connection.MainActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class QuickConnectView extends FrameLayout {

    @BindView(R.id.quick_server_flag_1) ImageView ivFlag1;
    @BindView(R.id.quick_server_flag_2) ImageView ivFlag2;
    @BindView(R.id.quick_server_flag_3) ImageView ivFlag3;
    @BindView(R.id.quick_server_flag_4) ImageView ivFlag4;
    @BindView(R.id.quick_server_flag_5) ImageView ivFlag5;
    @BindView(R.id.quick_server_flag_6) ImageView ivFlag6;

    @BindView(R.id.quick_favorite_1) AppCompatImageView ivFavorite1;
    @BindView(R.id.quick_favorite_2) AppCompatImageView ivFavorite2;
    @BindView(R.id.quick_favorite_3) AppCompatImageView ivFavorite3;
    @BindView(R.id.quick_favorite_4) AppCompatImageView ivFavorite4;
    @BindView(R.id.quick_favorite_5) AppCompatImageView ivFavorite5;
    @BindView(R.id.quick_favorite_6) AppCompatImageView ivFavorite6;

    private ImageView[] flags;
    private AppCompatImageView[] favorites;
    private ServerItem[] servers;

    public QuickConnectView(Context context) {
        super(context);
        init(context);
    }

    public QuickConnectView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public QuickConnectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context) {
        inflate(context, R.layout.view_quick_connect, this);
        ButterKnife.bind(this, this);

        flags = new ImageView[6];
        favorites = new AppCompatImageView[6];
        servers = new ServerItem[6];
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);

        flags[0] = ivFlag1;
        flags[1] = ivFlag2;
        flags[2] = ivFlag3;
        flags[3] = ivFlag4;
        flags[4] = ivFlag5;
        flags[5] = ivFlag6;

        favorites[0] = ivFavorite1;
        favorites[1] = ivFavorite2;
        favorites[2] = ivFavorite3;
        favorites[3] = ivFavorite4;
        favorites[4] = ivFavorite5;
        favorites[5] = ivFavorite6;

        presentServers();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    private PIAServer fetchServer(String serverName) {
        PIAServerHandler handler = PIAServerHandler.getInstance(getContext());

        for (PIAServer ps : handler.getServers(getContext(), PIAServerHandler.ServerSortingType.NAME)) {
            if (ps.getKey().equals(serverName)) {
                return ps;
            }
        }

        return null;
    }

    private void populateServers() {
        int currentIndex = 0;

        List<ServerItem> validServers = new ArrayList<>();
        String[] savedServers = PiaPrefHandler.getQuickConnectList(getContext());

        PIAServerHandler handler = PIAServerHandler.getInstance(getContext());
        for (PIAServer ps : handler.getServers(getContext(), PIAServerHandler.ServerSortingType.NAME)) {
            if (currentIndex >= servers.length)
                break;

            if (PiaPrefHandler.isFavorite(getContext(), ps.getName())) {
                favorites[currentIndex++].setVisibility(View.VISIBLE);
                validServers.add(new ServerItem(
                        ps.getKey(),
                        PIAServerHandler.getInstance(getContext()).getFlagResource(ps),
                        ps.getName(),
                        false,
                        ps.isAllowsPF()));
            }
        }

        for (int i = 0; i < savedServers.length; i++) {
            if (savedServers[i] != null) {
                PIAServer server = fetchServer(savedServers[i]);

                if (server != null && !PiaPrefHandler.isFavorite(getContext(), server.getName())) {
                    validServers.add(new ServerItem(
                            server.getKey(),
                            PIAServerHandler.getInstance(getContext()).getFlagResource(server),
                            server.getName(),
                            false,
                            server.isAllowsPF()));
                }
            }
        }

        for (int i = 0; i < 6; i++) {
            if (i < validServers.size()) {
                servers[i] = validServers.get(i);
            }
            else {
                servers[i] = null;
            }
        }
    }

    private void presentServers() {
        for (AppCompatImageView iv : favorites) {
            iv.setVisibility(View.GONE);
        }

        populateServers();

        for (int i = 0; i < servers.length; i++) {
            if (servers[i] == null || servers[i].equals("")) {
                flags[i].setImageResource(R.drawable.ic_map_empty);
                flags[i].setOnClickListener(null);
            }
            else {
                flags[i].setImageResource(servers[i].getFlagId());

                final String selectedRegion = servers[i].getKey();
                final String selectedRegionName = servers[i].getName();
                flags[i].setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PIAServerHandler handler = PIAServerHandler.getInstance(getContext());

                        PIAServer oldRegion = handler.getSelectedRegion(getContext(), true);
                        String oldRegionName = oldRegion != null ? oldRegion.getKey() : "";
                        handler.saveSelectedServer(getContext(), selectedRegion);

                        EventBus.getDefault().post(new ServerClickedEvent(selectedRegionName, selectedRegionName.hashCode()));

                        if(!selectedRegion.equals(oldRegionName) ||
                                !PIAFactory.getInstance().getVPN(getContext()).isVPNActive()) {
                            Activity activity = getActivity();

                            if (activity != null && activity instanceof MainActivity) {
                                DLog.d("QuickConnectView", "Reseting VPN");
                                ((MainActivity)activity).startVPN(true);
                            }
                        }
                    }
                });
            }
        }
    }

    @Subscribe
    public void onServerSelected(ServerClickedEvent event) {
        presentServers();
    }

    private Activity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity)context;
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
        return null;
    }
}
