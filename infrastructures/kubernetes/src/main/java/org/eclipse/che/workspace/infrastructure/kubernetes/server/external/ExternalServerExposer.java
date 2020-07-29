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
package org.eclipse.che.workspace.infrastructure.kubernetes.server.external;

import static java.util.Objects.requireNonNull;

import io.fabric8.kubernetes.api.model.ServicePort;
import java.util.Map;
import org.eclipse.che.api.core.model.workspace.config.ServerConfig;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.server.resolver.ServerResolver;

/**
 * Helps to expose internal Che services to outside the cluster. Implementations should create
 * objects in given {@link KubernetesEnvironment}. These object must be properly annotated (see
 * {@link org.eclipse.che.workspace.infrastructure.kubernetes.Annotations}) so {@link
 * ServerResolver} can later use them.
 *
 * @param <T> environment type
 */
public interface ExternalServerExposer<T extends KubernetesEnvironment> {

  /**
   * Exposes service port on given service. The exposed service port is associated with a specific
   * Server configuration. Server configuration should be encoded in the exposing object's
   * annotations, to be used by {@link ServerResolver}.
   *
   * @param k8sEnv environment
   * @param machineName machine containing servers
   * @param serviceName service associated with machine, mapping all machine server ports
   * @param serverId non-null for a unique server, null for a compound set of servers that should be
   *     exposed together.
   * @param servicePort specific service port to be exposed externally
   * @param externalServers server configs of servers to be exposed externally
   */
  void expose(
      T k8sEnv,
      @Nullable String machineName,
      String serviceName,
      String serverId,
      ServicePort servicePort,
      Map<String, ServerConfig> externalServers);

  /** Lists all the possible server resolver strategies. */
  enum Type {
    GATEWAY("gateway"),
    NATIVE("native");

    private final String configValue;

    Type(String configValue) {
      this.configValue = configValue;
    }

    public String getConfigValue() {
      return configValue;
    }

    public static Type fromConfigurationValue(String configValue) {
      String vale = requireNonNull(configValue);
      for (Type s : Type.values()) {
        if (s.configValue.equals(configValue)) {
          return s;
        }
      }

      throw new IllegalArgumentException("Unknown server resolver strategy: " + configValue);
    }
  }
}
