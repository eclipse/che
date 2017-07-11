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
package org.eclipse.che.ide.workspace.events;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.SubscriptionManagerClient;
import org.eclipse.che.ide.api.workspace.event.WorkspaceStoppedEvent;

import java.util.Map;

import static java.util.Collections.singletonMap;

/** Unsubscribes from receiving JSON-RPC notifications from WS-master when workspace is stopped. */
@Singleton
class WorkspaceEventsUnsubscriber {

    @Inject
    WorkspaceEventsUnsubscriber(EventBus eventBus, AppContext appContext, SubscriptionManagerClient subscriptionManagerClient) {
        eventBus.addHandler(WorkspaceStoppedEvent.TYPE, e -> {
            Map<String, String> scope = singletonMap("workspaceId", appContext.getWorkspaceId());

            // TODO (spi ide): consider shared constants for the endpoints
            subscriptionManagerClient.unSubscribe("ws-master", "workspace/statusChanged", scope);
            subscriptionManagerClient.unSubscribe("ws-master", "machine/statusChanged", scope);
            subscriptionManagerClient.unSubscribe("ws-master", "server/statusChanged", scope);
            subscriptionManagerClient.unSubscribe("ws-master-output", "machine/log", scope);
            subscriptionManagerClient.unSubscribe("ws-master-output", "installer/log", scope);
        });
    }
}
