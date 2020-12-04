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

package com.privateinternetaccess.android.ui.tv.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.events.VPNTrafficDataPointEvent;
import com.privateinternetaccess.android.model.states.VPNProtocol;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.ui.connection.GraphFragmentHandler;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.OpenVPNManagement;
import de.blinkt.openvpn.core.TrafficHistory;
import de.blinkt.openvpn.core.VpnStatus;


public class GraphView extends FrameLayout {

    private static final int TIME_PERIOD_SECONDS = 0;
    private static final int TIME_PERIOD_MINUTES = 1;
    private static final int TIME_PERIOD_HOURS = 2;
    public static final int DEFAULT_GRAPH_ITEMS = 10;

    @BindView(R.id.view_graph_barchart) BarChart chartConnection;
    @BindView(R.id.view_graph_under_down) TextView tvDown;
    @BindView(R.id.view_graph_title) TextView tvTitle;

    private Handler mHandler;

    private int colorDown;
    private int textColor;
    private int maxGraphItems;
    private boolean showLegend;
    private boolean showDownloadSpeed;
    private List<Long> wireguardRxBytes = new ArrayList<>();


    public GraphView(Context context) {
        super(context);
        init(context, null);
    }

    public GraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public GraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.view_graph, this);
        ButterKnife.bind(this, this);

        mHandler = new Handler();

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.GraphView, 0, 0);
        try {
            maxGraphItems = ta.getInt(R.styleable.GraphView_maxElements, DEFAULT_GRAPH_ITEMS);
            textColor = ta.getColor(R.styleable.GraphView_textColor, ContextCompat.getColor(getContext(), R.color.pia_text_light_primary));
            showLegend = ta.getBoolean(R.styleable.GraphView_showLegend, false);
            showDownloadSpeed = ta.getBoolean(R.styleable.GraphView_showDownloadSpeed, true);
        } finally {
            ta.recycle();
        }

        colorDown = ContextCompat.getColor(getContext(), R.color.pia_gen_green_dark);

        if(!showDownloadSpeed) {
            tvDown.setVisibility(View.INVISIBLE);
            tvTitle.setVisibility(View.INVISIBLE);
        }
        configureChart(textColor);
        clearGraph();
        updateSpeedText(0,0);
        if(PIAFactory.getInstance().getVPN(getContext()).isVPNActive())
            updateChart();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);

        mHandler.postDelayed(triggerRefresh, OpenVPNManagement.mBytecountInterval * 1500);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }


    private void configureChart(int textColor) {
        chartConnection.setPinchZoom(false);
        chartConnection.setDoubleTapToZoomEnabled(false);
        chartConnection.setDrawGridBackground(false);
        chartConnection.setTouchEnabled(false);
        chartConnection.setAutoScaleMinMaxEnabled(true);

        XAxis xAxis = chartConnection.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.transparent));
        xAxis.setDrawAxisLine(true);

        YAxis yAxis = chartConnection.getAxisLeft();
        yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yAxis.setTextColor(textColor);
        yAxis.setAxisMinimum(0f);
        yAxis.setLabelCount(4, false);
        yAxis.setEnabled(false);

        YAxis yAxis2 = chartConnection.getAxisRight();
        yAxis2.setEnabled(false);

        Legend l = chartConnection.getLegend();
        l.setTextColor(textColor);
        l.setTextSize(getResources().getInteger(R.integer.fragment_graph_text));
        l.setEnabled(showLegend);

        Description description = new Description();
        description.setText("");
        chartConnection.setDescription(description);

        chartConnection.fitScreen();

        chartConnection.offsetTopAndBottom(0);
    }

    private String getFormattedString(long bits) {
        String graphUnit = PiaPrefHandler.getGraphUnit(chartConnection.getContext());
        String[] array = chartConnection.getContext().getResources().getStringArray(R.array.preference_graph_values);
        return GraphFragmentHandler.getFormattedString(bits, graphUnit, array);
    }

    private void setupChart() {
        int textColor = ContextCompat.getColor(getContext(), R.color.textColorSecondaryDark);

        BarData dataSets;
        if (VPNProtocol.activeProtocol(getContext()) == VPNProtocol.Protocol.OpenVPN) {
            dataSets = getOpenVPNDataSet(TIME_PERIOD_SECONDS);
        } else {
            dataSets = getWireguardDataSet();
        }

        dataSets.setBarWidth(0.25f);
        dataSets.setValueTextColor(textColor);

        chartConnection.getXAxis().setSpaceMax(0.25f);
        chartConnection.setData(dataSets);
        chartConnection.requestLayout();
    }

    private void setupBarDataOptions(int textColor, int color, BarDataSet dataSet) {
        dataSet.setColor(color);
        dataSet.setDrawValues(false);
        dataSet.setValueTextColor(textColor);
    }

    private void updateChart() {
        if (chartConnection != null) {
            setupChart();
        }
    }

    private void updateSpeedText(long diffIn, long diffOut) {
        String down = getFormattedString(diffIn);
        tvDown.setText(getContext().getString(R.string.graph_under_formatting, down));
    }

    // From icsopenvpn: GraphFragment.java
    private BarData getOpenVPNDataSet(int timeperiod) {
        long interval;
        long totalInterval;

        LinkedList<TrafficHistory.TrafficDatapoint> list = new LinkedList<>();

        //Load Dummy items
        for(int i = 0; i < maxGraphItems; i++)
            list.add(new TrafficHistory.TrafficDatapoint(0, 0, System.currentTimeMillis()));

        switch (timeperiod) {
            case TIME_PERIOD_HOURS:
                list.addAll(VpnStatus.trafficHistory.getHours());
                interval = TrafficHistory.TIME_PERIOD_HOURS;
                totalInterval = 0;
                break;
            case TIME_PERIOD_MINUTES:
                list.addAll(VpnStatus.trafficHistory.getMinutes());
                interval = TrafficHistory.TIME_PERIOD_MINTUES;
                totalInterval = TrafficHistory.TIME_PERIOD_HOURS * TrafficHistory.PERIODS_TO_KEEP;
                break;
            default:
                list.addAll(VpnStatus.trafficHistory.getSeconds());
                interval = OpenVPNManagement.mBytecountInterval * 1000;
                // PIA only uses 60s
                totalInterval = TrafficHistory.TIME_PERIOD_MINTUES;
                break;
        }

        // Max of ten elements
        List<TrafficHistory.TrafficDatapoint> clippedList = list.subList(Math.max(list.size() - maxGraphItems, 0), list.size());

        long now = System.currentTimeMillis();
        long firstTimestamp = 0;
        long lastBytecountIn = 0;

        LinkedList<BarEntry> dataIn = new LinkedList<>();

        int i = 0;
        for (TrafficHistory.TrafficDatapoint tdp : clippedList) {
            if (totalInterval != 0 && (now - tdp.timestamp) > totalInterval) {
                continue;
            }

            if (firstTimestamp == 0) {
                firstTimestamp = clippedList.get(0).timestamp;
                lastBytecountIn = clippedList.get(0).in;
            }

            float in = (tdp.in - lastBytecountIn) / (float) (interval / 1000);

            lastBytecountIn = tdp.in;

            dataIn.add(new BarEntry(i, in));
            i++;
        }

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();

        BarDataSet indata = new BarDataSet(dataIn, getContext().getString(R.string.data_in));

        setupBarDataOptions(textColor, colorDown, indata);

        dataSets.add(indata);

        return new BarData(dataSets);
    }

    private BarData getWireguardDataSet() {
        List<BarEntry> dataIn = new LinkedList<>();

        // Load Dummy items
        for (int i = 0; i < maxGraphItems; i++) {
            dataIn.add(new BarEntry(i, 0));
        }

        for (int i = 0; i < wireguardRxBytes.size(); i++) {
            int index = maxGraphItems + i;
            dataIn.add(new BarEntry(index, wireguardRxBytes.get(i)));
        }

        dataIn = dataIn.subList(dataIn.size() - maxGraphItems, dataIn.size());
        List<IBarDataSet> dataSets = new ArrayList<>();
        BarDataSet indata = new BarDataSet(dataIn, getContext().getString(R.string.data_in));
        setupBarDataOptions(textColor, colorDown, indata);
        dataSets.add(indata);
        return new BarData(dataSets);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onVPNTrafficReceived(final VPNTrafficDataPointEvent event){
        if (VPNProtocol.activeProtocol(getContext()) == VPNProtocol.Protocol.Wireguard) {
            // No reason to keep more events than the max supported.
            if (wireguardRxBytes.size() > maxGraphItems) {
                wireguardRxBytes.remove(0);
            }
            wireguardRxBytes.add(event.getDiffIn());
        }

        if (!VpnStatus.trafficHistory.getSeconds().isEmpty()) {
            updateSpeedText(event.getDiffIn(), event.getDiffOut());
        }

        updateChart();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateState(VpnStateEvent event) {
        if (event.level == ConnectionStatus.LEVEL_NOTCONNECTED) {
            try { // This is here to prevent crashes in an rotation after changing servers.
                clearGraph();
                updateSpeedText(0,0);
                mHandler.removeCallbacks(triggerRefresh);
            } catch (Exception e) {
            }
        }
    }

    private void clearGraph(){
        wireguardRxBytes = new ArrayList<>();
        LinkedList<BarEntry> dataIn = new LinkedList<>();
        // Filled list with max items. We increase one by one as we receive the events.
        // Not doing this will mean the graph bar starting with a large size and slowly decrease
        // as we receive the events.
        for(int i = 0; i < maxGraphItems; i++) {
            dataIn.add(new BarEntry(0, 0));
        }
        BarDataSet indata = new BarDataSet(dataIn, getContext().getString(R.string.data_in));

        setupBarDataOptions(textColor, colorDown, indata);
        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(indata);
        chartConnection.setData(new BarData(dataSets));
        chartConnection.requestLayout();
    }

    private Runnable triggerRefresh = new Runnable() {
        @Override
        public void run() {
            if (VpnStatus.isVPNActive()) {
                updateChart();
                mHandler.postDelayed(triggerRefresh, OpenVPNManagement.mBytecountInterval * 1500);
            }
        }
    };
}
