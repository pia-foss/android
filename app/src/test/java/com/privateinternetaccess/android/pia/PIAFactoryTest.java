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

import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.interfaces.IVPN;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class PIAFactoryTest {

    Context context;

    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);
        context = mock(Context.class);
    }

    @Test
    public void creationTest_getInstance() {
        Assert.assertThat(PIAFactory.getInstance(), instanceOf(PIAFactory.class));
    }

    @Test
    public void creationTest_getConnection() {
        Assert.assertThat(PIAFactory.getInstance().getAccount(context), instanceOf(IAccount.class));
    }

    @Test
    public void creationTest_getAccount() {
        Assert.assertThat(PIAFactory.getInstance().getVPN(context), instanceOf(IVPN.class));
    }
}