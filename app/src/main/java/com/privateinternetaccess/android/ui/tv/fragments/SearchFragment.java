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

package com.privateinternetaccess.android.ui.tv.fragments;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.events.ServerClickedEvent;
import com.privateinternetaccess.android.model.listModel.ServerItem;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.ui.adapters.ServerListAdapter;
import com.privateinternetaccess.android.ui.tv.views.ServerSelectionItemDecoration;
import com.privateinternetaccess.core.model.PIAServer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class SearchFragment extends Fragment {

    @BindView(R.id.favorites_list) RecyclerView searchList;
    @BindView(R.id.favorites_search) EditText searchView;
    @BindView(R.id.favorites_search_icon) ImageView searchIcon;
    @BindView(R.id.favorites_title) TextView searchTitle;

    private List<ServerItem> mServerItems;
    private ServerListAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchTitle.setVisibility(View.GONE);

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mAdapter.applySearch(searchView.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        searchView.requestFocus();
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        setupAdapter(getContext());
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    private void setupAdapter(Context context) {
        PIAServerHandler handler = PIAServerHandler.getInstance(context);
        mServerItems = new ArrayList<>();

        for (PIAServer ps : handler.getServers(context, PIAServerHandler.ServerSortingType.NAME, PIAServerHandler.ServerSortingType.FAVORITES)) {
            mServerItems.add(
                    new ServerItem(
                            ps.getKey(),
                            handler.getFlagResource(ps.getIso()),
                            ps.getName(),
                            ps.getIso(),
                            false,
                            ps.isAllowsPF(),
                            ps.isGeo(),
                            ps.isOffline(),
                            ps.getLatency(),
                            ps.isDedicatedIp()
                    )
            );
        }

        mAdapter = new ServerListAdapter(mServerItems, getActivity());
        mAdapter.setSearchMode(true);
        GridLayoutManager manager = new GridLayoutManager(context, getResources().getInteger(R.integer.server_selection_grid_size));
        manager.setOrientation(LinearLayoutManager.VERTICAL);

        searchList.setLayoutManager(manager);
        searchList.addItemDecoration(getItemDecoration());
        searchList.setAdapter(mAdapter);
    }

    private RecyclerView.ItemDecoration getItemDecoration() {
        int spanCount = getResources().getInteger(R.integer.server_selection_grid_size);
        int spacing = 3;

        return new ServerSelectionItemDecoration(spanCount, spacing, 0);
    }

    @Subscribe
    public void updateState(final VpnStateEvent event) {
        if (PIAApplication.isAndroidTV(getContext()) && mAdapter != null) {
            mAdapter.handleVpnState(event);
        }
    }

    @Subscribe
    public void serverClicked(ServerClickedEvent event) {
        PIAServerHandler handler = PIAServerHandler.getInstance(getContext());

        String region = "";
        for (PIAServer ps : handler.getServers().values()) {
            if (ps.getName().equals(event.getName())) {
                region = ps.getKey();
                break;
            }
        }

        // Try also with the key of the objects
        if(TextUtils.isEmpty(region)) {
            for (PIAServer ps : handler.getServers().values()) {
                if (event.getId() == ps.getKey().hashCode()) {
                    region = ps.getKey();
                    break;
                }
            }
        }

        PIAServer oldregion = handler.getSelectedRegion(getContext(), true);
        handler.saveSelectedServer(getContext(), region);

        String oldRegionName = oldregion != null ? oldregion.getKey() : "";
        if(!region.equals(oldRegionName) ||
                !PIAFactory.getInstance().getVPN(getActivity()).isVPNActive())
            PIAFactory.getInstance().getVPN(getContext()).start();
    }
}
