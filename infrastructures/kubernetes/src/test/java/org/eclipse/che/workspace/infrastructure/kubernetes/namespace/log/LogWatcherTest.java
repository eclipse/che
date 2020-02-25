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
package org.eclipse.che.workspace.infrastructure.kubernetes.namespace.log;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.workspace.infrastructure.kubernetes.KubernetesClientFactory;
import org.eclipse.che.workspace.infrastructure.kubernetes.namespace.event.PodEvent;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(MockitoTestNGListener.class)
public class LogWatcherTest {

  private final String POD = "pod123";
  private final Set<String> PODNAMES = Collections.singleton(POD);
  private final String WORKSPACE_ID = "workspace123";
  private final String NAMESPACE = "namespace123";
  private final LogWatchTimeouts TIMEOUTS = new LogWatchTimeouts(100, 0, 0);

  @Mock private PodLogHandler handler;
  @Mock private KubernetesClientFactory clientFactory;
  @Mock private Executor executor;

  @BeforeMethod
  public void setUp() {}

  @Test
  public void executorIsNotCalledWhenContainerIsNull() throws InfrastructureException {
    // given
    LogWatcher logWatcher =
        new LogWatcher(clientFactory, WORKSPACE_ID, NAMESPACE, PODNAMES, executor, TIMEOUTS);
    logWatcher.addLogHandler(handler);
    PodEvent podEvent =
        new PodEvent(
            POD, null, "somereallygoodreason", "someevenbettermessage", "123456789", "987654321");

    // when
    logWatcher.handle(podEvent);

    // then
    verify(executor, times(0)).execute(any());
  }

  @Test
  public void executorIsNotCalledWhenPodNameDontMatch() throws InfrastructureException {
    // given
    LogWatcher logWatcher =
        new LogWatcher(clientFactory, WORKSPACE_ID, NAMESPACE, PODNAMES, executor, TIMEOUTS);
    logWatcher.addLogHandler(handler);
    PodEvent podEvent =
        new PodEvent(
            "someOtherPod",
            "container123",
            "Started",
            "someevenbettermessage",
            "123456789",
            "987654321");

    // when
    logWatcher.handle(podEvent);

    // then
    verify(executor, times(0)).execute(any());
  }

  @Test
  public void executorIsNotCalledWhenReasonIsNotStarted() throws InfrastructureException {
    // given
    LogWatcher logWatcher =
        new LogWatcher(clientFactory, WORKSPACE_ID, NAMESPACE, PODNAMES, executor, TIMEOUTS);
    logWatcher.addLogHandler(handler);
    PodEvent podEvent =
        new PodEvent(
            POD, "container123", "NotStarted", "someevenbettermessage", "123456789", "987654321");

    // when
    logWatcher.handle(podEvent);

    // then
    verify(executor, times(0)).execute(any());
  }

  @Test
  public void executorIsCalledWhenAllIsSet() throws InfrastructureException {
    // given
    LogWatcher logWatcher =
        new LogWatcher(clientFactory, WORKSPACE_ID, NAMESPACE, PODNAMES, executor, TIMEOUTS);
    logWatcher.addLogHandler(handler);
    PodEvent podEvent =
        new PodEvent(
            POD, "container123", "Started", "someevenbettermessage", "123456789", "987654321");

    // when
    logWatcher.handle(podEvent);

    // then
    verify(executor, times(1)).execute(any());
  }

  @Test
  public void executorIsCalledJustOnceWhenSameEventArriveAgain() throws InfrastructureException {
    // given
    LogWatcher logWatcher =
        new LogWatcher(clientFactory, WORKSPACE_ID, NAMESPACE, PODNAMES, executor, TIMEOUTS);
    logWatcher.addLogHandler(handler);
    PodEvent podEvent =
        new PodEvent(
            POD, "container123", "Started", "someevenbettermessage", "123456789", "987654321");

    // when
    logWatcher.handle(podEvent);
    logWatcher.handle(podEvent);

    // then
    verify(executor, times(1)).execute(any());
  }

  @Test
  public void executorIsNotCalledAgainAfterCleanup() throws InfrastructureException {
    // given
    LogWatcher logWatcher =
        new LogWatcher(clientFactory, WORKSPACE_ID, NAMESPACE, PODNAMES, executor, TIMEOUTS);
    logWatcher.addLogHandler(handler);
    PodEvent podEvent =
        new PodEvent(
            POD, "container123", "Started", "someevenbettermessage", "123456789", "987654321");

    // when
    logWatcher.handle(podEvent);
    logWatcher.close();
    logWatcher.handle(podEvent);

    // then
    verify(executor, times(1)).execute(any());
  }
}
