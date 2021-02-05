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

package com.privateinternetaccess.android.pia.api

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.privateinternetaccess.android.pia.api.PiaApi.ANDROID_HTTP_CLIENT
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler
import com.privateinternetaccess.android.pia.model.exceptions.PortForwardingError
import com.privateinternetaccess.android.utils.ServerUtils
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime

class Gen4PortForwardApi : PIACertPinningAPI() {

    companion object {
        private const val MIN_EXPIRATION_DAYS = 7
    }

    @Serializable
    private data class DecodedPayload(
            @SerialName("port")
            val port: Int,
            @SerialName("token")
            val token: String,
            @SerialName("expirationDate")
            val expirationDate: String
    )

    @Serializable
    private data class PortBindInformation(
            @SerialName("payload")
            val payload: String,
            @SerialName("signature")
            val signature: String,
            @SerialName("decodedPayload")
            val decodedPayload: DecodedPayload
    )

    @Throws(IOException::class, IllegalStateException::class, PortForwardingError::class)
    fun bindPort(context: Context): Int {
        val gateway = PiaPrefHandler.getGatewayEndpoint(context)
        if (gateway.isNullOrEmpty()) {
            throw PortForwardingError("Invalid gateway.")
        }

        // Set the gateway's CN for the selected protocol before the binding request
        val server = PIAServerHandler.getInstance(context).getSelectedRegion(context, false)
        server.endpoints[ServerUtils.getUserSelectedProtocol(context)]?.let {
            val tunnelCommonName = mutableListOf<Pair<String, String>>()
            for ((_, commonName) in it) {
                tunnelCommonName.add(Pair(gateway, commonName))
            }
            setKnownEndpointCommonName(tunnelCommonName)
        }

        // If there is active data persisted. Send the bind port reminder request to keep the NAT
        // on the server rather than requesting a new port
        getPortBindInformation(context)?.let {
            if (tokenExpirationDateDaysLeft(it.decodedPayload.expirationDate) > MIN_EXPIRATION_DAYS) {
                bindPortRequest(it.decodedPayload.token, it.payload, it.signature, gateway)
                return it.decodedPayload.port
            }
        }

        val applicationToken = PiaPrefHandler.getAuthToken(context)
        if (applicationToken.isNullOrEmpty()) {
            throw PortForwardingError("Invalid token.")
        }

        val payloadSignature = fetchPayloadAndSignature(applicationToken, gateway)
        val payload = payloadSignature.first
        val signature = payloadSignature.second
        val decodedPayload = decodePayload(payload)
        val port = decodedPayload.port
        bindPortRequest(decodedPayload.token, payload, signature, gateway)
        val portBindInformation = PortBindInformation(payload, signature, decodedPayload)
        persistPortBindInformation(context, portBindInformation)
        return port
    }

    fun clearBindPort(context: Context) {
        PiaPrefHandler.clearBindPortForwardInformation(context)
    }

    // region private
    @Throws(IOException::class, IllegalStateException::class, PortForwardingError::class)
    private fun bindPortRequest(token: String, payload: String, signature: String, endpoint: String) {
        val urlEncodedEndpoint: String = Uri.parse("https://$endpoint:19999/bindPort")
                .buildUpon()
                .appendQueryParameter("payload", payload)
                .appendQueryParameter("signature", signature)
                .build().toString()
        val request = Request.Builder().url(
                urlEncodedEndpoint
        ).addHeader("User-Agent", ANDROID_HTTP_CLIENT).addHeader("Authorization", token).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw PortForwardingError("Request failed. Unsuccessful: " + response.message)
        }
        val jsonString = response.body?.string()
                ?: throw PortForwardingError("Request failed. Invalid response.")
        val jsonResponse = JSONObject(jsonString)
        if (!jsonResponse.has("status") || jsonResponse["status"] != "OK") {
            throw PortForwardingError("Request failed. Status unsuccessful.")
        }
    }

    @Throws(IOException::class, PortForwardingError::class)
    private fun fetchPayloadAndSignature(token: String, endpoint: String): Pair<String, String> {
        var payload: String? = null
        var signature: String? = null
        val urlEncodedEndpoint: String = Uri.parse("https://$endpoint:19999/getSignature")
                .buildUpon()
                .appendQueryParameter("token", token)
                .build().toString()
        val request = Request.Builder().url(
                urlEncodedEndpoint
        ).addHeader("User-Agent", ANDROID_HTTP_CLIENT).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw PortForwardingError("Request failed. Unsuccessful: " + response.message)
        }
        val jsonString = response.body?.string()
                ?: throw PortForwardingError("Request failed. Invalid response.")
        val jsonResponse = JSONObject(jsonString)
        if (!jsonResponse.has("status") || jsonResponse["status"] != "OK") {
            throw PortForwardingError("Request failed. Status unsuccessful.")
        }
        if (jsonResponse.has("payload")) {
            payload = jsonResponse["payload"].toString()
        }
        if (jsonResponse.has("signature")) {
            signature = jsonResponse["signature"].toString()
        }
        if (payload == null || signature == null) {
            throw PortForwardingError("Request failed. Mandatory field missing.")
        }
        return Pair(payload, signature)
    }

    @Throws(PortForwardingError::class)
    private fun decodePayload(payload: String): DecodedPayload {
        var port: Int? = null
        var token: String? = null
        var expirationDate: String? = null
        val decodedString = String(Base64.decode(payload, Base64.DEFAULT))
        val json = JSONObject(decodedString)
        if (json.has("token")) {
            token = json["token"].toString()
        }
        if (json.has("port")) {
            port = json.optInt("port")
        }
        if (json.has("expires_at")) {
            expirationDate = json["expires_at"].toString()
        }
        if (port == null || token == null || expirationDate == null) {
            throw PortForwardingError("Decoding failed. Mandatory field missing.")
        }
        return DecodedPayload(port, token, expirationDate)
    }

    private fun persistPortBindInformation(
            context: Context,
            portBindInformation: PortBindInformation
    ) {
        val stringPortBindInformation = Json.encodeToString(PortBindInformation.serializer(), portBindInformation)
        PiaPrefHandler.setBindPortForwardInformation(context, stringPortBindInformation)
    }

    private fun getPortBindInformation(context: Context): PortBindInformation? {
        val stringPortBindInformation = PiaPrefHandler.getBindPortForwardInformation(context)
        return stringPortBindInformation?.let {
            Json.decodeFromString(PortBindInformation.serializer(), it)
        }
    }

    @UseExperimental(ExperimentalTime::class)
    private fun tokenExpirationDateDaysLeft(
            tokenExpirationDate: String
    ): Long {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        val expirationDate = format.parse(tokenExpirationDate)
        return TimeUnit.DAYS.convert(expirationDate.time - Date().time, TimeUnit.MILLISECONDS)
    }

    // endregion
}