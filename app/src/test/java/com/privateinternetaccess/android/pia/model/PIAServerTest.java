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

import com.privateinternetaccess.core.model.PIAServer;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;

public class PIAServerTest {

    @Test
    public void creationTest(){
        Assert.assertThat(new PIAServer(), instanceOf(PIAServer.class));
    }

    @Test
    public void parse_empty() {
        PIAServer server = new PIAServer();
        try {
            JSONObject object = new JSONObject("");
            server.parse(object, null);
        } catch (JSONException e) { }
        Assert.assertNull(server.getName());
    }

    @Test
    public void parse_nullKey() {
        PIAServer server = new PIAServer();
        try {
            JSONObject object = new JSONObject("");
            server.parse(object, null);
        } catch (JSONException e) { }
        Assert.assertNull(server.getKey());
    }

    @Test
    public void parse_emptyKey(){
        PIAServer server = new PIAServer();
        try {
            JSONObject object = new JSONObject("");
            server.parse(object, "");
        } catch (JSONException e) { }
        Assert.assertNull(server.getKey());
    }

    @Test
    public void parse_serverName(){
        PIAServer server = new PIAServer();
        try {
            JSONObject object = new JSONObject("{\"name\":\"testServer\"}");
            server.parse(object, "");
        } catch (JSONException e) {
            throw new IllegalStateException("Error parsing");
        }
        Assert.assertEquals("testServer", server.getName());
    }

    @Test
    public void parse_portFortwarding(){
        PIAServer server = new PIAServer();
        try {
            JSONObject object = new JSONObject("{\"port_forward\": true}");
            server.parse(object, "");
        } catch (JSONException e) {
            throw new IllegalStateException("Error parsing");
        }
        Assert.assertTrue(server.isAllowsPF());
    }

    @Test
    public void parse_udpObject(){
        PIAServer server = new PIAServer();
        try {
            JSONObject object = new JSONObject("{\"openvpn_udp\": {\"best\": \"0.0.0.0\"}}");
            server.parse(object, "");
        } catch (JSONException e) {
            throw new IllegalStateException("Error parsing");
        }
        Assert.assertEquals("0.0.0.0", server.getUdpbest());
    }

    @Test
    public void parse_notTcpObject(){
        PIAServer server = new PIAServer();
        try {
            JSONObject object = new JSONObject("{\"openvpn_udp\": {\"best\": \"0.0.0.0\"}}");
            server.parse(object, "");
        } catch (JSONException e) {
            throw new IllegalStateException("Error parsing");
        }
        Assert.assertNull(server.getTcpbest());
    }
}