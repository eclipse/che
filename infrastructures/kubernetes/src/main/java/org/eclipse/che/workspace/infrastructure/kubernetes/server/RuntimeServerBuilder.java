/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.kubernetes.server;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.che.api.core.model.workspace.config.ServerConfig;
import org.eclipse.che.api.core.model.workspace.runtime.ServerStatus;
import org.eclipse.che.api.workspace.server.model.impl.ServerImpl;
import org.eclipse.che.api.workspace.shared.Constants;

/**
 * Helper class to build {@link ServerImpl} from parts like port, host, path, etc. It also adds port
 * that let to server creation as attribute {@link
 * org.eclipse.che.api.workspace.shared.Constants#SERVER_TARGET_PORT_ATTRIBUTE}
 *
 * @author Oleksandr Garagatyi
 */
public class RuntimeServerBuilder {

  private String protocol;
  private String host;
  private String port;
  private String path;
  private Map<String, String> attributes;
  private String targetPort;

  public RuntimeServerBuilder protocol(String protocol) {
    this.protocol = protocol;
    return this;
  }

  public RuntimeServerBuilder host(String host) {
    this.host = host;
    return this;
  }

  public RuntimeServerBuilder port(String port) {
    this.port = port;
    return this;
  }

  public RuntimeServerBuilder path(String path) {
    this.path = path;
    return this;
  }

  public RuntimeServerBuilder attributes(Map<String, String> attributes) {
    this.attributes = attributes;
    return this;
  }

  public RuntimeServerBuilder targetPort(String targetPort) {
    this.targetPort = removeSuffix(targetPort);
    return this;
  }

  public ServerImpl build() {
    StringBuilder ub = new StringBuilder();
    if (protocol != null) {
      ub.append(protocol).append("://");
    } else {
      ub.append("tcp://");
    }
    ub.append(host);
    if (port != null) {
      ub.append(':').append(removeSuffix(port));
    }
    if (path != null) {
      if (!path.isEmpty() && !path.startsWith("/")) {
        ub.append("/");
      }
      ub.append(path);
    }
    Map<String, String> completeAttributes = new HashMap<>(attributes);
    completeAttributes.put(Constants.SERVER_TARGET_PORT_ATTRIBUTE, targetPort);
    return new ServerImpl()
        .withUrl(ub.toString())
        .withStatus(ServerStatus.UNKNOWN)
        .withAttributes(completeAttributes);
  }

  /** Removes suffix of {@link ServerConfig} such as "/tcp" when port value "8080/tcp". */
  private String removeSuffix(String port) {
    return port.split("/")[0];
  }
}
