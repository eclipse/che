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
package org.eclipse.che.workspace.infrastructure.openshift;

import static java.lang.String.format;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.InternalInfrastructureException;
import org.eclipse.che.api.workspace.server.spi.RuntimeContext;
import org.eclipse.che.api.workspace.server.spi.RuntimeInfrastructure;
import org.eclipse.che.api.workspace.server.spi.environment.InternalEnvironment;
import org.eclipse.che.api.workspace.server.spi.provision.InternalEnvironmentProvisioner;
import org.eclipse.che.workspace.infrastructure.docker.environment.dockerimage.DockerImageEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.cache.KubernetesRuntimeStateCache;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.convert.DockerImageEnvironmentConverter;
import org.eclipse.che.workspace.infrastructure.kubernetes.wsnext.SidecarToolingProvisioner;
import org.eclipse.che.workspace.infrastructure.openshift.environment.OpenShiftEnvironment;

/** @author Sergii Leshchenko */
@Singleton
public class OpenShiftInfrastructure extends RuntimeInfrastructure {

  public static final String NAME = "openshift";

  private final DockerImageEnvironmentConverter dockerImageEnvConverter;
  private final OpenShiftRuntimeContextFactory runtimeContextFactory;
  private final OpenShiftEnvironmentProvisioner osEnvProvisioner;
  private final KubernetesRuntimeStateCache runtimeStatusesCache;
  private final SidecarToolingProvisioner workspaceNext;

  @Inject
  public OpenShiftInfrastructure(
      EventService eventService,
      OpenShiftRuntimeContextFactory runtimeContextFactory,
      OpenShiftEnvironmentProvisioner osEnvProvisioner,
      Set<InternalEnvironmentProvisioner> internalEnvProvisioners,
      DockerImageEnvironmentConverter dockerImageEnvConverter,
      KubernetesRuntimeStateCache runtimeStatusesCache,
      SidecarToolingProvisioner workspaceNext) {
    super(
        NAME,
        ImmutableSet.of(
            OpenShiftEnvironment.TYPE, KubernetesEnvironment.TYPE, DockerImageEnvironment.TYPE),
        eventService,
        internalEnvProvisioners);
    this.runtimeContextFactory = runtimeContextFactory;
    this.osEnvProvisioner = osEnvProvisioner;
    this.dockerImageEnvConverter = dockerImageEnvConverter;
    this.runtimeStatusesCache = runtimeStatusesCache;
    this.workspaceNext = workspaceNext;
  }

  @Override
  public Set<RuntimeIdentity> getIdentities() throws InfrastructureException {
    return runtimeStatusesCache.getIdentities();
  }

  @Override
  public RuntimeContext prepare(RuntimeIdentity id, InternalEnvironment environment)
      throws ValidationException, InfrastructureException {

    // Sidecar-based tooling for now supports k8s/OS env only
    // Convert other env types to a OS type to use this tooling with other env types
    final OpenShiftEnvironment openShiftEnvironment = asOpenShiftEnv(environment);
    // We need to provision development tooling here because there is environment variables
    // provisioning in the superclass which might be important
    workspaceNext.provision(id, openShiftEnvironment);

    return super.prepare(id, openShiftEnvironment);
  }

  @Override
  protected OpenShiftRuntimeContext internalPrepare(
      RuntimeIdentity id, InternalEnvironment environment)
      throws ValidationException, InfrastructureException {
    final OpenShiftEnvironment openShiftEnvironment = asOpenShiftEnv(environment);

    osEnvProvisioner.provision(openShiftEnvironment, id);

    return runtimeContextFactory.create(openShiftEnvironment, id, this);
  }

  private OpenShiftEnvironment asOpenShiftEnv(InternalEnvironment source)
      throws ValidationException, InfrastructureException {
    if (source instanceof OpenShiftEnvironment) {
      return (OpenShiftEnvironment) source;
    }

    if (source instanceof KubernetesEnvironment) {
      return new OpenShiftEnvironment((KubernetesEnvironment) source);
    }

    if (source instanceof DockerImageEnvironment) {
      KubernetesEnvironment k8sEnv =
          dockerImageEnvConverter.convert((DockerImageEnvironment) source);
      return new OpenShiftEnvironment(k8sEnv);
    }

    throw new InternalInfrastructureException(
        format(
            "Environment type '%s' is not supported. Supported environment types: %s",
            source.getRecipe().getType(), OpenShiftEnvironment.TYPE));
  }
}
