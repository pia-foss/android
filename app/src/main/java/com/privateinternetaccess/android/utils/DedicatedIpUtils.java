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

import com.privateinternetaccess.account.model.response.DedicatedIPInformationResponse.Status;
import com.privateinternetaccess.account.model.response.DedicatedIPInformationResponse.DedicatedIPInformation;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.events.DedicatedIPUpdatedEvent;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.model.InAppLocalMessage;
import com.privateinternetaccess.android.pia.model.enums.RequestResponseStatus;
import com.privateinternetaccess.core.model.PIAServer;
import com.privateinternetaccess.core.model.PIAServer.PIAServerEndpointDetails;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import kotlin.Pair;


public class DedicatedIpUtils {

    private static final int DIP_WILL_EXPIRE_WARNING_MIN_DAYS = 5;

    @Nullable
    public static PIAServer serverForDip(DedicatedIPInformation dip, Context context) {
        PIAServerHandler serverHandler = PIAServerHandler.getInstance(context);

        for (PIAServer server : serverHandler.getServers(context, PIAServerHandler.ServerSortingType.NAME)) {
            if (server.getKey() != null &&
                    (server.getKey().equals(dip.getId()) || server.getName().toLowerCase().equals(dip.getId()))) {

                Map<PIAServer.Protocol, List<PIAServerEndpointDetails>> updatedEndpointsPerProtocol = new HashMap<>();
                for (Map.Entry<PIAServer.Protocol, List<PIAServerEndpointDetails>> endpointsPerProtocol : server.getEndpoints().entrySet()) {
                    List<PIAServerEndpointDetails> updatedEndpointDetails = new ArrayList<>();
                    switch (endpointsPerProtocol.getKey()) {
                        case OPENVPN_TCP:
                        case OPENVPN_UDP:
                        case META:
                            updatedEndpointDetails.add(new PIAServerEndpointDetails(
                                    dip.getIp(),
                                    dip.getCn(),
                                    false
                            ));
                            break;
                        case WIREGUARD:
                            String port = endpointsPerProtocol.getValue().get(0).getIp().split(":")[1];
                            updatedEndpointDetails.add(new PIAServerEndpointDetails(
                                    dip.getIp() + ":" + port,
                                    dip.getCn(),
                                    false
                            ));
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

    public static void refreshTokensAndInAppMessages(Context context) {
        updateDedicatedIpsIfNeeded(context);
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

    // region private
    private static void updateDedicatedIpsIfNeeded(Context context) {
        List<String> tokens = new ArrayList<>();
        List<DedicatedIPInformation> dips = PiaPrefHandler.getDedicatedIps(context);
        for (DedicatedIPInformation dip : dips) {
            tokens.add(dip.getDipToken());
        }
        if (tokens.isEmpty()) {
            return;
        }

        IAccount account = PIAFactory.getInstance().getAccount(context);
        String authToken = PiaPrefHandler.getAuthToken(context);
        account.dedicatedIPs(authToken, tokens, (apiDips, requestResponseStatus) -> {
            if (requestResponseStatus != RequestResponseStatus.SUCCEEDED) {
                return null;
            }

            // Get the old list to check for IP updates
            List<DedicatedIPInformation> oldDips = PiaPrefHandler.getDedicatedIps(context);

            // Persist API state
            PiaPrefHandler.saveDedicatedIps(context, apiDips);

            // Queue InApp messages if needed
            checkForIpUpdates(context, oldDips, apiDips);
            checkForAboutToExpireTokens(context);
            checkForExpiredTokens(context);

            // Request to start showing message in case any was queued
            if (InAppMessageManager.hasQueuedMessages()) {
                EventBus.getDefault().post(new DedicatedIPUpdatedEvent());
            }
            return null;
        });
    }

    private static void checkForIpUpdates(
            Context context,
            List<DedicatedIPInformation> oldDips,
            List<DedicatedIPInformation> apiDips
    ) {
        for (DedicatedIPInformation savedDip : oldDips) {
            for (DedicatedIPInformation apiDip : apiDips) {
                if (savedDip.getDipToken().equals(apiDip.getDipToken()) &&
                        savedDip.getIp() != null &&
                        apiDip.getIp() != null &&
                        !savedDip.getIp().equals(apiDip.getIp())
                ) {
                    InAppMessageManager.queueLocalMessage(
                            new InAppLocalMessage(
                                    randomAlphaNumeric(5),
                                    context.getString(R.string.dip_updated_warning),
                                    null,
                                    null
                            )
                    );
                }
            }
        }
    }

    private static void checkForAboutToExpireTokens(Context context) {
        List<DedicatedIPInformation> dips = PiaPrefHandler.getDedicatedIps(context);
        for (DedicatedIPInformation dip : dips) {
            if (dip.getDip_expire() == null) {
                continue;
            }

            Date currentDate = new Date();
            Date expirationDate = new Date(TimeUnit.SECONDS.toMillis(dip.getDip_expire()));
            long daysLeft = TimeUnit.DAYS.convert(
                    expirationDate.getTime() - currentDate.getTime(),
                    TimeUnit.MILLISECONDS
            );
            if (daysLeft >= 0 && daysLeft <= DIP_WILL_EXPIRE_WARNING_MIN_DAYS) {
                String message = context.getString(R.string.dip_will_expire_warning) + " " +
                        context.getString(R.string.dip_get_a_new_one);
                Integer linkStartIndex = context.getString(R.string.dip_will_expire_warning).length() + 1;
                Integer linkEndIndex = message.length();
                InAppMessageManager.queueLocalMessage(
                        new InAppLocalMessage(
                                dip.getDipToken(),
                                message,
                                new Pair<>(linkStartIndex, linkEndIndex),
                                "https://www.privateinternetaccess.com"
                        )
                );
            }
        }
    }

    private static void checkForExpiredTokens(Context context) {
        List<DedicatedIPInformation> dips = PiaPrefHandler.getDedicatedIps(context);
        DedicatedIPInformation expiredDip = null;

        // Check expiration date
        for (DedicatedIPInformation dip : dips) {
            if (dip.getDip_expire() == null) {
                continue;
            }

            Date currentDate = new Date();
            Date expirationDate = new Date(TimeUnit.SECONDS.toMillis(dip.getDip_expire()));
            long daysLeft = TimeUnit.DAYS.convert(
                    expirationDate.getTime() - currentDate.getTime(),
                    TimeUnit.MILLISECONDS
            );
            if (daysLeft < 0) {
                expiredDip = dip;
                break;
            }
        }

        // Remove them from the persisted list depending on status
        for (DedicatedIPInformation dip : dips) {
            if (dip.getDip_expire() == null ||
                    dip.getStatus() == Status.expired ||
                    dip.getStatus() == Status.invalid
            ) {
                expiredDip = dip;
            }
        }

        // If we have detected an expired token. Remove it and queue the InApp message
        if (expiredDip != null) {
            removeToken(context, expiredDip);
            InAppMessageManager.queueLocalMessage(
                    new InAppLocalMessage(
                            expiredDip.getDipToken(),
                            context.getString(R.string.dip_expired_warning),
                            null,
                            null
                    )
            );
        }
    }

    private static void removeToken(Context context, DedicatedIPInformation dip) {
        PiaPrefHandler.removeDedicatedIp(context, dip);
    }
    // endregion
}
