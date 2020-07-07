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

import com.privateinternetaccess.android.pia.model.PurchasingTestingData;
import com.privateinternetaccess.android.pia.model.exceptions.HttpResponseError;
import com.privateinternetaccess.android.pia.model.response.PurchasingResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import okhttp3.ResponseBody;
import okhttp3.mock.MockInterceptor;
import okhttp3.mock.Rule;

import static org.hamcrest.CoreMatchers.instanceOf;

import static okhttp3.mock.MediaTypes.MEDIATYPE_JSON;
import static org.mockito.Mockito.mock;

public class PurchasingApiTest {

    JSONObject testPurchasingJSON;
    MockInterceptor interceptor;
    PurchasingApi api;
    Context context;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        interceptor = new MockInterceptor();
        PiaApi.setInterceptor(interceptor);

        context = mock(Context.class);
        api = new PurchasingApi(context);

        testPurchasingJSON = api.createIAPJSON("2000","asdftg","1234","test@test.com",null, "1.0 (1)");
    }

    @Test
    public void signUp_not200_noResponse() throws IOException {
        interceptor.addRule(new Rule.Builder()
                .post()
                .url("https://www.privateinternetaccess.com/api/client/signup")
                .respond(401));
        PurchasingTestingData data = new PurchasingTestingData(false);
        PurchasingResponse response = api.signUp("","","","",null,data, 5);
        Assert.assertThat(response.getException(), instanceOf(HttpResponseError.class));
    }

    @Test
    public void signUp_200_emptyResponse() throws IOException {
        interceptor.reset();
        interceptor.addRule(new Rule.Builder()
                .post()
                .url("https://www.privateinternetaccess.com/api/client/signup")
                .respond(200)
                .body(ResponseBody.create(MEDIATYPE_JSON, "")));
        PurchasingTestingData data = new PurchasingTestingData(false);
        PurchasingResponse response = api.signUp("","","","",null,data, 5);
        Assert.assertNull(response.getException());
    }

    @Test
    public void signUp_200_emptyJSONResponse() throws IOException {
        interceptor.reset();
        interceptor.addRule(new Rule.Builder()
                .post()
                .url("https://www.privateinternetaccess.com/api/client/signup")
                .respond(200)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{}")));
        PurchasingTestingData data = new PurchasingTestingData(false);
        PurchasingResponse response = api.signUp("","","","",null,data, 5);
        Assert.assertTrue(response.getUsername().isEmpty());
    }

    @Test
    public void signUp_200_validResponse() throws IOException {
        interceptor.reset();
        interceptor.addRule(new Rule.Builder()
                .post()
                .url("https://www.privateinternetaccess.com/api/client/signup")
                .respond(200)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{\"username\":\"p1234567\", \"password\":\"testing1\"}")));
        PurchasingTestingData data = new PurchasingTestingData(false);
        PurchasingResponse response = api.signUp("","","","",null,data, 5);
        Assert.assertEquals("p1234567", response.getUsername());
        Assert.assertEquals("testing1", response.getPassword());
    }

    @Test
    public void signUp_200_testingActive() throws IOException {
        interceptor.reset();
        interceptor.addRule(new Rule.Builder()
                .post()
                .url("https://www.privateinternetaccess.com/api/client/signup")
                .respond(400)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{\"username\":\"p1234567\", \"password\":\"testing1\"}")));
        PurchasingTestingData data = new PurchasingTestingData(true, 200, "p7654321", "testing", null);
        PurchasingResponse response = api.signUp("","","","",null, data, 5);
        Assert.assertEquals("p7654321", response.getUsername());
        Assert.assertEquals("testing", response.getPassword());
        Assert.assertEquals(200, response.getResponseNumber());
    }

    @Test
    public void signUp_200_testingActive_exception() throws IOException {
        interceptor.reset();
        interceptor.addRule(new Rule.Builder()
                .post()
                .url("https://www.privateinternetaccess.com/api/client/signup")
                .respond(200)
                .body(ResponseBody.create(MEDIATYPE_JSON, "{\"username\":\"p1234567\", \"password\":\"testing1\"}")));
        PurchasingTestingData data = new PurchasingTestingData(true, 200, "p7654321", "testing", "bad crash");
        PurchasingResponse response = api.signUp("","","","",null, data, 5);
        Assert.assertNotNull(response.getException());
        Assert.assertEquals("bad crash", response.getResponse());
    }

    @Test
    public void createIAPJson_validateJSON() throws JSONException {
        Assert.assertNotNull(testPurchasingJSON);
    }

    @Test
    public void createIAPJson_validateRecieptJSON() throws JSONException {
        Assert.assertNotNull(testPurchasingJSON.getJSONObject("receipt"));
    }

    @Test
    public void createIAPJson_validateMarketingJSON() throws JSONException {
        Assert.assertNotNull(testPurchasingJSON.getJSONObject("marketing"));
    }

    @Test
    public void createIAPJson_correctReceiptData(){
        JSONObject reciept = testPurchasingJSON.optJSONObject("receipt");
        Assert.assertEquals("2000", reciept.optString("order_id"));
        Assert.assertEquals("asdftg", reciept.optString("token"));
        Assert.assertEquals("1234", reciept.optString("product_id"));
    }

    @Test
    public void createIAPJson_correctEmailPostiion(){
        Assert.assertEquals("test@test.com", testPurchasingJSON.optString("email"));
    }

    @Test
    public void createISPJson_correctVersionPosition(){
        Assert.assertEquals("1.0 (1)", testPurchasingJSON.optString("client_version"));
    }

    @After
    public void tearDown() throws Exception {
        PiaApi.setInterceptor(null);
    }
}
