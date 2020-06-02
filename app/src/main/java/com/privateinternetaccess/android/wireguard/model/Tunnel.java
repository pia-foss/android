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

package com.privateinternetaccess.android.wireguard.model;

import android.os.SystemClock;
import android.util.Pair;

import com.privateinternetaccess.android.wireguard.config.Config;
import com.privateinternetaccess.android.wireguard.crypto.Key;
import com.privateinternetaccess.android.wireguard.util.Keyed;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

/**
 * Encapsulates the volatile and nonvolatile state of a WireGuard tunnel.
 */

public class Tunnel extends BaseObservable implements Keyed<String> {
    public static final int NAME_MAX_LENGTH = 15;
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_=+.-]{1,15}");

    @Nullable
    private Config config;
    private String name;
    private State state;
    @Nullable
    private Statistics statistics;

    public Tunnel(final String name,
           @Nullable final Config config, final State state) {
        this.name = name;
        this.config = config;
        this.state = state;
    }

    @Bindable
    @androidx.annotation.Nullable
    public Config getConfig() {
        return config;
    }

    @Override
    public String getKey() {
        return name;
    }

    @Bindable
    public String getName() {
        return name;
    }

    @Bindable
    public State getState() {
        return state;
    }

    @Bindable
    @Nullable
    public Statistics getStatistics() {
        return statistics;
    }

    Config onConfigChanged(final Config config) {
        this.config = config;
        return config;
    }

    String onNameChanged(final String name) {
        this.name = name;
        return name;
    }

    State onStateChanged(final State state) {
        if (state != State.UP)
            onStatisticsChanged(null);
        this.state = state;
        return state;
    }

    public void onStateChange(final State newState) {
        onStateChanged(state);
    }

    @Nullable
    Statistics onStatisticsChanged(@Nullable final Statistics statistics) {
        this.statistics = statistics;
        return statistics;
    }

    public void setConfig(final Config config) {
        onConfigChanged(config);
    }

    public void setName(final String name) {
        onNameChanged(name);
    }

    public void setState(final State state) {
        onStateChange(state);
    }

    public enum State {
        DOWN,
        TOGGLE,
        UP;

        public static State of(final boolean running) {
            return running ? UP : DOWN;
        }
    }

    public static class Statistics extends BaseObservable {
        private long lastTouched = SystemClock.elapsedRealtime();
        private final Map<Key, Pair<Long, Long>> peerBytes = new HashMap<>();

        public void add(final Key key, final long rx, final long tx) {
            peerBytes.put(key, Pair.create(rx, tx));
            lastTouched = SystemClock.elapsedRealtime();
        }

        private boolean isStale() {
            return SystemClock.elapsedRealtime() - lastTouched > 900;
        }

        public Key[] peers() {
            return peerBytes.keySet().toArray(new Key[0]);
        }

        public long peerRx(final Key peer) {
            if (!peerBytes.containsKey(peer))
                return 0;
            return peerBytes.get(peer).first;
        }

        public long peerTx(final Key peer) {
            if (!peerBytes.containsKey(peer))
                return 0;
            return peerBytes.get(peer).second;
        }

        public long totalRx() {
            long rx = 0;
            for (final Pair<Long, Long> val : peerBytes.values()) {
                rx += val.first;
            }
            return rx;
        }

        public long totalTx() {
            long tx = 0;
            for (final Pair<Long, Long> val : peerBytes.values()) {
                tx += val.second;
            }
            return tx;
        }
    }
}
