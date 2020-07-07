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

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;

public class LoginResponseTest {

    @Test
    public void creationTest(){
        Assert.assertThat(new LoginResponse(), instanceOf(LoginResponse.class));
    }

    @Test
    public void parse_empty() {
        LoginResponse response = new LoginResponse();
        try {
            JSONObject object = new JSONObject("");
            response.parse(object);
        } catch (JSONException e) {  }
        Assert.assertNull(response.getEmail());
    }

    @Test
    public void parse_hasEmail() {
        LoginResponse response = new LoginResponse();
        try {
            JSONObject object = new JSONObject("{\"email\": \"dev@londontrustmedia.com\"}");
            response.parse(object);
        } catch (JSONException e) {
            throw new IllegalStateException("Error parsing");
        }
        Assert.assertEquals("dev@londontrustmedia.com", response.getEmail());
    }

    @Test
    public void parse_checkExpired() {
        LoginResponse response = new LoginResponse();
        try {
            JSONObject object = new JSONObject("{\"expired\": false}");
            response.parse(object);
        } catch (JSONException e) {
            throw new IllegalStateException("Error parsing");
        }
        Assert.assertFalse(response.isExpired());
    }
}