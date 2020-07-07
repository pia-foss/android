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

import com.privateinternetaccess.android.pia.model.exceptions.HttpResponseError;
import com.privateinternetaccess.android.pia.model.response.ReportResponse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import okhttp3.mock.MockInterceptor;
import okhttp3.mock.Rule;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class ReportingApiTest {

    MockInterceptor interceptor;
    ReportingApi api;
    Context context;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        interceptor = new MockInterceptor();
        PiaApi.setInterceptor(interceptor);

        context = mock(Context.class);
        api = new ReportingApi(context);
    }

    @Test
    public void sendReport_400() {
        interceptor.addRule(new Rule.Builder()
                .post()
                .url("https://www.privateinternetaccess.com/vpninfo/debug_log")
                .respond(400));

        String[] list = new String[2];
        list[0] = "";
        list[1] = "";
        try {
            ReportResponse response = api.sendReport(list);
        } catch (IOException e) {
            Assert.assertThat(e, instanceOf(HttpResponseError.class));
        }
    }

    @Test
    public void sendReport_200() {
        interceptor.reset();
        interceptor.addRule(new Rule.Builder()
                .post()
                .url("https://www.privateinternetaccess.com/vpninfo/debug_log")
                .respond(200));

        String[] list = new String[2];
        list[0] = "test";
        list[1] = "test";
        try {
            ReportResponse response = api.sendReport(list);
            Assert.assertNotNull(response.getTicketId());
        } catch (IOException e) {
            throw new IllegalStateException("Response error");
        }
    }

    @Test
    public void sendReport_200_validID() {
        interceptor.reset();
        interceptor.addRule(new Rule.Builder()
                .post()
                .url("https://www.privateinternetaccess.com/vpninfo/debug_log")
                .respond(200));
        String[] list = new String[2];
        list[0] = "test";
        list[1] = "test";
        try {
            ReportResponse response = api.sendReport(list);
            Assert.assertTrue(response.getTicketId().matches("^[A-F0-9]+$"));
        } catch (IOException e) {
            throw new IllegalStateException("Response error");
        }
    }

    @Test
    public void sendReport_200_noException() {
        interceptor.reset();
        interceptor.addRule(new Rule.Builder()
                .post()
                .url("https://www.privateinternetaccess.com/vpninfo/debug_log")
                .respond(200));
        String[] list = new String[2];
        list[0] = "test";
        list[1] = "test";
        try {
            ReportResponse response = api.sendReport(list);
            Assert.assertNull(response.exception);
        } catch (IOException e) {
            throw new IllegalStateException("Response error");
        }
    }

    @After
    public void tearDown() throws Exception {
        PiaApi.setInterceptor(null);
    }
}
