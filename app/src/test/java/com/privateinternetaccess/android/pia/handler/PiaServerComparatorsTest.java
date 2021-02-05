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

package com.privateinternetaccess.android.pia.handler;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.utils.KeyStoreUtils;
import com.privateinternetaccess.core.model.PIAServer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import kotlin.Pair;

@RunWith(RobolectricTestRunner.class)
public class PiaServerComparatorsTest {

    List<PIAServer> servers;
    HashSet<String> favorites;

    final String[] serverNames = new String[]{"US San Francisco","JP Tokyo", "UK London",
            "AU Sydney", "DE Berlin",
            "US New York", "CA Toronto",
            "WK Birnin Zana", "US Gotham City", "AZ Azeroth",
            "US South Park",  "HT HalloweenTown", "HP Godric Hallows",
            "SK Winterfell", "FC Braavos", "FC Hardhome"
            };

    final long[] pingsDefault = new long[]{62L, 138L, 99L,
            210L, 170L,
            34L, 40L,
            52L, 608L, 120L,
            108L, 1013L, 640L,
            999L, 999L, 999L};

    final String[] favs = new String[]{"US New York","CA Toronto","AU Sydney","JP Tokyo","DE Berlin"};

    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);
        initLists();

        Context context = ApplicationProvider.getApplicationContext();
        Prefs.setKeyStoreUtils(Mockito.mock(KeyStoreUtils.class));
        Prefs prefsSpy = Mockito.spy(Prefs.with(context));
        PIAServerHandler.setPrefs(prefsSpy);
    }

    public void initLists(){
        servers = new ArrayList<>();
        favorites = new HashSet<>();
        for(int i = 0; i < serverNames.length; i++){
            String name = serverNames[i];
            String key = nameToKey(name);
            servers.add(
                    new PIAServer(
                            name,
                            "iso",
                            "dns",
                            String.valueOf(pingsDefault[i]),
                            new HashMap<>(),
                            key,
                            "latitude",
                            "longitude",
                            false,
                            false,
                            false,
                            "dipToken",
                            "dedicatedIp"
                    )
            );
        }

        favorites.addAll(Arrays.asList(favs));
    }

    private String nameToKey(String name){
        return name.replace(" ", "_").toLowerCase();
    }

    //creation test fails due to needing context

    @Test
    public void sortingPingTest_firstPosition(){
        List<PIAServer> sortedByPing = new ArrayList<>(servers);
        Collections.sort(sortedByPing, new PIAServerHandler.PingComperator());
        PIAServer first = sortedByPing.get(0);
        Assert.assertEquals("us_new_york", first.getKey());
    }

    @Ignore("Waiting for the changes coming down to master with 3.7.0. Specifically MR #399")
    public void sortingPingTest_fourthPosition(){
        List<PIAServer> sortedByPing = new ArrayList<>(servers);
        Collections.sort(sortedByPing, new PIAServerHandler.PingComperator());
        PIAServer fourth = sortedByPing.get(3);
        Assert.assertEquals("us_san_francisco", fourth.getKey());
    }

    @Ignore("Waiting for the changes coming down to master with 3.7.0. Specifically MR #399")
    public void sortingPingTest_lastPosition(){
        List<PIAServer> sortedByPing = new ArrayList<>(servers);
        Collections.sort(sortedByPing, new PIAServerHandler.PingComperator());
        PIAServer last = sortedByPing.get(sortedByPing.size()-1);
        Assert.assertEquals("ht_halloweentown", last.getKey());
    }

    @Test
    public void sortingNameTest_firstPosition(){
        List<PIAServer> sortedByName = new ArrayList<>(servers);
        Collections.sort(sortedByName, new PIAServerHandler.ServerNameComperator());
        PIAServer first = sortedByName.get(0);
        Assert.assertEquals("au_sydney", first.getKey());
    }

    @Test
    public void sortingNameTest_fourthPosition(){
        List<PIAServer> sortedByName = new ArrayList<>(servers);
        Collections.sort(sortedByName, new PIAServerHandler.ServerNameComperator());
        PIAServer first = sortedByName.get(3);
        Assert.assertEquals("de_berlin", first.getKey());
    }

    @Test
    public void sortingNameTest_lastPosition(){
        List<PIAServer> sortedByName = new ArrayList<>(servers);
        Collections.sort(sortedByName, new PIAServerHandler.ServerNameComperator());
        PIAServer first = sortedByName.get(sortedByName.size()-1);
        Assert.assertEquals("wk_birnin_zana", first.getKey());
    }

    @Test
    public void sortingFavoritesTest_firstPosition(){
        List<PIAServer> sortedByFavorites = new ArrayList<>(servers);
        Collections.sort(sortedByFavorites, new PIAServerHandler.ServerNameComperator());
        Collections.sort(sortedByFavorites, new PIAServerHandler.FavoriteComperator(favorites));
        PIAServer first = sortedByFavorites.get(0);
        Assert.assertEquals("au_sydney", first.getKey());
    }

    @Test
    public void sortingFavoritesTest_fifthPosition(){
        List<PIAServer> sortedByFavorites = new ArrayList<>(servers);
        Collections.sort(sortedByFavorites, new PIAServerHandler.ServerNameComperator());
        Collections.sort(sortedByFavorites, new PIAServerHandler.FavoriteComperator(favorites));
        PIAServer first = sortedByFavorites.get(favorites.size()-1);
        Assert.assertEquals("us_new_york", first.getKey());
    }
    @Test
    public void sortingFavoritesTest_firstPosition_nonFavorite(){
        List<PIAServer> sortedByFavorites = new ArrayList<>(servers);
        Collections.sort(sortedByFavorites, new PIAServerHandler.ServerNameComperator());
        Collections.sort(sortedByFavorites, new PIAServerHandler.FavoriteComperator(favorites));
        PIAServer first = sortedByFavorites.get(favorites.size());
        Assert.assertEquals("az_azeroth", first.getKey());
    }

    @After
    public void close(){
        servers = null;
        favorites = null;
    }
}
