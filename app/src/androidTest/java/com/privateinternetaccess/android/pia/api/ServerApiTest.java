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

package com.privateinternetaccess.android.pia.api;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.privateinternetaccess.android.pia.model.response.ServerResponse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import okhttp3.ResponseBody;
import okhttp3.mock.MockInterceptor;
import okhttp3.mock.Rule;

import static okhttp3.mock.Behavior.UNORDERED;
import static okhttp3.mock.MediaTypes.MEDIATYPE_JSON;

public class ServerApiTest {

    MockInterceptor interceptor;
    Context context;
    ServerAPI api;

    @Before
    public void setUp() {
        interceptor = new MockInterceptor();
        PiaApi.setInterceptor(interceptor);

        context = ApplicationProvider.getApplicationContext();
        api = new ServerAPI(context);
        ServerAPI.setTesting(true);

        interceptor.reset();
        interceptor.behavior(UNORDERED);
        interceptor.addRule()
                .get()
                .url("https://www.privateinternetaccess.com/api/client/status")
                .anyTimes()
                .respond(401);
        interceptor.addRule()
                .get()
                .url("https://www.privateinternetaccess.com/api/client/android")
                .anyTimes()
                .respond(401);
    }

    @Test
    public void fetchServers_not200() {
        interceptor.addRule(new Rule.Builder()
                .get()
                .url("https://www.privateinternetaccess.com/vpninfo/servers?version=" + ServerAPI.SERVER_FILE_NUMBER + "&os=android")
                .respond(401));
        ServerResponse response = api.fetchServers();
        Assert.assertNull(response.getInfo());
    }

    @Test
    public void fetchServers_200() {
        interceptor.addRule(new Rule.Builder()
                .get()
                .url("https://www.privateinternetaccess.com/vpninfo/servers?version=" + ServerAPI.SERVER_FILE_NUMBER + "&os=android")
                .respond(200));

        ServerResponse response = api.fetchServers();
        Assert.assertNull(response.getBody());
    }

    @Test
    public void fetchServers_200_emptyBody() {
        interceptor.addRule(new Rule.Builder()
                .get()
                .url("https://www.privateinternetaccess.com/vpninfo/servers?version=" + ServerAPI.SERVER_FILE_NUMBER + "&os=android")
                .respond(200)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{}")));

        ServerResponse response = api.fetchServers();
        Assert.assertNull(response.getInfo());
    }

    @Test
    public void fetchServers_200_emptySig() {
        interceptor.addRule(new Rule.Builder()
                .get()
                .url("https://www.privateinternetaccess.com/vpninfo/servers?version=" + ServerAPI.SERVER_FILE_NUMBER + "&os=android")
                .respond(200)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{}\n\n")));

        ServerResponse response = api.fetchServers();
        Assert.assertNull(response.getServers());
    }

    @Ignore
    public void fetchServers_200_fullSig() {
        interceptor.addRule(new Rule.Builder()
                .get()
                .url("https://www.privateinternetaccess.com/vpninfo/servers?version=" + ServerAPI.SERVER_FILE_NUMBER + "&os=android")
                .respond(200)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{}\n\n" +
                        "YpkAhyTt7bk4//XWswqQbvkFv9QgEdiJKGgPUoIGdj9tMmxJoSvXm2wWAR0+\n" +
                        "9919oLS04apT7teC4O0jUALU0+7qHfLN8uXDorebeuylR/LA7bJCxp1ayUGB\n" +
                        "5xUrBgTtunIESmVoT49FXUxnS50fNdeeFE1GBD48rec5n9eh1L4HQP/7nZAH\n" +
                        "Le8bVsBdhigApv15Yj1GayiRCYEuiqJ+knPjG1Zv2C85Eb174/IhhK6pVBHa\n" +
                        "qCcIX4v50gkk3mI705xstyu/jx+vo+DWlEG4HTyVzHF9yD8J3x3Wb3vfiKgf\n" +
                        "9lnfyq5Xv6Y7c68nK7Pge8JfwdN+N6PhPb3rv94JAQ==")));

        ServerResponse response = api.fetchServers();
        Assert.assertNotNull(response.getBody());
    }

