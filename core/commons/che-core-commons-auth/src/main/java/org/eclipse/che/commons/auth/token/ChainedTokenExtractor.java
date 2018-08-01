/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which is available at http://www.eclipse.org/legal/epl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.commons.auth.token;

import javax.servlet.http.HttpServletRequest;

/**
 * Try to extract token from request in 3 steps. 1. From query parameter. 2. From header. 3. From
 * cookie.
 *
 * @author Sergii Kabashniuk
 */
public class ChainedTokenExtractor implements RequestTokenExtractor {

  private final HeaderRequestTokenExtractor headerRequestTokenExtractor;

  private final QueryRequestTokenExtractor queryRequestTokenExtractor;

  private final CookieRequestTokenExtractor cookieRequestTokenExtractor;

  public ChainedTokenExtractor() {
    headerRequestTokenExtractor = new HeaderRequestTokenExtractor();
    queryRequestTokenExtractor = new QueryRequestTokenExtractor();
    cookieRequestTokenExtractor = new CookieRequestTokenExtractor();
  }

  @Override
  public String getToken(HttpServletRequest req) {
    String token;
    if ((token = queryRequestTokenExtractor.getToken(req)) == null) {
      if ((token = headerRequestTokenExtractor.getToken(req)) == null) {
        token = cookieRequestTokenExtractor.getToken(req);
      }
    }
    return token;
  }
}
