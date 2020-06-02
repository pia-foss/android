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

import android.graphics.drawable.Drawable;

import com.privateinternetaccess.android.wireguard.util.Keyed;

public class ApplicationData implements Keyed<String> {
    private final Drawable icon;
    private final String name;
    private final String packageName;
    private boolean excludedFromTunnel;

    public ApplicationData(final Drawable icon, final String name, final String packageName, final boolean excludedFromTunnel) {
        this.icon = icon;
        this.name = name;
        this.packageName = packageName;
        this.excludedFromTunnel = excludedFromTunnel;
    }

    public Drawable getIcon() {
        return icon;
    }

    @Override
    public String getKey() {
        return name;
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean isExcludedFromTunnel() {
        return excludedFromTunnel;
    }

    public void setExcludedFromTunnel(final boolean excludedFromTunnel) {
        this.excludedFromTunnel = excludedFromTunnel;
    }
}
