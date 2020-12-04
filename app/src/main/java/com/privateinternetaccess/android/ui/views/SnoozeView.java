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

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.model.events.SnoozeEvent;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.utils.SnoozeUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.blinkt.openvpn.core.ConnectionStatus;

public class SnoozeView extends LinearLayout {

    //5 minute delay
    private static final long SHORT_DELAY = 1000 * 60 * 5;
    //15 minute delay
    private static final long MEDIUM_DELAY = 1000 * 60 * 15;
    //1 hour delay
    private static final long LONG_DELAY = 1000 * 60 * 60;

    @BindView(R.id.snooze_short_button) TextView tvShortButton;
    @BindView(R.id.snooze_medium_button) TextView tvMediumButton;
    @BindView(R.id.snooze_long_button) TextView tvLongButton;

    @BindView(R.id.snooze_resume_layout) LinearLayout lResume;
    @BindView(R.id.snooze_times_layout) LinearLayout lTimes;

    @BindView(R.id.snooze_resume_time_text) TextView tvResume;

    public SnoozeView(Context context) {
        super(context);
        init(context);
    }

    public SnoozeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SnoozeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context) {
        inflate(context, R.layout.view_vpn_snooze, this);
        ButterKnife.bind(this, this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);

        setLayouts();

        VpnStateEvent event = EventBus.getDefault().getStickyEvent(VpnStateEvent.class);
        ConnectionStatus status = event.getLevel();

        shouldEnable(status == ConnectionStatus.LEVEL_CONNECTED);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    private void setLayouts() {
        if (!SnoozeUtils.hasActiveAlarm(getContext())) {
            lTimes.setVisibility(View.VISIBLE);
            lResume.setVisibility(View.GONE);
        }
        else {
            lResume.setVisibility(View.VISIBLE);
            lTimes.setVisibility(View.GONE);

            tvResume.setText(String.format(getContext().getString(R.string.snooze_paused),
                    SnoozeUtils.getWakeupTime(getContext())));
        }
    }

    private void updateState() {
        VpnStateEvent event = EventBus.getDefault().getStickyEvent(VpnStateEvent.class);
        ConnectionStatus status = event.getLevel();

        shouldEnable(status == ConnectionStatus.LEVEL_CONNECTED);
    }

    private void shouldEnable(boolean shouldEnable) {
        tvShortButton.setEnabled(shouldEnable);
        tvMediumButton.setEnabled(shouldEnable);
        tvLongButton.setEnabled(shouldEnable);

        tvShortButton.setClickable(shouldEnable);
        tvMediumButton.setClickable(shouldEnable);
        tvLongButton.setClickable(shouldEnable);
    }

    private void setAlarm(long timeInFuture) {
        if (tvShortButton.isEnabled()) {
            SnoozeUtils.setSnoozeAlarm(getContext(), System.currentTimeMillis() + timeInFuture);
            setLayouts();
        }
    }

    @OnClick(R.id.snooze_short_button)
    public void onShortSnoozeClicked() {
        setAlarm(SHORT_DELAY);
        PIAFactory.getInstance().getVPN(getContext()).stop();
    }

    @OnClick(R.id.snooze_medium_button)
    public void onMediumSnoozeClicked() {
        setAlarm(MEDIUM_DELAY);
        PIAFactory.getInstance().getVPN(getContext()).stop();
    }

    @OnClick(R.id.snooze_long_button)
    public void onLongSnoozeClicked() {
        setAlarm(LONG_DELAY);
        PIAFactory.getInstance().getVPN(getContext()).stop();
    }

    @OnClick(R.id.snooze_resume_button)
    public void onResumeClicked() {
        SnoozeUtils.resumeVpn(getContext(), true);
        setLayouts();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateState(VpnStateEvent event) {
        updateState();
    }

    @Subscribe
    public void updateLayouts(SnoozeEvent event) {
        setLayouts();
    }
}
