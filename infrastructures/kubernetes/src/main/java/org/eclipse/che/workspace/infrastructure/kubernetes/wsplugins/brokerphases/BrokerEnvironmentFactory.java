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
package org.eclipse.che.workspace.infrastructure.kubernetes.wsplugins.brokerphases;

import static java.util.Collections.singletonMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.model.impl.VolumeImpl;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.InternalInfrastructureException;
import org.eclipse.che.api.workspace.server.spi.environment.InternalMachineConfig;
import org.eclipse.che.api.workspace.server.spi.provision.env.AgentAuthEnableEnvVarProvider;
import org.eclipse.che.api.workspace.server.spi.provision.env.MachineTokenEnvVarProvider;
import org.eclipse.che.api.workspace.server.wsplugins.model.PluginMeta;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.workspace.infrastructure.kubernetes.Names;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.util.Containers;
import org.eclipse.che.workspace.infrastructure.kubernetes.wsplugins.BrokersResult;

/**
 * Creates {@link KubernetesEnvironment} with everything needed to deploy Plugin brokers.
 *
 * <p>It has to be extended to be used in the kubernetes or openshift infrastructures because of the
 * usage of a complex inheritance between components of these infrastructures.
 *
 * <p>This API is in <b>Beta</b> and is subject to changes or removal.
 *
 * @author Oleksandr Garagatyi
 */
@Beta
public abstract class BrokerEnvironmentFactory<E extends KubernetesEnvironment> {

  private static final String CONFIG_MAP_NAME_SUFFIX = "broker-config-map";
  private static final String BROKER_VOLUME = "broker-config-volume";
  private static final String CONF_FOLDER = "/broker-config";
  private static final String CONFIG_FILE = "config.json";
  private static final String PLUGINS_VOLUME_NAME = "plugins";
  private static final String BROKERS_POD_NAME = "che-plugin-broker";

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String cheWebsocketEndpoint;
  private final String brokerPullPolicy;
  private final AgentAuthEnableEnvVarProvider authEnableEnvVarProvider;
  private final MachineTokenEnvVarProvider machineTokenEnvVarProvider;
  private final String unifiedBrokerImage;
  private final String initBrokerImage;

  @Inject
  public BrokerEnvironmentFactory(
      @Named("che.websocket.endpoint") String cheWebsocketEndpoint,
      @Named("che.workspace.plugin_broker.pull_policy") String brokerPullPolicy,
      AgentAuthEnableEnvVarProvider authEnableEnvVarProvider,
      MachineTokenEnvVarProvider machineTokenEnvVarProvider,
      @Named("che.workspace.plugin_broker.unified.image") String unifiedBrokerImage,
      @Named("che.workspace.plugin_broker.init.image") String initBrokerImage) {
    this.cheWebsocketEndpoint = cheWebsocketEndpoint;
    this.brokerPullPolicy = brokerPullPolicy;
    this.authEnableEnvVarProvider = authEnableEnvVarProvider;
    this.machineTokenEnvVarProvider = machineTokenEnvVarProvider;
    this.unifiedBrokerImage = unifiedBrokerImage;
    this.initBrokerImage = initBrokerImage;
  }

  /**
   * Creates {@link KubernetesEnvironment} with everything needed to deploy Plugin broker.
   *
   * @param pluginsMeta meta info of plugins that needs to be resolved by the broker
   * @param runtimeID ID of the runtime the broker would be started
   * @param brokersResult needs to be called with {@link BrokersResult#oneMoreBroker()} for each
   *     broker to allow proper waiting of execution of all the brokers
   * @return kubernetes environment (or its extension) with the Plugin broker objects
   */
  public E create(
      Collection<PluginMeta> pluginsMeta, RuntimeIdentity runtimeID, BrokersResult brokersResult)
      throws InfrastructureException {

    BrokersConfigs brokersConfigs = BrokersConfigs.instance();
    Pod pod = brokersConfigs.pod;

    PodSpec spec = pod.getSpec();
    List<EnvVar> envVars =
        Stream.of(
                authEnableEnvVarProvider.get(runtimeID), machineTokenEnvVarProvider.get(runtimeID))
            .map(this::asEnvVar)
            .collect(Collectors.toList());

    BrokerConfig brokerConfig =
        createBrokerConfig(runtimeID, pluginsMeta, envVars, unifiedBrokerImage, pod);
    brokersConfigs.machines.put(brokerConfig.machineName, brokerConfig.machineConfig);
    brokersConfigs.configMaps.put(brokerConfig.configMapName, brokerConfig.configMap);
    spec.getContainers().add(brokerConfig.container);
    spec.getVolumes()
        .add(
            new VolumeBuilder()
                .withName(brokerConfig.configMapVolume)
                .withNewConfigMap()
                .withName(brokerConfig.configMapName)
                .endConfigMap()
                .build());

    // Add init broker that cleans up /plugins
    BrokerConfig initBrokerConfig =
        createBrokerConfig(runtimeID, null, envVars, initBrokerImage, pod);
    pod.getSpec().getInitContainers().add(initBrokerConfig.container);
    brokersConfigs.machines.put(initBrokerConfig.machineName, initBrokerConfig.machineConfig);

    return doCreate(brokersConfigs);
  }

