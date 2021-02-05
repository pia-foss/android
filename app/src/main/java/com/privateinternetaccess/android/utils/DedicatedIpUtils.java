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

package com.privateinternetaccess.android.utils;

import android.content.Context;

import androidx.annotation.Nullable;

import com.privateinternetaccess.account.model.response.DedicatedIPInformationResponse;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.core.model.PIAServer;

import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlin.Pair;

public class DedicatedIpUtils {

    @Nullable
    public static PIAServer serverForDip(DedicatedIPInformationResponse.DedicatedIPInformation dip, Context context) {
        PIAServerHandler serverHandler = PIAServerHandler.getInstance(context);

        for (PIAServer server : serverHandler.getServers(context, PIAServerHandler.ServerSortingType.NAME)) {
            if (server.getKey() != null &&
                    (server.getKey().equals(dip.getId()) || server.getName().toLowerCase().equals(dip.getId()))) {

                Map<PIAServer.Protocol, List<Pair<String, String>>> updatedEndpointsPerProtocol = new HashMap<>();
                for (Map.Entry<PIAServer.Protocol, List<Pair<String, String>>> endpointsPerProtocol : server.getEndpoints().entrySet()) {
                    List<Pair<String, String>> updatedEndpointDetails = new ArrayList<>();
                    switch (endpointsPerProtocol.getKey()) {
                        case OPENVPN_TCP:
                        case OPENVPN_UDP:
                        case META:
                            updatedEndpointDetails.add(new Pair<>(dip.getIp(), dip.getCn()));
                            break;
                        case WIREGUARD:
                            String port = endpointsPerProtocol.getValue().get(0).getFirst().split(":")[1];
                            updatedEndpointDetails.add(new Pair<>(dip.getIp() + ":" + port, dip.getCn()));
                            break;
                    }
                    updatedEndpointsPerProtocol.put(endpointsPerProtocol.getKey(), updatedEndpointDetails);
                }

                return new PIAServer(
                        server.getName(),
                        server.getIso(),
                        server.getDns(),
                        "",
                        updatedEndpointsPerProtocol,
                        dip.getId(),
                        server.getLatitude(),
                        server.getLongitude(),
                        false,
                        false,
                        false,
                        dip.getDipToken(),
                        dip.getIp()
                );
            }
        }

        return null;
    }

    public static void refreshTokens(Context context) {
        List<DedicatedIPInformationResponse.DedicatedIPInformation> dips = PiaPrefHandler.getDedicatedIps(context);
        List<String> tokens = new ArrayList<>();

        IAccount account = PIAFactory.getInstance().getAccount(context);
        String authToken = PiaPrefHandler.getAuthToken(context);

        for (DedicatedIPInformationResponse.DedicatedIPInformation dip : dips) {
            tokens.add(dip.getDipToken());
        }

        if (!tokens.isEmpty()) {
            account.dedicatedIPs(authToken, tokens, (details, requestResponseStatus) -> {
                List<DedicatedIPInformationResponse.DedicatedIPInformation> dedicatedIps = details;

                if (PiaPrefHandler.updateDedicatedIp(context, details)) {
                    //TODO: Add logic to create a new in-app message for IP being updated in a DIP
                }

                return null;
            });
        }
    }

    public static String randomAlphaNumeric(int stringLength) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < stringLength; i++) {
            int pos = (int)(Math.random() * chars.length());
            builder.append(chars.charAt(pos));
        }

        return builder.toString();
    }
}
