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
package org.eclipse.che.workspace.infrastructure.kubernetes.provision;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.eclipse.che.workspace.infrastructure.kubernetes.namespace.pvc.CommonPVCStrategy.COMMON_STRATEGY;
import static org.eclipse.che.workspace.infrastructure.kubernetes.provision.AsyncStorageProvisioner.ASYNC_STORAGE;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.user.server.PreferenceManager;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.workspace.server.WorkspaceRuntimes;
import org.eclipse.che.api.workspace.server.spi.InternalRuntime;
import org.eclipse.che.api.workspace.shared.Constants;
import org.eclipse.che.workspace.infrastructure.kubernetes.KubernetesClientFactory;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(value = {MockitoTestNGListener.class})
public class AsyncStoragePodWatcherTest {

  private final String NAMESPACE = UUID.randomUUID().toString();
  private final String WORKSPACE_ID = UUID.randomUUID().toString();
  private final String USER_ID = UUID.randomUUID().toString();
  private final Instant clock = Clock.systemDefaultZone().instant();

  private Map<String, String> userPref;

  @Mock private KubernetesClientFactory kubernetesClientFactory;
  @Mock private UserManager userManager;
  @Mock private PreferenceManager preferenceManager;
  @Mock private WorkspaceRuntimes runtimes;
  @Mock private KubernetesClient kubernetesClient;
  @Mock private PodResource<Pod, DoneablePod> podResource;
  @Mock private MixedOperation mixedOperationPod;
  @Mock private NonNamespaceOperation namespacePodOperation;
  @Mock private UserImpl user;

  @BeforeMethod
  public void setUp() throws Exception {
    when(user.getId()).thenReturn(USER_ID);
    userPref = new HashMap<>(3);
    long epochSecond = Clock.systemDefaultZone().instant().getEpochSecond();
    long activityTime = epochSecond - 600; // stored time 10 minutes early
    userPref.put(Constants.LAST_ACTIVITY_TIME, Long.toString(activityTime));
    userPref.put(Constants.LAST_ACTIVE_INFRASTRUCTURE_NAMESPACE, NAMESPACE);
    when(preferenceManager.find(USER_ID)).thenReturn(userPref);

    Page<UserImpl> userPage = new Page<>(Collections.singleton(user), 0, 1, 1);
    when(userManager.getAll(anyInt(), anyLong())).thenReturn(userPage);

    when(kubernetesClientFactory.create()).thenReturn(kubernetesClient);
    when(kubernetesClient.pods()).thenReturn(mixedOperationPod);
    when(mixedOperationPod.inNamespace(NAMESPACE)).thenReturn(namespacePodOperation);
    when(namespacePodOperation.withName(ASYNC_STORAGE)).thenReturn(podResource);
  }

  @Test
  public void shouldDeleteAsyncStoragePod() throws Exception {
    AsyncStoragePodWatcher watcher =
        new AsyncStoragePodWatcher(
            kubernetesClientFactory,
            userManager,
            preferenceManager,
            runtimes,
            1,
            COMMON_STRATEGY,
            false,
            "<username>",
            1);

    when(runtimes.getInProgress(USER_ID)).thenReturn(emptySet());

    ObjectMeta meta = new ObjectMeta();
    meta.setName(ASYNC_STORAGE);
    Pod pod = new Pod();
    pod.setMetadata(meta);
    when(podResource.get()).thenReturn(pod);

    watcher.check();

    verify(preferenceManager).find(USER_ID);
    verify(podResource).delete();
  }

  @Test
  public void shouldNotDeleteAsyncStoragePodIfTooEarly() throws Exception {
    AsyncStoragePodWatcher watcher =
        new AsyncStoragePodWatcher(
            kubernetesClientFactory,
            userManager,
            preferenceManager,
            runtimes,
            10,
            COMMON_STRATEGY,
            false,
            "<username>",
            1);
    long epochSecond = clock.getEpochSecond();
    userPref.put(Constants.LAST_ACTIVITY_TIME, Long.toString(epochSecond));

    watcher.check();

    verify(preferenceManager).find(USER_ID);
    verifyNoMoreInteractions(kubernetesClientFactory);
    verifyNoMoreInteractions(podResource);
  }

