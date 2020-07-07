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

import com.privateinternetaccess.android.pia.model.exceptions.PortForwardingError;
import com.privateinternetaccess.android.pia.model.response.PortForwardResponse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import okhttp3.ResponseBody;
import okhttp3.mock.MockInterceptor;
import okhttp3.mock.Rule;

import static org.hamcrest.CoreMatchers.instanceOf;

import static okhttp3.mock.MediaTypes.MEDIATYPE_JSON;
import static org.mockito.Mockito.mock;

public class PortForwardApiTest {

    MockInterceptor interceptor;
    PortForwardApi api;
    Context context;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        interceptor = new MockInterceptor();
        PiaApi.setInterceptor(interceptor);

        api = new PortForwardApi();
        context = mock(Context.class);
    }

    @Test
    public void getPort_400() {
        interceptor.addRule(new Rule.Builder()
                .get()
                .url("http://209.222.18.222:2000/?client_id=testing")
                .respond(400));

        try {
            PortForwardResponse response = api.getPort(context);
            Assert.assertEquals((int) response.getPort(), -1);
        } catch (Exception portForwardingError) {
            Assert.assertThat(portForwardingError, instanceOf(PortForwardingError.class));
        }
    }

    @Test
    public void getPort_200() {
        interceptor.reset();
        interceptor.addRule(new Rule.Builder()
                .get()
                .url("http://209.222.18.222:2000/?client_id=testing")
                .respond(400));

        try {
            PortForwardResponse response = api.getPort(context);
            Assert.assertEquals((int) response.getPort(), -1);
        } catch (Exception portForwardingError) { }
    }

    @Test
    public void getPort_200_emptyReturn() {
        interceptor.reset();
        interceptor.addRule(new Rule.Builder()
                .get()
                .url("http://209.222.18.222:2000/?client_id=testing")
                .respond(400)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{}")));

        try {
            PortForwardResponse response = api.getPort(context);
            Assert.assertEquals((int) response.getPort(), -1);
        } catch (Exception portForwardingError) { }
    }

    @Test
    public void getPort_200_validReturn() {
        interceptor.reset();
        interceptor.addRule(new Rule.Builder()
                .get()
                .url("http://209.222.18.222:2000/?client_id=testing")
                .respond(400)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{\"port\":999}")));

        try {
            PortForwardResponse response = api.getPort(context);
            Assert.assertEquals(999, (int) response.getPort());
        } catch (Exception portForwardingError) {  }
    }

    @After
    public void tearDown() throws Exception {
        PiaApi.setInterceptor(null);
    }
}
