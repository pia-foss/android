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
import com.privateinternetaccess.android.model.listModel.PIALogItem;
import com.privateinternetaccess.android.model.states.VPNProtocol;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.ui.adapters.VpnLogAdapter;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;

import org.greenrobot.eventbus.Subscribe;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        setBackground();
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
                List<PIALogItem> items = new ArrayList();

                if (VPNProtocol.activeProtocol(VpnLogActivity.this) == VPNProtocol.Protocol.OpenVPN) {
                    LogItem[] ovpnItems = VpnStatus.getlogbuffer();

                    for (LogItem item : ovpnItems) {
                        items.add(new PIALogItem(item, VpnLogActivity.this));
                    }
                }
                else if (VPNProtocol.activeProtocol(VpnLogActivity.this) == VPNProtocol.Protocol.Wireguard) {
                    try {
                        final Process process = Runtime.getRuntime().exec(new String[]{
                                "logcat", "-b", "all", "-t", "2000",  "-d", "-v", "threadtime", "*:V"});
                        try (final BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
                             final BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream())))
                        {
                            String line;
                            while ((line = stdout.readLine()) != null) {
                                if (line.contains("Wire"))
                                    items.add(new PIALogItem(getLogMessage(line), getLogTime(line)));
                            }

                            stdout.close();
                            if (process.waitFor() != 0) {
                                final StringBuilder errors = new StringBuilder();
                                errors.append("Unable to run logcat:");
                                while ((line = stderr.readLine()) != null)
                                    errors.append(line);
                                throw new Exception(errors.toString());
                            }
                        }
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }

                Collections.reverse(items);

                recyclerView.post(() -> {
                    adapter = new VpnLogAdapter(recyclerView.getContext(), items);
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
    public void newLog(PIALogItem logItem) {
        int scrollPosition = manager.findFirstVisibleItemPosition();
        adapter.addLog(logItem);
        if(scrollPosition == 0)
            manager.scrollToPositionWithOffset(0, 0);
    }

    private String getLogTime(String line) {
        String split[] = line.split(" ");

        if (split[0] != null && split[1] != null) {
            return split[0] + " " + split[1];
        }

        return "";
    }

    private String getLogMessage(String line) {
        String split[] = line.split(" ");
        String message = "";

        for (int i = 5; i < split.length; i++) {
            message += split[i] + " ";
        }

        return message.trim();
    }
}
