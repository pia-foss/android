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

package com.privateinternetaccess.android.pia.model;

import com.privateinternetaccess.core.model.PIAServerInfo;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class PIAServerInfoTest {

    private String valid_JSON = "{\"web_ips\":[\"www.privateinternetaccess.com\"]," +
            "\"vpn_ports\":{\"udp\":[1111,8888,9999,55],\"tcp\":[222,111,66]}," +
            "\"latest_version\":65,\"poll_interval\":999," +
            "\"auto_regions\":[\"us_california\",\"us2\",\"us1\",\"us_chicago\",\"us_south_west\",\"us_florida\",\"us_seattle\"," +
            "\"us3\",\"us_silicon_valley\",\"us_new_york_city\",\"us_atlanta\",\"us_las_vegas\",\"uk\",\"uk_southampton\",\"uk_manchester\"," +
            "\"ca_toronto\",\"ca\",\"ca_vancouver\",\"aus\",\"aus_melbourne\",\"nl\",\"sweden\",\"no\",\"denmark\",\"fi\",\"swiss\",\"france\"," +
            "\"germany\",\"belgium\",\"austria\",\"czech\",\"italy\",\"spain\",\"ro\",\"hk\",\"sg\",\"japan\",\"israel\",\"mexico\",\"brazil\"," +
            "\"in\"]}";

    @Test
    public void parse_emptyJSON() {
        PIAServerInfo info = new PIAServerInfo();
        try {
            JSONObject object = new JSONObject("{}");
            info.parse(object);
        } catch (JSONException e) {
            throw new IllegalStateException("Error parsing");
        }
        Assert.assertEquals(0, info.getPollInterval());
    }

    @Test
    public void parse_validJSON_pollInterval() {
        PIAServerInfo info = new PIAServerInfo();
        try {
            JSONObject object = new JSONObject(valid_JSON);
            info.parse(object);
        } catch (JSONException e) {
            throw new IllegalStateException("Error parsing");
        }
        Assert.assertEquals(999, info.getPollInterval());
    }

    @Test
    public void parse_validJSON_webIPS() {
        PIAServerInfo info = new PIAServerInfo();
        try {
            JSONObject object = new JSONObject(valid_JSON);
            info.parse(object);
        } catch (JSONException e) {
            throw new IllegalStateException("Error parsing");
        }
        String ip = info.getWebIps().get(0);
        Assert.assertEquals("www.privateinternetaccess.com", ip);
    }

    @Test
    public void parse_validJSON_udp() {
        PIAServerInfo info = new PIAServerInfo();
        try {
            JSONObject object = new JSONObject(valid_JSON);
            info.parse(object);
        } catch (JSONException e) {
            throw new IllegalStateException("Error parsing");
        }
        int[] udpPorts = new int[]{1111, 8888, 9999, 55};
        int pos = 0;
        for(Integer i : info.getUdpPorts()){
            Assert.assertEquals(udpPorts[pos], (int) i);
            pos++;
        }
    }

    @Test
    public void parse_validJSON_tcp() {
        PIAServerInfo info = new PIAServerInfo();
        try {
            JSONObject object = new JSONObject(valid_JSON);
            info.parse(object);
        } catch (JSONException e) {
            throw new IllegalStateException("Error parsing");
        }
        int[] tcpPorts = new int[]{222, 111, 66};
        int pos = 0;
        for(Integer i : info.getTcpPorts()){
            Assert.assertEquals(tcpPorts[pos], (int) i);
            pos++;
        }
    }

    @Test
    public void parse_validJSON_autoRegions() {
        PIAServerInfo info = new PIAServerInfo();
        try {
            JSONObject object = new JSONObject(valid_JSON);
            info.parse(object);
        } catch (JSONException e) {
            throw new IllegalStateException("Error parsing");
        }
        Assert.assertTrue(info.getAutoRegions().contains("us2"));
        Assert.assertTrue(info.getAutoRegions().contains("aus"));
        Assert.assertTrue(info.getAutoRegions().contains("japan"));
        Assert.assertFalse(info.getAutoRegions().contains("random"));
        Assert.assertFalse(info.getAutoRegions().contains(""));
    }
}
