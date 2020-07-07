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

package com.privateinternetaccess.android.pia.interfaces;

import com.privateinternetaccess.android.pia.model.events.FetchIPEvent;
import com.privateinternetaccess.android.pia.model.events.PortForwardEvent;
import com.privateinternetaccess.android.pia.model.response.MaceResponse;
import com.privateinternetaccess.android.pia.tasks.HitMaceTask;
import com.privateinternetaccess.android.pia.tasks.PortForwardTask;
import com.privateinternetaccess.core.utils.IPIACallback;

/**
 *
 * This is how you interact with the PIA backend about connection information.
 *
 * Please check out {@link com.privateinternetaccess.android.pia.connection.ConnectionResponder} before implementing many of these.
 *
 * Created by hfrede on 9/6/17.
 */

public interface IConnection {

    /**
     * Use this to set the non-Eventbus callbacks for the {@link com.privateinternetaccess.android.pia.connection.ConnectionResponder}
     *
     * @param ipEvent
     * @param port
     * @param mace
     */
    void setConnectionResponderCallbacks(IPIACallback<FetchIPEvent> ipEvent, IPIACallback<PortForwardEvent> port, IPIACallback<MaceResponse> mace);

    /**
     * Executes {@link com.privateinternetaccess.android.pia.tasks.FetchIPTask}
     *
     * Calling this multiple times will not change a thing. {@link com.privateinternetaccess.android.pia.tasks.FetchIPTask} is a singleton.
     *
     * Responses:
     * Eventbus & callback - {@link com.privateinternetaccess.android.pia.model.events.FetchIPEvent}
     *
     */
    void fetchIP(IPIACallback<FetchIPEvent> callback);

    /**
     * Use this to clear {@link com.privateinternetaccess.android.pia.tasks.FetchIPTask}.
     *
     * Use this when you don't want the last saved IP information any more and want {@link com.privateinternetaccess.android.pia.tasks.FetchIPTask} to refresh all IP related values.
     *
     */
    void resetFetchIP();

    /**
     * Executes {@link com.privateinternetaccess.android.pia.tasks.HitMaceTask}
     *
     * Responses:
     * Eventbus - {@link com.privateinternetaccess.android.pia.model.events.HitMaceEvent}
     * Callback - {@link MaceResponse}
     *
     * @param callback
     */
    HitMaceTask hitMace(IPIACallback<MaceResponse> callback);

    /**
     * Executes the {@link com.privateinternetaccess.android.pia.tasks.PortForwardTask}
     *
     * Responses:
     * Eventbus & Callback - {@link PortForwardEvent}
     *
     * @param callback
     */
    PortForwardTask fetchPort(IPIACallback<PortForwardEvent> callback);

    /**
     *
     * @return last port number saved
     */
    String getPort();

    /**
     *
     * @return last saved IP
     */
    String getSavedIP();

    /**
     *
     * @return if mace has been called.
     */
    boolean hasHitMace();
}