  /** Needed to implement this component in both - Kubernetes and Openshift infrastructures. */
  protected abstract E doCreate(BrokersConfigs brokersConfigs);

  private String generateUniqueName(String suffix) {
    return NameGenerator.generate(suffix, 6);
  }

  private Container newContainer(
      RuntimeIdentity runtimeId,
      List<EnvVar> envVars,
      String image,
      @Nullable String brokerVolumeName) {
    final ContainerBuilder cb =
        new ContainerBuilder()
            .withName(image.toLowerCase().replaceAll("[^\\d\\w-]", "-"))
            .withImage(image)
            .withArgs(
                "-push-endpoint",
                cheWebsocketEndpoint,
                "-runtime-id",
                String.format(
                    "%s:%s:%s",
                    runtimeId.getWorkspaceId(),
                    MoreObjects.firstNonNull(runtimeId.getEnvName(), ""),
                    runtimeId.getOwnerId()))
            .withImagePullPolicy(brokerPullPolicy)
            .withEnv(envVars);
    if (brokerVolumeName != null) {
      cb.withVolumeMounts(new VolumeMount(CONF_FOLDER + "/", null, brokerVolumeName, true, null));
      cb.addToArgs("-metas", CONF_FOLDER + "/" + CONFIG_FILE);
    }
    Container container = cb.build();
    Containers.addRamLimit(container, "250Mi");
    Containers.addRamRequest(container, "250Mi");
    return container;
  }

  private ConfigMap newConfigMap(String configMapName, Collection<PluginMeta> pluginsMetas)
      throws InternalInfrastructureException {
    try {
      return new ConfigMapBuilder()
          .withNewMetadata()
          .withName(configMapName)
          .endMetadata()
          .withData(singletonMap(CONFIG_FILE, objectMapper.writeValueAsString(pluginsMetas)))
          .build();
    } catch (JsonProcessingException e) {
      throw new InternalInfrastructureException(e.getMessage(), e);
    }
  }

  private EnvVar asEnvVar(Pair<String, String> envVar) {
    return new EnvVarBuilder().withName(envVar.first).withValue(envVar.second).build();
  }

  private BrokerConfig createBrokerConfig(
      RuntimeIdentity runtimeId,
      @Nullable Collection<PluginMeta> pluginsMeta,
      List<EnvVar> envVars,
      String image,
      Pod pod)
      throws InternalInfrastructureException {

    BrokerConfig brokerConfig = new BrokerConfig();
    String configMapVolume = null;
    if (pluginsMeta != null) {
      brokerConfig.configMapName = generateUniqueName(CONFIG_MAP_NAME_SUFFIX);
      brokerConfig.configMapVolume = generateUniqueName(BROKER_VOLUME);
      brokerConfig.configMap = newConfigMap(brokerConfig.configMapName, pluginsMeta);
      configMapVolume = brokerConfig.configMapVolume;
    }
    brokerConfig.container = newContainer(runtimeId, envVars, image, configMapVolume);
    brokerConfig.machineName = Names.machineName(pod, brokerConfig.container);
    brokerConfig.machineConfig = new InternalMachineConfig();
    brokerConfig
        .machineConfig
        .getVolumes()
        .put(PLUGINS_VOLUME_NAME, new VolumeImpl().withPath("/plugins"));

    return brokerConfig;
  }

  private static class BrokerConfig {

    String configMapName;
    ConfigMap configMap;
    Container container;
    String machineName;
    InternalMachineConfig machineConfig;
    String configMapVolume;
  }

  public static class BrokersConfigs {

    public final Map<String, InternalMachineConfig> machines;
    public final Map<String, ConfigMap> configMaps;
    public final Pod pod;

    private static Pod newPod() {
      return new PodBuilder()
          .withNewMetadata()
          .withName(BROKERS_POD_NAME)
          .endMetadata()
          .withNewSpec()
          .withRestartPolicy("Never")
          .endSpec()
          .build();
    }

    public static BrokersConfigs instance() {

      Map<String, InternalMachineConfig> machines = new HashMap<>();
      Map<String, ConfigMap> configMaps = new HashMap<>();
      Pod pod = newPod();
      return new BrokersConfigs(machines, configMaps, pod);
    }

    public BrokersConfigs(
        final Map<String, InternalMachineConfig> machines,
        final Map<String, ConfigMap> configMaps,
        final Pod pod) {
      this.machines = machines;
      this.configMaps = configMaps;
      this.pod = pod;
    }
  }
}
