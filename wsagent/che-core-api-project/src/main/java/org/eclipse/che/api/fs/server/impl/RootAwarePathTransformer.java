/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.fs.server.impl;

import static org.eclipse.che.api.fs.server.WsPathUtils.ROOT;

import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.api.fs.server.PathTransformer;
import org.eclipse.che.api.project.server.impl.RootDirPathProvider;

@Singleton
public class RootAwarePathTransformer implements PathTransformer {

  private final Path root;

  @Inject
  public RootAwarePathTransformer(RootDirPathProvider rootProvider) {
    this.root = Paths.get(rootProvider.get());
  }

  @Override
  public Path transform(String wsPath) {
    if (ROOT.equals(wsPath)) {
      return root;
    }

    wsPath = wsPath.startsWith(ROOT) ? wsPath.substring(1) : wsPath;

    return root.resolve(wsPath).normalize().toAbsolutePath();
  }

  @Override
  public String transform(Path fsPath) {
    Path absoluteFsPath = fsPath.toAbsolutePath();

    return ROOT + root.relativize(absoluteFsPath);
  }
}
