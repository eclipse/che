/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.api.machine.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/** Fired when server ws-agent in a machine is stopped. */
public class WsAgentServerStoppedEvent extends GwtEvent<WsAgentServerStoppedEvent.Handler> {

    public static final Type<WsAgentServerStoppedEvent.Handler> TYPE = new Type<>();

    @Override
    public Type<Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(Handler handler) {
        handler.onWsAgentServerStopped(this);
    }

    public interface Handler extends EventHandler {
        void onWsAgentServerStopped(WsAgentServerStoppedEvent event);
    }
}
