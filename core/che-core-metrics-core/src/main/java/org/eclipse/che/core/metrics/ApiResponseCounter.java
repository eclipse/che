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
package org.eclipse.che.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import javax.inject.Singleton;

@Singleton
/**
 * Metric binding for Che API responses, that are grouped by http status codes.
 *
 * @author Mykhailo Kuznietsov
 */
public class ApiResponseCounter implements MeterBinder {
  protected Counter informationalResponseCounter;
  protected Counter successResponseCounter;
  protected Counter redirectResponseCounter;
  protected Counter clientErrorResponseCounter;
  protected Counter serverErrorResponseCounter;

  @Override
  public void bindTo(MeterRegistry registry) {
    informationalResponseCounter =
        Counter.builder("che.server.api.response")
            .description("Che Server Tomcat informational responses (1xx responses)")
            .tags("code=1xx", "area=http")
            .register(registry);
    successResponseCounter =
        Counter.builder("che.server.api.response")
            .description("Che Server Tomcat success responses (2xx responses)")
            .tag("code", "2xx")
            .tag("area", "http")
            .register(registry);
    redirectResponseCounter =
        Counter.builder("che.server.api.response")
            .description("Che Server Tomcat redirect responses (3xx responses)")
            .tag("code", "3xx")
            .tag("area", "http")
            .register(registry);
    clientErrorResponseCounter =
        Counter.builder("che.server.api.response")
            .description("Che Server Tomcat client errors (4xx responses)")
            .tag("code", "4xx")
            .tag("area", "http")
            .register(registry);
    serverErrorResponseCounter =
        Counter.builder("che.server.api.response")
            .description("Che Server Tomcat server errors (5xx responses)")
            .tag("code", "5xx")
            .tag("area", "http")
            .register(registry);
  }

  public void handleStatus(int status) {
    status = status / 100;
    switch (status) {
      case 1:
        informationalResponseCounter.increment();
        break;
      case 2:
        successResponseCounter.increment();
        break;
      case 3:
        redirectResponseCounter.increment();
        break;
      case 4:
        clientErrorResponseCounter.increment();
        break;
      case 5:
        serverErrorResponseCounter.increment();
        break;
    }
  }
}
