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

package com.privateinternetaccess.android.ui.views

/*
 *  Copyright (c) 2021 Private Internet Access, Inc.
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

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.privateinternetaccess.android.R
import com.privateinternetaccess.android.model.events.ServerClickedEvent
import com.privateinternetaccess.android.pia.PIAFactory
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler
import com.privateinternetaccess.android.utils.DedicatedIpUtils
import com.privateinternetaccess.core.model.PIAServer
import org.greenrobot.eventbus.EventBus


class QuickConnectFavoritesView : FrameLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attributesSet: AttributeSet) : super(context, attributesSet)
    constructor(context: Context, attributesSet: AttributeSet, defStyleAttr: Int) : super(context, attributesSet, defStyleAttr)

    companion object {
        private const val FAVORITES_QUICK_CONNECT_MAX_SIZE = 6
    }

    private var favoritesLinearLayout: LinearLayout

    init {
        inflate(context, R.layout.view_quick_connect_favorites, this)
        favoritesLinearLayout = findViewById(R.id.quick_connect_favorites_linear_layout)

        prepareFavoriteViews(prepareFavoriteServers())
    }

    // region private
    private fun prepareFavoriteServers(): List<PIAServer> {
        val favoriteServers: MutableList<PIAServer> = mutableListOf()
        val persistedFavoriteIdentifiers = PiaPrefHandler.getFavorites(context).reversed()
        for (persistedFavoriteIdentifier in persistedFavoriteIdentifiers) {
            serverForFavoriteIdentifier(persistedFavoriteIdentifier)?.let {
                favoriteServers.add(it)
            }
        }
        return favoriteServers
    }

    private fun prepareFavoriteViews(favoriteServers: List<PIAServer>) {
        val favoriteServerViews: MutableList<FavoriteServerView> = mutableListOf()

        // Create the max supported of views
        for (idx in 0 until FAVORITES_QUICK_CONNECT_MAX_SIZE) {
            val favoriteServerView = FavoriteServerView(context)
            favoriteServerViews.add(favoriteServerView)

            // Add it to the view hierarchy
            val params = LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT,
                    1.0f
            )
            favoritesLinearLayout.addView(favoriteServerView, params)
        }

        // Chop off the extra if we have more favorites than the max supported by the widget
        val maxSupportedFavorites = if (favoriteServers.size > FAVORITES_QUICK_CONNECT_MAX_SIZE) {
            favoriteServers.subList(0, FAVORITES_QUICK_CONNECT_MAX_SIZE)
        } else {
            favoriteServers
        }

        // Bootstrap the necessary amount views
        for ((idx, favoriteServer) in maxSupportedFavorites.withIndex()) {
            val favoriteServerView = favoriteServerViews[idx]
            favoriteServerView.bootstrap(favoriteServer)
        }
    }

    private fun serverForFavoriteIdentifier(favoriteIdentifier: String): PIAServer? {

        // Look for it on the DIP list
        for (dipServer in PiaPrefHandler.getDedicatedIps(context)) {
            if (dipServer.ip == favoriteIdentifier) {
                return DedicatedIpUtils.serverForDip(dipServer, context)
            }
        }

        // Look for it on the regular list.
        val handler = PIAServerHandler.getInstance(context)
        for (server in handler.getServers(context, PIAServerHandler.ServerSortingType.NAME)) {
            if (server.name == favoriteIdentifier) {
                return server
            }
        }
        return null
    }

    private inner class FavoriteServerView : FrameLayout {

        constructor(context: Context) : super(context)
        constructor(context: Context, attributesSet: AttributeSet) : super(context, attributesSet)
        constructor(context: Context, attributesSet: AttributeSet, defStyleAttr: Int) : super(context, attributesSet, defStyleAttr)

        private var favoriteServerContainerConstraintLayout: ConstraintLayout
        private var favoriteServerFlagImageView: AppCompatImageView
        private var favoriteServerNameTextView: AppCompatTextView
        private var favoriteServerDIPIconImageView: AppCompatImageView


        init {
            inflate(context, R.layout.snippet_quick_connect_favorite, this)
            favoriteServerContainerConstraintLayout = findViewById(R.id.quick_server_favorite_container)
            favoriteServerFlagImageView = findViewById(R.id.quick_server_favorite_flag)
            favoriteServerNameTextView = findViewById(R.id.quick_server_favorite_name)
            favoriteServerDIPIconImageView = findViewById(R.id.quick_server_favorite_dip)
        }

        fun bootstrap(server: PIAServer) {
            prepareView(server)
            prepareServerClickListener(server)
        }

        // region private
        private fun prepareView(server: PIAServer) {
            favoriteServerFlagImageView.setImageResource(
                    PIAServerHandler.getInstance(context).getFlagResource(server.iso)
            )
            favoriteServerFlagImageView.contentDescription = server.name
            favoriteServerNameTextView.contentDescription = server.name
            favoriteServerNameTextView.text = server.iso

            if (server.isDedicatedIp()) {
                favoriteServerDIPIconImageView.visibility = View.VISIBLE
            }
        }

        private fun prepareServerClickListener(server: PIAServer) {
            favoriteServerContainerConstraintLayout.setOnClickListener {

                // Get the previously selected region identifier to validate we are not
                // tapping the same one.
                val handler = PIAServerHandler.getInstance(context)
                val previousRegionIdentifier = handler.getSelectedRegion(context, true)?.let {
                    if (it.isDedicatedIp()) {
                        it.dedicatedIp
                    } else {
                        it.key
                    }
                } ?: ""

                // DIP clones regular server objects with the DIP properties being the only
                // difference. When selecting them, we need to use their DIP as identifier to tell
                // them apart. PIA-408
                val tappedRegionIdentifier = if (server.isDedicatedIp()) {
                    server.dedicatedIp
                } else {
                    server.key
                }

                // Save the new server and broadcast its selection.
                handler.saveSelectedServer(context, tappedRegionIdentifier)
                EventBus.getDefault().post(ServerClickedEvent(server.name, server.name.hashCode()))

                // Connect to the selected server if it's not the same or if it is
                // but we are not connected.
                val vpn = PIAFactory.getInstance().getVPN(context)
                if (tappedRegionIdentifier != previousRegionIdentifier || !vpn.isVPNActive) {
                    vpn.start()
                }
            }
        }
        // endregion
    }
    // endregion
}