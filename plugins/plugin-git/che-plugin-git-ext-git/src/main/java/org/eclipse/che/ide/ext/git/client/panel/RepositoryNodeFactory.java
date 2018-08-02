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
package org.eclipse.che.ide.ext.git.client.panel;

/** @author Mykola Morhun */
public interface RepositoryNodeFactory {
  /**
   * Creates new repository entry.
   *
   * @param repository repository name
   * @param changes number of changed file in the repository
   */
  RepositoryNode newRepositoryNode(String repository, int changes);
}
