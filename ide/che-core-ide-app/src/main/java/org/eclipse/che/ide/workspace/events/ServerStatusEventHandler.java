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

import org.eclipse.che.api.core.jsonrpc.commons.RequestHandlerConfigurator;
import org.eclipse.che.api.workspace.shared.dto.event.ServerStatusEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.machine.events.ServerRunningEvent;
import org.eclipse.che.ide.api.machine.events.ServerStoppedEvent;
import org.eclipse.che.ide.api.machine.events.WsAgentServerRunningEvent;
import org.eclipse.che.ide.api.machine.events.WsAgentServerStoppedEvent;
import org.eclipse.che.ide.context.AppContextImpl;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.workspace.WorkspaceServiceClient;

import java.util.function.BiConsumer;

import static org.eclipse.che.api.core.model.workspace.runtime.ServerStatus.RUNNING;
import static org.eclipse.che.api.core.model.workspace.runtime.ServerStatus.STOPPED;
import static org.eclipse.che.api.machine.shared.Constants.WSAGENT_REFERENCE;

@Singleton
class ServerStatusEventHandler {

    @Inject
    ServerStatusEventHandler(RequestHandlerConfigurator configurator,
                             EventBus eventBus,
                             AppContext appContext,
                             WorkspaceServiceClient workspaceServiceClient) {
        BiConsumer<String, ServerStatusEvent> operation = (String endpointId, ServerStatusEvent event) -> {
            Log.debug(getClass(), "Received notification from endpoint: " + endpointId);

            workspaceServiceClient.getWorkspace(appContext.getWorkspaceId()).then(workspace -> {
                // update workspace model stored in AppContext before firing an event
                // because AppContext must always return actual workspace model
                ((AppContextImpl)appContext).setWorkspace(workspace);

                if (event.getStatus() == RUNNING) {
                    eventBus.fireEvent(new ServerRunningEvent(event.getServerName(), event.getMachineName()));

                    if (WSAGENT_REFERENCE.equals(event.getServerName())) {
                        eventBus.fireEvent(new WsAgentServerRunningEvent());
                    }
                } else if (event.getStatus() == STOPPED) {
                    eventBus.fireEvent(new ServerStoppedEvent(event.getServerName(), event.getMachineName()));

                    if (WSAGENT_REFERENCE.equals(event.getServerName())) {
                        eventBus.fireEvent(new WsAgentServerStoppedEvent());
                    }
                }
            });
        };

        configurator.newConfiguration()
                    .methodName("server/statusChanged")
                    .paramsAsDto(ServerStatusEvent.class)
                    .noResult()
                    .withBiConsumer(operation);
    }
}
