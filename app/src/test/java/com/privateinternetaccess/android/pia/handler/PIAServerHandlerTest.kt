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

package com.privateinternetaccess.android.pia.handler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler.LAST_SERVER_BODY
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler
import com.privateinternetaccess.android.pia.utils.Prefs
import com.privateinternetaccess.android.utils.KeyStoreUtils
import com.privateinternetaccess.common.regions.RegionLowerLatencyInformation
import com.privateinternetaccess.common.regions.RegionsProtocol
import com.privateinternetaccess.common.regions.model.RegionsResponse
import com.privateinternetaccess.regions.RegionsAPI
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PIAServerHandlerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        context = ApplicationProvider.getApplicationContext()
        Prefs.setKeyStoreUtils(Mockito.mock(KeyStoreUtils::class.java))
        PIAServerHandler.releaseInstance()
    }

    @Test
    fun testGen4WorkersAllocation() {
        val prefsSpy = Mockito.spy(Prefs.with(context))
        Mockito.`when`(prefsSpy.get(PiaPrefHandler.GEN4_ACTIVE, true)).thenReturn(true)
        Mockito.`when`(prefsSpy.get(LAST_SERVER_BODY, "")).thenReturn("{}")
        PIAServerHandler.setPrefs(prefsSpy)

        PIAServerHandler.getInstance(context).fetchServers(context, true)
        Assert.assertNotNull(PIAServerHandler.getInstance(context).pingIntent)
        Assert.assertNotNull(PIAServerHandler.getInstance(context).fetchServersIntent)
    }

    @Test
    fun testGen4WorkersRelease() {
        val prefsSpy = Mockito.spy(Prefs.with(context))
        Mockito.`when`(prefsSpy.get(PiaPrefHandler.GEN4_ACTIVE, true)).thenReturn(false)
        Mockito.`when`(prefsSpy.get(LAST_SERVER_BODY, "")).thenReturn("{}")
        PIAServerHandler.setAlarmManager(Mockito.mock(AlarmManager::class.java))
        PIAServerHandler.setPrefs(prefsSpy)

        PIAServerHandler.getInstance(context).fetchServersIntent = Mockito.mock(PendingIntent::class.java)
        PIAServerHandler.getInstance(context).pingIntent = Mockito.mock(PendingIntent::class.java)

        PIAServerHandler.getInstance(context).fetchServers(context, true)
        Assert.assertNull(PIAServerHandler.getInstance(context).pingIntent)
        Assert.assertNull(PIAServerHandler.getInstance(context).fetchServersIntent)
    }

    @Test
    fun testGen4ServerFetchingInvocationOnObjectInitialization() {
        val prefsSpy = Mockito.spy(Prefs.with(context))
        Mockito.`when`(prefsSpy.get(PiaPrefHandler.GEN4_ACTIVE, true)).thenReturn(true)
        Mockito.`when`(prefsSpy.get(LAST_SERVER_BODY, "")).thenReturn("{}")
        val regionsSpy = Mockito.spy(MockRegionsApi(mockResponse = false))
        PIAServerHandler.setPrefs(prefsSpy)
        PIAServerHandler.setRegionModule(regionsSpy)

        PIAServerHandler.getInstance(context)
        verify(regionsSpy).fetchRegions(any(), any())
    }

    @Test
    fun testGen4ServerFetchingInvocationAfterObjectInitialization() {
        val prefsSpy = Mockito.spy(Prefs.with(context))
        Mockito.`when`(prefsSpy.get(PiaPrefHandler.GEN4_ACTIVE, true)).thenReturn(true)
        Mockito.`when`(prefsSpy.get(LAST_SERVER_BODY, "")).thenReturn("{}")
        val regionsSpy = Mockito.spy(MockRegionsApi(mockResponse = false))
        PIAServerHandler.setPrefs(prefsSpy)
        PIAServerHandler.setRegionModule(regionsSpy)

        PIAServerHandler.getInstance(context)
        verify(regionsSpy).fetchRegions(any(), any())
    }

    @Test
    fun testGen4ServerPingAfterFetching() {
        val prefsSpy = Mockito.spy(Prefs.with(context))
        Mockito.`when`(prefsSpy.get(PiaPrefHandler.GEN4_ACTIVE, true)).thenReturn(true)
        Mockito.`when`(prefsSpy.get(LAST_SERVER_BODY, "")).thenReturn("{}")
        val regionsSpy = Mockito.spy(MockRegionsApi(mockResponse = false))
        PIAServerHandler.setPrefs(prefsSpy)
        PIAServerHandler.setRegionModule(regionsSpy)

        PIAServerHandler.getInstance(context).fetchServers(context, true)

        // The initial request when the object is allocated + the explicit call to `fetchServers`
        verify(regionsSpy, Mockito.times(2)).pingRequests(any(), any())
    }

    @Ignore("Waiting for the introduction of `RegionsUtils`")
    fun testGen4ServerResponsePersistence() {
        val prefsSpy = Mockito.spy(Prefs.with(context))
        Mockito.`when`(prefsSpy.get(PiaPrefHandler.GEN4_ACTIVE, true)).thenReturn(true)
        Mockito.`when`(prefsSpy.get(LAST_SERVER_BODY, "")).thenReturn("{}")
        val regionsSpy = Mockito.spy(MockRegionsApi(mockResponse = true))
        PIAServerHandler.setPrefs(prefsSpy)
        PIAServerHandler.setRegionModule(regionsSpy)

        PIAServerHandler.getInstance(context).fetchServers(context, true)
        verify(prefsSpy).set(LAST_SERVER_BODY, "")
    }
}

private class MockRegionsApi(private val mockResponse: Boolean) : RegionsAPI {

    override fun fetchRegions(locale: String, callback: (response: RegionsResponse?, error: Error?) -> Unit) {
        if (mockResponse) {
            callback(Mockito.mock(RegionsResponse::class.java), null)
        } else {
            callback(null, Error("Tests"))
        }
    }

    override fun pingRequests(protocol: RegionsProtocol, callback: (response: List<RegionLowerLatencyInformation>, error: Error?) -> Unit) {
        callback(emptyList(), null)
    }
}