    @Test
    public void fetchServers_200_fullSig_isValid() {
        interceptor.addRule(new Rule.Builder()
                .get()
                .url("https://www.privateinternetaccess.com/vpninfo/servers?version=" + ServerAPI.SERVER_FILE_NUMBER + "&os=android")
                .respond(200)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{}\n\n" +
                        "YpkAhyTt7bk4//XWswqQbvkFv9QgEdiJKGgPUoIGdj9tMmxJoSvXm2wWAR0+\n" +
                        "9919oLS04apT7teC4O0jUALU0+7qHfLN8uXDorebeuylR/LA7bJCxp1ayUGB\n" +
                        "5xUrBgTtunIESmVoT49FXUxnS50fNdeeFE1GBD48rec5n9eh1L4HQP/7nZAH\n" +
                        "Le8bVsBdhigApv15Yj1GayiRCYEuiqJ+knPjG1Zv2C85Eb174/IhhK6pVBHa\n" +
                        "qCcIX4v50gkk3mI705xstyu/jx+vo+DWlEG4HTyVzHF9yD8J3x3Wb3vfiKgf\n" +
                        "9lnfyq5Xv6Y7c68nK7Pge8JfwdN+N6PhPb3rv94JAQ==")));

        ServerResponse response = api.fetchServers();
        boolean isValid = response.isValid();
        Assert.assertFalse(isValid);
    }

    @Ignore
    public void fetchServers_200_fullSig_validBody() {
        interceptor.addRule(new Rule.Builder()
                .get()
                .url("https://www.privateinternetaccess.com/vpninfo/servers?version=" + ServerAPI.SERVER_FILE_NUMBER + "&os=android")
                .respond(200)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{\"us_california\":{\"name\":\"US California\",\"country\":\"US\",\"dns\":\"us-california.privateinternetaccess.com\",\"port_forward\":false,\"ping\":\"198.8.80.67:8888\",\"openvpn_udp\":{\"best\":\"198.8.80.67:8080\"},\"openvpn_tcp\":{\"best\":\"198.8.80.67:500\"},\"serial\":\"2e75f27c33b40879a6c265d5b52994d9\"}}" +
                        "\n\n" +
                        "YpkAhyTt7bk4//XWswqQbvkFv9QgEdiJKGgPUoIGdj9tMmxJoSvXm2wWAR0+\n" +
                        "9919oLS04apT7teC4O0jUALU0+7qHfLN8uXDorebeuylR/LA7bJCxp1ayUGB\n" +
                        "5xUrBgTtunIESmVoT49FXUxnS50fNdeeFE1GBD48rec5n9eh1L4HQP/7nZAH\n" +
                        "Le8bVsBdhigApv15Yj1GayiRCYEuiqJ+knPjG1Zv2C85Eb174/IhhK6pVBHa\n" +
                        "qCcIX4v50gkk3mI705xstyu/jx+vo+DWlEG4HTyVzHF9yD8J3x3Wb3vfiKgf\n" +
                        "9lnfyq5Xv6Y7c68nK7Pge8JfwdN+N6PhPb3rv94JAQ==")));

        ServerResponse response = api.fetchServers();
        Assert.assertEquals(1, response.getServers().size());
    }

    @Ignore
    public void fetchServers_200_fullSig_validInfo(){
        interceptor.addRule(new Rule.Builder()
                .get()
                .url("https://www.privateinternetaccess.com/vpninfo/servers?version=" + ServerAPI.SERVER_FILE_NUMBER + "&os=android")
                .respond(200)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{\"info\":{\"web_ips\":[\"www.privateinternetaccess.com\"],\"vpn_ports\":{\"udp\":[1194,8080,9201,53],\"tcp\":[443,110,80]},\"latest_version\":81,\"poll_interval\":619,\"auto_regions\":[\"us_california\",\"us2\",\"us1\",\"us_chicago\",\"us_south_west\",\"us_florida\",\"us_seattle\",\"us3\",\"us_silicon_valley\",\"us_new_york_city\",\"us_washington_dc\",\"us_atlanta\",\"us_las_vegas\",\"us_houston\",\"us_denver\",\"uk\",\"uk_southampton\",\"uk_manchester\",\"ca_toronto\",\"ca\",\"ca_vancouver\",\"aus\",\"aus_melbourne\",\"de_berlin\",\"germany\",\"nl\",\"sweden\",\"no\",\"denmark\",\"fi\",\"swiss\",\"france\",\"belgium\",\"austria\",\"czech\",\"lu\",\"ireland\",\"italy\",\"spain\",\"ro\",\"hungary\",\"poland\",\"ae\",\"hk\",\"sg\",\"japan\",\"israel\",\"mexico\",\"brazil\",\"in\",\"za\"]}}" +
                        "\n\n" +
                        "YpkAhyTt7bk4//XWswqQbvkFv9QgEdiJKGgPUoIGdj9tMmxJoSvXm2wWAR0+\n" +
                        "9919oLS04apT7teC4O0jUALU0+7qHfLN8uXDorebeuylR/LA7bJCxp1ayUGB\n" +
                        "5xUrBgTtunIESmVoT49FXUxnS50fNdeeFE1GBD48rec5n9eh1L4HQP/7nZAH\n" +
                        "Le8bVsBdhigApv15Yj1GayiRCYEuiqJ+knPjG1Zv2C85Eb174/IhhK6pVBHa\n" +
                        "qCcIX4v50gkk3mI705xstyu/jx+vo+DWlEG4HTyVzHF9yD8J3x3Wb3vfiKgf\n" +
                        "9lnfyq5Xv6Y7c68nK7Pge8JfwdN+N6PhPb3rv94JAQ==")));

        ServerResponse response = api.fetchServers();
        Assert.assertTrue(response.getInfo().getAutoRegions().size() > 1);
    }

    @Test
    public void fetchServers_200_fullSig_validInfo_isValid(){
        interceptor.addRule(new Rule.Builder()
                .get()
                .url("https://www.privateinternetaccess.com/vpninfo/servers?version=" + ServerAPI.SERVER_FILE_NUMBER + "&os=android")
                .respond(200)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{\"info\":{\"web_ips\":[\"www.privateinternetaccess.com\"],\"vpn_ports\":{\"udp\":[1194,8080,9201,53],\"tcp\":[443,110,80]},\"latest_version\":81,\"poll_interval\":619,\"auto_regions\":[\"us_california\",\"us2\",\"us1\",\"us_chicago\",\"us_south_west\",\"us_florida\",\"us_seattle\",\"us3\",\"us_silicon_valley\",\"us_new_york_city\",\"us_washington_dc\",\"us_atlanta\",\"us_las_vegas\",\"us_houston\",\"us_denver\",\"uk\",\"uk_southampton\",\"uk_manchester\",\"ca_toronto\",\"ca\",\"ca_vancouver\",\"aus\",\"aus_melbourne\",\"de_berlin\",\"germany\",\"nl\",\"sweden\",\"no\",\"denmark\",\"fi\",\"swiss\",\"france\",\"belgium\",\"austria\",\"czech\",\"lu\",\"ireland\",\"italy\",\"spain\",\"ro\",\"hungary\",\"poland\",\"ae\",\"hk\",\"sg\",\"japan\",\"israel\",\"mexico\",\"brazil\",\"in\",\"za\"]}}" +
                        "\n\n" +
                        "YpkAhyTt7bk4//XWswqQbvkFv9QgEdiJKGgPUoIGdj9tMmxJoSvXm2wWAR0+\n" +
                        "9919oLS04apT7teC4O0jUALU0+7qHfLN8uXDorebeuylR/LA7bJCxp1ayUGB\n" +
                        "5xUrBgTtunIESmVoT49FXUxnS50fNdeeFE1GBD48rec5n9eh1L4HQP/7nZAH\n" +
                        "Le8bVsBdhigApv15Yj1GayiRCYEuiqJ+knPjG1Zv2C85Eb174/IhhK6pVBHa\n" +
                        "qCcIX4v50gkk3mI705xstyu/jx+vo+DWlEG4HTyVzHF9yD8J3x3Wb3vfiKgf\n" +
                        "9lnfyq5Xv6Y7c68nK7Pge8JfwdN+N6PhPb3rv94JAQ==")));

        ServerResponse response = api.fetchServers();
        Assert.assertFalse(response.isValid());
    }

    @Ignore
    public void fetchServers_200_fullSig_validInfo_validServers_isValid(){
        interceptor.addRule(new Rule.Builder()
                .get()
                .url("https://www.privateinternetaccess.com/vpninfo/servers?version=" + ServerAPI.SERVER_FILE_NUMBER + "&os=android")
                .respond(200)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{\"us_california\":{\"name\":\"US California\",\"country\":\"US\",\"dns\":\"us-california.privateinternetaccess.com\",\"port_forward\":false,\"ping\":\"198.8.80.67:8888\",\"openvpn_udp\":{\"best\":\"198.8.80.67:8080\"},\"openvpn_tcp\":{\"best\":\"198.8.80.67:500\"},\"serial\":\"2e75f27c33b40879a6c265d5b52994d9\"},\"info\":{\"web_ips\":[\"www.privateinternetaccess.com\"],\"vpn_ports\":{\"udp\":[1194,8080,9201,53],\"tcp\":[443,110,80]},\"latest_version\":81,\"poll_interval\":619,\"auto_regions\":[\"us_california\",\"us2\",\"us1\",\"us_chicago\",\"us_south_west\",\"us_florida\",\"us_seattle\",\"us3\",\"us_silicon_valley\",\"us_new_york_city\",\"us_washington_dc\",\"us_atlanta\",\"us_las_vegas\",\"us_houston\",\"us_denver\",\"uk\",\"uk_southampton\",\"uk_manchester\",\"ca_toronto\",\"ca\",\"ca_vancouver\",\"aus\",\"aus_melbourne\",\"de_berlin\",\"germany\",\"nl\",\"sweden\",\"no\",\"denmark\",\"fi\",\"swiss\",\"france\",\"belgium\",\"austria\",\"czech\",\"lu\",\"ireland\",\"italy\",\"spain\",\"ro\",\"hungary\",\"poland\",\"ae\",\"hk\",\"sg\",\"japan\",\"israel\",\"mexico\",\"brazil\",\"in\",\"za\"]}}" +
                        "\n\n" +
                        "YpkAhyTt7bk4//XWswqQbvkFv9QgEdiJKGgPUoIGdj9tMmxJoSvXm2wWAR0+\n" +
                        "9919oLS04apT7teC4O0jUALU0+7qHfLN8uXDorebeuylR/LA7bJCxp1ayUGB\n" +
                        "5xUrBgTtunIESmVoT49FXUxnS50fNdeeFE1GBD48rec5n9eh1L4HQP/7nZAH\n" +
                        "Le8bVsBdhigApv15Yj1GayiRCYEuiqJ+knPjG1Zv2C85Eb174/IhhK6pVBHa\n" +
                        "qCcIX4v50gkk3mI705xstyu/jx+vo+DWlEG4HTyVzHF9yD8J3x3Wb3vfiKgf\n" +
                        "9lnfyq5Xv6Y7c68nK7Pge8JfwdN+N6PhPb3rv94JAQ==")));

        ServerResponse response = api.fetchServers();
        Assert.assertTrue(response.isValid());
    }

    @After
    public void tearDown() throws Exception {
        PiaApi.setInterceptor(null);
    }
}
