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

package com.privateinternetaccess.android.pia;

import android.content.Context;

import com.privateinternetaccess.android.pia.impl.AccountImpl;
import com.privateinternetaccess.android.pia.impl.ConnectionImpl;
import com.privateinternetaccess.android.pia.impl.VPNImpl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class ImplementationTest {

    @Mock
    Context context;

    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);
        context = mock(Context.class);
    }

    @Test
    public void creationTest_accountImpl(){
        Assert.assertThat(new AccountImpl(context), instanceOf(AccountImpl.class));
    }

    @Test
    public void creationTest_connectionImpl(){
        Assert.assertThat(new ConnectionImpl(context), instanceOf(ConnectionImpl.class));
    }

    @Test
    public void creationTest_vpnImpl(){
        Assert.assertThat(new VPNImpl(context), instanceOf(VPNImpl.class));
    }
}
