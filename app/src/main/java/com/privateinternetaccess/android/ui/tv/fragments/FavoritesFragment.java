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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.listModel.ServerItem;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.ui.tv.adapters.FavoritesAdapter;
import com.privateinternetaccess.android.ui.tv.views.FavoriteSelectionItemDecoration;
import com.privateinternetaccess.android.utils.ServerUtils;
import com.privateinternetaccess.core.model.PIAServer;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FavoritesFragment extends Fragment {

    @BindView(R.id.favorites_list) RecyclerView favoritesList;
    @BindView(R.id.favorites_search) EditText searchView;
    @BindView(R.id.favorites_search_icon) ImageView searchIcon;

    private List<ServerItem> mServerItems;
    private FavoritesAdapter mAdapter;

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
        setupAdapter(getContext());
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
                            ps.getLatency()
                    )
            );
        }

        mAdapter = new FavoritesAdapter(mServerItems, getActivity());
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        favoritesList.setLayoutManager(layoutManager);
        favoritesList.addItemDecoration(new FavoriteSelectionItemDecoration(3));
        favoritesList.setAdapter(mAdapter);
    }
}
