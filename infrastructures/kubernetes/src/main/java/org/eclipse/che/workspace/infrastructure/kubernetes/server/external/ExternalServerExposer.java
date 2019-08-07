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

import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.che.api.core.model.workspace.config.ServerConfig;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.server.KubernetesServerResolver;

public class ExternalServerExposer<T extends KubernetesEnvironment> {
  private final IngressNamingStrategy strategy;
  private final Map<String, String> ingressAnnotations;
  private final String pathTransformFmt;

  @Inject
  public ExternalServerExposer(
      IngressNamingStrategy strategy,
      @Named("infra.kubernetes.ingress.annotations") Map<String, String> annotations,
      @Nullable @Named("che.infra.kubernetes.ingress.path_transform") String pathTransformFmt) {
    this.strategy = strategy;
    this.ingressAnnotations = annotations;
    this.pathTransformFmt = pathTransformFmt == null ? "%s" : pathTransformFmt;
  }

  /**
   * Exposes service ports on given service externally (outside kubernetes cluster). Each exposed
   * service port is associated with a specific Server configuration. Server configuration should be
   * encoded in the exposing object's annotations, to be used by {@link KubernetesServerResolver}.
   *
   * @param k8sEnv Kubernetes environment
   * @param machineName machine containing servers
   * @param serviceName service associated with machine, mapping all machine server ports
   * @param servicePort specific service port to be exposed externally
   * @param externalServers server configs of servers to be exposed externally
   */
  public void expose(
      T k8sEnv,
      String machineName,
      String serviceName,
      ServicePort servicePort,
      Map<String, ServerConfig> externalServers) {
    Ingress ingress = generateIngress(machineName, serviceName, servicePort, externalServers);
    k8sEnv.getIngresses().put(ingress.getMetadata().getName(), ingress);
  }

  private Ingress generateIngress(
      String machineName,
      String serviceName,
      ServicePort servicePort,
      Map<String, ServerConfig> ingressesServers) {

    ExternalServerIngressBuilder bld = new ExternalServerIngressBuilder();
    String host = strategy.getIngressHost(serviceName, servicePort);
    if (host != null) {
      bld = bld.withHost(host);
    }

    return bld.withPath(
            String.format(
                pathTransformFmt,
                ensureEndsWithSlash(strategy.getIngressPath(serviceName, servicePort))))
        .withName(strategy.getIngressName(serviceName, servicePort))
        .withMachineName(machineName)
        .withServiceName(serviceName)
        .withAnnotations(ingressAnnotations)
        .withServicePort(servicePort.getName())
        .withServers(ingressesServers)
        .build();
  }

  private static String ensureEndsWithSlash(String path) {
    if (path.charAt(path.length() - 1) == '/') {
      return path;
    } else {
      return path + '/';
    }
  }
}
