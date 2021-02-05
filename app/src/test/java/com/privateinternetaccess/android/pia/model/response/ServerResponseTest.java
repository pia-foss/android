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

package com.privateinternetaccess.android.pia.model.response;

import com.privateinternetaccess.core.model.PIAServer;
import com.privateinternetaccess.core.model.PIAServerInfo;
import com.privateinternetaccess.core.model.ServerResponse;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Vector;

import static org.hamcrest.CoreMatchers.instanceOf;

public class ServerResponseTest {

    @Test
    public void creationTest(){
        Assert.assertThat(new ServerResponse(), instanceOf(ServerResponse.class));
    }

    @Test
    public void isValidTest(){
        ServerResponse response = new ServerResponse();
        Assert.assertFalse(response.isValid());
    }

    @Test
    public void isValid_notNullServersInfo_Test(){
        ServerResponse response = new ServerResponse(new HashMap<String, PIAServer>(), new PIAServerInfo());
        Assert.assertFalse(response.isValid());
    }

    @Test
    public void isValid_notNullNotEmptyServers_EmptyInfo_Test(){
        HashMap<String, PIAServer> map = new HashMap<>();
        map.put("whatever", Mockito.mock(PIAServer.class));

        PIAServerInfo info = new PIAServerInfo();
        Vector<String> vector = new Vector<>();
        Vector<Integer> intVector = new Vector<>();
        info.setAutoRegions(vector);
        info.setTcpPorts(intVector);
        info.setUdpPorts(intVector);
        info.setWebIps(vector);

        ServerResponse response = new ServerResponse(map, info);
        Assert.assertFalse(response.isValid());
    }

    @Test
    public void isValid_NotEmptyServers_NotEmptyStringInfo_Test(){
        HashMap<String, PIAServer> map = new HashMap<>();
        map.put("US California", Mockito.mock(PIAServer.class));

        PIAServerInfo info = new PIAServerInfo();
        Vector<String> vector = new Vector<>();
        vector.add("54");
        Vector<Integer> intVector = new Vector<>();
        info.setAutoRegions(vector);
        info.setTcpPorts(intVector);
        info.setUdpPorts(intVector);
        info.setWebIps(vector);

        ServerResponse response = new ServerResponse(map, info);
        Assert.assertFalse(response.isValid());
    }

    @Test
    public void sValid_notEmptyServers_NotEmptyIntInfo_Test(){
        HashMap<String, PIAServer> map = new HashMap<>();
        map.put("US California", Mockito.mock(PIAServer.class));

        PIAServerInfo info = new PIAServerInfo();
        Vector<String> vector = new Vector<>();
        Vector<Integer> intVector = new Vector<>();
        intVector.add(1028);
        info.setAutoRegions(vector);
        info.setTcpPorts(intVector);
        info.setUdpPorts(intVector);
        info.setWebIps(vector);

        ServerResponse response = new ServerResponse(map, info);
        Assert.assertFalse(response.isValid());
    }

    @Test
    public void isValid_notEmptyServersNotEmptyInfo_Test(){
        HashMap<String, PIAServer> map = new HashMap<>();
        map.put("US California", Mockito.mock(PIAServer.class));

        PIAServerInfo info = new PIAServerInfo();
        Vector<String> vector = new Vector<>();
        vector.add("54");
        Vector<Integer> intVector = new Vector<>();
        intVector.add(1028);
        info.setAutoRegions(vector);
        info.setTcpPorts(intVector);
        info.setUdpPorts(intVector);
        info.setWebIps(vector);

        ServerResponse response = new ServerResponse(map, info);
        Assert.assertTrue(response.isValid());
    }
}
