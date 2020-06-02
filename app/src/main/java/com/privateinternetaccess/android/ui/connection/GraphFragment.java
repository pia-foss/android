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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.LinkedList;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.OpenVPNManagement;
import de.blinkt.openvpn.core.TrafficHistory;
import de.blinkt.openvpn.core.VpnStatus;

import static java.lang.Math.max;

/**
 * Created by half47 on 10/10/16.
 * <p>
 * Details about the connection, graphs.
 */


public class GraphFragment extends Fragment implements VpnStatus.ByteCountListener {

    private static final int TIME_PERIOD_SECDONS = 0;
    private static final int TIME_PERIOD_MINUTES = 1;
    private static final int TIME_PERIOD_HOURS = 2;

    private static final int TOTALVIEWS = 30;
    // schwabe: was 30 data points, data points are 2s
    private static final int TOTAL_SECONDS = 60;
    private LineChart chartConnection;

    private View aUnderText;
    private TextView tvUp;
    private TextView tvDown;
    private ImageView ivUp;
    private ImageView ivDown;

    private View tvConnectionText;
    private View tvConnect;

    private boolean mLogScale = false;
    private int colorDown;
    private int colorUp;
    private int textColor;

    private static LineData mData;
    private Handler mHandler;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_graph, container, false);
        bindView(layout);
        mHandler = new Handler();

        textColor = ContextCompat.getColor(getContext(), R.color.textColorSecondaryDark);
        colorUp = ContextCompat.getColor(getContext(), R.color.graph_color_up);
        colorDown = ContextCompat.getColor(getContext(), R.color.graph_color_down);

        return layout;
    }


    private void bindView(View layout) {
        chartConnection = layout.findViewById(R.id.fragment_graph_barchart);

        aUnderText = layout.findViewById(R.id.fragment_graph_under_area);
        tvUp = layout.findViewById(R.id.fragment_graph_under_up);
        tvDown = layout.findViewById(R.id.fragment_graph_under_down);

        ivUp = layout.findViewById(R.id.fragment_graph_under_up_image);
        ivDown = layout.findViewById(R.id.fragment_graph_under_down_image);

        ivUp.setColorFilter(ContextCompat.getColor(ivUp.getContext(), R.color.pia_gen_green));
        ivDown.setColorFilter(ContextCompat.getColor(ivDown.getContext(), R.color.connecting_orange));

        tvConnectionText = layout.findViewById(R.id.fragment_graph_text_connection);
        tvConnect = layout.findViewById(R.id.fragment_graph_connect_to_pia);

        mHandler = new Handler();
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        VpnStatus.addByteCountListener(this);
        mHandler.postDelayed(triggerRefresh, OpenVPNManagement.mBytecountInterval * 1500);
        initView();
    }

    private Runnable triggerRefresh = new Runnable() {
        @Override
        public void run() {
            if (VpnStatus.isVPNActive() && isResumed()) {
                updateChart();
                mHandler.postDelayed(triggerRefresh, OpenVPNManagement.mBytecountInterval * 1500);
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
        VpnStatus.removeByteCountListener(this);
        mHandler.removeCallbacks(triggerRefresh);
    }

    private void initView() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) chartConnection.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isVPNActive = VpnStatus.isVPNActive();

        if (getResources().getBoolean(R.bool.show_connect))
            tvConnect.setVisibility(!isVPNActive ? View.VISIBLE : View.GONE);

        if (activeNetworkInfo != null && PIAApplication.isNetworkAvailable(chartConnection.getContext())) {
            tvConnectionText.setVisibility(isVPNActive ? View.VISIBLE : View.GONE);
            setupChart();
        }
        aUnderText.setVisibility(isVPNActive ? View.VISIBLE : View.INVISIBLE);
    }


    // From icsopenvpn: GraphFragment.java
    private LineData getDataSet(int timeperiod) {

        LinkedList<Entry> dataIn = new LinkedList<>();
        LinkedList<Entry> dataOut = new LinkedList<>();

        long interval;
        long totalInterval;

        LinkedList<TrafficHistory.TrafficDatapoint> list;
        switch (timeperiod) {
            case TIME_PERIOD_HOURS:
                list = VpnStatus.trafficHistory.getHours();
                interval = TrafficHistory.TIME_PERIOD_HOURS;
                totalInterval = 0;
                break;
            case TIME_PERIOD_MINUTES:
                list = VpnStatus.trafficHistory.getMinutes();
                interval = TrafficHistory.TIME_PERIOD_MINTUES;
                totalInterval = TrafficHistory.TIME_PERIOD_HOURS * TrafficHistory.PERIODS_TO_KEEP;
                ;

                break;
            default:
                list = VpnStatus.trafficHistory.getSeconds();
                interval = OpenVPNManagement.mBytecountInterval * 1000;
//                totalInterval = TrafficHistory.TIME_PERIOD_MINTUES * TrafficHistory.PERIODS_TO_KEEP;
                // PIA only uses 60s
                totalInterval = TrafficHistory.TIME_PERIOD_MINTUES;
                break;
        }
        if (list.size() == 0) {
            list = TrafficHistory.getDummyList();
        }


        long lastts = 0;
        float zeroValue;
        if (mLogScale)
            zeroValue = 2;
        else
            zeroValue = 0;

        long now = System.currentTimeMillis();


        long firstTimestamp = 0;
        long lastBytecountOut = 0;
        long lastBytecountIn = 0;

        for (TrafficHistory.TrafficDatapoint tdp : list) {
            if (totalInterval != 0 && (now - tdp.timestamp) > totalInterval)
                continue;

            if (firstTimestamp == 0) {
                firstTimestamp = list.peek().timestamp;
                lastBytecountIn = list.peek().in;
                lastBytecountOut = list.peek().out;
            }

            float t = (tdp.timestamp - firstTimestamp) / 100f;

            float in = (tdp.in - lastBytecountIn) / (float) (interval / 1000);
            float out = (tdp.out - lastBytecountOut) / (float) (interval / 1000);

            lastBytecountIn = tdp.in;
            lastBytecountOut = tdp.out;

            if (mLogScale) {
                in = max(2f, (float) Math.log10(in * 8));
                out = max(2f, (float) Math.log10(out * 8));
            }

            if (lastts > 0 && (tdp.timestamp - lastts > 2 * interval)) {
                dataIn.add(new Entry((lastts - firstTimestamp + interval) / 100f, zeroValue));
                dataOut.add(new Entry((lastts - firstTimestamp + interval) / 100f, zeroValue));

                dataIn.add(new Entry(t - interval / 100f, zeroValue));
                dataOut.add(new Entry(t - interval / 100f, zeroValue));
            }

            lastts = tdp.timestamp;

            dataIn.add(new Entry(t, in));
            dataOut.add(new Entry(t, out));

        }
        if (lastts < now - interval) {

            if (now - lastts > 2 * interval * 1000) {
                dataIn.add(new Entry((lastts - firstTimestamp + interval * 1000) / 100f, zeroValue));
                dataOut.add(new Entry((lastts - firstTimestamp + interval * 1000) / 100f, zeroValue));
            }

            dataIn.add(new Entry((now - firstTimestamp) / 100, zeroValue));
            dataOut.add(new Entry((now - firstTimestamp) / 100, zeroValue));
        }

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();


        LineDataSet indata = new LineDataSet(dataIn, getString(R.string.data_in));
        LineDataSet outdata = new LineDataSet(dataOut, getString(R.string.data_out));


        setupLineDataOptions(colorDown, textColor, indata);
        setupLineDataOptions(colorUp, textColor, outdata);

        dataSets.add(indata);
        dataSets.add(outdata);

        return new LineData(dataSets);
    }

    private void updateChart() {
        if (chartConnection != null && chartConnection.getData() != null) {

            setupChart();

            chartConnection.notifyDataSetChanged();
            //chartConnection.moveViewToX(chartConnection.getData().getEntryCount());
            chartConnection.invalidate();
        }
    }


    private void setupChart() {
        int textColor = ContextCompat.getColor(getContext(), R.color.textColorSecondaryDark);
        configureChart(textColor);

        LineData dataSets = getDataSet(TIME_PERIOD_SECDONS);

        dataSets.setValueTextColor(textColor);

        chartConnection.setData(dataSets);

        chartConnection.requestLayout();
    }

    private void setupLineDataOptions(int textColor, int color, LineDataSet dataSet) {
        dataSet.setColor(color);
        dataSet.setDrawValues(false);
        dataSet.setValueTextColor(textColor);
        dataSet.setCircleColor(color);
        dataSet.setCircleColorHole(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(1f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(45);
        dataSet.setFillColor(color);
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
        //xAxis.setAxisMinimum(0f);
        xAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.transparent));

        YAxis yAxis = chartConnection.getAxisLeft();
        yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yAxis.setTextColor(textColor);
        yAxis.setAxisMinimum(0f);
        yAxis.setLabelCount(4, false);
        yAxis.setDrawAxisLine(false);

        YAxis yAxis2 = chartConnection.getAxisRight();
        yAxis2.setEnabled(false);

        Legend l = chartConnection.getLegend();
        l.setTextColor(textColor);
        l.setTextSize(getResources().getInteger(R.integer.fragment_graph_text));
        l.setEnabled(false);

        Description description = new Description();
        description.setText("");
        chartConnection.setDescription(description);
    }

    private void updateSpeedText(long diffIn, long diffOut) {
        String down = getFormattedString(diffIn);
        String up = getFormattedString(diffOut);
        tvUp.setText(getString(R.string.graph_under_formatting, up));
        tvDown.setText(getString(R.string.graph_under_formatting, down));
        aUnderText.setVisibility(VpnStatus.isVPNActive() ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Built this out so we can test these methods
     *
     * @param bits
     * @return
     */
    private float cleanBytes(long bits) {
        String graphUnit = PiaPrefHandler.getGraphUnit(chartConnection.getContext());
        return GraphFragmentHandler.cleanBytes(bits, graphUnit);
    }

    /**
     * Built this out so we can test these methods
     *
     * @param bits
     * @return
     */
    private String getFormattedString(long bits) {
        String graphUnit = PiaPrefHandler.getGraphUnit(chartConnection.getContext());
        String[] array = chartConnection.getContext().getResources().getStringArray(R.array.preference_graph_values);
        return GraphFragmentHandler.getFormattedString(bits, graphUnit, array);
    }

    @Override
    public void updateByteCount(final long in, final long out, final long diffIn, final long diffOut) {
        chartConnection.post(new Runnable() {
            @Override
            public void run() {
                if (!VpnStatus.trafficHistory.getSeconds().isEmpty())
                    updateSpeedText(diffIn, diffOut);
                updateChart();
                mHandler.removeCallbacks(triggerRefresh);
                mHandler.postDelayed(triggerRefresh, OpenVPNManagement.mBytecountInterval * 1500);
            }
        });
    }

    @Subscribe
    public void updateState(VpnStateEvent event) {
        if (event.level == ConnectionStatus.LEVEL_NOTCONNECTED) {
            try { // This is here to prevent crashes in an rotation after changing servers.
                setupChart();
                tvUp.setText("");
                tvDown.setText("");
                aUnderText.setVisibility(View.INVISIBLE);
                tvConnectionText.setVisibility(View.GONE);
                tvConnect.setVisibility(View.VISIBLE);
            } catch (Exception e) {
            }
        } else if (event.level == ConnectionStatus.LEVEL_CONNECTED) {
            tvConnectionText.setVisibility(View.VISIBLE);
            tvConnect.setVisibility(View.GONE);
        } else {
            tvConnect.setVisibility(View.GONE);
        }
    }
}