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

import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.ui.adapters.VpnLogAdapter;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;

import org.greenrobot.eventbus.Subscribe;

import java.util.Collections;
import java.util.Vector;

import de.blinkt.openvpn.core.LogItem;
import de.blinkt.openvpn.core.VpnStatus;

/**
 * Created by hfrede on 3/22/18.
 */

public class VpnLogActivity extends BaseActivity {

    private ProgressBar progressBar;
    private LinearLayoutManager manager;
    private RecyclerView recyclerView;
    private VpnLogAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary_list);
        initHeader(true, true);
        setTitle(getString(R.string.vpn_activity_title));
        setGreenBackground();
        setSecondaryGreenBackground();

        bindView();
    }

    private void bindView() {
        progressBar = findViewById(R.id.activity_secondary_progress);
        recyclerView = findViewById(android.R.id.list);

        // reset remote view the way we want in vpn logs
        int padding = getResources().getDimensionPixelOffset(R.dimen.activity_horizontal_margin);
        recyclerView.setPadding(padding,0,padding,0);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams();
        params.setMargins(0, 0,0,0);

        recyclerView.setLayoutParams(params);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initView();
    }

    private void initView() {
        manager = new LinearLayoutManager(recyclerView.getContext(), LinearLayoutManager.VERTICAL, false);

        recyclerView.setLayoutManager(manager);

        showHideProgress(true);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                LogItem[] items = VpnStatus.getlogbuffer();
                final Vector<LogItem> logs = new Vector<LogItem>();
                Collections.addAll(logs, items);
                Collections.reverse(logs);

                recyclerView.post(() -> {
                    adapter = new VpnLogAdapter(recyclerView.getContext(), logs);
                    recyclerView.setAdapter(adapter);
                    showHideProgress(false);
                });
            }
        });
    }

    public void showHideProgress(boolean show){
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Subscribe
    public void newLog(LogItem logItem) {
        int scrollPosition = manager.findFirstVisibleItemPosition();
        adapter.addLog(logItem);
        if(scrollPosition == 0)
            manager.scrollToPositionWithOffset(0, 0);
    }
}
