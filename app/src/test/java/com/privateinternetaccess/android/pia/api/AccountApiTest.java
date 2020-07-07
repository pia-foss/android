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

import com.privateinternetaccess.android.pia.model.TrialData;
import com.privateinternetaccess.android.pia.model.TrialTestingData;
import com.privateinternetaccess.android.pia.model.UpdateAccountInfo;
import com.privateinternetaccess.android.pia.model.enums.LoginResponseStatus;
import com.privateinternetaccess.android.pia.model.response.LoginResponse;
import com.privateinternetaccess.android.pia.model.response.TrialResponse;
import com.privateinternetaccess.android.pia.model.response.UpdateEmailResponse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import okhttp3.ResponseBody;
import okhttp3.mock.MockInterceptor;
import okhttp3.mock.Rule;

import static okhttp3.mock.MediaTypes.MEDIATYPE_JSON;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class AccountApiTest {

    AccountApi api;
    MockInterceptor interceptor;
    Context context;

    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);
        interceptor = new MockInterceptor();
        PiaApi.setInterceptor(interceptor);

        api = new AccountApi(context);
        context = mock(Context.class);
    }

    @Test
    public void getLoginInformation_emptyReturn(){
        interceptor.addRule(new Rule.Builder()
                .get()
                .url("https://www.privateinternetaccess.com/api/client/v2/account")
                .respond(401));

        LoginResponse response = api.getAccountInformation("");
        Assert.assertSame(response.getStatus(), LoginResponseStatus.AUTH_FAILED);
    }

    @Test
    public void getLoginInformation_200_EmptyBody(){
        interceptor.reset();
        interceptor.addRule(new Rule.Builder()
                .get()
                .url("https://www.privateinternetaccess.com/api/client/v2/account")
                .respond(200).body(ResponseBody.create(MEDIATYPE_JSON, "{}")));

        LoginResponse response = api.getAccountInformation("");
        Assert.assertSame(response.getStatus(), LoginResponseStatus.CONNECTED);
    }

    @Test
    public void getLoginInformation_notEmptyBody(){
        interceptor.reset();
        interceptor.addRule(new Rule.Builder()
                .get()
                .url("https://www.privateinternetaccess.com/api/client/v2/account").respond(200)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{\"email\":\"test@test.com\"}")));

        LoginResponse response = api.getAccountInformation("");
        Assert.assertEquals("test@test.com", response.getEmail());
    }

    @Test
    public void getLoginInformation_200_validBody(){
        interceptor.reset();
        interceptor.addRule(new Rule.Builder()
                .get()
                .url("https://www.privateinternetaccess.com/api/client/v2/account").respond(200)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{" +
                        "\"plan\":\"monthly\"," +
                        "\"active\":true," +
                        "\"expiration_time\":0," +
                        "\"expired\":false," +
                        "\"expire_alert\":false," +
                        "\"renewable\":false," +
                        "\"email\":\"test@test.com\"}")));

        LoginResponse response = api.getAccountInformation("");
        Assert.assertTrue(response.isActive());
        Assert.assertFalse(response.isExpired());
        Assert.assertFalse(response.isRenewable());
    }

    @Test
    public void changeEmail_200(){
        interceptor.addRule(new Rule.Builder()
                .post()
                .url("https://www.privateinternetaccess.com/api/client/account").respond(200));

        UpdateEmailResponse response = api.changeEmail("", new UpdateAccountInfo("", ""));
        Assert.assertTrue(response.isChanged());
    }

    @Test
    public void changeEmail_400(){
        interceptor.reset();
        interceptor.addRule(new Rule.Builder()
                .post()
                .url("https://www.privateinternetaccess.com/api/client/account").respond(400));

        UpdateEmailResponse response = api.changeEmail("", new UpdateAccountInfo("", ""));
        Assert.assertFalse(response.isChanged());
    }


    @Test
    public void createGiftCardAccount_200_emptyBody(){
        interceptor.addRule(new Rule.Builder()
                .post()
                .url("https://www.privateinternetaccess.com/api/client/giftcard_redeem")
                .respond(200)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{}")));

        TrialData data = new TrialData("","");

        TrialResponse response = api.createTrialAccount(data, new TrialTestingData(false, 200,"","",""));
        Assert.assertTrue(response.getUsername().isEmpty());
    }


    @Test
    public void createGiftCardAccount_200_correctBody(){
        interceptor.reset();
        interceptor.addRule(new Rule.Builder()
                .post()
                .url("https://www.privateinternetaccess.com/api/client/giftcard_redeem")
                .respond(200)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{\"username\":\"p1234567\", \"password\":\"testing1\"}")));

        TrialData data = new TrialData("","");

        TrialResponse response = api.createTrialAccount(data, new TrialTestingData(false, 200,"","",""));
        Assert.assertEquals("p1234567", response.getUsername());
        Assert.assertEquals("testing1", response.getPassword());
    }

    @Test
    public void createGiftCardAccount_400(){
        interceptor.reset();
        interceptor.addRule(new Rule.Builder()
                .post()
                .url("https://www.privateinternetaccess.com/api/client/giftcard_redeem")
                .respond(400)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{}"))
        );

        TrialData data = new TrialData("","");

        TrialResponse response = api.createTrialAccount(data, new TrialTestingData(false, 200,"","",""));
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void createGiftCardAccount_testingDetails(){
        TrialData data = new TrialData("","");
        TrialTestingData testing = new TrialTestingData(true, 300, "blah", "p1234567","testing1");
        TrialResponse response = api.createTrialAccount(data, testing);
        Assert.assertEquals(300, response.getStatus());
        Assert.assertNotEquals("p123456", response.getUsername());
        Assert.assertEquals("testing1", response.getPassword());
    }

    @After
    public void endTesting(){
        PiaApi.setInterceptor(null);
    }

}
