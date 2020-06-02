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

package com.privateinternetaccess.android.ui.drawer;

import android.app.Activity;
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

import com.google.android.material.appbar.AppBarLayout;
import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.events.ServerClickedEvent;
import com.privateinternetaccess.android.model.events.TVAppBarExpandEvent;
import com.privateinternetaccess.android.model.listModel.ServerItem;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.model.PIAServer;
import com.privateinternetaccess.android.pia.model.events.ServerPingEvent;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.ui.adapters.ServerListAdapter;
import com.privateinternetaccess.android.ui.tv.views.ServerSelectionItemDecoration;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.app.Activity.RESULT_OK;
import static com.privateinternetaccess.android.ui.drawer.ServerListActivity.RESULT_SERVER_CHANGED;

public class ServerListFragment extends Fragment {

    private static final String SERVER_SEARCH_KEY = "server_search";

    @Nullable @BindView(R.id.appbar) AppBarLayout appBar;
    @BindView(android.R.id.list) RecyclerView rvServerList;

    @Nullable @BindView(R.id.server_select_no_results) ImageView ivNoResults;
    @Nullable @BindView(R.id.cancel_icon) ImageView ivCancelSearch;
    @Nullable @BindView(R.id.search) EditText etSearchBar;

    private List<ServerItem> mServerItems;
    private ServerListAdapter mAdapter;

