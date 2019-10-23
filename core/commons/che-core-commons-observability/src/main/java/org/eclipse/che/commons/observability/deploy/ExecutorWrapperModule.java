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
package org.eclipse.che.commons.observability.deploy;

import com.google.inject.AbstractModule;
import org.eclipse.che.commons.observability.*;
import org.eclipse.che.commons.schedule.executor.ThreadPullLauncher;

public class ExecutorWrapperModule extends AbstractModule {

  @Override
  protected void configure() {

    if (Boolean.valueOf(System.getenv("CHE_METRICS_ENABLED"))) {
      if (Boolean.valueOf(System.getenv("CHE_TRACING_ENABLED"))) {
        bind(ExecutorServiceWrapper.class).to(MeteredAndTracedExecutorServiceWrapper.class);
      } else {
        bind(ExecutorServiceWrapper.class).to(MeteredExecutorServiceWrapper.class);
      }
    } else {
      if (Boolean.valueOf(System.getenv("CHE_TRACING_ENABLED"))) {
        bind(ExecutorServiceWrapper.class).to(TracedExecutorServiceWrapper.class);
      } else {
        bind(ExecutorServiceWrapper.class).to(NoopExecutorServiceWrapper.class);
      }
    }

    bind(ThreadPullLauncher.class).to(ObservableThreadPullLauncher.class);
  }
}