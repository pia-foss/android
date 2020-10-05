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

package com.privateinternetaccess.android.ui.rating

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.privateinternetaccess.android.BuildConfig
import com.privateinternetaccess.android.PIAApplication
import com.privateinternetaccess.android.R
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent
import de.blinkt.openvpn.core.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.time.ExperimentalTime

class Rating(val context: Context) : CoroutineScope {

    companion object {

        private const val INITIAL_RATING_COUNTER = 3
        private const val RATING_REMINDER_COUNTER = 50
        private const val REVIEW_REMINDER_WAIT_DAYS = 30
        private const val SUPPORT_URL = "https://www.privateinternetaccess.com/helpdesk/new-ticket"
        private const val APP_URL = "market://details?id=com.privateinternetaccess.android"
        private var rating: Rating? = null

        fun start(context: Context) {
            if (BuildConfig.FLAVOR_store != "playstore" || PIAApplication.isAndroidTV(context)) {
                return
            }
            rating = Rating(context)
            rating?.start()
        }

        fun stop() {
            rating?.stop()
            rating = null
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    // Because the state events are being reported multiple time per connection cycle, we need
    // to make sure a connected is counted with its disconnection accordingly.
    private var shouldCountConnectedEvent = true

    @Subscribe
    fun updateState(event: VpnStateEvent) {
        updateStateOnMainThread(event)
    }

    // region Private
    private fun start() {
        if (!getRatingState().active) {
            return
        }

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    private fun stop() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

    private fun updateStateOnMainThread(event: VpnStateEvent) = launch {
        event.level?.let {
            when(it) {
                ConnectionStatus.LEVEL_CONNECTED -> {
                    if (getRatingState().active && shouldCountConnectedEvent) {
                        shouldCountConnectedEvent = false
                        handleConnectedEvent()
                    }
                }
                ConnectionStatus.LEVEL_NOTCONNECTED -> {
                    shouldCountConnectedEvent = true
                }
                ConnectionStatus.LEVEL_VPNPAUSED,
                ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED,
                ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
                ConnectionStatus.LEVEL_NONETWORK,
                ConnectionStatus.LEVEL_START,
                ConnectionStatus.LEVEL_AUTH_FAILED,
                ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT,
                ConnectionStatus.UNKNOWN_LEVEL -> { }
            }
        }
    }

    private fun handleConnectedEvent() {
        val knownState = getRatingState()
        val updatedState = knownState.copy(counter = knownState.counter + 1)
        setRatingState(updatedState)

        if (updatedState.counter == INITIAL_RATING_COUNTER && updatedState.notEnjoyingDate == null) {
            showEnjoyingPrompt()
        } else if (
                updatedState.counter >= RATING_REMINDER_COUNTER &&
                updatedState.notEnjoyingDate != null &&
                daysPassedSinceNotEnjoyingReply(
                        updatedState.notEnjoyingDate
                ) >= REVIEW_REMINDER_WAIT_DAYS
        ) {
            showReviewPrompt()
        }
    }

    private fun showEnjoyingPrompt() {
        AlertDialog.Builder(context).
                setTitle(context.getString(R.string.rating_enjoy_title)).
                setMessage(context.getString(R.string.rating_enjoy_message)).
                setPositiveButton(context.getString(R.string.refer_yes)) { _, _ ->
                    showReviewPrompt()
                }.
                setNegativeButton(context.getString(R.string.refer_no)) { _, _ ->
                    showFeedbackPrompt()
                }.
                create().
                show()
    }

    private fun showReviewPrompt() {
        AlertDialog.Builder(context).
                setTitle(context.getString(R.string.rating_review_title)).
                setMessage(context.getString(R.string.rating_review_message)).
                setPositiveButton(context.getString(R.string.refer_yes)) { _, _ ->
                    val launchIntent = Intent(Intent.ACTION_VIEW)
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    launchIntent.data = Uri.parse(APP_URL)

                    // Silently fail if Google Play Store isn't installed.
                    if (launchIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(launchIntent)
                    }

                    val updatedState = getRatingState().copy(active = false)
                    setRatingState(updatedState)
                }.
                setNegativeButton(context.getString(R.string.refer_no)) { _, _ ->
                    val updatedState = getRatingState().copy(active = false)
                    setRatingState(updatedState)
                }.
                create().
                show()
    }

    private fun showFeedbackPrompt() {
        AlertDialog.Builder(context).
                setTitle(context.getString(R.string.rating_feedback_title)).
                setMessage(context.getString(R.string.rating_feedback_message)).
                setPositiveButton(context.getString(R.string.refer_yes)) { _, _ ->
                    val launchIntent = Intent(Intent.ACTION_VIEW)
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    launchIntent.data = Uri.parse(SUPPORT_URL)

                    // Silently fail if Google Play Store isn't installed.
                    if (launchIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(launchIntent)
                    }
                }.
                setNegativeButton(context.getString(R.string.refer_no)) { _, _ ->
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy")
                    val dateString = dateFormat.format(Date())
                    val updatedState = getRatingState().copy(notEnjoyingDate = dateString)
                    setRatingState(updatedState)
                }.
                create().
                show()
    }

    private fun setRatingState(state: RatingState) {
        PiaPrefHandler.setRatingState(
                context,
                Json.stringify(RatingState.serializer(), state)
        )
    }

    private fun getRatingState(): RatingState {
        val stringState = PiaPrefHandler.getRatingState(context)

        // If there is no initial state. Defaults to active.
        return stringState?.let {
            Json.parse(RatingState.serializer(), it)
        } ?: RatingState(active = true, counter = 0)
    }
    
    @UseExperimental(ExperimentalTime::class)
    private fun daysPassedSinceNotEnjoyingReply(
            dateString: String
    ): Long {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy")
        val date = dateFormat.parse(dateString)
        return TimeUnit.DAYS.convert(Date().time - date.time, TimeUnit.MILLISECONDS)
    }

    @Serializable
    private data class RatingState(
            @SerialName("active")
            val active: Boolean,
            @SerialName("counter")
            val counter: Int,
            @SerialName("decodedPayload")
            val notEnjoyingDate: String? = null
    )
    // endregion
}