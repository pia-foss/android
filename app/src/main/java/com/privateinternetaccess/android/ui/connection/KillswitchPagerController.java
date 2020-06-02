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

package com.privateinternetaccess.android.ui.connection;

import android.content.Context;
import android.content.DialogInterface;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.privateinternetaccess.android.PIAKillSwitchStatus;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.model.events.KillSwitchEvent;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.ui.adapters.pager.StatusPager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import de.blinkt.openvpn.core.VpnStatus;

public class KillswitchPagerController {

    private View aKillswitch;
    private Button bKillswitch;

    private ViewPager pager;
    private StatusPager pagerAdapter;

    private LinearLayout llPagerIndicator;
    private ImageView[] dots;
    private int dotsCount;

    public KillswitchPagerController(View aKillswitch, Button bKillswitch, ViewPager pager, LinearLayout llPagerIndicator) {
        this.aKillswitch = aKillswitch;
        this.bKillswitch = bKillswitch;
        this.pager = pager;
        this.llPagerIndicator = llPagerIndicator;
    }

    public void bindView(AppCompatActivity activity){
        if (pager.getAdapter() == null) {
            pagerAdapter = new StatusPager(activity.getSupportFragmentManager());
            pager.setAdapter(pagerAdapter);
//            setUiPageViewController();
        }
    }

    private void setUiPageViewController(Context context) {
        dotsCount = pagerAdapter.getCount();
        dots = new ImageView[dotsCount];

        for (int i = 0; i < dotsCount; i++) {
            dots[i] = new ImageView(context);
            dots[i].setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_unselected_dot));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            params.setMargins(4, 0, 20, 0);

            llPagerIndicator.addView(dots[i], params);
        }

        dots[0].setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_selected_dot));

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < dotsCount; i++) {
                    dots[i].setImageDrawable(ContextCompat.getDrawable(llPagerIndicator.getContext(), R.drawable.ic_unselected_dot));
                }
                dots[position].setImageDrawable(ContextCompat.getDrawable(llPagerIndicator.getContext(), R.drawable.ic_selected_dot));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    public void initView(){
        EventBus.getDefault().register(this);
        if (!VpnStatus.isVPNActive()) {
            aKillswitch.setVisibility(View.GONE);
            bKillswitch.setVisibility(View.GONE);
        }

        bKillswitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IVPN vpn = PIAFactory.getInstance().getVPN(llPagerIndicator.getContext());
                vpn.stopKillswitch();
                aKillswitch.setVisibility(View.GONE);
                bKillswitch.setVisibility(View.GONE);
            }
        });
        aKillswitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.killswitch_title);
                builder.setMessage(R.string.killswitchstatus_description);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }
        });

        updateKillSwitchButton(PIAKillSwitchStatus.isKillSwitchActive());
    }

    public void onPause(){
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void updateKillState(KillSwitchEvent event) {
        updateKillSwitchButton(event.isKillSwitchActive);
    }

    @Subscribe
    public void onVPNEvent(VpnStateEvent event){
        IVPN vpn = PIAFactory.getInstance().getVPN(aKillswitch.getContext());
        DLog.d("KillswitchPagerController","killswitchOn = " + vpn.isKillswitchActive());
        updateKillSwitchButton(vpn.isKillswitchActive());
    }

    private void updateKillSwitchButton(final boolean isInKillswitch) {
        aKillswitch.post(new Runnable() {
            @Override
            public void run() {
                DLog.d("PIA", "KillSwitch = " + isInKillswitch );
                if (!VpnStatus.isVPNActive()) {
                    if (isInKillswitch) {
                        aKillswitch.setVisibility(View.VISIBLE);
                        bKillswitch.setVisibility(View.VISIBLE);
                    } else {
                        aKillswitch.setVisibility(View.GONE);
                        bKillswitch.setVisibility(View.GONE);
                    }
                } else {
                    aKillswitch.setVisibility(View.GONE);
                    bKillswitch.setVisibility(View.GONE);
                }
            }
        });
    }
}