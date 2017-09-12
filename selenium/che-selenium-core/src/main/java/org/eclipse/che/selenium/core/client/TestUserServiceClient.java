/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.selenium.core.client;

import java.io.IOException;

/** @author Dmytro Nochevnov */
public interface TestUserServiceClient {

  /**
   * Creates user.
   *
   * @param name name of user
   * @param email email of user
   * @param password user's password
   * @return id of user
   * @throws IOException
   */
  String create(String name, String email, String password) throws IOException;

  /**
   * Deletes user by its id.
   *
   * @param id user's id
   * @throws IOException
   */
  void delete(String id) throws IOException;
}