  @Test
  public void shouldNotDeleteAsyncStoragePodIfHasActiveRuntime() throws Exception {
    AsyncStoragePodWatcher watcher =
        new AsyncStoragePodWatcher(
            kubernetesClientFactory,
            userManager,
            preferenceManager,
            runtimes,
            1,
            COMMON_STRATEGY,
            false,
            "<username>",
            1);

    // has active runtime
    InternalRuntime runtime = mock(InternalRuntime.class);
    when(runtime.getOwner()).thenReturn(USER_ID);
    when(runtimes.getInProgress(USER_ID)).thenReturn(singleton(WORKSPACE_ID));
    when(runtimes.getInternalRuntime(WORKSPACE_ID)).thenReturn(runtime);

    Page<UserImpl> userPage = new Page<>(Collections.singleton(user), 0, 1, 1);
    when(userManager.getAll(anyInt(), anyLong())).thenReturn(userPage);

    watcher.check();

    verify(preferenceManager).find(USER_ID);
    verifyNoMoreInteractions(kubernetesClientFactory);
    verifyNoMoreInteractions(podResource);
  }

  @Test
  public void shouldNotDeleteAsyncStoragePodIfNoRecord() throws Exception {
    AsyncStoragePodWatcher watcher =
        new AsyncStoragePodWatcher(
            kubernetesClientFactory,
            userManager,
            preferenceManager,
            runtimes,
            1,
            COMMON_STRATEGY,
            false,
            "<username>",
            1);
    when(preferenceManager.find(USER_ID)).thenReturn(emptyMap()); // no records in user preferences

    watcher.check();

    verify(preferenceManager).find(USER_ID);
    verifyNoMoreInteractions(kubernetesClientFactory);
    verifyNoMoreInteractions(podResource);
  }

  @Test
  public void shouldDoNothingIfNotCommonPvcStrategy() throws Exception {
    AsyncStoragePodWatcher watcher =
        new AsyncStoragePodWatcher(
            kubernetesClientFactory,
            userManager,
            preferenceManager,
            runtimes,
            1,
            "my-own-strategy",
            false,
            "<username>",
            1);
    when(preferenceManager.find(USER_ID)).thenReturn(emptyMap()); // no records in user preferences

    watcher.check();

    verifyNoMoreInteractions(preferenceManager);
    verifyNoMoreInteractions(kubernetesClientFactory);
    verifyNoMoreInteractions(podResource);
  }

  @Test
  public void shouldDoNothingIfAllowedUserDefinedNamespaces() throws Exception {
    AsyncStoragePodWatcher watcher =
        new AsyncStoragePodWatcher(
            kubernetesClientFactory,
            userManager,
            preferenceManager,
            runtimes,
            1,
            "my-own-strategy",
            true,
            "<username>",
            1);
    when(preferenceManager.find(USER_ID)).thenReturn(emptyMap()); // no records in user preferences

    watcher.check();

    verifyNoMoreInteractions(preferenceManager);
    verifyNoMoreInteractions(kubernetesClientFactory);
    verifyNoMoreInteractions(podResource);
  }

  @Test
  public void shouldDoNothingIfDefaultNamespaceNotCorrect() throws Exception {
    AsyncStoragePodWatcher watcher =
        new AsyncStoragePodWatcher(
            kubernetesClientFactory,
            userManager,
            preferenceManager,
            runtimes,
            1,
            "my-own-strategy",
            true,
            "<foo-bar>",
            1);
    when(preferenceManager.find(USER_ID)).thenReturn(emptyMap()); // no records in user preferences

    watcher.check();

    verifyNoMoreInteractions(preferenceManager);
    verifyNoMoreInteractions(kubernetesClientFactory);
    verifyNoMoreInteractions(podResource);
  }

  @Test
  public void shouldDoNothingIfAllowMoreThanOneRuntime() throws Exception {
    AsyncStoragePodWatcher watcher =
        new AsyncStoragePodWatcher(
            kubernetesClientFactory,
            userManager,
            preferenceManager,
            runtimes,
            1,
            "my-own-strategy",
            true,
            "<foo-bar>",
            2);
    when(preferenceManager.find(USER_ID)).thenReturn(emptyMap()); // no records in user preferences

    watcher.check();

    verifyNoMoreInteractions(preferenceManager);
    verifyNoMoreInteractions(kubernetesClientFactory);
    verifyNoMoreInteractions(podResource);
  }
}
