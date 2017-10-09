/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.core.jsonrpc.commons;

import static com.google.common.collect.Sets.newConcurrentHashSet;

import com.google.inject.Singleton;
import java.util.Set;
import javax.inject.Inject;

/** A mechanism for handling subscription from the client and registered its endpointId. */
@Singleton
public class ClientSubscriptionHandler {
  public static final String CHE_CLIENT_INITIALIZE_METHOD_NAME = "cheClient/initialize";
  public static final String CHE_CLIENT_EXPIRE_METHOD_NAME = "cheClient/expire";

  private final Set<String> endpointIds = newConcurrentHashSet();

  @Inject
  private void configureHandlers(RequestHandlerConfigurator configurator) {
    configurator
        .newConfiguration()
        .methodName(CHE_CLIENT_INITIALIZE_METHOD_NAME)
        .noParams()
        .noResult()
        .withConsumer(endpointIds::add);

    configurator
        .newConfiguration()
        .methodName(CHE_CLIENT_EXPIRE_METHOD_NAME)
        .noParams()
        .noResult()
        .withConsumer(endpointIds::remove);
  }

  /** returns set of endpoint ids of all registered clients. */
  public Set<String> getEndpointIds() {
    return endpointIds;
  }
}
