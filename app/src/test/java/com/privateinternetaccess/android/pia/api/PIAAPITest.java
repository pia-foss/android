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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import okhttp3.mock.MockInterceptor;
import okhttp3.mock.Rule;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class PIAAPITest {

    MockInterceptor interceptor;
    Context context;

    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);
        interceptor = new MockInterceptor();
        context = mock(Context.class);
    }

    @Test
    public void creationTest_piaApi(){
        Assert.assertThat(new PiaApi(), instanceOf(PiaApi.class));
    }

    @Test
    public void selectBaseURLTest_defaultAvailable() {
        interceptor.addRule(new Rule.Builder()
                .get()
                .url(PiaApi.PROXY_PATHS.get(0))
                .respond(200));
        Assert.assertEquals(PiaApi.getBaseURL(context), PiaApi.PROXY_PATHS.get(0));
    }

    @Test
    public void selectBaseURLTest_defaultUnavailable() {
        interceptor.reset();
        interceptor.addRule(new Rule.Builder()
                .get()
                .url(PiaApi.PROXY_PATHS.get(0))
                .respond(401));
        Assert.assertNotEquals(PiaApi.getBaseURL(context), PiaApi.PROXY_PATHS.get(1));
    }

    @Test
    public void selectBaseURLTest_allUnavailable() {
        interceptor.reset();

        for (String path : PiaApi.PROXY_PATHS) {
            interceptor.addRule(new Rule.Builder()
                    .get()
                    .url(path)
                    .respond(401));
        }

        Assert.assertEquals(PiaApi.getBaseURL(context), PiaApi.PROXY_PATHS.get(0));
    }

}
