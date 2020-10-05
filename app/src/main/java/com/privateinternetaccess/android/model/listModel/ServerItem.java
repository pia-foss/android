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

package com.privateinternetaccess.android.model.listModel;

/**
 * Created by half47 on 2/23/16.
 */
public class ServerItem {

    private String key;
    private int flagId;
    private String name;
    private String iso;
    private boolean selected;
    private boolean allowsPF;
    private boolean geo;
    private String latency;

    public ServerItem(
            String key,
            int flagId,
            String name,
            String iso,
            boolean selected,
            boolean allowsPF,
            boolean geo,
            String latency
    ) {
        this.key = key;
        this.flagId = flagId;
        this.name = name;
        this.iso = iso;
        this.selected = selected;
        this.allowsPF = allowsPF;
        this.geo = geo;
        this.latency = latency;
    }

    public String getKey() {
        return key;
    }

    public int getFlagId() {
        return flagId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIso() {
        return iso;
    }

    public int getHash() {
        return key.hashCode();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isAllowsPF() {
        return allowsPF;
    }

    public void setAllowsPF(boolean allowsPF) {
        this.allowsPF = allowsPF;
    }

    public boolean isGeo() {
        return geo;
    }

    public String getLatency() {
        return latency;
    }

    public void setLatency(String latency) {
        this.latency = latency;
    }
}