    private GridLayoutManager mLayoutManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(PIAApplication.isAndroidTV(getContext()) ?
                R.layout.activity_tv_secondary_list : R.layout.fragment_select_server,
                container, false);

        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!PIAApplication.isAndroidTV(getContext())) {
            ServerListActivity serverActivity = (ServerListActivity)getActivity();
            serverActivity.initAppBar();

            etSearchBar.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    DLog.d("ServerListFragment", "Text updated");
                    searchRegions();
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });

            ivCancelSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    etSearchBar.setText("");
                }
            });

            ivCancelSearch.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        initView();
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    private void initView() {
        if (PIAApplication.isAndroidTV(getContext())) {
            mServerItems = null;
        }

        int selectedPosition = setUpAdapter(getContext());
        setAdapterPosition(selectedPosition);
        searchRegions();
    }

    private void searchRegions() {
        if (!PIAApplication.isAndroidTV(getContext())) {
            ivCancelSearch.setVisibility(etSearchBar.getText().toString().length() == 0 ?
                    View.GONE : View.VISIBLE);

            if (mAdapter != null) {
                if (mAdapter.applySearch(etSearchBar.getText().toString()) == 0) {
                    ivNoResults.setVisibility(View.VISIBLE);
                }
                else {
                    ivNoResults.setVisibility(View.GONE);
                }
            }
        }
    }

    private void setAdapterPosition(int selectedPosition) {
        if(selectedPosition != -1) {
            if (!PIAApplication.isAndroidTV(getContext())) {
                rvServerList.scrollToPosition(selectedPosition);
                mAdapter.setmSelectedItem(selectedPosition);
            }
            else {
                mAdapter.setmConnectedItem(selectedPosition);
            }
        }
    }

    public int setUpAdapter(Context context, boolean reset, PIAServerHandler.ServerSortingType... types) {
        if (reset) {
            mServerItems = null;
        }

        PIAServerHandler handler = PIAServerHandler.getInstance(getContext());
        DLog.d("ServerListFragment", "Types: " + types);

        PIAServer selectedServer = handler.getSelectedRegion(context, true);
        int selectedPosition = -1;
        if (mServerItems == null) {
            mServerItems = new ArrayList<>();

            //add search option
            if (PIAApplication.isAndroidTV(getContext())) {
                mServerItems.add(new ServerItem(SERVER_SEARCH_KEY,
                        R.drawable.ic_search_tv,
                        getString(R.string.server_search),
                        false,
                        false
                ));
            }

            //add auto option
            mServerItems.add(new ServerItem("",
                    R.drawable.flag_world,
                    getString(R.string.automatic_server_selection_main),
                    handler.isSelectedRegionAuto(context),
                    false
            ));

            //Add other options
            for (PIAServer ps : handler.getServers(context, types)) {
                mServerItems.add(new ServerItem(ps.getKey(),
                        handler.getFlagResource(ps),
                        ps.getName(),
                        ps == selectedServer,
                        ps.isAllowsPF()));
                if(ps == selectedServer){
                    selectedPosition = mServerItems.size() - 1;
                }
            }
            //set up adapter
            mAdapter = new ServerListAdapter(mServerItems, getActivity());
            mAdapter.setSearchMode(true);

            mLayoutManager = new GridLayoutManager(context, getResources().getInteger(R.integer.server_selection_grid_size));
            //Required on some versions to correctly set span count
            mLayoutManager.setSpanCount(getResources().getInteger(R.integer.server_selection_grid_size));
            mLayoutManager.requestLayout();
            mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

            if (PIAApplication.isAndroidTV(context)) {
                mAdapter.setSearchMode(false);
                mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        if (position == 0)
                            return mLayoutManager.getSpanCount();

                        return 1;
                    }
                });
            }

            rvServerList.setLayoutManager(mLayoutManager);
            rvServerList.setHasFixedSize(true);
            rvServerList.addItemDecoration(getItemDecoration());
            rvServerList.setAdapter(mAdapter);
        } else {
            for(int i = 0; i < mServerItems.size(); i++){
                ServerItem item = mServerItems.get(i);
                if(i == 0){
                    item.setSelected(handler.isSelectedRegionAuto(context));
                } else {
                    if(selectedServer != null)
                        item.setSelected(item.getName().equals(selectedServer.getName()));
                    else {
                        item.setSelected(false);
                    }
                }
                mServerItems.set(i, item);
            }

            mAdapter.notifyDataSetChanged();
        }
        return selectedPosition;
    }

    private int setUpAdapter(Context context) {
        final String[] options = getResources().getStringArray(R.array.region_filters);
        String selected = Prefs.with(context).get(PiaPrefHandler.FILTERS_REGION_SORTING, "");

        if (selected.equals(options[0])){
            return setUpAdapter(context, true, PIAServerHandler.ServerSortingType.NAME);
        }
        else if (selected.equals(options[1])) {
            return setUpAdapter(context, true, PIAServerHandler.ServerSortingType.PING);
        }
        else {
            return setUpAdapter(context, true, PIAServerHandler.ServerSortingType.NAME, PIAServerHandler.ServerSortingType.FAVORITES);
        }
    }

    private RecyclerView.ItemDecoration getItemDecoration() {
        int spanCount = getResources().getInteger(R.integer.server_selection_grid_size);
        int spacing = 3;

        return new ServerSelectionItemDecoration(spanCount, spacing, 1);
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

        if (!PIAApplication.isAndroidTV(getContext())) {
            Activity activity = getActivity();

            activity.setResult(RESULT_OK);
            if (oldregion == null && region.equals(""))
                ; // Both regions are autoselect
            else
                activity.setResult(RESULT_SERVER_CHANGED);
            activity.overridePendingTransition(R.anim.right_to_left_exit, R.anim.left_to_right_exit);
            activity.finish();
        }
        else {
            String oldRegionName = oldregion != null ? oldregion.getKey() : "";
            if(!region.equals(oldRegionName) ||
                    !PIAFactory.getInstance().getVPN(getActivity()).isVPNActive())
                PIAFactory.getInstance().getVPN(getContext()).start();
        }
    }

    @Subscribe
    public void onServerChange(ServerPingEvent event){
        if(mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Subscribe
    public void onAppBarEventRecieve(TVAppBarExpandEvent event){
        if(!event.isOpen()){
            appBar.setExpanded(false, true);
        } else {
            appBar.setExpanded(true, true);
        }
    }
}
