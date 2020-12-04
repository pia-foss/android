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

package com.privateinternetaccess.android.ui.features;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;

import com.privateinternetaccess.android.ui.connection.MainActivity;
import com.privateinternetaccess.android.ui.connection.VPNPermissionActivity;

/**
 * Created by arne on 16.06.16.
 */

public class LaunchVPNForService extends Activity {
    private static final int START_VPN_PROFILE = 41;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startVPN();
    }

    private void startVPN() {

        Intent intent = VpnService.prepare(getApplicationContext());
        if (intent != null) {
            Intent i = new Intent(getApplicationContext(), VPNPermissionActivity.class);
            i.putExtra(MainActivity.START_VPN_SHORTCUT, true);
            startActivityForResult(i, START_VPN_PROFILE);
        } else {
            onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
        }
    }

    private void startVPNService() {
        new Thread() {
            @Override
            public void run() {
                finish();
            }
        }.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == START_VPN_PROFILE || requestCode == VPNPermissionActivity.VPN_PERMISSION_CODE) && resultCode == RESULT_OK) {
            startVPNService();
        }
    }
}


